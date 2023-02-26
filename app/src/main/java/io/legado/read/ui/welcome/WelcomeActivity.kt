package io.legado.read.ui.welcome

import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import androidx.core.view.postDelayed
import io.legado.read.base.BaseActivity
import io.legado.read.constant.PreferKey
import io.legado.read.constant.Theme
import io.legado.read.databinding.ActivityWelcomeBinding
import io.legado.read.help.config.ThemeConfig
import io.legado.read.lib.theme.accentColor
import io.legado.read.lib.theme.backgroundColor
import io.legado.read.ui.book.read.ReadBookActivity
import io.legado.read.ui.main.MainActivity
import io.legado.read.utils.*
import io.legado.read.utils.viewbindingdelegate.viewBinding
import io.legado.read.utils.*

open class WelcomeActivity : BaseActivity<ActivityWelcomeBinding>() {

    override val binding by viewBinding(ActivityWelcomeBinding::inflate)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        binding.ivBook.setColorFilter(accentColor)
        binding.vwTitleLine.setBackgroundColor(accentColor)
        // 避免从桌面启动程序后，会重新实例化入口类的activity
        if (intent.flags and Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT != 0) {
            finish()
        } else {
            binding.root.postDelayed(600) { startMainActivity() }
        }
    }

    override fun setupSystemBar() {
        fullScreen()
        setStatusBarColorAuto(backgroundColor, true, fullScreen)
        upNavigationBarColor()
    }

    override fun upBackgroundImage() {
        if (getPrefBoolean(PreferKey.customWelcome)) {
            kotlin.runCatching {
                when (ThemeConfig.getTheme()) {
                    Theme.Dark -> getPrefString(PreferKey.welcomeImageDark)?.let { path ->
                        val size = windowManager.windowSize
                        BitmapUtils.decodeBitmap(path, size.widthPixels, size.heightPixels).let {
                            binding.tvLegado.visible(getPrefBoolean(PreferKey.welcomeShowTextDark))
                            binding.ivBook.visible(getPrefBoolean(PreferKey.welcomeShowIconDark))
                            binding.tvGzh.visible(getPrefBoolean(PreferKey.welcomeShowTextDark))
                            window.decorView.background = BitmapDrawable(resources, it)
                            return
                        }
                    }
                    else -> getPrefString(PreferKey.welcomeImage)?.let { path ->
                        val size = windowManager.windowSize
                        BitmapUtils.decodeBitmap(path, size.widthPixels, size.heightPixels).let {
                            binding.tvLegado.visible(getPrefBoolean(PreferKey.welcomeShowText))
                            binding.ivBook.visible(getPrefBoolean(PreferKey.welcomeShowIcon))
                            binding.tvGzh.visible(getPrefBoolean(PreferKey.welcomeShowText))
                            window.decorView.background = BitmapDrawable(resources, it)
                            return
                        }
                    }
                }
            }
        }
        super.upBackgroundImage()
    }

    private fun startMainActivity() {
        startActivity<MainActivity>()
        if (getPrefBoolean(PreferKey.defaultToRead)) {
            startActivity<ReadBookActivity>()
        }
        finish()
    }

}

class Launcher1 : WelcomeActivity()
class Launcher2 : WelcomeActivity()
class Launcher3 : WelcomeActivity()
class Launcher4 : WelcomeActivity()
class Launcher5 : WelcomeActivity()
class Launcher6 : WelcomeActivity()