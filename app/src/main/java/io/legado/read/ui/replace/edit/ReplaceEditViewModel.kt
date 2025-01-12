package io.legado.read.ui.replace.edit

import android.app.Application
import android.content.Intent
import io.legado.read.base.BaseViewModel
import io.legado.read.data.appDb
import io.legado.read.data.entities.ReplaceRule
import io.legado.read.exception.NoStackTraceException
import io.legado.read.utils.*
import kotlinx.coroutines.Dispatchers

class ReplaceEditViewModel(application: Application) : BaseViewModel(application) {

    var replaceRule: ReplaceRule? = null

    fun initData(intent: Intent, finally: (replaceRule: ReplaceRule) -> Unit) {
        execute {
            val id = intent.getLongExtra("id", -1)
            replaceRule = if (id > 0) {
                appDb.replaceRuleDao.findById(id)
            } else {
                val pattern = intent.getStringExtra("pattern") ?: ""
                val isRegex = intent.getBooleanExtra("isRegex", false)
                val scope = intent.getStringExtra("scope")
                ReplaceRule(
                    name = pattern,
                    pattern = pattern,
                    isRegex = isRegex,
                    scope = scope
                )
            }
        }.onFinally {
            replaceRule?.let {
                finally(it)
            }
        }
    }

    fun pasteRule(success: (ReplaceRule) -> Unit) {
        execute(context = Dispatchers.Main) {
            val text = context.getClipText()
            if (text.isNullOrBlank()) {
                throw NoStackTraceException("剪贴板为空")
            }
            GSON.fromJsonObject<ReplaceRule>(text).getOrNull()
                ?: throw NoStackTraceException("格式不对")
        }.onSuccess {
            success.invoke(it)
        }.onError {
            context.toastOnUi(it.localizedMessage ?: "Error")
            it.printOnDebug()
        }
    }

    fun save(replaceRule: ReplaceRule, success: () -> Unit) {
        execute {
            if (replaceRule.order == Int.MIN_VALUE) {
                replaceRule.order = appDb.replaceRuleDao.maxOrder + 1
            }
            appDb.replaceRuleDao.insert(replaceRule)
        }.onSuccess {
            success()
        }
    }

}
