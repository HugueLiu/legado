package io.legado.read.ui.association

import android.app.Application
import androidx.lifecycle.MutableLiveData
import io.legado.read.base.BaseViewModel
import io.legado.read.constant.AppConst
import io.legado.read.constant.AppPattern
import io.legado.read.data.appDb
import io.legado.read.data.entities.ReplaceRule
import io.legado.read.exception.NoStackTraceException
import io.legado.read.help.ReplaceAnalyzer
import io.legado.read.help.http.newCallResponseBody
import io.legado.read.help.http.okHttpClient
import io.legado.read.help.http.text
import io.legado.read.utils.isAbsUrl
import io.legado.read.utils.isJsonArray
import io.legado.read.utils.isJsonObject
import io.legado.read.utils.splitNotBlank

class ImportReplaceRuleViewModel(app: Application) : BaseViewModel(app) {
    var isAddGroup = false
    var groupName: String? = null
    val errorLiveData = MutableLiveData<String>()
    val successLiveData = MutableLiveData<Int>()

    val allRules = arrayListOf<ReplaceRule>()
    val checkRules = arrayListOf<ReplaceRule?>()
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
            val group = groupName?.trim()
            val selectRules = arrayListOf<ReplaceRule>()
            selectStatus.forEachIndexed { index, b ->
                if (b) {
                    val rule = allRules[index]
                    if (!group.isNullOrEmpty()) {
                        if (isAddGroup) {
                            val groups = linkedSetOf<String>()
                            rule.group?.splitNotBlank(AppPattern.splitGroupRegex)?.let {
                                groups.addAll(it)
                            }
                            groups.add(group)
                            rule.group = groups.joinToString(",")
                        } else {
                            rule.group = group
                        }
                    }
                    selectRules.add(rule)
                }
            }
            appDb.replaceRuleDao.insert(*selectRules.toTypedArray())
        }.onFinally {
            finally.invoke()
        }
    }

    fun import(text: String) {
        execute {
            importAwait(text.trim())
        }.onError {
            errorLiveData.postValue(it.localizedMessage ?: "ERROR")
        }.onSuccess {
            comparisonSource()
        }
    }

    private suspend fun importAwait(text: String) {
        when {
            text.isAbsUrl() -> importUrl(text)
            text.isJsonArray() -> {
                val rules = ReplaceAnalyzer.jsonToReplaceRules(text).getOrThrow()
                allRules.addAll(rules)
            }
            text.isJsonObject() -> {
                val rule = ReplaceAnalyzer.jsonToReplaceRule(text).getOrThrow()
                allRules.add(rule)
            }
            else -> throw NoStackTraceException("格式不对")
        }
    }

    private suspend fun importUrl(url: String) {
        okHttpClient.newCallResponseBody {
            if (url.endsWith("#requestWithoutUA")) {
                url(url.substringBeforeLast("#requestWithoutUA"))
                header(AppConst.UA_NAME, "null")
            } else {
                url(url)
            }
        }.text("utf-8").let {
            importAwait(it)
        }
    }

    private fun comparisonSource() {
        execute {
            allRules.forEach {
                val rule = appDb.replaceRuleDao.findById(it.id)
                checkRules.add(rule)
                selectStatus.add(rule == null)
            }
        }.onSuccess {
            successLiveData.postValue(allRules.size)
        }
    }
}