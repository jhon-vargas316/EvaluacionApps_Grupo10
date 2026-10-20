@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.tecnoluna.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.tecnoluna.model.QrResult
import com.example.tecnoluna.model.ResultadoQr
import com.example.tecnoluna.model.Usuario
import com.example.tecnoluna.ui.components.EtiquetaColor
import com.example.tecnoluna.ui.components.color
import com.example.tecnoluna.ui.components.coloresBarraSuperior
import com.example.tecnoluna.ui.utils.anchoMaximo
import com.example.tecnoluna.utils.CameraPermissionHelper
import com.example.tecnoluna.utils.QrScanner
import com.example.tecnoluna.viewmodel.QrViewModel

// Pantalla para escanear un código QR de una solicitud.
// Tiene tres estados: sin permiso de cámara, escaneando y mostrando el resultado.
@Composable
fun QrScannerScreen(
    usuario: Usuario,
    qrVm: QrViewModel,
    widthClass: WindowWidthSizeClass,
    onBack: () -> Unit,
    onVerDetalle: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val qrResult by qrVm.qrResult.collectAsState() // resultado del último escaneo (null = aún no hay)

    // ¿La app tiene permiso de cámara? Y, ¿el usuario ya lo rechazó alguna vez?
    var tienePermiso by remember { mutableStateOf(CameraPermissionHelper.hasCameraPermission(context)) }
    var rechazado by remember { mutableStateOf(false) }

    // Ventana del sistema que pide el permiso de cámara
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        tienePermiso = concedido
        if (!concedido) rechazado = true
    }

    // Al abrir la pantalla por primera vez se pide el permiso automáticamente
    LaunchedEffect(Unit) {
        if (!tienePermiso) pedirPermiso.launch(Manifest.permission.CAMERA)
    }

    // Cada vez que la pantalla vuelve al primer plano se revisa de nuevo el permiso
    // (por si el usuario lo cambió desde los ajustes del celular)
    DisposableEffect(lifecycleOwner) {
        val observador = LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_RESUME) {
                tienePermiso = CameraPermissionHelper.hasCameraPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observador)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observador) }
    }

    // Al salir con el botón "atrás" del celular se borra el resultado
    BackHandler { qrVm.clearResult(); onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = coloresBarraSuperior(),
                title = { Text("Escanear código QR") },
                navigationIcon = {
                    IconButton(onClick = { qrVm.clearResult(); onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier.widthIn(max = anchoMaximo(widthClass)).fillMaxWidth()
                    .verticalScroll(rememberScrollState()).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val resultado = qrResult
                when {
                    // ESTADO 1: no hay permiso de cámara
                    !tienePermiso -> PedirPermiso(
                        rechazado = rechazado,
                        onPedir = { pedirPermiso.launch(Manifest.permission.CAMERA) },
                        onAjustes = {
                            // Abre los ajustes de la app para activar el permiso a mano
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                .setData(Uri.fromParts("package", context.packageName, null))
                            context.startActivity(intent)
                        }
                    )

                    // ESTADO 2: escaneando (todavía no hay resultado)
                    resultado == null -> Escaneando { contenido -> qrVm.onQrDetected(contenido, usuario) }

                    // ESTADO 3: ya se leyó un QR, se muestra el resultado
                    else -> MostrarResultado(
                        r = resultado,
                        onVerDetalle = { codigo -> qrVm.clearResult(); onVerDetalle(codigo) },
                        onOtro = qrVm::clearResult
                    )
                }
            }
        }
    }
}

// Estado 1: explica por qué se necesita la cámara y ofrece pedir el permiso
@Composable
private fun PedirPermiso(rechazado: Boolean, onPedir: () -> Unit, onAjustes: () -> Unit) {
    Spacer(Modifier.height(48.dp))
    Text("Permiso de cámara requerido", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
    Text(
        "Para escanear códigos QR, necesitamos acceso a la cámara.",
        style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center
    )
    Button(onClick = onPedir, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
        Text("Conceder permiso de cámara")
    }
    // Si ya lo rechazó, Android puede dejar de mostrar la ventana: se ofrece ir a los ajustes
    if (rechazado) {
        Text(
            "Si la ventana no aparece, activa el permiso de cámara desde los ajustes de la app.",
            style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center
        )
        OutlinedButton(onClick = onAjustes, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text("Abrir ajustes de la app")
        }
    }
}

// Estado 2: cámara en vivo con un marco para apuntar al código
@Composable
private fun Escaneando(onQrDetectado: (String) -> Unit) {
    Text("Apunta al código QR", style = MaterialTheme.typography.titleLarge)
    Box(
        modifier = Modifier.fillMaxWidth().height(380.dp).clip(RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        QrScanner(onQrCodeScanned = onQrDetectado, modifier = Modifier.fillMaxSize())
        // Marco guía: el código debe quedar dentro de este cuadrado
        Surface(
            color = Color.Transparent,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(3.dp, MaterialTheme.colorScheme.primary),
            modifier = Modifier.size(240.dp)
        ) {}
    }
    Text("Enfoca el código QR dentro del marco.", style = MaterialTheme.typography.bodyMedium)
    Text(
        "Puedes escanear el QR de una solicitud (contiene su código, por ejemplo SOL-1001, o su enlace de seguimiento).",
        style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center
    )
}

// Estado 3: muestra qué pasó con el código leído
@Composable
private fun MostrarResultado(r: QrResult, onVerDetalle: (String) -> Unit, onOtro: () -> Unit) {
    when (r.resultado) {
        // Se encontró la solicitud: se muestran sus datos principales
        ResultadoQr.ENCONTRADA -> {
            val s = r.solicitud!!
            Text("Solicitud encontrada", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(s.codigo, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(s.titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(s.descripcion, style = MaterialTheme.typography.bodyMedium)
                    Text("Categoría: ${s.categoria.etiqueta}", style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        EtiquetaColor(s.estado.etiqueta, s.estado.color())
                        EtiquetaColor("Prioridad ${s.prioridad.etiqueta.lowercase()}", s.prioridad.color())
                    }
                    Text(s.estado.descripcion, style = MaterialTheme.typography.bodySmall)
                }
            }
            Button(onClick = { onVerDetalle(s.codigo) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text("Ver detalle completo")
            }
        }
        // La solicitud es de otra persona
        ResultadoQr.SIN_ACCESO -> {
            Text("Sin acceso", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error)
            Text("La solicitud ${r.codigo} existe, pero no tienes permiso para verla.", textAlign = TextAlign.Center)
        }
        // El código no existe
        ResultadoQr.NO_ENCONTRADA -> {
            Text("Solicitud no encontrada", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error)
            Text("No existe ninguna solicitud con el código ${r.codigo}.", textAlign = TextAlign.Center)
        }
        // El QR trae otra cosa
        ResultadoQr.NO_ES_SOLICITUD -> {
            Text("Este QR no es de una solicitud", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error)
            Text("El código QR no contiene un código de solicitud de TecnoLuna. Contenido leído:", textAlign = TextAlign.Center)
            Card(Modifier.fillMaxWidth()) {
                Text(r.content, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
    OutlinedButton(onClick = onOtro, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
        Text("Escanear otro código QR")
    }
}
