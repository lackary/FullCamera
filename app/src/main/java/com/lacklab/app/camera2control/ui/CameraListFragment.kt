package com.lacklab.app.camera2control.ui

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lacklab.app.camera2control.R
import com.lacklab.app.camera2control.data.CameraDevice2Info
import com.lacklab.app.camera2control.databinding.FragmentCameraListBinding
import com.lacklab.app.camera2control.ui.adapter.GenericAdapter
import com.lacklab.app.camera2control.ui.base.BaseFragment
import timber.log.Timber

class CameraListFragment : BaseFragment<FragmentCameraListBinding, CameraListViewModel>() {

    private val cameraListViewModel: CameraListViewModel by viewModels()

    override fun getVB(
        inflater: LayoutInflater,
        container: ViewGroup?,
        attachToParent: Boolean?
    ): FragmentCameraListBinding =
        FragmentCameraListBinding.inflate(inflater, container, false)

    override fun getVM(): CameraListViewModel =  cameraListViewModel

    override fun init() {
        val cameraManager = requireContext().getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraListViewModel.getCamera2DeviceInfo(cameraManager)
    }

    override fun clear() {

    }

    override fun bindVM(binding: FragmentCameraListBinding, vm: CameraListViewModel) {
        with(binding) {
            val cameras = vm.camera2DeviceInfoList.value as List<CameraDevice2Info>
            cameraRecycleView.adapter =
                GenericAdapter(cameras, android.R.layout.simple_list_item_1) { view, item, _ ->
                    view.findViewById<TextView>(android.R.id.text1).text =
                        getString(
                            R.string.camera_title,
                            "${item.logicalCameraId} ${if(item.isMultipleCamera) item.physicalCameraIds else ""}"
                        )
                    view.setOnClickListener {

                    }
            }
        }
    }


}