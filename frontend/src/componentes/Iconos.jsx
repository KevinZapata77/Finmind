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

/**
 * La marca. Antes era la letra "F" dentro de un cuadro, que no dice de qué es
 * la aplicación: una F sirve igual para una ferretería.
 *
 * POR QUÉ UNA LÍNEA QUE BAJA Y SUBE
 * Es la forma con la que se reconoce el dinero en movimiento sin leer nada.
 * Y baja antes de subir a propósito: una flecha que solo sube promete
 * rendimientos, y FinMind no invierte ni promete ganancias — muestra lo que
 * pasa, que unos meses es peor y otros mejor. El punto del final es el dato
 * de hoy, que es lo que la aplicación responde.
 *
 * NO ES DE lucide, A DIFERENCIA DEL RESTO
 * Los íconos de la interfaz vienen de la librería para que sean coherentes
 * entre sí. Un logo es lo contrario: tiene que ser propio, o es el logo de
 * cualquiera. Son cuatro puntos y un círculo, no hace falta más.
 *
 * Decorativo: al lado siempre está escrito "FinMind", así que anunciarlo al
 * lector de pantalla lo repetiría dos veces.
 */
export function IconoMarca({ size = 21 }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24"
      aria-hidden="true" focusable="false">
      {/*
        DOS CAPAS, COMO UN TERMINAL DE BOLSA DE VERDAD

        Atrás las barras —el volumen— y encima la línea de la cotización. Es la
        composición que usa cualquier pantalla de mercado, y por eso se
        reconoce sin leer nada.

        Se llegó aquí después de descartar dos versiones más simples: una sola
        subida (quedaba como flecha de crecimiento, no como cotización) y una
        línea en zigzag sola (la usa igual cualquier aplicación de analítica).
        Lo que hace financiero al dibujo no es que suba y baje: es tener las
        dos capas.

        POR QUÉ LAS BARRAS VAN CON opacity Y NO CON UN TOKEN DE COLOR
        Podrían ser --color-primary-100, que es un token real. Se usa opacity
        porque las barras tienen que quedar por DEBAJO de la línea en peso
        visual: si compiten, el dibujo se convierte en una mancha a 21px. El
        0,32 sobre el teal del chip da un tono intermedio que se lee como fondo.

        Las barras están alineadas por abajo (todas terminan en y=21) porque un
        gráfico de volumen crece desde el eje, no flota.

        La línea va a opacidad completa porque es el elemento que identifica la
        marca: es la que tiene que cumplir contraste, y lo cumple con 5,16:1.
        Las barras son textura de fondo.
      */}
      <g fill="var(--color-sobre-lleno)" opacity="0.32">
        <rect x="2.5" y="12" width="5" height="9" rx="1.1" />
        <rect x="9.5" y="9" width="5" height="12" rx="1.1" />
        <rect x="16.5" y="14" width="5" height="7" rx="1.1" />
      </g>
      <polyline points="2,14 7,8.5 12,12 16,5 21.5,2.5"
        fill="none" stroke="var(--color-sobre-lleno)" strokeWidth="2.2"
        strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}
