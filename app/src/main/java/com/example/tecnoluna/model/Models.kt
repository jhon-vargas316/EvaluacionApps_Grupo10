package com.example.tecnoluna.model

// Los tres tipos de usuario que tiene la app
enum class Perfil(val etiqueta: String) {
    CLIENTE("Cliente"), ADMIN("Administrador"), TECNICO("Técnico")
}

// Etapas por las que pasa una solicitud. "descripcion" se muestra al usuario para que entienda qué significa.
enum class Estado(val etiqueta: String, val descripcion: String) {
    RECIBIDA("Recibida", "Recibimos la solicitud y pronto será revisada."),
    EN_REVISION("En revisión", "Estamos revisando los datos de la solicitud."),
    PENDIENTE_INFO("Pendiente de información", "Necesitamos más información para poder continuar."),
    ASIGNADA("Asignada", "Ya hay un responsable para esta solicitud."),
    EN_PROCESO("En proceso", "Estamos trabajando en esta solicitud."),
    RESUELTA("Resuelta", "Hay una solución. Falta la confirmación del cliente."),
    CERRADA("Cerrada", "La solicitud terminó."),
    CANCELADA("Cancelada", "La solicitud fue cancelada.");

    // Una solicitud cerrada o cancelada ya no se puede modificar
    val esFinal: Boolean get() = this == CERRADA || this == CANCELADA

    // Solo las solicitudes activas pueden quedar "atrasadas"
    val estaActiva: Boolean get() = !(this == RESUELTA || esFinal)
}

// Prioridad y plazo máximo de atención (en días)
enum class Prioridad(val etiqueta: String, val diasPlazo: Int) {
    BAJA("Baja", 7), MEDIA("Media", 3), ALTA("Alta", 2), URGENTE("Urgente", 1)
}

// Tipos de solicitud que puede elegir el cliente
enum class Categoria(val etiqueta: String) {
    DESARROLLO_WEB("Desarrollo web"),
    MANTENIMIENTO_WEB("Mantenimiento web"),
    INCIDENTE_TECNICO("Incidente técnico"),
    HOSTING("Hosting"),
    SOPORTE_CORREO("Soporte de correo"),
    MARKETING_DIGITAL("Marketing digital"),
    SOLICITUD_CAMBIO("Solicitud de cambio"),
    CONSULTA("Consulta"),
    ACCESO_CONFIG("Acceso o configuración"),
    OTRO("Otro requerimiento")
}

// Datos de una persona que usa la app
data class Usuario(
    val id: String,
    val nombre: String,
    val correo: String,
    val clave: String,
    val perfil: Perfil,
    val empresa: String? = null,
    val telefono: String? = null,
    val activo: Boolean = true
)

// Archivo que se puede adjuntar como evidencia (antes de adjuntarlo)
data class ArchivoAdjunto(
    val nombre: String,
    val tipo: String,
    val tamanoKb: Int,
    val descripcion: String
)

// Archivo ya adjuntado a una solicitud
data class Evidencia(
    val id: String,
    val nombreArchivo: String,
    val tipo: String,
    val tamanoKb: Int,
    val fecha: Long,
    val adjuntadoPor: String,
    val descripcion: String,
    val estadoRevision: String
)

// Mensaje escrito dentro de una solicitud
data class Comentario(
    val id: String,
    val autor: String,
    val perfil: Perfil,
    val texto: String,
    val fecha: Long,
    val interno: Boolean // true = el cliente no lo puede ver
)

// Una línea del historial: guarda qué pasó, quién lo hizo y cuándo
data class EventoHistorial(
    val id: String,
    val solicitudCodigo: String,
    val estadoAnterior: Estado?,
    val estadoNuevo: Estado?,
    val accion: String,
    val comentario: String,
    val perfil: Perfil,
    val usuario: String,
    val fecha: Long,
    val archivo: String? = null,
    val interno: Boolean = false
)

// Aviso que se muestra en la pestaña "Avisos"
data class Notificacion(
    val id: String,
    val destinatarioId: String,
    val mensaje: String,
    val fecha: Long,
    val leida: Boolean
)

// La solicitud completa de un cliente
data class Solicitud(
    val codigo: String,
    val clienteId: String,
    val clienteNombre: String,
    val empresa: String,
    val titulo: String,
    val descripcion: String,
    val categoria: Categoria,
    val prioridad: Prioridad,
    val servicio: String,
    val estado: Estado,
    val creada: Long,
    val actualizada: Long,
    val fechaLimite: Long,
    val responsableId: String? = null,
    val responsableNombre: String? = null,
    val evidencias: List<Evidencia> = emptyList(),
    val comentarios: List<Comentario> = emptyList(),
    val historial: List<EventoHistorial> = emptyList(),
    val fechaResolucion: Long? = null,
    val confirmadaPorCliente: Boolean = false,
    val motivoCancelacion: String? = null
) {
    // Está atrasada si sigue activa y ya pasó la fecha límite
    fun estaAtrasada(ahora: Long = System.currentTimeMillis()): Boolean =
        estado.estaActiva && fechaLimite < ahora

    // Texto listo para compartir por WhatsApp (solo código, título, categoría y enlace)
    fun mensajeWhatsApp(): String = buildString {
        appendLine("Hola, registré una solicitud de soporte:")
        appendLine("Código: $codigo")
        appendLine("Título: $titulo")
        appendLine("Categoría: ${categoria.etiqueta}")
        append("Seguimiento: https://tecnoluna.cl/seguimiento/$codigo")
    }
}
