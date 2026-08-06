# -*- coding: utf-8 -*-
from gen import *

W, H = 1280, 800

# ------------------------------------------------- UI-001 Iniciar sesion
def ui001():
    o = [rect(0, 0, W, H, C)]
    o.append(rect(0, 0, 520, H, P7))
    o.append(rect(64, 72, 34, 34, P5, RS))
    o.append(txt(81, 96, "F", "font.heading.lg", S, "middle"))
    o.append(txt(110, 98, "FinMind", "font.heading.lg", S))
    o.append(txt(64, 330, "Tus finanzas,", "font.display", S, size=34))
    o.append(txt(64, 374, "en orden.", "font.display", S, size=34))
    o.append(txt(64, 416, "Registra ingresos y gastos, define presupuestos", "font.body.md", P1))
    o.append(txt(64, 440, "y mira a donde se va tu dinero.", "font.body.md", P1))
    x = 660
    o.append(rect(x, 180, 460, 440, S, RL, N2))
    o.append(txt(x + 40, 236, "Iniciar sesion", "font.heading.lg"))
    o.append(txt(x + 40, 262, "Ingresa con tu correo y contrasena.", "font.body.md", N5))
    o.append(campo(x + 40, 306, 380, "Correo electronico", "kevin@ejemplo.com", ph=True))
    o.append(campo(x + 40, 392, 380, "Contrasena", "........", ph=True))
    o.append(txt(x + 420, 392, "Mostrar", "font.caption", P6, "end", weight=600))
    o.append(boton(x + 40, 486, 380, 44, "Iniciar sesion"))
    o.append(txt(x + 230, 566, "No tienes cuenta?  Crear cuenta", "font.body.md", P6, "middle", weight=600))
    pie(o, W, H, "MK-001 / UI-001")
    return svg(W, H, o, "UI-001 Iniciar sesion",
               "Pantalla de inicio de sesion con campos de correo y contrasena, boton principal y enlace a registro.")

# ------------------------------------- UI-001-E estado de error de validacion
def ui001e():
    o = [rect(0, 0, W, H, C)]
    o.append(rect(0, 0, 520, H, P7))
    o.append(rect(64, 72, 34, 34, P5, RS))
    o.append(txt(81, 96, "F", "font.heading.lg", S, "middle"))
    o.append(txt(110, 98, "FinMind", "font.heading.lg", S))
    o.append(txt(64, 350, "Tus finanzas,", "font.display", S, size=34))
    o.append(txt(64, 394, "en orden.", "font.display", S, size=34))
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
    o.append(rect(0, 0, 520, H, P7))
    o.append(rect(64, 72, 34, 34, P5, RS))
    o.append(txt(81, 96, "F", "font.heading.lg", S, "middle"))
    o.append(txt(110, 98, "FinMind", "font.heading.lg", S))
    o.append(txt(64, 340, "Empieza gratis.", "font.display", S, size=32))
    o.append(txt(64, 382, "Toma el control de tu dinero en", "font.body.md", P1))
    o.append(txt(64, 406, "menos de cinco minutos.", "font.body.md", P1))
    x = 640
    o.append(rect(x, 110, 500, 580, S, RL, N2))
    o.append(txt(x + 40, 166, "Crear cuenta", "font.heading.lg"))
    o.append(txt(x + 40, 192, "Todos los campos son obligatorios.", "font.body.md", N5))
    o.append(campo(x + 40, 232, 200, "Nombre", "Kevin", ph=True))
    o.append(campo(x + 260, 232, 200, "Apellido", "Zapata", ph=True))
    o.append(campo(x + 40, 318, 420, "Correo electronico", "kevin@ejemplo.com", ph=True))
    o.append(campo(x + 40, 404, 420, "Contrasena", "Minimo 8 caracteres", ph=True))
    o.append(rect(x + 40, 480, 420, 44, N1, RM))
    o.append(txt(x + 54, 500, "Fortaleza de la contrasena", "font.caption", N7, weight=600))
    o.append(rect(x + 54, 508, 260, 6, N3, 3))
    o.append(rect(x + 54, 508, 174, 6, OK6, 3))
    o.append(txt(x + 446, 507, "Buena", "font.caption", OK6, "end", weight=600))
    o.append(rect(x + 40, 546, 16, 16, S, RS, N3))
    o.append(txt(x + 66, 559, "Acepto el tratamiento de mis datos personales.", "font.body.md", N7))
    o.append(boton(x + 40, 590, 420, 44, "Crear cuenta"))
    o.append(txt(x + 250, 662, "Ya tienes cuenta?  Iniciar sesion", "font.body.md", P6, "middle", weight=600))
    pie(o, W, H, "MK-002 / UI-002")
    return svg(W, H, o, "UI-002 Crear cuenta",
               "Formulario de registro con nombre, apellido, correo, contrasena, indicador de fortaleza y consentimiento de datos.")

