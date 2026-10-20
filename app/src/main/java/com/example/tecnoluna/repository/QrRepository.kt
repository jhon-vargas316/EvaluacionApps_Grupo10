package com.example.tecnoluna.repository

import com.example.tecnoluna.model.QrResult
import com.example.tecnoluna.model.Reglas
import com.example.tecnoluna.model.ResultadoQr
import com.example.tecnoluna.model.Usuario

// Procesa el texto leído desde un QR y lo relaciona con una solicitud registrada
class QrRepository {

    // Busca un código con forma SOL-1234 dentro del texto.
    // Así funciona con QR que traen solo el código o con el enlace de seguimiento completo.
    private val patronCodigo = Regex("SOL-\\d+", RegexOption.IGNORE_CASE)

    fun processQrContent(content: String, usuario: Usuario): QrResult {
        val texto = content.trim()
        val codigo = patronCodigo.find(texto)?.value?.uppercase()

        // 1) El QR no trae un código de solicitud
        if (codigo == null) {
            return QrResult(texto, null, ResultadoQr.NO_ES_SOLICITUD)
        }
        // 2) El código tiene formato correcto, pero no existe
        val solicitud = SolicitudRepository.obtener(codigo)
            ?: return QrResult(texto, codigo, ResultadoQr.NO_ENCONTRADA)

        // 3) Existe, pero esta persona no tiene permiso para verla
        if (!Reglas.puedeVer(usuario, solicitud)) {
            return QrResult(texto, codigo, ResultadoQr.SIN_ACCESO)
        }
        // 4) Todo bien: se devuelve la solicitud
        return QrResult(texto, codigo, ResultadoQr.ENCONTRADA, solicitud)
    }
}
