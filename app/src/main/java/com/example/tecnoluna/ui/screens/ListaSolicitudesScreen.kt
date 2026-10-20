package com.example.tecnoluna.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.tecnoluna.model.*
import com.example.tecnoluna.ui.components.CampoSelector
import com.example.tecnoluna.ui.components.SolicitudCard
import com.example.tecnoluna.viewmodel.SolicitudViewModel

// Lista de solicitudes con buscador y filtros. La usan los tres perfiles,
// pero cada uno solo ve las solicitudes que le corresponden.
@Composable
fun ListaSolicitudesScreen(usuario: Usuario, vm: SolicitudViewModel, onAbrir: (String) -> Unit) {
    val todas by vm.solicitudes.collectAsState()
    val filtros by vm.filtros.collectAsState()
    var mostrarFiltros by rememberSaveable { mutableStateOf(false) }
    val lista = remember(todas, filtros, usuario) { vm.filtrar(usuario, todas, filtros) }
    val esAdmin = usuario.perfil == Perfil.ADMIN

    // Texto de ayuda según quién mira la lista
    val ayuda = when (usuario.perfil) {
        Perfil.CLIENTE -> "Aquí ves el estado de tus solicitudes. Toca una para ver el detalle."
        Perfil.TECNICO -> "Estas son las solicitudes que te asignaron. Toca una para trabajar en ella."
        Perfil.ADMIN -> "Aquí están todas las solicitudes. Toca una para clasificarla, priorizarla o asignarla."
    }

    // Cuadrícula adaptable: 1 columna en celular, más columnas en pantallas grandes
    LazyVerticalGrid(
        columns = GridCells.Adaptive(340.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Parte de arriba: ayuda, buscador y filtros (ocupa todo el ancho)
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(ayuda, style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = filtros.query,
                    onValueChange = { q -> vm.actualizarFiltros { it.copy(query = q) } },
                    label = { Text("Buscar por código, título o cliente") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { mostrarFiltros = !mostrarFiltros }) {
                        Text(if (mostrarFiltros) "Ocultar filtros" else "Filtrar")
                    }
                    Text("${lista.size} solicitud(es)", style = MaterialTheme.typography.bodySmall)
                }
                AnimatedVisibility(mostrarFiltros) { PanelFiltros(vm, esAdmin) }
            }
        }

        // Mensaje cuando no hay nada que mostrar
        if (lista.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    when (usuario.perfil) {
                        Perfil.CLIENTE -> "No encontramos solicitudes. Crea una con el botón \"Nueva solicitud\"."
                        Perfil.TECNICO -> "No tienes solicitudes asignadas con esos datos."
                        Perfil.ADMIN -> "No hay solicitudes con esos filtros."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Una tarjeta por cada solicitud
        items(lista, key = { it.codigo }) { s ->
            SolicitudCard(s, mostrarCliente = usuario.perfil != Perfil.CLIENTE) { onAbrir(s.codigo) }
        }
    }
}

// Menús para filtrar la lista
@Composable
private fun PanelFiltros(vm: SolicitudViewModel, esAdmin: Boolean) {
    val filtros by vm.filtros.collectAsState()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        CampoSelector(
            label = "Estado", valor = filtros.estado?.etiqueta ?: "Todos",
            opciones = listOf<Estado?>(null) + Estado.values().toList(),
            etiqueta = { it?.etiqueta ?: "Todos" },
            onSeleccion = { e -> vm.actualizarFiltros { it.copy(estado = e) } }
        )
        CampoSelector(
            label = "Prioridad", valor = filtros.prioridad?.etiqueta ?: "Todas",
            opciones = listOf<Prioridad?>(null) + Prioridad.values().toList(),
            etiqueta = { it?.etiqueta ?: "Todas" },
            onSeleccion = { p -> vm.actualizarFiltros { it.copy(prioridad = p) } }
        )
        // Estos dos filtros solo los usa el administrador
        if (esAdmin) {
            CampoSelector(
                label = "Cliente",
                valor = vm.clientes.firstOrNull { it.id == filtros.clienteId }?.nombre ?: "Todos",
                opciones = listOf<Usuario?>(null) + vm.clientes,
                etiqueta = { it?.nombre ?: "Todos" },
                onSeleccion = { c -> vm.actualizarFiltros { it.copy(clienteId = c?.id) } }
            )
            CampoSelector(
                label = "Responsable",
                valor = vm.tecnicos.firstOrNull { it.id == filtros.responsableId }?.nombre ?: "Todos",
                opciones = listOf<Usuario?>(null) + vm.tecnicos,
                etiqueta = { it?.nombre ?: "Todos" },
                onSeleccion = { t -> vm.actualizarFiltros { it.copy(responsableId = t?.id) } }
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = filtros.soloAtrasadas, onCheckedChange = { v -> vm.actualizarFiltros { it.copy(soloAtrasadas = v) } })
            Spacer(Modifier.width(8.dp))
            Text("Solo atrasadas")
            Spacer(Modifier.weight(1f))
            TextButton(onClick = vm::limpiarFiltros) { Text("Limpiar") }
        }
    }
}
