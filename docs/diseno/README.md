# Diseño UX/UI — FinMind

Insumo para el documento **ADSO-UXUI-01 – Diseño UX/UI y Sistema de Diseño**.
Versión **0.5** (wireframes y mockups en revisión) · 5 de agosto de 2026
Responsable: Kevin Andrés Zapata Murillo

> Los artefactos visuales son **SVG versionados en este repositorio**, no capturas pegadas.
> Se generan a partir de un único archivo de tokens (`generador/tokens.py`): si cambia un
> token de color o tipografía, cambian todas las pantallas. Esto responde al Anexo D de la
> plantilla, que señala como error "pegar capturas sin versión ni enlace editable" y
> "copiar un design system completo que el proyecto no necesita".

Para regenerar todo:

```bash
cd docs/diseno/generador
OUT=../pantallas python3 run.py
```

---

## 6.1 Inventario maestro de pantallas

| ID | Pantalla | Tipo | Actor | Objetivo | HU / RF | Estado | Versión |
|---|---|---|---|---|---|---|---|
| UI-001 | Iniciar sesión | Página | Usuario | Autenticarse para acceder a sus datos | *(mapear)* | En revisión | 0.5 |
| UI-002 | Crear cuenta | Página | Visitante | Registrarse en la plataforma | *(mapear)* | En revisión | 0.5 |
| UI-003 | Panel de balance | Página | Usuario | Ver su situación financiera del mes | *(mapear)* | En revisión | 0.5 |
| UI-004 | Movimientos | Página | Usuario | Consultar, filtrar y editar sus movimientos | *(mapear)* | En revisión | 0.5 |
| UI-005 | Registrar movimiento | Modal | Usuario | Registrar un ingreso o un gasto | *(mapear)* | En revisión | 0.5 |
| UI-006 | Presupuestos | Página | Usuario | Definir límites y ver su consumo | *(mapear)* | En revisión | 0.5 |
| UI-007 | Metas de ahorro | Página | Usuario | Crear metas y abonar a ellas | *(mapear)* | Pendiente | — |
| UI-008 | Cuentas | Página | Usuario | Administrar sus cuentas y saldos | *(mapear)* | Pendiente | — |
| UI-009 | Reportes | Página | Usuario | Consultar consolidados y comparativos | *(mapear)* | Pendiente | — |
| UI-010 | Administración | Página | Administrador | Gestionar la plataforma **sin acceso a datos financieros** | RF-26 | Pendiente | — |

> **Pendiente obligatorio:** los códigos HU y RF deben mapearse contra el Documento de
> Requerimientos v2.0 y las 22 historias de usuario. La plantilla lo exige en su regla de
> coherencia: "No se diseña una pantalla sin historia, requisito, caso de uso o necesidad
> trazable". Sin ese mapeo, la sección 17.1 queda incompleta.

## 8.1 Registro de mockups

| ID | Pantalla | Dispositivo | Archivo | Versión | Estado |
|---|---|---|---|---|---|
| MK-001 | UI-001 | Escritorio 1280×800 | `pantallas/UI-001_iniciar-sesion.svg` | 0.5 | En revisión |
| MK-001b | UI-001 estado error | Escritorio | `pantallas/UI-001b_iniciar-sesion_error.svg` | 0.5 | En revisión |
| MK-002 | UI-002 | Escritorio | `pantallas/UI-002_crear-cuenta.svg` | 0.5 | En revisión |
| MK-003 | UI-003 | Escritorio | `pantallas/UI-003_panel.svg` | 0.5 | En revisión |
| MK-003m | UI-003 | Móvil 390×844 | `pantallas/UI-003m_panel_movil.svg` | 0.5 | En revisión |
| MK-004 | UI-004 | Escritorio | `pantallas/UI-004_movimientos.svg` | 0.5 | En revisión |
| MK-004b | UI-004 estado vacío | Escritorio | `pantallas/UI-004b_movimientos_vacio.svg` | 0.5 | En revisión |
| MK-005 | UI-005 | Escritorio | `pantallas/UI-005_registrar-movimiento.svg` | 0.5 | En revisión |
| MK-005m | UI-005 | Móvil | `pantallas/UI-005m_registrar-movimiento_movil.svg` | 0.5 | En revisión |
| MK-006 | UI-006 | Escritorio | `pantallas/UI-006_presupuestos.svg` | 0.5 | En revisión |

Copias en PNG para pegar en el documento: `pantallas/png/`.

## 9.2 Matriz de estados por pantalla

