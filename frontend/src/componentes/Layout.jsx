import { NavLink } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

/**
 * Estructura común de las pantallas con sesión: barra lateral y contenido.
 *
 * Vive aparte para que la navegación exista en un solo lugar. Antes estaba
 * escrita dentro del Panel, con enlaces muertos, y cada pantalla nueva habría
 * tenido que copiarla.
 */
const SECCIONES = [
  { a: '/panel', texto: 'Panel' },
  { a: '/movimientos', texto: 'Movimientos' },
  { a: '/presupuestos', texto: 'Presupuestos' },
  { a: '/cuentas', texto: 'Cuentas' },
  { a: '/obligaciones', texto: 'Obligaciones' },
]

export default function Layout({ titulo, acciones, children }) {
  const { usuario, cerrarSesion } = useAuth()
  const iniciales = `${usuario?.nombre?.[0] ?? ''}${usuario?.apellido?.[0] ?? ''}`.toUpperCase()

  return (
    <div className="aplicacion">
      <aside className="barra-lateral">
        <div className="marca marca--clara"><span className="marca__logo">F</span> FinMind</div>

        <nav className="navegacion" aria-label="Secciones de la aplicación">
          {SECCIONES.map((s) => (
            <NavLink
              key={s.a}
              to={s.a}
              className={({ isActive }) =>
                `navegacion__item${isActive ? ' navegacion__item--activo' : ''}`}
            >
              {s.texto}
            </NavLink>
          ))}
        </nav>

        <div className="barra-lateral__pie">
          <div className="usuario">
            <span className="usuario__avatar" aria-hidden="true">{iniciales}</span>
            <div>
              <p className="usuario__nombre">{usuario?.nombre} {usuario?.apellido}</p>
              <button type="button" className="enlace-boton enlace-boton--claro" onClick={cerrarSesion}>
                Cerrar sesión
              </button>
            </div>
          </div>
        </div>
      </aside>

      <main className="contenido">
        <header className="contenido__encabezado">
          <h1 className="contenido__titulo">{titulo}</h1>
          {acciones}
        </header>
        {children}
      </main>
    </div>
  )
}
