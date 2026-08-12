import { Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './auth/AuthContext'
import RutaProtegida from './auth/RutaProtegida'
import IniciarSesion from './paginas/IniciarSesion'
import CrearCuenta from './paginas/CrearCuenta'
import Panel from './paginas/Panel'

export default function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/" element={<Navigate to="/panel" replace />} />
        <Route path="/iniciar-sesion" element={<IniciarSesion />} />
        <Route path="/crear-cuenta" element={<CrearCuenta />} />
        <Route path="/panel" element={<RutaProtegida><Panel /></RutaProtegida>} />
        <Route path="*" element={<Navigate to="/panel" replace />} />
      </Routes>
    </AuthProvider>
  )
}