| Pantalla | Inicial | Carga | Vacío | Éxito | Validación | Error sistema | Permiso | Sesión expirada |
|---|---|---|---|---|---|---|---|---|
| UI-001 | Sí | Pendiente | N.A. | Sí | **Sí (MK-001b)** | Sí | N.A. | N.A. |
| UI-002 | Sí | Pendiente | N.A. | Pendiente | Sí | Pendiente | N.A. | N.A. |
| UI-003 | Sí | Pendiente | Pendiente | N.A. | N.A. | Pendiente | N.A. | Pendiente |
| UI-004 | Sí | Pendiente | **Sí (MK-004b)** | N.A. | N.A. | Pendiente | N.A. | Pendiente |
| UI-005 | Sí | Pendiente | N.A. | Pendiente | Sí | Pendiente | N.A. | Pendiente |
| UI-006 | Sí | Pendiente | Pendiente | N.A. | Sí | Pendiente | N.A. | Pendiente |

Los "Pendiente" son honestos: faltan diseñar. La plantilla exige los diez estados
obligatorios de la sección 9.1 y declararlos incompletos es preferible a marcarlos como
cumplidos sin evidencia.

## 12.3 Paleta y contraste verificado

Ratios calculados con la fórmula de luminancia relativa de WCAG 2.1. **Los diez pares
evaluados cumplen AA (4.5:1).**

| Token | Hex | Uso | Contraste | AA |
|---|---|---|---|---|
| `color.primary.700` | `#0A5647` | Botón primario, navegación activa | 8.62 : 1 | Sí |
| `color.primary.600` | `#0B6B57` | Acciones principales, enlaces | 6.45 : 1 | Sí |
| `color.primary.500` | `#0E8368` | Acentos, gráficos | 4.62 : 1 | Sí |
| `color.primary.100` | `#DCF2EC` | Fondos suaves, etiquetas | — | Fondo |
| `color.success.600` | `#15803D` | Confirmaciones, ingresos | 5.02 : 1 | Sí |
| `color.warning.600` | `#B45309` | Advertencias, cerca del límite | 5.02 : 1 | Sí |
| `color.error.600` | `#B91C1C` | Errores, gastos, exceso | 6.47 : 1 | Sí |
| `color.neutral.900` | `#111827` | Texto principal | 17.74 : 1 | Sí |
| `color.neutral.700` | `#374151` | Texto secundario, etiquetas | 10.31 : 1 | Sí |
| `color.neutral.500` | `#6B7280` | Texto de ayuda | 4.83 : 1 | Sí |
| `color.neutral.300` | `#D1D5DB` | Bordes de campos | — | Borde |
| `color.canvas` | `#F7F8FA` | Fondo de página | — | Fondo |

Reproducible con `python3 generador/verificar_contraste.py`.

## 12.4 Tipografía

| Token | Tamaño / peso | Uso |
|---|---|---|
| `font.display` | 28 / 700 | Portada, cifra destacada |
| `font.heading.lg` | 22 / 700 | Título de página |
| `font.heading.md` | 17 / 600 | Título de sección o tarjeta |
| `font.body.md` | 14 / 400 | Texto general |
| `font.label` | 12 / 600 | Etiqueta de campo |
| `font.caption` | 11 / 400 | Ayuda secundaria |

Familia: Inter, con respaldo a Segoe UI y a la fuente del sistema.

## 12.5 Espaciado, grid y breakpoints

- Unidad base **8 px**; escala 8 / 16 / 24 / 32 / 48
- Radios: `sm` 4, `md` 8, `lg` 12
- Contenedor máximo 1280 px; barra lateral fija de 240 px en escritorio
- Breakpoints: móvil `<768`, tableta `768–1023`, escritorio `≥1024`

## 13.1 Catálogo de componentes

| ID | Componente | Variantes | Estados diseñados |
|---|---|---|---|
| CMP-UI-001 | Botón | Primario, secundario, peligro | Normal, foco, deshabilitado, cargando |
| CMP-UI-002 | Campo de texto | Con etiqueta persistente | Normal, foco, error |
| CMP-UI-003 | Selector de opción | Segmentado (tipo de movimiento) | Seleccionado, no seleccionado |
| CMP-UI-004 | Tabla de movimientos | Con filtros y paginación | Con datos, vacío |
| CMP-UI-005 | Modal | Formulario centrado con fondo atenuado | Abierto |
| CMP-UI-006 | Barra de progreso | Presupuesto | En rango, alerta, excedido |
| CMP-UI-007 | Etiqueta de estado | Éxito, alerta, error | Con icono y texto |
| CMP-UI-008 | Tarjeta de indicador | Balance, ingresos, gastos | Con valor y variación |
| CMP-UI-009 | Navegación lateral | Escritorio | Ítem activo e inactivo |
| CMP-UI-010 | Navegación inferior | Móvil, 4 destinos | Ítem activo e inactivo |

