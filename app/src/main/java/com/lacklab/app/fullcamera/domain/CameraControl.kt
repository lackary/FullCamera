package com.lacklab.app.fullcamera.domain

import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureFailure
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Range
import android.util.Size
import android.view.Surface
import com.lacklab.app.fullcamera.data.CameraDevice2Info
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CameraControl {

    private lateinit var cameraDevice: CameraDevice
    private lateinit var captureSession: CameraCaptureSession
    private lateinit var captureRequestBuilder: CaptureRequest.Builder

    lateinit var previewResolution: Size

    private lateinit var previewImageReader: ImageReader
    private val previewImageReaderHandlerThread =
        HandlerThread("previewImageReaderHandlerThread").apply { start() }
    private val previewImageReaderHandler = Handler(previewImageReaderHandlerThread.looper)

    private val cameraHandlerThread: HandlerThread =
        HandlerThread("cameraHandlerThread").apply { start() }
    private val cameraHandler = Handler(cameraHandlerThread.looper)

    private var listener: CameraCaptureSession.CaptureCallback? = null

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
    private var isMultiPreview = false

    suspend fun initCamera(
        previewSurface: Surface,
        manager: CameraManager,
        cameraId: String,
        resolution: Size,
        cameraDevice2Info: CameraDevice2Info?,
        isMultiPreview: Boolean = false
    ) {
        this.isMultiPreview = isMultiPreview
        previewImageReader = ImageReader.newInstance(
            resolution.width, resolution.height, previewFormat, imageBufferSize
        )

        previewImageReader.setOnImageAvailableListener({ imageReader ->
            val image = imageReader.acquireNextImage()
            image.also {
                it.close()
            }
        }, previewImageReaderHandler)

        val targetSurfaces = mutableListOf<Surface>()
        targetSurfaces.add(previewSurface)
        targetSurfaces.add(previewImageReader.surface)
        cameraDevice = openCamera(manager, cameraId, cameraHandler)

        captureSession = createCameraSession(cameraDevice, targetSurfaces, cameraHandler)

        captureRequestBuilder= cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
            targetSurfaces.forEach { surface ->
                addTarget(surface)
            }
            set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range(30, 30))
            set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
        }
    }

    @Suppress("MissingPermission")
    private suspend fun openCamera(
        cameraManager: CameraManager,
        cameraId: String,
        handler: Handler
    ): CameraDevice = suspendCancellableCoroutine { cont ->
        cameraManager.openCamera(
            cameraId,
            Executors.newSingleThreadExecutor(),
            object : CameraDevice.StateCallback() {
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

        })
    }

    private suspend fun createCameraSession(
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
                    val msg = "Camera ${cameraDevice.id} configure failed"
                    val exception = RuntimeException(msg)
                    cont.resumeWithException(exception)
                }
            })
        cameraDevice.createCaptureSession(sessionConfiguration)
    }

    /**
     * surfacePairs: List<Pair<Surface?, Surface>>
     *     first: surfaceView.holder.surface
     *     second: imageReader.surface
     * only it can
     */
    private suspend fun createMultiCameraSession(
        cameraDevice: CameraDevice,
        surfacePairs: List<Pair<Surface?, Surface>>,
        isMutablePreview: Boolean,
        physicalIds: List<String>,
        executor: Executor = Executors.newSingleThreadScheduledExecutor()
    ): CameraCaptureSession = suspendCancellableCoroutine { cont ->
        val outputConfigs: MutableList<OutputConfiguration> = mutableListOf()
        surfacePairs.forEachIndexed { index, pair ->
            val output = if( index == 0) {
                pair.first?.let { OutputConfiguration(it) }
            } else {
                val surface = if (isMutablePreview) pair.second else pair.first
                surface?.let {
                    OutputConfiguration(it).apply {
                        setPhysicalCameraId(physicalIds[index-1])
                        enableSurfaceSharing()
                        addSurface(pair.second)
                    }
                }
            }
            if (output != null) outputConfigs.add(output)
        }
        val sessionConfiguration = SessionConfiguration(SessionConfiguration.SESSION_REGULAR,
            outputConfigs, executor, object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                    cont.resume(cameraCaptureSession)
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    val msg = "Camera ${cameraDevice.id} multiple configure failed"
                    val exception = RuntimeException(msg)
                    cont.resumeWithException(exception)
                }
            })
        cameraDevice.createCaptureSession(sessionConfiguration)
    }

    suspend fun startPreview(): Flow<String> = callbackFlow {
        listener = object : CameraCaptureSession.CaptureCallback() {
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
                trySend("fps: $measureFps")
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
        }
        captureSession.setRepeatingRequest(captureRequestBuilder.build(), listener, cameraHandler)
        awaitClose {
            Timber.d("callback flow close")
            listener = null
        }
    }

    fun takePhoto() {

    }

    fun setWhiteBalance() {

    }

    fun setExposureTime() {

    }

    fun setISO() {

    }

    fun close() {
        lastFrameNumber = 0L
        measureFps = 0L
        startTime = 0L
        try {
            captureSession.close()
        } catch (e: Exception) {
            Timber.e(e, "Error closing capture session")
        }

        try {
            previewImageReader.close()
        } catch (e: Exception) {
            Timber.e(e, "Error closing image reader")
        }

        try {
            cameraDevice.close()
        } catch (e: Exception) {
            Timber.e(e, "Error closing camera device")
        }

    }
    fun clean() {
        try {
            previewImageReaderHandlerThread.quitSafely()
            cameraHandlerThread.quitSafely()
        } catch (e: Exception) {
            Timber.e(e, "Error quitting handler threads")
        }
    }
}