import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { ErrorApi } from '../api/cliente'
import Campo from '../componentes/Campo'
import Boton from '../componentes/Boton'
import Alerta from '../componentes/Alerta'

/** UI-002 — Crear cuenta. Implementa HU-001 / RF-001. */
export default function CrearCuenta() {
  const { crearCuenta } = useAuth()
  const navegar = useNavigate()
  const [datos, setDatos] = useState({ nombre: '', apellido: '', correo: '', contrasena: '' })
  const [errores, setErrores] = useState({})
  const [errorGeneral, setErrorGeneral] = useState(null)
  const [enviando, setEnviando] = useState(false)
  const [acepta, setAcepta] = useState(false)

  const cambiar = (e) => setDatos({ ...datos, [e.target.name]: e.target.value })

  // Solo orientación visual. La regla real (mínimo 8 caracteres) la valida el backend.
  const fuerza = datos.contrasena.length === 0 ? null
    : datos.contrasena.length < 8 ? { texto: 'Muy corta', pct: 25, tipo: 'error' }
    : datos.contrasena.length < 12 ? { texto: 'Aceptable', pct: 60, tipo: 'aviso' }
    : { texto: 'Buena', pct: 100, tipo: 'exito' }

  async function enviar(e) {
    e.preventDefault()
    setErrores({}); setErrorGeneral(null); setEnviando(true)
    try {
      await crearCuenta(datos)
      navegar('/panel', { replace: true })
    } catch (err) {
      if (err instanceof ErrorApi && err.erroresPorCampo) setErrores(err.erroresPorCampo)
      else setErrorGeneral(err.message)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="pantalla-auth">
      <aside className="pantalla-auth__lateral">
        <div className="marca"><span className="marca__logo">F</span> FinMind</div>
        <h1 className="pantalla-auth__lema">Empieza gratis.</h1>
        <p className="pantalla-auth__bajada">
          Toma el control de tu dinero en menos de cinco minutos.
        </p>
      </aside>

      <main className="pantalla-auth__panel">
        <form className="tarjeta" onSubmit={enviar} noValidate>
          <h2 className="tarjeta__titulo">Crear cuenta</h2>
          <p className="tarjeta__bajada">Todos los campos son obligatorios.</p>

          {errorGeneral && (
            <Alerta tipo="error" titulo="No pudimos crear la cuenta">{errorGeneral}</Alerta>
          )}

          <div className="fila-doble">
            <Campo id="nombre" name="nombre" etiqueta="Nombre" placeholder="Kevin"
              value={datos.nombre} onChange={cambiar} error={errores.nombre} required />
            <Campo id="apellido" name="apellido" etiqueta="Apellido" placeholder="Zapata"
              value={datos.apellido} onChange={cambiar} error={errores.apellido} required />
          </div>

          <Campo id="correo" name="correo" type="email" autoComplete="email"
            etiqueta="Correo electrónico" placeholder="kevin@ejemplo.com"
            value={datos.correo} onChange={cambiar} error={errores.correo} required />

          <Campo id="contrasena" name="contrasena" type="password" autoComplete="new-password"
            etiqueta="Contraseña" ayuda="Mínimo 8 caracteres."
            value={datos.contrasena} onChange={cambiar} error={errores.contrasena} required />

          {fuerza && (
            <div className="fuerza">
              <span className="fuerza__rotulo">Fortaleza de la contraseña</span>
              <div className="fuerza__barra">
                <div className={`fuerza__relleno fuerza__relleno--${fuerza.tipo}`}
                     style={{ width: `${fuerza.pct}%` }} />
              </div>
              <span className={`fuerza__texto fuerza__texto--${fuerza.tipo}`}>{fuerza.texto}</span>
            </div>
          )}

          <label className="consentimiento">
            <input type="checkbox" checked={acepta} onChange={(e) => setAcepta(e.target.checked)} />
            Acepto el tratamiento de mis datos personales.
          </label>

          <Boton type="submit" cargando={enviando} disabled={!acepta}>Crear cuenta</Boton>

          <p className="tarjeta__pie">
            ¿Ya tienes cuenta? <Link to="/iniciar-sesion">Iniciar sesión</Link>
          </p>
        </form>
      </main>
    </div>
  )
}
