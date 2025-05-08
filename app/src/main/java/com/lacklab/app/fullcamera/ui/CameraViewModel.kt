package com.lacklab.app.fullcamera.ui

import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.StreamConfigurationMap
import android.util.Size
import android.view.Surface
import androidx.lifecycle.SavedStateHandle
import com.lacklab.app.fullcamera.data.CameraDevice2Info
import com.lacklab.app.fullcamera.domain.CameraControl
import com.lacklab.app.fullcamera.ui.base.BaseViewModel
import com.lacklab.app.fullcamera.util.cam.ability.AeMode
import com.lacklab.app.fullcamera.util.cam.ability.AwbMode
import com.lacklab.app.fullcamera.util.cam.ability.CameraCapability
import com.lacklab.app.fullcamera.util.cam.ability.CameraFormat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber

class CameraViewModel(
    private val savedStateHandle: SavedStateHandle,
) : BaseViewModel() {
    companion object {
        private const val KEY_CAMERA_ITEM = "camera_item"
    }

    private val cameraControl = CameraControl()

    private val cameraDevice2Info = savedStateHandle.get<CameraDevice2Info>(KEY_CAMERA_ITEM)

    lateinit var previewResolution: Size

    private var previewFormat = ImageFormat.YUV_420_888
    private lateinit var outputFormats: List<Int>
    private val outputFormatTextList = mutableListOf<String>()
    private val supportTextList = mutableListOf<String>()
    private val aeModeTextList = mutableListOf<String>()
    private val awbModeTextList= mutableListOf<String>()
    private var resolutionList = listOf<Size>()

    private val _cameraInfo = MutableStateFlow<String>("fps")
    val cameraInfo: StateFlow<String> = _cameraInfo
//    var cameraInfo = MutableLiveData<String>()


    fun getCameraAbility(characteristics: CameraCharacteristics) {
        Timber.d("get streamConfigurationMap")
        val streamConfigurationMap =
            characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                ?.also {
                    outputFormats = it.outputFormats.sorted().onEach { value ->
                        val format = CameraFormat.formInt(value)?.name?: it.toString()
                        outputFormatTextList.add(format)
                    }
                }
        Timber.i("Camera outputFormats name: $outputFormatTextList")
        Timber.d("outputFormats: $outputFormats")

        streamConfigurationMap?.let {
            resolutionList = getResolutionList(it, previewFormat)
            previewResolution = getMaxResolution(resolutionList)
        }

        // Supporting Ability
        val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
            ?.sorted()?.onEach { value ->
                val support = CameraCapability.formInt(value)?.name?: value.toString()
                supportTextList.add(support)
            }
        Timber.i("Camera supports name: $supportTextList")
        Timber.d("capabilities: $capabilities")

        //AE capability
        val aeModes = characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_MODES)
            ?.sorted()?.onEach { value ->
                val mode = AeMode.formInt(value)?.name?: value.toString()
                aeModeTextList.add(mode)
            }
        Timber.i("Camera AE modes name: $aeModeTextList")
        Timber.d("AE modes: $aeModes")

        val awbModes = characteristics.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES)
            ?.sorted()?.onEach { value ->
                val mode = AwbMode.formInt(value)?.name?: value.toString()
                awbModeTextList.add(mode)
            }
        Timber.i("Camera AWB modes name: $awbModeTextList")
        Timber.d("AWB modes: $awbModes")
    }

    private fun getResolutionList(
        streamConfigurationMap: StreamConfigurationMap,
        format: Int
    ): List<Size> {
        val resolutions = streamConfigurationMap.getOutputSizes(format)
        Timber.i("format: ${CameraFormat.formInt(format)?.name?: format.toString()} support resolution below")
        resolutions.onEach { size ->
            Timber.i("size: $size")
        }
        return resolutions.toList()
    }

    private fun getMaxResolution(resolutions: List<Size>): Size {
        var maxSize: Size = Size(0, 0)
        var maxPixel: Int = 0
        resolutions.forEach {
            val pixel = it.height * it.width
            if (pixel > maxPixel) {
                maxPixel = pixel
                maxSize = it
            }
        }
        return maxSize
    }
    suspend fun initCamera(previewSurface: Surface, manager: CameraManager, cameraId: String) =
        cameraControl.initCamera(previewSurface, manager, cameraId, previewResolution, cameraDevice2Info)

    suspend fun startPreview() {
        cameraControl.startPreview().collect {
            _cameraInfo.value = it
//            cameraInfo.postValue(it) #MutableLiveData
        }
    }

    fun close() {
        cameraControl.close()
    }

    fun clean() {
        cameraControl.clean()
    }
}