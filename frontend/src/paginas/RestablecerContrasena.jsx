import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import Campo from '../componentes/Campo'
import Boton from '../componentes/Boton'
import Alerta from '../componentes/Alerta'
import { IconoMarca } from '../componentes/Iconos'

/** UI-012 — Restablecer contraseña. Implementa HU-024 / RF-028. */
export default function RestablecerContrasena() {
  const { restablecerContrasena } = useAuth()
  const navegar = useNavigate()
  const ubicacion = useLocation()
  const correo = ubicacion.state?.correo || ''

  const [datos, setDatos] = useState({ codigo: '', contrasena: '' })
  const [repetida, setRepetida] = useState('')
  const [error, setError] = useState(null)
  const [enviando, setEnviando] = useState(false)

  useEffect(() => { if (!correo) navegar('/recuperar', { replace: true }) }, [correo, navegar])

  const noCoinciden = repetida.length > 0 && repetida !== datos.contrasena

  async function enviar(e) {
    e.preventDefault()
    if (noCoinciden) return
    setError(null); setEnviando(true)
    try {
      await restablecerContrasena({ correo, ...datos })
      navegar('/panel', { replace: true })
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
        <h1 className="pantalla-auth__lema">Nueva contraseña.</h1>
      </aside>

      <main className="pantalla-auth__panel">
        <form className="tarjeta" onSubmit={enviar} noValidate>
          <h2 className="tarjeta__titulo">Restablecer contraseña</h2>
          <p className="tarjeta__bajada">
            Ingresa el código enviado a <strong>{correo}</strong> y tu nueva contraseña.
          </p>

          {ubicacion.state?.aviso && <Alerta tipo="exito">{ubicacion.state.aviso}</Alerta>}
          {error && <Alerta tipo="error" titulo="No pudimos cambiarla">{error}</Alerta>}

          <Campo id="codigo" name="codigo" etiqueta="Código de 6 dígitos"
            inputMode="numeric" maxLength={6} placeholder="482913"
            value={datos.codigo} onChange={(e) => setDatos({ ...datos, codigo: e.target.value })} required />

          <Campo id="contrasena" name="contrasena" type="password" autoComplete="new-password"
            etiqueta="Nueva contraseña" ayuda="Mínimo 8 caracteres."
            value={datos.contrasena} onChange={(e) => setDatos({ ...datos, contrasena: e.target.value })} required />

          <Campo id="repetida" name="repetida" type="password" autoComplete="new-password"
            etiqueta="Repite la contraseña"
            error={noCoinciden ? 'Las contraseñas no coinciden.' : null}
            value={repetida} onChange={(e) => setRepetida(e.target.value)} required />

          <Boton type="submit" cargando={enviando}
            disabled={noCoinciden || datos.codigo.length !== 6 || !datos.contrasena}>
            Cambiar contraseña
          </Boton>

          <p className="tarjeta__pie"><Link to="/iniciar-sesion">Volver a iniciar sesión</Link></p>
        </form>
      </main>
    </div>
  )
}
