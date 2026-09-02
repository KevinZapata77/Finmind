/**
 * Único punto de contacto con la API. Ningún componente hace fetch directamente:
 * así el token, los encabezados y el manejo de errores viven en un solo lugar.
 */
const BASE = import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api/v1'
const CLAVE_TOKEN = 'finmind.token'

export function guardarToken(token) {
  sessionStorage.setItem(CLAVE_TOKEN, token)
}
export function leerToken() {
  return sessionStorage.getItem(CLAVE_TOKEN)
}
export function borrarToken() {
  sessionStorage.removeItem(CLAVE_TOKEN)
}

/** Error de API con el mensaje que devuelve el backend, no uno inventado en el cliente. */
export class ErrorApi extends Error {
  constructor(mensaje, estado, erroresPorCampo) {
    super(mensaje)
    this.estado = estado
    this.erroresPorCampo = erroresPorCampo ?? null
  }
}

async function peticion(ruta, { metodo = 'GET', cuerpo, autenticada = true } = {}) {
  const encabezados = { Accept: 'application/json' }
  if (cuerpo) encabezados['Content-Type'] = 'application/json'

  if (autenticada) {
    const token = leerToken()
    if (token) encabezados.Authorization = `Bearer ${token}`
  }

  let respuesta
  try {
    respuesta = await fetch(`${BASE}${ruta}`, {
      method: metodo,
      headers: encabezados,
      body: cuerpo ? JSON.stringify(cuerpo) : undefined,
    })
  } catch {
    // Falla de red: el backend no responde. Estado 0 para distinguirlo de un error HTTP.
    throw new ErrorApi('No pudimos conectar con el servidor. Verifica tu conexión.', 0)
  }

  if (respuesta.status === 204) return null

  const datos = await respuesta.json().catch(() => null)

  if (!respuesta.ok) {
    // El backend responde siempre con la misma forma: message y fieldErrors.
    throw new ErrorApi(
      datos?.message ?? 'Ocurrió un error inesperado.',
      respuesta.status,
      datos?.fieldErrors,
    )
  }
  return datos
}

