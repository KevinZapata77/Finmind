# -*- coding: utf-8 -*-
"""Generador de mockups FinMind. Las pantallas se derivan de tokens.py."""
import os
from tokens import TOKENS as T, FONT, TYPO, RADIUS, MARCA_GOOGLE

OUT = os.environ.get("OUT", ".")
S = T["color.surface"]; C = T["color.canvas"]
N9, N7, N5, N3, N2, N1 = (T[f"color.neutral.{k}"] for k in (900, 700, 500, 300, 200, 100))
P7, P6, P5, P1 = T["color.primary.700"], T["color.primary.600"], T["color.primary.500"], T["color.primary.100"]
OK6, OK1 = T["color.success.600"], T["color.success.100"]
WA6, WA1 = T["color.warning.600"], T["color.warning.100"]
ER6, ER1 = T["color.error.600"], T["color.error.100"]
# Texto sobre un relleno de acento, y velo de los modales. En la paleta
# clara el primero era blanco; sobre teal brillante tiene que ser oscuro.
SOBRE, VELO = T["color.sobre-lleno"], T["color.velo"]
# Barra lateral y lateral de identidad. Salen de app.css, no de una
# suposicion: las dos superficies usan --color-barra.
BARRA, TXTBARRA, N4 = T["color.barra"], T["color.texto-barra"], T["color.neutral.400"]
RS, RM, RL = RADIUS["radius.sm"], RADIUS["radius.md"], RADIUS["radius.lg"]


