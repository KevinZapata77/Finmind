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

    /*
      SEG-06. El token llega en el FRAGMENTO (#token=...), no en la consulta.

      Antes venía como ?token=... y aquí se borraba de la barra de direcciones
      de inmediato. Eso arreglaba el historial del navegador, y nada más: la
      cadena de consulta viaja en la línea de petición HTTP, así que la escribe
      en su registro todo lo que esté en el camino —el servidor, un proxy, la
      consola de la plataforma— y también se filtra por el encabezado Referer.
      Un token de sesión en texto plano dentro de un log es un token regalado,
      y los logs viven mucho más que la vigencia del token.

      El navegador nunca manda el fragmento al servidor. Se queda de este lado,
      que es donde tiene que quedarse.

      Se lee de window.location.hash y no con useSearchParams porque ese hook
      solo mira la cadena de consulta.

      El error SÍ sigue viniendo por la consulta: no es secreto, y así el
      servidor puede registrarlo, que para un fallo es lo que uno quiere.
    */
    const token = new URLSearchParams(window.location.hash.replace(/^#/, '')).get('token')
    if (!token) {
      setError(params.get('error') || 'No recibimos la confirmación de Google.')
      return
    }
    // Se limpia igual: el fragmento no llega al servidor, pero sí queda en el
    // historial del navegador y a la vista en la barra de direcciones.
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
