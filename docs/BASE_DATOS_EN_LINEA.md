# Base de datos en la nube — Neon (PostgreSQL)

Objetivo: que cualquier persona pueda ejecutar FinMind sin instalar ni configurar
una base de datos.

Requisitos del `Estandar_de_Requisitos_Tecnicos_Minimos` que esto cubre:

| ID | Requisito | Cómo se cumple |
|---|---|---|
| `PROD-04` | Una persona distinta al autor logra ejecutar el sistema | Solo necesita el `.env` con la cadena de conexión |
| `CICD-02` | Configuración separada de desarrollo, pruebas y producción | `application.yml` / `application-test.yml` / `application-prod.yml` |
| `SEG-06` | Los despliegues públicos deben usar HTTPS/TLS | Neon rechaza conexiones sin TLS (`sslmode=require`) |
| `SEG-09` | Mínimo privilegio en cuentas y base de datos | Rol `finmind_app`, dueño solo de la base `finmind` |

**Lo que esto NO reemplaza:** `DAT-03` exige que una base vacía pueda llevarse al estado
actual. Eso lo cubre Flyway (`src/main/resources/db/migration/`), no la nube.

---

## Por qué Neon y no un MySQL gestionado

Decisión tomada por indicación del instructor. Registrada como decisión técnica (`ARQ-05`).

Neon es **PostgreSQL serverless**. Plan gratuito: 0,5 GB de almacenamiento por proyecto,
100 CU-horas al mes, sin tarjeta de crédito y sin vencimiento.

Ventajas concretas frente a la alternativa que se había configurado antes:

- **Escala a cero y despierta sola.** Cuando no hay actividad la computación se suspende, y
  la primera conexión la reactiva en milisegundos. No hay que ir a un panel a encender nada.
- **Ramas de base de datos.** Se puede crear una rama de la base para probar una migración
  sin tocar los datos de la demostración.
- **Restricciones más sólidas.** Las `CHECK` y las llaves foráneas son de primera clase, sin
  variables de sistema que activar.

Costo de la migración desde MySQL: reescritura completa del esquema, cambio de driver,
dialecto, tipo del id de `Rol` y perfil de pruebas. La lógica de negocio no se tocó.

---

## Paso 1 — Crear el proyecto

1. Entrar a `console.neon.tech` y registrarse (no pide tarjeta).
2. **New project**.
3. Nombre del proyecto: `finmind`.
4. Versión de PostgreSQL: **16**.
5. Región: la más cercana disponible.
6. **Create project**.

Neon crea una base por defecto llamada `neondb` y un rol dueño. No los vamos a usar
en la aplicación.

## Paso 2 — Crear el rol de la aplicación

En el proyecto → **Roles** → **New role** → nombre **`finmind_app`**.
Guardá la contraseña que genera; se muestra una sola vez.

## Paso 3 — Crear la base `finmind`

En **Databases** → **New database**:

- Nombre: **`finmind`**
- Owner: **`finmind_app`**

Que el dueño sea `finmind_app` es lo que le da permiso para crear tablas, que es lo que
Flyway necesita. Desde PostgreSQL 15, el esquema `public` ya no otorga `CREATE` a
cualquier rol, así que sin esto Flyway falla con `permission denied for schema public`.

**No uses el rol dueño del proyecto en la aplicación.** Ese queda para administración.
Esa separación es `SEG-09`.

## Paso 4 — Armar la cadena de conexión

En **Connection Details** copiá el host. Tiene esta forma:

```
ep-algo-algo-123456.us-east-2.aws.neon.tech
```

Y el `.env` queda así:

```
DB_URL=jdbc:postgresql://ep-algo-algo-123456.us-east-2.aws.neon.tech/finmind?sslmode=require
DB_USERNAME=finmind_app
DB_PASSWORD=la_contrasena_del_rol
```

Tres cosas que fallan seguido:

- **No lleva puerto.** Neon usa el 5432 por defecto y no hace falta escribirlo.
- **`sslmode=require` es obligatorio.** Sin eso la conexión se rechaza.
- El nombre de la base va al final del host, **sin** `:puerto` en medio.

> Si aparece un error de SNI o `endpoint ID not specified`, agregá el parámetro
> `&options=endpoint%3Dep-algo-algo-123456` con el identificador de tu endpoint.
> Es un caso raro con drivers antiguos; el driver de PostgreSQL que usa el proyecto
> soporta SNI y no debería hacer falta.

## Paso 5 — Aplicar el esquema

```bash
mvn spring-boot:run
```

En el log tiene que aparecer:

```
Successfully applied 1 migration to schema "public"
```

Verificá en Neon → **Tables**: deben existir **9 tablas** — las 8 del esquema más
`flyway_schema_history`, que es el registro de Flyway.

## Paso 6 — Probar

`http://localhost:8080/swagger-ui.html` → registrá un usuario en
`POST /api/v1/auth/registro`, copiá el token, pegalo en **Authorize**, y llamá a
`GET /api/v1/usuarios/me`.

## Modo demostración

```bash
SPRING_PROFILES_ACTIVE=prod mvn spring-boot:run
```

En Windows (PowerShell):

```powershell
$env:SPRING_PROFILES_ACTIVE="prod"; mvn spring-boot:run
```

Sin SQL en consola, sin trazas en las respuestas de error, y `flyway.clean-disabled`
activo para que nadie borre el esquema por accidente.

---

## Desarrollo local sin internet

```bash
docker compose up --build
```

Levanta PostgreSQL 16 y la API. PostgreSQL queda en el puerto **5433** del host.

## Reglas de trabajo

La base en la nube es una sola y la comparte el equipo:

- Nadie ejecuta `flyway:clean` ni `DROP` sobre ella. Nunca.
- Toda corrección de esquema entra como migración nueva (`V2__`, `V3__`), jamás editando
  la `V1` ya aplicada: Flyway guarda una huella del archivo y se detiene si cambia.
- Los datos de prueba son ficticios (`DAT-04`: prohibido usar datos personales reales).
- Para experimentar sin riesgo, usá una **rama de Neon** o el PostgreSQL local de Docker.

## Antes de la sustentación

- Confirmar que el proyecto está en plan **Free** y no en un trial de pago.
- Entrar a la aplicación una vez para despertar la computación.
- Verificar que las 9 tablas existen y que hay al menos un usuario de prueba registrado.
