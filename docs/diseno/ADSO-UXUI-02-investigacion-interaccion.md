# ADSO-UXUI-02 — Investigación de interacción y plan de rediseño del frontend

Complemento de **ADSO-UXUI-01 – Diseño UX/UI y Sistema de Diseño**.
Versión **1.0** · 27 de agosto de 2026
Responsables: Kevin Andrés Zapata Murillo, Luis Miguel Méndez
Ficha 3114227 — Tecnología en Análisis y Desarrollo de Software, SENA CTMA

> Este documento no propone un cambio de estética. Propone un cambio de
> **interacción**. La distinción es el hallazgo central de la investigación y
> conviene dejarla escrita antes de tocar una línea de CSS.

---

## 1. Motivo y método

El frontend de FinMind está funcionalmente completo: 16 pantallas, 10 módulos,
autenticación, y un backend de 171 pruebas en verde. Sin embargo, en la revisión
de aceptación se registró una observación que no era un defecto de función:

> *"La aplicación no aburre por lo que le falta, aburre porque cada pantalla
> termina en sí misma."*

Para no responder a esa observación con una intuición, se hizo lo siguiente:

1. **Inventario medido del frontend**: se recorrieron los 16 archivos de
   `src/paginas/`, los 12 de `src/componentes/` y las 995 líneas de CSS,
   contando enlaces, gráficos, transiciones y llamadas a la API.
2. **Inventario del backend**: se recorrieron los 13 controladores y todos los
   DTO de lectura, para saber qué datos **existen** y qué datos **no existen**.
   No se puede visualizar lo que la API no devuelve.
3. **Revisión de literatura de UX de productos financieros** (fuentes en §7),
   buscando específicamente qué hace que una persona vuelva a abrir una app de
   finanzas.

---

## 2. Qué dice la investigación

De la revisión de fuentes de diseño de producto financiero, tres hallazgos son
directamente aplicables y uno de ellos es contraintuitivo:

**H1 — La realimentación de progreso es el mayor motor de retorno.** Lo que hace
que alguien vuelva a abrir una app de finanzas no es la cantidad de información,
es la señal de que su situación está cambiando por lo que él hace.

**H2 — Un solo número comparativo pesa más que un tablero entero.** La fuente lo
plantea con un ejemplo concreto: *"gastaste 340 menos en restaurantes que el mes
pasado"* logra más que un dashboard completo, porque es específico, es personal, e
implica que la causa fue el comportamiento del usuario. Un mes aislado es una
foto; seis meses son una historia.

**H3 — La gente consulta MÁS cuando va mal, no cuando va bien.** El estudio de
comportamiento citado en §7 encontró que la frecuencia de consulta del saldo
**aumenta** cuando el progreso hacia la meta es pobre. Esto es contraintuitivo y
cambia el diseño de las alertas: la persona que más va a mirar FinMind es la que
está en problemas. Una alerta que solo informa ("vas a pasarte en Alimentación")
la deja exactamente donde estaba. Una alerta tiene que **llevar a la acción que
la resuelve**.

**H4 — Patrón de profundización (*drill-down*).** El patrón estándar de tablero
analítico: se muestra el resumen, y al hacer clic en un elemento del resumen se
revelan las filas que lo componen, ya filtradas. Reduce la carga cognitiva sin
esconder información.

---

## 3. Diagnóstico de FinMind hoy (cifras, no impresiones)

### 3.1 El hallazgo principal: los números no llevan a ninguna parte

| Medición | Resultado |
|---|---|
| `<Link>` fuera del menú de navegación | 7 en total |
| — de esos, en el flujo de autenticación | 4 |
| — de esos, enlaces genéricos sin contexto | 2 (`/presupuestos`, `/movimientos`) |
| — de esos, contextuales con filtro | **0** |
| `useNavigate` en pantallas del núcleo (fuera de auth) | **0** |
| Pantallas del núcleo que reciben un filtro por URL o estado | **0** |

Consecuencia concreta, verificada leyendo el código: un usuario ve en el panel
que se pasó del presupuesto de Alimentación. No puede hacer clic. Tiene que ir
al menú, entrar a Movimientos, abrir los filtros, elegir Alimentación, elegir el
rango de fechas del mes, y aplicar. Seis acciones para responder la pregunta que
el panel acabó de plantearle.

**Lo importante: el backend ya soporta esa consulta.**
`GET /api/v1/transacciones` acepta `categoriaId`, `desde`, `hasta`, `cuentaId` y
`tipo`. La capacidad está construida y pagada; el frontend simplemente nunca la
enlaza. Esta es la mejora con mejor relación valor/esfuerzo de todo el proyecto.

