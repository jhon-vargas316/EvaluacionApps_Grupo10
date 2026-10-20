package com.example.tecnoluna.ui.utils

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ¿La pantalla es angosta (un celular normal)?
fun WindowWidthSizeClass.esCompacto(): Boolean = this == WindowWidthSizeClass.Compact

// Ancho máximo del contenido, para que no se estire demasiado en tablets
fun anchoMaximo(c: WindowWidthSizeClass): Dp = when (c) {
    WindowWidthSizeClass.Compact -> 600.dp
    WindowWidthSizeClass.Medium -> 720.dp
    else -> 840.dp
}

// Convierte una fecha en texto: 25/12/2026 14:30
fun Long.fechaHora(): String =
    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "CL")).format(Date(this))

// Convierte una fecha en solo la hora: 14:30
fun Long.hora(): String =
    SimpleDateFormat("HH:mm", Locale("es", "CL")).format(Date(this))
