import { useEffect, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { ErrorApi } from '../api/cliente'
import Campo from '../componentes/Campo'
import Captcha from '../componentes/Captcha'
import Boton from '../componentes/Boton'
import Alerta from '../componentes/Alerta'
import BotonGoogle from '../componentes/BotonGoogle'
import { mensajeDeGoogle } from '../auth/mensajesGoogle'
import { IconoMarca } from '../componentes/Iconos'

/** UI-001 — Iniciar sesión. Implementa HU-002 / RF-002. */
export default function IniciarSesion() {
  const { iniciarSesion } = useAuth()
  const navegar = useNavigate()
  const [params, setParams] = useSearchParams()
  const [datos, setDatos] = useState({ correo: '', contrasena: '' })
  const [errores, setErrores] = useState({})
  const [errorGeneral, setErrorGeneral] = useState(null)
  const [enviando, setEnviando] = useState(false)
  const [verClave, setVerClave] = useState(false)
  const [captchaToken, setCaptchaToken] = useState('')

  // Igual que en el registro: si no hay clave configurada no se exige nada,
  // porque el backend tambien lo tiene apagado.
  const captchaExigido = Boolean(import.meta.env.VITE_CAPTCHA_SITE_KEY)

  /*
    DEF-023. El error del acceso con Google se quedaba en la barra de
    direcciones.

    Cuando Google autentica pero FinMind rechaza —por ejemplo, porque ese
    correo ya tiene una cuenta con contrasena (RN-013)—, el backend devuelve
    el navegador a /iniciar-sesion?error=<mensaje>. Esta pantalla no leia ese
    parametro, asi que el mensaje quedaba visible SOLO en la URL: el usuario
    volvia al formulario sin ninguna explicacion de por que no entro.

    Un mensaje de error que solo existe en la barra de direcciones es un
    mensaje que nadie lee.

    Se muestra en la misma alerta que los errores del formulario, y despues se
    limpia el parametro de la URL: si no, al recargar la pagina volveria a
    aparecer un error que ya no corresponde a nada.
  */
  useEffect(() => {
    const codigo = params.get('error')
    if (!codigo) return
    // Llega un código corto ('cuenta_sin_verificar'), no la frase. El texto
    // sale de mensajesGoogle.js: así la URL queda legible y la redacción se
    // cambia sin tocar el backend.
    setErrorGeneral(mensajeDeGoogle(codigo))
    const limpios = new URLSearchParams(params)
    limpios.delete('error')
    setParams(limpios, { replace: true })
  }, [params, setParams])

  const cambiar = (e) => setDatos({ ...datos, [e.target.name]: e.target.value })

  async function enviar(e) {
    e.preventDefault()
    setErrores({}); setErrorGeneral(null); setEnviando(true)
    try {
      await iniciarSesion(datos.correo, datos.contrasena, captchaToken)
      navegar('/panel', { replace: true })
    } catch (err) {
      if (err instanceof ErrorApi && err.erroresPorCampo) setErrores(err.erroresPorCampo)
      // El backend responde el mismo mensaje para correo inexistente y contraseña
      // errada, a propósito: distinguirlos revelaría qué correos están registrados.
      else setErrorGeneral(err.message)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="pantalla-auth">
      <aside className="pantalla-auth__lateral">
        <div className="marca"><span className="marca__logo"><IconoMarca /></span> FinMind</div>
        <h1 className="pantalla-auth__lema">Tus finanzas,<br />en orden.</h1>
        <p className="pantalla-auth__bajada">
          Registra ingresos y gastos, define presupuestos y mira a dónde se va tu dinero.
        </p>
      </aside>

      <main className="pantalla-auth__panel">
        <form className="tarjeta" onSubmit={enviar} noValidate>
          <h2 className="tarjeta__titulo">Iniciar sesión</h2>
          <p className="tarjeta__bajada">Ingresa con tu correo y contraseña.</p>

          {errorGeneral && (
            <Alerta tipo="error" titulo="No pudimos iniciar sesión">{errorGeneral}</Alerta>
          )}

          <Campo
            id="correo" name="correo" type="email" autoComplete="email"
            etiqueta="Correo electrónico" placeholder="kevin@ejemplo.com"
            value={datos.correo} onChange={cambiar} error={errores.correo} required
          />
          <div className="campo-con-accion">
            <Campo
              id="contrasena" name="contrasena" type={verClave ? 'text' : 'password'}
              autoComplete="current-password" etiqueta="Contraseña"
              value={datos.contrasena} onChange={cambiar} error={errores.contrasena} required
            />
            <button type="button" className="enlace-boton" onClick={() => setVerClave(!verClave)}>
              {verClave ? 'Ocultar' : 'Mostrar'}
            </button>
          </div>

          <Captcha onToken={setCaptchaToken} />

          <Boton type="submit" cargando={enviando}
                 disabled={captchaExigido && !captchaToken}>Iniciar sesión</Boton>

          <p className="tarjeta__pie">
            <Link to="/recuperar">¿Olvidaste tu contraseña?</Link>
          </p>

          <BotonGoogle texto="Entrar con Google" />

          <p className="tarjeta__pie">
            ¿No tienes cuenta? <Link to="/crear-cuenta">Crear cuenta</Link>
          </p>
        </form>
      </main>
    </div>
  )
}
