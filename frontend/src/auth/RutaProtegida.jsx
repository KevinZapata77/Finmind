import { Navigate } from 'react-router-dom'
import { useAuth } from './AuthContext'

/**
 * Ocultar una ruta en el cliente NO es seguridad: el backend rechaza igual
 * cualquier petición sin token válido. Esto es solo comodidad de navegación.
 */
export default function RutaProtegida({ children }) {
  const { usuario, cargando } = useAuth()
  if (cargando) return <p className="estado-carga">Cargando…</p>
  if (!usuario) return <Navigate to="/iniciar-sesion" replace />
  return children
}
