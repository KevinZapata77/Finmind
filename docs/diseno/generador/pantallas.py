# -*- coding: utf-8 -*-
from gen import *

W, H = 1280, 800

# ------------------------------------------------- UI-001 Iniciar sesion
def ui001():
    o = [rect(0, 0, W, H, C)]
    o.append(rect(0, 0, 520, H, BARRA))
    o.append(rect(64, 72, 34, 34, P5, RS))
    o.append(txt(81, 96, "F", "font.heading.lg", SOBRE, "middle"))
    o.append(txt(110, 98, "FinMind", "font.heading.lg", N9))
    o.append(txt(64, 330, "Tus finanzas,", "font.display", N9, size=34))
    o.append(txt(64, 374, "en orden.", "font.display", N9, size=34))
    o.append(txt(64, 416, "Registra ingresos y gastos, define presupuestos", "font.body.md", N7))
    o.append(txt(64, 440, "y mira a donde se va tu dinero.", "font.body.md", N7))
    x = 660
    o.append(rect(x, 120, 460, 560, S, RL, N2))
    o.append(txt(x + 40, 176, "Iniciar sesion", "font.heading.lg"))
    o.append(txt(x + 40, 202, "Ingresa con tu correo y contrasena.", "font.body.md", N5))
    o.append(campo(x + 40, 246, 380, "Correo electronico", "kevin@ejemplo.com", ph=True))
    o.append(campo(x + 40, 332, 380, "Contrasena", "........", ph=True))
    o.append(txt(x + 420, 332, "Mostrar", "font.caption", P6, "end", weight=600))
    o.append(boton(x + 40, 426, 380, 44, "Iniciar sesion"))
    o.append(txt(x + 420, 494, "Olvidaste tu contrasena?", "font.caption", P6, "end", weight=600))
    # RF-029: acceso con Google
    o.append(separador(x + 40, 524, 380))
    o.append(boton_google(x + 40, 548, 380, "Entrar con Google"))
    o.append(txt(x + 230, 634, "No tienes cuenta?  Crear cuenta", "font.body.md", P6, "middle", weight=600))
    pie(o, W, H, "MK-001 / UI-001")
    return svg(W, H, o, "UI-001 Iniciar sesion",
               "Pantalla de inicio de sesion con campos de correo y contrasena, boton principal y enlace a registro.")

# ------------------------------------- UI-001-E estado de error de validacion
def ui001e():
    o = [rect(0, 0, W, H, C)]
    o.append(rect(0, 0, 520, H, BARRA))
    o.append(rect(64, 72, 34, 34, P5, RS))
    o.append(txt(81, 96, "F", "font.heading.lg", SOBRE, "middle"))
    o.append(txt(110, 98, "FinMind", "font.heading.lg", N9))
    o.append(txt(64, 350, "Tus finanzas,", "font.display", N9, size=34))
    o.append(txt(64, 394, "en orden.", "font.display", N9, size=34))
    x = 660
    o.append(rect(x, 150, 460, 500, S, RL, N2))
    o.append(txt(x + 40, 206, "Iniciar sesion", "font.heading.lg"))
    o.append(rect(x + 40, 232, 380, 52, ER1, RM, ER6))
    o.append(txt(x + 56, 254, "No pudimos iniciar sesion", "font.body.md", ER6, weight=700))
    o.append(txt(x + 56, 272, "Correo o contrasena incorrectos.", "font.caption", ER6))
    o.append(campo(x + 40, 320, 380, "Correo electronico", "kevin@ejemplo.com"))
    o.append(campo(x + 40, 412, 380, "Contrasena", "........",
                   error="Revisa tus datos e intenta de nuevo.", foco=True))
    o.append(boton(x + 40, 526, 380, 44, "Iniciar sesion"))
    o.append(txt(x + 230, 606, "Olvidaste tu contrasena?", "font.body.md", P6, "middle", weight=600))
    pie(o, W, H, "MK-001b / UI-001 estado error")
    return svg(W, H, o, "UI-001 Iniciar sesion, estado de error",
               "Mismo inicio de sesion mostrando alerta de credenciales invalidas, campo con borde de error, mensaje con icono y texto, y foco visible.")

