package io.legado.read.ui.main.my

import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.preference.Preference
import io.legado.read.R
import io.legado.read.base.BaseFragment
import io.legado.read.constant.AppConst
import io.legado.read.constant.EventBus
import io.legado.read.constant.PreferKey
import io.legado.read.databinding.FragmentMyConfigBinding
import io.legado.read.help.config.ThemeConfig
import io.legado.read.lib.dialogs.selector
import io.legado.read.lib.prefs.NameListPreference
import io.legado.read.lib.prefs.PreferenceCategory
import io.legado.read.lib.prefs.SwitchPreference
import io.legado.read.lib.prefs.fragment.PreferenceFragment
import io.legado.read.lib.theme.primaryColor
import io.legado.read.service.WebService
import io.legado.read.ui.about.AboutActivity
import io.legado.read.ui.about.DonateActivity
import io.legado.read.ui.about.ReadRecordActivity
import io.legado.read.ui.book.bookmark.AllBookmarkActivity
import io.legado.read.ui.book.source.manage.BookSourceActivity
import io.legado.read.ui.book.toc.rule.TxtTocRuleActivity
import io.legado.read.ui.config.ConfigActivity
import io.legado.read.ui.config.ConfigTag
import io.legado.read.ui.dict.rule.DictRuleActivity
import io.legado.read.ui.replace.ReplaceRuleActivity
import io.legado.read.ui.widget.dialog.TextDialog
import io.legado.read.utils.*
import io.legado.read.utils.viewbindingdelegate.viewBinding
import io.legado.read.utils.*

class MyFragment : BaseFragment(R.layout.fragment_my_config) {

    private val binding by viewBinding(FragmentMyConfigBinding::bind)

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        setSupportToolbar(binding.titleBar.toolbar)
        val fragmentTag = "prefFragment"
        var preferenceFragment = childFragmentManager.findFragmentByTag(fragmentTag)
        if (preferenceFragment == null) preferenceFragment = MyPreferenceFragment()
        childFragmentManager.beginTransaction()
            .replace(R.id.pre_fragment, preferenceFragment, fragmentTag).commit()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu) {
        menuInflater.inflate(R.menu.main_my, menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem) {
        when (item.itemId) {
            R.id.menu_help -> {
                val text = String(requireContext().assets.open("help/appHelp.md").readBytes())
                showDialogFragment(TextDialog(getString(R.string.help), text, TextDialog.Mode.MD))
            }
        }
    }

    /**
     * 配置
     */
    class MyPreferenceFragment : PreferenceFragment(),
        SharedPreferences.OnSharedPreferenceChangeListener {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            putPrefBoolean(PreferKey.webService, WebService.isRun)
            addPreferencesFromResource(R.xml.pref_main)
            if (AppConst.isPlayChannel) {
                findPreference<PreferenceCategory>("aboutCategory")
                    ?.removePreferenceRecursively("donate")
            }
            findPreference<SwitchPreference>("webService")?.onLongClick {
                if (!WebService.isRun) {
                    return@onLongClick false
                }
                context?.selector(arrayListOf("复制地址", "浏览器打开")) { _, i ->
                    when (i) {
                        0 -> context?.sendToClip(it.summary.toString())
                        1 -> context?.openUrl(it.summary.toString())
                    }
                }
                true
            }
            observeEventSticky<String>(EventBus.WEB_SERVICE) {
                findPreference<SwitchPreference>(PreferKey.webService)?.let {
                    it.isChecked = WebService.isRun
                    it.summary = if (WebService.isRun) {
                        WebService.hostAddress
                    } else {
                        getString(R.string.web_service_desc)
                    }
                }
            }
            findPreference<NameListPreference>(PreferKey.themeMode)?.let {
                it.setOnPreferenceChangeListener { _, _ ->
                    view?.post { ThemeConfig.applyDayNight(requireContext()) }
                    true
                }
            }
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            listView.setEdgeEffectColor(primaryColor)
        }

        override fun onResume() {
            super.onResume()
            preferenceManager.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onPause() {
            preferenceManager.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
            super.onPause()
        }

        override fun onSharedPreferenceChanged(
            sharedPreferences: SharedPreferences?,
            key: String?
        ) {
            when (key) {
                PreferKey.webService -> {
                    if (requireContext().getPrefBoolean("webService")) {
                        WebService.start(requireContext())
                    } else {
                        WebService.stop(requireContext())
                    }
                }
                "recordLog" -> LogUtils.upLevel()
            }
        }

        override fun onPreferenceTreeClick(preference: Preference): Boolean {
            when (preference.key) {
                "bookSourceManage" -> startActivity<BookSourceActivity>()
                "replaceManage" -> startActivity<ReplaceRuleActivity>()
                "dictRuleManage" -> startActivity<DictRuleActivity>()
                "txtTocRuleManage" -> startActivity<TxtTocRuleActivity>()
                "bookmark" -> startActivity<AllBookmarkActivity>()
                "setting" -> startActivity<ConfigActivity> {
                    putExtra("configTag", ConfigTag.OTHER_CONFIG)
                }
                "web_dav_setting" -> startActivity<ConfigActivity> {
                    putExtra("configTag", ConfigTag.BACKUP_CONFIG)
                }
                "theme_setting" -> startActivity<ConfigActivity> {
                    putExtra("configTag", ConfigTag.THEME_CONFIG)
                }
                "readRecord" -> startActivity<ReadRecordActivity>()
                "donate" -> startActivity<DonateActivity>()
                "about" -> startActivity<AboutActivity>()
            }
            return super.onPreferenceTreeClick(preference)
        }


    }
}