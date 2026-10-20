package com.example.tecnoluna.navigation

// Lista de pantallas de la app y su "dirección" interna
sealed class Routes(val route: String) {
    object Splash : Routes("splash")   // pantalla de bienvenida
    object Login : Routes("login")     // inicio de sesión
    object Main : Routes("main")       // menú principal con pestañas
    object Nueva : Routes("nueva")     // formulario de nueva solicitud
    object Escaner : Routes("escaner") // escáner de códigos QR
    object Detalle : Routes("detalle/{codigo}") { // detalle de una solicitud
        const val ARG = "codigo"
        fun crear(codigo: String) = "detalle/$codigo"
    }
}
