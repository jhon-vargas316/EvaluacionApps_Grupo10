package com.example.tecnoluna.model

// Reglas de la app: quién puede ver qué y qué cambios puede hacer cada perfil
object Reglas {

    // Cada perfil solo ve las solicitudes que le corresponden
    fun puedeVer(actor: Usuario, s: Solicitud): Boolean = when (actor.perfil) {
        Perfil.ADMIN -> true                          // el administrador ve todas
        Perfil.CLIENTE -> s.clienteId == actor.id     // el cliente solo ve las suyas
        Perfil.TECNICO -> s.responsableId == actor.id // el técnico solo ve las asignadas a él
    }

    // Lista de estados a los que puede pasar la solicitud según quién la mire
    fun transiciones(actor: Usuario, s: Solicitud): List<Estado> = when (actor.perfil) {
        Perfil.ADMIN -> buildList {
            when (s.estado) {
                Estado.RECIBIDA -> add(Estado.EN_REVISION)
                Estado.EN_REVISION, Estado.ASIGNADA, Estado.EN_PROCESO -> add(Estado.PENDIENTE_INFO)
                Estado.RESUELTA -> add(Estado.CERRADA)
                else -> Unit
            }
            // Mientras no esté terminada, se puede cancelar
            if (!s.estado.esFinal) add(Estado.CANCELADA)
        }
        Perfil.TECNICO ->
            if (s.responsableId != actor.id) emptyList()
            else when (s.estado) {
                Estado.ASIGNADA -> listOf(Estado.EN_PROCESO)
                Estado.EN_PROCESO -> listOf(Estado.PENDIENTE_INFO, Estado.RESUELTA)
                else -> emptyList()
            }
        // El cliente no cambia estados: solo confirma, reabre o responde
        Perfil.CLIENTE -> emptyList()
    }

    // Estos cambios obligan a escribir una explicación
    fun requiereComentario(nuevo: Estado) =
        nuevo == Estado.PENDIENTE_INFO || nuevo == Estado.RESUELTA || nuevo == Estado.CANCELADA
}

// Archivos que se pueden adjuntar y sus límites
object Adjuntos {
    const val MAX_KB = 2048                         // máximo 2 MB
    val FORMATOS = setOf("png", "jpg", "pdf")       // formatos permitidos

    // Lista de archivos entre los que el usuario puede elegir
    val disponibles = listOf(
        ArchivoAdjunto("captura_error.png", "png", 320, "Captura de pantalla del problema"),
        ArchivoAdjunto("cotizacion.pdf", "pdf", 540, "Documento con la cotización"),
        ArchivoAdjunto("logo_empresa.jpg", "jpg", 150, "Logo de la empresa"),
        ArchivoAdjunto("respaldo.zip", "zip", 5200, "Archivo comprimido")
    )

    // Devuelve un mensaje de error si el archivo no cumple las reglas (o null si está bien)
    fun validar(a: ArchivoAdjunto): String? = when {
        a.tipo !in FORMATOS -> "El formato .${a.tipo} no está permitido (solo PNG, JPG o PDF)"
        a.tamanoKb > MAX_KB -> "El archivo supera el máximo de 2 MB"
        else -> null
    }
}
