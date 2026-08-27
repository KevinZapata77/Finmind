import { useMemo, useState } from 'react'
import { formatearDinero } from '../api/cliente'

/**
 * Cómo se acumuló el dinero día por día (RF-048).
 *
 * QUÉ MUESTRA
 * Dos líneas: lo que entró y lo que salió, acumulado. Cuando la línea de gasto
 * cruza la de ingreso, el usuario se quedó sin margen ese día — y eso se ve en
 * un segundo, mientras que en una tabla de movimientos hay que sumar a mano.
 *
 * Y una tercera línea, punteada, hacia el fin de mes: adónde llegaría el gasto
 * si el ritmo se mantuviera. Va punteada a propósito. Es la única parte del
 * gráfico que no ocurrió; dibujarla igual que las otras dos daría a entender
 * que ya pasó.
 *
 * DE DÓNDE SALEN LOS PUNTOS
 * Del servidor, uno por día. No se interpola nada en el cliente: repartir el
 * total del mes entre los días afirmaría que el usuario gastó lo mismo todos
 * los días, que es justo lo que este gráfico sirve para desmentir.
 */
const ANCHO = 640
const ALTO = 220
const MARGEN = { arriba: 12, derecha: 12, abajo: 26, izquierda: 58 }

export default function CurvaDelMes({ ritmo, nombreDelMes }) {
  const [diaSenalado, setDiaSenalado] = useState(null)

  const g = useMemo(() => calcular(ritmo), [ritmo])

  if (!g) {
    return (
      <p className="grafico__vacio">
        Todavía no hay movimientos en {nombreDelMes} para dibujar la curva.
      </p>
    )
  }

  const punto = diaSenalado != null
    ? g.dias.find((d) => d.dia === diaSenalado)
    : g.dias[g.dias.length - 1]

  return (
    <div className="grafico">
      <svg
        className="grafico__lienzo"
        viewBox={`0 0 ${ANCHO} ${ALTO}`}
        role="img"
        aria-label={
          `Acumulado de ${nombreDelMes}: ${formatearDinero(g.totalGastos)} gastados y `
          + `${formatearDinero(g.totalIngresos)} ingresados en ${g.diasTranscurridos} días`
        }
        onMouseLeave={() => setDiaSenalado(null)}
      >
        {/* Rejilla horizontal con su cifra. Sin las cifras, la altura de la
            línea no significa nada. */}
        {g.marcas.map((m) => (
          <g key={m.valor}>
            <line
              x1={MARGEN.izquierda} y1={m.y} x2={ANCHO - MARGEN.derecha} y2={m.y}
              stroke="var(--color-neutral-200)" strokeWidth="1"
            />
            <text x={MARGEN.izquierda - 8} y={m.y + 4} className="grafico__eje" textAnchor="end">
              {m.etiqueta}
            </text>
          </g>
        ))}

        {/* Área bajo el gasto: da peso visual a la línea que importa. */}
        <path d={g.areaGasto} fill="var(--color-error-100)" opacity="0.7" />

        {/* Proyección primero, para que las líneas reales queden encima. */}
        {g.proyeccion && (
          <line
            x1={g.proyeccion.x1} y1={g.proyeccion.y1}
            x2={g.proyeccion.x2} y2={g.proyeccion.y2}
            stroke="var(--color-warning-600)" strokeWidth="2"
            strokeDasharray="6 5" strokeLinecap="round"
          />
        )}

        <path d={g.lineaIngreso} fill="none" stroke="var(--color-success-600)"
          strokeWidth="2.5" strokeLinejoin="round" strokeLinecap="round" />
        <path d={g.lineaGasto} fill="none" stroke="var(--color-error-600)"
          strokeWidth="2.5" strokeLinejoin="round" strokeLinecap="round" />

        {/* Guía vertical del día señalado. */}
        {punto && (
          <g>
            <line
              x1={punto.x} y1={MARGEN.arriba} x2={punto.x} y2={ALTO - MARGEN.abajo}
              stroke="var(--color-neutral-500)" strokeWidth="1" strokeDasharray="3 3"
            />
            <circle cx={punto.x} cy={punto.yGasto} r="4.5"
              fill="var(--color-error-600)" stroke="var(--color-surface)" strokeWidth="2" />
            <circle cx={punto.x} cy={punto.yIngreso} r="4.5"
              fill="var(--color-success-600)" stroke="var(--color-surface)" strokeWidth="2" />
          </g>
        )}

        {/* Eje de días: solo tres marcas. Escribir los treinta y un números
            los amontonaría hasta volverlos ilegibles. */}
        {g.marcasDeDia.map((m) => (
          <text key={m.dia} x={m.x} y={ALTO - 8} className="grafico__eje" textAnchor="middle">
            {m.dia}
          </text>
        ))}

        {/*
          Zonas invisibles, una por día, para saber cuál está señalando el
          ratón. Es más fiable que calcular la posición dentro del SVG, que se
          desajusta en cuanto el gráfico se escala en una pantalla pequeña.
        */}
        {g.dias.map((d) => (
          <rect
            key={d.dia}
            x={d.x - g.anchoDeDia / 2} y={MARGEN.arriba}
            width={g.anchoDeDia} height={ALTO - MARGEN.arriba - MARGEN.abajo}
            fill="transparent"
            onMouseEnter={() => setDiaSenalado(d.dia)}
          />
        ))}
      </svg>

      {/*
        La lectura en texto no es un extra: es lo que hace utilizable el
        gráfico sin ratón y para quien no distingue el verde del rojo (RNF-008).
      */}
      {punto && (
        <p className="grafico__lectura">
          <strong>Día {punto.dia}</strong> · llevabas{' '}
          <span className="negativo">{formatearDinero(punto.gastoAcumulado)}</span> gastados
          {' '}y <span className="positivo">{formatearDinero(punto.ingresoAcumulado)}</span> ingresados.
        </p>
      )}

      <ul className="grafico__leyenda">
        <li><span className="grafico__marca grafico__marca--ingreso" /> Ingreso acumulado</li>
        <li><span className="grafico__marca grafico__marca--gasto" /> Gasto acumulado</li>
        {g.proyeccion && (
          <li>
            <span className="grafico__marca grafico__marca--proyeccion" />
            Si sigues así, cierras el mes en {formatearDinero(ritmo.proyeccionGasto)}
          </li>
        )}
      </ul>
    </div>
  )
}