## 14.2 Catálogo de mensajes

| ID | Contexto | Tipo | Mensaje | Pantalla |
|---|---|---|---|---|
| MSG-001 | Credenciales inválidas | Error seguro | "No pudimos iniciar sesión. Correo o contraseña incorrectos." | UI-001 |
| MSG-002 | Correo con formato inválido | Validación | "Escribe un correo con formato válido, por ejemplo nombre@dominio.com" | UI-001, UI-002 |
| MSG-003 | Contraseña corta | Validación | "La contraseña debe tener al menos 8 caracteres." | UI-002 |
| MSG-004 | Sin movimientos | Vacío | "Todavía no tienes movimientos. Registra tu primer ingreso o gasto para empezar a ver tu balance." | UI-004 |
| MSG-005 | Presupuesto excedido | Advertencia | "Superaste el límite en $ X." | UI-003, UI-006 |
| MSG-006 | Aviso al registrar gasto | Informativo | "Con este gasto llegas al 80% de tu presupuesto de Alimentación." | UI-005 |

**MSG-001 es deliberadamente genérico.** No distingue entre correo inexistente y contraseña
incorrecta, porque hacerlo permitiría averiguar qué correos están registrados. Coincide con
la implementación del backend, que devuelve el mismo 401 en ambos casos.

## 11.1 Accesibilidad — verificación

| N.° | Criterio | Cumple | Evidencia |
|---|---|---|---|
| 1 | Contraste suficiente | Sí | Tabla 12.3, ratios calculados |
| 2 | Foco visible | Sí | Anillo de foco en `sistema-de-diseno.svg` y en MK-001b, MK-005 |
| 3 | Etiquetas persistentes | Sí | Ningún campo usa placeholder como etiqueta |
| 4 | Controles con propósito claro | Sí | Botones nombran la acción: "Guardar movimiento", no "Aceptar" |
| 5 | No depende solo del color | Sí | Estados llevan icono y texto: "OK En rango", "~ Alerta", "! Excedido" |
| 6 | Texto legible | Sí | Mínimo 11 px en ayuda, 14 px en texto general |
| 10 | Errores identificables y corregibles | Sí | Mensaje junto al campo, con causa y corrección |
| 11 | Objetivos táctiles adecuados | Sí | Controles móviles de 44 px de alto, navegación inferior separada |
| 7, 8, 9, 12 | Alternativas textuales, tablas, foco en modales, movimiento | Pendiente | Se verifica en implementación |

Cada SVG incluye `role="img"` con `<title>` y `<desc>`, así que los propios mockups son
legibles por lectores de pantalla.

## 16.2 Registro de decisiones de diseño

| ID | Contexto | Alternativas | Decisión | Justificación |
|---|---|---|---|---|
| DD-001 | Formato de los mockups | Figma / capturas PNG / SVG en el repositorio | **SVG versionado y generado desde tokens** | Editable, con control de versiones, y garantiza que ninguna pantalla se desvíe del sistema de diseño |
| DD-002 | Registro de ingresos y gastos | Dos pantallas separadas / una con selector de tipo | **Una pantalla con selector segmentado (UI-005)** | Los campos son idénticos; separarlas duplicaría la interfaz y el código. Coincide con la tabla única `transacciones` del backend |
| DD-003 | Identificación del usuario en la interfaz | Id en la URL / id desde el token | **Desde el token, sin id en rutas** | Con el id en la URL no hay forma de impedir el acceso a datos de otro usuario |
| DD-004 | Señalización de estado de presupuesto | Solo color / color + icono + texto | **Color, icono y texto** | El color por sí solo excluye a personas con baja visión de color |
| DD-005 | Navegación en móvil | Menú lateral desplegable / barra inferior | **Barra inferior de 4 destinos** | Alcanzable con el pulgar y sin ocultar la acción principal |

## Limitación declarada

**No se han realizado pruebas de usabilidad con usuarios.** La plantilla lo advierte en su
cuidado metodológico: cuando no sea posible investigar directamente, se registra la
limitación. Los perfiles y necesidades se apoyan en la encuesta de capacidades financieras
CAF–Superintendencia Financiera 2019 citada en el DOC-01, no en entrevistas propias.
Las pruebas de la sección 15 quedan pendientes.

## Estructura de carpetas (Anexo B)

```
docs/diseno/
├── README.md            este documento
├── pantallas/           mockups en SVG, fuente editable
│   └── png/             copias en PNG para pegar en el documento
└── generador/           código que produce los mockups desde los tokens
```

Pendientes de crear: `investigacion/`, `arquitectura-informacion/`, `flujos/`,
`pruebas-usabilidad/`, `decisiones/`.
