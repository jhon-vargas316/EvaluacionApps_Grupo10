package com.example.tecnoluna.viewmodel

import androidx.lifecycle.ViewModel
import com.example.tecnoluna.model.Perfil
import com.example.tecnoluna.model.Usuario
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// Los dos tipos de marca que existen
enum class TipoMarca(val etiqueta: String) {
    ENTRADA("Entrada"), SALIDA("Salida")
}

// Una marca: qué tipo es y a qué hora se hizo
data class MarcaAsistencia(val tipo: TipoMarca, val fecha: Long)

// Guarda las marcas de entrada y salida de cada persona.
// La foto NO se guarda: la cámara solo sirve para confirmar la marca.
class AsistenciaViewModel : ViewModel() {

    // Marcas de cada usuario (clave = id del usuario)
    private val _marcas = MutableStateFlow<Map<String, List<MarcaAsistencia>>>(emptyMap())
    val marcas: StateFlow<Map<String, List<MarcaAsistencia>>> = _marcas.asStateFlow()

    // Mensajes cortos para mostrar al usuario
    private val _mensaje = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val mensaje: SharedFlow<String> = _mensaje.asSharedFlow()

    // Solo técnicos y administradores pueden marcar
    fun puedeMarcar(usuario: Usuario) = usuario.perfil != Perfil.CLIENTE

    // La persona está "dentro" si su última marca fue una entrada
    fun estaDentro(lista: List<MarcaAsistencia>) = lista.lastOrNull()?.tipo == TipoMarca.ENTRADA

    // Se llama DESPUÉS de tomar la foto: recién ahí queda registrada la marca
    fun marcar(usuario: Usuario, tipo: TipoMarca): Boolean {
        if (!puedeMarcar(usuario)) {
            _mensaje.tryEmit("Tu perfil no puede marcar entrada o salida")
            return false
        }
        val actuales = _marcas.value[usuario.id].orEmpty()
        // No se puede marcar dos entradas seguidas, ni una salida sin entrada
        if (tipo == TipoMarca.ENTRADA && estaDentro(actuales)) {
            _mensaje.tryEmit("Ya marcaste tu entrada. Ahora puedes marcar tu salida")
            return false
        }
        if (tipo == TipoMarca.SALIDA && !estaDentro(actuales)) {
            _mensaje.tryEmit("Primero debes marcar tu entrada")
            return false
        }
        val nueva = MarcaAsistencia(tipo, System.currentTimeMillis())
        _marcas.update { it + (usuario.id to (actuales + nueva)) }
        _mensaje.tryEmit("${tipo.etiqueta} marcada correctamente")
        return true
    }

    // Se usa cuando la foto no se pudo tomar
    fun avisar(texto: String) { _mensaje.tryEmit(texto) }
}
