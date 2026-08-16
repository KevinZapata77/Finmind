import { useEffect, useRef, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { api } from '../api/cliente'
import Boton from '../componentes/Boton'
import Alerta from '../componentes/Alerta'

const ESPERA_REENVIO = 60 // segundos

/** UI-010 — Verificar correo. Implementa HU-021 / RF-025 y HU-022 / RF-026. */
export default function VerificarCorreo() {
  const { verificarCorreo } = useAuth()
  const navegar = useNavigate()
  const ubicacion = useLocation()
  const correo = ubicacion.state?.correo || ''

  const [digitos, setDigitos] = useState(['', '', '', '', '', ''])
  const [error, setError] = useState(null)
  const [aviso, setAviso] = useState(ubicacion.state?.aviso || null)
  const [enviando, setEnviando] = useState(false)
  const [espera, setEspera] = useState(ESPERA_REENVIO)
  const casillas = useRef([])

  // Sin correo no hay nada que verificar: se vuelve al registro.
  useEffect(() => { if (!correo) navegar('/crear-cuenta', { replace: true }) }, [correo, navegar])

  useEffect(() => {
    if (espera <= 0) return
    const t = setTimeout(() => setEspera((s) => s - 1), 1000)
    return () => clearTimeout(t)
  }, [espera])

  const codigo = digitos.join('')

  function escribir(i, valor) {
    const limpio = valor.replace(/\D/g, '')
    if (!limpio) { const d = [...digitos]; d[i] = ''; setDigitos(d); return }
    const d = [...digitos]
    // Permite pegar el código completo de una vez.
    limpio.split('').forEach((c, k) => { if (i + k < 6) d[i + k] = c })
    setDigitos(d)
    const siguiente = Math.min(i + limpio.length, 5)
    casillas.current[siguiente]?.focus()
  }

  function retroceder(i, e) {
    if (e.key === 'Backspace' && !digitos[i] && i > 0) casillas.current[i - 1]?.focus()
  }

  async function enviar(e) {
    e.preventDefault()
    if (codigo.length !== 6) { setError('Ingresa los seis dígitos del código.'); return }
    setError(null); setAviso(null); setEnviando(true)
    try {
      await verificarCorreo(correo, codigo)
      navegar('/panel', { replace: true })
    } catch (err) {
      setError(err.message)
      setDigitos(['', '', '', '', '', ''])
      casillas.current[0]?.focus()
    } finally {
      setEnviando(false)
    }
  }

  async function reenviar() {
    setError(null)
    try {
      const r = await api.reenviarCodigo(correo)
      setAviso(r.mensaje)
      setEspera(ESPERA_REENVIO)
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <div className="pantalla-auth">
      <aside className="pantalla-auth__lateral">
        <div className="marca"><span className="marca__logo">F</span> FinMind</div>
        <h1 className="pantalla-auth__lema">Un paso más.</h1>
        <p className="pantalla-auth__bajada">
          Confirmamos tu correo para que solo tú puedas entrar a tu cuenta.
        </p>
      </aside>

      <main className="pantalla-auth__panel">
        <form className="tarjeta" onSubmit={enviar} noValidate>
          <h2 className="tarjeta__titulo">Verifica tu correo</h2>
          <p className="tarjeta__bajada">
            Enviamos un código de 6 dígitos a <strong>{correo}</strong>. Vence en 15 minutos.
          </p>

          {error && <Alerta tipo="error" titulo="Código incorrecto">{error}</Alerta>}
          {aviso && <Alerta tipo="exito">{aviso}</Alerta>}

          <fieldset className="codigo">
            <legend className="codigo__leyenda">Código de verificación</legend>
            {digitos.map((d, i) => (
              <input
                key={i}
                ref={(el) => (casillas.current[i] = el)}
                className="codigo__casilla"
                type="text"
                inputMode="numeric"
                autoComplete={i === 0 ? 'one-time-code' : 'off'}
                maxLength={6}
                value={d}
                aria-label={`Dígito ${i + 1} de 6`}
                onChange={(e) => escribir(i, e.target.value)}
                onKeyDown={(e) => retroceder(i, e)}
                autoFocus={i === 0}
              />
            ))}
          </fieldset>

          <Boton type="submit" cargando={enviando} disabled={codigo.length !== 6}>
            Verificar y entrar
          </Boton>

          <p className="tarjeta__pie">
            ¿No te llegó?{' '}
            {espera > 0
              ? <span className="apagado">Puedes reenviarlo en {espera}s</span>
              : <button type="button" className="enlace" onClick={reenviar}>Reenviar código</button>}
          </p>
          <p className="tarjeta__pie">
            <Link to="/iniciar-sesion">Volver a iniciar sesión</Link>
          </p>
        </form>
      </main>
    </div>
  )
}
