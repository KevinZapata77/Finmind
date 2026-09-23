package com.finmind.identidad.service;

/**
 * El HTML de los correos de FinMind.
 *
 * POR QUE EL CORREO NO SE MAQUETA COMO UNA PAGINA
 * Un cliente de correo no es un navegador. Gmail borra la etiqueta style,
 * Outlook renderiza con el motor de Word —sin flexbox, sin grid, sin position—,
 * y cada uno recorta lo que le parece. Lo que aqui se ve raro es lo que
 * sobrevive a todos:
 *
 *   1. TABLAS para maquetar. En 2026 es una aberracion en la web y la unica
 *      forma confiable de centrar una caja en Outlook.
 *   2. ESTILOS EN LINEA, atributo por atributo. No hay hoja de estilos que
 *      valga: se elimina antes de que la persona lo abra.
 *   3. NADA DE FUENTES EXTERNAS. Se usan las del sistema; una @font-face no
 *      carga y deja un salto de tipografia feo.
 *   4. ANCHO FIJO DE 560 px. Mas que eso se corta en el panel de vista previa
 *      de Outlook, que es donde mucha gente lee sin abrir el mensaje.
 *
 * SE MANDA TAMBIEN EN TEXTO PLANO, Y NO ES UN TRAMITE
 * Todo correo sale con las dos versiones. Sirve para tres cosas: los clientes
 * que no muestran HTML, los lectores de pantalla que prefieren el texto, y los
 * filtros de spam, que desconfian de un mensaje HTML sin alternativa. El
 * codigo tiene que poder leerse aunque no se vea ni un color.
 *
 * LOS COLORES SON LOS MISMOS TOKENS DE LA APLICACION, escritos a mano porque
 * aqui no existen las variables CSS. Si el tema cambia, esto hay que
 * actualizarlo: es el precio de que el correo se parezca a la aplicacion.
 */
final class PlantillaCorreo {

    private static final String CANVAS  = "#0B0E14";   // color-canvas
    private static final String TARJETA = "#151A23";   // color-tarjeta
    private static final String BORDE   = "#2A3240";   // color-neutral-400
    private static final String TEAL    = "#2DD4BF";   // color-primary-700
    private static final String TEAL_BG = "#14B8A6";   // color-primary-600
    private static final String TEXTO   = "#E6E8EC";   // color-neutral-900
    private static final String APAGADO = "#9BA3AF";   // color-neutral-700
    private static final String TENUE   = "#7C8493";   // color-neutral-500

    private static final String FUENTE =
            "-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif";

    private PlantillaCorreo() {
    }

    /** Correo de verificacion de la cuenta. */
    static String verificacion(String nombre, String codigo, int minutos) {
        return armar(nombre, codigo, minutos,
                "Confirma tu correo",
                "Escribe este código en FinMind para terminar de crear tu cuenta.",
                "Si no creaste esta cuenta, ignora este mensaje: sin el código no se activa nada.");
    }

    /** Correo de recuperacion de la contrasena. */
    static String recuperacion(String nombre, String codigo, int minutos) {
        return armar(nombre, codigo, minutos,
                "Restablece tu contraseña",
                "Escribe este código en FinMind para crear una contraseña nueva.",
                "Si no pediste este cambio, ignora este mensaje: tu contraseña sigue igual.");
    }

