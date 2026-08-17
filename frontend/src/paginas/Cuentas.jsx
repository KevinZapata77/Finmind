import { useCallback, useEffect, useState } from 'react'
import { api, ErrorApi, TIPOS_DE_CUENTA, etiquetaDeTipo, formatearDinero, ES_PASIVO } from '../api/cliente'
import Campo from '../componentes/Campo'
import Boton from '../componentes/Boton'
import Alerta from '../componentes/Alerta'

const VACIO = { nombre: '', tipo: 'AHORROS', saldoInicial: '' }

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
    setDatos({ nombre: c.nombre, tipo: c.tipo, saldoInicial: '' })
    setErrores({}); setErrorForm(null); setFormAbierto(true)
  }

  async function guardar(e) {
    e.preventDefault()
    setErrores({}); setErrorForm(null); setGuardando(true)
    try {
      if (editando) {
        await api.editarCuenta(editando, { nombre: datos.nombre, tipo: datos.tipo })
      } else {
        await api.crearCuenta({
          nombre: datos.nombre,
          tipo: datos.tipo,
          saldoInicial: datos.saldoInicial === '' ? 0 : Number(datos.saldoInicial),
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
    .filter((c) => !ES_PASIVO(c.tipo))
    .reduce((suma, c) => suma + Number(c.saldoActual ?? 0), 0)
  const enTarjetas = activas
    .filter((c) => ES_PASIVO(c.tipo))
    .reduce((suma, c) => suma + Number(c.saldoActual ?? 0), 0)

  return (
    <section className="pagina">
      <header className="pagina__encabezado">
        <div>
          <h1 className="pagina__titulo">Mis cuentas</h1>
          <p className="pagina__bajada">
            Dinero disponible: <strong>{formatearDinero(disponible)}</strong>
          </p>
          {enTarjetas > 0 && (
            <p className="pagina__nota">
              No incluye {formatearDinero(enTarjetas)} en tarjetas de crédito:
              eso es deuda, no dinero tuyo. Lo ves en <strong>Obligaciones</strong>.
            </p>
          )}
        </div>
        <Boton onClick={abrirCreacion}>Nueva cuenta</Boton>
      </header>

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
              etiqueta="Saldo inicial" placeholder="0" min="0" step="0.01"
              ayuda="Cuánto dinero hay hoy en esa cuenta. Si lo dejas vacío, empieza en cero."
              value={datos.saldoInicial} error={errores.saldoInicial}
              onChange={(e) => setDatos({ ...datos, saldoInicial: e.target.value })} />
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
                <strong className={ES_PASIVO(c.tipo) ? 'cuenta__deuda' : undefined}>
                  {ES_PASIVO(c.tipo) && '− '}{formatearDinero(c.saldoActual, c.moneda)}
                </strong>
                <span className="cuenta__inicial">
                  Inicial: {formatearDinero(c.saldoInicial, c.moneda)}
                </span>
              </div>

              <div className="cuenta__acciones">
                <button type="button" className="enlace" onClick={() => abrirEdicion(c)}>
                  Editar
                </button>
                <button type="button" className="enlace" onClick={() => alternarEstado(c)}>
                  {c.activa ? 'Desactivar' : 'Reactivar'}
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