def esc(s):
    return (s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"))


def rect(x, y, w, h, fill, rx=0, stroke=None, sw=1, dash=None):
    st = f' stroke="{stroke}" stroke-width="{sw}"' if stroke else ""
    da = f' stroke-dasharray="{dash}"' if dash else ""
    return f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{rx}" fill="{fill}"{st}{da}/>'


def txt(x, y, s, token="font.body.md", fill=None, anchor="start", size=None, weight=None):
    fs, fw = TYPO[token]
    fs = size or fs; fw = weight or fw
    return (f'<text x="{x}" y="{y}" font-family="{FONT}" font-size="{fs}" font-weight="{fw}" '
            f'fill="{fill or N9}" text-anchor="{anchor}">{esc(s)}</text>')


def boton(x, y, w, h, label, kind="primary"):
    if kind == "primary":
        f, tc, st = P6, S, None
    elif kind == "secondary":
        f, tc, st = S, N7, N3
    else:
        f, tc, st = S, ER6, ER6
    o = [rect(x, y, w, h, f, RM, st)]
    o.append(txt(x + w / 2, y + h / 2 + 5, label, "font.body.md", tc, "middle", weight=600))
    return "".join(o)


def campo(x, y, w, label, valor="", ph=False, error=None, foco=False):
    o = [txt(x, y, label, "font.label", N7)]
    by = y + 10
    borde = ER6 if error else (P6 if foco else N3)
    sw = 2 if (error or foco) else 1
    o.append(rect(x, by, w, 40, S, RM, borde, sw))
    if foco:
        o.append(rect(x - 3, by - 3, w + 6, 46, "none", RM + 2, P1, 3))
    o.append(txt(x + 12, by + 25, valor, "font.body.md", N5 if ph else N9))
    if error:
        o.append(txt(x, by + 58, "! " + error, "font.caption", ER6, weight=600))
    return "".join(o)


def separador(x, y, w, palabra="o"):
    """Linea con una palabra al medio, entre el formulario y el acceso externo."""
    mitad = w / 2
    return "".join([
        rect(x, y, mitad - 18, 1, N3),
        rect(x + mitad + 18, y, mitad - 18, 1, N3),
        txt(x + mitad, y + 5, palabra, "font.caption", N5, "middle"),
    ])


def boton_google(x, y, w, label="Continuar con Google"):
    """Fondo blanco y borde: es la presentacion que exigen las condiciones de marca.

    El fondo va explicitamente en blanco y NO en el token de superficie. Antes
    usaba S, y al pasar la paleta a oscuro S se volvio #151A23: el texto
    #1F1F1F de Google quedaba negro sobre negro, invisible. Google admite una
    variante oscura de su boton, pero el blanco resalta mejor sobre este lienzo
    y es igual de valido segun sus condiciones.
    """
    return "".join([
        rect(x, y, w, 44, MARCA_GOOGLE["fondo"], RM, N3),
        txt(x + 34, y + 29, "G", "font.heading.md", MARCA_GOOGLE["azul"], "middle", weight=700),
        txt(x + w / 2 + 16, y + 28, label, "font.body.md", MARCA_GOOGLE["texto"], "middle", weight=600),
    ])


def captcha(x, y, w):
    """RF-031. El widget solo obtiene un token; quien lo valida es el servidor."""
    return "".join([
        rect(x, y, w, 62, N1, RM, N3),
        rect(x + 14, y + 21, 20, 20, S, RS, N5, 2),
        txt(x + 20, y + 36, "v", "font.label", OK6, weight=700),
        txt(x + 48, y + 30, "No soy un robot", "font.body.md", N9),
        txt(x + 48, y + 46, "Verificacion de seguridad", "font.caption", N5),
        txt(x + w - 16, y + 40, "Turnstile", "font.caption", N5, "end"),
    ])


def casillas_codigo(x, y, digitos="", n=6, ancho=52, sep=12):
    """Las seis casillas del codigo de verificacion (UI-010, UI-012)."""
    o = []
    for i in range(n):
        cx = x + i * (ancho + sep)
        lleno = i < len(digitos)
        o.append(rect(cx, y, ancho, 64, S, RM, P6 if lleno else N3, 2 if lleno else 1))
        if lleno:
            o.append(txt(cx + ancho / 2, y + 42, digitos[i], "font.heading.lg", N9, "middle"))
    return "".join(o)


def aviso(x, y, w, titulo, cuerpo, tono="info"):
    """Franja de aviso. El color nunca es el unico indicador: siempre hay texto."""
    fondo, borde = {"info": (P1, P6), "ok": (OK1, OK6),
                    "warn": (WA1, WA6), "error": (ER1, ER6)}[tono]
    o = [rect(x, y, w, 56, fondo, RM, borde), rect(x, y, 4, 56, borde)]
    o.append(txt(x + 18, y + 24, titulo, "font.body.md", borde, weight=700))
    o.append(txt(x + 18, y + 42, cuerpo, "font.caption", N7))
    return "".join(o)


# Refleja la navegacion real de Layout.jsx, en su mismo orden: primero lo que se
# usa a diario, despues lo que se configura una vez. Si alli cambia, aqui tambien.
# El menu, en el mismo orden que Layout.jsx. Se compara con SECCIONES de ese
# archivo, no con la memoria: primero lo que se usa a diario y despues lo que se
# configura una vez.
#
# DOS CORRECCIONES DEL 08/09/2026:
#   - "Obligaciones" pasa a "Creditos y prestamos". El modulo se renombro y el
#     menu de los mockups se quedo con el nombre viejo, asi que las doce
#     pantallas mostraban una opcion que en la aplicacion no existe.
#   - Faltaba "Gastos fijos", que es un modulo entero (RF-046). No estaba en el
#     menu de ninguna pantalla.
NAV = [("Inicio", "UI-003"), ("Movimientos", "UI-004"), ("Presupuestos", "UI-006"),
       ("Gastos fijos", "UI-016"), ("Creditos y prestamos", "UI-014"),
       ("Metas", "UI-007"), ("Cuentas", "UI-008"), ("Categorias", "UI-015")]


def shell(titulo, activo, ancho=1280, alto=800):
    """Barra lateral y cabecera, calcadas de app.css.

    LO QUE ESTABA MAL: la barra se dibujaba con N9 y su texto con S. En la
    paleta clara eso daba un navy oscuro con letras blancas. Al invertir a
    oscuro, N9 paso a ser el color del TEXTO —claro— y S una superficie
    oscura, asi que la barra salio casi blanca con letras negras encima de
    otro negro. Es el riesgo de usar un token por el color que tiene hoy en
    vez de por lo que significa.

    Ahora sale de --color-barra, que es lo que usa el CSS de verdad, y el texto
    de --color-texto-barra. El destino activo lleva relleno teal con texto
    oscuro (--color-sobre-lleno), igual que .navegacion__item--activo.
    """
    o = [rect(0, 0, ancho, alto, C)]
    o.append(rect(0, 0, 240, alto, BARRA))
    o.append(rect(24, 28, 26, 26, P5, RS))
    # El logo de la aplicacion es una linea de cotizacion sobre barras de
    # volumen (CHG-UX-009). Aqui se dibuja simplificado: a este tamano las dos
    # capas se empastan, y el mockup no es el sitio para reproducir el detalle.
    o.append(f'<polyline points="28,46 33,40 37,44 42,34" fill="none" '
             f'stroke="{SOBRE}" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>')
    o.append(txt(60, 47, "FinMind", "font.heading.md", N9))
    y = 96
    for nombre, _ in NAV:
        act = nombre == activo
        if act:
            o.append(rect(12, y - 20, 216, 38, P7, RM))
        o.append(txt(28, y + 5, nombre, "font.body.md", SOBRE if act else TXTBARRA,
                     weight=600 if act else 400))
        y += 46
    o.append(rect(12, alto - 72, 216, 1, N4))
    o.append(rect(24, alto - 52, 28, 28, P5, 14))
    o.append(txt(38, alto - 33, "KZ", "font.caption", SOBRE, "middle", weight=700))
    o.append(txt(62, alto - 38, "Kevin Zapata", "font.caption", N9, weight=600))
    o.append(txt(62, alto - 24, "Cerrar sesion", "font.caption", TXTBARRA))
    o.append(rect(240, 0, ancho - 240, 68, BARRA))
    o.append(rect(240, 68, ancho - 240, 1, N4))
    o.append(txt(272, 42, titulo, "font.heading.lg"))
    return o


def svg(w, h, cuerpo, titulo, desc):
    return (f'<svg viewBox="0 0 {w} {h}" width="{w}" height="{h}" xmlns="http://www.w3.org/2000/svg" '
            f'role="img" aria-labelledby="t d"><title id="t">{esc(titulo)}</title>'
            f'<desc id="d">{esc(desc)}</desc>{"".join(cuerpo)}</svg>')


# Fecha de generacion de los mockups. Se actualiza al regenerar.
FECHA = "2026-09-08"


def pie(o, w, h, codigo, version="1.0"):
    o.append(txt(w - 20, h - 14, f"{codigo}  v{version}  FinMind  {FECHA}",
                 "font.caption", N5, "end"))


def guardar(nombre, contenido):
    with open(os.path.join(OUT, nombre), "w", encoding="utf-8") as f:
        f.write(contenido)
    print("  generado:", nombre)
