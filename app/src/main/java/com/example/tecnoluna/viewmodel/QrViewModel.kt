package com.example.tecnoluna.viewmodel

import androidx.lifecycle.ViewModel
import com.example.tecnoluna.model.QrResult
import com.example.tecnoluna.model.Usuario
import com.example.tecnoluna.repository.QrRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Guarda y expone el resultado del escaneo para que la pantalla lo observe
class QrViewModel : ViewModel() {

    private val repository = QrRepository()

    // Versión privada (solo el ViewModel la cambia) y versión pública de solo lectura
    private val _qrResult = MutableStateFlow<QrResult?>(null)
    val qrResult: StateFlow<QrResult?> = _qrResult.asStateFlow()

    // La pantalla llama a esta función cuando la cámara detecta un QR
    fun onQrDetected(content: String, usuario: Usuario) {
        _qrResult.value = repository.processQrContent(content, usuario)
    }

    // Borra el resultado para poder escanear otro código
    fun clearResult() {
        _qrResult.value = null
    }
}
