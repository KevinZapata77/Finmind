import { useCallback, useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { api, ErrorApi, formatearDinero, hoyISO, describirFiltro } from '../api/cliente'
import Layout from '../componentes/Layout'
import Campo from '../componentes/Campo'
import Boton from '../componentes/Boton'
import Alerta from '../componentes/Alerta'

const VACIO = { cuentaId: '', categoriaId: '', monto: '', fecha: hoyISO(), descripcion: '' }

/** Los filtros que viven en la URL (RF-049). */
const CAMPOS_FILTRO = ['desde', 'hasta', 'cuentaId', 'categoriaId', 'tipo']

/**
 * UI-004 y UI-005 — Movimientos. Implementa HU-010 a HU-014 / RF-012 a RF-016.
 *
 * RF-049: LOS FILTROS VIVEN EN LA URL, NO EN useState
 * Antes el filtro era estado interno, así que era imposible enlazar aquí desde
 * otra pantalla: el panel podía decir "te pasaste en Alimentación" pero no
 * llevarte a los movimientos de Alimentación. El usuario tenía que venir por el
 * menú y volver a armar el filtro a mano, seis acciones para responder una
 * pregunta que la aplicación acababa de plantearle.
 *
 * Con el filtro en la URL, cualquier pantalla puede enlazar a una vista concreta
 * (`/movimientos?categoriaId=3&desde=2026-08-01&hasta=2026-08-31`), y de paso el
 * usuario puede guardar o compartir ese enlace, y el botón "atrás" del navegador
 * hace lo que se espera.
 */
export default function Movimientos() {
  const [params, setParams] = useSearchParams()
  const [pagina, setPagina] = useState(null)
  const [cuentas, setCuentas] = useState([])
  const [categorias, setCategorias] = useState([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)

  const filtros = useMemo(
    () => Object.fromEntries(CAMPOS_FILTRO.map((c) => [c, params.get(c) ?? ''])),
    [params],
  )
  const page = Number(params.get('page') ?? 0)
  const hayFiltro = CAMPOS_FILTRO.some((c) => filtros[c])

  const [abierto, setAbierto] = useState(false)
  const [editando, setEditando] = useState(null)
  const [datos, setDatos] = useState(VACIO)
  const [errores, setErrores] = useState({})
  const [errorForm, setErrorForm] = useState(null)
  const [guardando, setGuardando] = useState(false)

  const cargar = useCallback(async () => {
    setCargando(true); setError(null)
    try {
      setPagina(await api.movimientos({ ...filtros, page, size: 20 }))
    } catch (err) {
      setError(err.message)
    } finally {
      setCargando(false)
    }
  }, [filtros, page])

  useEffect(() => { cargar() }, [cargar])

  useEffect(() => {
    Promise.all([api.cuentas(), api.categorias()])
      .then(([c, k]) => { setCuentas(c); setCategorias(k) })
      .catch((err) => setError(err.message))
  }, [])

  function abrirNuevo() {
    setEditando(null); setDatos(VACIO); setErrores({}); setErrorForm(null); setAbierto(true)
  }

  function abrirEdicion(m) {
    setEditando(m.id)
    setDatos({
      cuentaId: String(m.cuentaId), categoriaId: String(m.categoriaId),
      monto: String(m.monto), fecha: m.fecha, descripcion: m.descripcion ?? '',
    })
    setErrores({}); setErrorForm(null); setAbierto(true)
  }

  async function guardar(e) {
    e.preventDefault()
    setErrores({}); setErrorForm(null); setGuardando(true)
    const cuerpo = {
      cuentaId: Number(datos.cuentaId),
      categoriaId: Number(datos.categoriaId),
      monto: Number(datos.monto),
      fecha: datos.fecha,
      descripcion: datos.descripcion || null,
    }
    try {
      if (editando) await api.editarMovimiento(editando, cuerpo)
      else await api.crearMovimiento(cuerpo)
      setAbierto(false)
      await cargar()
    } catch (err) {
      if (err instanceof ErrorApi && err.erroresPorCampo) setErrores(err.erroresPorCampo)
      else setErrorForm(err.message)
    } finally {
      setGuardando(false)
    }
  }

  async function borrar(m) {
    // Los movimientos sí se borran de verdad: uno equivocado no es historia,
    // es un dato falso que descuadraría el saldo.
    if (!window.confirm(`¿Borrar el movimiento de ${formatearDinero(m.monto)}?`)) return
    try {
      await api.borrarMovimiento(m.id)
      await cargar()
    } catch (err) {
      setError(err.message)
    }
  }

  /**
   * Escribe el filtro en la URL. Un filtro vacío se borra del enlace en vez de
   * quedar como `categoriaId=`: así la barra de direcciones dice exactamente lo
   * que el usuario está viendo, sin ruido.
   */
  function cambiarFiltro(campo, valor) {
    const siguiente = new URLSearchParams(params)
    if (valor) siguiente.set(campo, valor)
    else siguiente.delete(campo)
    // Cambiar un filtro invalida la página: la 3 de un filtro puede no existir
    // en el siguiente.
    siguiente.delete('page')
    setParams(siguiente, { replace: true })
  }

  function irAPagina(n) {
    const siguiente = new URLSearchParams(params)
    if (n > 0) siguiente.set('page', String(n))
    else siguiente.delete('page')
    setParams(siguiente, { replace: true })
  }

  const limpiarFiltros = () => setParams(new URLSearchParams(), { replace: true })

  return (
    <Layout titulo="Movimientos" acciones={<Boton onClick={abrirNuevo}>Nuevo movimiento</Boton>}>
      {error && <Alerta tipo="error" titulo="No pudimos cargar tus movimientos">{error}</Alerta>}

      {abierto && (
        <form className="tarjeta tarjeta--formulario" onSubmit={guardar} noValidate>
          <h2 className="tarjeta__titulo">{editando ? 'Editar movimiento' : 'Nuevo movimiento'}</h2>

          {errorForm && <Alerta tipo="error">{errorForm}</Alerta>}

          <div className="fila-doble">
            <div className="campo">
              <label className="campo__etiqueta" htmlFor="cuentaId">Cuenta</label>
              <select id="cuentaId" className="campo__control" required value={datos.cuentaId}
                onChange={(e) => setDatos({ ...datos, cuentaId: e.target.value })}>
                <option value="">Elige una cuenta</option>
                {cuentas.map((c) => <option key={c.id} value={c.id}>{c.nombre}</option>)}
              </select>
              {errores.cuentaId && <p className="campo__error">{errores.cuentaId}</p>}
            </div>

            <div className="campo">
              <label className="campo__etiqueta" htmlFor="categoriaId">Categoría</label>
              <select id="categoriaId" className="campo__control" required value={datos.categoriaId}
                onChange={(e) => setDatos({ ...datos, categoriaId: e.target.value })}>
                <option value="">Elige una categoría</option>
                <optgroup label="Ingresos">
                  {categorias.filter((c) => c.tipo === 'INGRESO')
                    .map((c) => <option key={c.id} value={c.id}>{c.nombre}</option>)}
                </optgroup>
                <optgroup label="Gastos">
                  {categorias.filter((c) => c.tipo === 'GASTO')
                    .map((c) => <option key={c.id} value={c.id}>{c.nombre}</option>)}
                </optgroup>
              </select>
              {/* RN-002: la categoría define si es ingreso o gasto. No hay selector de tipo. */}
              <p className="campo__ayuda">La categoría define si es un ingreso o un gasto.</p>
            </div>
          </div>

          <div className="fila-doble">
            <Campo id="monto" name="monto" type="number" inputMode="decimal" min="0.01" step="0.01"
              etiqueta="Monto" placeholder="50000" required
              value={datos.monto} error={errores.monto}
              onChange={(e) => setDatos({ ...datos, monto: e.target.value })} />
            <Campo id="fecha" name="fecha" type="date" etiqueta="Fecha" required max={hoyISO()}
              value={datos.fecha} error={errores.fecha}
              onChange={(e) => setDatos({ ...datos, fecha: e.target.value })} />
          </div>

          <Campo id="descripcion" name="descripcion" etiqueta="Descripción (opcional)"
            placeholder="Mercado de la semana" value={datos.descripcion} error={errores.descripcion}
            onChange={(e) => setDatos({ ...datos, descripcion: e.target.value })} />

          <div className="acciones">
            <Boton type="submit" cargando={guardando}>
              {editando ? 'Guardar cambios' : 'Registrar'}
            </Boton>
            <button type="button" className="boton boton--secundario" onClick={() => setAbierto(false)}>
              Cancelar
            </button>
          </div>
        </form>
      )}

      {/* RF-049. Cuando se llega aquí desde otra pantalla, el filtro ya viene
          puesto. Sin este aviso el usuario ve una lista corta y no entiende por
          qué le faltan movimientos: cree que se perdieron. Decir en palabras qué
          está viendo, y dar un botón para salir del filtro, es lo que convierte
          la profundización en algo reversible. */}
      {hayFiltro && (
        <section className="filtro-activo" aria-live="polite">
          <p className="filtro-activo__texto">
            Estás viendo <strong>{describirFiltro(filtros, cuentas, categorias)}</strong>
          </p>
          <button type="button" className="enlace-boton" onClick={limpiarFiltros}>
            Ver todos mis movimientos
          </button>
        </section>
      )}

      {/* Filtros (RF-014) */}
      <section className="filtros" aria-label="Filtros">
        <label className="filtros__campo">
          <span className="campo__etiqueta">Desde</span>
          <input type="date" className="campo__control" value={filtros.desde}
            onChange={(e) => cambiarFiltro('desde', e.target.value)} />
        </label>
        <label className="filtros__campo">
          <span className="campo__etiqueta">Hasta</span>
          <input type="date" className="campo__control" value={filtros.hasta}
            onChange={(e) => cambiarFiltro('hasta', e.target.value)} />
        </label>
        <label className="filtros__campo">
          <span className="campo__etiqueta">Tipo</span>
          <select className="campo__control" value={filtros.tipo}
            onChange={(e) => cambiarFiltro('tipo', e.target.value)}>
            <option value="">Todos</option>
            <option value="INGRESO">Ingresos</option>
            <option value="GASTO">Gastos</option>
          </select>
        </label>
        <label className="filtros__campo">
          <span className="campo__etiqueta">Cuenta</span>
          <select className="campo__control" value={filtros.cuentaId}
            onChange={(e) => cambiarFiltro('cuentaId', e.target.value)}>
            <option value="">Todas</option>
            {cuentas.map((c) => <option key={c.id} value={c.id}>{c.nombre}</option>)}
          </select>
        </label>
        <label className="filtros__campo">
          <span className="campo__etiqueta">Categoría</span>
          <select className="campo__control" value={filtros.categoriaId}
            onChange={(e) => cambiarFiltro('categoriaId', e.target.value)}>
            <option value="">Todas</option>
            {categorias.map((c) => <option key={c.id} value={c.id}>{c.nombre}</option>)}
          </select>
        </label>
      </section>

      {/* Los totales corresponden al filtro completo, no a la página visible. */}
      {pagina && (
        <section className="totales" aria-label="Totales del filtro">
          <span>Ingresos: <strong className="positivo">{formatearDinero(pagina.totalIngresos)}</strong></span>
          <span>Gastos: <strong className="negativo">{formatearDinero(pagina.totalGastos)}</strong></span>
          <span>Diferencia: <strong>{formatearDinero(pagina.diferencia)}</strong></span>
          <span className="totales__nota">{pagina.totalElementos} movimientos en el filtro</span>
        </section>
      )}

      {cargando ? (
        <p className="estado-carga">Cargando…</p>
      ) : !pagina || pagina.contenido.length === 0 ? (
        <div className="vacio">
          <h2 className="vacio__titulo">No hay movimientos con estos filtros</h2>
          <p className="vacio__texto">Cambia los filtros o registra tu primer movimiento.</p>
          <Boton onClick={abrirNuevo}>Registrar un movimiento</Boton>
        </div>
      ) : (
        <>
          <ul className="lista-movimientos">
            {pagina.contenido.map((m) => (
              <li key={m.id} className="movimiento">
                <span className="movimiento__punto" aria-hidden="true"
                  style={{ background: m.categoriaColor || 'var(--color-neutral-300)' }} />
                <div className="movimiento__datos">
                  <span className="movimiento__descripcion">
                    {m.descripcion || m.categoriaNombre}
                  </span>
                  <span className="movimiento__meta">
                    {m.fecha} · {m.categoriaNombre} · {m.cuentaNombre}
                  </span>
                </div>
                {/* El signo va escrito, no solo el color. */}
                <span className={`movimiento__monto ${m.tipo === 'INGRESO' ? 'positivo' : 'negativo'}`}>
                  {m.tipo === 'INGRESO' ? '+' : '−'} {formatearDinero(m.monto)}
                </span>
                <div className="movimiento__acciones">
                  <button type="button" className="enlace" onClick={() => abrirEdicion(m)}>Editar</button>
                  <button type="button" className="enlace" onClick={() => borrar(m)}>Borrar</button>
                </div>
              </li>
            ))}
          </ul>

          {pagina.totalPaginas > 1 && (
            <nav className="paginacion" aria-label="Paginación">
              <button type="button" className="boton boton--secundario"
                disabled={page === 0} onClick={() => irAPagina(page - 1)}>Anterior</button>
              <span>Página {pagina.pagina + 1} de {pagina.totalPaginas}</span>
              <button type="button" className="boton boton--secundario"
                disabled={page + 1 >= pagina.totalPaginas} onClick={() => irAPagina(page + 1)}>Siguiente</button>
            </nav>
          )}
        </>
      )}
    </Layout>
  )
}
