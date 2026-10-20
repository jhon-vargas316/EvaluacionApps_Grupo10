@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.tecnoluna.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.tecnoluna.model.Perfil
import com.example.tecnoluna.model.Usuario
import com.example.tecnoluna.ui.components.coloresBarraSuperior
import com.example.tecnoluna.ui.utils.esCompacto
import com.example.tecnoluna.viewmodel.AsistenciaViewModel
import com.example.tecnoluna.viewmodel.SolicitudViewModel

// Las pestañas del menú principal
private enum class Tab(val titulo: String, val icono: ImageVector) {
    RESUMEN("Resumen", Icons.Default.Home),
    SOLICITUDES("Solicitudes", Icons.AutoMirrored.Filled.List),
    AVISOS("Avisos", Icons.Default.Notifications),
    PERFIL("Mi perfil", Icons.Default.Person)
}

// Pantalla principal: barra superior + contenido + menú de pestañas.
// En celulares el menú va abajo; en pantallas grandes va al costado.
@Composable
fun MainScreen(
    usuario: Usuario,
    vm: SolicitudViewModel,
    asistenciaVm: AsistenciaViewModel,
    widthClass: WindowWidthSizeClass,
    onAbrir: (String) -> Unit,
    onNueva: () -> Unit,
    onEscanear: () -> Unit,
    onLogout: () -> Unit
) {
    // Solo el administrador tiene la pestaña "Resumen"
    val tabs = remember(usuario.perfil) {
        when (usuario.perfil) {
            Perfil.ADMIN -> listOf(Tab.RESUMEN, Tab.SOLICITUDES, Tab.AVISOS, Tab.PERFIL)
            else -> listOf(Tab.SOLICITUDES, Tab.AVISOS, Tab.PERFIL)
        }
    }
    var seleccion by rememberSaveable(usuario.id) { mutableStateOf(0) } // pestaña elegida
    val tab = tabs[seleccion.coerceIn(0, tabs.lastIndex)]

    // Avisos del usuario que no ha leído (para el numerito rojo)
    val notifs by vm.notificaciones.collectAsState()
    val mias = remember(notifs, usuario) { notifs.filter { it.destinatarioId == usuario.id } }
    val noLeidas = mias.count { !it.leida }

    // Mensajes cortos que aparecen abajo (confirmaciones, errores y avisos nuevos)
    val snack = remember { SnackbarHostState() }
    var ultimaVista by remember { mutableStateOf(mias.firstOrNull()?.id) }
    LaunchedEffect(mias.firstOrNull()?.id) {
        val n = mias.firstOrNull()
        if (n != null && n.id != ultimaVista) {
            ultimaVista = n.id
            snack.showSnackbar("Aviso: ${n.mensaje}")
        }
    }
    LaunchedEffect(Unit) { vm.mensaje.collect { snack.showSnackbar(it) } }
    LaunchedEffect(Unit) { asistenciaVm.mensaje.collect { snack.showSnackbar(it) } }

    // Ícono de cada pestaña (los avisos muestran un numerito si hay sin leer)
    @Composable
    fun Icono(t: Tab) {
        if (t == Tab.AVISOS && noLeidas > 0) {
            BadgedBox(badge = { Badge { Text("$noLeidas") } }) { Icon(t.icono, contentDescription = t.titulo) }
        } else Icon(t.icono, contentDescription = t.titulo)
    }

    // Título que se ve arriba, según la pestaña y el perfil
    val titulo = when {
        tab == Tab.SOLICITUDES && usuario.perfil == Perfil.CLIENTE -> "Mis solicitudes"
        tab == Tab.SOLICITUDES && usuario.perfil == Perfil.TECNICO -> "Solicitudes asignadas"
        else -> tab.titulo
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = coloresBarraSuperior(),
                title = {
                    Column {
                        Text(titulo)
                        Text("${usuario.perfil.etiqueta} · ${usuario.nombre}", style = MaterialTheme.typography.bodySmall)
                    }
                },
                // Esquina superior derecha: botón para escanear QR (solo en la pestaña "Mi perfil")
                actions = {
                    if (tab == Tab.PERFIL) {
                        TextButton(
                            onClick = onEscanear,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) { Text("Escanear QR", style = MaterialTheme.typography.titleSmall) }
                    }
                }
            )
        },
        bottomBar = {
            // Menú inferior (solo en celulares)
            if (widthClass.esCompacto()) {
                NavigationBar {
                    tabs.forEachIndexed { i, t ->
                        NavigationBarItem(selected = i == seleccion, onClick = { seleccion = i },
                            icon = { Icono(t) }, label = { Text(t.titulo) })
                    }
                }
            }
        },
        floatingActionButton = {
            // Botón "Nueva solicitud": solo para el cliente, en la pestaña de solicitudes
            if (usuario.perfil == Perfil.CLIENTE && tab == Tab.SOLICITUDES) {
                ExtendedFloatingActionButton(
                    onClick = onNueva,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Nueva solicitud") }
                )
            }
        },
        snackbarHost = { SnackbarHost(snack) }
    ) { padding ->
        Row(Modifier.padding(padding).fillMaxSize()) {
            // Menú lateral (solo en pantallas grandes)
            if (!widthClass.esCompacto()) {
                NavigationRail {
                    Spacer(Modifier.height(8.dp))
                    tabs.forEachIndexed { i, t ->
                        NavigationRailItem(selected = i == seleccion, onClick = { seleccion = i },
                            icon = { Icono(t) }, label = { Text(t.titulo) })
                    }
                }
            }
            // Contenido de la pestaña elegida
            Box(Modifier.weight(1f).fillMaxHeight()) {
                when (tab) {
                    Tab.RESUMEN -> PanelResumenScreen(vm)
                    Tab.SOLICITUDES -> ListaSolicitudesScreen(usuario, vm, onAbrir)
                    Tab.AVISOS -> NotificacionesScreen(usuario, vm)
                    Tab.PERFIL -> PerfilScreen(usuario, asistenciaVm, onLogout)
                }
            }
        }
    }
}
