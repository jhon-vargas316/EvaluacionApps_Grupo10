# TecnoLuna

Aplicación móvil Android para la **gestión y el seguimiento de solicitudes de clientes** de una empresa de servicios digitales.

* **Proyecto:** EvaluacionApps\_Grupo10
* **Nombre de la app:** TecnoLuna
* **Tecnologías:** Kotlin · Jetpack Compose · Material 3 · arquitectura MVVM

## Integrantes

|N°|Nombre completo|Rol principal en el desarrollo|
|-|-|-|
|1|Jhon Vargas|Datos, lógica de negocio y lectura de QR|
|2|Elena Veloso|Interfaz, navegación y marcado de entrada/salida|

|Dato|Información|
|-|-|
|Asignatura|Desarrollo de Aplicaciones Moviles\_005D |
|Profesor(a)|Vicente Julio Zapata Valenzuela |
|Grupo|Grupo 10|
|Institución|Duoc Uc|

## Descripción del proyecto

TecnoLuna permite que los clientes de una empresa de servicios digitales (sitios web, hosting, correo, marketing) registren sus solicitudes y las sigan hasta su cierre, mientras el equipo de la empresa las clasifica, prioriza, asigna y resuelve. Cada cambio queda registrado en un historial con fecha, usuario y perfil.

La app tiene **tres perfiles**, y cada uno ve y hace solo lo que le corresponde:

|Perfil|Qué puede hacer|
|-|-|
|**Cliente**|Crear solicitudes, adjuntar evidencia, ver estado e historial de sus solicitudes, responder cuando se le pide información, confirmar la solución o pedir que se reabra, compartir el código por WhatsApp.|
|**Administrador**|Ver todas las solicitudes, buscar y filtrar, clasificar por categoría, definir prioridad, asignar responsable, cambiar estados, comentar, consultar el panel de resumen, marcar entrada y salida.|
|**Técnico**|Ver solo las solicitudes asignadas, comenzar el trabajo, registrar avances con evidencia, pedir más información, marcar como resuelta, marcar entrada y salida.|

## Funcionalidades implementadas

**Solicitudes**

* Creación de solicitudes con título, descripción, categoría, servicio relacionado y evidencia opcional.
* Código único por solicitud (por ejemplo `SOL-1008`), estado inicial «Recibida» y registro de fecha y hora.
* Ocho estados con su descripción en lenguaje simple: Recibida, En revisión, Pendiente de información, Asignada, En proceso, Resuelta, Cerrada y Cancelada.
* Prioridades con plazo de atención y detección de solicitudes **atrasadas**.
* Historial cronológico de cada solicitud y comentarios (incluyendo comentarios internos que el cliente no ve).
* Lista con buscador y filtros por estado, prioridad, cliente, responsable y atrasadas.

**Formularios y validaciones**

* Título obligatorio (mínimo 5 caracteres), descripción obligatoria (mínimo 20), categoría obligatoria.
* Evidencia solo en formato PNG, JPG o PDF y de hasta 2 MB.
* Errores visibles bajo cada campo, controlados desde el ViewModel.

**Perfiles y seguridad de acceso**

* Inicio de sesión con validación de campos y de credenciales.
* Menú de pestañas distinto según el perfil.
* Reglas centralizadas de quién puede ver cada solicitud y a qué estados puede pasar.

**Administración**

* Panel de resumen con totales, pendientes, atrasadas, conteo por estado y barras por prioridad y por responsable.
* Avisos dentro de la app con indicador de no leídos.

**Recursos de la cámara**

* **Escáner de códigos QR:** botón «Escanear QR» en la esquina superior derecha de *Mi perfil*. Lee el código de una solicitud (o su enlace de seguimiento) con CameraX y ML Kit, y muestra código, título, descripción, categoría y estado, con acceso al detalle completo.
* **Marcar entrada y salida** (solo técnicos y administradores): abre la cámara, se toma una foto y recién entonces se registra la hora. La foto no se guarda.
* Solicitud del permiso de cámara, con opción de abrir los ajustes si fue rechazado.

**Diseño**

* Paleta en tonos morados, con modo claro y oscuro.
* Diseño adaptable: menú inferior en celulares y menú lateral en pantallas grandes; listas en una o varias columnas.
* Animaciones de estados, números y barras, e indicadores de carga.

## Arquitectura

La app sigue MVVM con separación real de responsabilidades: las pantallas solo dibujan y nunca acceden directamente al repositorio.

```
app/src/main/java/com/example/tecnoluna/
├── model/        Datos y reglas: Solicitud, Estado, Reglas, Adjuntos, QrResult
├── repository/   SolicitudRepository, QrRepository
├── viewmodel/    AuthViewModel, SolicitudViewModel, NuevaSolicitudViewModel,
│                 AsistenciaViewModel, QrViewModel
├── navigation/   Routes, AppNavigation
├── utils/        QrScanner (CameraX + ML Kit), CameraPermissionHelper
├── ui/
│   ├── theme/        Color, Theme (paleta morada)
│   ├── components/   Componentes reutilizables
│   ├── screens/      Login, Main, Lista, Detalle, Nueva solicitud, Resumen,
│   │                 Avisos, Perfil, Asistencia, Escáner QR
│   └── utils/        Tamaño de pantalla y formato de fechas
└── MainActivity.kt
```

El estado se expone con `StateFlow` y las pantallas lo observan con `collectAsState`, de modo que la interfaz se actualiza sola.

## Requisitos

* Android Studio (versión reciente) con Android SDK instalado.
* JDK incluido con Android Studio.
* Emulador o dispositivo con **Android 13 (API 33) o superior**.
* Una cámara disponible para probar el escáner QR y el marcado de entrada y salida (en el emulador, configurar la cámara como `Webcam0`).
* Conexión a internet solo la primera vez, para que Gradle descargue las dependencias.

## Pasos para ejecutar

1. **Clonar el repositorio**

```bash
   git clone \\\[URL-del-repositorio]
   ```

2. **Abrir el proyecto** en Android Studio: *File > Open* y elegir la carpeta del proyecto `EvaluacionApps\\\_Grupo10`.
3. **Sincronizar Gradle:** esperar a que termine la sincronización automática o usar *Sync Now*. Si Gradle indica una versión no compatible, usar la que sugiera Android Studio.
4. **Preparar el emulador:** en *Device Manager*, crear o editar un dispositivo (por ejemplo Medium Phone, API 33 o superior). En *Additional settings > Camera* elegir `Webcam0` en las cámaras frontal y trasera.
5. **Ejecutar la app:** seleccionar el dispositivo y presionar *Run* (▶). La aplicación se instala con el nombre **TecnoLuna**.
6. **Iniciar sesión** con una de las cuentas de la tabla siguiente.

### Cuentas de acceso (contraseña de todas: `Luna1234`)

|Perfil|Correo|
|-|-|
|Cliente|`camila@tecnoluna.cl` · `diego@tecnoluna.cl`|
|Administrador|`admin@tecnoluna.cl`|
|Técnico|`tomas@tecnoluna.cl` · `tania@tecnoluna.cl`|

En la pantalla de inicio de sesión también hay botones de «Ingreso rápido» que rellenan cada cuenta.



Repositorio:https://github.com/jhon-vargas316/EvaluacionApps\_Grupo10.git

