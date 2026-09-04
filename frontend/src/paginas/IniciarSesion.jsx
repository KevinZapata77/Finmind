import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { ErrorApi } from '../api/cliente'
import Campo from '../componentes/Campo'
import Boton from '../componentes/Boton'
import Alerta from '../componentes/Alerta'
import BotonGoogle from '../componentes/BotonGoogle'
import { IconoMarca } from '../componentes/Iconos'

/** UI-001 — Iniciar sesión. Implementa HU-002 / RF-002. */
export default function IniciarSesion() {
  const { iniciarSesion } = useAuth()
  const navegar = useNavigate()
  const [datos, setDatos] = useState({ correo: '', contrasena: '' })
  const [errores, setErrores] = useState({})
  const [errorGeneral, setErrorGeneral] = useState(null)
  const [enviando, setEnviando] = useState(false)
  const [verClave, setVerClave] = useState(false)

  const cambiar = (e) => setDatos({ ...datos, [e.target.name]: e.target.value })

  async function enviar(e) {
    e.preventDefault()
    setErrores({}); setErrorGeneral(null); setEnviando(true)
    try {
      await iniciarSesion(datos.correo, datos.contrasena)
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

          <Boton type="submit" cargando={enviando}>Iniciar sesión</Boton>

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
