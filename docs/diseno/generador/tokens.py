# Sistema de diseno FinMind - tokens unicos de verdad.
# Los mockups se generan a partir de aqui: si cambia un token, cambian todas las pantallas.
#
# PALETA OSCURA TEAL (CHG-UX-008, version 1.7 del documento de diseno UX/UI).
# Estos valores son los mismos de frontend/src/estilos/tokens.css. Si se cambia
# uno alli, hay que cambiarlo aqui y regenerar, o los mockups dejan de
# parecerse a la aplicacion. Es justo lo que paso entre el 27/08 y el 07/09:
# la aplicacion se volvio oscura y los mockups siguieron claros.
#
# LO QUE HAY QUE ENTENDER AL LEER ESTA TABLA: LA RAMPA SE INVIERTE.
# En la paleta clara neutral.900 era el color mas OSCURO —el texto— y
# neutral.100 el mas claro —un fondo. En oscuro es al contrario: el 900 es el
# mas CLARO porque sigue siendo el texto, y el 100 el mas oscuro porque sigue
# siendo un fondo. El numero no describe la luminosidad: describe el CONTRASTE
# CONTRA EL LIENZO. Por eso el codigo que usa N9 para texto y N1 para fondos
# no hubo que tocarlo.
#
# Lo mismo con primary: el 700 era el teal mas oscuro y ahora es el mas
# brillante, porque en los dos casos es el acento que tiene que resaltar.

TOKENS = {
    "color.primary.700":  "#2DD4BF",
    "color.primary.600":  "#14B8A6",
    "color.primary.500":  "#0D9488",
    "color.primary.100":  "#0F2E27",
    "color.success.600":  "#22C55E",
    # success.700 existe porque el 600 no alcanza 4,5:1 sobre fondos tenidos
    # ni en hover. Se detecto tres veces en sitios distintos (ACC-005).
    "color.success.700":  "#4ADE80",
    "color.success.100":  "#0F2E1B",
    "color.warning.600":  "#FBBF24",
    "color.warning.100":  "#2E2410",
    "color.error.600":     "#F87171",
    "color.error.100":     "#2E1414",
    "color.neutral.900":  "#E6E8EC",
    "color.neutral.700":  "#9BA3AF",
    "color.neutral.500":  "#7C8493",
    "color.neutral.300":  "#252C38",
    "color.neutral.200":  "#1E2530",
    "color.neutral.100":  "#151A23",
    # surface es la TARJETA, no el campo de formulario. En tokens.css hay dos
    # valores distintos: --color-tarjeta (#151A23) para tarjetas y
    # --color-surface (#0E121A) para campos, que en oscuro se hunden. El
    # generador dibuja tarjetas, asi que aqui va el de tarjeta.
    "color.surface":      "#151A23",
    "color.canvas":       "#0B0E14",
    # Texto e iconos que van ENCIMA de un relleno de acento. En la paleta clara
    # este texto era blanco; sobre un teal brillante tiene que ser oscuro.
    "color.sobre-lleno":  "#0B0E14",
    # Velo de los modales. Antes estaba escrito a mano como #111827, que era el
    # neutral.900 de la paleta clara. En oscuro ese valor queda casi igual al
    # lienzo y el velo no se ve: el modal parecia flotar sin nada detras.
    "color.velo":         "#000000",
    # La barra lateral y el lateral de las pantallas de identidad. En el CSS
    # real las dos usan --color-barra, no un teal: se comprobo en app.css.
    # El generador usaba neutral.900 para la barra, que en la paleta clara era
    # el navy oscuro. Al invertir, neutral.900 paso a ser el color del TEXTO y
    # la barra salio blanca con letras negras sobre negro.
    "color.barra":        "#12171F",
    "color.texto-barra":  "#9BA3AF",
    # Bordes y separadores sobre superficies oscuras.
    "color.neutral.400":  "#2A3240",
}

FONT = "Inter, 'Segoe UI', system-ui, sans-serif"
TYPO = {
    "font.display":     (28, 700),
    "font.heading.lg":  (22, 700),
    "font.heading.md":  (17, 600),
    "font.body.md":     (14, 400),
    "font.label":       (12, 600),
    "font.caption":     (11, 400),
    "font.numero.lg":   (26, 700),
}
SPACE_BASE = 8
RADIUS = {"radius.sm": 4, "radius.md": 8, "radius.lg": 12}

# Azul de Google. NO es un token del sistema y no se cambia con el tema: son
# las condiciones de marca de Google las que fijan el color y la presentacion
# del boton. Vive aqui para que quede claro que es una excepcion declarada y no
# un color que alguien olvido pasar a token.
MARCA_GOOGLE = {
    "azul":   "#4285F4",
    "fondo":  "#FFFFFF",
    "texto":  "#1F1F1F",
}


def _lin(c):
    c = c / 255.0
    return c / 12.92 if c <= 0.03928 else ((c + 0.055) / 1.055) ** 2.4


def luminancia(hexcolor):
    h = hexcolor.lstrip("#")
    r, g, b = (int(h[i:i + 2], 16) for i in (0, 2, 4))
    return 0.2126 * _lin(r) + 0.7152 * _lin(g) + 0.0722 * _lin(b)


def contraste(c1, c2):
    l1, l2 = luminancia(c1), luminancia(c2)
    claro, oscuro = max(l1, l2), min(l1, l2)
    return round((claro + 0.05) / (oscuro + 0.05), 2)
