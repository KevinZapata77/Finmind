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
  // RF-001 / HU-001
  registro: (datos) => peticion('/auth/registro', { metodo: 'POST', cuerpo: datos, autenticada: false }),
  // RF-002 / HU-002
  login: (datos) => peticion('/auth/login', { metodo: 'POST', cuerpo: datos, autenticada: false }),
  // RF-003 / HU-003
  miPerfil: () => peticion('/usuarios/me'),
}
