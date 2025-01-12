@file:Suppress("DEPRECATION")

package io.legado.read.ui.book.toc

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import com.google.android.material.tabs.TabLayout
import io.legado.read.R
import io.legado.read.base.VMBaseActivity
import io.legado.read.databinding.ActivityChapterListBinding
import io.legado.read.help.book.isLocalTxt
import io.legado.read.help.config.AppConfig
import io.legado.read.lib.theme.accentColor
import io.legado.read.lib.theme.primaryTextColor
import io.legado.read.model.ReadBook
import io.legado.read.ui.about.AppLogDialog
import io.legado.read.ui.book.toc.rule.TxtTocRuleDialog
import io.legado.read.ui.widget.dialog.WaitDialog
import io.legado.read.utils.applyTint
import io.legado.read.utils.gone
import io.legado.read.utils.showDialogFragment
import io.legado.read.utils.viewbindingdelegate.viewBinding
import io.legado.read.utils.visible

/**
 * 目录
 */
class TocActivity : VMBaseActivity<ActivityChapterListBinding, TocViewModel>(),
    TxtTocRuleDialog.CallBack {

    override val binding by viewBinding(ActivityChapterListBinding::inflate)
    override val viewModel by viewModels<TocViewModel>()

    private lateinit var tabLayout: TabLayout
    private var menu: Menu? = null
    private var searchView: SearchView? = null
    private val waitDialog by lazy { WaitDialog(this) }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        tabLayout = binding.titleBar.findViewById(R.id.tab_layout)
        tabLayout.isTabIndicatorFullWidth = false
        tabLayout.setSelectedTabIndicatorColor(accentColor)
        binding.viewPager.adapter = TabFragmentPageAdapter()
        tabLayout.setupWithViewPager(binding.viewPager)
        tabLayout.tabGravity = TabLayout.GRAVITY_CENTER
        viewModel.bookData.observe(this) {
            menu?.setGroupVisible(R.id.menu_group_text, it.isLocalTxt)
        }
        intent.getStringExtra("bookUrl")?.let {
            viewModel.initBook(it)
        }
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.book_toc, menu)
        this.menu = menu
        viewModel.bookData.value?.let {
            menu.setGroupVisible(R.id.menu_group_text, it.isLocalTxt)
        }
        val search = menu.findItem(R.id.menu_search)
        searchView = (search.actionView as SearchView).apply {
            applyTint(primaryTextColor)
            maxWidth = resources.displayMetrics.widthPixels
            onActionViewCollapsed()
            setOnCloseListener {
                tabLayout.visible()
                false
            }
            setOnSearchClickListener { tabLayout.gone() }
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String): Boolean {
                    viewModel.searchKey = query
                    return false
                }

                override fun onQueryTextChange(newText: String): Boolean {
                    viewModel.searchKey = newText
                    if (tabLayout.selectedTabPosition == 1) {
                        viewModel.startBookmarkSearch(newText)
                    } else {
                        viewModel.startChapterListSearch(newText)
                    }
                    return false
                }
            })
            setOnQueryTextFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    searchView?.isIconified = true
                }
            }
        }
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onMenuOpened(featureId: Int, menu: Menu): Boolean {
        menu.findItem(R.id.menu_use_replace)?.isChecked = AppConfig.tocUiUseReplace
        return super.onMenuOpened(featureId, menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_toc_regex -> showDialogFragment(
                TxtTocRuleDialog(viewModel.bookData.value?.tocUrl)
            )
            R.id.menu_reverse_toc -> viewModel.reverseToc {
                viewModel.chapterListCallBack?.upChapterList(searchView?.query?.toString())
                setResult(RESULT_OK, Intent().apply {
                    putExtra("index", it.durChapterIndex)
                    putExtra("chapterPos", 0)
                })
            }
            R.id.menu_use_replace -> {
                AppConfig.tocUiUseReplace = !item.isChecked
                viewModel.chapterListCallBack?.clearDisplayTitle()
                viewModel.chapterListCallBack?.upChapterList(searchView?.query?.toString())
            }
            R.id.menu_log -> showDialogFragment<AppLogDialog>()
        }
        return super.onCompatOptionsItemSelected(item)
    }

    override fun onTocRegexDialogResult(tocRegex: String) {
        viewModel.bookData.value?.let { book ->
            book.tocUrl = tocRegex
            waitDialog.show()
            viewModel.upBookTocRule(book) {
                waitDialog.dismiss()
                ReadBook.book?.let { readBook ->
                    if (readBook == book) {
                        ReadBook.book = book
                        ReadBook.chapterSize = book.totalChapterNum
                        ReadBook.upMsg(null)
                        ReadBook.loadContent(resetPageOffset = true)
                    }
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private inner class TabFragmentPageAdapter :
        FragmentPagerAdapter(supportFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        override fun getItem(position: Int): Fragment {
            return when (position) {
                1 -> BookmarkFragment()
                else -> ChapterListFragment()
            }
        }

        override fun getCount(): Int {
            return 2
        }

        override fun getPageTitle(position: Int): CharSequence {
            return when (position) {
                1 -> getString(R.string.bookmark)
                else -> getString(R.string.chapter_list)
            }
        }

    }

}