### 3.2 La visualización vive en una sola pantalla

Existen exactamente dos gráficos: `Dona.jsx` (composición del gasto) y
`CurvaDelMes.jsx` (acumulado del mes, con proyección punteada y lectura por día
al pasar el cursor). Ambos son SVG dibujado a mano, sin librería, y ambos son de
buena calidad — la curva calcula escala, rejilla y proyección; la dona resalta la
porción con teclado y mantiene barras redundantes al lado para no depender del
color (RNF-008).

El problema no es su calidad, es su ubicación: **ambos están solo en el Panel.**
Las otras 8 pantallas del núcleo (Movimientos, Presupuestos, Gastos fijos,
Créditos, Metas, Cuentas, Categorías, Administración) no tienen un solo SVG.
Su única representación visual son barras de progreso `<div>` con `width` en
línea, sin interacción de ningún tipo.

### 3.3 Falta la dimensión del tiempo

Esto es un hallazgo de arquitectura, no de estilo. **Todos** los endpoints de
reportes reciben un `anio` y un `mes` sueltos:

```
GET /reportes/balance?anio=&mes=
GET /reportes/gasto-por-categoria?anio=&mes=
GET /reportes/panel?anio=&mes=
GET /reportes/ritmo?anio=&mes=
```

No existe ningún endpoint que acepte un rango de meses. Por lo tanto hoy es
**imposible** responder, sin hacer seis llamadas encadenadas:

- ¿cómo evolucionó mi gasto en los últimos 6 meses?
- ¿gasté más o menos que el mes pasado, y en qué categoría?
- ¿cuál es mi tendencia de patrimonio?

Cruzando esto con **H2**, el resultado es incómodo y hay que decirlo claro: *el
recurso con mayor efecto sobre la retención según la literatura —la comparación
contra el período anterior— es hoy estructuralmente imposible en FinMind.* No es
que esté mal diseñado; es que el dato no existe.

### 3.4 Las alertas no pueden señalar el origen

`AlertaResponse` es:

```java
public record AlertaResponse(
        String tipo, String severidad, String titulo, String mensaje,
        BigDecimal monto, String rutaSugerida
) {}
```

`rutaSugerida` apunta a un **módulo** (`/presupuestos`), nunca a un registro
(`/presupuestos/42`). No hay `presupuestoId`, `cuentaId` ni `obligacionId`. El
nombre de la entidad viaja incrustado en el texto de `mensaje`, así que la única
forma de que el frontend resalte el registro exacto sería parsear una frase en
español — frágil y descartado. Cruzado con **H3**, esta es la limitación más
costosa del sistema: la alerta llega justo al usuario que más la necesita, y lo
deja buscando a mano.

### 3.5 Hallazgos menores

- **Sistema de diseño**: 995 líneas de CSS, 5 transiciones simples, **0**
  `@keyframes`, sin modo oscuro. Los tokens están bien construidos (base 8,
  paleta verde `#0A5647`–`#0E8368` con contraste ya verificado). La base es
  sólida; está subutilizada, no mal hecha.
- **Cargas redundantes**: `api.cuentas()` se pide por separado en 4 pantallas y
  `api.categorias()` en 5, sin ningún cache ni contexto compartido. No es un
  defecto visible, pero multiplica las peticiones contra una base Neon que
  además tiene arranque en frío.
- **Estados de carga**: todas las pantallas muestran el texto `Cargando…`. No hay
  esqueletos ni preservación de layout, así que la página salta al llegar el dato.

---

## 4. Decisión de diseño

### 4.1 Lo que NO se va a hacer, y por qué

Se descarta explícitamente, para que quede registrado:

- **Paleta neón / oscuro "futurista"**: rompería el contraste WCAG AA ya
  verificado en ADSO-UXUI-01 y obligaría a re-verificar 995 líneas de CSS a 12
  días del congelamiento de código. El costo es alto y el beneficio es estético.
- **Librería de gráficos** (Chart.js, Recharts): añadiría ~200 KB a un proyecto
  que hoy tiene 3 dependencias de cliente, y los dos SVG a mano existentes
  demuestran que no hace falta.
- **Animaciones de entrada en listas**: en una app de dinero, el movimiento
  gratuito lee como inestabilidad. Se reserva la animación para confirmar
  causa-efecto (un número que cambia porque el usuario hizo algo).

### 4.2 La tesis

> El frontend de FinMind no necesita más color. Necesita que **cada número sea
> una puerta** y que **cada número tenga con qué compararse.**

"Futurista" en un producto financiero de 2026 no significa neón; significa que el
sistema parece saber lo que el usuario quiere preguntar a continuación. Eso se
consigue con tres cosas, en este orden de valor:

