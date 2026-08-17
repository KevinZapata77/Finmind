import { useEffect, useMemo, useState } from 'react'
import { api, ErrorApi, formatearDinero, hoyISO } from '../api/cliente'
import Alerta from './Alerta'

const ATAJOS = [5000, 10000, 20000, 50000, 100000]
const CLAVE_ULTIMA = 'finmind.ultimaCategoria'

/**
 * RF-040 — Registro rápido.
 *
 * Cabe en una sola fila a propósito. La versión anterior ocupaba media pantalla
 * y empujaba el resumen del mes hacia abajo: la pantalla de inicio tiene que
 * dejar anotar Y dejar ver, sin obligar a desplazarse.
 *
 * Tres decisiones para bajar la fricción, que es la causa número uno de que la
 * gente abandone una aplicación de finanzas:
 *
 *  1. La cuenta no se pregunta: se usa la principal, que existe desde el registro.
 *  2. La fecha no se pregunta: es hoy. Para otra fecha está Movimientos.
 *  3. La categoría viene preseleccionada con la última que usaste de ese tipo.
 *     Quien anota ventas todos los días no debería elegirla todos los días.
 */
export default function RegistroRapido({ onRegistrado }) {
  const [tipo, setTipo] = useState('INGRESO')
  const [monto, setMonto] = useState('')
  const [categorias, setCategorias] = useState([])
  const [categoriaId, setCategoriaId] = useState('')
  const [cuentaId, setCuentaId] = useState(null)
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState(null)
  const [exito, setExito] = useState(null)

  const [creando, setCreando] = useState(false)
  const [nuevaCategoria, setNuevaCategoria] = useState('')

  const cargarCategorias = () => api.categorias().then(setCategorias)

  useEffect(() => {
    Promise.all([cargarCategorias(), api.cuentas()])
      .then(([, ctas]) => setCuentaId(ctas[0]?.id ?? null))
      .catch((err) => setError(err.message))
  }, [])

  const delTipo = useMemo(() => categorias.filter((c) => c.tipo === tipo), [categorias, tipo])

  // Al cambiar de tipo, se recupera la última categoría usada para ese tipo.
  useEffect(() => {
    if (delTipo.length === 0) return
    const recordadas = JSON.parse(localStorage.getItem(CLAVE_ULTIMA) ?? '{}')
    const guardada = recordadas[tipo]
    const sigueExistiendo = delTipo.some((c) => String(c.id) === String(guardada))
    setCategoriaId(sigueExistiendo ? String(guardada) : String(delTipo[0].id))
  }, [delTipo, tipo])

  function recordar(id) {
    const recordadas = JSON.parse(localStorage.getItem(CLAVE_ULTIMA) ?? '{}')
    localStorage.setItem(CLAVE_ULTIMA, JSON.stringify({ ...recordadas, [tipo]: id }))
  }

  async function registrar(e) {
    e.preventDefault()
    setError(null); setExito(null); setGuardando(true)
    try {
      const m = await api.crearMovimiento({
        cuentaId, categoriaId: Number(categoriaId),
        monto: Number(monto), fecha: hoyISO(), descripcion: null,
      })
      recordar(categoriaId)
      setExito(`${m.tipo === 'INGRESO' ? 'Entró' : 'Salió'} ${formatearDinero(m.monto)} · ${m.categoriaNombre}`)
      setMonto('')
      onRegistrado?.()
      // El aviso desaparece solo: no hay que cerrarlo para seguir anotando.
      setTimeout(() => setExito(null), 4000)
    } catch (err) {
      setError(err instanceof ErrorApi ? err.message : 'No pudimos registrarlo.')
    } finally {
      setGuardando(false)
    }
  }

  /** Crear una categoría sin salir de aquí: salir del flujo es perder el momento. */
  async function crearCategoria(e) {
    e.preventDefault()
    setError(null)
    try {
      const c = await api.crearCategoria({
        nombre: nuevaCategoria, tipo, icono: null, colorHex: '#0E8368',
      })
      await cargarCategorias()
      setCategoriaId(String(c.id))
      recordar(String(c.id))
      setNuevaCategoria(''); setCreando(false)
    } catch (err) {
      setError(err.message)
    }
  }

  if (!cuentaId && !error) return null

  return (
    <section className="rapido" aria-label="Registro rápido">
      {error && <Alerta tipo="error">{error}</Alerta>}
      {exito && <p className="rapido__exito" role="status">{exito}</p>}

      <form className="rapido__barra" onSubmit={registrar}>
        <div className="rapido__tipos" role="group" aria-label="Tipo">
          <button type="button" aria-pressed={tipo === 'INGRESO'}
            className={`rapido__tipo ${tipo === 'INGRESO' ? 'rapido__tipo--ingreso' : ''}`}
            onClick={() => setTipo('INGRESO')}>
            Entró
          </button>
          <button type="button" aria-pressed={tipo === 'GASTO'}
            className={`rapido__tipo ${tipo === 'GASTO' ? 'rapido__tipo--gasto' : ''}`}
            onClick={() => setTipo('GASTO')}>
            Salió
          </button>
        </div>

        <input className="rapido__monto" type="number" inputMode="decimal"
          min="0.01" step="0.01" placeholder="¿Cuánto?" required
          aria-label="Monto" value={monto} onChange={(e) => setMonto(e.target.value)} />

        <select className="rapido__categoria" required aria-label="Categoría"
          value={categoriaId} onChange={(e) => setCategoriaId(e.target.value)}>
          {delTipo.map((c) => <option key={c.id} value={c.id}>{c.nombre}</option>)}
        </select>

        <button type="submit" className="boton rapido__anotar"
          disabled={guardando || !monto || !categoriaId}>
          {guardando ? 'Anotando…' : 'Anotar'}
        </button>
      </form>

      <div className="rapido__pie">
        {/* Los atajos solo aparecen cuando hace falta: si ya escribió, estorban. */}
        {monto === '' && ATAJOS.map((v) => (
          <button key={v} type="button" className="atajo" onClick={() => setMonto(String(v))}>
            {formatearDinero(v)}
          </button>
        ))}

        {creando ? (
          <form className="rapido__nueva" onSubmit={crearCategoria}>
            <input className="campo__control" required maxLength={60} autoFocus
              placeholder={`Nueva categoría de ${tipo === 'INGRESO' ? 'ingreso' : 'gasto'}`}
              value={nuevaCategoria} onChange={(e) => setNuevaCategoria(e.target.value)} />
            <button type="submit" className="enlace">Crear</button>
            <button type="button" className="enlace" onClick={() => setCreando(false)}>Cancelar</button>
          </form>
        ) : (
          <button type="button" className="enlace rapido__crear" onClick={() => setCreando(true)}>
            + Nueva categoría
          </button>
        )}
      </div>
    </section>
  )
}
