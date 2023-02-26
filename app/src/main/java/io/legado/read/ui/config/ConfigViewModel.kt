package io.legado.read.ui.config

import android.app.Application
import android.content.Context
import io.legado.read.R
import io.legado.read.base.BaseViewModel
import io.legado.read.help.AppWebDav
import io.legado.read.help.book.BookHelp
import io.legado.read.utils.FileUtils
import io.legado.read.utils.toastOnUi

class ConfigViewModel(application: Application) : BaseViewModel(application) {

    fun upWebDavConfig() {
        execute {
            AppWebDav.upConfig()
        }
    }

    fun clearCache() {
        execute {
            BookHelp.clearCache()
            FileUtils.delete(context.cacheDir.absolutePath)
        }.onSuccess {
            context.toastOnUi(R.string.clear_cache_success)
        }
    }

    fun clearWebViewData() {
        execute {
            FileUtils.delete(context.getDir("webview", Context.MODE_PRIVATE))
        }.onSuccess {
            context.toastOnUi(R.string.success)
        }
    }


}
