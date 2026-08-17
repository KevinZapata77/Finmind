# -*- coding: utf-8 -*-
"""UI-007 a UI-013. Las cuatro de identidad y cuentas reflejan codigo ya construido."""
from gen import *

W, H = 1280, 800


def _lateral(o, lema1, lema2, bajada1="", bajada2=""):
    """Panel izquierdo verde, comun a las pantallas sin sesion."""
    o.append(rect(0, 0, 520, H, P7))
    o.append(rect(64, 72, 34, 34, P5, RS))
    o.append(txt(81, 96, "F", "font.heading.lg", S, "middle"))
    o.append(txt(110, 98, "FinMind", "font.heading.lg", S))
    o.append(txt(64, 340, lema1, "font.display", S, size=34))
    o.append(txt(64, 384, lema2, "font.display", S, size=34))
    if bajada1:
        o.append(txt(64, 426, bajada1, "font.body.md", P1))
    if bajada2:
        o.append(txt(64, 450, bajada2, "font.body.md", P1))


# --------------------------------------------- UI-010 Verificar correo
def ui010():
    o = [rect(0, 0, W, H, C)]
    _lateral(o, "Un paso mas.", "",
             "Confirmamos tu correo para que solo tu", "puedas entrar a tu cuenta.")
    x = 640
    o.append(rect(x, 150, 520, 500, S, RL, N2))
    o.append(txt(x + 40, 206, "Verifica tu correo", "font.heading.lg"))
    o.append(txt(x + 40, 234, "Enviamos un codigo de 6 digitos a", "font.body.md", N5))
    o.append(txt(x + 40, 256, "kevin@ejemplo.com", "font.body.md", N9, weight=600))
    o.append(txt(x + 40, 278, "Vence en 15 minutos.", "font.caption", N5))
    o.append(txt(x + 40, 326, "Codigo de verificacion", "font.label", N7))
    o.append(casillas_codigo(x + 40, 340, "4829", 6, 62, 12))
    o.append(boton(x + 40, 442, 440, 44, "Verificar y entrar"))
    o.append(txt(x + 40, 522, "No te llego?", "font.body.md", N5))
    o.append(txt(x + 142, 522, "Puedes reenviarlo en 45s", "font.body.md", N5))
    o.append(aviso(x + 40, 546, 440, "El codigo es de un solo uso",
                   "Si pides otro, el anterior deja de servir.", "info"))
    o.append(txt(x + 260, 626, "Volver a iniciar sesion", "font.caption", P6, "middle", weight=600))
    pie(o, W, H, "MK-010 / UI-010")
    return svg(W, H, o, "UI-010 Verificar correo",
               "Pantalla de verificacion con seis casillas para el codigo, cuatro ya diligenciadas, "
               "boton principal, contador para reenviar y aviso de que el codigo es de un solo uso.")


# --------------------------------------------- UI-011 Recuperar contrasena
def ui011():
    o = [rect(0, 0, W, H, C)]
    _lateral(o, "Te ayudamos", "a volver.",
             "Te enviamos un codigo para crear", "una contrasena nueva.")
    x = 660
    o.append(rect(x, 210, 460, 380, S, RL, N2))
    o.append(txt(x + 40, 266, "Recuperar contrasena", "font.heading.lg"))
    o.append(txt(x + 40, 294, "Escribe tu correo y te enviaremos", "font.body.md", N5))
    o.append(txt(x + 40, 316, "un codigo para crear una nueva.", "font.body.md", N5))
    o.append(campo(x + 40, 360, 380, "Correo electronico", "kevin@ejemplo.com", ph=True))
    o.append(boton(x + 40, 442, 380, 44, "Enviar codigo"))
    o.append(aviso(x + 40, 504, 380, "La respuesta es siempre la misma",
                   "RN-014: no revelamos si el correo esta registrado.", "info"))
    pie(o, W, H, "MK-011 / UI-011")
    return svg(W, H, o, "UI-011 Recuperar contrasena",
               "Formulario de un solo campo de correo con boton de envio y nota sobre la respuesta "
               "uniforme que evita revelar que cuentas existen.")


