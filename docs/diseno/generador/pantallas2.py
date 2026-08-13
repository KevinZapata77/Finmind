# -*- coding: utf-8 -*-
from gen import *
W, H = 1280, 800

FILAS = [("05 ago", "Mercado del mes", "Alimentacion", "Nequi", "- $ 320.000", ER6),
         ("04 ago", "Salario agosto", "Salario", "Bancolombia", "+ $ 3.100.000", OK6),
         ("03 ago", "Recarga transporte", "Transporte", "Efectivo", "- $ 45.000", ER6),
         ("02 ago", "Arriendo", "Vivienda", "Bancolombia", "- $ 620.000", ER6),
         ("01 ago", "Internet y celular", "Servicios", "Nequi", "- $ 145.000", ER6),
         ("31 jul", "Cine con amigos", "Entretenimiento", "Efectivo", "- $ 58.000", ER6)]


def _tabla(o, y0):
    o.append(rect(272, y0, 976, 44, N1))
    enc = [("FECHA", 296), ("DESCRIPCION", 396), ("CATEGORIA", 660), ("CUENTA", 840)]
    for t_, x in enc:
        o.append(txt(x, y0 + 28, t_, "font.label", N7))
    o.append(txt(1140, y0 + 28, "MONTO", "font.label", N7, "end"))
    y = y0 + 44
    for fecha, desc, cat, cta, monto, col in FILAS:
        o.append(rect(272, y, 976, 56, S))
        o.append(rect(272, y + 56, 976, 1, N2))
        o.append(txt(296, y + 34, fecha, "font.body.md", N7))
        o.append(txt(396, y + 34, desc, "font.body.md", N9, weight=600))
        o.append(rect(660, y + 18, len(cat) * 7 + 20, 22, P1, 11))
        o.append(txt(670, y + 33, cat, "font.caption", P7, weight=600))
        o.append(txt(840, y + 34, cta, "font.body.md", N7))
        o.append(txt(1140, y + 34, monto, "font.body.md", col, "end", weight=700))
        o.append(txt(1196, y + 34, "Editar", "font.caption", P6, weight=600))
        y += 57
    return y


# --------------------------------------------- UI-004 Movimientos
def ui004():
    o = shell("Movimientos", "Movimientos")
    o.append(boton(1044, 90, 204, 40, "+ Nuevo movimiento"))
    o.append(rect(272, 150, 976, 64, S, RL, N2))
    for etq, x, w in [("Buscar descripcion", 292, 240), ("Categoria: Todas", 548, 170),
                      ("Cuenta: Todas", 730, 150), ("Agosto 2026", 892, 150)]:
        o.append(rect(x, 164, w, 36, S, RM, N3))
        o.append(txt(x + 12, 187, etq, "font.body.md", N5))
    o.append(txt(1230, 187, "Limpiar", "font.body.md", P6, "end", weight=600))
    yfin = _tabla(o, 236)
    o.append(txt(272, yfin + 34, "Mostrando 6 de 36 movimientos", "font.body.md", N5))
    for i, et in enumerate(["Anterior", "1", "2", "3", "Siguiente"]):
        x = 980 + i * 62
        act = et == "1"
        o.append(rect(x, yfin + 14, 56, 34, P6 if act else S, RM, None if act else N3))
        o.append(txt(x + 28, yfin + 36, et, "font.caption", S if act else N7, "middle", weight=600))
    pie(o, W, H, "MK-004 / UI-004")
    return svg(W, H, o, "UI-004 Movimientos",
               "Listado de movimientos con buscador, filtros por categoria, cuenta y periodo, tabla con seis registros y paginacion.")


# ------------------------------------ UI-004-V estado vacio
def ui004v():
    o = shell("Movimientos", "Movimientos")
    o.append(boton(1044, 90, 204, 40, "+ Nuevo movimiento"))
    o.append(rect(272, 150, 976, 64, S, RL, N2))
    for etq, x, w in [("Buscar descripcion", 292, 240), ("Categoria: Todas", 548, 170),
                      ("Cuenta: Todas", 730, 150), ("Agosto 2026", 892, 150)]:
        o.append(rect(x, 164, w, 36, S, RM, N3))
        o.append(txt(x + 12, 187, etq, "font.body.md", N5))
    o.append(rect(272, 236, 976, 420, S, RL, N2))
    o.append(rect(716, 320, 88, 88, P1, 44))
    o.append(txt(760, 378, "+", "font.display", P6, "middle", size=44))
    o.append(txt(760, 456, "Todavia no tienes movimientos", "font.heading.lg", N9, "middle"))
    o.append(txt(760, 486, "Registra tu primer ingreso o gasto para empezar a ver", "font.body.md", N5, "middle"))
    o.append(txt(760, 508, "tu balance y el consumo de tus presupuestos.", "font.body.md", N5, "middle"))
    o.append(boton(660, 542, 200, 44, "Registrar el primero"))
    pie(o, W, H, "MK-004b / UI-004 estado vacio")
    return svg(W, H, o, "UI-004 Movimientos, estado vacio",
               "Estado vacio del listado: explica que no hay datos y ofrece la accion de registrar el primer movimiento.")


