package io.legado.read.ui.book.changecover

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.legado.read.base.BaseViewModel
import io.legado.read.constant.AppConst
import io.legado.read.constant.AppPattern
import io.legado.read.data.appDb
import io.legado.read.data.entities.BookSource
import io.legado.read.data.entities.SearchBook
import io.legado.read.help.config.AppConfig
import io.legado.read.help.coroutine.CompositeCoroutine
import io.legado.read.model.webBook.WebBook
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import java.util.*
import java.util.concurrent.Executors
import kotlin.math.min

class ChangeCoverViewModel(application: Application) : BaseViewModel(application) {
    private val threadCount = AppConfig.threadCount
    private var searchPool: ExecutorCoroutineDispatcher? = null
    private val tasks = CompositeCoroutine()
    private var searchSuccess: ((SearchBook) -> Unit)? = null
    private var bookSourceList = arrayListOf<BookSource>()
    private val defaultCover by lazy {
        listOf(
            SearchBook(
                originName = "默认封面",
                name = name,
                author = author,
                coverUrl = "use_default_cover"
            )
        )
    }
    val searchStateData = MutableLiveData<Boolean>()
    var name: String = ""
    var author: String = ""
    val dataFlow = callbackFlow<List<SearchBook>> {
        val searchBooks = Collections.synchronizedList(arrayListOf<SearchBook>())

        searchSuccess = { searchBook ->
            if (!searchBooks.contains(searchBook)) {
                searchBooks.add(searchBook)
                trySend(defaultCover + searchBooks.sortedBy { it.originOrder })
            }
        }

        appDb.searchBookDao.getEnableHasCover(name, author).let {
            searchBooks.addAll(it)
            trySend(defaultCover + searchBooks.toList())
        }

        if (searchBooks.size <= 1) {
            startSearch()
        }

        awaitClose {
            searchBooks.clear()
            searchSuccess = null
        }
    }.flowOn(IO)

    @Volatile
    private var searchIndex = -1

    fun initData(arguments: Bundle?) {
        arguments?.let { bundle ->
            bundle.getString("name")?.let {
                name = it
            }
            bundle.getString("author")?.let {
                author = it.replace(AppPattern.authorRegex, "")
            }
        }
    }

    private fun initSearchPool() {
        searchPool = Executors
            .newFixedThreadPool(min(threadCount, AppConst.MAX_THREAD)).asCoroutineDispatcher()
        searchIndex = -1
    }

    private fun startSearch() {
        execute {
            stopSearch()
            bookSourceList.clear()
            bookSourceList.addAll(appDb.bookSourceDao.allEnabled)
            searchStateData.postValue(true)
            initSearchPool()
            for (i in 0 until threadCount) {
                search()
            }
        }
    }

    @Synchronized
    private fun search() {
        if (searchIndex >= bookSourceList.lastIndex) {
            return
        }
        searchIndex++
        val source = bookSourceList[searchIndex]
        if (source.getSearchRule().coverUrl.isNullOrBlank()) {
            searchNext()
            return
        }
        val task = WebBook
            .searchBook(viewModelScope, source, name, context = searchPool!!)
            .timeout(60000L)
            .onSuccess(IO) {
                it.firstOrNull()?.let { searchBook ->
                    if (searchBook.name == name && searchBook.author == author
                        && !searchBook.coverUrl.isNullOrEmpty()
                    ) {
                        appDb.searchBookDao.insert(searchBook)
                        searchSuccess?.invoke(searchBook)
                    }
                }
            }
            .onFinally(searchPool) {
                searchNext()
            }
        tasks.add(task)
    }

    @Synchronized
    private fun searchNext() {
        if (searchIndex < bookSourceList.lastIndex) {
            search()
        } else {
            searchIndex++
        }
        if (searchIndex >= bookSourceList.lastIndex + min(
                bookSourceList.size,
                threadCount
            )
        ) {
            searchStateData.postValue(false)
            tasks.clear()
        }
    }

    fun startOrStopSearch() {
        if (tasks.isEmpty) {
            startSearch()
        } else {
            stopSearch()
        }
    }

    private fun stopSearch() {
        tasks.clear()
        searchPool?.close()
        searchStateData.postValue(false)
    }

    override fun onCleared() {
        super.onCleared()
        searchPool?.close()
    }

}