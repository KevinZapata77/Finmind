import { formatearDinero } from '../api/cliente'

/**
 * "Vas gastando X más que a esta altura del mes pasado" (RF-050 / RN-032).
 *
 * POR QUÉ UNA SOLA FRASE MERECE SU PROPIO COMPONENTE
 * De la investigación de ADSO-UXUI-02: un número comparativo concreto pesa más
 * que un tablero entero, porque es específico, es personal, e implica que la
 * causa fue lo que la persona hizo. "Gastaste 1.200.000" es un dato; "vas
 * 300.000 por encima del mes pasado" es una señal a la que se puede reaccionar.
 *
 * LA COMPARACIÓN ES DE TRAMOS IGUALES
 * El servidor compara del día 1 al día de hoy contra el mismo tramo del mes
 * anterior. Es la única forma de que el número sea honesto: comparar nueve días
 * contra treinta diría siempre que se está gastando menos, y lo diría todos los
 * meses. Por eso el texto dice "a esta altura" y no "que el mes pasado" — la
 * frase tiene que describir lo que el número de verdad mide.
 */
export default function ComparacionConElMesPasado({ comparacion }) {
  if (!comparacion) return null

  const variacion = Number(comparacion.variacion)
  const gastaMas = variacion > 0
  const igual = variacion === 0

  // Sin gasto en ninguno de los dos tramos no hay nada que comparar, y
  // "gastaste lo mismo: nada" no le sirve a nadie.
  if (igual && Number(comparacion.gastoEsteMes) === 0) return null

  return (
    <section className={`comparacion comparacion--${igual ? 'igual' : gastaMas ? 'sube' : 'baja'}`}
      aria-label="Comparación con el mes pasado">
      <p className="comparacion__titular">
        {igual ? (
          <>Vas gastando <strong>lo mismo</strong> que a esta altura del mes pasado.</>
        ) : (
          <>
            Vas gastando <strong>{formatearDinero(Math.abs(variacion))}</strong>
            {gastaMas ? ' más' : ' menos'} que a esta altura del mes pasado
            {/* El porcentaje se omite cuando el mes pasado fue cero en ese
                tramo: no hay división posible y el servidor no lo manda. */}
            {comparacion.variacionPorcentaje != null && (
              <> ({gastaMas ? '+' : ''}{comparacion.variacionPorcentaje}%)</>
            )}
            .
          </>
        )}
      </p>
      <p className="comparacion__detalle">
        {formatearDinero(comparacion.gastoEsteMes)} en los primeros{' '}
        {comparacion.diaDeCorte} días de este mes, contra{' '}
        {formatearDinero(comparacion.gastoMesAnterior)} en los mismos días del anterior.
      </p>
    </section>
  )
}
