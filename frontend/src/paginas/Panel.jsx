import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, enlaceCategoriaDelMes, enlaceMovimientos, formatearDinero, MESES, rangoDelMes } from '../api/cliente'
import Layout from '../componentes/Layout'
import RegistroRapido from '../componentes/RegistroRapido'
import SelectorDeMes from '../componentes/SelectorDeMes'
import Alerta from '../componentes/Alerta'
import Alertas from '../componentes/Alertas'
import Dona from '../componentes/Dona'
import CurvaDelMes from '../componentes/CurvaDelMes'
import TendenciaMeses from '../componentes/TendenciaMeses'
import ComparacionConElMesPasado from '../componentes/ComparacionConElMesPasado'

/** UI-003 — Panel. Implementa HU-018, HU-019 / RF-021, RF-022, RF-038. */
export default function Panel() {
  const hoy = new Date()
  const [anio, setAnio] = useState(hoy.getFullYear())
  const [mes, setMes] = useState(hoy.getMonth() + 1)

  // "Cómo vas" solo tiene sentido en el mes que está corriendo. Al mirar
  // febrero en agosto, el mes ya terminó y la pregunta es en pasado. Y si
  // el año no es el actual, se escribe para que no se confunda con el mes
  // de este año.
  const esMesActual = anio === hoy.getFullYear() && mes === hoy.getMonth() + 1
  const nombreDelMes = MESES[mes - 1]
  const tituloDelPeriodo = esMesActual
    ? `Cómo vas en ${nombreDelMes}`
    : `Cómo te fue en ${nombreDelMes}${anio === hoy.getFullYear() ? '' : ` de ${anio}`}`
  const [datos, setDatos] = useState(null)
  const [rapido, setRapido] = useState(null)
  const [curva, setCurva] = useState(null)
  const [historico, setHistorico] = useState(null)
  const [alertas, setAlertas] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)

  const cargar = useCallback(async () => {
    setCargando(true); setError(null)
    try {
      // El panel, el resumen de hoy/semana y la curva del mes elegido.
      const [p, r, c] = await Promise.all([
        api.panel(anio, mes), api.resumenRapido(), api.ritmo(anio, mes),
      ])
      setDatos(p); setRapido(r); setCurva(c)

      /*
        Las alertas van en una llamada aparte y con su propio catch a propósito.
        Siempre son del mes en curso —no tiene sentido avisar "no te alcanza"
        sobre un mes que ya cerró—, así que no dependen del selector. Y si
        fallaran, el panel entero se caería por un bloque secundario: el usuario
        perdería su balance por no poder calcular un aviso.
      */
      if (esMesActual) {
        try {
          setAlertas(await api.alertas())
        } catch {
          setAlertas(null)
        }
      } else {
        setAlertas(null)
      }

      /*
        El histórico va aparte y con su propio catch, por el mismo motivo que
        las alertas: es un bloque secundario y no debe poder tumbar el panel.
        Además NO depende del selector de mes — la tendencia de los últimos 6
        meses es la misma se esté mirando agosto o febrero, así que volver a
        pedirla al cambiar de mes sería una llamada de más contra una base que
        se duerme.
      */
      try {
        setHistorico(await api.historico(6))
      } catch {
        setHistorico(null)
      }
    } catch (err) {
      setError(err.message)
    } finally {
      setCargando(false)
    }
    // esMesActual se deriva de anio y mes, que ya están en las dependencias.
    // eslint-disable-next-line react-hooks/exhaustive-deps
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

      {/*
        Los avisos van arriba, antes del balance. El balance dice qué pasó; los
        avisos dicen qué va a pasar, y eso es lo que todavía se puede cambiar.
        Solo aparecen en el mes en curso: sobre un mes cerrado, "no te alcanza"
        no sirve de nada.
      */}
      {esMesActual && <Alertas resumen={alertas} />}

      {/*
        RF-050. La comparación va arriba, junto a los avisos y antes de
        cualquier gráfico. Es una sola frase y es la que más mueve: dice si la
        persona va mejor o peor que el mes pasado, que es la pregunta con la que
        llega. Solo en el mes en curso: "vas gastando" en pasado no significa
        nada.
      */}
      {esMesActual && <ComparacionConElMesPasado comparacion={historico?.comparacion} />}

      <div className="contenido__encabezado">
        <h2 className="bloque__titulo">{tituloDelPeriodo}</h2>
        <SelectorDeMes anio={anio} mes={mes} onCambiar={cambiar} />
      </div>

      {/* Balance del período (RF-021).
          RF-049: ingresos y gastos abren su propio detalle. Es la profundización
          más obvia y la que más se pide: se ve la cifra, se quiere ver de qué
          está hecha. Diferencia y patrimonio no enlazan porque son cifras
          derivadas: no existe "la lista de movimientos de tu patrimonio". */}
      <section className="tarjetas" aria-label="Balance del período">
        <Link className="tarjeta-dato tarjeta-dato--enlace"
          to={enlaceMovimientos({ tipo: 'INGRESO', ...rangoDelMes(anio, mes) })}>
          <p className="tarjeta-dato__rotulo">Ingresos</p>
          <p className="tarjeta-dato__valor tarjeta-dato__valor--positivo">
            {formatearDinero(balance.ingresos)}
          </p>
          <p className="tarjeta-dato__nota tarjeta-dato__ver">Ver los movimientos</p>
        </Link>
        <Link className="tarjeta-dato tarjeta-dato--enlace"
          to={enlaceMovimientos({ tipo: 'GASTO', ...rangoDelMes(anio, mes) })}>
          <p className="tarjeta-dato__rotulo">Gastos</p>
          <p className="tarjeta-dato__valor tarjeta-dato__valor--negativo">
            {formatearDinero(balance.gastos)}
          </p>
          <p className="tarjeta-dato__nota tarjeta-dato__ver">Ver los movimientos</p>
        </Link>
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
              {/* RF-049. Antes este aviso era un callejón sin salida: decía que
                  te pasaste en una categoría y ahí terminaba. La pregunta
                  siguiente siempre es "¿en qué?", y ahora se responde en un
                  clic en vez de seis. */}
              {' '}
              <Link className="enlace" to={enlaceCategoriaDelMes(p.categoriaId, anio, mes)}>
                Ver en qué gastaste
              </Link>
            </Alerta>
          ))}
          <Link className="enlace" to="/presupuestos">Ver todos mis presupuestos</Link>
        </section>
      )}

      {/*
        La curva del mes (RF-048). Va antes de la composición porque responde
        una pregunta más urgente: no "en qué gasté" sino "voy bien o voy mal".
      */}
      <section className="bloque" aria-label="Cómo se acumuló el mes">
        <div className="bloque__cabecera">
          <h2 className="bloque__titulo">Cómo se acumuló {esMesActual ? 'este mes' : nombreDelMes}</h2>
          {curva?.proyeccionGasto != null && (
            <span className="bloque__meta">
              Proyección al día {curva.diasTranscurridos} de {curva.diasDelMes}
            </span>
          )}
        </div>
        <CurvaDelMes ritmo={curva} nombreDelMes={nombreDelMes} />
      </section>

      {/*
        RF-050. La tendencia de varios meses va después de la curva del mes y
        antes de la composición: primero "cómo voy este mes", luego "cómo voy
        comparado con antes", y al final "en qué se fue". De lo urgente a lo
        explicativo.

        No depende del selector de mes, y el título lo dice para que no parezca
        que el gráfico ignoró el cambio de período.
      */}
      {historico?.meses?.length > 1 && (
        <section className="bloque" aria-label="Tendencia de los últimos meses">
          <div className="bloque__cabecera">
            <h2 className="bloque__titulo">Cómo vienes mes a mes</h2>
            <span className="bloque__meta">Últimos {historico.meses.length} meses</span>
          </div>
          <TendenciaMeses historico={historico} />
        </section>
      )}

      {/* Composición del gasto (RF-022) */}
      <section className="bloque" aria-label="Composición del gasto">
        <h2 className="bloque__titulo">En qué se fue tu dinero</h2>

        {gastoPorCategoria.porciones.length === 0 ? (
          <div className="vacio">
            <h3 className="vacio__titulo">
              {/* Mismo motivo que el título: al mirar un mes cerrado, "este mes" es falso. */}
              {sinDatos
                ? `Todavía no hay movimientos ${esMesActual ? 'este mes' : `en ${nombreDelMes}`}`
                : `No registraste gastos ${esMesActual ? 'este mes' : `en ${nombreDelMes}`}`}
            </h3>
            <p className="vacio__texto">
              Registra tus ingresos y gastos para ver aquí en qué se va tu dinero.
            </p>
            <Link className="boton-enlace" to="/movimientos">Registrar un movimiento</Link>
          </div>
        ) : (
          <div className="composicion">
            {/*
              La dona y las barras muestran lo mismo, y eso es intencional. La
              dona responde de un vistazo "¿hay una categoría que se está
              comiendo el mes?"; las barras dan la cifra exacta de cada una.
              Quitar las barras dejaría el dato solo en un gráfico, y un gráfico
              no se puede leer con un lector de pantalla.
            */}
            <Dona porciones={gastoPorCategoria.porciones} total={gastoPorCategoria.total}
              enlaceDe={(categoriaId) => enlaceCategoriaDelMes(categoriaId, anio, mes)} />

            <ul className="barras">
            {gastoPorCategoria.porciones.map((p) => (
              <li key={p.categoriaId} className="barra">
                {/*
                  RF-049. Cada barra abre los movimientos de esa categoría en el
                  mes que se está mirando. Es el gesto que la gente ya intenta
                  hacer por instinto en cualquier gráfico de gastos; antes no
                  pasaba nada al hacer clic.
                */}
                <Link className="barra__enlace" to={enlaceCategoriaDelMes(p.categoriaId, anio, mes)}>
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
                  {/*
                    RF-050. La comparación por categoría: es la frase que la
                    investigación señala como la de más efecto, porque apunta a
                    algo concreto que la persona puede cambiar. Se omite cuando
                    la variación es cero — repetir "igual que el mes pasado" en
                    ocho categorías es ruido — y cuando el servidor no la mandó
                    por no tener con qué comparar (RN-032).
                  */}
                  {p.variacion != null && Number(p.variacion) !== 0 && (
                    <p className={`barra__variacion ${Number(p.variacion) > 0 ? 'negativo' : 'positivo'}`}>
                      {Number(p.variacion) > 0 ? '▲' : '▼'}{' '}
                      {formatearDinero(Math.abs(Number(p.variacion)))}
                      {Number(p.variacion) > 0 ? ' más' : ' menos'} que el mes pasado
                    </p>
                  )}
                </Link>
              </li>
            ))}
            </ul>
          </div>
        )}
      </section>
    </Layout>
  )
}
