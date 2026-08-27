import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { enlaceMovimientos, formatearDinero, rangoDelMes } from '../api/cliente'

/**
 * Ingresos y gastos de los últimos meses (RF-050).
 *
 * POR QUÉ ESTE GRÁFICO ES EL QUE FALTABA
 * Todo lo demás en FinMind habla de un mes: el balance, la curva, la dona. Un
 * mes solo es una foto, y con una foto no se ve si el gasto viene subiendo. La
 * pregunta que este gráfico responde —"¿estoy mejor o peor que antes?"— es,
 * según la investigación recogida en ADSO-UXUI-02, la que más hace que alguien
 * vuelva a abrir una aplicación de finanzas.
 *
 * BARRAS Y NO LÍNEAS
 * Una línea sugiere que entre dos puntos hay valores intermedios, y entre
 * "julio" y "agosto" no hay nada: son categorías, no un continuo. Las barras
 * además permiten poner ingreso y gasto lado a lado, que es la comparación que
 * de verdad importa.
 *
 * CADA BARRA ES UN ENLACE
 * Un mes del gráfico lleva a los movimientos de ese mes (RF-049). Es la
 * pregunta natural al ver una barra alta: "¿qué pasó en marzo?".
 */
const ANCHO = 640
const ALTO = 200
const MARGEN = { arriba: 14, derecha: 12, abajo: 30, izquierda: 62 }

export default function TendenciaMeses({ historico }) {
  const [senalado, setSenalado] = useState(null)

  const g = useMemo(() => calcular(historico), [historico])

  if (!g) {
    return (
      <p className="grafico__vacio">
        Todavía no hay suficientes movimientos para dibujar una tendencia.
      </p>
    )
  }

  // Sin nada señalado se describe el último mes: es el que interesa por defecto.
  const foco = senalado != null
    ? g.meses.find((m) => m.clave === senalado)
    : g.meses[g.meses.length - 1]

  return (
    <div className="grafico">
      <svg
        className="grafico__lienzo"
        viewBox={`0 0 ${ANCHO} ${ALTO}`}
        role="img"
        aria-label={`Ingresos y gastos de los últimos ${g.meses.length} meses`}
        onMouseLeave={() => setSenalado(null)}
      >
        {/* La rejilla con su cifra: sin las cifras, la altura de una barra no
            significa nada. */}
        {g.marcas.map((m) => (
          <g key={m.valor}>
            <line
              x1={MARGEN.izquierda} y1={m.y} x2={ANCHO - MARGEN.derecha} y2={m.y}
              className="grafico__rejilla"
            />
            <text x={MARGEN.izquierda - 8} y={m.y + 4} className="grafico__marca-texto"
              textAnchor="end">
              {m.etiqueta}
            </text>
          </g>
        ))}

        {g.meses.map((m) => (
          <g key={m.clave}
            onMouseEnter={() => setSenalado(m.clave)}
            className={`tendencia__grupo${foco?.clave === m.clave ? ' tendencia__grupo--activo' : ''}`}>

            {/* Zona sensible de todo el ancho del mes: apuntar a una barra de
                2 píxeles de alto —un mes casi sin gasto— es imposible. */}
            <rect x={m.x} y={MARGEN.arriba} width={g.anchoDeMes}
              height={ALTO - MARGEN.arriba - MARGEN.abajo}
              className="tendencia__zona" />

            <rect x={m.xIngreso} y={m.yIngreso} width={g.anchoBarra}
              height={m.altoIngreso} className="tendencia__barra tendencia__barra--ingreso"
              rx="2" />
            <rect x={m.xGasto} y={m.yGasto} width={g.anchoBarra}
              height={m.altoGasto} className="tendencia__barra tendencia__barra--gasto"
              rx="2" />

            {/* El mes en curso se rotula distinto: está incompleto, y una barra
                más baja no significa que se gastara menos, sino que el mes va
                por la mitad. Decirlo evita leer una mejora que no existe. */}
            <text x={m.x + g.anchoDeMes / 2} y={ALTO - 10}
              className={`grafico__marca-texto${m.enCurso ? ' grafico__marca-texto--curso' : ''}`}
              textAnchor="middle">
              {m.abreviatura}{m.enCurso ? '*' : ''}
            </text>
          </g>
        ))}
      </svg>

      {/* La lectura en texto: es lo que hace que el gráfico se pueda usar con
          un lector de pantalla, y de paso lo que la mayoría lee de verdad. */}
      {foco && (
        <p className="grafico__lectura">
          <strong>{foco.nombre}</strong>: entraron {formatearDinero(foco.ingresos)} y
          {' '}salieron {formatearDinero(foco.gastos)}.
          {foco.enCurso && ' Es el mes en curso, todavía no terminó.'}
          {' '}
          <Link className="enlace" to={enlaceMovimientos(rangoDelMes(foco.anio, foco.mes))}>
            Ver sus movimientos
          </Link>
        </p>
      )}

      <ul className="grafico__leyenda">
        <li><span className="grafico__marca grafico__marca--ingreso" /> Ingresos</li>
        <li><span className="grafico__marca grafico__marca--gasto" /> Gastos</li>
        {g.hayMesEnCurso && <li>* mes todavía en curso</li>}
      </ul>
    </div>
  )
}

