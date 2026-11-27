package io.nekohasekai.sagernet.ui

import io.nekohasekai.sagernet.ui.components.hapticClick

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.database.ProfileManager
import io.nekohasekai.sagernet.ktx.onMainDispatcher
import io.nekohasekai.sagernet.ktx.parseProxies
import io.nekohasekai.sagernet.ktx.readableMessage
import io.nekohasekai.sagernet.ktx.runOnDefaultDispatcher
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class ScannerActivity : ThemedActivity() {

    private val handled = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setComposeContent {
            var torchOn by remember { mutableStateOf(false) }
            var cameraRef by remember { mutableStateOf<Camera?>(null) }
            val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

            var hasCameraPermission by remember {
                mutableStateOf(
                    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                )
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted -> hasCameraPermission = granted }

            val galleryLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.GetContent()
            ) { uri ->
                if (uri != null) {
                    runOnDefaultDispatcher {
                        try {
                            @Suppress("DEPRECATION")
                            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri))
                                    .copy(android.graphics.Bitmap.Config.ARGB_8888, false)
                            } else {
                                MediaStore.Images.Media.getBitmap(contentResolver, uri)
                            }
                            val pixels = IntArray(bitmap.width * bitmap.height)
                            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                            val source = RGBLuminanceSource(bitmap.width, bitmap.height, pixels)
                            val decoded = MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(source))).text
                            importScannedText(decoded)
                        } catch (e: Exception) {
                            onMainDispatcher {
                                Toast.makeText(this@ScannerActivity, e.readableMessage, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }

            LaunchedEffect(Unit) {
                if (!hasCameraPermission) {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }

            DisposableEffect(Unit) {
                onDispose {
                    analysisExecutor.shutdown()
                }
            }

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(R.string.add_profile_methods_scan_qr_code), fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = hapticClick { finish() }) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            IconButton(onClick = hapticClick {
                                torchOn = !torchOn
                                cameraRef?.cameraControl?.enableTorch(torchOn)
                            }) {
                                Icon(
                                    imageVector = if (torchOn) Icons.Rounded.FlashOff else Icons.Rounded.FlashOn,
                                    contentDescription = "Flashlight",
                                )
                            }
                            IconButton(onClick = hapticClick { galleryLauncher.launch("image/*") }) {
                                Icon(Icons.Rounded.PhotoLibrary, contentDescription = "Import from Gallery")
                            }
                        },
                    )
                },
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    if (hasCameraPermission) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { ctx ->
                                val previewView = PreviewView(ctx)
                                val providerFuture = ProcessCameraProvider.getInstance(ctx)
                                providerFuture.addListener({
                                    val provider = providerFuture.get()
                                    val preview = Preview.Builder().build().also {
                                        it.surfaceProvider = previewView.surfaceProvider
                                    }
                                    val reader = MultiFormatReader()
                                    val analysis = ImageAnalysis.Builder()
                                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                        .build()
                                    analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                                        try {
                                            if (!handled.get()) {
                                                val buffer = imageProxy.planes[0].buffer
                                                val bytes = ByteArray(buffer.remaining())
                                                buffer.get(bytes)
                                                val source = PlanarYUVLuminanceSource(
                                                    bytes,
                                                    imageProxy.width,
                                                    imageProxy.height,
                                                    0,
                                                    0,
                                                    imageProxy.width,
                                                    imageProxy.height,
                                                    false,
                                                )
                                                val result = runCatching {
                                                    reader.decode(BinaryBitmap(HybridBinarizer(source)))
                                                }.getOrNull()
                                                val text = result?.text
                                                if (!text.isNullOrBlank() && handled.compareAndSet(false, true)) {
                                                    runOnDefaultDispatcher { importScannedText(text) }
                                                }
                                            }
                                        } finally {
                                            imageProxy.close()
                                        }
                                    }
                                    runCatching {
                                        provider.unbindAll()
                                        cameraRef = provider.bindToLifecycle(
                                            this@ScannerActivity,
                                            CameraSelector.DEFAULT_BACK_CAMERA,
                                            preview,
                                            analysis,
                                        )
                                    }
                                }, ContextCompat.getMainExecutor(ctx))
                                previewView
                            },
                        )
                    }
                }
            }
        }
    }

    private suspend fun importScannedText(text: String) {
        try {
            val profiles = parseProxies(text)
            if (profiles.isEmpty()) {
                handled.set(false)
                onMainDispatcher {
                    Toast.makeText(this@ScannerActivity, R.string.no_proxies_found, Toast.LENGTH_SHORT).show()
                }
                return
            }
            val groupId = DataStore.selectedGroupForImport()
            profiles.forEach { ProfileManager.createProfile(groupId, it) }
            onMainDispatcher {
                Toast.makeText(
                    this@ScannerActivity,
                    resources.getQuantityString(R.plurals.added, profiles.size, profiles.size),
                    Toast.LENGTH_SHORT,
                ).show()
                finish()
            }
        } catch (e: Exception) {
            handled.set(false)
            onMainDispatcher {
                Toast.makeText(this@ScannerActivity, e.readableMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }
}