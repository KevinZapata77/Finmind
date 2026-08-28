import { NavLink } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { ES_ADMIN } from '../api/cliente'
import {
  IconoInicio, IconoMovimientos, IconoPresupuesto, IconoGastoFijo,
  IconoCredito, IconoMeta, IconoCuenta, IconoCategoria, IconoAdmin,
} from './Iconos'

/**
 * Estructura común de las pantallas con sesión: barra lateral y contenido.
 *
 * Vive aparte para que la navegación exista en un solo lugar. Antes estaba
 * escrita dentro del Panel, con enlaces muertos, y cada pantalla nueva habría
 * tenido que copiarla.
 */
// El orden importa: primero lo que se usa a diario, despues lo que se configura
// una vez. Cuentas y Categorias estaban arriba y eran el primer tramite que veia
// alguien recien registrado, antes de poder anotar un solo peso.
// El ícono acompaña al texto, nunca lo reemplaza: la etiqueta siempre está
// escrita. Un menú de puros íconos obliga a adivinar, y aquí se navega a
// diario — la forma se aprende, pero la palabra no falla nunca.
const SECCIONES = [
  { a: '/panel', texto: 'Inicio', Icono: IconoInicio },
  { a: '/movimientos', texto: 'Movimientos', Icono: IconoMovimientos },
  { a: '/presupuestos', texto: 'Presupuestos', Icono: IconoPresupuesto },
  // Va pegado a Presupuestos: los dos responden a "cuanto puedo gastar".
  { a: '/gastos-fijos', texto: 'Gastos fijos', Icono: IconoGastoFijo },
  { a: '/obligaciones', texto: 'Créditos y préstamos', Icono: IconoCredito },
  { a: '/metas', texto: 'Metas', Icono: IconoMeta },
  { a: '/cuentas', texto: 'Cuentas', Icono: IconoCuenta },
  { a: '/categorias', texto: 'Categorías', Icono: IconoCategoria },
]

export default function Layout({ titulo, acciones, children }) {
  const { usuario, cerrarSesion } = useAuth()
  const iniciales = `${usuario?.nombre?.[0] ?? ''}${usuario?.apellido?.[0] ?? ''}`.toUpperCase()

  return (
    <div className="aplicacion">
      <aside className="barra-lateral">
        <div className="marca marca--clara"><span className="marca__logo">F</span> FinMind</div>

        <nav className="navegacion" aria-label="Secciones de la aplicación">
          {/* Administración solo aparece con el rol. Ocultarla no es la seguridad:
              el backend responde 403 igual. Es para no mostrar una puerta cerrada. */}
          {[...SECCIONES, ...(ES_ADMIN(usuario)
            ? [{ a: '/administracion', texto: 'Administración', Icono: IconoAdmin }]
            : [])].map(({ a, texto, Icono }) => (
            <NavLink
              key={a}
              to={a}
              className={({ isActive }) =>
                `navegacion__item${isActive ? ' navegacion__item--activo' : ''}`}
            >
              <Icono className="navegacion__icono" />
              {texto}
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
