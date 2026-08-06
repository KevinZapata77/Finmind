# -*- coding: utf-8 -*-
from gen import *
MW, MH = 390, 844


def _mshell(o, titulo):
    o.append(rect(0, 0, MW, MH, C))
    o.append(rect(0, 0, MW, 96, S))
    o.append(rect(0, 96, MW, 1, N2))
    o.append(txt(20, 44, "9:41", "font.caption", N7, weight=600))
    o.append(txt(20, 80, titulo, "font.heading.lg"))
    o.append(rect(MW - 56, 54, 34, 34, P5, 17))
    o.append(txt(MW - 39, 76, "KZ", "font.caption", S, "middle", weight=700))
    # barra inferior: destinos tactiles de 56 px, separados
    o.append(rect(0, MH - 76, MW, 76, S))
    o.append(rect(0, MH - 76, MW, 1, N2))
    tabs = [("Panel", True), ("Movim.", False), ("Presup.", False), ("Mas", False)]
    for i, (t_, act) in enumerate(tabs):
        cx = 48 + i * 98
        if act:
            o.append(rect(cx - 34, MH - 66, 68, 34, P1, RM))
        o.append(txt(cx, MH - 44, t_, "font.caption", P7 if act else N5, "middle",
                     weight=700 if act else 400))


def m003():
    o = []
    _mshell(o, "Panel")
    o.append(rect(20, 116, 350, 104, P7, RL))
    o.append(txt(40, 148, "BALANCE DE AGOSTO", "font.label", "#DCF2EC"))
    o.append(txt(40, 186, "$ 1.240.000", "font.numero.lg", S))
    o.append(txt(40, 208, "+8% frente a julio", "font.caption", "#DCF2EC"))
    for t_, v, col, x in [("Ingresos", "$ 3.100.000", OK6, 20), ("Gastos", "$ 1.860.000", ER6, 202)]:
        o.append(rect(x, 236, 168, 82, S, RL, N2))
        o.append(txt(x + 16, 264, t_, "font.label", N5))
        o.append(txt(x + 16, 296, v, "font.body.md", col, weight=700))
    o.append(rect(20, 334, 350, 214, S, RL, N2))
    o.append(txt(40, 366, "Gastos por categoria", "font.heading.md"))
    datos = [("Vivienda", 620, P6), ("Alimentacion", 480, P5), ("Transporte", 310, OK6), ("Servicios", 240, WA6)]
    y = 396
    for nombre, valor, color in datos:
        o.append(txt(40, y + 10, nombre, "font.caption", N7))
        o.append(rect(150, y, 160, 12, N1, 6))
        o.append(rect(150, y, int(valor / 620 * 160), 12, color, 6))
        o.append(txt(350, y + 10, f"$ {valor}.000", "font.caption", N9, "end", weight=600))
        y += 34
    o.append(rect(20, 564, 350, 68, WA1, RL, WA6))
    o.append(txt(40, 592, "! Presupuesto excedido", "font.body.md", WA6, weight=700))
    o.append(txt(40, 614, "Entretenimiento va en 104% del limite.", "font.caption", WA6))
    o.append(rect(20, 648, 350, 48, P6, RM))
    o.append(txt(195, 678, "+ Nuevo movimiento", "font.body.md", S, "middle", weight=600))
    pie(o, MW, MH, "MK-003m", "0.5")
    return svg(MW, MH, o, "UI-003 Panel, version movil",
               "Panel adaptado a movil: balance destacado, tarjetas de ingresos y gastos en dos columnas, gastos por categoria, alerta de presupuesto y navegacion inferior de cuatro destinos.")


def m005():
    o = []
    _mshell(o, "Nuevo movimiento")
    o.append(txt(20, 132, "Tipo *", "font.label", N7))
    o.append(rect(20, 142, 170, 44, P1, RM, P6, 2))
    o.append(txt(105, 170, "Gasto", "font.body.md", P7, "middle", weight=700))
    o.append(rect(200, 142, 170, 44, S, RM, N3))
    o.append(txt(285, 170, "Ingreso", "font.body.md", N7, "middle"))
    o.append(campo(20, 214, 350, "Monto *", "$ 320.000", foco=True))
    o.append(campo(20, 306, 350, "Fecha *", "05/08/2026"))
    o.append(campo(20, 392, 350, "Categoria *", "Alimentacion"))
    o.append(campo(20, 478, 350, "Cuenta *", "Nequi"))
    o.append(campo(20, 564, 350, "Descripcion", "Mercado del mes", ph=True))
    o.append(rect(20, 650, 350, 44, P1, RM))
    o.append(txt(36, 670, "Presupuesto de Alimentacion", "font.caption", P7, weight=700))
    o.append(txt(36, 686, "Llegas al 80% de $ 1.000.000", "font.caption", P7))
    o.append(rect(20, 712, 350, 48, P6, RM))
    o.append(txt(195, 742, "Guardar movimiento", "font.body.md", S, "middle", weight=600))
    pie(o, MW, MH, "MK-005m", "0.5")
    return svg(MW, MH, o, "UI-005 Registrar movimiento, version movil",
               "Formulario de registro en una sola columna con campos apilados, controles de 44 pixeles de alto y accion principal fija al final.")


