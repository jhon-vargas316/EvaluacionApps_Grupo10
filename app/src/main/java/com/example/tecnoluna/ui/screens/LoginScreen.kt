package com.example.tecnoluna.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.tecnoluna.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

// Pantalla de bienvenida: muestra el nombre y un círculo de carga por unos segundos
@Composable
fun SplashScreen(onFinish: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true      // activa la animación de entrada
        delay(1800)         // espera 1,8 segundos
        onFinish()          // pasa al login
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AnimatedVisibility(visible, enter = fadeIn() + slideInVertically { it / 2 }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TecnoLuna", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("Gestión de solicitudes de clientes", style = MaterialTheme.typography.bodyLarge)
                }
            }
            CircularProgressIndicator()
        }
    }
}

// Pantalla de inicio de sesión
@Composable
fun LoginScreen(vm: AuthViewModel, onLogin: () -> Unit) {
    val ui by vm.login.collectAsState()
    val usuario by vm.usuario.collectAsState()

    // Cuando el login es correcto, se avisa para pasar a la siguiente pantalla
    LaunchedEffect(usuario) { if (usuario != null) onLogin() }

    Box(Modifier.fillMaxSize().systemBarsPadding(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("TecnoLuna", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text("Ingresa con tu correo y contraseña", style = MaterialTheme.typography.bodyLarge)

            // Campo del correo
            OutlinedTextField(
                value = ui.correo, onValueChange = vm::onCorreo, label = { Text("Correo") },
                singleLine = true, isError = ui.error != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
            // Campo de la contraseña (los caracteres se ocultan con puntos)
            OutlinedTextField(
                value = ui.clave, onValueChange = vm::onClave, label = { Text("Contraseña") },
                singleLine = true, isError = ui.error != null,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                supportingText = ui.error?.let { { Text(it) } }, // el error aparece bajo el campo
                modifier = Modifier.fillMaxWidth()
            )
            // Botón de ingreso (muestra un círculo mientras carga)
            Button(onClick = vm::ingresar, enabled = !ui.cargando, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                if (ui.cargando) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("Ingresar", style = MaterialTheme.typography.titleMedium)
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            // Botones de ingreso rápido: rellenan el correo y la contraseña de cada perfil
            Text("Ingreso rápido", style = MaterialTheme.typography.titleSmall)
            vm.cuentas.forEach { u ->
                FilledTonalButton(onClick = { vm.usarCuenta(u) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text("${u.perfil.etiqueta}: ${u.nombre}", textAlign = TextAlign.Center)
                }
            }
        }
    }
}
