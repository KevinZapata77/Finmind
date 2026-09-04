import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, enlaceMovimientos, ErrorApi, formatearDinero, MESES, ritmoDelPeriodo } from '../api/cliente'
import Layout from '../componentes/Layout'
import SelectorDeMes from '../componentes/SelectorDeMes'
import Campo from '../componentes/Campo'
import Boton from '../componentes/Boton'
import Alerta from '../componentes/Alerta'

const ETIQUETA_LIMITE = {
  MENSUAL: 'Límite del mes',
  QUINCENAL: 'Límite de cada quincena',
  SEMANAL: 'Límite de cada semana',
}

const AYUDA_LIMITE = {
  MENSUAL: 'Cuánto quieres gastar como máximo en esta categoría durante el mes.',
  QUINCENAL: 'Cuánto quieres gastar como máximo en cada quincena: del 1 al 15 y del 16 en adelante.',
  SEMANAL: 'Cuánto quieres gastar como máximo cada semana, de lunes a domingo.',
}

const hoy = new Date()

/** UI-006 — Presupuestos. Implementa HU-015 a HU-017 / RF-017 a RF-020. */
export default function Presupuestos() {
  const [anio, setAnio] = useState(hoy.getFullYear())
  const [mes, setMes] = useState(hoy.getMonth() + 1)
  const [lista, setLista] = useState([])
  const [categorias, setCategorias] = useState([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)

  const [abierto, setAbierto] = useState(false)
  const [editando, setEditando] = useState(null)
  const [datos, setDatos] = useState({ categoriaId: '', montoLimite: '', periodo: 'MENSUAL' })
  const [errorForm, setErrorForm] = useState(null)
  const [errores, setErrores] = useState({})
  const [guardando, setGuardando] = useState(false)
  const [copiando, setCopiando] = useState(false)

  const cargar = useCallback(async () => {
    setCargando(true); setError(null)
    try {
      setLista(await api.presupuestos(anio, mes))
    } catch (err) {
      setError(err.message)
    } finally {
      setCargando(false)
    }
  }, [anio, mes])

  useEffect(() => { cargar() }, [cargar])

  useEffect(() => {
    // Solo GASTO: presupuestar un ingreso no significa nada, no se limita lo que entra.
    api.categorias('GASTO').then(setCategorias).catch((err) => setError(err.message))
  }, [])

  function abrirNuevo() {
    setEditando(null); setDatos({ categoriaId: '', montoLimite: '', periodo: 'MENSUAL' })
    setErrores({}); setErrorForm(null); setAbierto(true)
  }

  function abrirEdicion(p) {
    setEditando(p.id)
    setDatos({ categoriaId: String(p.categoriaId), montoLimite: String(p.montoLimite),
      periodo: p.periodo ?? 'MENSUAL' })
    setErrores({}); setErrorForm(null); setAbierto(true)
  }

  async function guardar(e) {
    e.preventDefault()
    setErrores({}); setErrorForm(null); setGuardando(true)
    try {
      if (editando) {
        await api.editarPresupuesto(editando, { montoLimite: Number(datos.montoLimite) })
      } else {
        await api.crearPresupuesto({
          categoriaId: Number(datos.categoriaId),
          montoLimite: Number(datos.montoLimite),
          periodo: datos.periodo,
          anio, mes,
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

  async function desactivar(p) {
    try { await api.desactivarPresupuesto(p.id); await cargar() }
    catch (err) { setError(err.message) }
  }

  /**
   * RF-054. Trae los presupuestos del mes anterior a este mes.
   *
   * EL PROBLEMA QUE RESUELVE
   * Un presupuesto se guarda con año y mes fijos, así que el de septiembre no
   * existe en octubre. Eso es correcto para el histórico —hay que poder mirar
   * atrás y ver qué límite regía entonces, y cuánto se consumió— pero deja al
   * usuario recreando la misma lista de ocho categorías cada mes. Nadie
   * sostiene eso: a los dos meses deja de usar los presupuestos.
   *
   * Se copia en vez de hacerlos recurrentes porque un límite que se arrastra
   * solo, sin que nadie lo mire, envejece mal: el arriendo sube, el mercado
   * cambia, y el presupuesto seguiría diciendo la cifra del año pasado.
   * Copiar deja el gesto en manos del usuario y le da la oportunidad de
   * ajustar, que es justo el momento en que conviene pensarlo.
   */
  async function copiarDelMesAnterior() {
    setCopiando(true); setError(null)
    try {
      const previo = mes === 1
        ? { anio: anio - 1, mes: 12 }
        : { anio, mes: mes - 1 }

      const anteriores = await api.presupuestos(previo.anio, previo.mes)
      const aCopiar = anteriores.filter((p) => p.activo)

      if (aCopiar.length === 0) {
        setError(`No hay presupuestos en ${MESES[previo.mes - 1]} para copiar.`)
        return
      }

      // Las categorías que ya tienen presupuesto este mes se saltan: el
      // backend responde 409 y un error a mitad de la copia dejaría el mes a
      // medio armar sin decir cuáles entraron.
      const yaEstan = new Set(lista.map((p) => p.categoriaId))
      let copiados = 0
      for (const p of aCopiar) {
        if (yaEstan.has(p.categoriaId)) continue
        await api.crearPresupuesto({
          categoriaId: p.categoriaId,
          montoLimite: Number(p.montoLimite),
          periodo: p.periodo,
          anio, mes,
        })
        copiados++
      }

      await cargar()
      if (copiados === 0) {
        setError('Todas las categorías del mes anterior ya tienen presupuesto aquí.')
      }
    } catch (err) {
      setError(err.message)
    } finally {
      setCopiando(false)
    }
  }

  const totalLimite = lista.reduce((s, p) => s + Number(p.montoLimite), 0)
  const totalConsumo = lista.reduce((s, p) => s + Number(p.consumo), 0)

  return (
    <Layout titulo="Presupuestos" acciones={
      <div className="acciones">
        <SelectorDeMes anio={anio} mes={mes} onCambiar={(a, m) => { setAnio(a); setMes(m) }} />
        <Boton onClick={abrirNuevo}>Nuevo presupuesto</Boton>
      </div>
    }>
      {/*
        La bajada explica el modelo en una línea. Sin esto, el selector de mes
        se lee como un filtro sobre una lista permanente, y entonces encontrar
        octubre vacío parece un error. Es un tope que te pones para un mes
        concreto, y decirlo evita la confusión antes de que ocurra.
      */}
      <p className="contenido__bajada">
        Tus topes de gasto para <strong>{MESES[mes - 1]} de {anio}</strong>. Cada mes
        tiene los suyos, así puedes mirar atrás y ver qué límite regía y cuánto
        gastaste de verdad.
      </p>

      {error && <Alerta tipo="error" titulo="No pudimos cargar tus presupuestos">{error}</Alerta>}

      {abierto && (
        <form className="tarjeta tarjeta--formulario" onSubmit={guardar} noValidate>
          <h2 className="tarjeta__titulo">
            {editando ? 'Cambiar el límite' : `Nuevo presupuesto para ${MESES[mes - 1]}`}
          </h2>

          {errorForm && <Alerta tipo="error">{errorForm}</Alerta>}

          {!editando && (
            <div className="campo">
              <label className="campo__etiqueta" htmlFor="categoriaId">Categoría de gasto</label>
              <select id="categoriaId" className="campo__control" required value={datos.categoriaId}
                onChange={(e) => setDatos({ ...datos, categoriaId: e.target.value })}>
                <option value="">Elige una categoría</option>
                {categorias.map((c) => <option key={c.id} value={c.id}>{c.nombre}</option>)}
              </select>
              {errores.categoriaId && <p className="campo__error">{errores.categoriaId}</p>}
            </div>
          )}

          {!editando && (
            <div className="campo">
              <label className="campo__etiqueta" htmlFor="periodo">Cada cuánto</label>
              <select id="periodo" className="campo__control" value={datos.periodo}
                onChange={(e) => setDatos({ ...datos, periodo: e.target.value })}>
                <option value="MENSUAL">Al mes</option>
                <option value="QUINCENAL">Por quincena</option>
                <option value="SEMANAL">Por semana</option>
              </select>
              <p className="campo__ayuda">
                El límite aplica a cada período, no al mes completo. Un presupuesto
                semanal de $100.000 son $100.000 cada semana.
              </p>
            </div>
          )}

          <Campo id="montoLimite" name="montoLimite" type="number" inputMode="decimal"
            min="0.01" step="0.01" required placeholder="500000"
            etiqueta={ETIQUETA_LIMITE[datos.periodo] ?? 'Límite del mes'}
            ayuda={AYUDA_LIMITE[datos.periodo] ?? AYUDA_LIMITE.MENSUAL}
            value={datos.montoLimite} error={errores.montoLimite}
            onChange={(e) => setDatos({ ...datos, montoLimite: e.target.value })} />

          {editando && (
            <p className="nota">
              La categoría y el período no se cambian: sería otro presupuesto distinto.
            </p>
          )}

          <div className="acciones">
            <Boton type="submit" cargando={guardando}>
              {editando ? 'Guardar' : 'Crear presupuesto'}
            </Boton>
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
          <h2 className="vacio__titulo">No tienes presupuestos en {MESES[mes - 1]}</h2>
          <p className="vacio__texto">
            Ponle un límite a una categoría y FinMind te avisa antes de que te pases.
          </p>
          <div className="acciones acciones--centradas">
            {/*
              RF-054. Traer los del mes anterior va PRIMERO y como acción
              principal: quien ya usó la aplicación el mes pasado casi siempre
              quiere los mismos topes, y armar ocho categorías a mano otra vez
              es lo que hace que la gente deje de usar los presupuestos.

              El texto dice "seguir con" y no "copiar": copiar suena a
              duplicado y a parche. Lo que el usuario quiere es continuar con
              sus mismos límites, y la palabra tiene que decir eso.
            */}
            <Boton onClick={copiarDelMesAnterior} cargando={copiando}>
              Seguir con mis topes de {MESES[(mes === 1 ? 12 : mes - 1) - 1]}
            </Boton>
            <Boton tipo="secundario" onClick={abrirNuevo}>Empezar de cero</Boton>
          </div>
          <p className="vacio__meta">
            Traerlos te deja ajustar las cifras antes de arrancar el mes, que es
            justo cuando conviene revisarlas.
          </p>
        </div>
      ) : (
        <>
          <section className="totales" aria-label="Totales del mes">
            <span>Presupuestado: <strong>{formatearDinero(totalLimite)}</strong></span>
            <span>Consumido: <strong>{formatearDinero(totalConsumo)}</strong></span>
            <span>Queda: <strong>{formatearDinero(totalLimite - totalConsumo)}</strong></span>
          </section>

          <ul className="lista-presupuestos">
            {lista.map((p) => (
              <li key={p.id} className={`presupuesto presupuesto--${p.estado.toLowerCase()}`}>
                <div className="presupuesto__fila">
                  <span className="presupuesto__nombre">
                    {p.categoriaNombre}
                    {!p.activo && <span className="etiqueta">Desactivado</span>}
                  </span>
                  {/* Estado en texto además del color: un semáforo solo de color
                      deja fuera a quien no distingue rojo de verde. */}
                  <span className={`insignia insignia--${p.estado.toLowerCase()}`}>
                    {p.estado === 'EXCEDIDO' ? 'Excedido'
                      : p.estado === 'EN_ALERTA' ? 'Cerca del límite' : 'Va bien'}
                  </span>
                </div>

                {/*
                  RF-052. La barra con su marca de ritmo.

                  Una barra al 60% no dice nada sola: el día 10 es alarma y el
                  día 27 es buena noticia. La marca señala dónde deberías ir
                  hoy, y el texto de abajo lo dice en palabras — porque una
                  línea sin explicación se lee como un adorno.
                */}
                <div className="barra__pista barra__pista--con-marca">
                  <div className="barra__relleno"
                    style={{
                      width: `${Math.min(Number(p.porcentajeConsumido), 100)}%`,
                      background: p.estado === 'EXCEDIDO' ? 'var(--color-error-600)'
                        : p.estado === 'EN_ALERTA' ? 'var(--color-warning-600)'
                        : 'var(--color-primary-600)',
                    }} />
                  {ritmoDe(p) != null && (
                    <span className="barra__marca" style={{ left: `${ritmoDe(p) * 100}%` }}
                      aria-hidden="true" />
                  )}
                </div>
                {lecturaDeRitmo(p) && (
                  <p className={`presupuesto__ritmo presupuesto__ritmo--${lecturaDeRitmo(p).tono}`}>
                    {lecturaDeRitmo(p).texto}
                  </p>
                )}

                <div className="presupuesto__fila">
                  <span className="presupuesto__cifras">
                    {formatearDinero(p.consumo)} de {formatearDinero(p.montoLimite)}
                    {' · '}<strong>{p.porcentajeConsumido}%</strong>
                  </span>
                  <span className={p.disponible < 0 ? 'negativo' : ''}>
                    {p.disponible < 0
                      ? `Te pasaste por ${formatearDinero(Math.abs(p.disponible))}`
                      : `Quedan ${formatearDinero(p.disponible)}`}
                  </span>
                </div>

                <div className="presupuesto__acciones">
                  {/*
                    RF-049. Se usan `desde` y `hasta` del propio presupuesto, no
                    el mes del selector: un presupuesto quincenal o semanal cubre
                    un tramo más corto que el mes, y filtrar por el mes completo
                    mostraría movimientos que no cuentan para este consumo. La
                    cifra del enlace y la del filtro tienen que ser la misma.
                  */}
                  <Link className="enlace"
                    to={enlaceMovimientos({ categoriaId: p.categoriaId, desde: p.desde, hasta: p.hasta })}>
                    Ver los {formatearDinero(p.consumo)} gastados
                  </Link>
                  <button type="button" className="enlace" onClick={() => abrirEdicion(p)}>
                    Cambiar límite
                  </button>
                  {p.activo && (
                    <button type="button" className="enlace" onClick={() => desactivar(p)}>
                      Desactivar
                    </button>
                  )}
                </div>
              </li>
            ))}
          </ul>
        </>
      )}
    </Layout>
  )
}

/** RF-052. Fracción del período ya transcurrida, o null si no está en curso. */
const ritmoDe = (p) => ritmoDelPeriodo(p.desde, p.hasta)

/**
 * RF-052. Traduce la distancia entre lo gastado y lo que tocaría a esta altura.
 *
 * POR QUÉ HAY UN MARGEN DE 5 PUNTOS
 * Sin él, casi ningún presupuesto estaría "en ritmo": el consumo real nunca
 * cae exactamente sobre la línea, así que la pantalla estaría siempre
 * regañando o siempre felicitando por diferencias de un punto. Cinco puntos es
 * lo que separa "vas bien" de una desviación que vale la pena mirar.
 *
 * Devuelve null cuando no hay nada útil que decir: período cerrado, o ya
 * excedido — ahí el problema no es el ritmo, es que se pasó, y la insignia de
 * arriba ya lo grita.
 */
function lecturaDeRitmo(p) {
  const ritmo = ritmoDe(p)
  if (ritmo == null) return null
  if (p.estado === 'EXCEDIDO') return null

  const consumido = Number(p.porcentajeConsumido)
  const esperado = ritmo * 100
  const diferencia = Math.round(consumido - esperado)
  const MARGEN = 5

  if (Math.abs(diferencia) <= MARGEN) {
    return { tono: 'bien', texto: 'Vas justo en tu ritmo para lo que va del período.' }
  }
  if (diferencia > 0) {
    return {
      tono: 'alerta',
      texto: `Vas ${diferencia} puntos por delante de tu ritmo: a esta altura tocaría `
             + `${Math.round(esperado)}% y llevas ${Math.round(consumido)}%.`,
    }
  }
  return {
    tono: 'bien',
    texto: `Vas ${Math.abs(diferencia)} puntos por debajo de tu ritmo. Buen margen.`,
  }
}
