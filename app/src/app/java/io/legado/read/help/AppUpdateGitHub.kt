package io.legado.read.help

import androidx.annotation.Keep
import io.legado.read.constant.AppConst
import io.legado.read.exception.NoStackTraceException
import io.legado.read.help.coroutine.Coroutine
import io.legado.read.help.http.newCallStrResponse
import io.legado.read.help.http.okHttpClient
import io.legado.read.utils.jsonPath
import io.legado.read.utils.readString
import kotlinx.coroutines.CoroutineScope

@Keep
@Suppress("unused")
object AppUpdateGitHub: AppUpdate.AppUpdateInterface {

    override fun check(
        scope: CoroutineScope,
    ): Coroutine<AppUpdate.UpdateInfo> {
        return Coroutine.async(scope) {
            val lastReleaseUrl = "https://api.github.com/repos/gedoor/legado/releases/latest"
            val body = okHttpClient.newCallStrResponse {
                url(lastReleaseUrl)
            }.body
            if (body.isNullOrBlank()) {
                throw NoStackTraceException("获取新版本出错")
            }
            val rootDoc = jsonPath.parse(body)
            val tagName = rootDoc.readString("$.tag_name")
                ?: throw NoStackTraceException("获取新版本出错")
            if (tagName > AppConst.appInfo.versionName) {
                val updateBody = rootDoc.readString("$.body")
                    ?: throw NoStackTraceException("获取新版本出错")
                val downloadUrl = rootDoc.readString("$.assets[0].browser_download_url")
                    ?: throw NoStackTraceException("获取新版本出错")
                val fileName = rootDoc.readString("$.assets[0].name")
                    ?: throw NoStackTraceException("获取新版本出错")
                return@async AppUpdate.UpdateInfo(tagName, updateBody, downloadUrl, fileName)
            } else {
                throw NoStackTraceException("已是最新版本")
            }
        }.timeout(10000)
    }


}