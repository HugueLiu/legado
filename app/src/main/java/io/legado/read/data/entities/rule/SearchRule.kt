package io.legado.read.data.entities.rule

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 搜索结果处理规则
 */
@Parcelize
data class SearchRule(
    /**校验关键字**/
    var checkKeyWord: String? = null,
    override var bookList: String? = null,
    override var name: String? = null,
    override var author: String? = null,
    override var intro: String? = null,
    override var kind: String? = null,
    override var lastChapter: String? = null,
    override var updateTime: String? = null,
    override var bookUrl: String? = null,
    override var coverUrl: String? = null,
    override var wordCount: String? = null
) : BookListRule, Parcelable