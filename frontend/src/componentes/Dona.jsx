import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { formatearDinero } from '../api/cliente'

/**
 * Composición del gasto en forma de dona (RF-022).
 *
 * POR QUÉ SVG A MANO Y NO UNA LIBRERÍA
 * El proyecto tiene tres dependencias de cliente: react, react-dom y el
 * enrutador. Traer Chart.js o Recharts por dos gráficos sumaría cientos de
 * kilobytes y un contrato de colores que no es el nuestro. Esto son cuarenta
 * líneas de SVG y usa los mismos tokens que el resto de la aplicación.
 *
 * CÓMO SE DIBUJA
 * No hay cálculo de arcos. Cada porción es un círculo completo al que se le
 * recorta el trazo con stroke-dasharray: se pinta el tramo que le toca y se
 * deja invisible el resto. Es menos código y no puede equivocarse de
 * trigonometría.
 *
 * ACCESIBILIDAD
 * El gráfico es decorativo en el sentido estricto: los mismos números están
 * escritos abajo en la lista de barras. Por eso lleva role="img" con un
 * resumen, y no intenta ser una tabla navegable que duplicaría lo que ya hay.
 */
/*
  El hueco central tiene que caber la cifra. Con GROSOR 26 el diametro interior
  quedaba en 98 y "$ 1.550.000" medía unos 100: el texto se salía sobre el
  anillo. Se adelgazó el trazo y además la cifra se escribe abreviada.
*/
const RADIO = 64
const GROSOR = 20
const CENTRO = 90
const CIRCUNFERENCIA = 2 * Math.PI * RADIO

/** Un gris que se distingue del fondo si una categoría llegara sin color. */
const COLOR_POR_DEFECTO = 'var(--color-neutral-500)'

/**
 * RF-049: `enlaceDe` es opcional y recibe el categoriaId, devolviendo la ruta
 * al detalle de esa categoría. Se pasa como función y no como ruta fija para
 * que la dona no tenga que saber nada del enrutador ni del período que se está
 * mirando: quien la usa ya lo sabe. Sin `enlaceDe`, la dona se comporta igual
 * que antes.
 */
export default function Dona({ porciones, total, enlaceDe }) {
  const [activa, setActiva] = useState(null)
  const navegar = useNavigate()

  if (!porciones?.length) return null

  // El centro muestra el total; al señalar una porción, muestra esa porción.
  const foco = activa != null ? porciones[activa] : null

  let recorrido = 0

  return (
    <div className="dona">
      <svg
        className="dona__grafico"
        viewBox="0 0 180 180"
        role="img"
        aria-label={`Composición del gasto: ${porciones.length} categorías, ${formatearDinero(total)} en total`}
      >
        {/* Pista de fondo: sin ella, un mes con una sola categoría pequeña
            dejaría un anillo casi vacío que parece un error de dibujo. */}
        <circle
          cx={CENTRO} cy={CENTRO} r={RADIO}
          fill="none" stroke="var(--color-neutral-200)" strokeWidth={GROSOR}
        />

        {porciones.map((p, i) => {
          const largo = (Number(p.porcentaje) / 100) * CIRCUNFERENCIA
          const desfase = -recorrido
          recorrido += largo
          const resaltada = activa === i

          return (
            <circle
              key={p.categoriaId}
              cx={CENTRO} cy={CENTRO} r={RADIO}
              fill="none"
              stroke={p.color || COLOR_POR_DEFECTO}
              strokeWidth={resaltada ? GROSOR + 6 : GROSOR}
              strokeDasharray={`${largo} ${CIRCUNFERENCIA - largo}`}
              strokeDashoffset={desfase}
              // Sin esto la dona empieza a las 3 en punto, no arriba.
              transform={`rotate(-90 ${CENTRO} ${CENTRO})`}
              className={`dona__porcion${enlaceDe ? ' dona__porcion--enlace' : ''}`}
              onMouseEnter={() => setActiva(i)}
              onMouseLeave={() => setActiva(null)}
              /*
                Clic en la porción: la gente lo intenta por instinto en
                cualquier gráfico de gastos. No lleva foco ni rol de enlace a
                propósito — para teclado y lector de pantalla está la leyenda
                de abajo, que sí son enlaces reales. Duplicar el foco aquí
                obligaría a tabular dos veces por cada categoría.
              */
              onClick={enlaceDe ? () => navegar(enlaceDe(p.categoriaId)) : undefined}
            />
          )
        })}

        <text x={CENTRO} y={CENTRO - 6} className="dona__cifra" textAnchor="middle">
          {compacto(foco ? foco.monto : total)}
        </text>
        <text x={CENTRO} y={CENTRO + 14} className="dona__rotulo" textAnchor="middle">
          {foco ? recortar(foco.nombre) : 'gastado'}
        </text>
      </svg>

      {/* La leyenda no es decoración: es la forma de saber qué color es qué
          sin tener que pasar el ratón, y la única que sirve en un teléfono. */}
      <ul className="dona__leyenda">
        {porciones.map((p, i) => {
          // Mismo contenido en los dos casos; cambia solo el elemento. Se
          // mantiene el <button> cuando no hay enlace para no perder el
          // resaltado por teclado que ya existía.
          const contenido = (
            <>
              <span className="dona__punto" style={{ background: p.color || COLOR_POR_DEFECTO }} />
              <span className="dona__nombre">{p.nombre}</span>
              <span className="dona__pct">{p.porcentaje}%</span>
            </>
          )
          const comunes = {
            className: `dona__item${activa === i ? ' dona__item--activo' : ''}`,
            onMouseEnter: () => setActiva(i),
            onMouseLeave: () => setActiva(null),
            onFocus: () => setActiva(i),
            onBlur: () => setActiva(null),
          }

          return (
            <li key={p.categoriaId}>
              {enlaceDe ? (
                <Link {...comunes} to={enlaceDe(p.categoriaId)}
                  aria-label={`Ver los gastos de ${p.nombre}, ${formatearDinero(p.monto)}`}>
                  {contenido}
                </Link>
              ) : (
                <button type="button" {...comunes}>{contenido}</button>
              )}
            </li>
          )
        })}
      </ul>
    </div>
  )
}

/** El centro de la dona es estrecho: un nombre largo se sale del círculo. */
function recortar(nombre) {
  return nombre.length > 12 ? `${nombre.slice(0, 11)}…` : nombre
}

/**
 * Cifra abreviada para el centro de la dona.
 *
 * "$1,55 M" es como se habla del dinero en Colombia y ocupa un tercio de lo que
 * ocupa "$ 1.550.000", que no cabe en el hueco. La cifra exacta no se pierde:
 * está en la lista de barras, al lado del gráfico.
 */
function compacto(valor) {
  const v = Number(valor ?? 0)
  if (v >= 1_000_000) return `$${(v / 1_000_000).toFixed(2).replace('.', ',')} M`
  if (v >= 10_000) return `$${Math.round(v / 1000)} mil`
  return formatearDinero(v)
}
