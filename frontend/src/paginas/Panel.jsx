import { useAuth } from '../auth/AuthContext'

/**
 * UI-003 — Panel. Estructura mínima con el usuario autenticado.
 * Las tarjetas de balance y el gráfico de gastos por categoría se construyen
 * cuando el backend exponga los endpoints de movimientos (RF-012 a RF-014).
 */
export default function Panel() {
  const { usuario, cerrarSesion } = useAuth()

  return (
    <div className="aplicacion">
      <aside className="barra-lateral">
        <div className="marca marca--clara"><span className="marca__logo">F</span> FinMind</div>
        <nav className="navegacion">
          <a className="navegacion__item navegacion__item--activo" href="#panel">Panel</a>
          <span className="navegacion__item navegacion__item--inactivo">Movimientos</span>
          <span className="navegacion__item navegacion__item--inactivo">Presupuestos</span>
        </nav>
        <div className="barra-lateral__pie">
          <p className="barra-lateral__usuario">{usuario?.nombre} {usuario?.apellido}</p>
          <button className="enlace-boton enlace-boton--claro" onClick={cerrarSesion}>
            Cerrar sesión
          </button>
        </div>
      </aside>

      <main className="contenido">
        <h1 className="contenido__titulo">Panel</h1>
        <div className="tarjeta tarjeta--vacia">
          <p className="vacio__titulo">Todavía no tienes movimientos</p>
          <p className="vacio__texto">
            Cuando el módulo de movimientos esté disponible, aquí verás tu balance
            del mes y la composición de tu gasto por categoría.
          </p>
          <p className="vacio__meta">
            Sesión activa como <strong>{usuario?.correo}</strong> · rol {usuario?.rol}
          </p>
        </div>
      </main>
    </div>
  )
}
