package com.example.tecnoluna.repository

import com.example.tecnoluna.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

// Aquí viven los datos de la app mientras está abierta.
// Los ViewModels le piden datos y le ordenan cambios; las pantallas nunca lo usan directamente.
object SolicitudRepository {

    private const val DIA = 86_400_000L // un día en milisegundos

    // ---------- Usuarios ----------
    val usuarios: List<Usuario> = listOf(
        Usuario("u-c1", "Camila Soto", "camila@tecnoluna.cl", "Luna1234", Perfil.CLIENTE, "Pyme Andes SpA", "+56 9 1111 0001"),
        Usuario("u-c2", "Diego Vargas", "diego@tecnoluna.cl", "Luna1234", Perfil.CLIENTE, "Tienda Sur Ltda", "+56 9 1111 0002"),
        Usuario("u-a1", "Alma Coordinadora", "admin@tecnoluna.cl", "Luna1234", Perfil.ADMIN),
        Usuario("u-t1", "Tomás Herrera", "tomas@tecnoluna.cl", "Luna1234", Perfil.TECNICO),
        Usuario("u-t2", "Tania Núñez", "tania@tecnoluna.cl", "Luna1234", Perfil.TECNICO)
    )
    val tecnicos: List<Usuario> get() = usuarios.filter { it.perfil == Perfil.TECNICO }
    val clientes: List<Usuario> get() = usuarios.filter { it.perfil == Perfil.CLIENTE }
    private val admins: List<Usuario> get() = usuarios.filter { it.perfil == Perfil.ADMIN }

    // ---------- Listas que las pantallas observan ----------
    // StateFlow avisa a las pantallas cada vez que la lista cambia
    private val _solicitudes = MutableStateFlow<List<Solicitud>>(emptyList())
    val solicitudes: StateFlow<List<Solicitud>> = _solicitudes.asStateFlow()

    private val _notificaciones = MutableStateFlow<List<Notificacion>>(emptyList())
    val notificaciones: StateFlow<List<Notificacion>> = _notificaciones.asStateFlow()

    private var contador = 1000 // sirve para crear códigos: SOL-1001, SOL-1002...

    private fun ahora() = System.currentTimeMillis()
    private fun uuid() = UUID.randomUUID().toString()

    // ---------- Inicio de sesión ----------
    // Busca un usuario con ese correo y contraseña. Si no existe devuelve null.
    fun login(correo: String, clave: String): Usuario? =
        usuarios.firstOrNull { it.activo && it.correo.equals(correo.trim(), ignoreCase = true) && it.clave == clave }

    fun obtener(codigo: String): Solicitud? = _solicitudes.value.firstOrNull { it.codigo == codigo }

    // ---------- Crear solicitud ----------
    fun crear(
        cliente: Usuario, titulo: String, descripcion: String, categoria: Categoria,
        servicio: String, archivo: ArchivoAdjunto?, origen: String = "App TecnoLuna"
    ): Solicitud {
        val t = ahora()
        val codigo = "SOL-${++contador}"        // código único
        val prioridad = Prioridad.MEDIA          // prioridad inicial
        val evidencia = archivo?.aEvidencia(cliente)
        val s = Solicitud(
            codigo = codigo, clienteId = cliente.id, clienteNombre = cliente.nombre,
            empresa = cliente.empresa ?: "Sin empresa", titulo = titulo.trim(),
            descripcion = descripcion.trim(), categoria = categoria, prioridad = prioridad,
            servicio = servicio.trim().ifBlank { "No indicado" }, estado = Estado.RECIBIDA, // siempre parte "Recibida"
            creada = t, actualizada = t, fechaLimite = t + prioridad.diasPlazo * DIA,
            evidencias = listOfNotNull(evidencia),
            historial = listOf(
                EventoHistorial(
                    uuid(), codigo, null, Estado.RECIBIDA, "Solicitud registrada",
                    "Origen: $origen", cliente.perfil, cliente.nombre, t, evidencia?.nombreArchivo
                )
            )
        )
        _solicitudes.update { listOf(s) + it } // la nueva queda primera en la lista
        notificar(admins.map { it.id }, "Nueva solicitud $codigo recibida: ${s.titulo}")
        notificar(listOf(cliente.id), "Tu solicitud $codigo fue registrada (estado: Recibida)")
        return s
    }

