# Despliegue y contenerización

## Por qué hay un Dockerfile (justificación para `CICD-08`)

El estándar `EST-TEC-01` clasifica la contenerización como **condicionada**:

> `CICD-08` — "La contenedorización es obligatoria solo cuando mejora reproducibilidad
> o corresponde al entorno." Evidencia: Dockerfile / justificación.

**Justificación de FinMind:** la plataforma elegida para publicar la API despliega a partir
de un contenedor. Además, la imagen resuelve `PROD-04` de forma directa — quien evalúe no
necesita instalar JDK 21, Maven ni PostgreSQL — y `CICD-01`, porque la construcción es un solo
comando reproducible.

## Qué hace el Dockerfile

Dos etapas:

1. **build** — sobre `maven:3.9.9-eclipse-temurin-21`, compila y empaqueta el `.jar`.
   El `pom.xml` se copia antes que `src/` para que Docker cachee las dependencias:
   si el pom no cambia, no vuelve a descargar Maven Central en cada build.
2. **runtime** — sobre `eclipse-temurin:21-jre-alpine`, copia solo el `.jar`.
   La imagen final no contiene el JDK, ni Maven, ni el código fuente.

Dos decisiones que hay que poder sustentar:

- **No corre como root.** Se crea el usuario `finmind` (`SEG-09`, mínimo privilegio).
- **`-XX:MaxRAMPercentage=75`.** Sin esto, en un contenedor de 512 MB la JVM asume que
  tiene toda la memoria de la máquina y el proceso muere por falta de memoria. Con
  `UseSerialGC` además se reduce el consumo, que en instancias pequeñas importa.

**Las credenciales no están en la imagen.** Se inyectan como variables de entorno al
ejecutar (`ARQ-03`, `SEG-02`). El `.dockerignore` excluye `.env` explícitamente: sin ese
archivo, un `docker build` metería las credenciales dentro de la imagen.

## Construir y correr localmente

```bash
docker build -t finmind:0.1.0 .

docker run --rm -p 8080:8080 \
  -e DB_URL="jdbc:postgresql://HOST.neon.tech/finmind?sslmode=require" \
  -e DB_USERNAME="usuario_de_neon" \
  -e DB_PASSWORD="..." \
  -e JWT_SECRET="..." \
  -e CORS_ALLOWED_ORIGINS="http://localhost:5173" \
  finmind:0.1.0
```

## Entorno local completo sin instalar MySQL

```bash
docker compose up --build
```

Levanta PostgreSQL 16 y la API juntos. PostgreSQL queda en el puerto **5433** del host
para no chocar con una instalación previa. Flyway aplica las migraciones al arrancar.

Requiere un `JWT_SECRET` definido en el `.env`.

---

# Despliegue público

La base de datos **ya está en la nube** (Neon). Faltan la API y el cliente.

```
   navegador
       │
       │  todo contra  https://finmind.vercel.app
       ▼
 ┌─────────────────────┐
 │  Vercel             │   cliente React (estático)
 │                     │
 │  /api/*  ───────────┼──► Render  (Spring Boot, desde el Dockerfile)
 └─────────────────────┘          │
                                  ▼
                            Neon (PostgreSQL 18)
```

## Por qué el cliente hace de proxy y no se llama a la API directo

Es la decisión más importante de este despliegue y la que más se equivoca.

La sesión viaja en una cookie `HttpOnly` con `SameSite=Lax` (SEG-08). **`Lax`
significa que el navegador no manda la cookie cuando la petición sale hacia otro
sitio.** Si el cliente en `finmind.vercel.app` llamara directo a
`finmind.onrender.com`, el navegador guardaría la cookie del login y no la
enviaría nunca más: la aplicación cargaría perfecta y el inicio de sesión
fallaría sin ningún mensaje que explique la causa.

La salida obvia sería poner la cookie en `SameSite=None`. **No se hace**, y la
razón hay que poder sustentarla: el argumento por el que esta API no lleva token
anti-CSRF es exactamente que `Lax` no manda la cookie desde otro sitio y que
todos los endpoints que modifican datos son `POST`, `PUT`, `PATCH` o `DELETE`.
Con `None` ese argumento se cae y habría que agregar protección CSRF de verdad,
tocando cliente y servidor.

Con el *rewrite* de `frontend/vercel.json`, el navegador solo habla con
`finmind.vercel.app`. Vercel reenvía `/api/*` al backend **desde su servidor**,
no desde el navegador. Para el navegador todo es el mismo sitio: la cookie sigue
siendo *same-site*, `Lax` sigue valiendo, el argumento de CSRF sigue en pie y
CORS deja de hacer falta.

## Paso 1 — la API en Render

Opciones con plan gratuito real, verificadas en 2026:

| Plataforma | Qué da gratis | Advertencia |
|---|---|---|
| **Render** | 750 h/mes, 512 MB RAM, sin tarjeta | **Se duerme a los 15 min** sin tráfico; el primer acceso tarda 30–50 s |
| **Koyeb** | 1 instancia, 512 MB RAM, 0.1 vCPU | No se duerme, pero 0.1 vCPU hace lento el arranque de Spring Boot |
| **Railway** | 5 USD de crédito + 1 USD/mes | El crédito se agota en una o dos semanas |
| **DigitalOcean** | 200 USD con el GitHub Student Pack | Pide tarjeta para verificar |
| **Azure** | 100 USD con el GitHub Student Pack | App Service acepta contenedor |

1. `render.com` → **New** → **Web Service** → conectar el repositorio.
2. Rama **`main`**. Runtime **Docker** (detecta el `Dockerfile`; no hay que
   configurar build command).
3. Variables de entorno — **las mismas del `.env`, escritas a mano en el panel de
   Render.** El `.env` no se sube al repositorio y el `.dockerignore` lo excluye
   de la imagen a propósito:

   | Variable | Valor |
   |---|---|
   | `SPRING_PROFILES_ACTIVE` | `prod,google` |
   | `DB_URL` | la de Neon, con `?sslmode=require` |
   | `DB_USERNAME` / `DB_PASSWORD` | las de Neon |
   | `JWT_SECRET` | **uno nuevo, distinto al de desarrollo** |
   | `JWT_EXPIRATION_MS` | `3600000` |
   | `MAIL_*` | las mismas del `.env` |
   | `CAPTCHA_ENABLED` / `CAPTCHA_SECRET` | las de Cloudflare |
   | `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | las de Google Cloud |
   | `OAUTH2_SUCCESS_URL` | `https://TU-APP.vercel.app/oauth2/callback` |
   | `CORS_ALLOWED_ORIGINS` | `https://TU-APP.vercel.app` |

   > El `JWT_SECRET` de producción **tiene que ser otro**. Si se reutiliza el de
   > desarrollo, cualquiera que haya visto ese `.env` puede firmar un token
   > válido contra el sistema publicado.

4. Health check: `/actuator/health`.

Anotá la URL que te da Render. Es la que va en el paso 2.

## Paso 2 — el cliente en Vercel

1. Editar `frontend/vercel.json` y reemplazar `CAMBIA-ESTO.onrender.com` por el
   host real del paso 1. No es un secreto: es una dirección pública, y Vercel no
   admite variables de entorno dentro de `vercel.json`.
2. `vercel.com` → **Add New Project** → el repositorio.
   **Root Directory: `frontend`.** Framework: Vite.
3. Variables de entorno del proyecto en Vercel:

   | Variable | Valor |
   |---|---|
   | `VITE_API_URL` | `/api/v1` ← **relativa, sin dominio.** Eso es lo que hace que pase por el proxy |
   | `VITE_CAPTCHA_SITE_KEY` | la clave **pública** de Turnstile |
   | `VITE_GOOGLE_HABILITADO` | `true` |

4. Deploy.

## Paso 3 — Google y Cloudflare con el dominio nuevo

Los dos validan el dominio desde el que se les llama. Sin esto el botón de
Google y el CAPTCHA fallan solo en producción.

- **Google Cloud Console** → Credenciales → el ID de OAuth 2.0 → agregar a
  *URIs de redireccionamiento autorizados*:
  `https://TU-BACKEND.onrender.com/login/oauth2/code/google`
- **Cloudflare Turnstile** → el sitio → agregar el dominio `TU-APP.vercel.app`.

## Verificación, en este orden

1. `https://TU-BACKEND.onrender.com/actuator/health` → `{"status":"UP"}`.
   Si tarda 40 s la primera vez, es el arranque en frío, no un fallo.
2. `https://TU-APP.vercel.app` carga la pantalla de inicio de sesión.
3. Entrar con correo y contraseña. **Recargar con F5.** Si la sesión sobrevive,
   la cookie está viajando bien y el proxy quedó correcto. Si te devuelve al
   login, el `VITE_API_URL` quedó con dominio absoluto en vez de `/api/v1`.
4. Entrar con Google.
5. Registrar una cuenta nueva y comprobar que el CAPTCHA aparece.

## Para la sustentación

**Encendé la API diez minutos antes** entrando a la URL. El arranque en frío de
Render es de 30–50 segundos y ocurre una sola vez; si esperás a que el instructor
abra el enlace, ese medio minuto de pantalla en blanco lo va a leer como que la
aplicación no funciona.

## Deuda declarada de este despliegue

- **Swagger queda abierto** (`finmind.swagger.publico=true`). Es a propósito,
  para que el evaluador pruebe la API. En un despliegue real iría cerrado
  (SEG-07).
- **Render se duerme.** Con el crédito del GitHub Student Pack en DigitalOcean o
  Azure no pasaría, pero exige tarjeta de verificación.
- **Sin dominio propio.** El Student Pack da uno gratis en Namecheap; no se usa
  porque cambiar el dominio obliga a rehacer los pasos 2 y 3.