def sistema():
    W2, H2 = 1280, 900
    o = [rect(0, 0, W2, H2, S)]
    o.append(txt(48, 64, "FinMind - Sistema de diseno", "font.display"))
    o.append(txt(48, 92, "Todos los mockups se generan a partir de estos tokens. v0.5 - 2026-08-05", "font.body.md", N5))
    o.append(txt(48, 148, "Paleta y contraste verificado (WCAG AA 4.5:1)", "font.heading.md"))
    paleta = [("primary.700", P7, "8.62"), ("primary.600", P6, "6.45"), ("primary.500", P5, "4.62"),
              ("primary.100", P1, "-"), ("success.600", OK6, "5.02"), ("warning.600", WA6, "5.02"),
              ("error.600", ER6, "6.47"), ("neutral.900", N9, "17.74"), ("neutral.700", N7, "10.31"),
              ("neutral.500", N5, "4.83"), ("neutral.300", N3, "-"), ("canvas", C, "-")]
    x, y = 48, 174
    for nombre, color, ratio in paleta:
        o.append(rect(x, y, 88, 64, color, RM, N2))
        o.append(txt(x, y + 82, nombre, "font.caption", N7, weight=600))
        o.append(txt(x, y + 98, f"{color}  {ratio}", "font.caption", N5))
        x += 100
    o.append(txt(48, 350, "Tipografia", "font.heading.md"))
    esc_t = [("font.display", "font.display  28/700  Titulo de portada"),
             ("font.heading.lg", "font.heading.lg  22/700  Titulo de pagina"),
             ("font.heading.md", "font.heading.md  17/600  Seccion"),
             ("font.body.md", "font.body.md  14/400  Texto general"),
             ("font.label", "font.label  12/600  Etiqueta de campo"),
             ("font.caption", "font.caption  11/400  Ayuda secundaria")]
    y = 384
    for token, muestra in esc_t:
        o.append(txt(48, y, muestra, token))
        y += 42
    o.append(txt(560, 350, "Botones y estados", "font.heading.md"))
    o.append(boton(560, 376, 150, 44, "Primario"))
    o.append(boton(724, 376, 150, 44, "Secundario", "secondary"))
    o.append(boton(888, 376, 150, 44, "Peligro", "danger"))
    o.append(rect(560, 436, 150, 44, N2, RM))
    o.append(txt(635, 464, "Deshabilitado", "font.body.md", N5, "middle", weight=600))
    o.append(rect(724, 436, 150, 44, P6, RM))
    o.append(txt(799, 464, "Cargando...", "font.body.md", S, "middle", weight=600))
    o.append(rect(885, 433, 156, 50, "none", RM + 2, P1, 3))
    o.append(boton(888, 436, 150, 44, "Con foco"))
    o.append(txt(560, 528, "Campos", "font.heading.md"))
    o.append(campo(560, 554, 220, "Normal", "Texto"))
    o.append(campo(818, 554, 220, "Con foco", "Texto", foco=True))
    o.append(campo(560, 646, 480, "Con error", "correo-invalido",
                   error="Escribe un correo con formato valido, por ejemplo nombre@dominio.com"))
    o.append(txt(48, 646, "Espaciado y radios", "font.heading.md"))
    o.append(txt(48, 676, "Unidad base 8 px. Escala 8 / 16 / 24 / 32 / 48.", "font.body.md", N7))
    o.append(txt(48, 700, "Radios: sm 4, md 8, lg 12. Contenedor maximo 1280 px.", "font.body.md", N7))
    o.append(txt(48, 724, "Breakpoints: movil <768, tableta 768-1023, escritorio >=1024.", "font.body.md", N7))
    o.append(txt(48, 780, "Estados semanticos: nunca solo color", "font.heading.md"))
    for etq, col, bg, x2 in [("OK En rango", OK6, OK1, 48), ("~ Alerta", WA6, WA1, 200), ("! Excedido", ER6, ER1, 340)]:
        o.append(rect(x2, 800, 130, 30, bg, 15))
        o.append(txt(x2 + 65, 820, etq, "font.caption", col, "middle", weight=700))
    o.append(txt(48, 862, "Cada estado lleva icono y texto ademas del color, para cumplir UXA-03.", "font.body.md", N5))
    pie(o, W2, H2, "Sistema de diseno")
    return svg(W2, H2, o, "Sistema de diseno FinMind",
               "Hoja del sistema de diseno con paleta y ratios de contraste, escala tipografica, botones en todos sus estados, campos normal, con foco y con error, espaciado, breakpoints y estados semanticos con icono y texto.")
