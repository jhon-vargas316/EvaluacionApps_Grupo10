@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.tecnoluna.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tecnoluna.model.Estado
import com.example.tecnoluna.model.Prioridad
import com.example.tecnoluna.model.Solicitud
import com.example.tecnoluna.ui.utils.fechaHora

// ---------- Colores de cada estado y prioridad (para reconocerlos de un vistazo) ----------
fun Estado.color(): Color = when (this) {
    Estado.RECIBIDA -> Color(0xFFD1C4E9)
    Estado.EN_REVISION -> Color(0xFFB39DDB)
    Estado.PENDIENTE_INFO -> Color(0xFFFFCC80)
    Estado.ASIGNADA -> Color(0xFF80CBC4)
    Estado.EN_PROCESO -> Color(0xFF81D4FA)
    Estado.RESUELTA -> Color(0xFFA5D6A7)
    Estado.CERRADA -> Color(0xFFBDBDBD)
    Estado.CANCELADA -> Color(0xFFEF9A9A)
}

fun Prioridad.color(): Color = when (this) {
    Prioridad.BAJA -> Color(0xFFC8E6C9)
    Prioridad.MEDIA -> Color(0xFFFFF59D)
    Prioridad.ALTA -> Color(0xFFFFCC80)
    Prioridad.URGENTE -> Color(0xFFEF9A9A)
}

// Colores de las barras superiores (morado suave)
@Composable
fun coloresBarraSuperior() = TopAppBarDefaults.topAppBarColors(
    containerColor = MaterialTheme.colorScheme.primaryContainer,
    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
)

// Etiqueta redonda de color (ej: "Alta", "En proceso"). El color cambia con una pequeña animación.
@Composable
fun EtiquetaColor(texto: String, color: Color, modifier: Modifier = Modifier) {
    val animado by animateColorAsState(color, tween(450), label = "colorEtiqueta")
    Surface(color = animado, shape = RoundedCornerShape(50), modifier = modifier) {
        Text(
            texto,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            color = Color(0xFF1B1B1B),
            style = MaterialTheme.typography.labelLarge
        )
    }
}

// Tarjeta que resume una solicitud dentro de la lista
@Composable
fun SolicitudCard(s: Solicitud, mostrarCliente: Boolean, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Código a la izquierda y aviso de atraso a la derecha
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(s.codigo, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                if (s.estaAtrasada()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Atrasada", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            Text(s.titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EtiquetaColor(s.estado.etiqueta, s.estado.color())
                EtiquetaColor("Prioridad ${s.prioridad.etiqueta.lowercase()}", s.prioridad.color())
            }
            if (mostrarCliente) Text("Cliente: ${s.clienteNombre} · ${s.empresa}", style = MaterialTheme.typography.bodySmall)
            Text("Responsable: ${s.responsableNombre ?: "Sin asignar"}", style = MaterialTheme.typography.bodySmall)
            Text("Última actualización: ${s.actualizada.fechaHora()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// Lista desplegable: al tocar el campo se abre un menú con opciones
@Composable
fun <T> CampoSelector(
    label: String,
    valor: String,
    opciones: List<T>,
    etiqueta: (T) -> String,
    onSeleccion: (T) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    enabled: Boolean = true
) {
    var abierto by remember { mutableStateOf(false) } // ¿el menú está abierto?
    Box(modifier) {
        OutlinedTextField(
            value = valor, onValueChange = {}, readOnly = true, enabled = enabled,
            label = { Text(label) },
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
        )
        // Capa invisible encima del campo para detectar el toque
        Box(Modifier.matchParentSize().clickable(enabled = enabled) { abierto = true })
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            opciones.forEach { op ->
                DropdownMenuItem(text = { Text(etiqueta(op)) }, onClick = { onSeleccion(op); abierto = false })
            }
        }
    }
}

// Barra horizontal que muestra cuánto representa un número del total
@Composable
fun BarraIndicador(etiqueta: String, valor: Int, maximo: Int, color: Color) {
    val fraccion = if (maximo == 0) 0f else valor.toFloat() / maximo
    val animada by animateFloatAsState(fraccion, tween(600), label = "barra")
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(etiqueta, style = MaterialTheme.typography.bodyMedium)
            Text("$valor", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().height(8.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))) {
            Box(Modifier.fillMaxWidth(animada.coerceIn(0f, 1f)).fillMaxHeight().background(color, RoundedCornerShape(4.dp)))
        }
    }
}

// Ventana que muestra el mensaje que se compartirá por WhatsApp
@Composable
fun WhatsAppDialog(mensaje: String, onEnviar: () -> Unit, onCerrar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Compartir por WhatsApp") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Este es el mensaje que se compartirá:", style = MaterialTheme.typography.bodyMedium)
                // Burbuja verde, parecida a un mensaje de WhatsApp
                Surface(color = Color(0xFFDCF8C6), shape = RoundedCornerShape(12.dp)) {
                    Text(mensaje, modifier = Modifier.padding(12.dp), color = Color(0xFF1B1B1B), style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = { Button(onClick = onEnviar) { Text("Compartir") } },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } }
    )
}
