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
      SEG-08. Ya no llega ningun token por la URL, ni en la consulta ni en el
      fragmento. El backend abrio la cookie HttpOnly antes de devolver el
      navegador, asi que aqui solo queda confirmar quien es.

      Es la version buena del recorrido que hicimos: primero el token venia como
      ?token=..., que queda en el historial y en el registro de todo lo que este
      en el camino. Despues se movio al fragmento, que el navegador no manda al
      servidor. Con la cookie no hace falta ninguno de los dos: el secreto nunca
      pasa por la barra de direcciones.

      Solo se sigue leyendo ?error=..., que no es secreto y conviene que el
      servidor lo pueda registrar.
    */
    const fallo = params.get('error')
    if (fallo) {
      setError(fallo)
      return
    }

    entrarConToken()
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
