# FrameNote

Plataforma de revisión colaborativa de videos. Permite guardar observaciones con texto y dibujos sobre un instante, responder y dar seguimiento por versión.

## Ejecutar el proyecto

Requisitos: Java 21, Maven y Docker Desktop.

Desde la carpeta del proyecto:

~~~powershell
mvn clean package
docker compose up -d --build
~~~

- Aplicación: http://127.0.0.1:3000/
- API: http://127.0.0.1:8080/api
- Documentación: http://127.0.0.1:8080/swagger-ui/index.html

Docker ejecuta frontend, backend y PostgreSQL por separado. El Dockerfile de backend utiliza el JAR generado por Maven, por lo que la compilación debe completarse antes de construir la imagen.

Para arrancar únicamente PostgreSQL y backend:

~~~powershell
docker compose up -d postgres backend
~~~

## Recorrido principal

1. Registrar una cuenta e iniciar sesión.
2. Abrir el espacio y crear un proyecto.
3. Crear un video y subir un archivo MP4.
4. Pausar, dibujar o escribir una observación y guardarla.
5. Abrir la bandera, responder y resolver la observación.
6. Publicar otra versión y consultar las anteriores.

El propietario agrega cuentas registradas como EDITOR o REVIEWER. Los permisos se verifican en el backend.

El borrado requiere confirmación en la interfaz. El propietario puede borrar proyectos editables. El propietario y el editor pueden borrar videos y versiones de proyectos editables. Los archivos se limpian en segundo plano y su cuota se libera al completar la limpieza. Los proyectos aprobados y archivados mantienen la protección de solo lectura.

## Probar la API con Hoppscotch

Registrar mediante POST /api/auth/register:

~~~json
{
  "email": "demo@example.com",
  "name": "Usuario de prueba",
  "password": "FrameNoteDemo2026"
}
~~~

La respuesta contiene token, workspaceId y accountId. Usa token como Bearer Token en las siguientes peticiones.

- GET /api/workspace-access consulta espacios propios y compartidos.
- POST /api/workspaces/{workspaceId}/projects recibe {"name":"Proyecto de prueba"}.
- POST /api/projects/{projectId}/videos recibe {"title":"Primer corte"}.
- POST /api/videos/{videoId}/upload recibe multipart/form-data con file (archivo MP4) y durationMs (entero de texto, por ejemplo 60000 para 60 segundos).
- POST /api/versions/{versionId}/annotations recibe {"timestampMs":1000,"text":"Revisar el audio"}.

Usa el identificador del recurso correcto en cada ruta. El id de la respuesta de carga identifica la versión. El campo videoId identifica el video.

## Estructura

- frontend/dist/index.html: documento HTML de entrada.
- frontend/dist/app.js: vistas, navegación, reproductor, dibujos y comunicación con la API.
- frontend/dist/styles.css: estilos.
- src/main/java/com/framenote/domain: modelos y reglas Java independientes de JPA.
- src/main/java/com/framenote/application: casos de uso y contratos en port.
- src/main/java/com/framenote/infrastructure: adaptadores Spring Data y limpieza de medios.
- src/main/java/com/framenote/api: controladores REST.
- src/main/java/com/framenote/security: configuración de seguridad y JWT.
- src/main/resources/META-INF/orm.xml: mapeo de persistencia.

La inversión Onion está aplicada a persistencia. Algunos casos de uso todavía dependen de Spring para JWT, transacciones y archivos.

## Pruebas

~~~powershell
mvn test
~~~

Las pruebas existentes verifican políticas de tiempo, coordenadas, cuota y estados del dominio.

## Datos locales y configuración

El repositorio contiene código y configuración de desarrollo. Los videos, la base de datos, credenciales locales, dependencias y compilaciones se generan o conservan fuera de Git.

PostgreSQL mantiene sus datos en el volumen pgdata. Los MP4 se guardan en media, montado como /media en el contenedor. El frontend calcula la duración desde el navegador. El backend valida el encabezado MP4 y confía en la duración recibida.

Los valores predeterminados de contraseña de PostgreSQL y clave JWT son de desarrollo. Configura valores propios para otro entorno. La aplicación permite personalizar la conexión mediante DB_URL, DB_USERNAME, DB_PASSWORD y la firma mediante JWT_SECRET.

Los planes comerciales, enlaces temporales, retención automática, validación multimedia avanzada y reapertura de proyectos quedan pendientes.
