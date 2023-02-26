package io.legado.read.ui.association

import android.app.Application
import androidx.lifecycle.MutableLiveData
import io.legado.read.R
import io.legado.read.base.BaseViewModel
import io.legado.read.constant.AppConst
import io.legado.read.data.appDb
import io.legado.read.data.entities.HttpTTS
import io.legado.read.exception.NoStackTraceException
import io.legado.read.help.http.newCallResponseBody
import io.legado.read.help.http.okHttpClient
import io.legado.read.help.http.text
import io.legado.read.utils.isAbsUrl
import io.legado.read.utils.isJsonArray
import io.legado.read.utils.isJsonObject
import io.legado.read.utils.printOnDebug

class ImportHttpTtsViewModel(app: Application) : BaseViewModel(app) {

    val errorLiveData = MutableLiveData<String>()
    val successLiveData = MutableLiveData<Int>()

    val allSources = arrayListOf<HttpTTS>()
    val checkSources = arrayListOf<HttpTTS?>()
    val selectStatus = arrayListOf<Boolean>()

    val isSelectAll: Boolean
        get() {
            selectStatus.forEach {
                if (!it) {
                    return false
                }
            }
            return true
        }

    val selectCount: Int
        get() {
            var count = 0
            selectStatus.forEach {
                if (it) {
                    count++
                }
            }
            return count
        }

    fun importSelect(finally: () -> Unit) {
        execute {
            val selectSource = arrayListOf<HttpTTS>()
            selectStatus.forEachIndexed { index, b ->
                if (b) {
                    selectSource.add(allSources[index])
                }
            }
            appDb.httpTTSDao.insert(*selectSource.toTypedArray())
        }.onFinally {
            finally.invoke()
        }
    }

    fun importSource(text: String) {
        execute {
            importSourceAwait(text.trim())
        }.onError {
            it.printOnDebug()
            errorLiveData.postValue(it.localizedMessage ?: "")
        }.onSuccess {
            comparisonSource()
        }
    }

    private suspend fun importSourceAwait(text: String) {
        when {
            text.isJsonObject() -> {
                HttpTTS.fromJson(text).getOrThrow().let {
                    allSources.add(it)
                }
            }
            text.isJsonArray() -> HttpTTS.fromJsonArray(text).getOrThrow().let { items ->
                allSources.addAll(items)
            }
            text.isAbsUrl() -> {
                importSourceUrl(text)
            }
            else -> throw NoStackTraceException(context.getString(R.string.wrong_format))
        }
    }

    private suspend fun importSourceUrl(url: String) {
        okHttpClient.newCallResponseBody {
            if (url.endsWith("#requestWithoutUA")) {
                url(url.substringBeforeLast("#requestWithoutUA"))
                header(AppConst.UA_NAME, "null")
            } else {
                url(url)
            }
        }.text().let {
            importSourceAwait(it)
        }
    }

    private fun comparisonSource() {
        execute {
            allSources.forEach {
                val source = appDb.httpTTSDao.get(it.id)
                checkSources.add(source)
                selectStatus.add(source == null || source.lastUpdateTime < it.lastUpdateTime)
            }
            successLiveData.postValue(allSources.size)
        }
    }

}