# ------------------------------------------------- UI-003 Panel
def ui003():
    o = shell("Panel", "Panel")
    o.append(txt(272, 108, "Agosto 2026", "font.body.md", N5))
    o.append(boton(1044, 90, 204, 40, "+ Nuevo movimiento"))
    tarjetas = [("Balance del mes", "$ 1.240.000", OK6, "+8% vs julio"),
                ("Ingresos", "$ 3.100.000", N9, "2 registros"),
                ("Gastos", "$ 1.860.000", ER6, "34 registros")]
    x = 272
    for tit, val, col, sub in tarjetas:
        o.append(rect(x, 150, 300, 118, S, RL, N2))
        o.append(txt(x + 24, 182, tit, "font.label", N5))
        o.append(txt(x + 24, 220, val, "font.numero.lg", col))
        o.append(txt(x + 24, 244, sub, "font.caption", N5))
        x += 320
    o.append(rect(272, 292, 620, 400, S, RL, N2))
    o.append(txt(296, 328, "Gastos por categoria", "font.heading.md"))
    o.append(txt(296, 350, "Agosto 2026", "font.caption", N5))
    datos = [("Vivienda", 620, "$ 620.000", P6), ("Alimentacion", 480, "$ 480.000", P5),
             ("Transporte", 310, "$ 310.000", OK6), ("Servicios", 240, "$ 240.000", WA6),
             ("Salud", 130, "$ 130.000", N5), ("Otros", 80, "$ 80.000", N3)]
    y = 396
    for nombre, valor, etiqueta, color in datos:
        ancho = int(valor / 620 * 300)
        o.append(txt(296, y + 12, nombre, "font.body.md", N7))
        o.append(rect(420, y, 300, 16, N1, 8))
        o.append(rect(420, y, ancho, 16, color, 8))
        o.append(txt(868, y + 12, etiqueta, "font.body.md", N9, "end", weight=600))
        y += 46
    o.append(rect(912, 292, 336, 400, S, RL, N2))
    o.append(txt(936, 328, "Presupuestos", "font.heading.md"))
    pres = [("Alimentacion", 0.80, "80%", OK6), ("Transporte", 0.62, "62%", OK6),
            ("Entretenimiento", 1.04, "104%", ER6), ("Servicios", 0.48, "48%", OK6)]
    y = 372
    for nombre, pct, etiqueta, color in pres:
        o.append(txt(936, y, nombre, "font.body.md", N7))
        o.append(txt(1224, y, etiqueta, "font.body.md", color, "end", weight=700))
        o.append(rect(936, y + 12, 288, 10, N1, 5))
        o.append(rect(936, y + 12, int(min(pct, 1) * 288), 10, color, 5))
        if pct > 1:
            o.append(txt(936, y + 42, "Superaste el limite en $ 48.000", "font.caption", ER6, weight=600))
            y += 20
        y += 62
    o.append(rect(936, 636, 288, 36, WA1, RM, WA6))
    o.append(txt(950, 659, "1 presupuesto excedido este mes", "font.caption", WA6, weight=600))
    pie(o, W, H, "MK-003 / UI-003")
    return svg(W, H, o, "UI-003 Panel de balance",
               "Panel con tarjetas de balance, ingresos y gastos, grafico de barras de gastos por categoria y consumo de presupuestos con alerta de exceso.")
