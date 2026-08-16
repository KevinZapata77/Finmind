import { useEffect, useRef, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import Alerta from '../componentes/Alerta'

/**
 * Punto de regreso desde Google. No es una pantalla que el usuario use:
 * toma el token de la URL, abre la sesión y sale hacia el panel.
 */
export default function CallbackGoogle() {
  const { entrarConToken } = useAuth()
  const navegar = useNavigate()
  const [params] = useSearchParams()
  const [error, setError] = useState(null)
  const yaCorrio = useRef(false)

  useEffect(() => {
    if (yaCorrio.current) return
    yaCorrio.current = true

    const token = params.get('token')
    if (!token) {
      setError(params.get('error') || 'No recibimos la confirmación de Google.')
      return
    }
    // Se borra el token de la barra de direcciones antes de cualquier otra cosa,
    // para que no quede guardado en el historial del navegador.
    window.history.replaceState({}, '', '/oauth2/callback')

    entrarConToken(token)
      .then(() => navegar('/panel', { replace: true }))
      .catch((err) => setError(err.message))
  }, [params, entrarConToken, navegar])

  if (error) {
    return (
      <div className="pantalla-auth pantalla-auth--simple">
        <main className="pantalla-auth__panel">
          <div className="tarjeta">
            <h2 className="tarjeta__titulo">No pudimos iniciar sesión</h2>
            <Alerta tipo="error">{error}</Alerta>
            <p className="tarjeta__pie"><a href="/iniciar-sesion">Volver a iniciar sesión</a></p>
          </div>
        </main>
      </div>
    )
  }
  return <div className="cargando-pantalla" role="status">Confirmando tu identidad…</div>
}
