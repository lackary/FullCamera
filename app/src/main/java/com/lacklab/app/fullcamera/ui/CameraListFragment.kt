package com.lacklab.app.fullcamera.ui

import android.content.Context
import android.hardware.camera2.CameraManager
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.navigation.Navigation
import com.lacklab.app.fullcamera.R
import com.lacklab.app.fullcamera.data.CameraDevice2Info
import com.lacklab.app.fullcamera.databinding.FragmentCameraListBinding
import com.lacklab.app.fullcamera.ui.adapter.GenericAdapter
import com.lacklab.app.fullcamera.ui.base.BaseFragment

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
        setCameraIdsAdapter(binding, vm)
    }

    private fun setCameraIdsAdapter(binding: FragmentCameraListBinding, vm: CameraListViewModel) {
        val cameras = vm.camera2DeviceInfoList.value as List<CameraDevice2Info>
        with(binding) {
            cameraRecycleView.adapter =
                GenericAdapter(cameras, android.R.layout.simple_list_item_1) { view, item, _ ->
                    view.findViewById<TextView>(android.R.id.text1).text =
                        getString(
                            R.string.camera_title,
                            "${item.logicalCameraId} ${if(item.isMultipleCamera) item.physicalCameraIds else ""}"
                        )
                    view.setOnClickListener {
                        Navigation.findNavController(requireActivity(), R.id.nav_host_main)
                            .navigate(CameraListFragmentDirections
                                .actionCameraListFragmentToCameraFragment(item, false))
                    }
                }
        }
    }

}