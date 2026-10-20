package com.example.tecnoluna.ui.screens

import android.Manifest
import android.content.ActivityNotFoundException
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tecnoluna.model.Usuario
import com.example.tecnoluna.ui.utils.fechaHora
import com.example.tecnoluna.ui.utils.hora
import com.example.tecnoluna.utils.CameraPermissionHelper
import com.example.tecnoluna.viewmodel.AsistenciaViewModel
import com.example.tecnoluna.viewmodel.TipoMarca

// Sección "Marcar entrada" / "Marcar salida".
// Funcionamiento: se toca el botón -> (si falta) se pide el permiso de cámara -> se abre la cámara
// -> se toma la foto -> recién ahí se marca. La foto no se guarda en el celular.
@Composable
fun SeccionAsistencia(usuario: Usuario, vm: AsistenciaViewModel) {
    val context = LocalContext.current
    val todas by vm.marcas.collectAsState()
    val marcas = todas[usuario.id].orEmpty()   // marcas de esta persona
    val dentro = vm.estaDentro(marcas)         // true si ya marcó entrada y aún no sale

    // Guarda qué botón se tocó (entrada o salida) mientras la cámara está abierta
    var pendiente by rememberSaveable { mutableStateOf<String?>(null) }
    // Foto y tipo de marca para mostrar la ventana de confirmación
    var foto by remember { mutableStateOf<Bitmap?>(null) }
    var tipoConfirmado by remember { mutableStateOf<TipoMarca?>(null) }

    // Prepara la cámara. TakePicturePreview abre la cámara del celular y devuelve la foto (o null si se cancela).
    val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        val tipo = pendiente?.let { TipoMarca.valueOf(it) }
        pendiente = null
        if (bitmap != null && tipo != null) {
            // Se tomó la foto: ahora sí se registra la entrada o la salida
            if (vm.marcar(usuario, tipo)) {
                foto = bitmap
                tipoConfirmado = tipo
            }
        } else {
            // Se cerró la cámara sin tomar foto: no se marca nada
            vm.avisar("No se tomó la foto, por eso no se marcó la ${tipo?.etiqueta?.lowercase() ?: "asistencia"}")
        }
    }

    // Abre la cámara del celular (el permiso ya debe estar concedido)
    fun lanzarCamara() {
        try {
            camara.launch(null)
        } catch (e: ActivityNotFoundException) {
            pendiente = null
            vm.avisar("No se encontró una cámara en este dispositivo")
        }
    }

    // Ventana del sistema que pide el permiso de cámara
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) {
            lanzarCamara() // si lo aceptó, se abre la cámara de inmediato
        } else {
            pendiente = null
            vm.avisar("Necesitas permitir el uso de la cámara para marcar tu asistencia")
        }
    }

    // Se llama al tocar "Marcar entrada" o "Marcar salida"
    fun abrirCamara(tipo: TipoMarca) {
        pendiente = tipo.name // se recuerda qué botón se tocó
        if (CameraPermissionHelper.hasCameraPermission(context)) {
            lanzarCamara()
        } else {
            pedirPermiso.launch(Manifest.permission.CAMERA)
        }
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Entrada y salida", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Al marcar se abrirá la cámara. Toma una foto y se registrará la hora. La foto no se guarda.",
                style = MaterialTheme.typography.bodyMedium
            )

            // Estado actual de la persona
            val ultima = marcas.lastOrNull()
            Text(
                when {
                    ultima == null -> "Aún no has marcado tu entrada."
                    dentro -> "Estás dentro desde las ${ultima.fecha.hora()}."
                    else -> "Saliste a las ${ultima.fecha.hora()}."
                },
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            // Botón de entrada (se desactiva si ya marcó entrada)
            Button(
                onClick = { abrirCamara(TipoMarca.ENTRADA) },
                enabled = !dentro,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
            ) { Text("Marcar entrada") }

            // Botón de salida (se desactiva si todavía no marca entrada)
            OutlinedButton(
                onClick = { abrirCamara(TipoMarca.SALIDA) },
                enabled = dentro,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
            ) { Text("Marcar salida") }

            // Últimas marcas (la más nueva arriba)
            if (marcas.isNotEmpty()) {
                HorizontalDivider()
                Text("Últimos registros", style = MaterialTheme.typography.titleSmall)
                marcas.takeLast(5).reversed().forEach { m ->
                    Text("${m.tipo.etiqueta} · ${m.fecha.fechaHora()}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }

    // Ventana que confirma la marca y muestra la foto recién tomada
    val bitmap = foto
    val tipo = tipoConfirmado
    if (bitmap != null && tipo != null) {
        AlertDialog(
            onDismissRequest = { foto = null; tipoConfirmado = null },
            title = { Text("${tipo.etiqueta} registrada") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Foto tomada",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp)
                    )
                    Text("Hora: ${System.currentTimeMillis().fechaHora()}")
                }
            },
            confirmButton = { Button(onClick = { foto = null; tipoConfirmado = null }) { Text("Listo") } }
        )
    }
}
