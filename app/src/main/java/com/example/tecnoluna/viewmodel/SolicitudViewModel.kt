package com.example.tecnoluna.viewmodel

import androidx.lifecycle.ViewModel
import com.example.tecnoluna.model.*
import com.example.tecnoluna.repository.SolicitudRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// Opciones elegidas en los filtros de la lista
data class Filtros(
    val query: String = "",
    val estado: Estado? = null,
    val prioridad: Prioridad? = null,
    val clienteId: String? = null,
    val responsableId: String? = null,
    val soloAtrasadas: Boolean = false
)

// Números que se muestran en el panel de resumen
data class Resumen(
    val total: Int,
    val porEstado: Map<Estado, Int>,
    val porPrioridad: Map<Prioridad, Int>,
    val porResponsable: Map<String, Int>,
    val pendientes: Int,
    val atrasadas: Int
)

// Conecta las pantallas de solicitudes con el repositorio
class SolicitudViewModel : ViewModel() {

    private val repo = SolicitudRepository

    // Listas que las pantallas observan
    val solicitudes: StateFlow<List<Solicitud>> = repo.solicitudes
    val notificaciones: StateFlow<List<Notificacion>> = repo.notificaciones
    val tecnicos: List<Usuario> get() = repo.tecnicos
    val clientes: List<Usuario> get() = repo.clientes

    // Filtros activos
    private val _filtros = MutableStateFlow(Filtros())
    val filtros: StateFlow<Filtros> = _filtros.asStateFlow()
    fun actualizarFiltros(cambio: (Filtros) -> Filtros) = _filtros.update(cambio)
    fun limpiarFiltros() { _filtros.value = Filtros() }

    // Mensajes cortos que se muestran abajo de la pantalla (Snackbar)
    private val _mensaje = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val mensaje: SharedFlow<String> = _mensaje.asSharedFlow()

    // Ejecuta una acción del repositorio. Si falla, muestra el error; si sale bien, muestra el mensaje "ok".
    private fun ejecutar(ok: String, op: () -> String?): Boolean {
        val error = op()
        _mensaje.tryEmit(error ?: ok)
        return error == null
    }

    // ---------- Consultas ----------
    // Deja solo las solicitudes que el usuario puede ver y que cumplen los filtros
    fun filtrar(usuario: Usuario, todas: List<Solicitud>, f: Filtros): List<Solicitud> {
        val q = f.query.trim().lowercase()
        return todas.filter { Reglas.puedeVer(usuario, it) }
            .filter { f.estado == null || it.estado == f.estado }
            .filter { f.prioridad == null || it.prioridad == f.prioridad }
            .filter { f.clienteId == null || it.clienteId == f.clienteId }
            .filter { f.responsableId == null || it.responsableId == f.responsableId }
            .filter { !f.soloAtrasadas || it.estaAtrasada() }
            .filter {
                q.isEmpty() || it.codigo.lowercase().contains(q) || it.titulo.lowercase().contains(q) ||
                    it.clienteNombre.lowercase().contains(q) || it.empresa.lowercase().contains(q)
            }
            .sortedByDescending { it.actualizada } // las más recientes primero
    }

    // Cuenta las solicitudes por estado, prioridad y responsable
    fun calcularResumen(todas: List<Solicitud>): Resumen = Resumen(
        total = todas.size,
        porEstado = Estado.values().associateWith { e -> todas.count { it.estado == e } },
        porPrioridad = Prioridad.values().associateWith { p -> todas.count { it.prioridad == p } },
        porResponsable = todas.groupingBy { it.responsableNombre ?: "Sin asignar" }.eachCount(),
        pendientes = todas.count {
            it.estado in listOf(Estado.RECIBIDA, Estado.EN_REVISION, Estado.PENDIENTE_INFO, Estado.ASIGNADA)
        },
        atrasadas = todas.count { it.estaAtrasada() }
    )

    // ---------- Acciones (devuelven true si se pudieron hacer) ----------
    fun clasificar(codigo: String, actor: Usuario, c: Categoria) =
        ejecutar("Categoría actualizada") { repo.clasificar(codigo, actor, c) }

    fun definirPrioridad(codigo: String, actor: Usuario, p: Prioridad) =
        ejecutar("Prioridad actualizada") { repo.definirPrioridad(codigo, actor, p) }

    fun asignar(codigo: String, actor: Usuario, tecnicoId: String) =
        ejecutar("Responsable asignado") { repo.asignar(codigo, actor, tecnicoId) }

    fun cambiarEstado(codigo: String, actor: Usuario, nuevo: Estado, comentario: String) =
        ejecutar("Estado actualizado a ${nuevo.etiqueta}") { repo.cambiarEstado(codigo, actor, nuevo, comentario) }

    fun comentar(codigo: String, actor: Usuario, texto: String, interno: Boolean) =
        ejecutar("Comentario agregado") { repo.comentar(codigo, actor, texto, interno) }

    fun registrarAvance(codigo: String, actor: Usuario, texto: String, archivo: ArchivoAdjunto?) =
        ejecutar("Avance registrado") { repo.registrarAvance(codigo, actor, texto, archivo) }

    fun responderInfo(codigo: String, actor: Usuario, texto: String, archivo: ArchivoAdjunto?) =
        ejecutar("Información enviada") { repo.responderInfo(codigo, actor, texto, archivo) }

    fun confirmar(codigo: String, actor: Usuario) =
        ejecutar("¡Gracias! La solicitud quedó cerrada") { repo.confirmarConformidad(codigo, actor) }

    fun reabrir(codigo: String, actor: Usuario, motivo: String) =
        ejecutar("Solicitud reabierta") { repo.reabrir(codigo, actor, motivo) }

    fun registrarEnvioWhatsApp(codigo: String, actor: Usuario) =
        ejecutar("Referencia compartida por WhatsApp") { repo.registrarEnvioWhatsApp(codigo, actor) }

    fun registrarSolicitudDeWhatsApp() {
        val s = repo.registrarDesdeWhatsApp()
        _mensaje.tryEmit("Llegó la solicitud ${s.codigo} por WhatsApp")
    }

    fun marcarLeidas(usuario: Usuario) = repo.marcarLeidas(usuario.id)
}
