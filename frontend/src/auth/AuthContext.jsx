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

  const crearCuenta = useCallback(async (datos) => {
    const r = await api.registro(datos)
    guardarToken(r.token)
    setUsuario(r.usuario)
    return r.usuario
  }, [])

  const cerrarSesion = useCallback(() => {
    borrarToken()
    setUsuario(null)
  }, [])

  const valor = useMemo(
    () => ({ usuario, cargando, iniciarSesion, crearCuenta, cerrarSesion }),
    [usuario, cargando, iniciarSesion, crearCuenta, cerrarSesion],
  )

  return <AuthContext.Provider value={valor}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth debe usarse dentro de AuthProvider')
  return ctx
}
