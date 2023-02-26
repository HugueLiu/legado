package io.legado.read.base

import androidx.lifecycle.ViewModel
import androidx.viewbinding.ViewBinding
import io.legado.read.constant.Theme

abstract class VMBaseActivity<VB : ViewBinding, VM : ViewModel>(
    fullScreen: Boolean = true,
    theme: Theme = Theme.Auto,
    toolBarTheme: Theme = Theme.Auto,
    transparent: Boolean = false,
    imageBg: Boolean = true
) : BaseActivity<VB>(fullScreen, theme, toolBarTheme, transparent, imageBg) {

    protected abstract val viewModel: VM

}