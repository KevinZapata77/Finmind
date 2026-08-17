import { useCallback, useEffect, useState } from 'react'
import { api, ErrorApi, formatearDinero, hoyISO, TIPOS_DE_OBLIGACION } from '../api/cliente'
import Layout from '../componentes/Layout'
import Campo from '../componentes/Campo'
import Boton from '../componentes/Boton'
import Alerta from '../componentes/Alerta'

const NUEVA = {
  nombre: '', acreedor: '', tipo: 'TARJETA_CREDITO', montoOriginal: '',
  tasaAnual: '', cuotaMensual: '', diaPago: '5', fechaInicio: hoyISO(),
}

const etiquetaTipo = (v) => TIPOS_DE_OBLIGACION.find((t) => t.valor === v)?.etiqueta ?? v

/** UI-014 — Obligaciones. Implementa HU-031 a HU-035 / RF-035 a RF-039. */
export default function Obligaciones() {
  const [lista, setLista] = useState([])
  const [patrimonio, setPatrimonio] = useState(null)
  const [soloActivas, setSoloActivas] = useState(true)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)

  const [abierto, setAbierto] = useState(false)
  const [datos, setDatos] = useState(NUEVA)
  const [errores, setErrores] = useState({})
  const [errorForm, setErrorForm] = useState(null)
  const [guardando, setGuardando] = useState(false)

  const [pagando, setPagando] = useState(null)
  const [pago, setPago] = useState({ monto: '', fecha: hoyISO(), descripcion: '' })
  const [avisoPago, setAvisoPago] = useState(null)

  const cargar = useCallback(async () => {
    setCargando(true); setError(null)
    try {
      const [obl, pat] = await Promise.all([api.obligaciones(soloActivas), api.patrimonio()])
      setLista(obl); setPatrimonio(pat)
    } catch (err) {
      setError(err.message)
    } finally {
      setCargando(false)
    }
  }, [soloActivas])

  useEffect(() => { cargar() }, [cargar])

  async function crear(e) {
    e.preventDefault()
    setErrores({}); setErrorForm(null); setGuardando(true)
    try {
      await api.crearObligacion({
        nombre: datos.nombre, acreedor: datos.acreedor, tipo: datos.tipo,
        montoOriginal: Number(datos.montoOriginal),
        tasaAnual: datos.tasaAnual === '' ? 0 : Number(datos.tasaAnual),
        cuotaMensual: Number(datos.cuotaMensual),
        diaPago: Number(datos.diaPago), fechaInicio: datos.fechaInicio,
      })
      setAbierto(false); setDatos(NUEVA)
      await cargar()
    } catch (err) {
      if (err instanceof ErrorApi && err.erroresPorCampo) setErrores(err.erroresPorCampo)
      else setErrorForm(err.message)
    } finally {
      setGuardando(false)
    }
  }

  async function registrarPago(e, obligacion) {
    e.preventDefault()
    setAvisoPago(null)
    try {
      const r = await api.pagarObligacion(obligacion.id, {
        monto: Number(pago.monto), fecha: pago.fecha, descripcion: pago.descripcion || null,
      })
      // RN-019: si el pago no cubrió el interés, el servidor lo advierte y se muestra.
      setAvisoPago(r.advertencia ??
        `Pago aplicado: ${formatearDinero(r.interes)} de interés y ${formatearDinero(r.abonoCapital)} a capital.`)
      setPagando(null); setPago({ monto: '', fecha: hoyISO(), descripcion: '' })
      await cargar()
    } catch (err) {
      setAvisoPago(err.message)
    }
  }

  async function cancelar(o) {
    if (!window.confirm(`¿Cancelar "${o.nombre}"? Conserva su historial de pagos.`)) return
    try { await api.cancelarObligacion(o.id); await cargar() }
    catch (err) { setError(err.message) }
  }

  const totalDeuda = lista.filter((o) => o.estado === 'ACTIVA')
    .reduce((s, o) => s + Number(o.saldoPendiente), 0)
  const cuotaDelMes = lista.filter((o) => o.estado === 'ACTIVA')
    .reduce((s, o) => s + Number(o.cuotaMensual), 0)
  const proximas = lista.filter((o) => o.venceEnSieteDias)

  return (
    <Layout titulo="Obligaciones" acciones={<Boton onClick={() => setAbierto(true)}>Nueva obligación</Boton>}>
      {error && <Alerta tipo="error" titulo="No pudimos cargar tus obligaciones">{error}</Alerta>}
      {avisoPago && <Alerta tipo="aviso" titulo="Pago registrado">{avisoPago}</Alerta>}

      <section className="tarjetas" aria-label="Resumen de deuda">
        <article className="tarjeta-dato">
          <p className="tarjeta-dato__rotulo">Debes en total</p>
          <p className="tarjeta-dato__valor tarjeta-dato__valor--negativo">{formatearDinero(totalDeuda)}</p>
        </article>
        <article className="tarjeta-dato">
          <p className="tarjeta-dato__rotulo">Cuotas de este mes</p>
          <p className="tarjeta-dato__valor">{formatearDinero(cuotaDelMes)}</p>
        </article>
        {patrimonio && (
          <article className="tarjeta-dato">
            <p className="tarjeta-dato__rotulo">Patrimonio neto</p>
            <p className={`tarjeta-dato__valor ${patrimonio.patrimonioNeto < 0 ? 'tarjeta-dato__valor--negativo' : ''}`}>
              {formatearDinero(patrimonio.patrimonioNeto)}
            </p>
            <p className="tarjeta-dato__nota">{patrimonio.lectura}</p>
          </article>
        )}
      </section>

      {proximas.length > 0 && (
        <Alerta tipo="aviso" titulo="Cuotas por vencer">
          {proximas.map((o) => `${o.nombre} el día ${o.diaPago}`).join(' · ')}
        </Alerta>
      )}

      {abierto && (
        <form className="tarjeta tarjeta--formulario" onSubmit={crear} noValidate>
          <h2 className="tarjeta__titulo">Nueva obligación</h2>
          {errorForm && <Alerta tipo="error">{errorForm}</Alerta>}

          <div className="fila-doble">
            <Campo id="nombre" etiqueta="Nombre" placeholder="Tarjeta Visa" required
              value={datos.nombre} error={errores.nombre}
              onChange={(e) => setDatos({ ...datos, nombre: e.target.value })} />
            <Campo id="acreedor" etiqueta="A quién le debes" placeholder="Bancolombia" required
              value={datos.acreedor} error={errores.acreedor}
              onChange={(e) => setDatos({ ...datos, acreedor: e.target.value })} />
          </div>

          <div className="campo">
            <label className="campo__etiqueta" htmlFor="tipo">Tipo</label>
            <select id="tipo" className="campo__control" value={datos.tipo}
              onChange={(e) => setDatos({ ...datos, tipo: e.target.value })}>
              {TIPOS_DE_OBLIGACION.map((t) => <option key={t.valor} value={t.valor}>{t.etiqueta}</option>)}
            </select>
          </div>

          <div className="fila-doble">
            <Campo id="montoOriginal" type="number" min="0.01" step="0.01" inputMode="decimal"
              etiqueta="Monto total de la deuda" placeholder="2400000" required
              value={datos.montoOriginal} error={errores.montoOriginal}
              onChange={(e) => setDatos({ ...datos, montoOriginal: e.target.value })} />
            <Campo id="tasaAnual" type="number" min="0" max="200" step="0.0001" inputMode="decimal"
              etiqueta="Tasa anual (%)" placeholder="24.5"
              ayuda="Déjala en blanco si no cobra intereses."
              value={datos.tasaAnual} error={errores.tasaAnual}
              onChange={(e) => setDatos({ ...datos, tasaAnual: e.target.value })} />
          </div>

          <div className="fila-doble">
            <Campo id="cuotaMensual" type="number" min="0.01" step="0.01" inputMode="decimal"
              etiqueta="Cuota mensual" placeholder="300000" required
              value={datos.cuotaMensual} error={errores.cuotaMensual}
              onChange={(e) => setDatos({ ...datos, cuotaMensual: e.target.value })} />
            <Campo id="diaPago" type="number" min="1" max="28" etiqueta="Día de pago" required
              ayuda="Del 1 al 28, para que exista en todos los meses."
              value={datos.diaPago} error={errores.diaPago}
              onChange={(e) => setDatos({ ...datos, diaPago: e.target.value })} />
          </div>

          <Campo id="fechaInicio" type="date" etiqueta="Fecha de inicio" required
            value={datos.fechaInicio} error={errores.fechaInicio}
            onChange={(e) => setDatos({ ...datos, fechaInicio: e.target.value })} />

          <div className="acciones">
            <Boton type="submit" cargando={guardando}>Registrar obligación</Boton>
            <button type="button" className="boton boton--secundario" onClick={() => setAbierto(false)}>
              Cancelar
            </button>
          </div>
        </form>
      )}

      <label className="interruptor">
        <input type="checkbox" checked={!soloActivas}
          onChange={(e) => setSoloActivas(!e.target.checked)} />
        Mostrar también las pagadas y canceladas
      </label>

      {cargando ? (
        <p className="estado-carga">Cargando…</p>
      ) : lista.length === 0 ? (
        <div className="vacio">
          <h2 className="vacio__titulo">No tienes obligaciones registradas</h2>
          <p className="vacio__texto">
            Registra tus deudas para saber cuánto debes en total y cuánto de cada cuota
            se va en intereses.
          </p>
          <Boton onClick={() => setAbierto(true)}>Registrar una obligación</Boton>
        </div>
      ) : (
        <ul className="lista-obligaciones">
          {lista.map((o) => (
            <li key={o.id} className={`obligacion ${o.estado !== 'ACTIVA' ? 'obligacion--cerrada' : ''}`}>
              <div className="presupuesto__fila">
                <span className="presupuesto__nombre">
                  {o.nombre}
                  {o.estado !== 'ACTIVA' && (
                    <span className="etiqueta">{o.estado === 'PAGADA' ? 'Pagada' : 'Cancelada'}</span>
                  )}
                </span>
                <span className="obligacion__saldo">{formatearDinero(o.saldoPendiente)}</span>
              </div>

              <p className="movimiento__meta">
                {etiquetaTipo(o.tipo)} · {o.acreedor} · cuota {formatearDinero(o.cuotaMensual)} el día {o.diaPago}
              </p>

              <div className="barra__pista">
                <div className="barra__relleno"
                  style={{ width: `${o.porcentajePagado}%`, background: 'var(--color-primary-600)' }} />
              </div>

              <div className="presupuesto__fila">
                <span className="presupuesto__cifras">
                  Pagado {o.porcentajePagado}% de {formatearDinero(o.montoOriginal)}
                </span>
                {o.estado === 'ACTIVA' && Number(o.interesDelPeriodo) > 0 && (
                  <span className="obligacion__interes">
                    Interés de este mes: {formatearDinero(o.interesDelPeriodo)}
                  </span>
                )}
              </div>

              {o.venceEnSieteDias && (
                <p className="obligacion__aviso">Tu cuota vence dentro de los próximos 7 días.</p>
              )}

              {o.estado === 'ACTIVA' && (
                <div className="presupuesto__acciones">
                  <button type="button" className="enlace" onClick={() => setPagando(o.id)}>
                    Registrar pago
                  </button>
                  <button type="button" className="enlace" onClick={() => cancelar(o)}>Cancelar</button>
                </div>
              )}

              {pagando === o.id && (
                <form className="pago" onSubmit={(e) => registrarPago(e, o)}>
                  <Campo id={`pago-${o.id}`} type="number" min="0.01" step="0.01" inputMode="decimal"
                    etiqueta="Monto del pago" required
                    ayuda={`El interés de este mes es ${formatearDinero(o.interesDelPeriodo)}. Un pago menor no baja la deuda.`}
                    value={pago.monto} onChange={(e) => setPago({ ...pago, monto: e.target.value })} />
                  <Campo id={`fecha-${o.id}`} type="date" etiqueta="Fecha" required max={hoyISO()}
                    value={pago.fecha} onChange={(e) => setPago({ ...pago, fecha: e.target.value })} />
                  <div className="acciones">
                    <Boton type="submit">Aplicar pago</Boton>
                    <button type="button" className="boton boton--secundario" onClick={() => setPagando(null)}>
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