# --------------------------------------------- UI-012 Restablecer contrasena
def ui012():
    o = [rect(0, 0, W, H, C)]
    _lateral(o, "Nueva", "contrasena.",
             "Ingresa el codigo que te llego", "y elige una contrasena nueva.")
    x = 640
    o.append(rect(x, 90, 520, 620, S, RL, N2))
    o.append(txt(x + 40, 146, "Restablecer contrasena", "font.heading.lg"))
    o.append(txt(x + 40, 174, "Codigo enviado a kevin@ejemplo.com", "font.body.md", N5))
    o.append(aviso(x + 40, 196, 440, "Codigo enviado",
                   "Si el correo esta registrado, recibiras un codigo.", "ok"))
    o.append(campo(x + 40, 288, 440, "Codigo de 6 digitos", "482913", ph=True))
    o.append(campo(x + 40, 374, 440, "Nueva contrasena", "Minimo 8 caracteres", ph=True))
    o.append(campo(x + 40, 460, 440, "Repite la contrasena", "........", ph=True, foco=True))
    o.append(boton(x + 40, 560, 440, 44, "Cambiar contrasena"))
    o.append(txt(x + 260, 640, "Volver a iniciar sesion", "font.caption", P6, "middle", weight=600))
    pie(o, W, H, "MK-012 / UI-012")
    return svg(W, H, o, "UI-012 Restablecer contrasena",
               "Formulario con codigo, contrasena nueva y confirmacion, con el ultimo campo enfocado "
               "y una franja verde confirmando el envio del codigo.")


# --------------------------------------------- UI-013 Retorno de Google
def ui013():
    o = [rect(0, 0, W, H, C)]
    o.append(rect(390, 250, 500, 300, S, RL, N2))
    o.append(rect(615, 300, 50, 50, P1, 25))
    o.append(txt(640, 334, "G", "font.heading.lg", "#4285F4", "middle", weight=700))
    o.append(txt(640, 400, "Confirmando tu identidad...", "font.heading.md", N9, "middle"))
    o.append(txt(640, 428, "Volviendo desde Google. Esto toma un momento.", "font.body.md", N5, "middle"))
    o.append(rect(490, 462, 300, 6, N2, 3))
    o.append(rect(490, 462, 190, 6, P6, 3))
    o.append(txt(640, 512, "No cierres esta ventana", "font.caption", N5, "middle"))
    o.append(aviso(140, 640, 1000, "Pantalla tecnica, no interactiva",
                   "Recibe el token, abre la sesion y sale al panel. Si la cuenta ya existe como local, "
                   "vuelve a UI-001 con el aviso correspondiente.", "info"))
    pie(o, W, H, "MK-013 / UI-013")
    return svg(W, H, o, "UI-013 Retorno de Google",
               "Pantalla de espera que aparece al volver de Google mientras se abre la sesion. "
               "El usuario no interactua con ella.")


