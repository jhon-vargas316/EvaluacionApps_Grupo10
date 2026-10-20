package com.example.tecnoluna

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.ui.Modifier
import com.example.tecnoluna.navigation.AppNavigation
import com.example.tecnoluna.ui.theme.TecnoLunaTheme

// Primera pantalla que abre Android. Aquí solo se prepara el tema y se inicia la navegación.
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class, ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // la app usa toda la pantalla, incluso bajo la barra de estado
        setContent {
            TecnoLunaTheme { // colores morados de la app
                // Fondo de toda la app
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    // Calcula si la pantalla es chica, mediana o grande
                    val widthClass = calculateWindowSizeClass(this).widthSizeClass
                    AppNavigation(widthClass)
                }
            }
        }
    }
}