/**
 * Pasa la serie del servidor a coordenadas.
 *
 * La escala arranca siempre en cero. Recortarla —empezar en el valor mínimo—
 * hace que una diferencia del 3% parezca que se duplicó el gasto, y en una
 * aplicación de dinero eso no es un adorno: es una conclusión falsa.
 */
function calcular(historico) {
  const meses = historico?.meses
  if (!meses?.length) return null

  const alturaUtil = ALTO - MARGEN.arriba - MARGEN.abajo
  const anchoUtil = ANCHO - MARGEN.izquierda - MARGEN.derecha
  const anchoDeMes = anchoUtil / meses.length
  // Dos barras por mes con un respiro entre grupos.
  const anchoBarra = Math.min(22, (anchoDeMes - 10) / 2)

  const maximo = Math.max(
    ...meses.map((m) => Math.max(Number(m.ingresos), Number(m.gastos))),
  )
  // Todo en cero dividiría por cero al escalar. Se usa 1 como techo: las
  // barras quedan en cero y el gráfico se lee como un mes plano.
  const techo = maximo > 0 ? maximo : 1

  const alto = (valor) => (Number(valor) / techo) * alturaUtil
  const y = (valor) => MARGEN.arriba + alturaUtil - alto(valor)

  const hoy = new Date()
  const puntos = meses.map((m, i) => {
    const x = MARGEN.izquierda + i * anchoDeMes
    const centro = x + anchoDeMes / 2
    return {
      clave: `${m.anio}-${m.mes}`,
      anio: m.anio,
      mes: m.mes,
      nombre: `${m.nombre} de ${m.anio}`,
      abreviatura: m.nombre.slice(0, 3),
      ingresos: m.ingresos,
      gastos: m.gastos,
      enCurso: m.anio === hoy.getFullYear() && m.mes === hoy.getMonth() + 1,
      x,
      xIngreso: centro - anchoBarra - 2,
      xGasto: centro + 2,
      yIngreso: y(m.ingresos),
      yGasto: y(m.gastos),
      altoIngreso: alto(m.ingresos),
      altoGasto: alto(m.gastos),
    }
  })

  return {
    meses: puntos,
    anchoDeMes,
    anchoBarra,
    marcas: marcas(techo, alturaUtil),
    hayMesEnCurso: puntos.some((p) => p.enCurso),
  }
}

/** Tres líneas de rejilla: cero, mitad y techo. Más líneas ensucian. */
function marcas(techo, alturaUtil) {
  return [0, 0.5, 1].map((f) => ({
    valor: f,
    y: MARGEN.arriba + alturaUtil - f * alturaUtil,
    etiqueta: compacto(techo * f),
  }))
}

/** El eje es estrecho: "$ 1.550.000" no cabe, "$1,5 M" sí. */
function compacto(valor) {
  const v = Number(valor ?? 0)
  if (v >= 1_000_000) return `$${(v / 1_000_000).toFixed(1).replace('.', ',')}M`
  if (v >= 1_000) return `$${Math.round(v / 1000)}k`
  return `$${Math.round(v)}`
}
