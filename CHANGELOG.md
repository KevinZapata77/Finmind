# Historial de cambios — FinMind

Todos los cambios relevantes del proyecto quedan registrados en este archivo.

El formato sigue [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y el proyecto
usa [versionado semántico](https://semver.org/lang/es/).

Referencias: `RF-xx` son requisitos funcionales de DOC-03, `DEF-xx` son defectos de CAL-01,
`ADR-xx` son decisiones de arquitectura de ARQ-01.

---

## [Sin publicar]

Pendiente de incorporar a `develop` en tres ramas: `feature/registro-rapido`,
`feature/inicio-compacto` y `docs/ui-003-rehecha`.

### Agregado
- Registro rápido de movimientos desde la pantalla de inicio, sin necesidad de crear
  una cuenta primero (RF-041). Recuerda la última categoría usada.
- Cinco categorías de ingreso y dos de gasto adicionales. Total: 17 categorías del
  sistema (migración `V5`).
- Cuenta automática por omisión para usuarios nuevos.

### Corregido
- Tres columnas pasan de `CHAR` a `VARCHAR` (migración `V4`). Con `CHAR`, la validación
  de esquema impedía el arranque contra Neon (DEF-08).
- Pantalla de inicio compactada: la información principal cabe sin desplazamiento.

---

## [0.9.0] — 2026-08-17

Versión candidata. Los siete módulos funcionales están completos.

### Agregado
- **Metas de ahorro** (RF-035 a RF-038): creación, abonos y progreso. El monto acumulado
  se persiste en lugar de recalcularse, a diferencia del resto de los totales (ADR-016).
- **Administración** (RF-039 a RF-042): resumen del sistema y bitácora de acciones
  administrativas. Solo lectura: no permite editar ni eliminar.
- Cliente web completo: 14 pantallas con navegación real y guardas de sesión.

### Cambiado
- El módulo de administración exige `ROLE_ADMIN` a nivel de clase, no por método.

---

## [0.8.0] — 2026-08-17

### Agregado
- **Obligaciones financieras** (RF-031 a RF-034): deudas con saldo, tasa anual y cuotas.
  Cada pago se descompone en interés del período y abono a capital, y la base valida con
  una restricción que la suma cuadre (RN-018, RN-019, migración `V3`).
- Patrimonio neto: activos menos obligaciones.

### Corregido
- Las tarjetas de crédito ya no se contaban como dinero disponible (RN-020). Lo detectó
  el equipo usando la aplicación, no las pruebas.
- Un acceso denegado devuelve 403 en lugar de 500, y se registra como advertencia y no
  como error (DEF-11).

---

## [0.7.0] — 2026-08-16

### Agregado
- **Presupuestos** (RF-019 a RF-023): límite por categoría y por mes, con alerta al
  alcanzar el 80 % del consumo.
- **Reportes** (RF-024 a RF-028): balance, composición del gasto por categoría y panel.

### Corregido
- La limpieza de datos entre pruebas se centraliza en una sola clase, con un único orden
  de borrado y una única técnica. Antes cada clase de prueba borraba a su manera y
  producía violaciones de integridad referencial intermitentes (DEF-05, DEF-06).
- Las pruebas siembran las categorías del sistema. En pruebas Flyway está apagado, así
  que los datos de las migraciones no existían y algunos casos fallaban sin causa visible.

---

## [0.6.0] — 2026-08-16

### Agregado
- **Movimientos** (RF-013 a RF-018): ingresos y gastos con paginación y filtros.
  El tipo del movimiento lo determina la categoría, no el usuario (RN-002).
- **Categorías** (RF-009 a RF-012): las del sistema son de solo lectura; cada usuario
  puede crear las suyas.

---

## [0.5.0] — 2026-08-16

### Agregado
- **Cuentas** (RF-005 a RF-008) con cálculo de saldo derivado de los movimientos.
- Verificación de correo por código de un solo uso y recuperación de contraseña
  (RF-025 a RF-030, migración `V2`).
- Acceso con Google mediante OAuth 2.
- Verificación anti-automatización en el registro, validada en el servidor (RF-031,
  RN-016). La clave secreta nunca sale del servidor.

### Corregido
- La aplicación arranca sin las credenciales de Google configuradas: la configuración
  del proveedor se movió a un perfil aparte (DEF-04).
- El chequeo de disponibilidad ya no depende del servidor de correo. Devolvía 503 con la
  aplicación sana, y un orquestador la habría reiniciado en bucle (DEF-03).

---

## [0.4.0] — 2026-08-15

### Agregado
- Base de datos migrada a PostgreSQL en Neon, con TLS obligatorio.
- Contenedor con construcción en dos etapas y usuario sin privilegios.
- Composición con PostgreSQL local para trabajar sin conexión.

---

## [0.3.0] — 2026-08-14

### Agregado
- Sistema de diseño y 20 pantallas generadas por código desde un único archivo de
  tokens. Contraste verificado programáticamente contra el mínimo WCAG AA.

---

## [0.2.0] — 2026-08-13

### Agregado
- Registro, inicio de sesión y emisión de fichas firmadas (RF-001 a RF-004).
  Contraseñas con BCrypt. La ficha dura una hora y no se renueva.
- Integración continua: compila y ejecuta las pruebas en cada peticion de incorporación.
- Protección de `main` y `develop`. Se activó después de que entrara dos veces código
  que no compilaba, porque el botón de incorporar seguía disponible con el control en rojo.

---

## [0.1.0] — 2026-08-12

### Agregado
- Estructura del proyecto: Java 21, Spring Boot 3.4.1, Maven, organización por módulo
  de negocio.
- Esquema inicial de base de datos con Flyway: 8 tablas, 2 roles y 10 categorías
  del sistema (migración `V1`).
- Reglas de ramas y de mensajes de commit en `CONTRIBUTING.md`.
