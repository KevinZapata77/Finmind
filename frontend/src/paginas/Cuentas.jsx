import { useCallback, useEffect, useState } from 'react'
import { api, ErrorApi, TIPOS_DE_CUENTA, etiquetaDeTipo, formatearDinero, ES_PASIVO } from '../api/cliente'
import Campo from '../componentes/Campo'
import Boton from '../componentes/Boton'
import Alerta from '../componentes/Alerta'
import Layout from '../componentes/Layout'

const VACIO = { nombre: '', tipo: 'AHORROS', saldoInicial: '', cupo: '' }

/** UI-008 — Cuentas. Implementa HU-006, HU-007 / RF-006 a RF-008. */
export default function Cuentas() {
  const [cuentas, setCuentas] = useState([])
  const [incluirInactivas, setIncluirInactivas] = useState(false)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)

  const [formAbierto, setFormAbierto] = useState(false)
  const [editando, setEditando] = useState(null)   // id o null si es creación
  const [datos, setDatos] = useState(VACIO)
  const [errores, setErrores] = useState({})

  // El formulario cambia según el tipo elegido: una tarjeta pide cupo y su
  // saldo inicial significa deuda, no dinero disponible.
  const esTarjeta = ES_PASIVO(datos.tipo)

  // Historial de pagos de una tarjeta, por identificador de cuenta.
  // No hay tabla de pagos: cada pago es un ingreso que el usuario ya registró
  // sobre la tarjeta, así que se piden con el filtro que la API ya tenía.
  const [pagos, setPagos] = useState({})
  const [abierta, setAbierta] = useState(null)

  async function alternarHistorial(c) {
    if (abierta === c.id) { setAbierta(null); return }
    setAbierta(c.id)
    if (pagos[c.id]) return                    // ya se cargó antes
    try {
      const r = await api.movimientos({ cuentaId: c.id, tipo: 'INGRESO', size: 50 })
      setPagos((p) => ({ ...p, [c.id]: r.contenido ?? r.movimientos ?? [] }))
    } catch (err) {
      setError(err.message)
    }
  }
  const [errorForm, setErrorForm] = useState(null)
  const [guardando, setGuardando] = useState(false)

  const cargar = useCallback(async () => {
    setCargando(true); setError(null)
    try {
      setCuentas(await api.cuentas(incluirInactivas))
    } catch (err) {
      setError(err.message)
    } finally {
      setCargando(false)
    }
  }, [incluirInactivas])

  useEffect(() => { cargar() }, [cargar])

  function abrirCreacion() {
    setEditando(null); setDatos(VACIO); setErrores({}); setErrorForm(null); setFormAbierto(true)
  }

  function abrirEdicion(c) {
    // El saldo inicial no se edita: cambiarlo alteraría hacia atrás los saldos
    // ya calculados y el historial dejaría de cuadrar.
    setEditando(c.id)
    // El cupo sí se edita: el banco lo sube o lo baja y cambiarlo no altera
    // ningún movimiento pasado, solo lo que queda por gastar.
    setDatos({ nombre: c.nombre, tipo: c.tipo, saldoInicial: '', cupo: c.cupo ?? '' })
    setErrores({}); setErrorForm(null); setFormAbierto(true)
  }

  async function guardar(e) {
    e.preventDefault()
    setErrores({}); setErrorForm(null); setGuardando(true)
    try {
      // El cupo solo viaja si la cuenta es tarjeta: el servidor rechaza un cupo
      // en cualquier otro tipo, y mandar null lo deja sin registrar.
      const cupo = esTarjeta && datos.cupo !== '' ? Number(datos.cupo) : null

      if (editando) {
        await api.editarCuenta(editando, { nombre: datos.nombre, tipo: datos.tipo, cupo })
      } else {
        await api.crearCuenta({
          nombre: datos.nombre,
          tipo: datos.tipo,
          saldoInicial: datos.saldoInicial === '' ? 0 : Number(datos.saldoInicial),
          cupo,
        })
      }
      setFormAbierto(false)
      await cargar()
    } catch (err) {
      if (err instanceof ErrorApi && err.erroresPorCampo) setErrores(err.erroresPorCampo)
      else setErrorForm(err.message)
    } finally {
      setGuardando(false)
    }
  }

  async function alternarEstado(c) {
    setError(null)
    try {
      await (c.activa ? api.desactivarCuenta(c.id) : api.activarCuenta(c.id))
      await cargar()
    } catch (err) {
      setError(err.message)
    }
  }

  // RN-020. Antes esto sumaba TODO, tarjetas incluidas, y le informaba al usuario
  // mas dinero del que tiene: el saldo de una tarjeta es deuda, no disponible.
  const activas = cuentas.filter((c) => c.activa)
  const disponible = activas
    .filter((c) => !c.esPasivo)
    .reduce((suma, c) => suma + Number(c.saldoActual ?? 0), 0)
  const enTarjetas = activas
    .filter((c) => c.esPasivo)
    .reduce((suma, c) => suma + Math.max(Number(c.saldoActual ?? 0), 0), 0)

  return (
    <Layout titulo="Mis cuentas" acciones={<Boton onClick={abrirCreacion}>Nueva cuenta</Boton>}>
      <div className="pagina__resumen">
        <div>
          <p className="pagina__bajada">
            Dinero disponible: <strong>{formatearDinero(disponible)}</strong>
          </p>
          {enTarjetas > 0 && (
            <p className="pagina__nota">
              No incluye {formatearDinero(enTarjetas)} que debes en tarjetas de crédito:
              eso es deuda, no dinero tuyo. Ya se resta de tu patrimonio en el <strong>Inicio</strong>.
            </p>
          )}
        </div>
      </div>

      {error && <Alerta tipo="error" titulo="No pudimos cargar tus cuentas">{error}</Alerta>}

      <label className="interruptor">
        <input
          type="checkbox"
          checked={incluirInactivas}
          onChange={(e) => setIncluirInactivas(e.target.checked)}
        />
        Mostrar también las cuentas desactivadas
      </label>

      {formAbierto && (
        <form className="tarjeta tarjeta--formulario" onSubmit={guardar} noValidate>
          <h2 className="tarjeta__titulo">
            {editando ? 'Editar cuenta' : 'Nueva cuenta'}
          </h2>

          {errorForm && <Alerta tipo="error">{errorForm}</Alerta>}

          <Campo id="nombre" name="nombre" etiqueta="Nombre" placeholder="Cuenta de ahorros"
            value={datos.nombre} error={errores.nombre} required
            onChange={(e) => setDatos({ ...datos, nombre: e.target.value })} />

          <div className="campo">
            <label className="campo__etiqueta" htmlFor="tipo">Tipo de cuenta</label>
            <select id="tipo" className="campo__control" value={datos.tipo}
              onChange={(e) => setDatos({ ...datos, tipo: e.target.value })}>
              {TIPOS_DE_CUENTA.map((t) => (
                <option key={t.valor} value={t.valor}>{t.etiqueta}</option>
              ))}
            </select>
            {errores.tipo && <p className="campo__error">{errores.tipo}</p>}
          </div>

          {!editando && (
            <Campo id="saldoInicial" name="saldoInicial" type="number" inputMode="decimal"
              etiqueta={esTarjeta ? 'Cuánto debes hoy' : 'Saldo inicial'}
              placeholder="0" min="0" step="0.01"
              ayuda={esTarjeta
                ? 'Lo que ya debes en la tarjeta. Cada compra que registres lo aumenta y cada pago lo baja.'
                : 'Cuánto dinero hay hoy en esa cuenta. Si lo dejas vacío, empieza en cero.'}
              value={datos.saldoInicial} error={errores.saldoInicial}
              onChange={(e) => setDatos({ ...datos, saldoInicial: e.target.value })} />
          )}

          {esTarjeta && (
            <Campo id="cupo" name="cupo" type="number" inputMode="decimal"
              etiqueta="Cupo total (opcional)" placeholder="0" min="0" step="0.01"
              ayuda="El máximo que te presta el banco. Sirve para saber cuánto te queda disponible."
              value={datos.cupo} error={errores.cupo}
              onChange={(e) => setDatos({ ...datos, cupo: e.target.value })} />
          )}

          {editando && (
            <p className="nota">
              El saldo inicial y la moneda no se editan: cambiarlos alteraría los saldos
              ya calculados y tus movimientos dejarían de cuadrar.
            </p>
          )}

          <div className="acciones">
            <Boton type="submit" cargando={guardando}>
              {editando ? 'Guardar cambios' : 'Crear cuenta'}
            </Boton>
            <button type="button" className="boton boton--secundario"
              onClick={() => setFormAbierto(false)}>
              Cancelar
            </button>
          </div>
        </form>
      )}

      {cargando ? (
        <p className="estado-carga">Cargando tus cuentas…</p>
      ) : cuentas.length === 0 ? (
        <div className="vacio">
          <h2 className="vacio__titulo">Todavía no tienes cuentas</h2>
          <p className="vacio__texto">
            Crea tu primera cuenta para empezar a registrar en qué se va tu dinero.
          </p>
          <Boton onClick={abrirCreacion}>Crear mi primera cuenta</Boton>
        </div>
      ) : (
        <ul className="lista-cuentas">
          {cuentas.map((c) => (
            <li key={c.id} className={`cuenta ${c.activa ? '' : 'cuenta--inactiva'}`}>
              <div className="cuenta__datos">
                <span className="cuenta__nombre">
                  {c.nombre}
                  {!c.activa && <span className="etiqueta">Desactivada</span>}
                </span>
                <span className="cuenta__tipo">{etiquetaDeTipo(c.tipo)}</span>
              </div>

              <div className="cuenta__saldo">
                <strong className={c.esPasivo ? 'cuenta__deuda' : undefined}>
                  {c.esPasivo && 'Debes '}{formatearDinero(c.saldoActual, c.moneda)}
                </strong>

                {c.esPasivo && c.cupo != null ? (
                  <span className={Number(c.cupoDisponible) < 0
                    ? 'cuenta__inicial cuenta__deuda' : 'cuenta__inicial'}>
                    {Number(c.cupoDisponible) < 0
                      ? `Te pasaste del cupo por ${formatearDinero(Math.abs(Number(c.cupoDisponible)), c.moneda)}`
                      : `Te quedan ${formatearDinero(c.cupoDisponible, c.moneda)} de ${formatearDinero(c.cupo, c.moneda)}`}
                  </span>
                ) : (
                  <span className="cuenta__inicial">
                    {c.esPasivo ? 'Debía' : 'Inicial'}: {formatearDinero(c.saldoInicial, c.moneda)}
                  </span>
                )}

                {c.esPasivo && Number(c.totalPagado ?? 0) > 0 && (
                  <span className="cuenta__inicial">
                    Ya le has abonado {formatearDinero(c.totalPagado, c.moneda)}
                  </span>
                )}
              </div>

              <div className="cuenta__acciones">
                <button type="button" className="enlace" onClick={() => abrirEdicion(c)}>
                  Editar
                </button>
                {c.esPasivo && (
                  <button type="button" className="enlace"
                    aria-expanded={abierta === c.id}
                    onClick={() => alternarHistorial(c)}>
                    {abierta === c.id ? 'Ocultar pagos' : 'Ver pagos'}
                  </button>
                )}
                <button type="button" className="enlace" onClick={() => alternarEstado(c)}>
                  {c.activa ? 'Desactivar' : 'Reactivar'}
                </button>
              </div>

              {abierta === c.id && (
                <div className="cuenta__historial">
                  <h3 className="cuenta__historial-titulo">Pagos hechos a esta tarjeta</h3>
                  {(pagos[c.id] ?? []).length === 0 ? (
                    <p className="campo__ayuda">
                      Todavía no has registrado pagos. Para anotar uno, registra un
                      ingreso sobre esta tarjeta desde Movimientos.
                    </p>
                  ) : (
                    <ul className="cuenta__pagos">
                      {pagos[c.id].map((p) => (
                        <li key={p.id} className="cuenta__pago">
                          <span>{p.fecha}</span>
                          <span>{p.descripcion || 'Pago'}</span>
                          <strong>{formatearDinero(p.monto, c.moneda)}</strong>
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              )}
            </li>
          ))}
        </ul>
      )}
    </Layout>
  )
}