# ------------------------------------ UI-005 Registrar movimiento (modal)
def ui005():
    o = shell("Movimientos", "Movimientos")
    o.append(rect(272, 150, 976, 64, S, RL, N2))
    _tabla(o, 236)
    o.append(rect(0, 0, W, H, "#111827", 0))
    o[-1] = f'<rect x="0" y="0" width="{W}" height="{H}" fill="#111827" opacity="0.55"/>'
    x, y, w = 390, 96, 500
    o.append(rect(x, y, w, 620, S, RL))
    o.append(txt(x + 32, y + 52, "Registrar movimiento", "font.heading.lg"))
    o.append(txt(x + 32, y + 76, "Los campos con asterisco son obligatorios.", "font.body.md", N5))
    o.append(txt(x + 452, y + 50, "X", "font.heading.md", N5, "end"))
    o.append(txt(x + 32, y + 116, "Tipo *", "font.label", N7))
    o.append(rect(x + 32, y + 126, 216, 42, P1, RM, P6, 2))
    o.append(txt(x + 140, y + 153, "Gasto", "font.body.md", P7, "middle", weight=700))
    o.append(rect(x + 252, y + 126, 216, 42, S, RM, N3))
    o.append(txt(x + 360, y + 153, "Ingreso", "font.body.md", N7, "middle"))
    o.append(campo(x + 32, y + 196, 436, "Monto *", "$ 320.000", foco=True))
    o.append(txt(x + 32, y + 296, "Fecha *", "font.label", N7))
    o.append(rect(x + 32, y + 306, 208, 40, S, RM, N3))
    o.append(txt(x + 44, y + 331, "05/08/2026", "font.body.md", N9))
    o.append(txt(x + 32, y + 366, "Categoria *", "font.label", N7))
    o.append(rect(x + 32, y + 376, 208, 40, S, RM, N3))
    o.append(txt(x + 44, y + 401, "Alimentacion", "font.body.md", N9))
    o.append(txt(x + 260, y + 296, "Cuenta *", "font.label", N7))
    o.append(rect(x + 260, y + 306, 208, 40, S, RM, N3))
    o.append(txt(x + 272, y + 331, "Nequi", "font.body.md", N9))
    o.append(txt(x + 260, y + 366, "Descripcion", "font.label", N7))
    o.append(rect(x + 260, y + 376, 208, 40, S, RM, N3))
    o.append(txt(x + 272, y + 401, "Mercado del mes", "font.body.md", N9))
    o.append(rect(x + 32, y + 440, 436, 44, P1, RM))
    o.append(txt(x + 48, y + 461, "Presupuesto de Alimentacion", "font.caption", P7, weight=700))
    o.append(txt(x + 48, y + 477, "Con este gasto llegas al 80% de $ 1.000.000", "font.caption", P7))
    o.append(rect(x + 32, y + 508, 436, 1, N2))
    o.append(boton(x + 32, y + 528, 140, 44, "Cancelar", "secondary"))
    o.append(boton(x + 288, y + 528, 180, 44, "Guardar movimiento"))
    pie(o, W, H, "MK-005 / UI-005")
    return svg(W, H, o, "UI-005 Registrar movimiento",
               "Modal de registro de movimiento sobre el listado, con selector de tipo, monto enfocado, fecha, categoria, cuenta, descripcion y aviso de consumo de presupuesto.")


# ------------------------------------ UI-006 Presupuestos
def ui006():
    o = shell("Presupuestos", "Presupuestos")
    o.append(txt(272, 108, "Agosto 2026", "font.body.md", N5))
    o.append(boton(1032, 90, 216, 40, "+ Nuevo presupuesto"))
    o.append(rect(272, 150, 976, 88, S, RL, N2))
    o.append(txt(296, 182, "Presupuestado este mes", "font.label", N5))
    o.append(txt(296, 216, "$ 2.400.000", "font.numero.lg"))
    o.append(txt(650, 182, "Consumido", "font.label", N5))
    o.append(txt(650, 216, "$ 1.860.000", "font.numero.lg", ER6))
    o.append(txt(1000, 182, "Disponible", "font.label", N5))
    o.append(txt(1000, 216, "$ 540.000", "font.numero.lg", OK6))
    datos = [("Alimentacion", "$ 800.000", "$ 1.000.000", 0.80, OK6, "Te queda $ 200.000"),
             ("Vivienda", "$ 620.000", "$ 700.000", 0.88, WA6, "Cerca del limite"),
             ("Entretenimiento", "$ 208.000", "$ 200.000", 1.04, ER6, "Excedido en $ 8.000"),
             ("Transporte", "$ 232.000", "$ 500.000", 0.46, OK6, "Te queda $ 268.000")]
    y = 264
    for nombre, gastado, limite, pct, color, nota in datos:
        o.append(rect(272, y, 976, 96, S, RL, N2))
        o.append(txt(296, y + 34, nombre, "font.heading.md"))
        o.append(txt(296, y + 58, f"{gastado} de {limite}", "font.body.md", N5))
        o.append(rect(296, y + 72, 700, 10, N1, 5))
        o.append(rect(296, y + 72, int(min(pct, 1) * 700), 10, color, 5))
        etq = f"{int(pct*100)}%"
        o.append(txt(1224, y + 38, etq, "font.heading.md", color, "end"))
        marca = "!" if pct >= 1 else ("~" if pct >= 0.85 else "OK")
        o.append(rect(1150, y + 52, 74, 22, ER1 if pct >= 1 else (WA1 if pct >= 0.85 else OK1), 11))
        o.append(txt(1187, y + 67, marca + " " + ("Excede" if pct >= 1 else ("Alerta" if pct >= 0.85 else "En rango")),
                     "font.caption", color, "middle", weight=600))
        o.append(txt(296, y + 96 - 4, "", "font.caption"))
        o.append(txt(1000, y + 58, nota, "font.caption", color, weight=600))
        y += 112
    pie(o, W, H, "MK-006 / UI-006")
    return svg(W, H, o, "UI-006 Presupuestos",
               "Resumen de presupuestos del mes con totales y tarjetas por categoria mostrando consumo, porcentaje y estado en rango, alerta o excedido con texto e icono ademas del color.")
