# -*- coding: utf-8 -*-
"""Generador de mockups FinMind. Las pantallas se derivan de tokens.py."""
import os
from tokens import TOKENS as T, FONT, TYPO, RADIUS

OUT = os.environ.get("OUT", ".")
S = T["color.surface"]; C = T["color.canvas"]
N9, N7, N5, N3, N2, N1 = (T[f"color.neutral.{k}"] for k in (900, 700, 500, 300, 200, 100))
P7, P6, P5, P1 = T["color.primary.700"], T["color.primary.600"], T["color.primary.500"], T["color.primary.100"]
OK6, OK1 = T["color.success.600"], T["color.success.100"]
WA6, WA1 = T["color.warning.600"], T["color.warning.100"]
ER6, ER1 = T["color.error.600"], T["color.error.100"]
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
    """Fondo blanco y borde: es la presentacion que exigen las condiciones de marca."""
    return "".join([
        rect(x, y, w, 44, S, RM, N3),
        txt(x + 34, y + 29, "G", "font.heading.md", "#4285F4", "middle", weight=700),
        txt(x + w / 2 + 16, y + 28, label, "font.body.md", "#1F1F1F", "middle", weight=600),
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


# Refleja la navegacion real de Layout.jsx. Si alli cambia, aqui tambien.
NAV = [("Panel", "UI-003"), ("Movimientos", "UI-004"), ("Presupuestos", "UI-006"),
       ("Cuentas", "UI-008"), ("Categorias", "UI-015"), ("Obligaciones", "UI-014"),
       ("Metas", "UI-007")]


def shell(titulo, activo, ancho=1280, alto=800):
    o = [rect(0, 0, ancho, alto, C)]
    o.append(rect(0, 0, 240, alto, N9))
    o.append(rect(24, 28, 26, 26, P5, RS))
    o.append(txt(37, 46, "F", "font.heading.md", S, "middle"))
    o.append(txt(60, 47, "FinMind", "font.heading.md", S))
    y = 96
    for nombre, _ in NAV:
        act = nombre == activo
        if act:
            o.append(rect(12, y - 20, 216, 38, P7, RM))
        o.append(txt(28, y + 5, nombre, "font.body.md", S if act else N3,
                     weight=600 if act else 400))
        y += 46
    o.append(rect(12, alto - 72, 216, 1, N7))
    o.append(rect(24, alto - 52, 28, 28, P5, 14))
    o.append(txt(38, alto - 33, "KZ", "font.caption", S, "middle", weight=700))
    o.append(txt(62, alto - 38, "Kevin Zapata", "font.caption", S, weight=600))
    o.append(txt(62, alto - 24, "Cerrar sesion", "font.caption", N3))
    o.append(rect(240, 0, ancho - 240, 68, S))
    o.append(rect(240, 68, ancho - 240, 1, N2))
    o.append(txt(272, 42, titulo, "font.heading.lg"))
    return o


def svg(w, h, cuerpo, titulo, desc):
    return (f'<svg viewBox="0 0 {w} {h}" width="{w}" height="{h}" xmlns="http://www.w3.org/2000/svg" '
            f'role="img" aria-labelledby="t d"><title id="t">{esc(titulo)}</title>'
            f'<desc id="d">{esc(desc)}</desc>{"".join(cuerpo)}</svg>')


FECHA = "2026-08-16"


def pie(o, w, h, codigo, version="1.0"):
    o.append(txt(w - 20, h - 14, f"{codigo}  v{version}  FinMind  {FECHA}",
                 "font.caption", N5, "end"))


def guardar(nombre, contenido):
    with open(os.path.join(OUT, nombre), "w", encoding="utf-8") as f:
        f.write(contenido)
    print("  generado:", nombre)
