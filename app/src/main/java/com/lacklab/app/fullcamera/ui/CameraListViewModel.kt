package com.lacklab.app.fullcamera.ui

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.lacklab.app.fullcamera.data.CameraDevice2Info
import com.lacklab.app.fullcamera.data.FloatRangeData
import com.lacklab.app.fullcamera.ui.base.BaseViewModel
import com.lacklab.app.fullcamera.util.CovertHelper.toIntRange
import timber.log.Timber

class CameraListViewModel(
    private val savedStateHandle: SavedStateHandle,
) : BaseViewModel() {
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
            val cameraDevice2Info = CameraDevice2Info(
                logicalCameraId = it.first,
                physicalCameraIds = it.second.physicalCameraIds,
                isMultipleCamera = it.second.get(CameraCharacteristics
                    .REQUEST_AVAILABLE_CAPABILITIES)!!
                    .contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA),
                fpsRange = it.second.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
                    ?.maxByOrNull { it.upper }?.toIntRange(),
                exposureCompensationRange = it.second.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)
                    ?.toIntRange(),
                exposureCompensationStep = it.second.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP)
                    ?.toFloat(),
                isoRange = it.second.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
                    ?.toIntRange(),
                zoom = it.second.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE)
                    ?.let { FloatRangeData(it.lower, it.upper) },
            )
            Timber.d("$cameraDevice2Info")

            cameras.add(cameraDevice2Info)
        }

        _camera2DeviceInfoList.value = cameras
    }
}