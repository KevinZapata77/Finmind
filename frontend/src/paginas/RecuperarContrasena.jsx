import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api } from '../api/cliente'
import Campo from '../componentes/Campo'
import Boton from '../componentes/Boton'
import Alerta from '../componentes/Alerta'
import { IconoMarca } from '../componentes/Iconos'

/** UI-011 — Recuperar contraseña. Implementa HU-023 / RF-027. */
export default function RecuperarContrasena() {
  const navegar = useNavigate()
  const [correo, setCorreo] = useState('')
  const [error, setError] = useState(null)
  const [usaGoogle, setUsaGoogle] = useState(false)
  const [enviando, setEnviando] = useState(false)

  async function enviar(e) {
    e.preventDefault()
    setError(null); setUsaGoogle(false); setEnviando(true)
    try {
      const r = await api.recuperar(correo)

      /*
        DEF-025. La cuenta entra solo con Google: no hay contraseña que
        restablecer.

        Antes esta respuesta era idéntica a las demás y la persona terminaba en
        la pantalla de escribir el código, esperando un correo que no existía.
        Volvía a pedirlo, lo buscaba en el spam, y concluía que la aplicación
        estaba rota. No lo estaba: simplemente no había nada que enviar.

        Se decide por el campo usaGoogle y no leyendo el texto del mensaje: la
        navegación no puede depender de cómo esté redactada una frase.

        Y NO se navega. Quedarse aquí importa, porque el botón de Google está en
        la pantalla anterior, a un clic del enlace de abajo. Mandarla a
        /restablecer sería alejarla del único camino que le sirve.
      */
      if (r.usaGoogle) {
        setUsaGoogle(true)
        return
      }

      // RN-014: la respuesta es la misma exista o no la cuenta. Decir "ese correo
      // no está registrado" le confirmaría a un atacante qué direcciones existen.
      navegar('/restablecer', { state: { correo, aviso: r.message } })
    } catch (err) {
      setError(err.message)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="pantalla-auth">
      <aside className="pantalla-auth__lateral">
        <div className="marca"><span className="marca__logo"><IconoMarca /></span> FinMind</div>
        <h1 className="pantalla-auth__lema">Te ayudamos a volver.</h1>
      </aside>

      <main className="pantalla-auth__panel">
        <form className="tarjeta" onSubmit={enviar} noValidate>
          <h2 className="tarjeta__titulo">Recuperar contraseña</h2>
          <p className="tarjeta__bajada">
            Escribe tu correo y te enviaremos un código para crear una nueva.
          </p>

          {error && <Alerta tipo="error" titulo="No pudimos continuar">{error}</Alerta>}

          {/*
            Aviso, no error. El color importa: en rojo se lee como "hiciste algo
            mal", y quien llega aquí no hizo nada mal. Solo está en la puerta
            equivocada, y la buena está a un clic.
          */}
          {usaGoogle && (
            <Alerta tipo="aviso" titulo="Esta cuenta entra con Google">
              No tiene contraseña que restablecer, así que no hay ningún código
              que enviarte. Vuelve a iniciar sesión y usa el botón
              <strong> Continuar con Google</strong>.
              <br />
              <Link to="/iniciar-sesion">Ir a iniciar sesión con Google</Link>
            </Alerta>
          )}

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
