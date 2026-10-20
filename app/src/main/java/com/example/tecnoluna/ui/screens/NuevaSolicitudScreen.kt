@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.tecnoluna.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.tecnoluna.model.*
import com.example.tecnoluna.ui.components.CampoSelector
import com.example.tecnoluna.ui.components.WhatsAppDialog
import com.example.tecnoluna.ui.components.coloresBarraSuperior
import com.example.tecnoluna.ui.utils.anchoMaximo
import com.example.tecnoluna.viewmodel.NuevaSolicitudViewModel
import com.example.tecnoluna.viewmodel.SolicitudViewModel
import kotlinx.coroutines.launch

// Formulario para que el cliente cree una solicitud nueva
@Composable
fun NuevaSolicitudScreen(
    usuario: Usuario,
    formVm: NuevaSolicitudViewModel,
    solVm: SolicitudViewModel,
    widthClass: WindowWidthSizeClass,
    onBack: () -> Unit,
    onVerDetalle: (String) -> Unit
) {
    val ui by formVm.ui.collectAsState() // lo que el formulario debe mostrar
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var mostrarWhatsApp by remember { mutableStateOf(false) }
    var mostrarExito by remember { mutableStateOf(false) }

    // Cuando la solicitud ya fue creada, se muestra la ventana de éxito
    LaunchedEffect(ui.creada) { if (ui.creada != null) mostrarExito = true }
    LaunchedEffect(Unit) { solVm.mensaje.collect { snack.showSnackbar(it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = coloresBarraSuperior(),
                title = { Text("Nueva solicitud") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
                }
            )
        },
        snackbarHost = { SnackbarHost(snack) }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier.widthIn(max = anchoMaximo(widthClass)).fillMaxWidth()
                    .verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Cuéntanos qué necesitas. Los campos con error se marcan en rojo.", style = MaterialTheme.typography.bodyMedium)

                // Campo: título
                OutlinedTextField(
                    value = ui.titulo, onValueChange = formVm::onTitulo, label = { Text("Título") },
                    singleLine = true, isError = ui.errores.titulo != null,
                    supportingText = { Text(ui.errores.titulo ?: "${ui.titulo.length}/60") }, // muestra el error o el contador
                    modifier = Modifier.fillMaxWidth()
                )
                // Campo: descripción
                OutlinedTextField(
                    value = ui.descripcion, onValueChange = formVm::onDescripcion, label = { Text("Descripción") },
                    minLines = 4, isError = ui.errores.descripcion != null,
                    supportingText = { Text(ui.errores.descripcion ?: "${ui.descripcion.length}/500") },
                    modifier = Modifier.fillMaxWidth()
                )
                // Campo: categoría (lista desplegable)
                CampoSelector(
                    label = "Categoría", valor = ui.categoria?.etiqueta ?: "Elige una categoría",
                    opciones = Categoria.values().toList(), etiqueta = { it.etiqueta },
                    onSeleccion = formVm::onCategoria, error = ui.errores.categoria
                )
                // Campo: servicio o sitio relacionado
                OutlinedTextField(
                    value = ui.servicio, onValueChange = formVm::onServicio,
                    label = { Text("Servicio o sitio web relacionado (opcional)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                // Campo: evidencia (archivo adjunto)
                CampoSelector(
                    label = "Evidencia (opcional)",
                    valor = ui.archivo?.let { "${it.nombre} (${it.tamanoKb} KB)" } ?: "Sin evidencia",
                    opciones = listOf<ArchivoAdjunto?>(null) + Adjuntos.disponibles,
                    etiqueta = { a -> a?.let { "${it.nombre} (${it.tamanoKb} KB)" } ?: "Sin evidencia" },
                    onSeleccion = formVm::onArchivo, error = ui.errores.archivo
                )
                Text("Formatos permitidos: PNG, JPG o PDF · máximo 2 MB.", style = MaterialTheme.typography.bodySmall)

                Spacer(Modifier.height(8.dp))
                // Botón: guardar borrador
                OutlinedButton(
                    onClick = { scope.launch { snack.showSnackbar("Borrador guardado") } },
                    enabled = !ui.enviando,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                ) { Text("Guardar borrador") }
                // Botón: enviar (muestra "Enviando…" mientras trabaja)
                Button(
                    onClick = { formVm.enviar(usuario) },
                    enabled = !ui.enviando,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                ) {
                    if (ui.enviando) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Enviando…")
                    } else Text("Enviar solicitud")
                }
            }
        }
    }

    // Ventana de éxito: muestra el código de seguimiento
    val creada = ui.creada
    if (creada != null && mostrarExito && !mostrarWhatsApp) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("¡Solicitud registrada!") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Tu código de seguimiento es:")
                    Text(creada.codigo, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                    Text("Estado actual: ${creada.estado.etiqueta}", style = MaterialTheme.typography.bodyMedium)
                    Text("Si quieres, comparte este código por WhatsApp.", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = { Button(onClick = { mostrarWhatsApp = true }) { Text("Compartir por WhatsApp") } },
            dismissButton = {
                TextButton(onClick = {
                    val c = creada.codigo
                    formVm.limpiar(); mostrarExito = false
                    onVerDetalle(c)
                }) { Text("Ver solicitud") }
            }
        )
    }
    // Ventana con el mensaje de WhatsApp
    if (creada != null && mostrarWhatsApp) {
        WhatsAppDialog(
            mensaje = creada.mensajeWhatsApp(),
            onEnviar = {
                solVm.registrarEnvioWhatsApp(creada.codigo, usuario)
                val c = creada.codigo
                mostrarWhatsApp = false; mostrarExito = false
                formVm.limpiar()
                onVerDetalle(c)
            },
            onCerrar = { mostrarWhatsApp = false }
        )
    }
}
