import { MESES } from '../api/cliente'

/** Selector de período compartido por panel y presupuestos. */
export default function SelectorDeMes({ anio, mes, onCambiar }) {
  const anioActual = new Date().getFullYear()
  const anios = [anioActual - 1, anioActual, anioActual + 1]

  return (
    <div className="periodo">
      <label className="periodo__campo">
        <span className="campo__etiqueta">Mes</span>
        <select className="campo__control" value={mes}
          onChange={(e) => onCambiar(anio, Number(e.target.value))}>
          {MESES.map((m, i) => <option key={m} value={i + 1}>{m}</option>)}
        </select>
      </label>
      <label className="periodo__campo">
        <span className="campo__etiqueta">Año</span>
        <select className="campo__control" value={anio}
          onChange={(e) => onCambiar(Number(e.target.value), mes)}>
          {anios.map((a) => <option key={a} value={a}>{a}</option>)}
        </select>
      </label>
    </div>
  )
}
