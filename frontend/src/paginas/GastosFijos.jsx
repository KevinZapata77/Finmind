import { useCallback, useEffect, useMemo, useState } from 'react'
import { api, formatearDinero, PERIODICIDAD_CORTA, PERIODICIDADES } from '../api/cliente'
import Layout from '../componentes/Layout'
import Campo from '../componentes/Campo'
import Boton from '../componentes/Boton'
import Alerta from '../componentes/Alerta'

/**
 * UI-011 — Gastos fijos. Implementa HU-030 / RF-046.
 *
 * QUÉ ES ESTA PANTALLA Y QUÉ NO
 * Aquí se anota lo que hay que pagar, no lo que ya se pagó. Un gasto fijo es la
 * intención de un pago que vuelve cada período; cuando el pago ocurre de verdad
 * se registra en Movimientos como cualquier otro gasto. Conviene decirlo en la
 * pantalla, porque la confusión natural es creer que anotar el arriendo aquí ya
 * lo descuenta del saldo — y no lo hace: si lo hiciera, el mismo dinero se
 * restaría dos veces.
 *
 * PARA QUÉ SIRVE ENTONCES
 * Es lo que permite a la aplicación anticipar. Sin esta lista, FinMind sabe en
 * qué se gastó pero no qué viene, y no puede decir "no te alcanza para el
 * arriendo". Es la base de las alertas del panel.
 */
