import { useCallback, useEffect, useState } from 'react'
import { api, ErrorApi } from '../api/cliente'
import Layout from '../componentes/Layout'
import Campo from '../componentes/Campo'
import Boton from '../componentes/Boton'
import Alerta from '../componentes/Alerta'

const VACIA = { nombre: '', tipo: 'GASTO', icono: '', colorHex: '#0E8368' }

/** UI-015 — Categorías. Implementa HU-008, HU-009 / RF-009 a RF-011. */
export default function Categorias() {
  const [lista, setLista] = useState([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)

  const [abierto, setAbierto] = useState(false)
  const [editando, setEditando] = useState(null)
  const [datos, setDatos] = useState(VACIA)
  const [errores, setErrores] = useState({})
  const [errorForm, setErrorForm] = useState(null)
  const [guardando, setGuardando] = useState(false)

  const cargar = useCallback(async () => {
    setCargando(true); setError(null)
    try {
      setLista(await api.todasLasCategorias())
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

  function abrirEdicion(c) {
    // El tipo no se edita: si una categoría de GASTO pasara a INGRESO, todos los
    // movimientos ya registrados con ella quedarían contados al revés.
    setEditando(c.id)
    setDatos({ nombre: c.nombre, tipo: c.tipo, icono: c.icono ?? '', colorHex: c.colorHex ?? '#0E8368' })
    setErrores({}); setErrorForm(null); setAbierto(true)
  }

  async function guardar(e) {
    e.preventDefault()
    setErrores({}); setErrorForm(null); setGuardando(true)
    try {
      if (editando) {
        await api.editarCategoria(editando, {
          nombre: datos.nombre, icono: datos.icono || null, colorHex: datos.colorHex,
        })
      } else {
        await api.crearCategoria({
          nombre: datos.nombre, tipo: datos.tipo,
          icono: datos.icono || null, colorHex: datos.colorHex,
        })
      }
      setAbierto(false)
      await cargar()
    } catch (err) {
      if (err instanceof ErrorApi && err.erroresPorCampo) setErrores(err.erroresPorCampo)
      else setErrorForm(err.message)
    } finally {
      setGuardando(false)
    }
  }

  async function alternar(c) {
    try {
      await (c.activa ? api.desactivarCategoria(c.id) : api.activarCategoria(c.id))
      await cargar()
    } catch (err) {
      setError(err.message)
    }
  }

  const porTipo = (tipo) => lista.filter((c) => c.tipo === tipo)

  const bloque = (tipo, titulo) => (
    <section className="bloque" aria-label={titulo}>
      <h2 className="bloque__titulo">{titulo}</h2>
      <ul className="lista-categorias">
        {porTipo(tipo).map((c) => (
          <li key={c.id} className={`categoria ${c.activa ? '' : 'categoria--inactiva'}`}>
            <span className="categoria__color" aria-hidden="true"
              style={{ background: c.colorHex || 'var(--color-neutral-300)' }} />
            <span className="categoria__nombre">
              {c.nombre}
              {c.delSistema && <span className="etiqueta">Del sistema</span>}
              {!c.activa && <span className="etiqueta">Desactivada</span>}
            </span>
            <div className="categoria__acciones">
              {c.delSistema ? (
                // Las del sistema las ven todos y no las edita nadie: si un usuario
                // pudiera renombrar "Salario", se lo cambiaría a todos los demás.
                <span className="apagado">No se modifica</span>
              ) : (
                <>
                  <button type="button" className="enlace" onClick={() => abrirEdicion(c)}>Editar</button>
                  <button type="button" className="enlace" onClick={() => alternar(c)}>
                    {c.activa ? 'Desactivar' : 'Reactivar'}
                  </button>
                </>
              )}
            </div>
          </li>
        ))}
      </ul>
    </section>
  )

  return (
    <Layout titulo="Categorías" acciones={<Boton onClick={abrirNueva}>Nueva categoría</Boton>}>
      {error && <Alerta tipo="error" titulo="No pudimos cargar tus categorías">{error}</Alerta>}

      {abierto && (
        <form className="tarjeta tarjeta--formulario" onSubmit={guardar} noValidate>
          <h2 className="tarjeta__titulo">{editando ? 'Editar categoría' : 'Nueva categoría'}</h2>
          {errorForm && <Alerta tipo="error">{errorForm}</Alerta>}

          <Campo id="nombre" etiqueta="Nombre" placeholder="Mascotas" required
            value={datos.nombre} error={errores.nombre}
            onChange={(e) => setDatos({ ...datos, nombre: e.target.value })} />

          {!editando ? (
            <div className="campo">
              <label className="campo__etiqueta" htmlFor="tipo">Tipo</label>
              <select id="tipo" className="campo__control" value={datos.tipo}
                onChange={(e) => setDatos({ ...datos, tipo: e.target.value })}>
                <option value="GASTO">Gasto</option>
                <option value="INGRESO">Ingreso</option>
              </select>
              <p className="campo__ayuda">El tipo no se puede cambiar después de crearla.</p>
            </div>
          ) : (
            <p className="nota">
              El tipo no se edita. Si una categoría de gasto pasara a ingreso, todos los
              movimientos ya registrados con ella quedarían contados al revés.
            </p>
          )}

          <div className="fila-doble">
            <div className="campo">
              <label className="campo__etiqueta" htmlFor="colorHex">Color</label>
              <input id="colorHex" type="color" className="campo__color" value={datos.colorHex}
                onChange={(e) => setDatos({ ...datos, colorHex: e.target.value })} />
              {errores.colorHex && <p className="campo__error">{errores.colorHex}</p>}
            </div>
            <Campo id="icono" etiqueta="Icono (opcional)" placeholder="heart"
              value={datos.icono} error={errores.icono}
              onChange={(e) => setDatos({ ...datos, icono: e.target.value })} />
          </div>

          <div className="acciones">
            <Boton type="submit" cargando={guardando}>{editando ? 'Guardar' : 'Crear'}</Boton>
            <button type="button" className="boton boton--secundario" onClick={() => setAbierto(false)}>
              Cancelar
            </button>
          </div>
        </form>
      )}

      {cargando ? <p className="estado-carga">Cargando…</p> : (
        <>
          {bloque('GASTO', 'Gastos')}
          {bloque('INGRESO', 'Ingresos')}
          <p className="campo__ayuda">
            Desactivar una categoría no borra los movimientos que ya tenía: solo deja
            de aparecer al registrar movimientos nuevos.
          </p>
        </>
      )}
    </Layout>
  )
}