    // Registra una solicitud como si hubiera llegado por WhatsApp
    fun registrarDesdeWhatsApp(): Solicitud {
        val c = clientes.random()
        val plantillas = listOf(
            Triple("Cambio de horario en sitio web", "Piden actualizar el horario de atención que aparece en la portada.", Categoria.SOLICITUD_CAMBIO),
            Triple("No puedo acceder al panel de hosting", "El acceso al panel muestra un error al ingresar.", Categoria.ACCESO_CONFIG),
            Triple("Consulta por avance de campaña", "Consultan cómo va la campaña de redes sociales.", Categoria.MARKETING_DIGITAL)
        )
        val (t, d, cat) = plantillas.random()
        return crear(c, t, d, cat, "Sitio web", null, origen = "WhatsApp")
    }

    // ---------- Acciones del administrador ----------
    // Cada función devuelve un texto de error, o null si todo salió bien.
    fun clasificar(codigo: String, actor: Usuario, categoria: Categoria): String? {
        val s = validarAdmin(codigo, actor) ?: return "Solo el administrador puede clasificar"
        if (s.categoria == categoria) return null
        val accion = "Categoría cambiada de ${s.categoria.etiqueta} a ${categoria.etiqueta}"
        aplicar(codigo, actor, accion, avisar = null) { it.copy(categoria = categoria) }
        return null
    }

    fun definirPrioridad(codigo: String, actor: Usuario, prioridad: Prioridad): String? {
        val s = validarAdmin(codigo, actor) ?: return "Solo el administrador puede definir prioridades"
        if (s.prioridad == prioridad) return null
        val accion = "Prioridad cambiada de ${s.prioridad.etiqueta} a ${prioridad.etiqueta}"
        aplicar(codigo, actor, accion, avisar = "prioridad actualizada a ${prioridad.etiqueta}") {
            // Al cambiar la prioridad también cambia la fecha límite
            it.copy(prioridad = prioridad, fechaLimite = it.creada + prioridad.diasPlazo * DIA)
        }
        return null
    }

    fun asignar(codigo: String, actor: Usuario, tecnicoId: String): String? {
        val s = validarAdmin(codigo, actor) ?: return "Solo el administrador puede asignar responsables"
        val tec = tecnicos.firstOrNull { it.id == tecnicoId } ?: return "Técnico no válido"
        if (s.responsableId == tec.id) return null
        // Si estaba recién recibida, pasa a "Asignada"
        val nuevoEstado = if (s.estado == Estado.RECIBIDA || s.estado == Estado.EN_REVISION) Estado.ASIGNADA else s.estado
        val accion = if (s.responsableId == null) "Asignada a ${tec.nombre}"
        else "Reasignada de ${s.responsableNombre} a ${tec.nombre}"
        aplicar(codigo, actor, accion, nuevoEstado, avisar = accion.replaceFirstChar { it.lowercase() }) {
            it.copy(responsableId = tec.id, responsableNombre = tec.nombre)
        }
        return null
    }

    // Comprueba que quien actúa sea administrador y que la solicitud siga abierta
    private fun validarAdmin(codigo: String, actor: Usuario): Solicitud? {
        if (actor.perfil != Perfil.ADMIN) return null
        return obtener(codigo)?.takeIf { !it.estado.esFinal }
    }

    // ---------- Cambiar el estado (administrador o técnico) ----------
    fun cambiarEstado(codigo: String, actor: Usuario, nuevo: Estado, comentario: String): String? {
        val s = obtener(codigo) ?: return "Solicitud no encontrada"
        if (nuevo !in Reglas.transiciones(actor, s)) return "Tu perfil no puede pasar la solicitud a \"${nuevo.etiqueta}\""
        // Algunos cambios necesitan una explicación escrita
        if (Reglas.requiereComentario(nuevo) && comentario.isBlank()) {
            return when (nuevo) {
                Estado.PENDIENTE_INFO -> "Escribe qué información adicional se necesita"
                Estado.RESUELTA -> "Escribe la solución que se realizó"
                else -> "Escribe el motivo de la cancelación"
            }
        }
        val txt = comentario.trim()
        aplicar(codigo, actor, "Cambio de estado a ${nuevo.etiqueta}", nuevo, txt,
            avisar = "ahora está ${nuevo.etiqueta.lowercase()}") {
            it.copy(
                fechaResolucion = if (nuevo == Estado.RESUELTA) ahora() else it.fechaResolucion,
                motivoCancelacion = if (nuevo == Estado.CANCELADA) txt else it.motivoCancelacion
            )
        }
        return null
    }

