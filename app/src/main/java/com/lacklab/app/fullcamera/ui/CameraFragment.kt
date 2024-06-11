package com.lacklab.app.fullcamera.ui

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCaptureSession.CaptureCallback
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
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.lacklab.app.fullcamera.databinding.FragmentCameraBinding
import com.lacklab.app.fullcamera.ui.base.BaseFragment
import com.lacklab.app.fullcamera.util.cam.ability.AeMode
import com.lacklab.app.fullcamera.util.cam.ability.AwbMode
import com.lacklab.app.fullcamera.util.cam.ability.CameraCapability
import com.lacklab.app.fullcamera.util.cam.ability.CameraFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CameraFragment : BaseFragment<FragmentCameraBinding, CameraViewModel>() {
    private val cameraViewModel: CameraViewModel by viewModels()

    private val args: CameraFragmentArgs by navArgs()

    private val cameraManager: CameraManager by lazy {
        Timber.d("lazy cameraManager")
        val context = requireContext()
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    private lateinit var cameraDevice: CameraDevice

    private val characteristics: CameraCharacteristics by lazy {
        Timber.d("lazy characteristics")
        cameraManager.getCameraCharacteristics(args.cameraItem.logicalCameraId)
    }

    // One second
    private val oneSecond = 1000000000.0
    private val imageBufferSize = 3
    // calculate fps
    private var lastFrameNumber = 0L
    private var startTime = 0L
    private var measureFps = 0L

    private lateinit var previewImageReader: ImageReader

    private val previewImageReaderThread = HandlerThread("previewImageReaderThread").apply { start() }
    private val previewImageReaderHandler = Handler(previewImageReaderThread.looper)

    private val cameraThread = HandlerThread("cameraThread").apply { start() }
    private val cameraHandler = Handler(cameraThread.looper)

    override fun getVB(
        inflater: LayoutInflater,
        container: ViewGroup?,
        attachToParent: Boolean?
    ): FragmentCameraBinding =
        FragmentCameraBinding.inflate(inflater, container, false)

    override fun getVM(): CameraViewModel = cameraViewModel

    override fun init() {
        cameraViewModel.getCameraAbility(characteristics)
    }

    override fun clear() {
        Timber.d("clear")
        cameraViewModel.close()
    }

    override fun bindVM(binding: FragmentCameraBinding, vm: CameraViewModel) {
        with(binding) {
            surfaceViewPreview.holder.addCallback(object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    Timber.d("SurfaceView size: ${surfaceViewPreview.width} x ${surfaceViewPreview.height}")
                    vm.previewResolution.let {
                        surfaceViewPreview.setAspectRatio(it.width, it.height)
                    }
                    lifecycleScope.launch(Dispatchers.Main) {
                        vm.initCamera(
                            surfaceViewPreview.holder.surface,
                            cameraManager,
                            args.cameraItem.logicalCameraId
                        )
                        vm.startPreview()
                    }
                    view?.post{

                    }
                }

                override fun surfaceChanged(holder: SurfaceHolder, p1: Int, p2: Int, p3: Int) {

                }

                override fun surfaceDestroyed(holder: SurfaceHolder) {

                }
            })
            with(vm) {
                cameraInfo.observe(viewLifecycleOwner, Observer {
                    textViewPreviewInfo.text = it
                })
            }
        }
    }

}