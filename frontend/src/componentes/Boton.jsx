export default function Boton({ tipo = 'primario', cargando = false, children, ...props }) {
  return (
    <button
      className={`boton boton--${tipo}`}
      disabled={cargando || props.disabled}
      aria-busy={cargando}
      {...props}
    >
      {cargando ? 'Procesando…' : children}
    </button>
  )
}
