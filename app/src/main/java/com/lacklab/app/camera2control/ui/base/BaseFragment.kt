package com.lacklab.app.camera2control.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

abstract class BaseFragment<VB: ViewBinding, VM: BaseViewModel> : Fragment()  {

    private lateinit var _binding: VB
    private val binding: VB
        get() = _binding

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
        _binding = getVB(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindVM(binding, viewModel)
    }

    override fun onDestroy() {
        super.onDestroy()
        clear()
    }

    abstract fun getVB(inflater: LayoutInflater,
                       container: ViewGroup?,
                       attachToParent: Boolean?): VB

    abstract fun getVM(): VM

    abstract fun init()

    abstract fun bindVM(binding: VB, vm: VM)

    abstract fun clear()
}