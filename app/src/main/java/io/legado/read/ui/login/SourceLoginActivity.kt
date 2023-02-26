package io.legado.read.ui.login

import android.os.Bundle
import androidx.activity.viewModels
import io.legado.read.R
import io.legado.read.base.VMBaseActivity
import io.legado.read.data.entities.BaseSource
import io.legado.read.databinding.ActivitySourceLoginBinding
import io.legado.read.utils.showDialogFragment
import io.legado.read.utils.viewbindingdelegate.viewBinding


class SourceLoginActivity : VMBaseActivity<ActivitySourceLoginBinding, SourceLoginViewModel>() {

    override val binding by viewBinding(ActivitySourceLoginBinding::inflate)
    override val viewModel by viewModels<SourceLoginViewModel>()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        viewModel.initData(intent) { source ->
            initView(source)
        }
    }

    private fun initView(source: BaseSource) {
        if (source.loginUi.isNullOrEmpty()) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fl_fragment, WebViewLoginFragment(), "webViewLogin")
                .commit()
        } else {
            showDialogFragment<SourceLoginDialog>()
        }
    }

}