1. **Profundización (H4)**: todo agregado es clicable y abre las filas que lo
   componen, ya filtradas.
2. **Comparación (H1, H2)**: todo número del mes trae al lado su equivalente del
   mes anterior y la diferencia.
3. **Alertas accionables (H3)**: la alerta lleva al registro exacto, no al módulo.

---

## 5. Plan priorizado

Ordenado por valor/esfuerzo, con el congelamiento del **8 de septiembre** como
límite. Las fases A y B son independientes y pueden ir en paralelo entre los dos
repositorios.

### Fase A — Interconexión (frontend, riesgo bajo)

La API ya existe; es cableado.

| # | Cambio | Archivos |
|---|---|---|
| A1 | `Movimientos` lee filtros de la URL con `useSearchParams`, y muestra un aviso "estás viendo un filtro" con opción de quitarlo | `Movimientos.jsx` |
| A2 | Porción de la dona y barras de composición → movimientos de esa categoría en ese mes | `Dona.jsx`, `Panel.jsx` |
| A3 | Cada presupuesto (en Panel y en Presupuestos) → sus movimientos | `Presupuestos.jsx`, `Panel.jsx` |
| A4 | Cada cuenta → sus movimientos; cada gasto fijo → los de su categoría | `Cuentas.jsx`, `GastosFijos.jsx` |

**RF-049** (nuevo): desde cualquier cifra agregada, el usuario puede abrir el
detalle de movimientos que la compone, sin configurar filtros a mano.

### Fase B — Datos que faltan (backend, riesgo medio)

| # | Cambio | Nota |
|---|---|---|
| B1 | `GET /reportes/historico?meses=6` → serie mensual de ingresos/gastos | Habilita §3.3. Agregado por mes, una consulta |
| B2 | `referenciaTipo` + `referenciaId` en `AlertaResponse` | Aditivo, sin migración. Resuelve §3.4 |
| B3 | Comparación con el mes anterior en `gasto-por-categoria` | Habilita H2 |

**RF-050**: el sistema presenta la evolución de ingresos y gastos de los últimos
6 meses.
**RF-051**: cada alerta identifica el registro que la originó.
**RN-032**: la comparación contra el mes anterior se omite —no se muestra en
cero— cuando no hay datos del período previo, para no sugerir una caída del 100 %
donde en realidad no había historia.

### Fase C — Visualización (frontend, riesgo bajo)

| # | Cambio |
|---|---|
| C1 | `TendenciaMeses.jsx`: SVG de barras de 6 meses, ingreso vs. gasto, mes actual destacado |
| C2 | Delta contra el mes anterior junto a cada cifra del panel y en la dona |
| C3 | Micro-visual en las pantallas hoy sin SVG (presupuestos, metas, créditos) |
| C4 | Esqueletos de carga en lugar de `Cargando…` |

### Fase D — Verificación

`vite build` limpio · 171+ pruebas en verde · contraste AA de todo color nuevo ·
navegación completa por teclado de cada elemento nuevo que sea clicable ·
actualización de ADSO-UXUI-01 y la matriz de trazabilidad.

---

## 5.bis Resultado de la implementación

Estado al 27 de agosto de 2026. Fases A, B y C implementadas y verificadas.

### Lo que se construyó

| Fase | Cambio | Verificación |
|---|---|---|
| A | Filtros de Movimientos migrados a la URL (`useSearchParams`) | 14 pruebas unitarias de los ayudantes de enlace, incluido febrero bisiesto |
| A | Aviso "estás viendo…" con descripción en español y salida | `vite build` limpio |
| A | 8 puntos de profundización nuevos: dona (porción y leyenda), barras de composición, tarjetas de ingresos y gastos, presupuestos en alerta, presupuestos, cuentas, gastos fijos | — |
| B | `GET /reportes/historico?meses=6` | 13 casos nuevos en `HistoricoYComparacionTest` |
| B | Comparación por categoría en `gasto-por-categoria` | idem |
| B | `referenciaTipo` / `referenciaId` en `AlertaResponse` | aditivo, pruebas existentes intactas |
| C | `TendenciaMeses.jsx` — SVG de 6 meses, ingreso vs. gasto | renderizado a PNG y revisado visualmente |
| C | `ComparacionConElMesPasado.jsx` | — |
| C | Variación por categoría bajo cada barra | — |

### Dos decisiones que cambiaron sobre la marcha

