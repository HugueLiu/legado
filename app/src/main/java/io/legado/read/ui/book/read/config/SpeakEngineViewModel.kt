package io.legado.read.ui.book.read.config

import android.app.Application
import android.net.Uri
import android.speech.tts.TextToSpeech
import io.legado.read.base.BaseViewModel
import io.legado.read.data.appDb
import io.legado.read.data.entities.HttpTTS
import io.legado.read.exception.NoStackTraceException
import io.legado.read.help.DefaultData
import io.legado.read.help.http.newCallResponseBody
import io.legado.read.help.http.okHttpClient
import io.legado.read.help.http.text
import io.legado.read.utils.isJsonArray
import io.legado.read.utils.isJsonObject
import io.legado.read.utils.readText
import io.legado.read.utils.toastOnUi

class SpeakEngineViewModel(application: Application) : BaseViewModel(application) {

    val sysEngines: List<TextToSpeech.EngineInfo> by lazy {
        val tts = TextToSpeech(context, null)
        val engines = tts.engines
        tts.shutdown()
        engines
    }

    fun importDefault() {
        execute {
            DefaultData.importDefaultHttpTTS()
        }
    }

    fun importOnLine(url: String) {
        execute {
            okHttpClient.newCallResponseBody {
                url(url)
            }.text("utf-8").let { json ->
                import(json)
            }
        }.onSuccess {
            context.toastOnUi("导入成功")
        }.onError {
            context.toastOnUi("导入失败")
        }
    }

    fun importLocal(uri: Uri) {
        execute {
            import(uri.readText(context))
        }.onSuccess {
            context.toastOnUi("导入成功")
        }.onError {
            context.toastOnUi("导入失败\n${it.localizedMessage}")
        }
    }

    fun import(text: String) {
        when {
            text.isJsonArray() -> {
                HttpTTS.fromJsonArray(text).getOrThrow().let {
                    appDb.httpTTSDao.insert(*it.toTypedArray())
                }
            }
            text.isJsonObject() -> {
                HttpTTS.fromJson(text).getOrThrow().let {
                    appDb.httpTTSDao.insert(it)
                }
            }
            else -> {
                throw NoStackTraceException("格式不对")
            }
        }
    }

}