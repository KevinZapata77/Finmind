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
import { useAuth } from '../auth/AuthContext'
import {
  IconoDinero, IconoAviso, IconoEntra, IconoSale, IconoPatrimonio, IconoListo,
} from '../componentes/Iconos'

/** UI-003 — Panel. Implementa HU-018, HU-019 / RF-021, RF-022, RF-038. */
export default function Panel() {
  const { usuario } = useAuth()
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

  // Días que le faltan al mes. Sale de la curva, que ya se pidió, así que no
  // cuesta una llamada más. Null si no hay curva: no se inventa el dato.
  const diasQueQuedan = curva?.diasDelMes != null && curva?.diasTranscurridos != null
    ? curva.diasDelMes - curva.diasTranscurridos
    : null

  return (
    <Layout titulo="Inicio">
      {/*
        El panel va en dos columnas: el contenido a la izquierda y los avisos
        en un riel a la derecha.

        POR QUE LOS AVISOS SE MUEVEN A UN LADO
        Antes iban intercalados arriba, empujando hacia abajo el balance y los
        gráficos. Eso obligaba a elegir entre dos cosas que compiten: los
        avisos son lo más urgente, pero el balance es lo que la persona vino a
        ver. En una columna propia dejan de competir: se leen de un vistazo sin
        empujar nada.

        El riel se desplaza con la página. Antes quedaba pegado arriba, y eso
        era un defecto: el riel mide más que la ventana, y sticky no puede
        desplazar algo más alto que la ventana — clavaba el borde de arriba y
        dejaba el último aviso fuera de alcance. Está explicado en .panel, en
        app.css. Que los avisos no queden pegados no los deja desatendidos: la
        cuarta tarjeta de métricas ya dice cuántos hay y cuántos son urgentes.

        En pantallas angostas el riel se va abajo (ver .panel en app.css): en
        móvil una columna de 300px al lado no cabe, y partir el ancho haría
        ilegibles las dos.
      */}
      <div className="panel">
        <div className="panel__principal">
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

      {esMesActual && <ComparacionConElMesPasado comparacion={historico?.comparacion} />}

      {/*
        Cabecera del panel: saludo a la izquierda, buscador a la derecha.

        El saludo no es decoración. Un panel que empieza con "Inicio" no dice
        nada; uno que empieza con el nombre y el mes sitúa a la persona en un
        segundo. Es lo primero que hace cualquier aplicación de finanzas.
      */}
      <div className="panel__cabecera">
        <div>
          <p className="panel__saludo">Hola, {usuario?.nombre ?? 'de nuevo'}</p>
          <p className="panel__periodo">
            {esMesActual ? `Así va tu ${nombreDelMes.toLowerCase()}`
              : `Así te fue en ${nombreDelMes.toLowerCase()}`}
          </p>
        </div>
        <SelectorDeMes anio={anio} mes={mes} onCambiar={cambiar} />
      </div>

      {/*
        Balance del período (RF-021), en jerarquía enfocada — opción B de
        ADSO-UXUI-02.

        POR QUÉ SE DEJARON DE USAR CUATRO TARJETAS IGUALES
        Antes esto eran cuatro cifras del mismo tamaño en cuatro cajas idénticas:
        ingresos, gastos, diferencia y patrimonio. Cuatro cosas con el mismo peso
        visual equivalen a ninguna: el ojo las recorre y no se queda con nada, y
        es exactamente lo que hacía que la pantalla se leyera como un formulario.

        Ahora hay UNA cifra grande —lo que le queda— y el resto queda como
        contexto. La diferencia es la que se destaca porque es la única que
        responde la pregunta con la que la persona abre la aplicación: "¿me
        alcanza?". Ingresos y gastos siguen ahí, más pequeños, y siguen abriendo
        su detalle (RF-049).
      */}
      {/*
        Cuatro tarjetas del mismo tamaño, con "Te queda" primera.

        NOTA SOBRE UN CAMBIO DE CRITERIO
        En ADSO-UXUI-02 se argumentó lo contrario: una cifra grande y el resto
        como contexto, porque cuatro cifras iguales equivalen a ninguna. Ese
        argumento sigue siendo válido para un panel de tema claro y sin bordes.

        Lo que lo resuelve aquí es el BORDE SEMÁNTICO. En la referencia visual
        las cuatro tarjetas no compiten porque cada una está clasificada por
        color antes de leerse: teal es lo que queda, verde lo que entró, rojo lo
        que salió, ámbar lo que pide atención. La jerarquía la da el color y la
        posición, no el tamaño. Y "Te queda" va primera, que es donde el ojo
        empieza.
      */}
      <section className="metricas" aria-label="Balance del período">
        <div className={`metrica ${balance.diferencia < 0 ? 'metrica--negativa' : 'metrica--marca'}`}>
          <span className={`chip-icono ${balance.diferencia < 0 ? 'chip-icono--negativa' : 'chip-icono--marca'}`}>
            {balance.diferencia < 0 ? <IconoAviso /> : <IconoDinero />}
          </span>
          <span className="metrica__rotulo">
            {esMesActual ? 'Te queda' : `Te quedó en ${nombreDelMes}`}
          </span>
          <span className={`metrica__valor metrica__valor--grande${balance.diferencia < 0 ? ' negativo' : ''}`}>
            {formatearDinero(balance.diferencia)}
          </span>
          {/* Los días que faltan salen de la curva, que ya se pidió. Solo tienen
              sentido en el mes en curso. El día 31 la resta da 0, y "quedan 0
              días" se lee como un error. */}
          <span className="metrica__nota">
            {esMesActual && diasQueQuedan != null
              ? (diasQueQuedan === 0 ? 'Hoy cierra el mes'
                : diasQueQuedan === 1 ? 'Queda 1 día' : `Quedan ${diasQueQuedan} días`)
              : 'Mes cerrado'}
          </span>
        </div>

        <Link className="metrica metrica--positiva"
          to={enlaceMovimientos({ tipo: 'INGRESO', ...rangoDelMes(anio, mes) })}>
          <span className="chip-icono chip-icono--positiva"><IconoEntra /></span>
          <span className="metrica__rotulo">Entró</span>
          <span className="metrica__valor positivo">{formatearDinero(balance.ingresos)}</span>
          <span className="metrica__nota">Ver los movimientos</span>
        </Link>

        <Link className="metrica metrica--negativa"
          to={enlaceMovimientos({ tipo: 'GASTO', ...rangoDelMes(anio, mes) })}>
          <span className="chip-icono chip-icono--negativa"><IconoSale /></span>
          <span className="metrica__rotulo">Salió</span>
          <span className="metrica__valor negativo">{formatearDinero(balance.gastos)}</span>
          {/* La variación viene del histórico: es la cifra que la referencia
              pone aquí, y la que más significa de un vistazo. */}
          <span className="metrica__nota">
            {historico?.comparacion?.variacionPorcentaje != null
              ? `${historico.comparacion.variacion > 0 ? '+' : ''}${historico.comparacion.variacionPorcentaje}% vs. el mes pasado`
              : 'Ver los movimientos'}
          </span>
        </Link>

        {/* La cuarta es alertas en el mes en curso, y patrimonio cuando se mira
            un mes cerrado — ahí las alertas no existen y la tarjeta quedaría
            vacía. */}
        {esMesActual ? (
          <div className={`metrica${(alertas?.alertas?.length ?? 0) > 0 ? ' metrica--aviso' : ' metrica--positiva'}`}>
            <span className={`chip-icono ${(alertas?.alertas?.length ?? 0) > 0 ? 'chip-icono--aviso' : 'chip-icono--positiva'}`}>
              {(alertas?.alertas?.length ?? 0) > 0 ? <IconoAviso /> : <IconoListo />}
            </span>
            <span className="metrica__rotulo">Alertas</span>
            <span className="metrica__valor">{alertas?.alertas?.length ?? 0}</span>
            <span className="metrica__nota">
              {(alertas?.alertas?.length ?? 0) === 0 ? 'Todo en orden'
                : `${alertas.alertas.filter((a) => a.severidad === 'ALTA').length} urgente(s)`}
            </span>
          </div>
        ) : (
          <div className="metrica">
            <span className="chip-icono chip-icono--marca"><IconoPatrimonio /></span>
            <span className="metrica__rotulo">Patrimonio</span>
            <span className={`metrica__valor${patrimonio.patrimonioNeto < 0 ? ' negativo' : ''}`}>
              {formatearDinero(patrimonio.patrimonioNeto)}
            </span>
            <span className="metrica__nota">Cuentas menos deudas</span>
          </div>
        )}
      </section>

      {/* La lectura del servidor y el patrimonio quedan como línea de apoyo:
          siguen siendo datos reales y no se pierden al compactar las tarjetas. */}
      <p className="panel__lectura">
        {balance.lectura}
        {' · '}
        <span className="panel__lectura-patrimonio">
          Patrimonio {formatearDinero(patrimonio.patrimonioNeto)}
        </span>
        {' — '}
        {formatearDinero(patrimonio.activos)} en cuentas menos{' '}
        {formatearDinero(patrimonio.deudaTotal ?? patrimonio.obligaciones)} en deudas.
      </p>

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
      {/*
        Curva y composición lado a lado, como en la referencia visual.

        Apiladas, cada gráfico ocupaba el ancho entero y había que desplazarse
        para pasar de "cómo voy" a "en qué se fue" — dos preguntas que se leen
        mejor juntas. La curva se lleva más ancho (1,4 contra 1) porque tiene
        un eje de tiempo: comprimirla aplasta la forma, que es justo el dato.
      */}
      <div className="panel__graficos">
        <section className="bloque bloque--tarjeta" aria-label="Cómo se acumuló el mes">
          <div className="bloque__cabecera">
            <h2 className="bloque__titulo">Cómo se acumuló {esMesActual ? 'este mes' : nombreDelMes}</h2>
            {curva?.proyeccionGasto != null && (
              <span className="bloque__meta">
                Día {curva.diasTranscurridos} de {curva.diasDelMes}
              </span>
            )}
          </div>
          <CurvaDelMes ritmo={curva} nombreDelMes={nombreDelMes} />
        </section>

        <section className="bloque bloque--tarjeta" aria-label="Composición del gasto">
          <h2 className="bloque__titulo">En qué se fue</h2>
          {gastoPorCategoria.porciones.length === 0 ? (
            <p className="grafico__vacio">
              {sinDatos
                ? `Sin movimientos ${esMesActual ? 'este mes' : `en ${nombreDelMes}`}.`
                : `Sin gastos ${esMesActual ? 'este mes' : `en ${nombreDelMes}`}.`}
            </p>
          ) : (
            <Dona porciones={gastoPorCategoria.porciones} total={gastoPorCategoria.total}
              enlaceDe={(categoriaId) => enlaceCategoriaDelMes(categoriaId, anio, mes)} />
          )}
        </section>
      </div>

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

      {/*
        Detalle por categoría (RF-022).

        La dona subió al bloque de arriba, junto a la curva. Aquí queda la
        lista con la cifra exacta de cada categoría y su variación contra el mes
        pasado. Las dos cosas muestran el mismo dato a propósito: la dona
        responde de un vistazo "¿hay una categoría comiéndose el mes?", y la
        lista da el número. Y un gráfico no se puede leer con un lector de
        pantalla, así que la lista no es redundancia: es la versión accesible.
      */}
      <section className="bloque" aria-label="Detalle del gasto por categoría">
        <h2 className="bloque__titulo">El detalle, categoría por categoría</h2>

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
          <div className="composicion composicion--solo-barras">
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
        </div>

        {/*
          El riel de avisos. Solo existe en el mes en curso: sobre un mes
          cerrado, "no te alcanza" no significa nada, y una columna vacía a la
          derecha desequilibraría la pantalla sin aportar.
        */}
        {esMesActual && (
          <aside className="panel__riel" aria-label="Tus alertas">
            <Alertas resumen={alertas} />
          </aside>
        )}
      </div>
    </Layout>
  )
}
