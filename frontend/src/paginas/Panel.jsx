import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, formatearDinero, MESES } from '../api/cliente'
import Layout from '../componentes/Layout'
import SelectorDeMes from '../componentes/SelectorDeMes'
import Alerta from '../componentes/Alerta'

/** UI-003 — Panel. Implementa HU-018, HU-019 / RF-021, RF-022, RF-038. */
export default function Panel() {
  const hoy = new Date()
  const [anio, setAnio] = useState(hoy.getFullYear())
  const [mes, setMes] = useState(hoy.getMonth() + 1)
  const [datos, setDatos] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)

  const cargar = useCallback(async () => {
    setCargando(true); setError(null)
    try {
      // Una sola llamada: el backend arma balance, composición, patrimonio y alertas.
      setDatos(await api.panel(anio, mes))
    } catch (err) {
      setError(err.message)
    } finally {
      setCargando(false)
    }
  }, [anio, mes])

  useEffect(() => { cargar() }, [cargar])

  const cambiar = (a, m) => { setAnio(a); setMes(m) }

  if (cargando) return <Layout titulo="Panel"><p className="estado-carga">Cargando tu panel…</p></Layout>

  if (error) {
    return (
      <Layout titulo="Panel">
        <Alerta tipo="error" titulo="No pudimos cargar el panel">{error}</Alerta>
      </Layout>
    )
  }

  const { balance, gastoPorCategoria, patrimonio, presupuestosEnAlerta } = datos
  const sinDatos = balance.ingresos === 0 && balance.gastos === 0

  return (
    <Layout titulo="Panel" acciones={<SelectorDeMes anio={anio} mes={mes} onCambiar={cambiar} />}>
      <p className="contenido__bajada">{MESES[mes - 1]} de {anio}</p>

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
            {formatearDinero(patrimonio.obligaciones)} en deudas
          </p>
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
