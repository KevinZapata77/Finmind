import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { api, sesionActual } from '../api/cliente'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [usuario, setUsuario] = useState(null)
  const [cargando, setCargando] = useState(true)

  /*
    SEG-08. Al abrir la aplicación se le pregunta al servidor si hay sesión.

    Antes esto empezaba con `if (!leerToken())`: si no había token guardado, se
    daba por hecho que no había sesión y no se preguntaba nada. Con una cookie
    HttpOnly ese atajo es imposible —el navegador tiene la cookie y no la
    muestra—, así que la única fuente de verdad es el servidor.

    Y de paso queda mejor que antes. El token guardado podía estar vencido, y
    aun así la aplicación arrancaba creyendo que había sesión hasta que fallara
    la primera petición de verdad. Ahora la respuesta del servidor es la
    respuesta: perfil, o null.

    sesionActual() distingue un 401 —no hay sesión, es normal— de una caída de
    red, que no debe hacerse pasar por sesión cerrada.
  */
  useEffect(() => {
    sesionActual()
      .then(setUsuario)
      .catch(() => setUsuario(null))
      .finally(() => setCargando(false))
  }, [])

  /*
    Ya no se guarda el token. El backend lo sigue devolviendo en el cuerpo para
    Swagger, curl y las pruebas, pero si el frontend lo guardara en
    sessionStorage volvería justo el problema que este cambio vino a resolver.
    Se ignora a propósito.
  */
  const iniciarSesion = useCallback(async (correo, contrasena) => {
    const r = await api.login({ correo, contrasena })
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

  /** Tras verificar el código la sesión queda abierta: el backend pone la cookie. */
  const verificarCorreo = useCallback(async (correo, codigo) => {
    const r = await api.verificar({ correo, codigo })
    setUsuario(r.usuario)
    return r.usuario
  }, [])

  const restablecerContrasena = useCallback(async (datos) => {
    const r = await api.restablecer(datos)
    setUsuario(r.usuario)
    return r.usuario
  }, [])

  /*
    SEG-08. El retorno de Google ya no trae token que haya que guardar: el
    backend abrió la cookie antes de devolver el navegador. Acá solo queda
    confirmar quién es, preguntándole al servidor.

    Se conserva el nombre entrarConToken para no tocar CallbackGoogle ni el
    resto, pero ya no recibe nada. Es, en el fondo, "confirmar la sesión que el
    servidor ya abrió".
  */
  const entrarConToken = useCallback(async () => {
    const u = await api.miPerfil()
    setUsuario(u)
    return u
  }, [])

  /*
    SEG-08. Cerrar sesión ahora tiene que pasar por el servidor.

    Antes bastaba con borrar el token de sessionStorage. La cookie es HttpOnly,
    así que este código no la puede borrar: solo la puede quitar quien la puso.

    El usuario se marca como desconectado PASE LO QUE PASE con la petición. Si
    el backend está caído, el botón tiene que funcionar igual: dejar a alguien
    con la pantalla abierta porque no se pudo avisar al servidor es peor que
    limpiar de este lado. La cookie caduca sola con el token.
  */
  const cerrarSesion = useCallback(async () => {
    try {
      await api.logout()
    } catch {
      // Da igual por qué falló: la sesión se cierra de este lado igual.
    }
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
