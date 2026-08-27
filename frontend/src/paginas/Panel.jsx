import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, formatearDinero, MESES } from '../api/cliente'
import Layout from '../componentes/Layout'
import RegistroRapido from '../componentes/RegistroRapido'
import SelectorDeMes from '../componentes/SelectorDeMes'
import Alerta from '../componentes/Alerta'

/** UI-003 — Panel. Implementa HU-018, HU-019 / RF-021, RF-022, RF-038. */
export default function Panel() {
  const hoy = new Date()
  const [anio, setAnio] = useState(hoy.getFullYear())
  const [mes, setMes] = useState(hoy.getMonth() + 1)
  const [datos, setDatos] = useState(null)
  const [rapido, setRapido] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)

  const cargar = useCallback(async () => {
    setCargando(true); setError(null)
    try {
      // Dos llamadas: el resumen de hoy/semana y el panel del periodo elegido.
      const [p, r] = await Promise.all([api.panel(anio, mes), api.resumenRapido()])
      setDatos(p); setRapido(r)
    } catch (err) {
      setError(err.message)
    } finally {
      setCargando(false)
    }
  }, [anio, mes])

  useEffect(() => { cargar() }, [cargar])

  const cambiar = (a, m) => { setAnio(a); setMes(m) }

  if (cargando) return <Layout titulo="Inicio"><p className="estado-carga">Cargando…</p></Layout>

  if (error) {
    return (
      <Layout titulo="Inicio">
        <Alerta tipo="error" titulo="No pudimos cargar el panel">{error}</Alerta>
      </Layout>
    )
  }

  const { balance, gastoPorCategoria, patrimonio, presupuestosEnAlerta } = datos
  const sinDatos = balance.ingresos === 0 && balance.gastos === 0

  return (
    <Layout titulo="Inicio">
      {/* RF-040: lo primero es anotar, no consultar. */}
      <RegistroRapido onRegistrado={cargar} />

      {/* Una tira compacta, no tres tarjetas: el resumen del mes tiene que
          quedar a la vista sin desplazarse. */}
      {rapido && (
        <section className="tira" aria-label="Lo que llevas">
          {[['Hoy', rapido.hoy], ['Esta semana', rapido.semana], ['Este mes', rapido.mes]]
            .map(([rot, p]) => (
              <div key={rot} className="tira__dato">
                <span className="tira__rotulo">{rot}</span>
                <strong className={`tira__valor ${p.neto < 0 ? 'negativo' : p.neto > 0 ? 'positivo' : ''}`}>
                  {formatearDinero(p.neto)}
                </strong>
                <span className="tira__detalle">
                  +{formatearDinero(p.ingresos)} · −{formatearDinero(p.gastos)}
                </span>
              </div>
            ))}
        </section>
      )}

      <div className="contenido__encabezado">
        <h2 className="bloque__titulo">Cómo vas en {MESES[mes - 1]}</h2>
        <SelectorDeMes anio={anio} mes={mes} onCambiar={cambiar} />
      </div>

      {/* Balance del período (RF-021) */}
      <section className="tarjetas" aria-label="Balance del período">
        <article className="tarjeta-dato">
          <p className="tarjeta-dato__rotulo">Ingresos</p>
          <p className="tarjeta-dato__valor tarjeta-dato__valor--positivo">
            {formatearDinero(balance.ingresos)}
          </p>
        </article>
        <article className="tarjeta-dato">
          <p className="tarjeta-dato__rotulo">Gastos</p>
          <p className="tarjeta-dato__valor tarjeta-dato__valor--negativo">
            {formatearDinero(balance.gastos)}
          </p>
        </article>
        <article className="tarjeta-dato">
          <p className="tarjeta-dato__rotulo">Diferencia</p>
          <p className={`tarjeta-dato__valor ${balance.diferencia < 0 ? 'tarjeta-dato__valor--negativo' : ''}`}>
            {formatearDinero(balance.diferencia)}
          </p>
          {/* El texto viene del servidor: el signo solo es fácil de pasar por alto. */}
          <p className="tarjeta-dato__nota">{balance.lectura}</p>
        </article>
        <article className="tarjeta-dato">
          <p className="tarjeta-dato__rotulo">Patrimonio neto</p>
          <p className={`tarjeta-dato__valor ${patrimonio.patrimonioNeto < 0 ? 'tarjeta-dato__valor--negativo' : ''}`}>
            {formatearDinero(patrimonio.patrimonioNeto)}
          </p>
          <p className="tarjeta-dato__nota">
            {formatearDinero(patrimonio.activos)} en cuentas −{' '}
            {formatearDinero(patrimonio.deudaTotal ?? patrimonio.obligaciones)} en deudas
          </p>
          {/*
            Se desglosa cuando hay deuda de tarjetas. Antes esa deuda no
            aparecía en ningún lado del patrimonio, y al empezar a restarla
            el número cambia: conviene que se vea de dónde sale.
          */}
          {Number(patrimonio.deudaEnTarjetas ?? 0) > 0 && (
            <p className="tarjeta-dato__nota">
              De la deuda, {formatearDinero(patrimonio.deudaEnTarjetas)} son tarjetas
              de crédito y {formatearDinero(patrimonio.obligaciones)} son préstamos.
            </p>
          )}
        </article>
      </section>

      {/* Presupuestos que piden atención (RF-019) */}
      {presupuestosEnAlerta.length > 0 && (
        <section className="bloque" aria-label="Presupuestos que requieren atención">
          <h2 className="bloque__titulo">Presupuestos que necesitan tu atención</h2>
          {presupuestosEnAlerta.map((p) => (
            <Alerta key={p.id} tipo={p.estado === 'EXCEDIDO' ? 'error' : 'aviso'}
              titulo={`${p.categoriaNombre} · ${p.porcentajeConsumido}%`}>
              {p.aviso}
            </Alerta>
          ))}
          <Link className="enlace" to="/presupuestos">Ver todos mis presupuestos</Link>
        </section>
      )}

      {/* Composición del gasto (RF-022) */}
      <section className="bloque" aria-label="Composición del gasto">
        <h2 className="bloque__titulo">En qué se fue tu dinero</h2>

        {gastoPorCategoria.porciones.length === 0 ? (
          <div className="vacio">
            <h3 className="vacio__titulo">
              {sinDatos ? 'Todavía no hay movimientos este mes' : 'No registraste gastos este mes'}
            </h3>
            <p className="vacio__texto">
              Registra tus ingresos y gastos para ver aquí en qué se va tu dinero.
            </p>
            <Link className="boton-enlace" to="/movimientos">Registrar un movimiento</Link>
          </div>
        ) : (
          <ul className="barras">
            {gastoPorCategoria.porciones.map((p) => (
              <li key={p.categoriaId} className="barra">
                <div className="barra__fila">
                  <span className="barra__nombre">{p.nombre}</span>
                  <span className="barra__monto">
                    {formatearDinero(p.monto)} <span className="barra__pct">{p.porcentaje}%</span>
                  </span>
                </div>
                <div className="barra__pista">
                  {/* El color acompaña, pero el porcentaje siempre está escrito al lado. */}
                  <div className="barra__relleno"
                    style={{ width: `${p.porcentaje}%`, background: p.color || 'var(--color-primary-600)' }} />
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>
    </Layout>
  )
}
