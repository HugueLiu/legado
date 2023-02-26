package io.legado.read.ui.book.read.page.provider

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Size
import androidx.collection.LruCache
import io.legado.read.R
import io.legado.read.constant.AppLog.putDebug
import io.legado.read.constant.PageAnim
import io.legado.read.data.entities.Book
import io.legado.read.data.entities.BookSource
import io.legado.read.exception.NoStackTraceException
import io.legado.read.help.book.BookHelp
import io.legado.read.help.book.isEpub
import io.legado.read.help.book.isPdf
import io.legado.read.help.config.AppConfig
import io.legado.read.help.coroutine.Coroutine
import io.legado.read.model.ReadBook
import io.legado.read.model.localBook.EpubFile
import io.legado.read.model.localBook.PdfFile
import io.legado.read.utils.BitmapUtils
import io.legado.read.utils.FileUtils
import io.legado.read.utils.SvgUtils
import io.legado.read.utils.toastOnUi
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.withContext
import splitties.init.appCtx
import java.io.File
import java.io.FileOutputStream

object ImageProvider {

    private val errorBitmap: Bitmap by lazy {
        BitmapFactory.decodeResource(appCtx.resources, R.drawable.image_loading_error)
    }

    /**
     * 缓存bitmap LruCache实现
     * filePath bitmap
     */
    private const val M = 1024 * 1024
    val cacheSize get() = AppConfig.bitmapCacheSize * M
    var triggerRecycled = false
    val bitmapLruCache = object : LruCache<String, Bitmap>(cacheSize) {

        override fun sizeOf(filePath: String, bitmap: Bitmap): Int {
            return bitmap.byteCount
        }

        override fun entryRemoved(
            evicted: Boolean,
            filePath: String,
            oldBitmap: Bitmap,
            newBitmap: Bitmap?
        ) {
            //错误图片不能释放,占位用,防止一直重复获取图片
            if (oldBitmap != errorBitmap) {
                oldBitmap.recycle()
                triggerRecycled = true
                //putDebug("ImageProvider: trigger bitmap recycle. URI: $filePath")
                //putDebug("ImageProvider : cacheUsage ${size()}bytes / ${maxSize()}bytes")
            }
        }

    }

    private fun getNotRecycled(key: String): Bitmap? {
        val bitmap = bitmapLruCache.get(key) ?: return null
        if (bitmap.isRecycled) {
            bitmapLruCache.remove(key)
            return null
        }
        return bitmap
    }

    /**
     *缓存网络图片和epub图片
     */
    suspend fun cacheImage(
        book: Book,
        src: String,
        bookSource: BookSource?
    ): File {
        return withContext(IO) {
            val vFile = BookHelp.getImage(book, src)
            if (!vFile.exists()) {
                if (book.isEpub) {
                    EpubFile.getImage(book, src)?.use { input ->
                        val newFile = FileUtils.createFileIfNotExist(vFile.absolutePath)
                        @Suppress("BlockingMethodInNonBlockingContext")
                        FileOutputStream(newFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                } else if (book.isPdf) {
                    PdfFile.getImage(book, src)?.use { input ->
                        val newFile = FileUtils.createFileIfNotExist(vFile.absolutePath)
                        @Suppress("BlockingMethodInNonBlockingContext")
                        FileOutputStream(newFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                } else {
                    BookHelp.saveImage(bookSource, book, src)
                }
            }
            return@withContext vFile
        }
    }

    /**
     *获取图片宽度高度信息
     */
    suspend fun getImageSize(
        book: Book,
        src: String,
        bookSource: BookSource?
    ): Size {
        val file = cacheImage(book, src, bookSource)
        //svg size
        val size = SvgUtils.getSize(file.absolutePath)
        if (size != null) return size
        val op = BitmapFactory.Options()
        // inJustDecodeBounds如果设置为true,仅仅返回图片实际的宽和高,宽和高是赋值给opts.outWidth,opts.outHeight;
        op.inJustDecodeBounds = true
        BitmapFactory.decodeFile(file.absolutePath, op)
        if (op.outWidth < 1 && op.outHeight < 1) {
            putDebug("ImageProvider: $src Unsupported image type")
            //file.delete() 重复下载
            return Size(errorBitmap.width, errorBitmap.height)
        }
        return Size(op.outWidth, op.outHeight)
    }

    /**
     *获取bitmap 使用LruCache缓存
     */
    fun getImage(
        book: Book,
        src: String,
        width: Int,
        height: Int? = null,
        block: (() -> Unit)? = null
    ): Bitmap? {
        //src为空白时 可能被净化替换掉了 或者规则失效
        if (book.getUseReplaceRule() && src.isBlank()) {
            book.setUseReplaceRule(false)
            appCtx.toastOnUi(R.string.error_image_url_empty)
        }
        val vFile = BookHelp.getImage(book, src)
        if (!vFile.exists()) return errorBitmap
        //epub文件提供图片链接是相对链接，同时阅读多个epub文件，缓存命中错误
        //bitmapLruCache的key同一改成缓存文件的路径
        val cacheBitmap = getNotRecycled(vFile.absolutePath)
        if (cacheBitmap != null) return cacheBitmap
        if (height != null && AppConfig.asyncLoadImage && ReadBook.pageAnim() == PageAnim.scrollPageAnim) {
            Coroutine.async {
                val bitmap = BitmapUtils.decodeBitmap(vFile.absolutePath, width, height)
                    ?: SvgUtils.createBitmap(vFile.absolutePath, width, height)
                    ?: throw NoStackTraceException(appCtx.getString(R.string.error_decode_bitmap))
                withContext(Main) {
                    bitmapLruCache.put(vFile.absolutePath, bitmap)
                }
            }.onError {
                //错误图片占位,防止重复获取
                bitmapLruCache.put(vFile.absolutePath, errorBitmap)
            }.onFinally {
                block?.invoke()
            }
            return null
        }
        @Suppress("BlockingMethodInNonBlockingContext")
        return kotlin.runCatching {
            val bitmap = BitmapUtils.decodeBitmap(vFile.absolutePath, width, height)
                ?: SvgUtils.createBitmap(vFile.absolutePath, width, height)
                ?: throw NoStackTraceException(appCtx.getString(R.string.error_decode_bitmap))
            bitmapLruCache.put(vFile.absolutePath, bitmap)
            bitmap
        }.onFailure {
            //错误图片占位,防止重复获取
            bitmapLruCache.put(vFile.absolutePath, errorBitmap)
        }.getOrDefault(errorBitmap)
    }

    fun isImageAlive(book: Book, src: String): Boolean {
        val vFile = BookHelp.getImage(book, src)
        if (!vFile.exists()) return true // 使用 errorBitmap
        val cacheBitmap = bitmapLruCache.get(vFile.absolutePath)
        return cacheBitmap != null
    }

    fun isTriggerRecycled(): Boolean {
        val tmp = triggerRecycled
        triggerRecycled = false
        return tmp
    }

}