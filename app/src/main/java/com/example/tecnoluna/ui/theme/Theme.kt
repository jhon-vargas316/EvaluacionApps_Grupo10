package com.example.tecnoluna.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Paleta completa para el modo claro
private val EsquemaClaro = lightColorScheme(
    primary = MoradoPrimario,
    onPrimary = MoradoSobrePrimario,
    primaryContainer = MoradoSuave,
    onPrimaryContainer = MoradoSobreSuave,
    secondary = LilaSecundario,
    onSecondary = Color.White,
    secondaryContainer = LilaContenedor,
    onSecondaryContainer = Color(0xFF1D192B),
    tertiary = OrquideaTerciario,
    onTertiary = Color.White,
    background = FondoClaro,
    onBackground = Color(0xFF1D1B20),
    surface = FondoClaro,
    onSurface = Color(0xFF1D1B20),
    surfaceVariant = Color(0xFFE9DFF2),
    onSurfaceVariant = Color(0xFF4A4458),
    surfaceContainer = Color(0xFFF1E9F8),
    surfaceContainerHigh = Color(0xFFEDE4F5),
    surfaceContainerHighest = TarjetaClara,
    outline = BordeClaro
)

// Paleta completa para el modo oscuro
private val EsquemaOscuro = darkColorScheme(
    primary = MoradoPrimarioOscuro,
    onPrimary = MoradoSobrePrimarioOscuro,
    primaryContainer = MoradoSuaveOscuro,
    onPrimaryContainer = MoradoSobreSuaveOscuro,
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFE5B8F4),
    onTertiary = Color(0xFF4A2557),
    background = FondoOscuro,
    onBackground = Color(0xFFE6E0E9),
    surface = FondoOscuro,
    onSurface = Color(0xFFE6E0E9),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    surfaceContainer = Color(0xFF211F26),
    surfaceContainerHigh = Color(0xFF2B2930),
    surfaceContainerHighest = TarjetaOscura,
    outline = Color(0xFF938F99)
)

// Tema de la app: elige claro u oscuro según el celular
@Composable
fun TecnoLunaTheme(oscuro: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (oscuro) EsquemaOscuro else EsquemaClaro,
        typography = Typography(),
        content = content
    )
}
