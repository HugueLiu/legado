package io.legado.read.base

import androidx.lifecycle.ViewModel

abstract class VMBaseFragment<VM : ViewModel>(layoutID: Int) : BaseFragment(layoutID) {

    protected abstract val viewModel: VM

}