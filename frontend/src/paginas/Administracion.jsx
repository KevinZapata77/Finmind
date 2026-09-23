import { useCallback, useEffect, useState } from 'react'
import { api, ES_ADMIN } from '../api/cliente'
import { useAuth } from '../auth/AuthContext'
import Layout from '../componentes/Layout'
import Alerta from '../componentes/Alerta'

/** UI-009 — Administración. Implementa HU-020 / RF-023, RF-024. */
export default function Administracion() {
  const { usuario } = useAuth()
  const [usuarios, setUsuarios] = useState([])
  const [resumen, setResumen] = useState(null)
  const [auditoria, setAuditoria] = useState([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)
  const [pestana, setPestana] = useState('usuarios')

  const cargar = useCallback(async () => {
    setCargando(true); setError(null)
    try {
      const [u, r, a] = await Promise.all([
        api.adminUsuarios(), api.adminResumen(), api.adminAuditoria(),
      ])
      setUsuarios(u); setResumen(r); setAuditoria(a)
    } catch (err) {
      setError(err.message)
    } finally {
      setCargando(false)
    }
  }, [])

  useEffect(() => {
    if (ES_ADMIN(usuario)) {
      cargar()
    } else {
      setCargando(false)
    }
  }, [cargar, usuario])

  async function alternar(u) {
    const activar = !u.activo
    if (!window.confirm(`¿${activar ? 'Reactivar' : 'Desactivar'} la cuenta de ${u.correo}?`)) return
    try {
      await (activar ? api.adminActivar(u.id) : api.adminDesactivar(u.id))
      await cargar()
    } catch (err) {
      setError(err.message)
    }
  }

  // Ocultar la pantalla no es la seguridad: el backend responde 403 igual.
  // Esto solo evita mostrar una sección vacía a quien no le corresponde.
  if (!ES_ADMIN(usuario)) {
    return (
      <Layout titulo="Administración">
        <Alerta tipo="error" titulo="Esta sección no es para tu cuenta">
          Solo un administrador puede gestionar usuarios.
        </Alerta>
      </Layout>
    )
  }

  const clase = (e) => e === 'Activa' ? 'en_curso' : e === 'Sin verificar' ? 'en_alerta' : 'excedido'

  return (
    <Layout titulo="Administración">
      {error && <Alerta tipo="error" titulo="No pudimos completar la operación">{error}</Alerta>}

      <Alerta tipo="aviso" titulo="Qué puedes y qué no puedes ver aquí">
        Gestionas el acceso a la plataforma, no el dinero de las personas. Esta pantalla
        no muestra saldos, movimientos ni deudas de nadie. Toda activación o desactivación
        queda registrada con tu correo y la fecha.
      </Alerta>

      {/*
        Dos filas, y la separación tiene sentido: arriba el ESTADO de las
        cuentas —cuántas hay y cómo están—, abajo la ACTIVIDAD —si la
        plataforma se está usando—.

        Son preguntas distintas y se responden con números distintos. Tener
        veinte cuentas activas no dice nada si ninguna entró en un mes; por eso
        no se mezclan en una sola fila de ocho, donde el ojo las leería como
        variantes de lo mismo.
      */}
      {resumen && (
        <>
          <section className="tarjetas" aria-label="Estado de las cuentas">
            {[['Usuarios en total', resumen.total], ['Activos', resumen.activos],
              ['Sin verificar', resumen.sinVerificar], ['Desactivados', resumen.desactivados]]
              .map(([rot, val]) => (
                <article key={rot} className="tarjeta-dato">
                  <p className="tarjeta-dato__rotulo">{rot}</p>
                  <p className="tarjeta-dato__valor">{val}</p>
                </article>
              ))}
          </section>

          <section className="tarjetas" aria-label="Actividad de la plataforma">
            {[
              ['Nuevos, últimos 7 días', resumen.registrosUltimos7Dias,
               'Cuentas creadas en la última semana'],
              ['Nuevos, últimos 30 días', resumen.registrosUltimos30Dias,
               'Cuentas creadas en el último mes'],
              ['Entraron esta semana', resumen.activosUltimos7Dias,
               'Iniciaron sesión en los últimos 7 días'],
              /*
                Este es el número que más dice de los ocho, y por eso lleva su
                propia explicación: alguien que se registró y nunca volvió
                significa que algo se rompió entre el registro y el primer uso
                —el correo que no llega, por ejemplo—. Ese hueco no aparece en
                ningún otro contador.
              */
              ['Nunca han entrado', resumen.nuncaIniciaronSesion,
               'Se registraron pero jamás iniciaron sesión'],
            ].map(([rot, val, ayuda]) => (
              <article key={rot} className="tarjeta-dato">
                <p className="tarjeta-dato__rotulo">{rot}</p>
                <p className="tarjeta-dato__valor">{val}</p>
                <p className="tarjeta-dato__ayuda">{ayuda}</p>
              </article>
            ))}
          </section>
        </>
      )}

      {/*
        La misma información de arriba, pero vista.

        Las cifras dicen cuántas hay; la barra dice qué proporción representan,
        y eso el ojo lo saca de un vistazo mientras que con números hay que
        hacer la cuenta. Tres estados y cien por ciento: no hace falta más.

        Es SVG dibujado a mano, como el resto de los gráficos de FinMind. Sin
        librería: son cuatro rectángulos, y traer una dependencia entera para
        esto sería cargar cien kilobytes para dibujar lo que cabe en veinte
        líneas.
      */}
      {resumen && resumen.total > 0 && (
        <section className="tarjeta" aria-label="Distribución de las cuentas">
          <h2 className="tarjeta__titulo">Cómo están las {resumen.total} cuentas</h2>

          {(() => {
            const partes = [
              ['Activas', resumen.activos, 'var(--color-success-600)'],
              ['Sin verificar', resumen.sinVerificar, 'var(--color-warning-600)'],
              ['Desactivadas', resumen.desactivados, 'var(--color-error-600)'],
            ]
            // El total se recalcula sumando, en vez de usar resumen.total.
            // Si algún día los estados dejaran de cubrir todas las cuentas, la
            // barra seguiría sumando 100% en vez de quedar corta sin avisar.
            const suma = partes.reduce((a, [, v]) => a + v, 0) || 1
            let x = 0

            return (
              <>
                <svg viewBox="0 0 100 8" className="barra-estados" role="img"
                  aria-label={partes.map(([n, v]) => `${n}: ${v}`).join('. ')}>
                  {partes.map(([nombre, valor, color]) => {
                    const ancho = (valor / suma) * 100
                    const inicio = x
                    x += ancho
                    return ancho > 0 ? (
                      <rect key={nombre} x={inicio} y="0" width={ancho} height="8"
                        fill={color} rx="0.6">
                        <title>{`${nombre}: ${valor}`}</title>
                      </rect>
                    ) : null
                  })}
                </svg>

                {/* La leyenda no es decorativa: sin ella el gráfico solo se
                    entiende por color, y eso deja afuera a quien no distingue
                    verde de rojo (criterio UXA-03). */}
                <ul className="leyenda-estados">
                  {partes.map(([nombre, valor, color]) => (
                    <li key={nombre} className="leyenda-estados__item">
                      <span className="leyenda-estados__punto" style={{ background: color }} />
                      <strong>{valor}</strong> {nombre}
                      <span className="leyenda-estados__pct">
                        {Math.round((valor / suma) * 100)}%
                      </span>
                    </li>
                  ))}
                </ul>
              </>
            )
          })()}
        </section>
      )}

      <nav className="pestanas" aria-label="Secciones de administración">
        <button type="button" onClick={() => setPestana('usuarios')}
          className={`pestana ${pestana === 'usuarios' ? 'pestana--activa' : ''}`}>
          Usuarios
        </button>
        <button type="button" onClick={() => setPestana('auditoria')}
          className={`pestana ${pestana === 'auditoria' ? 'pestana--activa' : ''}`}>
          Auditoría ({auditoria.length})
        </button>
      </nav>

      {cargando ? <p className="estado-carga">Cargando…</p> : pestana === 'usuarios' ? (
        <div className="tabla-envoltura">
          <table className="tabla">
            <caption className="tabla__titulo">Usuarios registrados</caption>
            <thead>
              <tr>
                <th scope="col">Usuario</th><th scope="col">Correo</th>
                <th scope="col">Acceso</th><th scope="col">Estado</th>
                <th scope="col">Último acceso</th><th scope="col">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {usuarios.map((u) => (
                <tr key={u.id} className={u.activo ? '' : 'fila--inactiva'}>
                  <td data-rotulo="Usuario">{u.nombre} {u.apellido}</td>
                  <td data-rotulo="Correo">{u.correo}</td>
                  <td data-rotulo="Acceso">{u.proveedor === 'GOOGLE' ? 'Google' : 'Contraseña'}</td>
                  <td data-rotulo="Estado">
                    {/* Estado en texto además del color. */}
                    <span className={`insignia insignia--${clase(u.estado)}`}>{u.estado}</span>
                  </td>
                  <td data-rotulo="Último acceso">{u.ultimoAcceso ? u.ultimoAcceso.slice(0, 10) : 'Nunca'}</td>
                  <td data-rotulo="Acciones">
                    {u.id === usuario.id ? (
                      // Si el último admin se apaga, nadie puede reactivarlo.
                      <span className="apagado">Tu cuenta</span>
                    ) : (
                      <button type="button" className="enlace" onClick={() => alternar(u)}>
                        {u.activo ? 'Desactivar' : 'Reactivar'}
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : auditoria.length === 0 ? (
        <div className="vacio">
          <h2 className="vacio__titulo">Todavía no hay acciones registradas</h2>
          <p className="vacio__texto">
            Cada vez que actives o desactives una cuenta, quedará aquí con tu correo y la fecha.
          </p>
        </div>
      ) : (
        <div className="tabla-envoltura">
          <table className="tabla">
            <caption className="tabla__titulo">Registro de acciones administrativas</caption>
            <thead>
              <tr>
                <th scope="col">Fecha</th><th scope="col">Administrador</th>
                <th scope="col">Acción</th><th scope="col">Sobre</th>
              </tr>
            </thead>
            <tbody>
              {auditoria.map((a) => (
                <tr key={a.id}>
                  <td data-rotulo="Fecha">{a.fecha.replace('T', ' ').slice(0, 16)}</td>
                  <td data-rotulo="Administrador">{a.adminCorreo}</td>
                  <td data-rotulo="Acción">{a.accion === 'DESACTIVAR_USUARIO' ? 'Desactivó una cuenta' : 'Reactivó una cuenta'}</td>
                  <td data-rotulo="Sobre">{a.detalle}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </Layout>
  )
}
