package io.legado.read.ui.book.manage

import android.app.Application
import androidx.lifecycle.MutableLiveData
import io.legado.read.base.BaseViewModel
import io.legado.read.constant.BookType
import io.legado.read.data.appDb
import io.legado.read.data.entities.Book
import io.legado.read.data.entities.BookSource
import io.legado.read.help.book.isLocal
import io.legado.read.help.book.removeType
import io.legado.read.help.coroutine.Coroutine
import io.legado.read.model.localBook.LocalBook
import io.legado.read.model.webBook.WebBook
import io.legado.read.utils.toastOnUi


class BookshelfManageViewModel(application: Application) : BaseViewModel(application) {
    var groupId: Long = -1L
    var groupName: String? = null
    val batchChangeSourceState = MutableLiveData<Boolean>()
    val batchChangeSourceProcessLiveData = MutableLiveData<String>()
    var batchChangeSourceCoroutine: Coroutine<Unit>? = null

    fun upCanUpdate(books: List<Book>, canUpdate: Boolean) {
        execute {
            val array = Array(books.size) {
                books[it].copy(canUpdate = canUpdate)
            }
            appDb.bookDao.update(*array)
        }
    }

    fun updateBook(vararg book: Book) {
        execute {
            appDb.bookDao.update(*book)
        }
    }

    fun deleteBook(books: List<Book>, deleteOriginal: Boolean = false) {
        execute {
            appDb.bookDao.delete(*books.toTypedArray())
            books.forEach {
                if (it.isLocal) {
                    LocalBook.deleteBook(it, deleteOriginal)
                }
            }
        }
    }

    fun changeSource(books: List<Book>, source: BookSource) {
        batchChangeSourceCoroutine?.cancel()
        batchChangeSourceCoroutine = execute {
            books.forEachIndexed { index, book ->
                batchChangeSourceProcessLiveData.postValue("${index + 1}/${books.size}")
                if (book.isLocal) return@forEachIndexed
                if (book.origin == source.bookSourceUrl) return@forEachIndexed
                WebBook.preciseSearchAwait(this, source, book.name, book.author)
                    .onFailure {
                        context.toastOnUi("获取书籍出错\n${it.localizedMessage}")
                    }.getOrNull()?.let { newBook ->
                        WebBook.getChapterListAwait(source, newBook)
                            .onFailure {
                                context.toastOnUi("获取目录出错\n${it.localizedMessage}")
                            }.getOrNull()?.let { toc ->
                                book.migrateTo(newBook, toc)
                                book.removeType(BookType.updateError)
                                appDb.bookDao.insert(newBook)
                                appDb.bookChapterDao.insert(*toc.toTypedArray())
                            }
                    }
            }
        }.onStart {
            batchChangeSourceState.postValue(true)
        }.onFinally {
            batchChangeSourceState.postValue(false)
        }
    }

}