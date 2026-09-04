import { Link } from 'react-router-dom'
import { formatearDinero, PESO_SEVERIDAD, SEVERIDADES } from '../api/cliente'
import { IconoAviso, IconoSube, IconoPendiente } from './Iconos'

/**
 * Avisos del mes y las cifras que los sustentan (RF-047).
 *
 * POR QUÉ SE MUESTRAN LOS NÚMEROS Y NO SOLO EL AVISO
 * "No te alcanza para el arriendo" es una afirmación fuerte. Si el usuario no
 * puede ver de dónde sale, tiene dos opciones: creerla a ciegas o desconfiar de
 * la aplicación. Por eso debajo de los avisos va la resta completa —lo que
 * entró, lo que salió, lo que falta por pagar— y cada aviso enlaza a la
 * pantalla donde se puede actuar.
 *
 * POR QUÉ NO SE PUEDEN CERRAR
 * Un aviso descartable invita a descartarlo, y el problema sigue ahí. Estos
 * desaparecen solos cuando la situación que los provoca deja de ser cierta:
 * registrar el pago del arriendo quita el aviso del arriendo. Es la ventaja de
 * calcularlos en vez de guardarlos.
 */
export default function Alertas({ resumen }) {
  if (!resumen) return null

  const { alertas, ingresosDelMes, gastadoHastaHoy, compromisosPendientes,
    disponible, holgura, lectura, diasTranscurridos, diasDelMes } = resumen

  // Lo grave arriba. El servidor los devuelve en orden de cálculo, que no es
  // el orden en que le sirven al usuario.
  const ordenadas = [...alertas].sort(
    (a, b) => (PESO_SEVERIDAD[a.severidad] ?? 9) - (PESO_SEVERIDAD[b.severidad] ?? 9))

  const sinNada = ordenadas.length === 0

  return (
    <section className="bloque" aria-label="Cómo va tu mes">
      <div className="bloque__cabecera">
        <h2 className="bloque__titulo">Cómo va tu mes</h2>
        <span className="bloque__meta">Día {diasTranscurridos} de {diasDelMes}</span>
      </div>

      {/* El termómetro: la resta entera en una sola tira. */}
      <div className="termometro">
        <Cifra rotulo="Te entró" valor={ingresosDelMes} tono="positivo" />
        <span className="termometro__signo" aria-hidden="true">−</span>
        <Cifra rotulo="Has gastado" valor={gastadoHastaHoy} tono="negativo" />
        <span className="termometro__signo" aria-hidden="true">−</span>
        <Cifra rotulo="Falta por pagar" valor={compromisosPendientes}
          tono={Number(compromisosPendientes) > 0 ? 'negativo' : ''} />
        <span className="termometro__signo termometro__signo--igual" aria-hidden="true">=</span>
        <Cifra rotulo="Te queda libre" valor={holgura}
          tono={Number(holgura) < 0 ? 'negativo' : 'positivo'} destacado />
      </div>

      {/* El texto dice lo mismo que los colores, para quien no los distingue. */}
      <p className="termometro__lectura">{lectura}</p>

      {sinNada ? (
        <div className="alerta alerta--exito">
          <span className="alerta__icono" aria-hidden="true">✓</span>
          <div>
            <p className="alerta__titulo">Sin avisos por ahora</p>
            <p className="alerta__texto">
              Tu ritmo de gasto y tus compromisos van dentro de lo previsto.
              {Number(disponible) > 0 && ` Tienes ${formatearDinero(disponible)} disponibles.`}
            </p>
          </div>
        </div>
      ) : (
        <ul className="avisos">
          {ordenadas.map((a, i) => {
            const pinta = SEVERIDADES[a.severidad] ?? SEVERIDADES.BAJA
            return (
              <li key={`${a.tipo}-${i}`} className={`aviso aviso--${pinta.clase}`}>
                {/*
                  Chip con ícono por severidad, como en la referencia visual.
                  El ícono es la señal más rápida en una columna de avisos: se
                  distingue un triángulo de un reloj antes de leer la palabra.
                  Pero la palabra sigue ahí — "Urgente", "Atención" — porque un
                  ícono solo obliga a aprender un código (RNF-008).
                */}
                <span className={`aviso__chip aviso__chip--${pinta.clase}`} aria-hidden="true">
                  {a.severidad === 'ALTA' ? <IconoAviso size={15} />
                    : a.severidad === 'MEDIA' ? <IconoSube size={15} />
                      : <IconoPendiente size={15} />}
                </span>
                <div className="aviso__cuerpo">
                  <p className="aviso__severidad">{pinta.texto}</p>
                  <p className="aviso__titulo">{a.titulo}</p>
                  <p className="aviso__mensaje">{a.mensaje}</p>
                </div>
                {/* Un aviso sin salida frustra: si le dices que no le alcanza,
                    dile también dónde mirarlo. */}
                {a.rutaSugerida && (
                  <Link className="aviso__accion" to={a.rutaSugerida}>Revisar</Link>
                )}
              </li>
            )
          })}
        </ul>
      )}
    </section>
  )
}

function Cifra({ rotulo, valor, tono, destacado }) {
  return (
    <div className={`termometro__dato${destacado ? ' termometro__dato--destacado' : ''}`}>
      <span className="termometro__rotulo">{rotulo}</span>
      <strong className={`termometro__valor ${tono ?? ''}`}>{formatearDinero(valor)}</strong>
    </div>
  )
}
