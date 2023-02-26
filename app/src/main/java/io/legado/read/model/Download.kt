package io.legado.read.model

import android.content.Context
import io.legado.read.constant.IntentAction
import io.legado.read.service.DownloadService
import io.legado.read.utils.startService

object Download {


    fun start(context: Context, url: String, fileName: String) {
        context.startService<DownloadService> {
            action = IntentAction.start
            putExtra("url", url)
            putExtra("fileName", fileName)
        }
    }

}