export const api = {
  // RF-001 / HU-001. Ya no devuelve token: la cuenta nace sin verificar
  registro: (datos) => peticion('/auth/registro', { metodo: 'POST', cuerpo: datos, autenticada: false }),
  // RF-002 / HU-002
  login: (datos) => peticion('/auth/login', { metodo: 'POST', cuerpo: datos, autenticada: false }),
  // RF-003 / HU-003
  miPerfil: () => peticion('/usuarios/me'),
  // RF-025 / HU-021. Al verificar sí devuelve token
  verificar: (datos) => peticion('/auth/verificar', { metodo: 'POST', cuerpo: datos, autenticada: false }),
  // RF-026 / HU-022
  reenviarCodigo: (correo) => peticion('/auth/reenviar-codigo', { metodo: 'POST', cuerpo: { correo }, autenticada: false }),
  // RF-027 / HU-023
  recuperar: (correo) => peticion('/auth/recuperar', { metodo: 'POST', cuerpo: { correo }, autenticada: false }),
  // RF-028 / HU-024
  restablecer: (datos) => peticion('/auth/restablecer', { metodo: 'POST', cuerpo: datos, autenticada: false }),

  // --- Cuentas (RF-006 a RF-008). Contrato: docs/api/contrato-cuentas.md ---
  cuentas: (incluirInactivas = false) =>
    peticion(`/cuentas?incluirInactivas=${incluirInactivas}`),
  crearCuenta: (datos) => peticion('/cuentas', { metodo: 'POST', cuerpo: datos }),
  editarCuenta: (id, datos) => peticion(`/cuentas/${id}`, { metodo: 'PUT', cuerpo: datos }),
  desactivarCuenta: (id) => peticion(`/cuentas/${id}/desactivar`, { metodo: 'PATCH' }),
  activarCuenta: (id) => peticion(`/cuentas/${id}/activar`, { metodo: 'PATCH' }),
  /**
   * RF-044. Abonar a una tarjeta. Es una transferencia, no un ingreso: el dinero
   * sale de otra cuenta y baja la deuda, sin inflar los ingresos del mes.
   */
  abonarTarjeta: (id, datos) => peticion(`/cuentas/${id}/abonos`, { metodo: 'POST', cuerpo: datos }),

  // --- Gastos fijos (RF-046) ---
  gastosFijos: (incluirInactivos = false) =>
    peticion(`/gastos-fijos?incluirInactivos=${incluirInactivos}`),
  crearGastoFijo: (datos) => peticion('/gastos-fijos', { metodo: 'POST', cuerpo: datos }),
  editarGastoFijo: (id, datos) => peticion(`/gastos-fijos/${id}`, { metodo: 'PUT', cuerpo: datos }),
  desactivarGastoFijo: (id) => peticion(`/gastos-fijos/${id}/desactivar`, { metodo: 'PATCH' }),
  activarGastoFijo: (id) => peticion(`/gastos-fijos/${id}/activar`, { metodo: 'PATCH' }),

  // --- Alertas (RF-047). Se calculan al pedirlas: no hay tabla que consultar ---
  alertas: () => peticion('/alertas'),

  // --- Obligaciones (RF-035 a RF-039) ---
  obligaciones: (soloActivas = true) => peticion(`/obligaciones?soloActivas=${soloActivas}`),
  patrimonio: () => peticion('/obligaciones/patrimonio'),
  crearObligacion: (datos) => peticion('/obligaciones', { metodo: 'POST', cuerpo: datos }),
  editarObligacion: (id, datos) => peticion(`/obligaciones/${id}`, { metodo: 'PUT', cuerpo: datos }),
  cancelarObligacion: (id) => peticion(`/obligaciones/${id}/cancelar`, { metodo: 'PATCH' }),
  pagarObligacion: (id, datos) => peticion(`/obligaciones/${id}/pagos`, { metodo: 'POST', cuerpo: datos }),
  pagosDeObligacion: (id) => peticion(`/obligaciones/${id}/pagos`),

  // --- Categorias (RF-009 a RF-011) ---
  categorias: (tipo) => peticion(`/categorias${tipo ? `?tipo=${tipo}` : ''}`),
  crearCategoria: (datos) => peticion('/categorias', { metodo: 'POST', cuerpo: datos }),
  editarCategoria: (id, datos) => peticion(`/categorias/${id}`, { metodo: 'PUT', cuerpo: datos }),
  desactivarCategoria: (id) => peticion(`/categorias/${id}/desactivar`, { metodo: 'PATCH' }),
  activarCategoria: (id) => peticion(`/categorias/${id}/activar`, { metodo: 'PATCH' }),
  todasLasCategorias: () => peticion('/categorias?soloActivas=false'),

  // --- Movimientos (RF-012 a RF-016) ---
  movimientos: (filtros = {}) => {
    const q = new URLSearchParams(
      Object.entries(filtros).filter(([, v]) => v !== '' && v != null)
    ).toString()
    return peticion(`/transacciones${q ? `?${q}` : ''}`)
  },
  crearMovimiento: (datos) => peticion('/transacciones', { metodo: 'POST', cuerpo: datos }),
  editarMovimiento: (id, datos) => peticion(`/transacciones/${id}`, { metodo: 'PUT', cuerpo: datos }),
  borrarMovimiento: (id) => peticion(`/transacciones/${id}`, { metodo: 'DELETE' }),

  // --- Presupuestos (RF-017 a RF-020) ---
  presupuestos: (anio, mes) => peticion(`/presupuestos?anio=${anio}&mes=${mes}`),
  crearPresupuesto: (datos) => peticion('/presupuestos', { metodo: 'POST', cuerpo: datos }),
  editarPresupuesto: (id, datos) => peticion(`/presupuestos/${id}`, { metodo: 'PUT', cuerpo: datos }),
  desactivarPresupuesto: (id) => peticion(`/presupuestos/${id}/desactivar`, { metodo: 'PATCH' }),

  // --- Reportes (RF-021, RF-022) ---
  panel: (anio, mes) => peticion(`/reportes/panel?anio=${anio}&mes=${mes}`),
  resumenRapido: () => peticion('/reportes/resumen-rapido'),
  /** RF-048. Un punto por día: es lo que permite dibujar la curva del mes. */
  ritmo: (anio, mes) => peticion(`/reportes/ritmo?anio=${anio}&mes=${mes}`),
  /**
   * RF-050. Un punto por mes, y la comparación con el mes anterior. Es el
   * único endpoint que ve más de un mes: los demás solo saben del presente.
   */
  historico: (meses = 6) => peticion(`/reportes/historico?meses=${meses}`),

  // --- Metas de ahorro (RF-032 a RF-034) ---
  metas: (estado) => peticion(`/metas${estado ? `?estado=${estado}` : ''}`),
  crearMeta: (datos) => peticion('/metas', { metodo: 'POST', cuerpo: datos }),
  editarMeta: (id, datos) => peticion(`/metas/${id}`, { metodo: 'PUT', cuerpo: datos }),
  abonarMeta: (id, datos) => peticion(`/metas/${id}/abonos`, { metodo: 'POST', cuerpo: datos }),
  cancelarMeta: (id) => peticion(`/metas/${id}/cancelar`, { metodo: 'PATCH' }),

  // --- Administracion (RF-023, RF-024). Solo para rol administrador ---
  adminUsuarios: () => peticion('/admin/usuarios'),
  adminResumen: () => peticion('/admin/resumen'),
  adminAuditoria: () => peticion('/admin/auditoria'),
  adminDesactivar: (id) => peticion(`/admin/usuarios/${id}/desactivar`, { metodo: 'PATCH' }),
  adminActivar: (id) => peticion(`/admin/usuarios/${id}/activar`, { metodo: 'PATCH' }),
}

