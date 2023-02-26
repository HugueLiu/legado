package io.legado.read.ui.book.bookmark

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import io.legado.read.R
import io.legado.read.base.VMBaseActivity
import io.legado.read.data.entities.Bookmark
import io.legado.read.databinding.ActivityAllBookmarkBinding
import io.legado.read.ui.document.HandleFileContract
import io.legado.read.utils.launch
import io.legado.read.utils.showDialogFragment
import io.legado.read.utils.viewbindingdelegate.viewBinding

class AllBookmarkActivity : VMBaseActivity<ActivityAllBookmarkBinding, AllBookmarkViewModel>(),
    BookmarkAdapter.Callback,
    BookmarkDialog.Callback {

    override val viewModel by viewModels<AllBookmarkViewModel>()
    override val binding by viewBinding(ActivityAllBookmarkBinding::inflate)
    private val adapter by lazy {
        BookmarkAdapter(this, this)
    }
    private val exportDir = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { uri ->
            viewModel.saveToFile(uri)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initView()
        viewModel.initData {
            adapter.setItems(it)
        }
    }

    private fun initView() {
        binding.recyclerView.addItemDecoration(BookmarkDecoration(adapter))
        binding.recyclerView.adapter = adapter
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.bookmark, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_export -> exportDir.launch()
        }
        return super.onCompatOptionsItemSelected(item)
    }

    override fun onItemClick(bookmark: Bookmark, position: Int) {
        showDialogFragment(BookmarkDialog(bookmark, position))
    }

    override fun upBookmark(pos: Int, bookmark: Bookmark) {
        adapter.setItem(pos, bookmark)
    }

    override fun deleteBookmark(pos: Int) {
        adapter.getItem(pos)?.let {
            viewModel.deleteBookmark(it)
        }
        adapter.removeItem(pos)
    }

}