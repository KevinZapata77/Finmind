import { useEffect, useState } from 'react'
import { NavLink, useLocation } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { ES_ADMIN } from '../api/cliente'
import {
  IconoInicio, IconoMovimientos, IconoPresupuesto, IconoGastoFijo,
  IconoCredito, IconoMeta, IconoCuenta, IconoCategoria, IconoAdmin, IconoMarca,
} from './Iconos'

/**
 * Estructura común de las pantallas con sesión: barra lateral y contenido.
 *
 * Vive aparte para que la navegación exista en un solo lugar. Antes estaba
 * escrita dentro del Panel, con enlaces muertos, y cada pantalla nueva habría
 * tenido que copiarla.
 *
 * EN MÓVIL EL MENÚ SE ABRE CON UN BOTÓN
 * En escritorio la barra lateral está siempre a la vista, que es lo correcto:
 * hay espacio de sobra y tener los nueve destinos visibles ahorra un clic en
 * cada salto.
 *
 * En un teléfono no. Los nueve enlaces ocupaban la pantalla entera antes de
 * dejar ver el contenido, y la versión anterior —una tira horizontal que se
 * deslizaba— escondía la mitad de los destinos fuera del borde sin avisar:
 * quien no arrastrara nunca sabía que existían Metas o Categorías.
 *
 * Ahora el menú está cerrado por omisión y se abre con el botón. Se cierra
 * solo al elegir un destino, que es lo que la persona espera: ya llegó a donde
 * iba, el menú estorba.
 */
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
  const { pathname } = useLocation()
  const [menuAbierto, setMenuAbierto] = useState(false)
  const iniciales = `${usuario?.nombre?.[0] ?? ''}${usuario?.apellido?.[0] ?? ''}`.toUpperCase()

  /*
    Cerrar al cambiar de pantalla.

    Se escucha el pathname y no el clic del enlace: así también se cierra
    cuando la navegación viene de otro lado —el botón "atrás" del teléfono, o
    un enlace desde dentro del contenido, como los del panel que llevan al
    detalle—. Atarlo al onClick del enlace dejaría el menú abierto en esos
    casos, encima de la pantalla nueva.
  */
  useEffect(() => { setMenuAbierto(false) }, [pathname])

  /*
    Escape cierra el menú. Es lo que espera cualquiera que use teclado, y
    cuesta cuatro líneas.
  */
  useEffect(() => {
    if (!menuAbierto) return
    const alPulsar = (e) => { if (e.key === 'Escape') setMenuAbierto(false) }
    window.addEventListener('keydown', alPulsar)
    return () => window.removeEventListener('keydown', alPulsar)
  }, [menuAbierto])

  /*
    Con el menú abierto, la página de atrás NO se desplaza.

    Sin esto el menú queda flotando sobre un contenido que sigue moviéndose:
    se arrastra el dedo y las tarjetas suben por debajo de los enlaces. No es
    un fallo de dibujo, es que hay dos zonas desplazables compitiendo por el
    mismo gesto, y el resultado se lee como que la pantalla está rota.

    Se guarda el valor anterior en vez de poner 'auto' al salir: si algún día
    otra pantalla define su propio overflow en body, esto lo respeta.
  */
  useEffect(() => {
    if (!menuAbierto) return
    const anterior = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => { document.body.style.overflow = anterior }
  }, [menuAbierto])

  /*
    Al pasar a escritorio el menú se cierra. Si alguien lo abre en vertical y
    gira el teléfono, el botón desaparece —en escritorio no existe— y el estado
    quedaría abierto para siempre, con el desplazamiento bloqueado y sin forma
    de soltarlo.
  */
  useEffect(() => {
    const escritorio = window.matchMedia('(min-width: 768px)')
    const alCambiar = (e) => { if (e.matches) setMenuAbierto(false) }
    escritorio.addEventListener('change', alCambiar)
    return () => escritorio.removeEventListener('change', alCambiar)
  }, [])

  const destinos = [...SECCIONES, ...(ES_ADMIN(usuario)
    ? [{ a: '/administracion', texto: 'Administración', Icono: IconoAdmin }]
    : [])]

  return (
    <div className="aplicacion">
      <aside className={`barra-lateral${menuAbierto ? ' barra-lateral--abierta' : ''}`}>
        <div className="barra-lateral__cabecera">
          {/*
            El botón solo existe en móvil: en escritorio la barra está siempre
            desplegada y un botón para abrir algo que ya está abierto confunde.
            Se oculta con CSS, no se desmonta, para no duplicar el árbol.
          */}
          <button
            type="button"
            className="boton-menu"
            aria-expanded={menuAbierto}
            aria-controls="navegacion-principal"
            aria-label={menuAbierto ? 'Cerrar el menú' : 'Abrir el menú'}
            onClick={() => setMenuAbierto(!menuAbierto)}
          >
            <span className="boton-menu__barras" aria-hidden="true" />
          </button>

          <div className="marca marca--clara">
            <span className="marca__logo"><IconoMarca /></span> FinMind
          </div>
        </div>

        <nav
          id="navegacion-principal"
          className="navegacion"
          aria-label="Secciones de la aplicación"
        >
          {/* Administración solo aparece con el rol. Ocultarla no es la seguridad:
              el backend responde 403 igual. Es para no mostrar una puerta cerrada. */}
          {destinos.map(({ a, texto, Icono }) => (
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

      {/*
        Velo. Atenúa el contenido y, tocándolo, cierra el menú — que es lo que
        hace cualquiera antes de buscar el botón de cerrar.
        aria-hidden y sin foco: para un lector de pantalla no existe, porque no
        aporta nada que no diga ya el botón con aria-expanded.
      */}
      {menuAbierto && (
        <div
          className="velo-menu"
          onClick={() => setMenuAbierto(false)}
          aria-hidden="true"
        />
      )}

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