export const ES_ADMIN = (usuario) => usuario?.rol === 'ROLE_ADMIN'

/**
 * Tipos de crédito y préstamo. Los valores los fija el backend.
 *
 * 'TARJETA_CREDITO' salió de esta lista: las tarjetas se registran en Cuentas,
 * donde tienen cupo y sus gastos quedan categorizados. Estaba en los dos sitios
 * y quien registraba la misma tarjeta en ambos veía su deuda restada dos veces
 * del patrimonio.
 */
export const TIPOS_DE_OBLIGACION = [
  { valor: 'PRESTAMO_BANCARIO', etiqueta: 'Préstamo bancario' },
  { valor: 'PRESTAMO_PERSONAL', etiqueta: 'Préstamo personal' },
  { valor: 'CREDITO_HIPOTECARIO', etiqueta: 'Crédito hipotecario' },
  { valor: 'CREDITO_VEHICULO', etiqueta: 'Crédito de vehículo' },
  { valor: 'OTRO', etiqueta: 'Otro' },
]

export const MESES = ['Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
  'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre']

/** RF-046. Cada cuánto vuelve un compromiso. Los valores los fija el backend. */
export const PERIODICIDADES = [
  { valor: 'MENSUAL', etiqueta: 'Cada mes' },
  { valor: 'QUINCENAL', etiqueta: 'Cada quincena' },
  { valor: 'SEMANAL', etiqueta: 'Cada semana' },
]

/**
 * Versión corta para las listas. Escribir "Cada mes" al lado de cada monto
 * ocupa media línea; "al mes" cabe junto a la cifra.
 */
export const PERIODICIDAD_CORTA = {
  MENSUAL: 'al mes',
  QUINCENAL: 'cada quincena',
  SEMANAL: 'cada semana',
}

/**
 * RF-046 (DEF-19). Solo aplica cuando periodicidad es SEMANAL: ahí diaPago no
 * es un día del mes sino un día de la semana, 1 lunes a 7 domingo (ISO-8601,
 * igual que el backend). Antes el formulario mostraba el mismo campo numérico
 * 1-28 para las tres periodicidades, así que "cada viernes" no se podía decir.
 */
export const DIAS_SEMANA = [
  { valor: 1, etiqueta: 'Lunes' },
  { valor: 2, etiqueta: 'Martes' },
  { valor: 3, etiqueta: 'Miércoles' },
  { valor: 4, etiqueta: 'Jueves' },
  { valor: 5, etiqueta: 'Viernes' },
  { valor: 6, etiqueta: 'Sábado' },
  { valor: 7, etiqueta: 'Domingo' },
]