    // ---------- Comentarios y avances ----------
    fun comentar(codigo: String, actor: Usuario, texto: String, interno: Boolean): String? {
        val s = obtener(codigo) ?: return "Solicitud no encontrada"
        if (!Reglas.puedeVer(actor, s)) return "No tienes acceso a esta solicitud"
        if (texto.isBlank()) return "Escribe un comentario"
        if (actor.perfil == Perfil.CLIENTE && (interno || s.estado.esFinal)) return "No puedes comentar en este estado"
        val c = Comentario(uuid(), actor.nombre, actor.perfil, texto.trim(), ahora(), interno)
        aplicar(
            codigo, actor,
            if (interno) "Comentario interno registrado" else "Comentario de seguimiento",
            comentario = if (interno) "" else texto.trim(), // los internos no muestran su texto en el historial
            avisar = if (interno) null else "nuevo comentario de ${actor.nombre}",
            internoEvento = interno
        ) { it.copy(comentarios = it.comentarios + c) }
        return null
    }

    fun registrarAvance(codigo: String, actor: Usuario, texto: String, archivo: ArchivoAdjunto?): String? {
        val s = obtener(codigo) ?: return "Solicitud no encontrada"
        if (actor.perfil != Perfil.TECNICO || s.responsableId != actor.id) return "Solo el técnico asignado puede registrar avances"
        if (s.estado != Estado.ASIGNADA && s.estado != Estado.EN_PROCESO) return "No se pueden registrar avances en estado \"${s.estado.etiqueta}\""
        if (texto.isBlank()) return "Describe el avance realizado"
        val ev = archivo?.let { a -> Adjuntos.validar(a)?.let { return it }; a.aEvidencia(actor) }
        val c = Comentario(uuid(), actor.nombre, actor.perfil, texto.trim(), ahora(), false)
        aplicar(codigo, actor, "Avance registrado", comentario = texto.trim(), evidencia = ev,
            avisar = "nuevo avance registrado") {
            it.copy(comentarios = it.comentarios + c, evidencias = it.evidencias + listOfNotNull(ev))
        }
        return null
    }

    // ---------- Acciones del cliente ----------
    // El cliente responde cuando le piden más información
    fun responderInfo(codigo: String, actor: Usuario, texto: String, archivo: ArchivoAdjunto?): String? {
        val s = obtener(codigo) ?: return "Solicitud no encontrada"
        if (actor.perfil != Perfil.CLIENTE || s.clienteId != actor.id) return "Solo el cliente titular puede responder"
        if (s.estado != Estado.PENDIENTE_INFO) return "La solicitud no está esperando información"
        if (texto.isBlank()) return "Escribe tu respuesta"
        val ev = archivo?.let { a -> Adjuntos.validar(a)?.let { return it }; a.aEvidencia(actor) }
        val c = Comentario(uuid(), actor.nombre, actor.perfil, texto.trim(), ahora(), false)
        // Si ya tiene responsable vuelve a "En proceso"; si no, a "En revisión"
        val destino = if (s.responsableId != null) Estado.EN_PROCESO else Estado.EN_REVISION
        aplicar(codigo, actor, "Cliente aportó información adicional", destino, texto.trim(), ev,
            avisar = "el cliente respondió la solicitud de información") {
            it.copy(comentarios = it.comentarios + c, evidencias = it.evidencias + listOfNotNull(ev))
        }
        return null
    }

    // El cliente acepta la solución y la solicitud se cierra
    fun confirmarConformidad(codigo: String, actor: Usuario): String? {
        val s = obtener(codigo) ?: return "Solicitud no encontrada"
        if (actor.perfil != Perfil.CLIENTE || s.clienteId != actor.id) return "Solo el cliente titular puede confirmar"
        if (s.estado != Estado.RESUELTA) return "Solo se puede confirmar una solicitud resuelta"
        aplicar(codigo, actor, "Cliente confirmó conformidad", Estado.CERRADA, "Solución aceptada",
            avisar = "cerrada: el cliente aceptó la solución") { it.copy(confirmadaPorCliente = true) }
        return null
    }

