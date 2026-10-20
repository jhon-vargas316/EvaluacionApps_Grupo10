package com.example.tecnoluna.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tecnoluna.model.*
import com.example.tecnoluna.repository.SolicitudRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Errores que se muestran debajo de cada campo (null = el campo está bien)
data class ErroresForm(
    val titulo: String? = null,
    val descripcion: String? = null,
    val categoria: String? = null,
    val archivo: String? = null
) {
    val hayErrores: Boolean get() = listOfNotNull(titulo, descripcion, categoria, archivo).isNotEmpty()
}

// Todo lo que el formulario muestra en pantalla
data class NuevaSolicitudUiState(
    val titulo: String = "",
    val descripcion: String = "",
    val categoria: Categoria? = null,
    val servicio: String = "",
    val archivo: ArchivoAdjunto? = null,
    val errores: ErroresForm = ErroresForm(),
    val enviando: Boolean = false,       // true mientras se guarda
    val creada: Solicitud? = null        // la solicitud ya creada (para mostrar su código)
)

// Controla el formulario "Nueva solicitud": guarda lo escrito, valida y envía
class NuevaSolicitudViewModel : ViewModel() {

    private val _ui = MutableStateFlow(NuevaSolicitudUiState())
    val ui: StateFlow<NuevaSolicitudUiState> = _ui.asStateFlow()

    // Cada vez que el usuario escribe, se guarda el texto y se borra el error de ese campo
    fun onTitulo(v: String) = _ui.update { it.copy(titulo = v.take(60), errores = it.errores.copy(titulo = null)) }
    fun onDescripcion(v: String) = _ui.update { it.copy(descripcion = v.take(500), errores = it.errores.copy(descripcion = null)) }
    fun onCategoria(v: Categoria) = _ui.update { it.copy(categoria = v, errores = it.errores.copy(categoria = null)) }
    fun onServicio(v: String) = _ui.update { it.copy(servicio = v.take(80)) }
    fun onArchivo(a: ArchivoAdjunto?) = _ui.update {
        it.copy(archivo = a, errores = it.errores.copy(archivo = a?.let { x -> Adjuntos.validar(x) }))
    }

    // Revisa todos los campos. Devuelve true si todo está correcto.
    private fun validar(): Boolean {
        val s = _ui.value
        val e = ErroresForm(
            titulo = when {
                s.titulo.isBlank() -> "El título es obligatorio"
                s.titulo.trim().length < 5 -> "Escribe al menos 5 caracteres"
                else -> null
            },
            descripcion = when {
                s.descripcion.isBlank() -> "Cuéntanos qué necesitas"
                s.descripcion.trim().length < 20 -> "Agrega más detalle (mínimo 20 caracteres)"
                else -> null
            },
            categoria = if (s.categoria == null) "Elige una categoría" else null,
            archivo = s.archivo?.let { Adjuntos.validar(it) }
        )
        _ui.update { it.copy(errores = e) }
        return !e.hayErrores
    }

    // Botón "Enviar solicitud"
    fun enviar(usuario: Usuario) {
        if (_ui.value.enviando || !validar()) return // si hay errores, se detiene aquí
        viewModelScope.launch {
            _ui.update { it.copy(enviando = true) }
            delay(900) // pequeña espera para mostrar "Enviando…"
            val s = _ui.value
            val nueva = SolicitudRepository.crear(usuario, s.titulo, s.descripcion, s.categoria!!, s.servicio, s.archivo)
            _ui.update { it.copy(enviando = false, creada = nueva) }
        }
    }

    // Deja el formulario vacío otra vez
    fun limpiar() { _ui.value = NuevaSolicitudUiState() }
}
