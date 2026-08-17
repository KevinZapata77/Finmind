import { Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './auth/AuthContext'
import RutaProtegida from './auth/RutaProtegida'
import IniciarSesion from './paginas/IniciarSesion'
import CrearCuenta from './paginas/CrearCuenta'
import VerificarCorreo from './paginas/VerificarCorreo'
import RecuperarContrasena from './paginas/RecuperarContrasena'
import RestablecerContrasena from './paginas/RestablecerContrasena'
import CallbackGoogle from './paginas/CallbackGoogle'
import Panel from './paginas/Panel'
import Cuentas from './paginas/Cuentas'
import Movimientos from './paginas/Movimientos'
import Presupuestos from './paginas/Presupuestos'
import Obligaciones from './paginas/Obligaciones'
import Categorias from './paginas/Categorias'
import Metas from './paginas/Metas'
import Administracion from './paginas/Administracion'

export default function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/" element={<Navigate to="/panel" replace />} />
        <Route path="/iniciar-sesion" element={<IniciarSesion />} />
        <Route path="/crear-cuenta" element={<CrearCuenta />} />
        <Route path="/verificar" element={<VerificarCorreo />} />
        <Route path="/recuperar" element={<RecuperarContrasena />} />
        <Route path="/restablecer" element={<RestablecerContrasena />} />
        <Route path="/oauth2/callback" element={<CallbackGoogle />} />
        <Route path="/panel" element={<RutaProtegida><Panel /></RutaProtegida>} />
        <Route path="/cuentas" element={<RutaProtegida><Cuentas /></RutaProtegida>} />
        <Route path="/movimientos" element={<RutaProtegida><Movimientos /></RutaProtegida>} />
        <Route path="/presupuestos" element={<RutaProtegida><Presupuestos /></RutaProtegida>} />
        <Route path="/obligaciones" element={<RutaProtegida><Obligaciones /></RutaProtegida>} />
        <Route path="/categorias" element={<RutaProtegida><Categorias /></RutaProtegida>} />
        <Route path="/metas" element={<RutaProtegida><Metas /></RutaProtegida>} />
        <Route path="/administracion" element={<RutaProtegida><Administracion /></RutaProtegida>} />
        <Route path="*" element={<Navigate to="/panel" replace />} />
      </Routes>
    </AuthProvider>
  )
}
