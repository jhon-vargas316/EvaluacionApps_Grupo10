package com.example.tecnoluna.model

// Posibles resultados después de leer un código QR
enum class ResultadoQr {
    ENCONTRADA,       // el QR trae el código de una solicitud y la persona puede verla
    SIN_ACCESO,       // la solicitud existe, pero pertenece a otra persona
    NO_ENCONTRADA,    // el código tiene formato de solicitud, pero no existe
    NO_ES_SOLICITUD   // el QR trae otra cosa (por ejemplo, una página web cualquiera)
}

// Resultado de leer un QR: qué texto traía, qué código se encontró y qué pasó
data class QrResult(
    val content: String,                 // texto completo que traía el QR
    val codigo: String?,                 // código de solicitud encontrado (ej: SOL-1001)
    val resultado: ResultadoQr,
    val solicitud: Solicitud? = null     // la solicitud, solo si se encontró y se puede ver
)
