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

      {resumen && (
        <section className="tarjetas" aria-label="Resumen de la plataforma">
          {[['Usuarios en total', resumen.total], ['Activos', resumen.activos],
            ['Sin verificar', resumen.sinVerificar], ['Desactivados', resumen.desactivados]]
            .map(([rot, val]) => (
              <article key={rot} className="tarjeta-dato">
                <p className="tarjeta-dato__rotulo">{rot}</p>
                <p className="tarjeta-dato__valor">{val}</p>
              </article>
            ))}
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
                  <td>{u.nombre} {u.apellido}</td>
                  <td>{u.correo}</td>
                  <td>{u.proveedor === 'GOOGLE' ? 'Google' : 'Contraseña'}</td>
                  <td>
                    {/* Estado en texto además del color. */}
                    <span className={`insignia insignia--${clase(u.estado)}`}>{u.estado}</span>
                  </td>
                  <td>{u.ultimoAcceso ? u.ultimoAcceso.slice(0, 10) : 'Nunca'}</td>
                  <td>
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
                  <td>{a.fecha.replace('T', ' ').slice(0, 16)}</td>
                  <td>{a.adminCorreo}</td>
                  <td>{a.accion === 'DESACTIVAR_USUARIO' ? 'Desactivó una cuenta' : 'Reactivó una cuenta'}</td>
                  <td>{a.detalle}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </Layout>
  )
}
