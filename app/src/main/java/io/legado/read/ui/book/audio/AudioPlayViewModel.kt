package io.legado.read.ui.book.audio

import android.app.Application
import android.content.Intent
import io.legado.read.R
import io.legado.read.base.BaseViewModel
import io.legado.read.constant.BookType
import io.legado.read.constant.EventBus
import io.legado.read.data.appDb
import io.legado.read.data.entities.Book
import io.legado.read.data.entities.BookChapter
import io.legado.read.data.entities.BookSource
import io.legado.read.help.book.removeType
import io.legado.read.model.AudioPlay
import io.legado.read.model.webBook.WebBook
import io.legado.read.utils.postEvent
import io.legado.read.utils.toastOnUi
import kotlinx.coroutines.Dispatchers

class AudioPlayViewModel(application: Application) : BaseViewModel(application) {

    fun initData(intent: Intent) = AudioPlay.apply {
        execute {
            val bookUrl = intent.getStringExtra("bookUrl")
            if (bookUrl != null && bookUrl != book?.bookUrl) {
                stop(context)
                inBookshelf = intent.getBooleanExtra("inBookshelf", true)
                book = appDb.bookDao.getBook(bookUrl)
                book?.let { book ->
                    titleData.postValue(book.name)
                    coverData.postValue(book.getDisplayCover())
                    durChapter = appDb.bookChapterDao.getChapter(book.bookUrl, book.durChapterIndex)
                    upDurChapter(book)
                    bookSource = appDb.bookSourceDao.getBookSource(book.origin)
                    if (durChapter == null) {
                        if (book.tocUrl.isEmpty()) {
                            loadBookInfo(book)
                        } else {
                            loadChapterList(book)
                        }
                    }
                }
            }
            saveRead()
        }
    }

    private fun loadBookInfo(book: Book) {
        execute {
            AudioPlay.bookSource?.let {
                WebBook.getBookInfo(this, it, book)
                    .onSuccess {
                        loadChapterList(book)
                    }
            }
        }
    }

    private fun loadChapterList(book: Book) {
        execute {
            AudioPlay.bookSource?.let {
                WebBook.getChapterList(this, it, book)
                    .onSuccess(Dispatchers.IO) { cList ->
                        appDb.bookChapterDao.insert(*cList.toTypedArray())
                        AudioPlay.upDurChapter(book)
                    }.onError {
                        context.toastOnUi(R.string.error_load_toc)
                    }
            }
        }
    }

    fun upSource() {
        execute {
            AudioPlay.book?.let { book ->
                AudioPlay.bookSource = appDb.bookSourceDao.getBookSource(book.origin)
            }
        }
    }

    fun changeTo(source: BookSource, book: Book, toc: List<BookChapter>) {
        execute {
            AudioPlay.book?.migrateTo(book, toc)
            book.removeType(BookType.updateError)
            AudioPlay.book?.delete()
            appDb.bookDao.insert(book)
            AudioPlay.book = book
            AudioPlay.bookSource = source
            appDb.bookChapterDao.insert(*toc.toTypedArray())
            AudioPlay.upDurChapter(book)
        }.onFinally {
            postEvent(EventBus.SOURCE_CHANGED, book.bookUrl)
        }
    }

    fun removeFromBookshelf(success: (() -> Unit)?) {
        execute {
            AudioPlay.book?.let {
                appDb.bookDao.delete(it)
            }
        }.onSuccess {
            success?.invoke()
        }
    }

}