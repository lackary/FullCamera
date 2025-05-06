package com.lacklab.app.fullcamera.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment

abstract class BaseComposeFragment<VM: BaseViewModel> : Fragment() {
    private lateinit var _viewModel: VM
    private val viewModel: VM
        get() = _viewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _viewModel = getVM()
        init()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return ComposeView(requireContext()).apply {
            setContent {
                ComposeScreen(viewModel)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        bindVM(viewModel)
    }

    override fun onDestroy() {
        super.onDestroy()
        clear()
    }

    abstract fun getVM(): VM

    abstract fun init()

    abstract fun bindVM(vm: VM)

    @Composable
    abstract fun ComposeScreen(vm: VM)

    abstract fun clear()
}