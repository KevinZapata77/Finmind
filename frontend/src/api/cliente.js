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
}

/** Tipos de obligacion. Los valores los fija el backend. */
export const TIPOS_DE_OBLIGACION = [
  { valor: 'TARJETA_CREDITO', etiqueta: 'Tarjeta de crédito' },
  { valor: 'PRESTAMO_BANCARIO', etiqueta: 'Préstamo bancario' },
  { valor: 'PRESTAMO_PERSONAL', etiqueta: 'Préstamo personal' },
  { valor: 'CREDITO_HIPOTECARIO', etiqueta: 'Crédito hipotecario' },
  { valor: 'CREDITO_VEHICULO', etiqueta: 'Crédito de vehículo' },
  { valor: 'OTRO', etiqueta: 'Otro' },
]

export const MESES = ['Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
  'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre']

/** Fecha de hoy en el formato que espera el backend, sin desfase de zona horaria. */
export const hoyISO = () => {
  const d = new Date()
  return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 10)
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
