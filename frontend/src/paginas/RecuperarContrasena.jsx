import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api } from '../api/cliente'
import Campo from '../componentes/Campo'
import Boton from '../componentes/Boton'
import Alerta from '../componentes/Alerta'

/** UI-011 — Recuperar contraseña. Implementa HU-023 / RF-027. */
export default function RecuperarContrasena() {
  const navegar = useNavigate()
  const [correo, setCorreo] = useState('')
  const [error, setError] = useState(null)
  const [enviando, setEnviando] = useState(false)

  async function enviar(e) {
    e.preventDefault()
    setError(null); setEnviando(true)
    try {
      const r = await api.recuperar(correo)
      // RN-014: la respuesta es la misma exista o no la cuenta. Decir "ese correo
      // no está registrado" le confirmaría a un atacante qué direcciones existen.
      navegar('/restablecer', { state: { correo, aviso: r.mensaje } })
    } catch (err) {
      setError(err.message)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="pantalla-auth">
      <aside className="pantalla-auth__lateral">
        <div className="marca"><span className="marca__logo">F</span> FinMind</div>
        <h1 className="pantalla-auth__lema">Te ayudamos a volver.</h1>
      </aside>

      <main className="pantalla-auth__panel">
        <form className="tarjeta" onSubmit={enviar} noValidate>
          <h2 className="tarjeta__titulo">Recuperar contraseña</h2>
          <p className="tarjeta__bajada">
            Escribe tu correo y te enviaremos un código para crear una nueva.
          </p>

          {error && <Alerta tipo="error" titulo="No pudimos continuar">{error}</Alerta>}

          <Campo id="correo" name="correo" type="email" autoComplete="email"
            etiqueta="Correo electrónico" placeholder="kevin@ejemplo.com"
            value={correo} onChange={(e) => setCorreo(e.target.value)} required />

          <Boton type="submit" cargando={enviando} disabled={!correo}>Enviar código</Boton>

          <p className="tarjeta__pie"><Link to="/iniciar-sesion">Volver a iniciar sesión</Link></p>
        </form>
      </main>
    </div>
  )
}
