package io.legado.read.ui.about


import android.os.Bundle
import io.legado.read.R
import io.legado.read.base.BaseActivity
import io.legado.read.databinding.ActivityDonateBinding
import io.legado.read.utils.viewbindingdelegate.viewBinding

/**
 * Created by GKF on 2018/1/13.
 * 捐赠页面
 */

class DonateActivity : BaseActivity<ActivityDonateBinding>() {

    override val binding by viewBinding(ActivityDonateBinding::inflate)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        val fTag = "donateFragment"
        var donateFragment = supportFragmentManager.findFragmentByTag(fTag)
        if (donateFragment == null) donateFragment = DonateFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fl_fragment, donateFragment, fTag)
            .commit()
    }

}