# --------------------------------------------- UI-008 Cuentas
def ui008():
    o = shell("Mis cuentas", "Cuentas")
    o.append(txt(272, 116, "Saldo total disponible", "font.label", N7))
    o.append(txt(272, 152, "$ 4.235.000", "font.numero.lg", N9))
    o.append(boton(1020, 108, 200, 44, "Nueva cuenta"))
    o.append(rect(272, 186, 16, 16, S, RS, N3))
    o.append(txt(298, 199, "Mostrar tambien las cuentas desactivadas", "font.body.md", N7))

    filas = [
        ("Ahorros Bancolombia", "Cuenta de ahorros", "$ 2.500.000", "$ 2.350.000", True),
        ("Efectivo", "Efectivo", "$ 185.000", "$ 200.000", True),
        ("Nequi", "Billetera digital", "$ 1.550.000", "$ 1.400.000", True),
        ("Tarjeta Visa", "Tarjeta de credito", "$ 0", "$ 0", False),
    ]
    y = 232
    for nombre, tipo, actual, inicial, activa in filas:
        fondo = S if activa else N1
        o.append(rect(272, y, 948, 86, fondo, RL, N2))
        o.append(txt(296, y + 34, nombre, "font.heading.md", N9 if activa else N7))
        if not activa:
            o.append(rect(296 + len(nombre) * 9 + 16, y + 20, 96, 20, N2, RS))
            o.append(txt(296 + len(nombre) * 9 + 64, y + 34, "Desactivada",
                         "font.caption", N7, "middle", weight=600))
        o.append(txt(296, y + 58, tipo, "font.caption", N5))
        o.append(txt(1010, y + 34, actual, "font.heading.md", N9 if activa else N7, "end"))
        o.append(txt(1010, y + 56, "Inicial: " + inicial, "font.caption", N5, "end"))
        o.append(txt(1060, y + 48, "Editar", "font.caption", P6, weight=600))
        o.append(txt(1130, y + 48, "Desactivar" if activa else "Reactivar",
                     "font.caption", P6, weight=600))
        y += 98
    o.append(txt(272, y + 30, "El saldo actual es el saldo inicial mas ingresos menos gastos.",
                 "font.caption", N5))
    pie(o, W, H, "MK-008 / UI-008")
    return svg(W, H, o, "UI-008 Cuentas",
               "Listado de cuentas con saldo actual y saldo inicial, una cuenta desactivada en gris "
               "con su etiqueta, acciones de editar y desactivar, y el saldo total arriba.")


# --------------------------------------------- UI-009 Administracion
def ui009():
    o = shell("Administracion", "Reportes")
    o.append(txt(272, 116, "Usuarios registrados", "font.heading.md", N9))
    o.append(txt(272, 140, "Solo el administrador ve esta pantalla (RF-023, RF-024).",
                 "font.caption", N5))
    for i, (rot, val) in enumerate([("Usuarios activos", "128"), ("Sin verificar", "14"),
                                    ("Desactivados", "6")]):
        cx = 272 + i * 240
        o.append(rect(cx, 164, 220, 88, S, RL, N2))
        o.append(txt(cx + 20, 194, rot, "font.label", N7))
        o.append(txt(cx + 20, 228, val, "font.numero.lg", N9))

    o.append(rect(272, 280, 948, 48, N1, RM))
    for cx, rot in [(296, "Usuario"), (620, "Correo"), (880, "Estado"), (1050, "Acciones")]:
        o.append(txt(cx, 310, rot, "font.label", N7))
    filas = [
        ("Ana Rodriguez", "ana@ejemplo.com", "Activa", OK6, OK1),
        ("Luis Mendez", "luis@ejemplo.com", "Sin verificar", WA6, WA1),
        ("Sofia Torres", "sofia@ejemplo.com", "Activa", OK6, OK1),
        ("Carlos Ruiz", "carlos@ejemplo.com", "Desactivada", N7, N2),
    ]
    y = 328
    for nombre, correo, estado, ct, cf in filas:
        o.append(rect(272, y, 948, 62, S, 0, N2))
        o.append(txt(296, y + 38, nombre, "font.body.md", N9))
        o.append(txt(620, y + 38, correo, "font.body.md", N7))
        o.append(rect(880, y + 20, 122, 24, cf, RS))
        o.append(txt(941, y + 37, estado, "font.caption", ct, "middle", weight=600))
        o.append(txt(1050, y + 38, "Desactivar" if estado != "Desactivada" else "Reactivar",
                     "font.caption", P6, weight=600))
        y += 62
    o.append(aviso(272, y + 24, 948, "Toda accion queda registrada",
                   "Cada activacion o desactivacion se guarda en auditoria_admin con quien la hizo "
                   "y cuando. El administrador no ve los movimientos de nadie (RN-005).", "warn"))
    pie(o, W, H, "MK-009 / UI-009")
    return svg(W, H, o, "UI-009 Administracion",
               "Tabla de usuarios con estado por color y texto, acciones de activar y desactivar, "
               "tarjetas de resumen y nota sobre el registro de auditoria.")