/**
 * RF-047. Cómo se pinta cada severidad. El texto acompaña siempre al color:
 * un usuario que no distingue rojo de ámbar tiene que poder saber qué es grave
 * (RNF-008).
 */
export const SEVERIDADES = {
  ALTA: { clase: 'error', texto: 'Urgente' },
  MEDIA: { clase: 'aviso', texto: 'Atención' },
  BAJA: { clase: 'aviso', texto: 'Para tener en cuenta' },
}

/** Orden en que se muestran: lo grave primero. */
export const PESO_SEVERIDAD = { ALTA: 0, MEDIA: 1, BAJA: 2 }

/** Fecha de hoy en el formato que espera el backend, sin desfase de zona horaria. */
export const hoyISO = () => {
  const d = new Date()
  return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 10)
}

// ===================================================== RF-049: profundización
//
// Estas funciones son el pegamento entre módulos. La idea: toda cifra agregada
// que se muestre en cualquier pantalla debe poder abrir los movimientos que la
// componen, sin que el usuario configure un filtro a mano. El backend ya acepta
// `categoriaId`, `cuentaId`, `desde`, `hasta` y `tipo` en /transacciones; lo que
// faltaba era que el frontend construyera esos enlaces.

/** Primer y último día de un mes, en el formato que espera el backend. */
export function rangoDelMes(anio, mes) {
  const dosDigitos = (n) => String(n).padStart(2, '0')
  // new Date(anio, mes, 0) es el último día del mes `mes` (1-12), porque el
  // constructor cuenta los meses desde 0 y el día 0 retrocede uno.
  const ultimo = new Date(anio, mes, 0).getDate()
  return {
    desde: `${anio}-${dosDigitos(mes)}-01`,
    hasta: `${anio}-${dosDigitos(mes)}-${dosDigitos(ultimo)}`,
  }
}

/**
 * Construye el enlace a Movimientos ya filtrado. Los campos vacíos no se
 * escriben, para que el enlace diga solo lo que de verdad filtra.
 */
export function enlaceMovimientos(filtros = {}) {
  const q = new URLSearchParams()
  for (const [clave, valor] of Object.entries(filtros)) {
    if (valor !== '' && valor != null) q.set(clave, String(valor))
  }
  const cadena = q.toString()
  return `/movimientos${cadena ? `?${cadena}` : ''}`
}

/** Enlace a los movimientos de una categoría dentro de un mes. */
export const enlaceCategoriaDelMes = (categoriaId, anio, mes) =>
  enlaceMovimientos({ categoriaId, ...rangoDelMes(anio, mes) })

const fechaCorta = (iso) => {
  const [a, m, d] = iso.split('-').map(Number)
  return `${d} de ${MESES[m - 1].toLowerCase()} de ${a}`
}

function describirPeriodo(desde, hasta) {
  if (!desde && !hasta) return ''
  if (!desde) return `hasta el ${fechaCorta(hasta)}`
  if (!hasta) return `desde el ${fechaCorta(desde)}`

  const [a1, m1, d1] = desde.split('-').map(Number)
  const [a2, m2, d2] = hasta.split('-').map(Number)
  // Si el rango es exactamente un mes completo, se nombra el mes. "de agosto de
  // 2026" se lee mucho mejor que "del 1 de agosto al 31 de agosto".
  if (a1 === a2 && m1 === m2 && d1 === 1 && d2 === new Date(a2, m2, 0).getDate()) {
    return `de ${MESES[m1 - 1].toLowerCase()} de ${a1}`
  }
  return `del ${fechaCorta(desde)} al ${fechaCorta(hasta)}`
}

/**
 * Traduce un filtro a una frase en español, para el aviso de Movimientos.
 *
 * Se arma con los nombres reales de cuenta y categoría, no con los ids: al
 * usuario no le dice nada "categoriaId=3".
 */
