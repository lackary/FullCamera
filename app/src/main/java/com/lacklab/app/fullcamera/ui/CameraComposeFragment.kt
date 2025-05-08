package com.lacklab.app.fullcamera.ui

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.view.SurfaceHolder
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.lacklab.app.fullcamera.util.AutoFitSurfaceView
import com.lacklab.app.fullcamera.ui.base.BaseComposeFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class CameraComposeFragment : BaseComposeFragment<CameraViewModel>() {
    private val cameraViewModel: CameraViewModel by viewModels()
    private val args: CameraFragmentArgs by navArgs()

    private val cameraManager: CameraManager by lazy {
        Timber.d("lazy cameraManager")
        val context = requireContext()
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    private val characteristics: CameraCharacteristics by lazy {
        Timber.d("lazy characteristics")
        cameraManager.getCameraCharacteristics(args.cameraItem.logicalCameraId)
    }

    override fun getVM(): CameraViewModel = cameraViewModel

    override fun init() {
        cameraViewModel.getCameraAbility(characteristics)
    }

    override fun bindVM(vm: CameraViewModel) {
        // No-op as we're using Compose
    }

    override fun clear() {
        Timber.d("clear")
        cameraViewModel.close()
    }

    @Composable
    override fun ComposeScreen(vm: CameraViewModel) {
        val context = LocalContext.current
        val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
        val cameraInfo = vm.cameraInfo.collectAsState()

        // Create and remember the surfaceView
        val surfaceView = remember {
            AutoFitSurfaceView(context).apply {
                holder.addCallback(object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        vm.setSurface(holder.surface)
                        Timber.d("SurfaceView size: ${this@apply.width} x ${this@apply.height}")
                        vm.previewResolution.let {
                            this@apply.setAspectRatio(it.width, it.height)
                        }
                    }

                    override fun surfaceChanged(
                        holder: SurfaceHolder,
                        p1: Int,
                        width: Int,
                        height: Int
                    ) {
                        Timber.d("分辨率: ${width}x${height}")
                    }

                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                        Timber.d("camera was closed")
                    }
                })
            }
        }

        // Handle lifecycle events
        DisposableEffect(lifecycleOwner) {
//            var cameraInitJob: Deferred<Unit>? = null
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_CREATE -> {
                        Timber.d("Compose Lifecycle on create ")
                    }
                    Lifecycle.Event.ON_START -> {
                        Timber.d("Compose Lifecycle on start ")
                    }
                    Lifecycle.Event.ON_RESUME -> {
                        Timber.d("Compose Lifecycle on resume ")
                        lifecycleScope.launch(Dispatchers.Main) {
                            vm.previewSurface.value?.let { surface ->
                                vm.initCamera(
                                    surface,
                                    cameraManager,
                                    args.cameraItem.logicalCameraId
                                )
                                Timber.d("Camera init completed, starting preview")
                                vm.startPreview()
                            }
                        }
                    }
                    Lifecycle.Event.ON_PAUSE -> {
                        Timber.d("Compose Lifecycle on pause ")
                        vm.close()
                    }
                    Lifecycle.Event.ON_STOP -> {
                        Timber.d("Compose Lifecycle on stop ")
                    }
                    Lifecycle.Event.ON_DESTROY -> {
                        Timber.d("Compose Lifecycle on destroy ")
                        vm.clean()
                    }
                    else -> {}
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            // Camera preview
            AndroidView(
                factory = { surfaceView },
                modifier = Modifier.fillMaxSize()
            )

            // Camera info overlay
            Text(
                text = cameraInfo.value,
                color = Color.Yellow,
                fontSize = 14.sp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            )
        }
    }
}