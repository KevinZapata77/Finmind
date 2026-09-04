/**
 * Los íconos de FinMind, en un solo archivo.
 *
 * POR QUÉ TODO PASA POR AQUÍ
 * Para que el conjunto de íconos que usa la aplicación sea una lista visible y
 * corta, y no un import suelto en cada pantalla. Si mañana hay que cambiar de
 * librería, se cambia aquí y no en quince archivos.
 *
 * POR QUÉ LUCIDE Y NO OTRA
 * Se comparó con Tabler: 148 kB comprimidos contra 523 kB. Con Vite se sacude
 * lo que no se usa, así que lo que de verdad cuesta es 1 o 2 kB por ícono
 * importado. Licencia ISC. Es la única dependencia de cliente que se agrega
 * al proyecto, que pasa de 3 a 4.
 *
 * CÓMO SE USAN
 * Todos los íconos son DECORATIVOS: acompañan a un texto que ya dice lo mismo.
 * Por eso llevan aria-hidden y nunca son la única forma de entender algo. Un
 * ícono sin texto al lado obliga a adivinar, y en una aplicación de dinero
 * adivinar sale caro.
 */
import {
  LayoutDashboard,
  ArrowLeftRight,
  Target,
  CalendarClock,
  Landmark,
  PiggyBank,
  Wallet,
  Tags,
  ShieldCheck,
  TrendingUp,
  TrendingDown,
  Minus,
  Banknote,
  CreditCard,
  AlertTriangle,
  CheckCircle2,
  Clock,
  ArrowDownLeft,
  ArrowUpRight,
  Scale,
  Bell,
  Search,
} from 'lucide-react'

/**
 * Tamaño y grosor únicos para toda la aplicación.
 *
 * El grosor 1.75 está entre el 2 por defecto de Lucide —que al lado de una
 * tipografía de peso 400 se ve pesado— y el 1.5, que a 18px se desvanece.
 */
const BASE = { size: 18, strokeWidth: 1.75, 'aria-hidden': true, focusable: false }

/** Un ícono decorativo: hereda el color del texto que acompaña. */
const icono = (Componente) => function Icono(props) {
  return <Componente {...BASE} {...props} />
}

// --- Navegación. Uno por sección del menú, en el mismo orden. ---
export const IconoInicio      = icono(LayoutDashboard)
export const IconoMovimientos = icono(ArrowLeftRight)
export const IconoPresupuesto = icono(Target)
export const IconoGastoFijo   = icono(CalendarClock)
export const IconoCredito     = icono(Landmark)
export const IconoMeta        = icono(PiggyBank)
export const IconoCuenta      = icono(Wallet)
export const IconoCategoria   = icono(Tags)
export const IconoAdmin       = icono(ShieldCheck)

// --- Contenido ---
export const IconoSube        = icono(TrendingUp)
export const IconoBaja        = icono(TrendingDown)
export const IconoIgual       = icono(Minus)
export const IconoDinero      = icono(Banknote)
export const IconoTarjeta     = icono(CreditCard)
export const IconoAviso       = icono(AlertTriangle)
export const IconoListo       = icono(CheckCircle2)
export const IconoPendiente   = icono(Clock)

/**
 * Dirección del dinero. La flecha entra o sale, y eso se entiende antes de
 * leer el signo: es la señal más rápida en una lista larga de movimientos.
 * Va siempre acompañada del signo escrito y del color (RNF-008).
 */
export const IconoEntra       = icono(ArrowDownLeft)
export const IconoSale        = icono(ArrowUpRight)
export const IconoPatrimonio  = icono(Scale)
export const IconoCampana     = icono(Bell)
export const IconoBuscar      = icono(Search)