# --------------------------------------------- UI-007 Metas de ahorro
def ui007():
    o = shell("Metas de ahorro", "Metas")
    o.append(aviso(272, 100, 948, "Programada para la ultima iteracion",
                   "Incorporada al MVP por CR-005, que revoco la exclusion EXC-01. Se construye despues "
                   "del nucleo: movimientos, presupuestos y reportes van primero.", "info"))
    o.append(boton(1020, 196, 200, 44, "Nueva meta"))
    metas = [("Viaje a Cartagena", "$ 1.200.000", "$ 3.000.000", 0.40),
             ("Fondo de emergencia", "$ 4.500.000", "$ 6.000.000", 0.75),
             ("Computador nuevo", "$ 300.000", "$ 4.000.000", 0.075)]
    y = 240
    for nombre, actual, meta, pct in metas:
        o.append(rect(272, y, 948, 96, S, RL, N2))
        o.append(txt(296, y + 32, nombre, "font.heading.md", N9))
        o.append(txt(1196, y + 32, f"{int(pct * 100)}%", "font.heading.md", P6, "end"))
        o.append(rect(296, y + 52, 900, 10, N2, 5))
        o.append(rect(296, y + 52, int(900 * pct), 10, P6, 5))
        o.append(txt(296, y + 82, f"{actual} de {meta}", "font.caption", N5))
        o.append(txt(1196, y + 82, "Abonar", "font.caption", P6, "end", weight=600))
        y += 108
    o.append(rect(272, y + 10, 948, 70, S, RL, N3, dash="6 4"))
    o.append(txt(746, y + 52, "+  Nueva meta de ahorro", "font.body.md", N5, "middle", weight=600))
    pie(o, W, H, "MK-007 / UI-007")
    return svg(W, H, o, "UI-007 Metas de ahorro",
               "Listado de metas con barra de avance, monto acumulado sobre objetivo y porcentaje. "
               "Incluye accion para crear una meta nueva y abonar a las existentes.")


# --------------------------------------------- UI-014 Obligaciones
def ui014():
    o = shell("Obligaciones", "Obligaciones")
    for i, (rot, val, col) in enumerate([
            ("Debes en total", "$ 6.850.000", ER6),
            ("Cuotas de este mes", "$ 780.000", N9),
            ("Patrimonio neto", "-$ 2.615.000", ER6)]):
        cx = 272 + i * 320
        o.append(rect(cx, 100, 300, 96, S, RL, N2))
        o.append(txt(cx + 20, 130, rot, "font.label", N7))
        o.append(txt(cx + 20, 166, val, "font.numero.lg", col))
    o.append(boton(1020, 212, 200, 44, "Nueva obligacion"))
    o.append(aviso(272, 212, 700, "Cuotas por vencer",
                   "Tarjeta Visa el dia 15 y Credito de vehiculo el dia 20.", "warn"))

    deudas = [("Tarjeta Visa", "Bancolombia · Tarjeta de credito", "$ 1.850.000",
               "$ 2.400.000", 23, "$ 37.000", "$ 300.000", 15),
              ("Credito de vehiculo", "Banco de Bogota · Credito de vehiculo", "$ 4.200.000",
               "$ 9.000.000", 53, "$ 56.000", "$ 380.000", 20),
              ("Prestamo de mi tia", "Familia · Prestamo personal, sin interes", "$ 800.000",
               "$ 1.000.000", 20, "$ 0", "$ 100.000", 5)]
    y = 296
    for nombre, meta, saldo, original, pct, interes, cuota, dia in deudas:
        o.append(rect(272, y, 948, 130, S, RL, N2))
        o.append(txt(296, y + 32, nombre, "font.heading.md"))
        o.append(txt(1196, y + 32, saldo, "font.heading.md", ER6, "end"))
        o.append(txt(296, y + 54, f"{meta} · cuota {cuota} el dia {dia}", "font.caption", N5))
        o.append(rect(296, y + 68, 900, 10, N2, 5))
        o.append(rect(296, y + 68, int(900 * pct / 100), 10, P6, 5))
        o.append(txt(296, y + 98, f"Pagado {pct}% de {original}", "font.caption", N7))
        o.append(txt(700, y + 98, f"Interes de este mes: {interes}", "font.caption", WA6))
        o.append(txt(1000, y + 98, "Registrar pago", "font.caption", P6, weight=600))
        o.append(txt(1196, y + 98, "Cancelar", "font.caption", P6, "end", weight=600))
        y += 142
    pie(o, W, H, "MK-014 / UI-014")
    return svg(W, H, o, "UI-014 Obligaciones",
               "Listado de deudas con saldo pendiente, barra de avance, interes del mes y cuota. "
               "Arriba, el total adeudado, las cuotas del mes y el patrimonio neto en negativo.")


