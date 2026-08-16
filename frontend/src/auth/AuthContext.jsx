import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { api, borrarToken, guardarToken, leerToken } from '../api/cliente'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [usuario, setUsuario] = useState(null)
  const [cargando, setCargando] = useState(true)

  // Al abrir la aplicación, si hay token guardado se verifica contra el backend.
  // No se confía en el token sin comprobarlo: puede estar expirado.
  useEffect(() => {
    if (!leerToken()) {
      setCargando(false)
      return
    }
    api.miPerfil()
      .then(setUsuario)
      .catch(() => borrarToken())
      .finally(() => setCargando(false))
  }, [])

  const iniciarSesion = useCallback(async (correo, contrasena) => {
    const r = await api.login({ correo, contrasena })
    guardarToken(r.token)
    setUsuario(r.usuario)
    return r.usuario
  }, [])

  /**
   * El registro ya NO deja al usuario dentro: la cuenta nace sin verificar.
   * Devuelve el usuario para que la pantalla lo lleve a ingresar el código.
   */
  const crearCuenta = useCallback(async (datos) => {
    const r = await api.registro(datos)
    return r.usuario
  }, [])

  /** Tras verificar el código sí llega el token y la sesión queda abierta. */
  const verificarCorreo = useCallback(async (correo, codigo) => {
    const r = await api.verificar({ correo, codigo })
    guardarToken(r.token)
    setUsuario(r.usuario)
    return r.usuario
  }, [])

  const restablecerContrasena = useCallback(async (datos) => {
    const r = await api.restablecer(datos)
    guardarToken(r.token)
    setUsuario(r.usuario)
    return r.usuario
  }, [])

  /** El token llega en la URL de retorno de Google. */
  const entrarConToken = useCallback(async (token) => {
    guardarToken(token)
    const u = await api.miPerfil()
    setUsuario(u)
    return u
  }, [])

  const cerrarSesion = useCallback(() => {
    borrarToken()
    setUsuario(null)
  }, [])

  const valor = useMemo(
    () => ({ usuario, cargando, iniciarSesion, crearCuenta, verificarCorreo,
             restablecerContrasena, entrarConToken, cerrarSesion }),
    [usuario, cargando, iniciarSesion, crearCuenta, verificarCorreo,
     restablecerContrasena, entrarConToken, cerrarSesion],
  )

  return <AuthContext.Provider value={valor}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth debe usarse dentro de AuthProvider')
  return ctx
}
