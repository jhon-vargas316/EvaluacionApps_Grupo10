package com.example.tecnoluna.ui.screens

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tecnoluna.model.Estado
import com.example.tecnoluna.model.Prioridad
import com.example.tecnoluna.ui.components.BarraIndicador
import com.example.tecnoluna.ui.components.color
import com.example.tecnoluna.viewmodel.SolicitudViewModel

// Resumen del administrador. Los números salen de las mismas solicitudes que muestra la lista,
// por eso siempre coinciden.
@Composable
fun PanelResumenScreen(vm: SolicitudViewModel) {
    val todas by vm.solicitudes.collectAsState()
    val r = remember(todas) { vm.calcularResumen(todas) } // se recalcula cuando cambian las solicitudes

    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Título y botón para registrar una solicitud que llegó por WhatsApp
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Hay ${r.total} solicitudes en total", style = MaterialTheme.typography.titleMedium)
                OutlinedButton(onClick = vm::registrarSolicitudDeWhatsApp, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text("Registrar solicitud recibida por WhatsApp")
                }
            }
        }
        // Cuadros con números
        item { CuadroNumero("Pendientes", r.pendientes, Color(0xFFFFE0B2)) }
        item { CuadroNumero("Atrasadas", r.atrasadas, Color(0xFFEF9A9A)) }
        items(Estado.values().toList()) { e -> CuadroNumero(e.etiqueta, r.porEstado[e] ?: 0, e.color()) }

        // Barras por prioridad
        item(span = { GridItemSpan(maxLineSpan) }) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Por prioridad", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Prioridad.values().forEach { p ->
                        BarraIndicador(p.etiqueta, r.porPrioridad[p] ?: 0, r.total, p.color())
                    }
                }
            }
        }
        // Barras por responsable
        item(span = { GridItemSpan(maxLineSpan) }) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Por responsable", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    r.porResponsable.entries.sortedByDescending { it.value }.forEach { (nombre, n) ->
                        BarraIndicador(nombre, n, r.total, MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

// Cuadro de color con un número grande (el número sube con una animación)
@Composable
private fun CuadroNumero(titulo: String, valor: Int, color: Color) {
    val animado by animateIntAsState(valor, label = "conteo")
    Card(colors = CardDefaults.cardColors(containerColor = color)) {
        Column(Modifier.padding(16.dp).fillMaxWidth()) {
            Text("$animado", style = MaterialTheme.typography.headlineMedium, color = Color(0xFF1B1B1B), fontWeight = FontWeight.Bold)
            Text(titulo, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF1B1B1B))
        }
    }
}