# --------------------------------------------- UI-015 Categorias
def ui015():
    o = shell("Categorias", "Categorias")
    o.append(boton(1020, 108, 200, 44, "Nueva categoria"))
    o.append(txt(272, 130, "Gastos", "font.heading.md"))

    filas = [("Alimentacion", "#B45309", True, True), ("Transporte", "#0B6B57", True, True),
             ("Mascotas", "#0E8368", False, True), ("Gimnasio", "#6B7280", False, False)]
    y = 156
    for nombre, color, sistema, activa in filas:
        o.append(rect(272, y, 948, 56, S if activa else N1, RM, N2))
        o.append(rect(296, y + 22, 14, 14, color, 7))
        o.append(txt(324, y + 34, nombre, "font.body.md", N9 if activa else N7))
        ancho = len(nombre) * 8 + 40
        if sistema:
            o.append(rect(324 + ancho, y + 18, 92, 20, N2, RS))
            o.append(txt(370 + ancho, y + 32, "Del sistema", "font.caption", N7, "middle", weight=600))
            o.append(txt(1196, y + 34, "No se modifica", "font.caption", N5, "end"))
        else:
            if not activa:
                o.append(rect(324 + ancho, y + 18, 92, 20, N2, RS))
                o.append(txt(370 + ancho, y + 32, "Desactivada", "font.caption", N7, "middle", weight=600))
            o.append(txt(1090, y + 34, "Editar", "font.caption", P6, weight=600))
            o.append(txt(1196, y + 34, "Desactivar" if activa else "Reactivar",
                         "font.caption", P6, "end", weight=600))
        y += 64

    o.append(txt(272, y + 34, "Ingresos", "font.heading.md"))
    y += 60
    for nombre, color, sistema in [("Salario", "#15803D", True), ("Freelance", "#0E8368", False)]:
        o.append(rect(272, y, 948, 56, S, RM, N2))
        o.append(rect(296, y + 22, 14, 14, color, 7))
        o.append(txt(324, y + 34, nombre, "font.body.md"))
        ancho = len(nombre) * 8 + 40
        if sistema:
            o.append(rect(324 + ancho, y + 18, 92, 20, N2, RS))
            o.append(txt(370 + ancho, y + 32, "Del sistema", "font.caption", N7, "middle", weight=600))
            o.append(txt(1196, y + 34, "No se modifica", "font.caption", N5, "end"))
        else:
            o.append(txt(1090, y + 34, "Editar", "font.caption", P6, weight=600))
            o.append(txt(1196, y + 34, "Desactivar", "font.caption", P6, "end", weight=600))
        y += 64

    o.append(aviso(272, y + 16, 948, "Las del sistema no se editan",
                   "Las ven todos los usuarios. Si uno pudiera renombrar 'Salario', se lo cambiaria "
                   "a todos los demas. Desactivar una propia no borra sus movimientos.", "info"))
    pie(o, W, H, "MK-015 / UI-015")
    return svg(W, H, o, "UI-015 Categorias",
               "Categorias separadas en gastos e ingresos, con su color, las del sistema marcadas "
               "y sin acciones de edicion, y una propia desactivada en gris.")
