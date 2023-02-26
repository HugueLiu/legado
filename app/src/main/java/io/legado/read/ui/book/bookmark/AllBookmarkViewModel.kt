package io.legado.read.ui.book.bookmark

import android.annotation.SuppressLint
import android.app.Application
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import io.legado.read.base.BaseViewModel
import io.legado.read.data.appDb
import io.legado.read.data.entities.Bookmark
import io.legado.read.utils.*
import io.legado.read.utils.FileUtils
import io.legado.read.utils.GSON
import io.legado.read.utils.isContentScheme
import io.legado.read.utils.writeToOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class AllBookmarkViewModel(application: Application) : BaseViewModel(application) {


    fun initData(onSuccess: (bookmarks: List<Bookmark>) -> Unit) {
        execute {
            appDb.bookmarkDao.all
        }.onSuccess {
            onSuccess.invoke(it)
        }
    }

    fun deleteBookmark(bookmark: Bookmark) {
        execute {
            appDb.bookmarkDao.delete(bookmark)
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    @SuppressLint("SimpleDateFormat")
    fun saveToFile(treeUri: Uri) {
        execute {
            val dateFormat = SimpleDateFormat("yyMMddHHmmss")
            if (treeUri.isContentScheme()) {
                val doc = DocumentFile.fromTreeUri(context, treeUri)
                    ?.createFile("", "bookmark-${dateFormat.format(Date())}")
                doc?.let {
                    context.contentResolver.openOutputStream(doc.uri)!!.use {
                        GSON.writeToOutputStream(it, appDb.bookmarkDao.all)
                    }
                }
            } else {
                val path = treeUri.path!!
                val file = FileUtils.createFileIfNotExist(File(path), "bookmark-${dateFormat.format(Date())}")
                FileOutputStream(file).use {
                    GSON.writeToOutputStream(it, appDb.bookmarkDao.all)
                }
            }
        }
    }

}