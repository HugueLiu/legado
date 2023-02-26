package io.legado.read.ui.dict

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.google.android.material.tabs.TabLayout
import io.legado.read.R
import io.legado.read.base.BaseDialogFragment
import io.legado.read.data.entities.DictRule
import io.legado.read.databinding.DialogDictBinding
import io.legado.read.lib.theme.accentColor
import io.legado.read.lib.theme.backgroundColor
import io.legado.read.utils.setHtml
import io.legado.read.utils.setLayout
import io.legado.read.utils.toastOnUi
import io.legado.read.utils.viewbindingdelegate.viewBinding

/**
 * 词典
 */
class DictDialog() : BaseDialogFragment(R.layout.dialog_dict) {

    constructor(word: String) : this() {
        arguments = Bundle().apply {
            putString("word", word)
        }
    }

    private val viewModel by viewModels<DictViewModel>()
    private val binding by viewBinding(DialogDictBinding::bind)

    private var word: String? = null

    override fun onStart() {
        super.onStart()
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.tvDict.movementMethod = LinkMovementMethod()
        word = arguments?.getString("word")
        if (word.isNullOrEmpty()) {
            toastOnUi(R.string.cannot_empty)
            dismiss()
            return
        }
        binding.tabLayout.setBackgroundColor(backgroundColor)
        binding.tabLayout.setSelectedTabIndicatorColor(accentColor)
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab) {

            }

            override fun onTabUnselected(tab: TabLayout.Tab) {

            }

            override fun onTabSelected(tab: TabLayout.Tab) {
                val dictRule = tab.tag as DictRule
                binding.rotateLoading.visible()
                viewModel.dict(dictRule, word!!) {
                    binding.rotateLoading.inVisible()
                    binding.tvDict.setHtml(it)
                }
            }
        })
        viewModel.initData {
            it.forEach {
                binding.tabLayout.addTab(binding.tabLayout.newTab().apply {
                    text = it.name
                    tag = it
                })
            }
        }

    }

}