/**
 * Pasa las cifras a coordenadas. Vive fuera del componente porque es aritmética
 * pura: así se puede leer y comprobar sin pensar en React.
 */
function calcular(ritmo) {
  if (!ritmo?.dias?.length) return null

  const { dias, diasDelMes, diasTranscurridos, proyeccionGasto } = ritmo
  const totalIngresos = Number(ritmo.totalIngresos ?? 0)
  const totalGastos = Number(ritmo.totalGastos ?? 0)

  // Un mes sin un solo peso registrado no se dibuja: dos líneas pegadas al
  // suelo no dicen nada y el techo quedaría en cero, sin escala posible.
  if (totalIngresos === 0 && totalGastos === 0) return null

  // El techo tiene en cuenta la proyección: si no, la línea punteada se saldría
  // por arriba del recuadro.
  const techo = Math.max(totalIngresos, totalGastos, Number(proyeccionGasto ?? 0)) || 1

  const anchoUtil = ANCHO - MARGEN.izquierda - MARGEN.derecha
  const altoUtil = ALTO - MARGEN.arriba - MARGEN.abajo

  // El eje siempre abarca el mes completo, aunque solo haya datos hasta hoy:
  // así se ve cuánto mes queda por delante, que es media lectura del gráfico.
  const x = (dia) => MARGEN.izquierda + ((dia - 1) / Math.max(diasDelMes - 1, 1)) * anchoUtil
  const y = (valor) => MARGEN.arriba + altoUtil - (Number(valor) / techo) * altoUtil

  const puntos = dias.map((d) => ({
    ...d,
    x: x(d.dia),
    yGasto: y(d.gastoAcumulado),
    yIngreso: y(d.ingresoAcumulado),
  }))

  const trazo = (clave) =>
    puntos.map((p, i) => `${i === 0 ? 'M' : 'L'} ${p.x.toFixed(1)} ${p[clave].toFixed(1)}`).join(' ')

  const primero = puntos[0]
  const ultimo = puntos[puntos.length - 1]
  const suelo = ALTO - MARGEN.abajo

  return {
    dias: puntos,
    diasTranscurridos,
    totalIngresos,
    totalGastos,
    anchoDeDia: anchoUtil / Math.max(diasDelMes - 1, 1),
    lineaGasto: trazo('yGasto'),
    lineaIngreso: trazo('yIngreso'),
    areaGasto: `${trazo('yGasto')} L ${ultimo.x.toFixed(1)} ${suelo} L ${primero.x.toFixed(1)} ${suelo} Z`,
    // Del último dato hasta el fin de mes. Nula si el servidor no proyectó.
    proyeccion: proyeccionGasto == null ? null : {
      x1: ultimo.x, y1: ultimo.yGasto,
      x2: x(diasDelMes), y2: y(proyeccionGasto),
    },
    marcas: escala(techo).map((valor) => ({
      valor, y: y(valor), etiqueta: abreviar(valor),
    })),
    marcasDeDia: [1, Math.ceil(diasDelMes / 2), diasDelMes]
      .map((dia) => ({ dia, x: x(dia) })),
  }
}

/** Cuatro líneas de rejilla: suficientes para ubicarse, pocas para no estorbar. */
function escala(techo) {
  return [0, 0.25, 0.5, 0.75, 1].map((f) => Math.round(techo * f))
}

/**
 * En el eje no cabe "$1.850.000". Se abrevia a millones o miles, que es como
 * se habla del dinero en Colombia.
 */
function abreviar(valor) {
  if (valor === 0) return '0'
  if (valor >= 1_000_000) return `${(valor / 1_000_000).toFixed(1).replace('.0', '')}M`
  if (valor >= 1000) return `${Math.round(valor / 1000)}k`
  return String(valor)
}
