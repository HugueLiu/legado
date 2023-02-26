@file:Suppress("unused")

package io.legado.read.help.book

import android.net.Uri
import io.legado.read.constant.BookType
import io.legado.read.data.entities.BaseBook
import io.legado.read.data.entities.Book
import io.legado.read.data.entities.BookSource
import io.legado.read.exception.NoStackTraceException
import io.legado.read.utils.*
import io.legado.read.utils.*
import splitties.init.appCtx
import java.io.File
import java.util.concurrent.ConcurrentHashMap


val Book.isAudio: Boolean
    get() {
        return type and io.legado.read.constant.BookType.audio > 0
    }

val Book.isImage: Boolean
    get() {
        return type and io.legado.read.constant.BookType.image > 0
    }

val Book.isLocal: Boolean
    get() {
        if (type == 0) {
            return origin == io.legado.read.constant.BookType.localTag || origin.startsWith(io.legado.read.constant.BookType.webDavTag)
        }
        return type and io.legado.read.constant.BookType.local > 0
    }

val Book.isLocalTxt: Boolean
    get() {
        return isLocal && originName.endsWith(".txt", true)
    }

val Book.isEpub: Boolean
    get() {
        return isLocal && originName.endsWith(".epub", true)
    }

val Book.isUmd: Boolean
    get() {
        return isLocal && originName.endsWith(".umd", true)
    }
val Book.isPdf: Boolean
    get() {
        return isLocal && originName.endsWith(".pdf", true)
    }

val Book.isOnLineTxt: Boolean
    get() {
        return !isLocal && type and io.legado.read.constant.BookType.text > 0
    }

val Book.isUpError: Boolean
    get() = type and io.legado.read.constant.BookType.updateError > 0

fun Book.contains(word: String?): Boolean {
    if (word.isNullOrEmpty()) {
        return true
    }
    return name.contains(word) || author.contains(word)
            || originName.contains(word) || origin.contains(word)
}

private val localUriCache by lazy {
    ConcurrentHashMap<String, Uri>()
}

fun Book.getLocalUri(): Uri {
    if (!isLocal) {
        throw NoStackTraceException("不是本地书籍")
    }
    var uri = localUriCache[bookUrl]
    if (uri != null) {
        return uri
    }
    uri = if (bookUrl.isUri()) {
        Uri.parse(bookUrl)
    } else {
        Uri.fromFile(File(bookUrl))
    }
    //先检测uri是否有效,这个比较快
    uri.inputStream(appCtx).getOrNull()?.use {
        localUriCache[bookUrl] = uri
    }?.let {
        return uri
    }
    //不同的设备书籍保存路径可能不一样, uri无效时尝试寻找当前保存路径下的文件
    val defaultBookDir = io.legado.read.help.config.AppConfig.defaultBookTreeUri
    val importBookDir = io.legado.read.help.config.AppConfig.importBookPath

    // 查找书籍保存目录
    if (!defaultBookDir.isNullOrBlank()) {
        val treeUri = Uri.parse(defaultBookDir)
        val treeFileDoc = io.legado.read.utils.FileDoc.fromUri(treeUri, true)
        if (!treeFileDoc.exists()) {
            appCtx.toastOnUi("书籍保存目录失效，请重新设置！")
        } else {
            val fileDoc = treeFileDoc.find(originName, 5)
            if (fileDoc != null) {
                localUriCache[bookUrl] = fileDoc.uri
                //更新bookUrl 重启不用再找一遍
                bookUrl = fileDoc.toString()
                save()
                return fileDoc.uri
            }
        }
    }

    // 查找添加本地选择的目录
    if (!importBookDir.isNullOrBlank() && defaultBookDir != importBookDir) {
        val treeUri = if (importBookDir.isUri()) {
            Uri.parse(importBookDir)
        } else {
            Uri.fromFile(File(importBookDir))
        }
        val treeFileDoc = io.legado.read.utils.FileDoc.fromUri(treeUri, true)
        val fileDoc = treeFileDoc.find(originName, 5)
        if (fileDoc != null) {
            localUriCache[bookUrl] = fileDoc.uri
            bookUrl = fileDoc.toString()
            save()
            return fileDoc.uri
        }
    }

    localUriCache[bookUrl] = uri
    return uri
}

fun Book.cacheLocalUri(uri: Uri) {
    localUriCache[bookUrl] = uri
}

fun Book.removeLocalUriCache() {
    localUriCache.remove(bookUrl)
}

fun Book.getRemoteUrl(): String? {
    if (origin.startsWith(io.legado.read.constant.BookType.webDavTag)) {
        return origin.substring(8)
    }
    return null
}

fun Book.setType(@BookType.Type vararg types: Int) {
    type = 0
    addType(*types)
}

fun Book.addType(@BookType.Type vararg types: Int) {
    types.forEach {
        type = type or it
    }
}

fun Book.removeType(@BookType.Type vararg types: Int) {
    types.forEach {
        type = type and it.inv()
    }
}

fun Book.clearType() {
    type = 0
}

fun Book.upType() {
    if (type < 8) {
        type = when (type) {
            io.legado.read.constant.BookSourceType.image -> io.legado.read.constant.BookType.image
            io.legado.read.constant.BookSourceType.audio -> io.legado.read.constant.BookType.audio
            io.legado.read.constant.BookSourceType.file -> io.legado.read.constant.BookType.webFile
            else -> io.legado.read.constant.BookType.text
        }
        if (origin == "loc_book" || origin.startsWith(io.legado.read.constant.BookType.webDavTag)) {
            type = type or io.legado.read.constant.BookType.local
        }
    }
}

fun BookSource.getBookType(): Int {
    return when (bookSourceType) {
        io.legado.read.constant.BookSourceType.file -> io.legado.read.constant.BookType.text or io.legado.read.constant.BookType.webFile
        io.legado.read.constant.BookSourceType.image -> io.legado.read.constant.BookType.image
        io.legado.read.constant.BookSourceType.audio -> io.legado.read.constant.BookType.audio
        else -> io.legado.read.constant.BookType.text
    }
}

fun Book.sync(oldBook: Book) {
    val curBook = io.legado.read.data.appDb.bookDao.getBook(oldBook.bookUrl)!!
    durChapterTime = curBook.durChapterTime
    durChapterIndex = curBook.durChapterIndex
    durChapterPos = curBook.durChapterPos
    durChapterTitle = curBook.durChapterTitle
    canUpdate = curBook.canUpdate
}

fun Book.isSameNameAuthor(other: Any?): Boolean {
    if (other is BaseBook) {
        return name == other.name && author == other.author
    }
    return false
}