    private static String armar(String nombre, String codigo, int minutos,
                                String titulo, String instruccion, String aviso) {
        return """
            <!DOCTYPE html>
            <html lang="es">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width,initial-scale=1">
              <title>%1$s</title>
            </head>
            <body style="margin:0;padding:0;background:%2$s;">
              <!-- Preencabezado: es lo que la bandeja muestra junto al asunto, antes
                   de abrir el mensaje. Sin esto Gmail muestra el primer texto que
                   encuentre, que suele ser el nombre de la marca repetido. Se oculta
                   con alto cero y color transparente, que es el truco que funciona
                   en todos los clientes. -->
              <div style="display:none;max-height:0;overflow:hidden;opacity:0;">
                Tu código es %3$s y vence en %4$d minutos.
              </div>

              <table role="presentation" width="100%%" cellpadding="0" cellspacing="0"
                     style="background:%2$s;padding:32px 12px;">
                <tr>
                  <td align="center">

                    <table role="presentation" width="560" cellpadding="0" cellspacing="0"
                           style="width:560px;max-width:100%%;background:%5$s;
                                  border:1px solid %6$s;border-radius:12px;">

                      <!-- Marca -->
                      <tr>
                        <td style="padding:28px 32px 0;">
                          <table role="presentation" cellpadding="0" cellspacing="0">
                            <tr>
                              <td style="background:%7$s;border-radius:7px;width:30px;height:30px;
                                         text-align:center;vertical-align:middle;
                                         font:700 15px/30px %8$s;color:%2$s;">F</td>
                              <td style="padding-left:10px;font:700 18px %8$s;color:%9$s;">FinMind</td>
                            </tr>
                          </table>
                        </td>
                      </tr>

                      <tr>
                        <td style="padding:24px 32px 0;font:700 24px/1.3 %8$s;color:%9$s;">%1$s</td>
                      </tr>

                      <tr>
                        <td style="padding:14px 32px 0;font:400 15px/1.6 %8$s;color:%10$s;">
                          Hola %11$s, %12$s
                        </td>
                      </tr>

                      <!-- El codigo.
                           Va en una tabla propia y no en un parrafo porque necesita
                           fondo, borde y espacio a los lados, y un div con padding no
                           se comporta igual en Outlook.
                           La fuente es monoespaciada para que no se confundan el cero
                           y la O, y el espaciado entre letras da aire para copiarlo a
                           mano desde un telefono. -->
                      <tr>
                        <td style="padding:24px 32px 0;">
                          <table role="presentation" width="100%%" cellpadding="0" cellspacing="0"
                                 style="background:%2$s;border:1px solid %6$s;border-radius:10px;">
                            <tr>
                              <td align="center" style="padding:22px 12px;
                                     font:700 34px 'SF Mono',Menlo,Consolas,monospace;
                                     letter-spacing:10px;color:%7$s;">%3$s</td>
                            </tr>
                          </table>
                        </td>
                      </tr>

                      <tr>
                        <td style="padding:16px 32px 0;font:400 13px/1.6 %8$s;color:%13$s;">
                          Vence en %4$d minutos y solo se puede usar una vez.
                        </td>
                      </tr>

                      <tr>
                        <td style="padding:20px 32px 28px;font:400 13px/1.6 %8$s;color:%13$s;
                                   border-top:1px solid %6$s;margin-top:20px;">
                          %14$s
                        </td>
                      </tr>
                    </table>

                    <table role="presentation" width="560" cellpadding="0" cellspacing="0"
                           style="width:560px;max-width:100%%;">
                      <tr>
                        <td align="center" style="padding:20px 12px;font:400 12px/1.6 %8$s;color:%13$s;">
                          FinMind · Tus finanzas, en orden<br>
                          Este es un mensaje automático, no hace falta responderlo.
                        </td>
                      </tr>
                    </table>

                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(
                titulo,        // 1
                CANVAS,        // 2
                codigo,        // 3
                minutos,       // 4
                TARJETA,       // 5
                BORDE,         // 6
                TEAL,          // 7
                FUENTE,        // 8
                TEXTO,         // 9
                APAGADO,       // 10
                nombre,        // 11
                instruccion,   // 12
                TENUE,         // 13
                aviso);        // 14
    }

    /**
     * La version en texto plano.
     *
     * No es el HTML sin etiquetas: es un mensaje escrito para leerse asi. El
     * codigo va solo en su linea y con espacio alrededor para que se distinga
     * de un vistazo, que es lo unico que la persona vino a buscar.
     */
    static String texto(String nombre, String codigo, int minutos, String instruccion, String aviso) {
        return """
            Hola %s,

            %s

                %s

            Vence en %d minutos y solo se puede usar una vez.

            %s

            --
            FinMind · Tus finanzas, en orden
            Este es un mensaje automatico, no hace falta responderlo.
            """.formatted(nombre, instruccion, codigo, minutos, aviso);
    }
}
