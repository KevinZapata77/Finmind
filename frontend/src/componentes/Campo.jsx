/**
 * Campo con etiqueta persistente. El placeholder nunca sustituye a la etiqueta:
 * es una de las reglas del documento de diseño y un criterio de accesibilidad.
 */
export default function Campo({ id, etiqueta, error, ayuda, ...props }) {
  const idAyuda = ayuda ? `${id}-ayuda` : undefined
  const idError = error ? `${id}-error` : undefined
  return (
    <div className="campo">
      <label className="campo__etiqueta" htmlFor={id}>{etiqueta}</label>
      <input
        id={id}
        className={`campo__control${error ? ' campo__control--error' : ''}`}
        aria-invalid={error ? 'true' : undefined}
        aria-describedby={[idError, idAyuda].filter(Boolean).join(' ') || undefined}
        {...props}
      />
      {ayuda && <p id={idAyuda} className="campo__ayuda">{ayuda}</p>}
      {error && <p id={idError} className="campo__error" role="alert">! {error}</p>}
    </div>
  )
}