    // El cliente pide volver a abrir una solicitud resuelta o cerrada
    fun reabrir(codigo: String, actor: Usuario, motivo: String): String? {
        val s = obtener(codigo) ?: return "Solicitud no encontrada"
        if (actor.perfil != Perfil.CLIENTE || s.clienteId != actor.id) return "Solo el cliente titular puede reabrir"
        if (s.estado != Estado.RESUELTA && s.estado != Estado.CERRADA) return "Solo se reabren solicitudes resueltas o cerradas"
        if (motivo.isBlank()) return "Escribe el motivo de la reapertura"
        aplicar(codigo, actor, "Solicitud reabierta por el cliente", Estado.EN_REVISION, motivo.trim(),
            avisar = "reabierta por el cliente") {
            it.copy(confirmadaPorCliente = false, fechaResolucion = null,
                fechaLimite = ahora() + it.prioridad.diasPlazo * DIA)
        }
        return null
    }

    // Deja en el historial que se compartió la referencia por WhatsApp
    fun registrarEnvioWhatsApp(codigo: String, actor: Usuario): String? {
        val s = obtener(codigo) ?: return "Solicitud no encontrada"
        if (!Reglas.puedeVer(actor, s)) return "No tienes acceso a esta solicitud"
        aplicar(codigo, actor, "Referencia compartida por WhatsApp", avisar = null) { it }
        return null
    }

    fun marcarLeidas(usuarioId: String) {
        _notificaciones.update { l -> l.map { if (it.destinatarioId == usuarioId) it.copy(leida = true) else it } }
    }


    // Todos los cambios pasan por aquí: aplica el cambio, agrega la línea al historial y avisa a los involucrados.
    private fun aplicar(
        codigo: String, actor: Usuario, accion: String,
        nuevoEstado: Estado? = null, comentario: String = "", evidencia: Evidencia? = null,
        avisar: String?, internoEvento: Boolean = false,
        cambio: (Solicitud) -> Solicitud
    ) {
        var resultado: Solicitud? = null
        _solicitudes.update { lista ->
            lista.map { s ->
                if (s.codigo != codigo) s else {
                    val t = ahora()
                    val estadoFinal = nuevoEstado ?: s.estado
                    // Nueva línea del historial (fecha, usuario y perfil de quien hizo el cambio)
                    val evento = EventoHistorial(
                        uuid(), codigo, s.estado, estadoFinal, accion, comentario,
                        actor.perfil, actor.nombre, t, evidencia?.nombreArchivo, internoEvento
                    )
                    cambio(s).copy(estado = estadoFinal, actualizada = t, historial = s.historial + evento)
                        .also { resultado = it }
                }
            }
        }
        val r = resultado ?: return
        if (avisar != null) {
            // Se avisa al cliente, al técnico y a los administradores (menos a quien hizo el cambio)
            val dest = (listOfNotNull(r.clienteId, r.responsableId) + admins.map { it.id }).filter { it != actor.id }
            notificar(dest, "${r.codigo}: $avisar")
        }
    }

    private fun notificar(ids: Collection<String>, mensaje: String) {
        if (ids.isEmpty()) return
        val t = ahora()
        val nuevas = ids.distinct().map { Notificacion(uuid(), it, mensaje, t, false) }
        _notificaciones.update { nuevas + it }
    }

    // Convierte un archivo elegido en una evidencia de la solicitud
    private fun ArchivoAdjunto.aEvidencia(actor: Usuario) = Evidencia(
        uuid(), nombre, tipo, tamanoKb, ahora(), actor.nombre, descripcion, "Pendiente de revisión"
    )