# ------------------------------------------------- UI-002 Crear cuenta
def ui002():
    o = [rect(0, 0, W, H, C)]
    o.append(rect(0, 0, 520, H, BARRA))
    o.append(rect(64, 72, 34, 34, P5, RS))
    o.append(txt(81, 96, "F", "font.heading.lg", SOBRE, "middle"))
    o.append(txt(110, 98, "FinMind", "font.heading.lg", N9))
    o.append(txt(64, 340, "Empieza gratis.", "font.display", S, size=32))
    o.append(txt(64, 382, "Toma el control de tu dinero en", "font.body.md", N7))
    o.append(txt(64, 406, "menos de cinco minutos.", "font.body.md", N7))
    x = 640
    o.append(rect(x, 40, 500, 720, S, RL, N2))
    o.append(txt(x + 40, 96, "Crear cuenta", "font.heading.lg"))
    o.append(txt(x + 40, 122, "Todos los campos son obligatorios.", "font.body.md", N5))
    o.append(campo(x + 40, 162, 200, "Nombre", "Kevin", ph=True))
    o.append(campo(x + 260, 162, 200, "Apellido", "Zapata", ph=True))
    o.append(campo(x + 40, 248, 420, "Correo electronico", "kevin@ejemplo.com", ph=True))
    o.append(campo(x + 40, 334, 420, "Contrasena", "Minimo 8 caracteres", ph=True))
    o.append(rect(x + 40, 410, 420, 44, N1, RM))
    o.append(txt(x + 54, 430, "Fortaleza de la contrasena", "font.caption", N7, weight=600))
    o.append(rect(x + 54, 438, 260, 6, N3, 3))
    o.append(rect(x + 54, 438, 174, 6, OK6, 3))
    o.append(txt(x + 446, 437, "Buena", "font.caption", OK6, "end", weight=600))
    o.append(rect(x + 40, 476, 16, 16, S, RS, N3))
    o.append(txt(x + 66, 489, "Acepto el tratamiento de mis datos personales.", "font.body.md", N7))
    # RF-031: el desafio va antes del boton, y el boton sigue inactivo hasta resolverlo
    o.append(captcha(x + 40, 512, 420))
    o.append(boton(x + 40, 592, 420, 44, "Crear cuenta"))
    # RF-030: registro con Google
    o.append(separador(x + 40, 662, 420))
    o.append(boton_google(x + 40, 686, 420, "Registrarme con Google"))
    o.append(txt(x + 250, 748, "Ya tienes cuenta?  Iniciar sesion", "font.body.md", P6, "middle", weight=600))
    pie(o, W, H, "MK-002 / UI-002")
    return svg(W, H, o, "UI-002 Crear cuenta",
               "Formulario de registro con nombre, apellido, correo, contrasena, indicador de fortaleza y consentimiento de datos.")

def _glifo(cx, cy, clase, tono):
    """Forma simple dentro del chip de una metrica.

    0 billete, 1 flecha que entra, 2 flecha que sale, 3 triangulo de aviso.
    Se dibujan a mano porque a 26 px lo que se distingue es la silueta, no el
    detalle del icono real.
    """
    if clase == 0:
        return ('<rect x="%d" y="%d" width="13" height="9" rx="2" fill="none" '
                'stroke="%s" stroke-width="1.6"/>' % (cx - 6.5, cy - 4.5, tono))
    if clase == 1:
        return ('<polyline points="%d,%d %d,%d %d,%d" fill="none" stroke="%s" '
                'stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>'
                % (cx + 4, cy - 4, cx - 4, cy + 4, cx + 2, cy + 4, tono))
    if clase == 2:
        return ('<polyline points="%d,%d %d,%d %d,%d" fill="none" stroke="%s" '
                'stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>'
                % (cx - 4, cy + 4, cx + 4, cy - 4, cx - 2, cy - 4, tono))
    return ('<polygon points="%d,%d %d,%d %d,%d" fill="none" stroke="%s" '
            'stroke-width="1.6" stroke-linejoin="round"/>'
            % (cx, cy - 5, cx + 5, cy + 4, cx - 5, cy + 4, tono))


