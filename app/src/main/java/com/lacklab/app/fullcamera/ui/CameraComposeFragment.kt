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
                        Timber.d("SurfaceView size: ${this@apply.width} x ${this@apply.height}")
                        vm.previewResolution.let {
                            this@apply.setAspectRatio(it.width, it.height)
                        }
                        lifecycleScope.launch(Dispatchers.Main) {
                            vm.initCamera(
                                holder.surface,
                                cameraManager,
                                args.cameraItem.logicalCameraId
                            )
                            vm.startPreview()
                        }

                        view?.post {
//                            vm.updateCameraInfo("相機已就緒")
                        }
                    }

                    override fun surfaceChanged(
                        holder: SurfaceHolder,
                        p1: Int,
                        p2: Int,
                        p3: Int
                    ) {
//                        vm.updateCameraInfo("分辨率: ${p2}x${p3}")
                    }

                    override fun surfaceDestroyed(holder: SurfaceHolder) {
//                        vm.updateCameraInfo("相機已關閉")
                    }
                })
            }
        }

        // Handle lifecycle events
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_PAUSE -> {
                        vm.close()
//                        vm.updateCameraInfo("相機已暫停")
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