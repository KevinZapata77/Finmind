import { irAGoogle } from '../api/cliente'

// El acceso con Google necesita credenciales en el backend Y el perfil 'google'
// activo. Sin eso la ruta /oauth2/authorization/google no existe y el boton
// lleva a un error. Mostrar un boton que siempre falla es peor que no mostrarlo.
const HABILITADO = import.meta.env.VITE_GOOGLE_HABILITADO === 'true'

/** RF-029. Saca el navegador de la aplicacion y lo devuelve con el token. */
export default function BotonGoogle({ texto = 'Continuar con Google' }) {
  if (!HABILITADO) return null

  return (
    <>
      <div className="separador"><span>o</span></div>
      <button type="button" className="boton boton--google" onClick={irAGoogle}>
        <span className="boton__marca" aria-hidden="true">G</span>
        {texto}
      </button>
    </>
  )
}