# ------------------------------------------------- UI-003 Inicio
def ui003():
    """UI-003 Inicio, rehecha por CHG-UX-008, CHG-UX-010 y CHG-UX-011.

    POR QUE SE REHIZO ENTERA Y NO SE REPINTO
    La version anterior mostraba la composicion del gasto en barras a la
    izquierda y el patrimonio a la derecha. La aplicacion ya no se ve asi: el
    panel pasa a dos columnas, con cuatro tarjetas de metrica de borde
    semantico, la curva del mes junto a la dona, y los avisos en un riel a la
    derecha. Repintarla de oscuro habria dado una figura correcta de color y
    falsa de contenido, que es peor que una figura vieja: parece actual.

    POR QUE 1440x900 Y NO 1280x800 COMO LAS DEMAS
    A 1280 el area de contenido queda en 976 px y, con el riel de 300, la
    columna principal baja a 652. Cuatro tarjetas ahi miden 154 px y una cifra
    como $ 1.860.000 no entra. Se ensancha el lienzo en vez de encoger la
    tipografia: una figura ilegible no documenta nada.
    """
    A, B = 1440, 900
    o = shell("Inicio", "Inicio", ancho=A, alto=B)
    X, ANCHO = 272, 1136
    COL = 812                      # columna principal
    RX, RW = X + COL + 24, 300     # riel de avisos

    # --- RF-040: registro rapido. Lo primero es anotar, no consultar --------
    o.append(rect(X, 96, ANCHO, 96, S, RL, P6, sw=2))
    o.append(rect(X + 24, 116, 84, 36, OK1, RM, OK6, sw=2))
    o.append(txt(X + 66, 139, "Entro", "font.body.md", OK6, "middle", weight=600))
    o.append(rect(X + 116, 116, 84, 36, N1, RM, N2, sw=2))
    o.append(txt(X + 158, 139, "Salio", "font.body.md", N7, "middle", weight=600))
    o.append(rect(X + 212, 116, 340, 36, S, RM, N3))
    o.append(txt(X + 226, 139, "Cuanto?", "font.body.md", N5))
    o.append(rect(X + 564, 116, 300, 36, S, RM, N3))
    o.append(txt(X + 578, 139, "Ventas", "font.body.md", N9))
    o.append(txt(X + 848, 140, "v", "font.caption", N5))
    o.append(boton(X + 880, 116, 128, 36, "Anotar"))
    x = X + 24
    for etiqueta in ("$ 5.000", "$ 10.000", "$ 20.000", "$ 50.000", "$ 100.000"):
        w = len(etiqueta) * 7 + 16
        o.append(rect(x, 160, w, 22, P1, RS))
        o.append(txt(x + w / 2, 175, etiqueta, "font.caption", P7, "middle"))
        x += w + 8
    o.append(txt(X + ANCHO - 12, 175, "+ Nueva categoria", "font.caption", P7, "end", weight=600))

    # --- Tira compacta: hoy, la semana, el mes -----------------------------
    o.append(rect(X, 208, ANCHO, 70, N1, RM))
    for k, (rot, neto, det, col) in enumerate([
            ("Hoy", "$ 92.000", "+$ 120.000 y -$ 28.000", OK6),
            ("Esta semana", "$ 418.000", "+$ 610.000 y -$ 192.000", N9),
            ("Este mes", "$ 1.240.000", "+$ 3.100.000 y -$ 1.860.000", N9)]):
        cx = X + 24 + k * 378
        o.append(txt(cx, 232, rot, "font.label", N7))
        o.append(txt(cx, 256, neto, "font.heading.md", col))
        o.append(txt(cx, 272, det, "font.caption", N5))

    # --- RF-050: la comparacion contra el mes pasado -----------------------
    o.append(rect(X, 294, ANCHO, 54, WA1, RM, WA6))
    o.append(txt(X + 22, 320, "^", "font.heading.md", WA6, weight=700))
    o.append(txt(X + 46, 318, "Vas gastando $ 100.000 mas que a esta altura del mes pasado.",
                 "font.body.md", N9, weight=600))
    o.append(txt(X + 46, 337, "$ 460.000 en los primeros 8 dias de este mes, contra $ 360.000 "
                 "en los mismos dias del anterior.", "font.caption", N7))

    # --- Cabecera: el saludo situa a la persona en un segundo --------------
    o.append(txt(X, 386, "Hola, Kevin", "font.heading.lg"))
    o.append(txt(X, 408, "Asi va tu septiembre", "font.caption", N7))
    o.append(rect(X + COL - 250, 372, 120, 38, S, RM, N3))
    o.append(txt(X + COL - 236, 396, "Septiembre", "font.body.md", N9))
    o.append(rect(X + COL - 118, 372, 118, 38, S, RM, N3))
    o.append(txt(X + COL - 104, 396, "2026", "font.body.md", N9))

    # --- CHG-UX-011: cuatro tarjetas del mismo tamano ----------------------
    # La jerarquia la da el BORDE SEMANTICO, no el tamano: cada tarjeta queda
    # clasificada por color antes de leerse. "Te queda" va primera y su cifra
    # un punto mas grande.
    tarjetas = [
        ("Te queda", "$ 350.000",   "Quedan 22 dias",      P6,  P1,  P7,  25),
        ("Entro",    "$ 3.100.000", "Ver los movimientos", OK6, OK1, OK6, 20),
        ("Salio",    "$ 1.860.000", "+22,9% vs. agosto",   ER6, ER1, ER6, 20),
        ("Alertas",  "3",           "1 urgente",           WA6, WA1, WA6, 20),
    ]
    for k, (rot, val, nota, borde, chip, tono, tam) in enumerate(tarjetas):
        cx = X + k * 206
        o.append(rect(cx, 424, 194, 112, S, RL, borde))
        o.append(rect(cx + 16, 440, 26, 26, chip, RS))
        # Un glifo dentro del chip. Sin esto queda un cuadro de color vacio,
        # que en una figura de entrega se lee como algo a medio hacer. No es
        # el icono de lucide: es su forma, que es lo que se distingue a 26 px.
        o.append(_glifo(cx + 29, 453, k, tono))
        o.append(txt(cx + 16, 490, rot, "font.label", N7))
        o.append(txt(cx + 16, 514, val, "font.numero.lg", tono, size=tam))
        o.append(txt(cx + 16, 529, nota, "font.caption", N5))

    # --- La lectura del servidor y el patrimonio, como linea de apoyo ------
    o.append(txt(X, 560, "Te entraron $ 3.100.000 y llevas gastados $ 1.860.000. "
                 "Patrimonio $ 1.620.000: $ 4.235.000 en cuentas menos $ 2.615.000 en deudas.",
                 "font.caption", N7))

    # --- CHG-UX-010: la curva y la dona, lado a lado -----------------------
    o.append(rect(X, 580, 480, 220, S, RL, N2))
    o.append(txt(X + 20, 610, "Como se acumulo este mes", "font.heading.md"))
    o.append(txt(X + 460, 610, "Dia 8 de 30", "font.caption", N5, "end"))
    for gy in (652, 690, 728, 766):
        o.append(rect(X + 20, gy, 440, 1, N2))
    puntos = [(0, 766), (55, 752), (110, 730), (165, 736), (220, 706),
              (275, 712), (330, 682), (385, 674), (440, 652)]
    ruta = " ".join("%d,%d" % (X + 20 + px, py) for px, py in puntos)
    o.append('<polygon points="%s %d,%d %d,%d" fill="%s" opacity="0.26"/>'
             % (ruta, X + 460, 776, X + 20, 776, P7))
    o.append('<polyline points="%s" fill="none" stroke="%s" stroke-width="2.5" '
             'stroke-linejoin="round"/>' % (ruta, P7))
    o.append('<polyline points="%d,682 %d,666 %d,646" fill="none" stroke="%s" '
             'stroke-width="2" stroke-dasharray="5 4"/>' % (X + 350, X + 405, X + 460, WA6))
    o.append(rect(X + 20, 789, 14, 2, P7))
    o.append(txt(X + 40, 793, "gasto acumulado", "font.caption", N7))
    o.append(rect(X + 156, 789, 14, 2, WA6))
    o.append(txt(X + 176, 793, "proyeccion de cierre", "font.caption", N7))

    o.append(rect(X + 496, 580, 316, 220, S, RL, N2))
    o.append(txt(X + 516, 610, "En que se fue", "font.heading.md"))
    # Dona: los tramos son arcos sobre un mismo circulo con stroke-dasharray,
    # igual que el componente Dona.jsx. Sin libreria de graficos.
    # La dona se encoge y se corre a la izquierda para dejarle sitio a la
    # leyenda: con r=44 y la leyenda empezando en X+646, "Alimentacion" se
    # solapaba con su monto. Se vio al mirar el PNG, no al leer el codigo.
    cx, cy, r, gw = X + 556, 700, 38, 15
    circ = 2 * 3.14159 * r
    segmentos = [(0.34, P7), (0.26, "#A78BFA"), (0.20, "#F472B6"), (0.20, WA6)]
    o.append('<circle cx="%d" cy="%d" r="%d" fill="none" stroke="%s" stroke-width="%d"/>'
             % (cx, cy, r, N2, gw))
    off = 0.0
    for frac, color in segmentos:
        largo = circ * frac
        o.append('<circle cx="%d" cy="%d" r="%d" fill="none" stroke="%s" stroke-width="%d" '
                 'stroke-dasharray="%.1f %.1f" stroke-dashoffset="%.1f" '
                 'transform="rotate(-90 %d %d)"/>'
                 % (cx, cy, r, color, gw, largo, circ - largo, -circ * off, cx, cy))
        off += frac
    ly = 662
    for nombre, monto, color in [("Alimentacion", "$ 620.000", P7),
                                 ("Vivienda", "$ 480.000", "#A78BFA"),
                                 ("Transporte", "$ 370.000", "#F472B6"),
                                 ("Otros", "$ 390.000", WA6)]:
        o.append(rect(X + 610, ly - 8, 9, 9, color, 4))
        o.append(txt(X + 626, ly, nombre, "font.caption", N9))
        o.append(txt(X + 796, ly, monto, "font.caption", N9, "end", weight=600))
        ly += 24

    # --- CHG-UX-010: el riel de avisos, en columna propia ------------------
    o.append(txt(RX, 386, "Tus alertas", "font.heading.md"))
    o.append(txt(RX, 408, "Dia 8 de 30", "font.caption", N5))
    avisos = [
        ("URGENTE", "No te alcanza para el mes",
         "Te quedan $ 350.000 y debes", "$ 530.000 en compromisos.", ER6, ER1),
        ("ATENCION", "Vas a pasarte en Alimentacion",
         "Llevas $ 360.000 de $ 500.000.", "Terminarias en $ 560.000.", WA6, WA1),
        ("AL DIA", "Transporte va bien",
         "8 puntos por debajo de tu", "ritmo del mes.", P7, P1),
    ]
    ay = 424
    for sev, titulo, l1, l2, tono, chip in avisos:
        o.append(rect(RX, ay, RW, 104, S, RM, tono))
        o.append(rect(RX + 14, ay + 16, 24, 24, chip, RS))
        o.append(_glifo(RX + 26, ay + 28, 3 if sev != "AL DIA" else 0, tono))
        o.append(txt(RX + 48, ay + 32, sev, "font.caption", tono, weight=700))
        o.append(txt(RX + 14, ay + 62, titulo, "font.body.md", N9, weight=600))
        o.append(txt(RX + 14, ay + 80, l1, "font.caption", N7))
        o.append(txt(RX + 14, ay + 94, l2, "font.caption", N7))
        ay += 116

    pie(o, A, B, "MK-003 / UI-003")
    return svg(A, B, o, "UI-003 Inicio",
               "Panel de inicio: registro rapido, cuatro metricas con borde semantico, "
               "la curva del mes junto a la dona de categorias, y el riel de avisos a la derecha.")