**1. La comparación ingenua estaba mal y se detectó antes de probarla.**
La primera versión comparaba el total del mes en curso contra el total del mes
anterior. Es la versión obvia y produce un número roto: el día 5, comparar cinco
días contra treinta dice siempre que el usuario gasta mucho menos, y lo repite
todos los meses. *Un número que siempre tranquiliza es peor que ningún número.*
Se rehízo para medir **el mismo número de días en los dos meses** (día 1 al día
de hoy, contra día 1 al mismo día del mes anterior), con el recorte del caso
"hoy es 31 y el mes anterior tuvo 30". Por eso la interfaz dice *"a esta altura
del mes pasado"* y no *"que el mes pasado"*: la frase describe lo que el número
mide.

**2. Se encontró y corrigió una regresión de contraste.**
La verificación programática de WCAG sobre los 15 pares color/fondo nuevos
detectó que `--color-success-600` (#15803D) usado como texto pequeño sobre el
fondo gris del `:hover` de una fila (#E5E7EB) daba **4.05:1**, por debajo del
mínimo AA de 4.5:1. Es exactamente el mismo problema que en su momento obligó a
oscurecer `--color-neutral-500`, y solo aparece en el estado `:hover`, así que a
ojo no se habría visto.

Se añadió el token **`--color-success-700: #146B34`** (6.60:1 sobre blanco,
5.33:1 sobre el gris de hover), que además llena un hueco real del sistema:
`primary` tenía 700/600/500 y `success` solo tenía 600. El 600 se sigue usando
en rellenos y barras, donde el mínimo aplicable es 3:1 y no 4.5:1.

Los 15 pares quedan verificados en AA. La verificación es reproducible: el
cálculo de luminancia relativa y razón de contraste está en el historial de
trabajo y puede reejecutarse sobre `tokens.css`.

### Nota de honestidad sobre la verificación del backend

El backend de las fases B se verificó con revisión de tipos y balance de
delimitadores, **no con `mvn test`**: el entorno donde se escribió no tiene
acceso a Maven Central. Las 13 pruebas nuevas están escritas y las existentes se
revisaron una por una para confirmar que los campos añadidos son aditivos y no
rompen ninguna aserción (`porciones[0].monto`, `porcentaje`, `rutaSugerida`
siguen valiendo lo mismo). **Queda pendiente correr `.\finmind.ps1 pruebas`
antes de mezclar a `develop`.**

### Fuera de alcance para el 8 de septiembre

Modo oscuro · cache/contexto de datos compartido (§3.5) · exportación a CSV ·
histórico de patrimonio con instantáneas · búsqueda por texto en movimientos.
Quedan registrados como trabajo futuro en CIE-01.

---

## 6. Trazabilidad

| ID | Descripción | Origen | Fase |
|---|---|---|---|
| RF-049 | Profundización desde cifra agregada a detalle | H4, §3.1 | A |
| RF-050 | Evolución de 6 meses de ingresos y gastos | H1/H2, §3.3 | B, C |
| RF-051 | Alerta con referencia al registro de origen | H3, §3.4 | B |
| RN-032 | La comparación se omite si no hay período previo | §5 B3 | B |
| DEF-19 | El compromiso semanal usaba el día del mes | Prueba manual | Corregido |
| UI-011 | Gastos fijos: día de la semana y abono directo | Prueba manual | Corregido |

---

## 7. Fuentes

- [Budgeting Apps UX Patterns for Trustworthy Finance Products — Appthetics](https://www.appthetics.com/blog/budgeting-apps-ux-patterns)
- [Engagement Optimization for Personal Finance Apps — Lifecycle Architect](https://lifecyclearchitect.com/guides/engagement-optimization-for-personal-finance-apps/)
- [Monitoring personal finances: goal progress and regulatory focus influence when people check their balance — ScienceDirect](https://www.sciencedirect.com/science/article/abs/pii/S0167487017303173)
- [Compare Monthly Spending: How to Track Spending Trends — Finny](https://getfinny.app/blog/compare-monthly-spending)
- [Drill-down — Embeddable Documentation](https://docs.embeddable.com/dashboards/drill-down)
- [Progressive Disclosure UX: Guide + Examples — Lollypop Design](https://lollypop.design/blog/2025/may/progressive-disclosure/)
- [Dashboard Design Principles — UXPin](https://www.uxpin.com/studio/blog/dashboard-design-principles/)
- [Fintech UX Best Practices — Eleken](https://www.eleken.co/blog-posts/fintech-ux-best-practices)

Inventario interno: `src/paginas/` (16 archivos), `src/componentes/` (12),
`src/estilos/` (995 líneas), y los 13 controladores de `finmind-estructura`.
Las cifras de §3 se obtuvieron por conteo directo sobre el código del commit
`1e7d72b`.
