package com.lacklab.app.camera2control.ui

import android.hardware.Camera
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.lacklab.app.camera2control.data.CameraDevice2Info
import com.lacklab.app.camera2control.ui.base.BaseViewModel

class CameraListViewModel : BaseViewModel() {
    private val _camera2DeviceInfoList = MutableLiveData<List<CameraDevice2Info>>()
    val camera2DeviceInfoList: LiveData<List<CameraDevice2Info>>
        get() = _camera2DeviceInfoList

    fun getCamera2DeviceInfo(cameraManager: CameraManager) {
        val cameraIds = cameraManager.cameraIdList
        val cameras = mutableListOf<CameraDevice2Info>()
        cameraIds.map {
            Pair(it, cameraManager.getCameraCharacteristics(it))
        }.forEach {
            val syncType = it.second.get(CameraCharacteristics.LOGICAL_MULTI_CAMERA_SENSOR_SYNC_TYPE)
            val hardwareLevel = it.second.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
            cameras.add(CameraDevice2Info(
                logicalCameraId = it.first,
                physicalCameraIds = it.second.physicalCameraIds,
                isMultipleCamera = it.second.get(CameraCharacteristics
                    .REQUEST_AVAILABLE_CAPABILITIES)!!
                    .contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA),
                exposureCompensationRange = it.second.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE),
                exposureCompensationStep = it.second.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP),
                isoRange = it.second.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE),
                zoom = it.second.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE),
            ))
        }
        _camera2DeviceInfoList.value = cameras
    }
}