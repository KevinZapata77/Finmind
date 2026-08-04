# Base de datos en la nube — guía de montaje

Objetivo: que cualquier persona pueda ejecutar FinMind sin instalar ni configurar MySQL.

Esto responde a tres requisitos críticos del `Estandar_de_Requisitos_Tecnicos_Minimos`:

| ID | Requisito | Cómo se cumple |
|---|---|---|
| `PROD-04` | Una persona distinta al autor logra ejecutar el sistema | Solo necesita el `.env` con la cadena de conexión |
| `CICD-02` | Configuración separada de desarrollo, pruebas y producción | `application.yml` / `application-test.yml` / `application-prod.yml` |
| `SEG-09` | Mínimo privilegio en cuentas y base de datos | Usuario `finmind_app`, no la cuenta administradora |

**Lo que esto NO reemplaza:** `DAT-03` sigue exigiendo que una base vacía pueda llevarse al
estado actual. Eso lo cubre Flyway (`src/main/resources/db/migration/`), no la nube.

---

## Proveedor

**Aiven for MySQL, plan gratuito.** MySQL real, no un motor compatible.

- 1 nodo, 1 CPU, 1 GB de RAM, 1 GB de disco, con backups
- Sin tarjeta de crédito, sin límite de tiempo
- Un solo servicio gratuito por organización

**Dos advertencias que importan:**

1. Aiven **apaga los servicios gratuitos sin actividad continua**. Avisa antes y se
   vuelven a encender desde el panel, pero un servicio apagado el día de la sustentación
   es un desastre evitable. Verificá que esté encendido 24 horas antes.
2 . 1 GB de disco es de sobra para este proyecto, pero no metas datos de prueba masivos.

**Por qué no otros:** TiDB Cloud da 5 GiB gratis pero es MySQL-*compatible*, no idéntico —
las llaves foráneas y las restricciones `CHECK` del esquema pueden comportarse distinto.
Clever Cloud ya no tiene plan gratuito.

---

## Paso 1 — Crear el servicio

1. Registrarse en `console.aiven.io` (sin tarjeta).
2. **Create service** → **MySQL**.
3. Plan: seleccionar el que dice **Free**.
4. Región: la más cercana a Colombia disponible en el plan gratuito.
5. Nombre del servicio: `finmind-db`.
6. **Create service**. Tarda unos minutos en pasar a `RUNNING`.

## Paso 2 — Crear la base y el usuario de la aplicación

En el panel del servicio, pestaña **Databases** → crear la base **`finmind`**.

En **Users** → **Add service user** → nombre `finmind_app`. Guardá la contraseña que genera.

`SEG-09` exige mínimo privilegio: **no uses la cuenta administradora (`avnadmin`) en la aplicación.**
Esa queda solo para tareas de administración.

## Paso 3 — Armar la cadena de conexión

Del panel copiá **Host**, **Port** y armá el `.env`:

```
DB_URL=jdbc:mysql://finmind-db-xxxx.aivencloud.com:12345/finmind?sslMode=REQUIRED&serverTimezone=America/Bogota&useUnicode=true&characterEncoding=UTF-8
DB_USERNAME=finmind_app
DB_PASSWORD=la_contrasena_del_usuario
```

El puerto **no** es 3306. Y sin `sslMode=REQUIRED` la conexión es rechazada.

> Versión más estricta (opcional): descargar el certificado **CA** del panel, importarlo a un
> truststore y usar `sslMode=VERIFY_CA`. Recomendable si querés apuntar a nivel 3 de madurez;
> `REQUIRED` ya cifra el tráfico.

## Paso 4 — Aplicar el esquema

Flyway corre solo al arrancar:

```bash
mvn spring-boot:run
```

En el log tiene que aparecer `Migrating schema` y `Successfully applied 1 migration`.
Verificá en el panel de Aiven que existan las 8 tablas.

Si falla la conexión, en orden: puerto equivocado, falta `sslMode=REQUIRED`, o el usuario
no tiene permisos sobre la base `finmind`.

## Paso 5 — Levantar en modo demostración

```bash
SPRING_PROFILES_ACTIVE=prod mvn spring-boot:run
```

En Windows (PowerShell):

```powershell
$env:SPRING_PROFILES_ACTIVE="prod"; mvn spring-boot:run
```

Diferencias del perfil `prod`: sin SQL en consola, sin trazas en las respuestas de error,
`flyway.clean-disabled` activo para que nadie borre el esquema por accidente.

---

## Regla de trabajo para el equipo

La base en la nube es **una sola** y la comparten Kevin y Luis. Consecuencias:

- Nadie corre `flyway:clean` ni `DROP` sobre ella. Nunca.
- Toda corrección de esquema entra como migración nueva (`V2__`, `V3__`), jamás editando `V1__`.
- Los datos de prueba son ficticios (`DAT-04`: prohibido usar datos personales reales).
- Si necesitás romper cosas experimentando, hacelo contra un MySQL local (Opción B del `.env.example`).

## Pendiente: ¿hace falta desplegar la aplicación?

`CICD-04` (crítico) pide que el despliegue esté **realizado o documentado y probado**. La base
en la nube cubre el dato, no la aplicación. Si el profesor espera abrir una URL y ver FinMind
funcionando, hay que desplegar además el backend y el frontend, y eso son 2 o 3 días que deben
quedar reservados en el cronograma antes del congelamiento del 8 de septiembre.

Pregunta concreta para hacerle al instructor:

> Para cumplir CICD-04, ¿es suficiente que el despliegue esté documentado y probado con la base
> de datos en la nube y ejecución local reproducible, o se exige una URL pública operativa del
> sistema completo el día de la sustentación?
