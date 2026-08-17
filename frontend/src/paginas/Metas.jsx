import { useCallback, useEffect, useState } from 'react'
import { api, ErrorApi, formatearDinero } from '../api/cliente'
import Layout from '../componentes/Layout'
import Campo from '../componentes/Campo'
import Boton from '../componentes/Boton'
import Alerta from '../componentes/Alerta'

const VACIA = { nombre: '', montoObjetivo: '', fechaLimite: '' }

/** UI-007 — Metas de ahorro. Implementa HU-028 a HU-030 / RF-032 a RF-034. */
export default function Metas() {
  const [lista, setLista] = useState([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)
  const [aviso, setAviso] = useState(null)

  const [abierto, setAbierto] = useState(false)
  const [editando, setEditando] = useState(null)
  const [datos, setDatos] = useState(VACIA)
  const [errores, setErrores] = useState({})
  const [errorForm, setErrorForm] = useState(null)
  const [guardando, setGuardando] = useState(false)

  const [abonando, setAbonando] = useState(null)
  const [abono, setAbono] = useState('')
  const [errorAbono, setErrorAbono] = useState(null)

  const cargar = useCallback(async () => {
    setCargando(true); setError(null)
    try {
      setLista(await api.metas())
    } catch (err) {
      setError(err.message)
    } finally {
      setCargando(false)
    }
  }, [])

  useEffect(() => { cargar() }, [cargar])

  function abrirNueva() {
    setEditando(null); setDatos(VACIA); setErrores({}); setErrorForm(null); setAbierto(true)
  }

  function abrirEdicion(m) {
    setEditando(m.id)
    setDatos({
      nombre: m.nombre, montoObjetivo: String(m.montoObjetivo),
      fechaLimite: m.fechaLimite ?? '',
    })
    setErrores({}); setErrorForm(null); setAbierto(true)
  }

  async function guardar(e) {
    e.preventDefault()
    setErrores({}); setErrorForm(null); setGuardando(true)
    const cuerpo = {
      nombre: datos.nombre,
      montoObjetivo: Number(datos.montoObjetivo),
      fechaLimite: datos.fechaLimite || null,
    }
    try {
      if (editando) await api.editarMeta(editando, cuerpo)
      else await api.crearMeta(cuerpo)
      setAbierto(false)
      await cargar()
    } catch (err) {
      if (err instanceof ErrorApi && err.erroresPorCampo) setErrores(err.erroresPorCampo)
      else setErrorForm(err.message)
    } finally {
      setGuardando(false)
    }
  }

  async function registrarAbono(e, meta) {
    e.preventDefault()
    setErrorAbono(null)
    try {
      const r = await api.abonarMeta(meta.id, { monto: Number(abono) })
      setAviso(r.estado === 'COMPLETADA'
        ? `¡Lograste "${r.nombre}"! Ahorraste ${formatearDinero(r.montoActual)}.`
        : `Abono aplicado. ${r.lectura}`)
      setAbonando(null); setAbono('')
      await cargar()
    } catch (err) {
      // Un abono mayor que lo que falta se rechaza: el servidor explica por qué.
      setErrorAbono(err.message)
    }
  }

  async function cancelar(m) {
    if (!window.confirm(`¿Cancelar "${m.nombre}"? Lo ahorrado sigue registrado.`)) return
    try { await api.cancelarMeta(m.id); await cargar() }
    catch (err) { setError(err.message) }
  }

  const enCurso = lista.filter((m) => m.estado === 'EN_CURSO')
  const totalAhorrado = lista.reduce((s, m) => s + Number(m.montoActual), 0)
  const totalObjetivo = enCurso.reduce((s, m) => s + Number(m.montoObjetivo), 0)

  return (
    <Layout titulo="Metas de ahorro" acciones={<Boton onClick={abrirNueva}>Nueva meta</Boton>}>
      {error && <Alerta tipo="error" titulo="No pudimos cargar tus metas">{error}</Alerta>}
      {aviso && <Alerta tipo="exito">{aviso}</Alerta>}

      <section className="tarjetas" aria-label="Resumen de ahorro">
        <article className="tarjeta-dato">
          <p className="tarjeta-dato__rotulo">Llevas ahorrado</p>
          <p className="tarjeta-dato__valor tarjeta-dato__valor--positivo">
            {formatearDinero(totalAhorrado)}
          </p>
        </article>
        <article className="tarjeta-dato">
          <p className="tarjeta-dato__rotulo">Metas en curso</p>
          <p className="tarjeta-dato__valor">{enCurso.length}</p>
          <p className="tarjeta-dato__nota">
            Objetivo total: {formatearDinero(totalObjetivo)}
          </p>
        </article>
      </section>

      {abierto && (
        <form className="tarjeta tarjeta--formulario" onSubmit={guardar} noValidate>
          <h2 className="tarjeta__titulo">{editando ? 'Editar meta' : 'Nueva meta'}</h2>
          {errorForm && <Alerta tipo="error">{errorForm}</Alerta>}

          <Campo id="nombre" etiqueta="¿Para qué estás ahorrando?"
            placeholder="Viaje a Cartagena" required
            value={datos.nombre} error={errores.nombre}
            onChange={(e) => setDatos({ ...datos, nombre: e.target.value })} />

          <div className="fila-doble">
            <Campo id="montoObjetivo" type="number" min="0.01" step="0.01" inputMode="decimal"
              etiqueta="¿Cuánto necesitas?" placeholder="3000000" required
              value={datos.montoObjetivo} error={errores.montoObjetivo}
              onChange={(e) => setDatos({ ...datos, montoObjetivo: e.target.value })} />
            <Campo id="fechaLimite" type="date" etiqueta="¿Para cuándo? (opcional)"
              value={datos.fechaLimite} error={errores.fechaLimite}
              onChange={(e) => setDatos({ ...datos, fechaLimite: e.target.value })} />
          </div>

          {editando && (
            <p className="nota">
              El objetivo no puede quedar por debajo de lo que ya ahorraste.
            </p>
          )}

          <div className="acciones">
            <Boton type="submit" cargando={guardando}>{editando ? 'Guardar' : 'Crear meta'}</Boton>
            <button type="button" className="boton boton--secundario" onClick={() => setAbierto(false)}>
              Cancelar
            </button>
          </div>
        </form>
      )}

      {cargando ? (
        <p className="estado-carga">Cargando…</p>
      ) : lista.length === 0 ? (
        <div className="vacio">
          <h2 className="vacio__titulo">Todavía no tienes metas</h2>
          <p className="vacio__texto">
            Ponle nombre a lo que quieres lograr y ve cuánto te falta cada vez que abonas.
          </p>
          <Boton onClick={abrirNueva}>Crear mi primera meta</Boton>
        </div>
      ) : (
        <ul className="lista-presupuestos">
          {lista.map((m) => (
            <li key={m.id} className={`presupuesto ${m.estado !== 'EN_CURSO' ? 'obligacion--cerrada' : ''}`}>
              <div className="presupuesto__fila">
                <span className="presupuesto__nombre">
                  {m.nombre}
                  {m.estado === 'COMPLETADA' && <span className="insignia insignia--en_curso">Lograda</span>}
                  {m.estado === 'CANCELADA' && <span className="etiqueta">Cancelada</span>}
                </span>
                <span className="presupuesto__cifras">
                  <strong>{m.porcentajeAvance}%</strong>
                </span>
              </div>

              <div className="barra__pista">
                <div className="barra__relleno"
                  style={{
                    width: `${m.porcentajeAvance}%`,
                    background: m.estado === 'COMPLETADA'
                      ? 'var(--color-success-600)' : 'var(--color-primary-600)',
                  }} />
              </div>

              <div className="presupuesto__fila">
                <span className="presupuesto__cifras">
                  {formatearDinero(m.montoActual)} de {formatearDinero(m.montoObjetivo)}
                </span>
                {/* El texto lo arma el servidor: no depende solo de la barra. */}
                <span>{m.lectura}</span>
              </div>

              {m.fechaLimite && (
                <p className="movimiento__meta">Fecha límite: {m.fechaLimite}</p>
              )}

              {m.estado === 'EN_CURSO' && (
                <div className="presupuesto__acciones">
                  <button type="button" className="enlace" onClick={() => setAbonando(m.id)}>
                    Abonar
                  </button>
                  <button type="button" className="enlace" onClick={() => abrirEdicion(m)}>Editar</button>
                  <button type="button" className="enlace" onClick={() => cancelar(m)}>Cancelar</button>
                </div>
              )}

              {abonando === m.id && (
                <form className="pago" onSubmit={(e) => registrarAbono(e, m)}>
                  {errorAbono && <Alerta tipo="error">{errorAbono}</Alerta>}
                  <Campo id={`abono-${m.id}`} type="number" min="0.01" step="0.01" inputMode="decimal"
                    etiqueta="¿Cuánto vas a abonar?" required
                    ayuda={`Te faltan ${formatearDinero(m.loQueFalta)}. No puedes abonar más que eso.`}
                    value={abono} onChange={(e) => setAbono(e.target.value)} />
                  <div className="acciones">
                    <Boton type="submit">Abonar</Boton>
                    <button type="button" className="boton boton--secundario"
                      onClick={() => { setAbonando(null); setErrorAbono(null) }}>
                      Cancelar
                    </button>
                  </div>
                </form>
              )}
            </li>
          ))}
        </ul>
      )}
    </Layout>
  )
}
