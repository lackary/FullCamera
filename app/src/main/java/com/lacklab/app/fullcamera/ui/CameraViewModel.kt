package com.lacklab.app.fullcamera.ui

import android.app.Application
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureFailure
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Range
import android.util.Size
import android.view.Surface
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.lacklab.app.fullcamera.data.CameraDevice2Info
import com.lacklab.app.fullcamera.domain.CameraControl
import com.lacklab.app.fullcamera.ui.base.BaseViewModel
import com.lacklab.app.fullcamera.util.cam.ability.AeMode
import com.lacklab.app.fullcamera.util.cam.ability.AwbMode
import com.lacklab.app.fullcamera.util.cam.ability.CameraCapability
import com.lacklab.app.fullcamera.util.cam.ability.CameraFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CameraViewModel(
    savedStateHandle: SavedStateHandle,
) : BaseViewModel() {

    private val cameraDevice2Info = savedStateHandle.get<CameraDevice2Info>("camera_item")

    private lateinit var cameraDevice: CameraDevice
    private lateinit var captureSession: CameraCaptureSession
    private lateinit var captureRequestBuilder: CaptureRequest.Builder

    lateinit var previewResolution: Size

    private lateinit var previewImageReader: ImageReader
    private val previewImageReaderHandlerThread =
        HandlerThread("previewImageReaderHandlerThread").apply { start() }
    private val previewImageReaderHandler = Handler(previewImageReaderHandlerThread.looper)

    private val cameraHandlerThread:HandlerThread =
        HandlerThread("cameraHandlerThread").apply { start() }
    private val cameraHandler = Handler(cameraHandlerThread.looper)

    private var previewFormat = ImageFormat.YUV_420_888
    private lateinit var outputFormats: List<Int>
    private val outputFormatTextList = mutableListOf<String>()
    private val supportTextList = mutableListOf<String>()
    private val aeModeTextList = mutableListOf<String>()
    private val awbModeTextList= mutableListOf<String>()
    private var resolutionList = listOf<Size>()

    private val imageBufferSize = 1
    private val oneSecond = 1000000000 // Unit is nanosecond
    private var startTime = 0L
    private var lastFrameNumber = 0L
    private var measureFps = 0L
    val cameraInfo = MutableLiveData<String>()

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
    suspend fun initCamera(previewSurface: Surface, manager: CameraManager, cameraId: String) {
        previewImageReader = ImageReader.newInstance(
            previewResolution.width, previewResolution.height, previewFormat, imageBufferSize
        )

        previewImageReader.setOnImageAvailableListener({ imageReader ->
            val image = imageReader.acquireNextImage()
            image.also {
                it.close()
            }
        }, previewImageReaderHandler)

        val targets = listOf(previewSurface, previewImageReader.surface)
        cameraDevice = openCamera(manager, cameraId, cameraHandler)
        captureSession = createCaptureSession(cameraDevice, targets, cameraHandler)

        captureRequestBuilder= cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
            addTarget(previewSurface)
            addTarget(previewImageReader.surface)
            set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range(30, 30))
            set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
        }
    }

    @Suppress("MissingPermission")
    private suspend fun openCamera(
        manager: CameraManager,
        cameraId: String,
        handler: Handler
    ): CameraDevice = suspendCancellableCoroutine { cont ->
        manager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(device: CameraDevice) {
                cont.resume(device)
            }

            override fun onDisconnected(device: CameraDevice) {
                device.close()
                cont.cancel(Throwable("camera disconnect"))
            }

            override fun onError(cameraDevice: CameraDevice, error: Int) {
                val message = when(error) {
                    ERROR_CAMERA_IN_USE -> "Camera in use"
                    ERROR_MAX_CAMERAS_IN_USE -> "Maximum cameras in use"
                    ERROR_CAMERA_DISABLED -> "Device disable"
                    ERROR_CAMERA_DEVICE -> "fatal (camera device)"
                    ERROR_CAMERA_SERVICE -> "fatal (camera service)"
                    else -> "unknown"
                }
                val exception = RuntimeException("Camera ${cameraDevice.id} error: ($error) $message")
                if (cont.isActive) cont.resumeWithException(exception)
            }

            override fun onClosed(camera: CameraDevice) {
                super.onClosed(camera)
            }

        }, handler)
    }

    private suspend fun createCaptureSession(
        cameraDevice: CameraDevice,
        targets: List<Surface>,
        handler: Handler
    ): CameraCaptureSession = suspendCancellableCoroutine { cont ->
        val outputConfiguration = targets.map {
            OutputConfiguration(it)
        }

        val sessionConfiguration = SessionConfiguration(
            SessionConfiguration.SESSION_REGULAR,
            outputConfiguration,
            Executors.newSingleThreadExecutor(),
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                    cont.resume(cameraCaptureSession)
                }

                override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                   val exception = RuntimeException("Camera ${cameraDevice.id}")
                    cont.resumeWithException(exception)
                }
            })
        cameraDevice.createCaptureSession(sessionConfiguration)
    }

    fun startPreview() {
        captureSession.setRepeatingRequest(captureRequestBuilder.build(),
            object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureStarted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    timestamp: Long,
                    frameNumber: Long
                ) {
                    Timber.d("timestamp: $timestamp, frameNumber: ${frameNumber+1}")
                    super.onCaptureStarted(session, request, timestamp, frameNumber)
                }

                override fun onCaptureProgressed(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    partialResult: CaptureResult
                ) {
                    super.onCaptureProgressed(session, request, partialResult)
                }

                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    val frameNumber = result.frameNumber
                    val sensorTimestamp = result.get(CaptureResult.SENSOR_TIMESTAMP)

                    sensorTimestamp?.let {
                        Timber.d("timestamp: $it, frameNumber: ${result.frameNumber+1}")
                        if(frameNumber == 0L) {
                            startTime = it
                        } else {
                            val dif = it.minus(startTime)
                            val sec = dif.div(oneSecond)
                            Timber.d("second: $sec")
                            if (sec >= 1.0) {
                                startTime = it
                                measureFps = frameNumber - lastFrameNumber
                                lastFrameNumber = frameNumber
                                Timber.d("fps: $measureFps")
                            }
                        }
                    }
                    cameraInfo.postValue("fps: $measureFps")
                    super.onCaptureCompleted(session, request, result)
                }

                override fun onCaptureFailed(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    failure: CaptureFailure
                ) {
                    super.onCaptureFailed(session, request, failure)
                }

                override fun onCaptureBufferLost(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    target: Surface,
                    frameNumber: Long
                ) {
                    super.onCaptureBufferLost(session, request, target, frameNumber)
                }
            },
            cameraHandler)
    }

    fun close() {
        cameraDevice.close()
    }
}