export default function GastosFijos() {
  const [lista, setLista] = useState([])
  const [categorias, setCategorias] = useState([])
  const [verInactivos, setVerInactivos] = useState(false)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)

  const [abierto, setAbierto] = useState(false)
  const [editando, setEditando] = useState(null)
  const [datos, setDatos] = useState(vacio())
  const [errores, setErrores] = useState({})
  const [errorForm, setErrorForm] = useState(null)
  const [guardando, setGuardando] = useState(false)

  const cargar = useCallback(async () => {
    setCargando(true); setError(null)
    try {
      setLista(await api.gastosFijos(verInactivos))
    } catch (err) {
      setError(err.message)
    } finally {
      setCargando(false)
    }
  }, [verInactivos])

  useEffect(() => { cargar() }, [cargar])

  useEffect(() => {
    // Solo categorías de gasto: un compromiso recurrente de tipo ingreso no
    // significa nada, y el backend lo rechaza con 400.
    api.categorias('GASTO').then(setCategorias).catch(() => setCategorias([]))
  }, [])

  /**
   * El total mensual equivalente. Se calcula sobre montoMensual y no sobre
   * monto: sumar un compromiso semanal por su valor semanal subestimaría el
   * compromiso real del mes por cuatro.
   */
  const totales = useMemo(() => {
    const activos = lista.filter((g) => g.activo)
    return {
      mensual: activos.reduce((s, g) => s + Number(g.montoMensual), 0),
      pendiente: activos
        .filter((g) => !g.cubiertoEsteMes)
        .reduce((s, g) => s + Number(g.montoMensual), 0),
      cubiertos: activos.filter((g) => g.cubiertoEsteMes).length,
      activos: activos.length,
    }
  }, [lista])

  function abrirNuevo() {
    setEditando(null); setDatos(vacio())
    setErrores({}); setErrorForm(null); setAbierto(true)
  }

  function abrirEdicion(g) {
    setEditando(g.id)
    setDatos({
      nombre: g.nombre,
      categoriaId: String(g.categoriaId),
      monto: String(g.monto),
      periodicidad: g.periodicidad,
      diaPago: String(g.diaPago),
    })
    setErrores({}); setErrorForm(null); setAbierto(true)
  }

  async function guardar(e) {
    e.preventDefault()
    setGuardando(true); setErrores({}); setErrorForm(null)
    const cuerpo = {
      nombre: datos.nombre.trim(),
      categoriaId: Number(datos.categoriaId),
      monto: Number(datos.monto),
      periodicidad: datos.periodicidad,
      diaPago: Number(datos.diaPago),
    }
    try {
      if (editando) await api.editarGastoFijo(editando, cuerpo)
      else await api.crearGastoFijo(cuerpo)
      setAbierto(false)
      await cargar()
    } catch (err) {
      // El mensaje viene del backend: repetir aquí las reglas las duplicaría
      // y tarde o temprano las dos versiones dirían cosas distintas.
      setErrores(err.erroresPorCampo ?? {})
      setErrorForm(err.message)
    } finally {
      setGuardando(false)
    }
  }

  async function alternar(g) {
    try {
      if (g.activo) await api.desactivarGastoFijo(g.id)
      else await api.activarGastoFijo(g.id)
      await cargar()
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <Layout
      titulo="Gastos fijos"
      acciones={<Boton onClick={abrirNuevo}>Anotar un compromiso</Boton>}
    >
      <p className="contenido__bajada">
        Lo que tienes que pagar cada período: arriendo, servicios, suscripciones.
        Anotarlo aquí no descuenta el dinero — eso pasa cuando registras el pago
        en Movimientos. Sirve para que FinMind pueda avisarte antes de que te
        quedes corto.
      </p>

      {error && <Alerta tipo="error" titulo="No pudimos cargar tus compromisos">{error}</Alerta>}

      {abierto && (
        <form className="tarjeta tarjeta--formulario" onSubmit={guardar}>
          <h2 className="tarjeta__titulo">
            {editando ? 'Editar compromiso' : 'Nuevo compromiso'}
          </h2>

          {errorForm && <Alerta tipo="error" titulo="Revisa los datos">{errorForm}</Alerta>}

          <Campo id="nombre" name="nombre" etiqueta="Nombre" placeholder="Arriendo"
            maxLength={80} required
            value={datos.nombre} error={errores.nombre}
            ayuda="Como lo llamas tú: Arriendo, Internet, Gimnasio."
            onChange={(e) => setDatos({ ...datos, nombre: e.target.value })} />

          <div className="fila-doble">
            <div className="campo">
              <label className="campo__etiqueta" htmlFor="categoriaId">Categoría</label>
              <select id="categoriaId" className="campo__control" value={datos.categoriaId}
                onChange={(e) => setDatos({ ...datos, categoriaId: e.target.value })} required>
                <option value="">Elige una…</option>
                {categorias.map((c) => (
                  <option key={c.id} value={c.id}>{c.nombre}</option>
                ))}
              </select>
              <p className="campo__ayuda">
                Es lo que permite saber si ya lo pagaste: FinMind compara con lo
                que gastaste en esa categoría este mes.
              </p>
            </div>

            <Campo id="monto" name="monto" type="number" inputMode="decimal"
              etiqueta="Monto" placeholder="0" min="0.01" step="0.01" required
              value={datos.monto} error={errores.monto}
              ayuda="Lo que pagas cada período, no al mes."
              onChange={(e) => setDatos({ ...datos, monto: e.target.value })} />
          </div>

          <div className="fila-doble">
            <div className="campo">
              <label className="campo__etiqueta" htmlFor="periodicidad">Cada cuánto</label>
              <select id="periodicidad" className="campo__control" value={datos.periodicidad}
                onChange={(e) => setDatos({ ...datos, periodicidad: e.target.value })}>
                {PERIODICIDADES.map((p) => (
                  <option key={p.valor} value={p.valor}>{p.etiqueta}</option>
                ))}
              </select>
              {/* La equivalencia se muestra mientras se escribe: es la cifra que
                  de verdad va a pesar en las alertas, y no es evidente. */}
              {datos.monto > 0 && datos.periodicidad !== 'MENSUAL' && (
                <p className="campo__ayuda">
                  Equivale a {formatearDinero(equivalenteMensual(datos))} al mes.
                </p>
              )}
            </div>

            <Campo id="diaPago" name="diaPago" type="number" inputMode="numeric"
              etiqueta="Día de pago" placeholder="1" min="1" max="28" step="1" required
              value={datos.diaPago} error={errores.diaPago}
              ayuda="Del 1 al 28. No se admiten 29, 30 ni 31: en febrero no existen."
              onChange={(e) => setDatos({ ...datos, diaPago: e.target.value })} />
          </div>

          <div className="acciones">
            <Boton type="submit" cargando={guardando}>
              {editando ? 'Guardar cambios' : 'Anotar compromiso'}
            </Boton>
            <button type="button" className="enlace-boton" onClick={() => setAbierto(false)}>
              Cancelar
            </button>
          </div>
        </form>
      )}

      {/* El resumen antes de la lista: la pregunta que trae al usuario aquí es
          "cuánto tengo comprometido", no "cuáles son mis compromisos". */}
      {totales.activos > 0 && (
        <section className="tira" aria-label="Resumen de compromisos">
          <div className="tira__dato">
            <span className="tira__rotulo">Comprometido al mes</span>
            <strong className="tira__valor">{formatearDinero(totales.mensual)}</strong>
            <span className="tira__detalle">
              {totales.activos} compromiso{totales.activos === 1 ? '' : 's'} activo{totales.activos === 1 ? '' : 's'}
            </span>
          </div>
          <div className="tira__dato">
            <span className="tira__rotulo">Ya cubierto este mes</span>
            <strong className="tira__valor positivo">
              {formatearDinero(totales.mensual - totales.pendiente)}
            </strong>
            <span className="tira__detalle">{totales.cubiertos} de {totales.activos}</span>
          </div>
          <div className="tira__dato">
            <span className="tira__rotulo">Falta por pagar</span>
            <strong className={`tira__valor ${totales.pendiente > 0 ? 'negativo' : 'positivo'}`}>
              {formatearDinero(totales.pendiente)}
            </strong>
            <span className="tira__detalle">Es el número que usan las alertas</span>
          </div>
        </section>
      )}

      <div className="interruptor">
        <label>
          <input type="checkbox" checked={verInactivos}
            onChange={(e) => setVerInactivos(e.target.checked)} />
          {' '}Ver también los que ya no aplican
        </label>
      </div>

      {cargando ? (
        <p className="estado-carga">Cargando…</p>
      ) : lista.length === 0 ? (
        <div className="vacio">
          <h3 className="vacio__titulo">Todavía no anotaste ningún compromiso</h3>
          <p className="vacio__texto">
            Empieza por el arriendo y los servicios. Con eso FinMind ya puede
            decirte si lo que te queda alcanza para lo que falta del mes.
          </p>
          <Boton onClick={abrirNuevo}>Anotar el primero</Boton>
        </div>
      ) : (
        <ul className="lista-fijos">
          {lista.map((g) => (
            <li key={g.id} className={`fijo${g.activo ? '' : ' fijo--inactivo'}`}>
              <div className="fijo__datos">
                <p className="fijo__nombre">
                  {g.nombre}
                  <span className="etiqueta" style={{ background: g.categoriaColor }}>
                    {g.categoriaNombre}
                  </span>
                  {/* El estado va escrito, no solo en color (RNF-008). */}
                  {g.activo && (
                    <span className={`insignia insignia--${g.cubiertoEsteMes ? 'en_curso' : 'en_alerta'}`}>
                      {g.cubiertoEsteMes ? 'Pagado este mes' : 'Pendiente'}
                    </span>
                  )}
                </p>
                <p className="fijo__meta">
                  {formatearDinero(g.monto)} {PERIODICIDAD_CORTA[g.periodicidad] ?? ''}
                  {' · próximo pago el '}{formatearFecha(g.proximoPago)}
                  {/* Solo cuando no coinciden: repetir la misma cifra dos veces
                      confunde más de lo que aclara. */}
                  {g.periodicidad !== 'MENSUAL' && (
                    <> · {formatearDinero(g.montoMensual)} al mes</>
                  )}
                </p>
              </div>

              <div className="fijo__acciones">
                {g.activo && (
                  <button type="button" className="enlace-boton" onClick={() => abrirEdicion(g)}>
                    Editar
                  </button>
                )}
                <button type="button" className="enlace-boton" onClick={() => alternar(g)}>
                  {g.activo ? 'Ya no lo pago' : 'Volver a activarlo'}
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}
    </Layout>
  )
}

const vacio = () => ({
  nombre: '', categoriaId: '', monto: '', periodicidad: 'MENSUAL', diaPago: '1',
})

/**
 * Las mismas constantes que usa el backend (GastoFijo.java). Se duplican a
 * propósito y solo para la vista previa del formulario: la cifra que cuenta es
 * la que devuelve el servidor en montoMensual.
 */
const SEMANAS_POR_MES = 4.345

function equivalenteMensual({ monto, periodicidad }) {
  const m = Number(monto) || 0
  if (periodicidad === 'SEMANAL') return m * SEMANAS_POR_MES
  if (periodicidad === 'QUINCENAL') return m * 2
  return m
}

/** El backend manda ISO; aquí se muestra como lo lee una persona. */
function formatearFecha(iso) {
  if (!iso) return '—'
  const [a, m, d] = iso.split('-')
  return new Date(Number(a), Number(m) - 1, Number(d))
    .toLocaleDateString('es-CO', { day: 'numeric', month: 'long' })
}
