/**
 * El tipo se comunica con icono y texto, no solo con color.
 * Es el criterio UXA-03: la información no puede depender únicamente del color.
 */
const ICONO = { error: '!', exito: 'OK', aviso: '~' }

export default function Alerta({ tipo = 'error', titulo, children }) {
  return (
    <div className={`alerta alerta--${tipo}`} role={tipo === 'error' ? 'alert' : 'status'}>
      <span className="alerta__icono" aria-hidden="true">{ICONO[tipo]}</span>
      <div>
        {titulo && <p className="alerta__titulo">{titulo}</p>}
        <p className="alerta__texto">{children}</p>
      </div>
    </div>
  )
}
