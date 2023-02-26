package io.legado.read.ui.main.explore

import android.app.Application
import io.legado.read.base.BaseViewModel
import io.legado.read.data.appDb
import io.legado.read.data.entities.BookSource
import io.legado.read.help.config.SourceConfig

class ExploreViewModel(application: Application) : BaseViewModel(application) {

    fun topSource(bookSource: BookSource) {
        execute {
            val minXh = appDb.bookSourceDao.minOrder
            bookSource.customOrder = minXh - 1
            appDb.bookSourceDao.insert(bookSource)
        }
    }

    fun deleteSource(source: BookSource) {
        execute {
            appDb.bookSourceDao.delete(source)
            SourceConfig.removeSource(source.bookSourceUrl)
        }
    }

}