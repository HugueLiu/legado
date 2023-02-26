package io.legado.read.ui.rss.favorites

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.read.base.BaseActivity
import io.legado.read.data.appDb
import io.legado.read.data.entities.RssStar
import io.legado.read.databinding.ActivityRssFavoritesBinding
import io.legado.read.lib.theme.accentColor
import io.legado.read.ui.rss.read.ReadRssActivity
import io.legado.read.ui.widget.recycler.VerticalDivider
import io.legado.read.utils.startActivity
import io.legado.read.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch


class RssFavoritesActivity : BaseActivity<ActivityRssFavoritesBinding>(),
    RssFavoritesAdapter.CallBack {

    override val binding by viewBinding(ActivityRssFavoritesBinding::inflate)
    private val adapter by lazy { RssFavoritesAdapter(this, this) }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initView()
        initData()
    }

    private fun initView() {
        binding.refreshLayout.setColorSchemeColors(accentColor)
        binding.recyclerView.let {
            it.layoutManager = LinearLayoutManager(this)
            it.addItemDecoration(VerticalDivider(this))
            it.adapter = adapter
        }
    }

    private fun initData() {
        launch {
            appDb.rssStarDao.liveAll().conflate().collect {
                adapter.setItems(it)
            }
        }
    }

    override fun readRss(rssStar: RssStar) {
        startActivity<ReadRssActivity> {
            putExtra("title", rssStar.title)
            putExtra("origin", rssStar.origin)
            putExtra("link", rssStar.link)
        }
    }
}