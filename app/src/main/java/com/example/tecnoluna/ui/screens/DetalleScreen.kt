@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.tecnoluna.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tecnoluna.model.*
import com.example.tecnoluna.ui.components.*
import com.example.tecnoluna.ui.utils.anchoMaximo
import com.example.tecnoluna.ui.utils.fechaHora
import com.example.tecnoluna.viewmodel.SolicitudViewModel

// Pantalla con todo el detalle de una solicitud
@Composable
fun DetalleScreen(
    codigo: String,
    usuario: Usuario,
    vm: SolicitudViewModel,
    widthClass: WindowWidthSizeClass,
    onBack: () -> Unit
) {
    val todas by vm.solicitudes.collectAsState()
    val s = todas.firstOrNull { it.codigo == codigo } // busca la solicitud por su código
    val snack = remember { SnackbarHostState() }
    var mostrarWhatsApp by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { vm.mensaje.collect { snack.showSnackbar(it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = coloresBarraSuperior(),
                title = { Text(codigo) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
                }
            )
        },
        snackbarHost = { SnackbarHost(snack) }
    ) { padding ->
        // Si la persona no tiene permiso para ver esta solicitud, se muestra un mensaje
        if (s == null || !Reglas.puedeVer(usuario, s)) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No tienes acceso a esta solicitud o no existe.")
            }
            return@Scaffold
        }
        val mod = Modifier.widthIn(max = anchoMaximo(widthClass)).fillMaxWidth()

        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item { Cabecera(s, mod) }
            item { Seccion("Datos de la solicitud", mod) { Datos(s) } }
            item { Seccion("Evidencias (${s.evidencias.size})", mod) { Evidencias(s) } }

            // Solo el cliente puede compartir la referencia por WhatsApp
            if (usuario.perfil == Perfil.CLIENTE) {
                item {
                    OutlinedButton(onClick = { mostrarWhatsApp = true }, modifier = mod.heightIn(min = 48.dp)) {
                        Text("Compartir por WhatsApp")
                    }
                }
            }

            // Botones y campos según el perfil
            item { PanelAcciones(s, usuario, vm, mod) }

            // El cliente no ve los comentarios internos
            val comentarios = s.comentarios.filter { usuario.perfil != Perfil.CLIENTE || !it.interno }
            item { Seccion("Comentarios (${comentarios.size})", mod) { Comentarios(comentarios) } }

            // Historial de todo lo que ha pasado, en orden de fecha
            val historial = s.historial.filter { usuario.perfil != Perfil.CLIENTE || !it.interno }.sortedBy { it.fecha }
            item { Seccion("Historial", mod) { Historial(historial) } }
        }

        if (mostrarWhatsApp) {
            WhatsAppDialog(
                mensaje = s.mensajeWhatsApp(),
                onEnviar = { vm.registrarEnvioWhatsApp(s.codigo, usuario); mostrarWhatsApp = false },
                onCerrar = { mostrarWhatsApp = false }
            )
        }
    }
}

// ---------------------------------------------------------------- partes de la pantalla

// Tarjeta con un título y contenido adentro
@Composable
private fun Seccion(titulo: String, modifier: Modifier, contenido: @Composable ColumnScope.() -> Unit) {
    Card(modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            contenido()
        }
    }
}

// Parte de arriba: título, estado, prioridad y qué significa el estado
@Composable
private fun Cabecera(s: Solicitud, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(s.titulo, style = MaterialTheme.typography.headlineSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EtiquetaColor(s.estado.etiqueta, s.estado.color())
            EtiquetaColor("Prioridad ${s.prioridad.etiqueta.lowercase()}", s.prioridad.color())
        }
        Text(s.estado.descripcion, style = MaterialTheme.typography.bodyLarge) // explica el estado en palabras simples
        if (s.estaAtrasada()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(6.dp))
                Text("Solicitud atrasada (debía resolverse antes del ${s.fechaLimite.fechaHora()})", color = MaterialTheme.colorScheme.error)
            }
        }
        Text("Última actualización: ${s.actualizada.fechaHora()}", style = MaterialTheme.typography.bodySmall)
    }
}

// Una línea "nombre: valor"
@Composable
private fun Fila(label: String, valor: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Text(valor, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f, fill = false))
    }
}

// Datos principales de la solicitud
@Composable
private fun Datos(s: Solicitud) {
    Text(s.descripcion, style = MaterialTheme.typography.bodyMedium)
    HorizontalDivider()
    Fila("Cliente", "${s.clienteNombre} (${s.empresa})")
    Fila("Categoría", s.categoria.etiqueta)
    Fila("Servicio o sitio", s.servicio)
    Fila("Responsable", s.responsableNombre ?: "Sin asignar")
    Fila("Creada", s.creada.fechaHora())
    Fila("Fecha límite", s.fechaLimite.fechaHora())
    s.fechaResolucion?.let { Fila("Resuelta", it.fechaHora()) }
    if (s.confirmadaPorCliente) Fila("Conformidad", "Confirmada por el cliente")
    s.motivoCancelacion?.let { Fila("Motivo de cancelación", it) }
}

