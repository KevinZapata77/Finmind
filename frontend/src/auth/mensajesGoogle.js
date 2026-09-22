/**
 * Traduce los códigos de error del acceso con Google a algo que se pueda leer.
 *
 * POR QUÉ EXISTE ESTE ARCHIVO
 * El backend devuelve el navegador a /iniciar-sesion?error=<código>. Antes
 * mandaba la frase completa, y la barra de direcciones terminaba mostrando
 * "?error=Ese%20correo%20ya%20tiene%20una%20cuenta%20en%20FinMind": el mensaje
 * quedaba a la vista, codificado, y parecía que la aplicación se había roto.
 *
 * Con códigos cortos la URL queda limpia y el texto vive aquí, junto al resto
 * de lo que lee el usuario. Cambiar una redacción ya no obliga a desplegar el
 * backend.
 *
 * CADA MENSAJE TIENE QUE DECIR QUÉ HACER
 * Un error que solo informa deja a la persona en el mismo punto donde se
 * atascó. Por eso ninguno termina en el diagnóstico: todos siguen con el paso
 * siguiente, y ese paso está a un clic de esta misma pantalla.
 */
const MENSAJES = {
  /*
    El caso frecuente y el que más importa cuidar.

    Suele ser alguien que se registró, no alcanzó a poner el código que le
    llegó, y volvió días después por el botón de Google. No hizo nada mal, así
    que el mensaje no lo trata como sospechoso: le dice qué falta y dónde está.

    Tampoco se le explica que la verificación es lo que impide que otro se
    apropie de su cuenta. Es cierto, pero a quien solo quiere entrar no le
    sirve, y mencionar una amenaza asusta sin ayudar.
  */
  /*
    Ya no se emite: desde RN-034 una cuenta sin verificar no se rechaza, Google
    se queda con ella. Se conserva por la ventana de despliegue —Vercel publica
    antes que Render, asi que durante unos minutos el navegador nuevo habla con
    el backend viejo— y porque borrarlo solo ahorraria seis lineas.
  */
  cuenta_sin_verificar:
    'Ya tienes una cuenta con ese correo, pero te falta verificarla. '
    + 'Entra con tu contraseña y escribe el código que te enviamos; '
    + 'después podrás usar el botón de Google cuando quieras.',

  /*
    Google autenticó, pero no garantiza ese correo. Pasa sobre todo con cuentas
    de empresa donde el administrador creó direcciones que Google nunca
    comprobó.

    La persona no puede hacer nada al respecto desde su lado, así que se le
    ofrece el camino que sí funciona en vez de pedirle que arregle algo que no
    está en sus manos.
  */
  google_correo_sin_verificar:
    'Google no confirma que ese correo sea tuyo, así que no podemos usarlo '
    + 'para entrar. Crea tu cuenta con correo y contraseña.',

  google_sin_correo:
    'Tu cuenta de Google no tiene un correo asociado y lo necesitamos para '
    + 'identificarte. Crea tu cuenta con correo y contraseña.',

  /*
    Desactivada por un administrador. No se dice por qué —el frontend no lo
    sabe y adivinar sería peor—, pero sí a dónde escribir.
  */
  cuenta_inactiva:
    'Esa cuenta está desactivada. Escríbenos si crees que es un error.',

  /*
    El comodín: algo falló del lado del servidor. No se muestra el detalle
    técnico porque no le sirve a nadie que esté frente a la pantalla, y porque
    los mensajes internos a veces cuentan más de la cuenta.
  */
  google_fallo:
    'No pudimos completar el acceso con Google. Vuelve a intentarlo en un '
    + 'momento o entra con tu correo y contraseña.',
}

/**
 * Devuelve el texto de un código. Si llega algo que no está en la tabla, se
 * muestra tal cual.
 *
 * Ese respaldo es a propósito y cubre dos casos reales: una versión del backend
 * más nueva que la del navegador —despliegues que no caen a la vez— y las
 * versiones anteriores, que mandaban la frase completa en vez del código.
 * Mostrar un texto imperfecto siempre es mejor que dejar la alerta vacía.
 */
export function mensajeDeGoogle(codigo) {
  if (!codigo) return null
  return MENSAJES[codigo] ?? codigo
}
