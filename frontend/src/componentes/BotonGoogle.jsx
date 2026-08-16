import { irAGoogle } from '../api/cliente'

/**
 * RF-029. No es un formulario: saca al navegador de la aplicación hacia
 * Google y este vuelve al callback con el token ya emitido por FinMind.
 */
export default function BotonGoogle({ texto = 'Continuar con Google' }) {
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