export function describirFiltro(filtros, cuentas = [], categorias = []) {
  const mismo = (a, b) => String(a) === String(b)
  const categoria = categorias.find((c) => mismo(c.id, filtros.categoriaId))
  const cuenta = cuentas.find((c) => mismo(c.id, filtros.cuentaId))

  const partes = [
    filtros.tipo === 'INGRESO' ? 'los ingresos'
      : filtros.tipo === 'GASTO' ? 'los gastos'
        : filtros.tipo === 'TRANSFERENCIA' ? 'las transferencias'
          : 'los movimientos',
  ]
  if (categoria) partes.push(`de ${categoria.nombre}`)
  if (cuenta) partes.push(`en ${cuenta.nombre}`)

  const periodo = describirPeriodo(filtros.desde, filtros.hasta)
  if (periodo) partes.push(periodo)

  return partes.join(' ')
}

// ====================================================== RF-052: marca de ritmo
//
// Qué fracción del período ya transcurrió, de 0 a 1.
//
// POR QUÉ ESTO CAMBIA UNA BARRA DE PRESUPUESTO
// Una barra al 60% no dice nada por sí sola: el día 10 es una alarma y el día
// 27 es una buena noticia, y hoy las dos se ven idénticas. Sabiendo en qué
// punto del período estamos, la barra pasa de informar a avisar.
//
// Se calcula con el `desde` y el `hasta` del propio presupuesto, no con el día
// del mes: un presupuesto quincenal o semanal cubre un tramo más corto, y usar
// el mes entero pondría la marca en el lugar equivocado justo en esos casos.
//
// Devuelve null cuando el período no está en curso —ya terminó o no ha
// empezado—, porque "deberías ir por el 40%" sobre un mes cerrado no significa
// nada. Sin marca es mejor que con una marca falsa.
export function ritmoDelPeriodo(desde, hasta, hoy = new Date()) {
  if (!desde || !hasta) return null

  // Se parte la fecha a mano en vez de usar new Date(iso): esa forma
  // interpreta la cadena como UTC y en Colombia (UTC-5) devuelve el día
  // anterior, corriendo la marca un día entero.
  const aFecha = (iso) => {
    const [a, m, d] = iso.split('-').map(Number)
    return new Date(a, m - 1, d)
  }

  const inicio = aFecha(desde)
  const fin = aFecha(hasta)
  const ahora = new Date(hoy.getFullYear(), hoy.getMonth(), hoy.getDate())

  if (ahora < inicio || ahora > fin) return null

  const DIA = 24 * 60 * 60 * 1000
  // +1 en los dos lados: un período de un solo día está transcurrido al 100%,
  // no al 0%.
  const total = Math.round((fin - inicio) / DIA) + 1
  const corridos = Math.round((ahora - inicio) / DIA) + 1
  return corridos / total
}

/** RN-020: la tarjeta de credito no es dinero disponible, es deuda. */
export const ES_PASIVO = (tipo) => tipo === 'TARJETA_CREDITO'

/** Etiquetas de los seis tipos. Los valores los fija el backend. */
export const TIPOS_DE_CUENTA = [
  { valor: 'EFECTIVO', etiqueta: 'Efectivo' },
  { valor: 'AHORROS', etiqueta: 'Cuenta de ahorros' },
  { valor: 'CORRIENTE', etiqueta: 'Cuenta corriente' },
  { valor: 'TARJETA_CREDITO', etiqueta: 'Tarjeta de crédito' },
  { valor: 'BILLETERA_DIGITAL', etiqueta: 'Billetera digital' },
  { valor: 'OTRO', etiqueta: 'Otro' },
]

export const etiquetaDeTipo = (valor) =>
  TIPOS_DE_CUENTA.find((t) => t.valor === valor)?.etiqueta ?? valor

/** El servidor manda el número; aquí solo se presenta. */
export const formatearDinero = (valor, moneda = 'COP') =>
  new Intl.NumberFormat('es-CO', {
    style: 'currency', currency: moneda, minimumFractionDigits: 0, maximumFractionDigits: 2,
  }).format(valor ?? 0)

/** RF-029. Sale de la aplicación: el navegador va a Google y vuelve con el token. */
export function irAGoogle() {
  const raiz = BASE.replace(/\/api\/v1\/?$/, '')
  window.location.href = `${raiz}/oauth2/authorization/google`
}
