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

## Publicar la API

Opciones con plan gratuito real, verificadas en 2026:

| Plataforma | Qué da gratis | Advertencia |
|---|---|---|
| **Render** | 750 horas/mes, 512 MB RAM, CPU compartida, sin tarjeta | **Se duerme a los 15 minutos** sin tráfico; el primer acceso tarda 30–50 s |
| **Koyeb** | 1 instancia libre, 512 MB RAM, 0.1 vCPU, 2 GB SSD | No se duerme, pero 0.1 vCPU hace lento el arranque de Spring Boot |
| **Railway** | 5 USD de crédito inicial + 1 USD/mes | El crédito se agota en una o dos semanas de uso continuo |

**Recomendación para la sustentación:** Render, y **encenderlo diez minutos antes**
entrando a la URL. El arranque en frío de 30–50 segundos ocurre una sola vez; si esperás
a que el instructor abra el link, ese medio minuto de pantalla en blanco lo va a
interpretar como que la aplicación no funciona.

Si preferís que nunca se duerma, Koyeb — pero probá el tiempo de arranque antes de
comprometerte, porque 0.1 vCPU con Spring Boot puede tardar varios minutos.

## Pendiente

- **`OPS-03` — health check.** Falta exponer `/actuator/health`: requiere agregar
  `spring-boot-starter-actuator` al `pom.xml` y permitir esa ruta en `SecurityConfig`.
  Se hace después de que la rama de autenticación entre a `develop`, porque toca ese
  mismo archivo.
- **`GIT-07` / `PROD-03` — etiqueta de entrega.** Al congelar el código el 8 de
  septiembre hay que crear el release y etiquetar la imagen con esa misma versión.
