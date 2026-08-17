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

# ------------------------------------------------- UI-003 Panel
def ui003():
    """UI-003 Inicio. Rehecha por CHG-UX-007: primero anotar, despues consultar."""
    o = shell("Inicio", "Inicio")

    # --- RF-040: registro rapido, en una sola fila -------------------------
    o.append(rect(272, 96, 948, 96, S, RL, P6, sw=2))
    o.append(rect(296, 116, 84, 36, OK1, RM, OK6, sw=2))
    o.append(txt(338, 139, "Entro", "font.body.md", OK6, "middle", weight=600))
    o.append(rect(388, 116, 84, 36, N1, RM, N2, sw=2))
    o.append(txt(430, 139, "Salio", "font.body.md", N7, "middle", weight=600))
    o.append(rect(484, 116, 300, 36, S, RM, N3))
    o.append(txt(498, 139, "Cuanto?", "font.body.md", N5))
    o.append(rect(796, 116, 260, 36, S, RM, N3))
    o.append(txt(810, 139, "Ventas", "font.body.md", N9))
    o.append(txt(1040, 140, "v", "font.caption", N5))
    o.append(boton(1068, 116, 128, 36, "Anotar"))
    x = 296
    for etiqueta in ("$ 5.000", "$ 10.000", "$ 20.000", "$ 50.000", "$ 100.000"):
        ancho = len(etiqueta) * 7 + 16
        o.append(rect(x, 160, ancho, 22, P1, RS))
        o.append(txt(x + ancho / 2, 175, etiqueta, "font.caption", P6, "middle"))
        x += ancho + 8
    o.append(txt(1196, 175, "+ Nueva categoria", "font.caption", P6, "end", weight=600))

    # --- Tira compacta: la pregunta de todos los dias ----------------------
    o.append(rect(272, 208, 948, 70, N1, RM))
    for k, (rot, neto, det, col) in enumerate([
            ("Hoy", "$ 92.000", "+$ 120.000 y -$ 28.000", OK6),
            ("Esta semana", "$ 418.000", "+$ 610.000 y -$ 192.000", N9),
            ("Este mes", "$ 1.240.000", "+$ 3.100.000 y -$ 1.860.000", N9)]):
        cx = 296 + k * 316
        o.append(txt(cx, 232, rot, "font.label", N7))
        o.append(txt(cx, 256, neto, "font.heading.md", col))
        o.append(txt(cx, 272, det, "font.caption", N5))

    # --- Composicion del gasto (RF-022) ------------------------------------
    o.append(txt(272, 316, "Como vas en agosto", "font.heading.md"))
    o.append(txt(1196, 316, "Agosto 2026", "font.caption", N5, "end"))
    o.append(rect(272, 336, 620, 344, S, RL, N2))
    o.append(txt(296, 370, "En que se fue tu dinero", "font.heading.md"))
    datos = [("Vivienda", 620, "$ 620.000", P6), ("Alimentacion", 480, "$ 480.000", P5),
             ("Transporte", 310, "$ 310.000", OK6), ("Servicios", 240, "$ 240.000", WA6),
             ("Deudas y cuotas", 130, "$ 130.000", ER6), ("Otros", 80, "$ 80.000", N3)]
    y = 404
    for nombre, valor, etiqueta, color in datos:
        o.append(txt(296, y + 12, nombre, "font.body.md", N7))
        o.append(rect(456, y, 264, 14, N1, 7))
        o.append(rect(456, y, int(valor / 620 * 264), 14, color, 7))
        o.append(txt(868, y + 12, etiqueta, "font.body.md", N9, "end", weight=600))
        y += 44

    # --- Patrimonio neto (RF-038) ------------------------------------------
    o.append(rect(912, 336, 308, 160, S, RL, N2))
    o.append(txt(936, 370, "Patrimonio neto", "font.label", N7))
    o.append(txt(936, 404, "-$ 2.615.000", "font.numero.lg", ER6))
    o.append(txt(936, 430, "$ 4.235.000 en cuentas", "font.caption", N5))
    o.append(txt(936, 448, "-$ 6.850.000 en deudas", "font.caption", N5))
    o.append(txt(936, 474, "Debes mas de lo que tienes.", "font.caption", ER6, weight=600))

    # --- Presupuestos que piden atencion (RF-019) --------------------------
    o.append(rect(912, 512, 308, 168, S, RL, WA6))
    o.append(txt(936, 546, "Necesitan tu atencion", "font.heading.md"))
    o.append(rect(936, 564, 260, 46, ER1, RM, ER6))
    o.append(txt(950, 586, "Entretenimiento 104%", "font.caption", ER6, weight=700))
    o.append(txt(950, 602, "Te pasaste por $ 48.000", "font.caption", N7))
    o.append(rect(936, 618, 260, 46, WA1, RM, WA6))
    o.append(txt(950, 640, "Alimentacion 80%", "font.caption", WA6, weight=700))
    o.append(txt(950, 656, "Te quedan $ 100.000", "font.caption", N7))

    pie(o, W, H, "MK-003 / UI-003")
    return svg(W, H, o, "UI-003 Inicio",
               "Inicio con registro rapido en una fila, el resumen de hoy, la semana y el mes en "
               "una tira compacta, la composicion del gasto, el patrimonio neto en negativo y los "
               "presupuestos que requieren atencion.")
