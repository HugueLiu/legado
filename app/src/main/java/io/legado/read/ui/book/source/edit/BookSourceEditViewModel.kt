package io.legado.read.ui.book.source.edit

import android.app.Application
import android.content.Intent
import io.legado.read.base.BaseViewModel
import io.legado.read.data.appDb
import io.legado.read.data.entities.BookSource
import io.legado.read.exception.NoStackTraceException
import io.legado.read.help.RuleComplete
import io.legado.read.help.config.SourceConfig
import io.legado.read.help.http.CookieStore
import io.legado.read.help.http.newCallStrResponse
import io.legado.read.help.http.okHttpClient
import io.legado.read.utils.*
import kotlinx.coroutines.Dispatchers


class BookSourceEditViewModel(application: Application) : BaseViewModel(application) {
    var autoComplete = false
    var bookSource: BookSource? = null

    fun initData(intent: Intent, onFinally: () -> Unit) {
        execute {
            val sourceUrl = intent.getStringExtra("sourceUrl")
            var source: BookSource? = null
            if (sourceUrl != null) {
                source = appDb.bookSourceDao.getBookSource(sourceUrl)
            }
            source?.let {
                bookSource = it
            }
        }.onFinally {
            onFinally()
        }
    }

    fun save(source: BookSource, success: (() -> Unit)? = null) {
        execute {
            bookSource?.let {
                appDb.bookSourceDao.delete(it)
                SourceConfig.removeSource(it.bookSourceUrl)
            }
            source.lastUpdateTime = System.currentTimeMillis()
            appDb.bookSourceDao.insert(source)
            bookSource = source
        }.onSuccess {
            success?.invoke()
        }.onError {
            context.toastOnUi(it.localizedMessage)
            it.printOnDebug()
        }
    }

    fun pasteSource(onSuccess: (source: BookSource) -> Unit) {
        execute(context = Dispatchers.Main) {
            val text = context.getClipText()
            if (text.isNullOrBlank()) {
                throw NoStackTraceException("剪贴板为空")
            } else {
                importSource(text, onSuccess)
            }
        }.onError {
            context.toastOnUi(it.localizedMessage ?: "Error")
            it.printOnDebug()
        }
    }

    fun importSource(text: String, finally: (source: BookSource) -> Unit) {
        execute {
            importSource(text)
        }.onSuccess {
            it?.let(finally) ?: context.toastOnUi("格式不对")
        }.onError {
            context.toastOnUi(it.localizedMessage ?: "Error")
        }
    }

    suspend fun importSource(text: String): BookSource? {
        return when {
            text.isAbsUrl() -> {
                val text1 = okHttpClient.newCallStrResponse { url(text) }.body
                text1?.let { importSource(text1) }
            }
            text.isJsonArray() -> {
                val items: List<Map<String, Any>> = jsonPath.parse(text).read("$")
                val jsonItem = jsonPath.parse(items[0])
                BookSource.fromJson(jsonItem.jsonString()).getOrThrow()
            }
            text.isJsonObject() -> {
                BookSource.fromJson(text).getOrThrow()
            }
            else -> throw NoStackTraceException("格式不对")
        }
    }

    fun clearCookie(url: String) {
        execute {
            CookieStore.removeCookie(url)
        }
    }

    fun ruleComplete(rule: String?, preRule: String? = null, type: Int = 1): String? {
        if (autoComplete) {
            return RuleComplete.autoComplete(rule, preRule, type)
        }
        return rule
    }

}