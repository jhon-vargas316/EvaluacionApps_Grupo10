package com.example.tecnoluna.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tecnoluna.model.Usuario
import com.example.tecnoluna.repository.SolicitudRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Lo que la pantalla de login necesita mostrar
data class LoginUiState(
    val correo: String = "",
    val clave: String = "",
    val error: String? = null,   // mensaje de error (si hay)
    val cargando: Boolean = false
)

// Maneja el inicio y cierre de sesión
class AuthViewModel : ViewModel() {

    // Usuario que tiene la sesión iniciada (null = nadie)
    private val _usuario = MutableStateFlow<Usuario?>(null)
    val usuario: StateFlow<Usuario?> = _usuario.asStateFlow()

    // Estado de los campos del login
    private val _login = MutableStateFlow(LoginUiState())
    val login: StateFlow<LoginUiState> = _login.asStateFlow()

    // Cuentas para el ingreso rápido
    val cuentas: List<Usuario> get() = SolicitudRepository.usuarios

    fun onCorreo(v: String) = _login.update { it.copy(correo = v, error = null) }
    fun onClave(v: String) = _login.update { it.copy(clave = v, error = null) }

    // Rellena correo y contraseña con la cuenta elegida
    fun usarCuenta(u: Usuario) = _login.update { it.copy(correo = u.correo, clave = u.clave, error = null) }

    fun ingresar() {
        val s = _login.value
        if (s.cargando) return
        // Primero se revisa que no haya campos vacíos
        if (s.correo.isBlank() || s.clave.isBlank()) {
            _login.update { it.copy(error = "Escribe tu correo y tu contraseña") }
            return
        }
        viewModelScope.launch {
            _login.update { it.copy(cargando = true, error = null) }
            delay(700) // pequeña espera para mostrar el círculo de carga
            val u = SolicitudRepository.login(s.correo, s.clave)
            if (u == null) {
                _login.update { it.copy(cargando = false, error = "Correo o contraseña incorrectos") }
            } else {
                _login.value = LoginUiState()
                _usuario.value = u // al cambiar, la pantalla navega sola al menú
            }
        }
    }

    fun cerrarSesion() {
        _usuario.value = null
        _login.value = LoginUiState()
    }
}
