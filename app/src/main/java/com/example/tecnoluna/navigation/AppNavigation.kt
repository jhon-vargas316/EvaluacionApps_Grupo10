package com.example.tecnoluna.navigation

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.tecnoluna.ui.screens.*
import com.example.tecnoluna.viewmodel.AsistenciaViewModel
import com.example.tecnoluna.viewmodel.AuthViewModel
import com.example.tecnoluna.viewmodel.NuevaSolicitudViewModel
import com.example.tecnoluna.viewmodel.QrViewModel
import com.example.tecnoluna.viewmodel.SolicitudViewModel

// Define cómo se pasa de una pantalla a otra
@Composable
fun AppNavigation(widthClass: WindowWidthSizeClass) {
    val nav = rememberNavController() // controla la navegación

    // Los ViewModels se crean una sola vez y se comparten entre todas las pantallas
    val authVm: AuthViewModel = viewModel()
    val solVm: SolicitudViewModel = viewModel()
    val formVm: NuevaSolicitudViewModel = viewModel()
    val asistenciaVm: AsistenciaViewModel = viewModel()
    val qrVm: QrViewModel = viewModel()
    val usuario by authVm.usuario.collectAsState()

    NavHost(navController = nav, startDestination = Routes.Splash.route) {

        // 1) Bienvenida -> pasa sola al login
        composable(Routes.Splash.route) {
            SplashScreen {
                nav.navigate(Routes.Login.route) { popUpTo(Routes.Splash.route) { inclusive = true } }
            }
        }

        // 2) Login -> si entra bien, va al menú principal
        composable(Routes.Login.route) {
            LoginScreen(authVm) {
                nav.navigate(Routes.Main.route) { popUpTo(Routes.Login.route) { inclusive = true } }
            }
        }

        // 3) Menú principal (pestañas)
        composable(Routes.Main.route) {
            usuario?.let { u ->
                MainScreen(
                    usuario = u, vm = solVm, asistenciaVm = asistenciaVm, widthClass = widthClass,
                    onAbrir = { nav.navigate(Routes.Detalle.crear(it)) },
                    onNueva = { nav.navigate(Routes.Nueva.route) },
                    onEscanear = { nav.navigate(Routes.Escaner.route) },
                    onLogout = {
                        // Al cerrar sesión se limpia todo y se vuelve al login
                        authVm.cerrarSesion(); solVm.limpiarFiltros(); formVm.limpiar(); qrVm.clearResult()
                        nav.navigate(Routes.Login.route) { popUpTo(0) }
                    }
                )
            }
        }

        // 4) Formulario de nueva solicitud
        composable(Routes.Nueva.route) {
            usuario?.let { u ->
                NuevaSolicitudScreen(
                    usuario = u, formVm = formVm, solVm = solVm, widthClass = widthClass,
                    onBack = { nav.popBackStack() },
                    onVerDetalle = { codigo ->
                        nav.navigate(Routes.Detalle.crear(codigo)) { popUpTo(Routes.Nueva.route) { inclusive = true } }
                    }
                )
            }
        }

        // 5) Escáner de códigos QR (se abre desde "Mi perfil")
        composable(Routes.Escaner.route) {
            usuario?.let { u ->
                QrScannerScreen(
                    usuario = u, qrVm = qrVm, widthClass = widthClass,
                    onBack = { nav.popBackStack() },
                    onVerDetalle = { codigo -> nav.navigate(Routes.Detalle.crear(codigo)) }
                )
            }
        }

        // 6) Detalle de una solicitud (recibe el código por la ruta)
        composable(
            route = Routes.Detalle.route,
            arguments = listOf(navArgument(Routes.Detalle.ARG) { type = NavType.StringType })
        ) { entry ->
            val codigo = entry.arguments?.getString(Routes.Detalle.ARG).orEmpty()
            usuario?.let { u ->
                DetalleScreen(codigo, u, solVm, widthClass) { nav.popBackStack() }
            }
        }
    }

    // Si se pierde la sesión estando dentro de la app, se vuelve al login
    LaunchedEffect(usuario) {
        val actual = nav.currentDestination?.route
        if (usuario == null && actual != null && actual != Routes.Login.route && actual != Routes.Splash.route) {
            nav.navigate(Routes.Login.route) { popUpTo(0) }
        }
    }
}
