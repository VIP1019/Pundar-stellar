package com.example.pundarapp.ui.screens.home

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.pundarapp.data.qr.QrScanHandler
import com.example.pundarapp.data.qr.QrScanResult
import com.example.pundarapp.ui.components.FuturisticIcon
import com.example.pundarapp.ui.components.PundarDetailTopBar
import com.example.pundarapp.ui.data.AppState
import com.example.pundarapp.ui.navigation.Routes
import com.example.pundarapp.ui.theme.*
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val scannerOptions = remember {
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
    }
    val barcodeScanner = remember { BarcodeScanning.getClient(scannerOptions) }

    var scannedResult by remember { mutableStateOf<String?>(null) }
    var scanError by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var scanMode by remember { mutableStateOf("camera") }
    val scanLocked = remember { AtomicBoolean(false) }

    LaunchedEffect(isProcessing, scannedResult) {
        scanLocked.set(isProcessing || scannedResult != null)
    }

    suspend fun handleRawQr(raw: String) {
        isProcessing = true
        scanError = null
        when (val result = QrScanHandler.processRawQr(raw)) {
            is QrScanResult.Success -> {
                AppState.pendingQrPayload.value = result.payload
                navController.navigate(Routes.QR_SEND_CONFIRM)
                scannedResult = null
                scanError = null
                scanLocked.set(false)
            }
            is QrScanResult.Error -> {
                scanError = result.message
                scannedResult = "error"
            }
        }
        isProcessing = false
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scanMode = "gallery"
        isProcessing = true
        scanError = null
        scannedResult = "processing"
        try {
            val image = InputImage.fromFilePath(context, uri)
            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    val raw = barcodes.firstOrNull()?.rawValue
                    if (raw == null) {
                        scanError = QrScanHandler.NO_QR_FOUND
                        scannedResult = "error"
                        isProcessing = false
                    } else {
                        scope.launch {
                            scannedResult = raw
                            handleRawQr(raw)
                        }
                    }
                }
                .addOnFailureListener {
                    scanError = QrScanHandler.NO_QR_FOUND
                    scannedResult = "error"
                    isProcessing = false
                }
        } catch (_: Exception) {
            scanError = QrScanHandler.NO_QR_FOUND
            scannedResult = "error"
            isProcessing = false
        }
    }

    Scaffold(
        topBar = {
            PundarDetailTopBar(
                title = "Scan QR",
                onBack = { navController.navigateUp() }
            )
        },
        containerColor = PundarBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (hasCameraPermission) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }
                                val cameraExecutor = Executors.newSingleThreadExecutor()
                                val imageAnalyzer = ImageAnalysis.Builder()
                                    .setTargetResolution(Size(1280, 720))
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()
                                imageAnalyzer.setAnalyzer(cameraExecutor) { imageProxy ->
                                    if (!scanLocked.get()) {
                                        processImageProxy(barcodeScanner, imageProxy) { barcode ->
                                            if (scanLocked.compareAndSet(false, true)) {
                                                ContextCompat.getMainExecutor(ctx).execute {
                                                    scannedResult = barcode
                                                    scanMode = "camera"
                                                }
                                            }
                                        }
                                    } else {
                                        imageProxy.close()
                                    }
                                }
                                try {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview,
                                        imageAnalyzer
                                    )
                                } catch (_: Exception) { }
                            }, ContextCompat.getMainExecutor(ctx))
                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(250.dp)
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = Color.Transparent,
                            shape = RoundedCornerShape(24.dp),
                            border = androidx.compose.foundation.BorderStroke(2.dp, PundarBlue)
                        ) {}
                    }
                }
            } else {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = PundarTextSecondary
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Camera permission is required to scan QR codes.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PundarTextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                            Text("Grant Permission")
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when {
                        isProcessing -> {
                            CircularProgressIndicator(color = PundarBlue, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.height(12.dp))
                            Text(
                                if (scanMode == "gallery") "Processing image..." else "Validating QR...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = PundarTextSecondary
                            )
                        }
                        scanError != null -> {
                            Text(
                                scanError!!,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = {
                                scannedResult = null
                                scanError = null
                                isProcessing = false
                                scanLocked.set(false)
                            }) { Text("Try Again") }
                        }
                        scannedResult != null && scanError == null -> {
                            LaunchedEffect(scannedResult) {
                                val raw = scannedResult ?: return@LaunchedEffect
                                if (raw == "processing" || raw == "error") return@LaunchedEffect
                                handleRawQr(raw)
                            }
                            Text("QR detected", style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold, color = PundarTextPrimary)
                            Spacer(Modifier.height(8.dp))
                            Text("Opening payment confirmation...",
                                style = MaterialTheme.typography.bodyMedium, color = PundarTextSecondary)
                        }
                        else -> {
                            Text("Scan or Upload QR", style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold, color = PundarTextPrimary)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Supports Receive Money, Bill Payment, and Merchant QR codes.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = PundarTextSecondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(20.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ScanOptionButton(
                                    modifier = Modifier.weight(1f),
                                    label = "Scan QR",
                                    icon = Icons.Default.QrCodeScanner,
                                    tint = ElectricBlue,
                                    onClick = {
                                        if (!hasCameraPermission) {
                                            permissionLauncher.launch(Manifest.permission.CAMERA)
                                        }
                                    }
                                )
                                ScanOptionButton(
                                    modifier = Modifier.weight(1f),
                                    label = "Upload Image",
                                    icon = Icons.Default.Image,
                                    tint = NeonCyan,
                                    onClick = {
                                        galleryLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanOptionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = tint),
        border = androidx.compose.foundation.BorderStroke(1.dp, tint.copy(0.5f))
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            FuturisticIcon(icon = icon, tint = tint, size = 36.dp, iconSize = 18.dp,
                shape = RoundedCornerShape(10.dp))
            Spacer(Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun processImageProxy(
    barcodeScanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    imageProxy: ImageProxy,
    onSuccess: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    val rawValue = barcode.rawValue
                    if (rawValue != null) {
                        onSuccess(rawValue)
                        break
                    }
                }
            }
            .addOnCompleteListener { imageProxy.close() }
    } else {
        imageProxy.close()
    }
}
