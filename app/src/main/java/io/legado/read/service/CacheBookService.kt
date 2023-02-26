package io.legado.read.service

import android.content.Intent
import androidx.core.app.NotificationCompat
import io.legado.read.R
import io.legado.read.base.BaseService
import io.legado.read.constant.AppConst
import io.legado.read.constant.AppLog
import io.legado.read.constant.EventBus
import io.legado.read.constant.IntentAction
import io.legado.read.data.appDb
import io.legado.read.help.config.AppConfig
import io.legado.read.model.CacheBook
import io.legado.read.model.webBook.WebBook
import io.legado.read.ui.book.cache.CacheActivity
import io.legado.read.utils.activityPendingIntent
import io.legado.read.utils.postEvent
import io.legado.read.utils.servicePendingIntent
import io.legado.read.utils.toastOnUi
import kotlinx.coroutines.*
import splitties.init.appCtx
import java.util.concurrent.Executors
import kotlin.math.min

class CacheBookService : BaseService() {

    companion object {
        var isRun = false
            private set
    }

    private val threadCount = AppConfig.threadCount
    private var cachePool =
        Executors.newFixedThreadPool(min(threadCount, AppConst.MAX_THREAD)).asCoroutineDispatcher()
    private var downloadJob: Job? = null
    private var notificationContent = appCtx.getString(R.string.service_starting)
    private val notificationBuilder by lazy {
        val builder = NotificationCompat.Builder(this, AppConst.channelIdDownload)
            .setSmallIcon(R.drawable.ic_download)
            .setOngoing(true)
            .setContentTitle(getString(R.string.offline_cache))
            .setContentIntent(activityPendingIntent<CacheActivity>("cacheActivity"))
        builder.addAction(
            R.drawable.ic_stop_black_24dp,
            getString(R.string.cancel),
            servicePendingIntent<CacheBookService>(IntentAction.stop)
        )
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
    }

    override fun onCreate() {
        super.onCreate()
        isRun = true
        launch {
            while (isActive) {
                delay(1000)
                notificationContent = CacheBook.downloadSummary
                upNotification()
                postEvent(EventBus.UP_DOWNLOAD, "")
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let { action ->
            when (action) {
                IntentAction.start -> addDownloadData(
                    intent.getStringExtra("bookUrl"),
                    intent.getIntExtra("start", 0),
                    intent.getIntExtra("end", 0)
                )
                IntentAction.remove -> removeDownload(intent.getStringExtra("bookUrl"))
                IntentAction.stop -> stopSelf()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        isRun = false
        cachePool.close()
        CacheBook.cacheBookMap.forEach { it.value.stop() }
        CacheBook.cacheBookMap.clear()
        super.onDestroy()
        postEvent(EventBus.UP_DOWNLOAD, "")
    }

    private fun addDownloadData(bookUrl: String?, start: Int, end: Int) {
        bookUrl ?: return
        execute {
            val cacheBook = CacheBook.getOrCreate(bookUrl) ?: return@execute
            val chapterCount = appDb.bookChapterDao.getChapterCount(bookUrl)
            if (chapterCount == 0) {
                WebBook.getChapterListAwait(cacheBook.bookSource, cacheBook.book)
                    .onFailure {
                        AppLog.put("缓存书籍没有目录且加载目录失败\n${it.localizedMessage}", it)
                        appCtx.toastOnUi("缓存书籍没有目录且加载目录失败\n${it.localizedMessage}")
                    }.getOrNull()?.let { toc ->
                        appDb.bookChapterDao.insert(*toc.toTypedArray())
                    }
            }
            val end2 = if (end == 0) {
                appDb.bookChapterDao.getChapterCount(bookUrl)
            } else {
                end
            }
            cacheBook.addDownload(start, end2)
            notificationContent = CacheBook.downloadSummary
            upNotification()
            if (downloadJob == null) {
                download()
            }
        }
    }

    private fun removeDownload(bookUrl: String?) {
        CacheBook.cacheBookMap[bookUrl]?.stop()
        postEvent(EventBus.UP_DOWNLOAD, "")
        if (downloadJob == null && CacheBook.isRun) {
            download()
            return
        }
        if (CacheBook.cacheBookMap.isEmpty()) {
            stopSelf()
        }
    }

    private fun download() {
        downloadJob?.cancel()
        downloadJob = launch(cachePool) {
            while (isActive) {
                if (!CacheBook.isRun) {
                    CacheBook.stop(this@CacheBookService)
                    return@launch
                }
                CacheBook.cacheBookMap.forEach {
                    val cacheBookModel = it.value
                    while (cacheBookModel.waitCount > 0) {
                        if (CacheBook.onDownloadCount < threadCount) {
                            cacheBookModel.download(this, cachePool)
                        } else {
                            delay(100)
                        }
                    }
                }
            }
        }
    }

    /**
     * 更新通知
     */
    override fun upNotification() {
        notificationBuilder.setContentText(notificationContent)
        val notification = notificationBuilder.build()
        startForeground(AppConst.notificationIdCache, notification)
    }

}