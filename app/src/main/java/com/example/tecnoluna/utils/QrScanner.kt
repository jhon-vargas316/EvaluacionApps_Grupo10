package com.example.tecnoluna.utils

import android.annotation.SuppressLint
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "QrScanner"

// Muestra la cámara en pantalla y avisa cuando detecta un código QR.
// Usa CameraX para la cámara y ML Kit para leer el código.
@SuppressLint("UnsafeOptInUsageError")
@Composable
fun QrScanner(
    onQrCodeScanned: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Mantiene siempre la última versión de la función, sin reiniciar la cámara
    val currentCallback by rememberUpdatedState(onQrCodeScanned)

    // Vista previa de la cámara (se crea una sola vez)
    val previewView = remember(context) {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            // COMPATIBLE funciona mejor en emuladores
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    // Dibuja la cámara dentro de Compose
    AndroidView(factory = { previewView }, modifier = modifier)

    // Enciende la cámara al entrar a la pantalla y la apaga al salir
    DisposableEffect(lifecycleOwner) {
        val executor = Executors.newSingleThreadExecutor()       // hilo para analizar imágenes
        val opciones = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)            // solo leer códigos QR
            .build()
        val scanner = BarcodeScanning.getClient(opciones)
        val yaDetectado = AtomicBoolean(false)                    // evita avisar dos veces
        var liberado = false
        var proveedor: ProcessCameraProvider? = null
        val futuro = ProcessCameraProvider.getInstance(context)

        futuro.addListener({
            if (liberado) return@addListener                      // la pantalla ya se cerró
            try {
                val provider = futuro.get()
                proveedor = provider

                // 1) Vista previa
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                // 2) Análisis de imágenes (se descartan los cuadros antiguos)
                val analisis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                analisis.setAnalyzer(executor) { imageProxy ->
                    val imagen = imageProxy.image
                    if (imagen == null || yaDetectado.get()) {
                        imageProxy.close()
                        return@setAnalyzer
                    }
                    val entrada = InputImage.fromMediaImage(imagen, imageProxy.imageInfo.rotationDegrees)
                    scanner.process(entrada)
                        .addOnSuccessListener { codigos ->
                            val valor = codigos.firstNotNullOfOrNull { it.rawValue }
                            // compareAndSet asegura que el aviso ocurra una sola vez
                            if (valor != null && yaDetectado.compareAndSet(false, true)) {
                                analisis.clearAnalyzer()
                                Log.d(TAG, "QR detectado: $valor")
                                currentCallback(valor)
                            }
                        }
                        .addOnFailureListener { Log.e(TAG, "Error al leer el QR", it) }
                        .addOnCompleteListener { imageProxy.close() } // siempre se libera la imagen
                }

                // 3) Conectar la cámara trasera al ciclo de vida de la pantalla
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analisis
                )
            } catch (e: Exception) {
                Log.e(TAG, "No se pudo iniciar la cámara", e)
            }
        }, ContextCompat.getMainExecutor(context))

        // Al salir de la pantalla se liberan todos los recursos
        onDispose {
            liberado = true
            proveedor?.unbindAll()
            scanner.close()
            executor.shutdown()
            Log.d(TAG, "Cámara liberada")
        }
    }
}