// Archivos adjuntos
@Composable
private fun Evidencias(s: Solicitud) {
    if (s.evidencias.isEmpty()) Text("No hay archivos adjuntos.", style = MaterialTheme.typography.bodySmall)
    s.evidencias.forEach { e ->
        Column {
            Text(e.nombreArchivo, fontWeight = FontWeight.SemiBold)
            Text(
                "${e.tipo.uppercase()} · ${e.tamanoKb} KB · ${e.adjuntadoPor} · ${e.fecha.fechaHora()}",
                style = MaterialTheme.typography.bodySmall
            )
            Text("${e.descripcion} · ${e.estadoRevision}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

// Mensajes escritos en la solicitud
@Composable
private fun Comentarios(lista: List<Comentario>) {
    if (lista.isEmpty()) Text("Todavía no hay comentarios.", style = MaterialTheme.typography.bodySmall)
    lista.sortedBy { it.fecha }.forEach { c ->
        Column {
            Text(
                "${c.autor} (${c.perfil.etiqueta})${if (c.interno) " · INTERNO" else ""}",
                style = MaterialTheme.typography.labelLarge
            )
            Text(c.texto)
            Text(c.fecha.fechaHora(), style = MaterialTheme.typography.bodySmall)
        }
    }
}

// Línea de tiempo: qué pasó, quién lo hizo y cuándo
@Composable
private fun Historial(lista: List<EventoHistorial>) {
    lista.forEachIndexed { i, e ->
        if (i > 0) HorizontalDivider()
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(e.accion, fontWeight = FontWeight.SemiBold)
            // Si cambió de estado, se muestra "antes -> después"
            if (e.estadoAnterior != null && e.estadoNuevo != null && e.estadoAnterior != e.estadoNuevo) {
                Text("${e.estadoAnterior.etiqueta} → ${e.estadoNuevo.etiqueta}", style = MaterialTheme.typography.bodySmall)
            }
            if (e.comentario.isNotBlank()) Text(e.comentario, style = MaterialTheme.typography.bodyMedium)
            e.archivo?.let { Text("Archivo: $it", style = MaterialTheme.typography.bodySmall) }
            Text("${e.fecha.fechaHora()} · ${e.usuario} (${e.perfil.etiqueta})", style = MaterialTheme.typography.bodySmall)
        }
    }
}

// ---------------------------------------------------------------- acciones según el perfil

@Composable
private fun PanelAcciones(s: Solicitud, usuario: Usuario, vm: SolicitudViewModel, modifier: Modifier) {
    Card(modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("¿Qué quieres hacer?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            // Cada perfil ve opciones distintas
            when (usuario.perfil) {
                Perfil.ADMIN -> AccionesAdmin(s, usuario, vm)
                Perfil.TECNICO -> {
                    if (s.responsableId == usuario.id) EstadoYComentarios(s, usuario, vm, permiteAvance = true)
                    else Text("Esta solicitud no está asignada a ti.")
                }
                Perfil.CLIENTE -> AccionesCliente(s, usuario, vm)
            }
        }
    }
}

// Opciones del administrador: clasificar, priorizar y asignar
@Composable
private fun AccionesAdmin(s: Solicitud, usuario: Usuario, vm: SolicitudViewModel) {
    val editable = !s.estado.esFinal // si está cerrada o cancelada ya no se puede cambiar
    CampoSelector(
        label = "Categoría", valor = s.categoria.etiqueta,
        opciones = Categoria.values().toList(), etiqueta = { it.etiqueta },
        onSeleccion = { vm.clasificar(s.codigo, usuario, it) }, enabled = editable
    )
    CampoSelector(
        label = "Prioridad", valor = s.prioridad.etiqueta,
        opciones = Prioridad.values().toList(), etiqueta = { it.etiqueta },
        onSeleccion = { vm.definirPrioridad(s.codigo, usuario, it) }, enabled = editable
    )
    CampoSelector(
        label = "Responsable", valor = s.responsableNombre ?: "Sin asignar",
        opciones = vm.tecnicos, etiqueta = { it.nombre },
        onSeleccion = { vm.asignar(s.codigo, usuario, it.id) }, enabled = editable
    )
    HorizontalDivider()
    EstadoYComentarios(s, usuario, vm, permiteAvance = false)
}

// Texto de cada botón de cambio de estado
private fun etiquetaAccion(e: Estado, perfil: Perfil) = when (e) {
    Estado.EN_REVISION -> "Pasar a revisión"
    Estado.PENDIENTE_INFO -> if (perfil == Perfil.ADMIN) "Pedir información al cliente" else "Necesito más información"
    Estado.EN_PROCESO -> "Comenzar a trabajar"
    Estado.RESUELTA -> "Marcar como resuelta"
    Estado.CERRADA -> "Cerrar solicitud"
    Estado.CANCELADA -> "Cancelar solicitud"
    else -> e.etiqueta
}

// Comentarios, avances y botones para cambiar el estado (administrador y técnico)
@Composable
private fun EstadoYComentarios(s: Solicitud, usuario: Usuario, vm: SolicitudViewModel, permiteAvance: Boolean) {
    var texto by rememberSaveable(s.codigo) { mutableStateOf("") }   // lo que se escribe
    var interno by rememberSaveable { mutableStateOf(false) }         // ¿comentario interno?
    var archivo by remember { mutableStateOf<ArchivoAdjunto?>(null) } // archivo elegido
    val transiciones = Reglas.transiciones(usuario, s)                // estados a los que puede pasar

    OutlinedTextField(
        value = texto, onValueChange = { texto = it },
        label = { Text(if (permiteAvance) "Escribe el avance, comentario o solución" else "Escribe un comentario o motivo") },
        minLines = 2, modifier = Modifier.fillMaxWidth()
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = interno, onCheckedChange = { interno = it })
        Text("Comentario interno (el cliente no lo ve)")
    }
    // Solo el técnico puede adjuntar evidencia a un avance
    if (permiteAvance) {
        CampoSelector(
            label = "Evidencia (opcional)",
            valor = archivo?.nombre ?: "Sin evidencia",
            opciones = listOf<ArchivoAdjunto?>(null) + Adjuntos.disponibles,
            etiqueta = { it?.nombre ?: "Sin evidencia" },
            onSeleccion = { archivo = it }
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { if (vm.comentar(s.codigo, usuario, texto, interno)) texto = "" }, // si salió bien, limpia el texto
            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
        ) { Text("Comentar") }
        if (permiteAvance) {
            OutlinedButton(
                onClick = { if (vm.registrarAvance(s.codigo, usuario, texto, archivo)) { texto = ""; archivo = null } },
                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
            ) { Text("Guardar avance") }
        }
    }
    // Botones de cambio de estado (solo los que corresponden)
    AnimatedVisibility(transiciones.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Cambiar estado (se usa el texto de arriba como explicación):", style = MaterialTheme.typography.bodySmall)
            transiciones.forEach { e ->
                Button(
                    onClick = { if (vm.cambiarEstado(s.codigo, usuario, e, texto)) texto = "" },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) { Text(etiquetaAccion(e, usuario.perfil)) }
            }
        }
    }
}

// Opciones del cliente: responder, confirmar, reabrir o comentar
@Composable
private fun AccionesCliente(s: Solicitud, usuario: Usuario, vm: SolicitudViewModel) {
    var texto by rememberSaveable(s.codigo, s.estado) { mutableStateOf("") }
    var archivo by remember(s.estado) { mutableStateOf<ArchivoAdjunto?>(null) }

    when (s.estado) {
        // Le piden más información: el cliente responde
        Estado.PENDIENTE_INFO -> {
            Text("Necesitamos más información para continuar.")
            OutlinedTextField(value = texto, onValueChange = { texto = it }, label = { Text("Tu respuesta") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            CampoSelector(
                label = "Adjuntar evidencia (opcional)", valor = archivo?.nombre ?: "Sin evidencia",
                opciones = listOf<ArchivoAdjunto?>(null) + Adjuntos.disponibles,
                etiqueta = { it?.nombre ?: "Sin evidencia" }, onSeleccion = { archivo = it }
            )
            Button(
                onClick = { vm.responderInfo(s.codigo, usuario, texto, archivo) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) { Text("Enviar respuesta") }
        }
        // Hay una solución: el cliente la acepta o pide reabrir
        Estado.RESUELTA -> {
            Text("Tenemos una solución. ¿Estás conforme?")
            Button(
                onClick = { vm.confirmar(s.codigo, usuario) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) { Text("Estoy conforme, cerrar solicitud") }
            OutlinedTextField(value = texto, onValueChange = { texto = it }, label = { Text("Motivo para reabrir") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            OutlinedButton(
                onClick = { vm.reabrir(s.codigo, usuario, texto) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) { Text("Pedir que se reabra") }
        }
        // Ya está cerrada: solo puede pedir reabrirla
        Estado.CERRADA -> {
            Text("La solicitud está cerrada. Si el problema sigue, puedes pedir que se reabra.")
            OutlinedTextField(value = texto, onValueChange = { texto = it }, label = { Text("Motivo para reabrir") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            OutlinedButton(
                onClick = { vm.reabrir(s.codigo, usuario, texto) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) { Text("Pedir que se reabra") }
        }
        Estado.CANCELADA -> Text("Esta solicitud fue cancelada y ya no admite cambios.")
        // En cualquier otro estado puede agregar un comentario
        else -> {
            Text("Si quieres, agrega más información a tu solicitud.")
            OutlinedTextField(value = texto, onValueChange = { texto = it }, label = { Text("Comentario") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            OutlinedButton(
                onClick = { if (vm.comentar(s.codigo, usuario, texto, false)) texto = "" },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) { Text("Enviar comentario") }
        }
    }
}