    // ---------- Solicitudes iniciales ----------
    // Crea una solicitud ya avanzada para que la lista no parta vacía
    private fun solicitudInicial(
        num: Int, cliente: Usuario, titulo: String, desc: String, cat: Categoria, prio: Prioridad,
        estado: Estado, resp: Usuario?, diasAtras: Int, servicio: String,
        evidencia: ArchivoAdjunto? = null
    ): Solicitud {
        val codigo = "SOL-$num"
        val creada = ahora() - diasAtras * DIA
        val coord = admins.first()
        // Historial básico: registrada -> asignada -> estado actual
        val hist = mutableListOf(
            EventoHistorial(uuid(), codigo, null, Estado.RECIBIDA, "Solicitud registrada",
                "Origen: WhatsApp", Perfil.CLIENTE, cliente.nombre, creada)
        )
        if (resp != null) hist += EventoHistorial(uuid(), codigo, Estado.RECIBIDA, Estado.ASIGNADA,
            "Asignada a ${resp.nombre}", "", Perfil.ADMIN, coord.nombre, creada + DIA / 4)
        if (estado != Estado.RECIBIDA && estado != Estado.ASIGNADA) hist += EventoHistorial(
            uuid(), codigo, if (resp != null) Estado.ASIGNADA else Estado.RECIBIDA, estado,
            "Cambio de estado a ${estado.etiqueta}", "",
            if (resp != null) Perfil.TECNICO else Perfil.ADMIN, resp?.nombre ?: coord.nombre, creada + DIA / 2
        )
        val actualizada = hist.last().fecha
        return Solicitud(
            codigo = codigo, clienteId = cliente.id, clienteNombre = cliente.nombre,
            empresa = cliente.empresa ?: "", titulo = titulo, descripcion = desc, categoria = cat,
            prioridad = prio, servicio = servicio, estado = estado, creada = creada,
            actualizada = actualizada, fechaLimite = creada + prio.diasPlazo * DIA,
            responsableId = resp?.id, responsableNombre = resp?.nombre,
            evidencias = listOfNotNull(evidencia?.aEvidencia(cliente)),
            historial = hist,
            fechaResolucion = if (estado == Estado.RESUELTA || estado == Estado.CERRADA) actualizada else null,
            confirmadaPorCliente = estado == Estado.CERRADA
        )
    }

    private fun cargarSolicitudesIniciales() {
        val (c1, c2) = clientes
        val (t1, t2) = tecnicos
        _solicitudes.value = listOf(
            solicitudInicial(1007, c1, "Crear formulario de contacto", "Se necesita un formulario simple en la página de inicio.", Categoria.DESARROLLO_WEB, Prioridad.ALTA, Estado.EN_REVISION, null, 1, "Sitio web corporativo"),
            solicitudInicial(1006, c2, "Consulta por certificado SSL", "¿Cuándo vence el certificado del sitio de la tienda?", Categoria.CONSULTA, Prioridad.BAJA, Estado.CERRADA, t1, 10, "tiendasur.cl"),
            solicitudInicial(1005, c1, "Renovación de hosting", "Renovar el plan de hosting anual del sitio web.", Categoria.HOSTING, Prioridad.MEDIA, Estado.RESUELTA, t2, 4, "Hosting anual"),
            solicitudInicial(1004, c2, "Cotización de campaña digital", "Solicito una propuesta de campaña en redes sociales.", Categoria.MARKETING_DIGITAL, Prioridad.BAJA, Estado.RECIBIDA, null, 0, "Marketing digital"),
            solicitudInicial(1003, c2, "Correo corporativo no recibe mensajes", "Las cuentas de la tienda no reciben correos desde ayer.", Categoria.SOPORTE_CORREO, Prioridad.ALTA, Estado.PENDIENTE_INFO, t1, 3, "Correo corporativo"),
            solicitudInicial(1002, c1, "Cambiar banner de portada", "Reemplazar el banner por la promoción de temporada.", Categoria.SOLICITUD_CAMBIO, Prioridad.MEDIA, Estado.ASIGNADA, t2, 2, "pymeandes.cl", Adjuntos.disponibles[2]),
            solicitudInicial(1001, c1, "Sitio web caído tras actualización", "El sitio muestra error 500 desde la última actualización.", Categoria.INCIDENTE_TECNICO, Prioridad.URGENTE, Estado.EN_PROCESO, t1, 6, "pymeandes.cl", Adjuntos.disponibles[0])
        )
        contador = 1007
        notificar(listOf("u-a1"), "Nueva solicitud SOL-1004 recibida: Cotización de campaña digital")
        notificar(listOf("u-t1"), "SOL-1001: asignada a Tomás Herrera")
        notificar(listOf("u-c1"), "SOL-1005: resuelta, por favor confirma tu conformidad")
        notificar(listOf("u-c2"), "SOL-1003: se necesita información adicional")
    }

    init { cargarSolicitudesIniciales() }
}
