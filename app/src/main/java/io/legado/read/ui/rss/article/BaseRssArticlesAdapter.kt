package io.legado.read.ui.rss.article

import android.content.Context
import androidx.viewbinding.ViewBinding
import io.legado.read.base.adapter.RecyclerAdapter
import io.legado.read.data.entities.RssArticle


abstract class BaseRssArticlesAdapter<VB : ViewBinding>(context: Context, val callBack: CallBack) :
    RecyclerAdapter<RssArticle, VB>(context) {

    interface CallBack {
        val isGridLayout: Boolean
        fun readRss(rssArticle: RssArticle)
    }
}