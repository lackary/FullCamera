package com.lacklab.app.fullcamera.ui

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.YuvImage
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCaptureSession.CaptureCallback
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Range
import android.util.Size
import android.view.LayoutInflater
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.lacklab.app.fullcamera.databinding.FragmentCameraBinding
import com.lacklab.app.fullcamera.ui.base.BaseFragment
import com.lacklab.app.fullcamera.util.cam.ability.AeMode
import com.lacklab.app.fullcamera.util.cam.ability.AwbMode
import com.lacklab.app.fullcamera.util.cam.ability.CameraCapability
import com.lacklab.app.fullcamera.util.cam.ability.CameraFormat
import com.lacklab.app.fullcamera.util.getPreviewOutputSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CameraFragment : BaseFragment<FragmentCameraBinding, CameraViewModel>() {
    private val cameraViewModel: CameraViewModel by viewModels()

    private val args: CameraFragmentArgs by navArgs()

    private val cameraManager: CameraManager by lazy {
        val context = requireContext()
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    private val characteristics: CameraCharacteristics by lazy {
        cameraManager.getCameraCharacteristics(args.cameraItem.logicalCameraId)
    }

    // One second
    private val oneSecond = 1000000000
    private val imageBufferSize = 3

    private lateinit var previewImageReader: ImageReader

    private val previewImageReaderThread = HandlerThread("previewImageReaderThread").apply { start() }
    private val previewImageReaderHandler = Handler(previewImageReaderThread.looper)

    private val cameraThread = HandlerThread("cameraThread").apply { start() }
    private val cameraHandler = Handler(cameraThread.looper)

    private var streamConfigurationMap: StreamConfigurationMap? = null
    private var previewResolution = Size(0,0)

    private lateinit var captureSession: CameraCaptureSession
    private lateinit var captureRequestBuilder: CaptureRequest.Builder

    // depended on surfaceView size
    private lateinit var previewSize: Size
    private var previewFormat = ImageFormat.YUV_420_888
    private lateinit var outputFormats: List<Int>
    private val outputFormatTextList = mutableListOf<String>()
    private val supportTextList = mutableListOf<String>()
    private val aeModeTextList = mutableListOf<String>()
    private val awbModeTextList= mutableListOf<String>()
    private var resolutionList = listOf<Size>()

    override fun getVB(
        inflater: LayoutInflater,
        container: ViewGroup?,
        attachToParent: Boolean?
    ): FragmentCameraBinding =
        FragmentCameraBinding.inflate(inflater, container, false)

    override fun getVM(): CameraViewModel = cameraViewModel

    override fun init() {
        getCameraAbility(characteristics)
        streamConfigurationMap?.let {
            resolutionList = getResolutionList(it, previewFormat)
            previewResolution = getMaxResolution(resolutionList)
        }
    }

    override fun clear() {

    }

    override fun bindVM(binding: FragmentCameraBinding, vm: CameraViewModel) {
        with(binding) {
            surfaceViewPreview.holder.addCallback(object : SurfaceHolder.Callback {
                override fun surfaceCreated(p0: SurfaceHolder) {
                    Timber.d("SurfaceView size: ${surfaceViewPreview.width} x ${surfaceViewPreview.height}")
                    surfaceViewPreview.setAspectRatio(previewResolution.width, previewResolution.height)
                    view?.post{ initCamera(surfaceViewPreview)}
                }

                override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {

                }

                override fun surfaceDestroyed(p0: SurfaceHolder) {

                }
            })
        }
    }

    private fun getCameraAbility(characteristics: CameraCharacteristics) {
        streamConfigurationMap =
            characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                ?.also {
                    outputFormats = it.outputFormats.sorted().onEach { value ->
                        val format = CameraFormat.formInt(value)?.name?: it.toString()
                        outputFormatTextList.add(format)
                    }
                }
        Timber.i("Camera outputFormats name: $outputFormatTextList")
        Timber.d("outputFormats: $outputFormats")

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

    fun initCamera(surfaceView: SurfaceView) = lifecycleScope.launch(Dispatchers.Main) {
        val previewImageSize = previewResolution

        previewImageReader = ImageReader.newInstance(
            previewImageSize.width,
            previewResolution.height,
            previewFormat,
            imageBufferSize
        )

        previewImageReader.setOnImageAvailableListener({ imageReader ->
            val image = imageReader.acquireNextImage()
            image.also {
                it.close()
            }
        }, previewImageReaderHandler)

        val targets = listOf(surfaceView.holder.surface, previewImageReader.surface)
        val cameraDevice = openCamera(cameraManager, args.cameraItem.logicalCameraId, cameraHandler)
        captureSession = createCaptureSession(cameraDevice, targets, cameraHandler)

        captureRequestBuilder= cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
            addTarget(surfaceView.holder.surface)
            addTarget(previewImageReader.surface)
            set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range(30, 30))
            set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
        }

        captureSession.setRepeatingRequest(captureRequestBuilder.build(),
            object : CaptureCallback() {}, cameraHandler)
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
                requireActivity().finish()
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
}