# -*- coding: utf-8 -*-
import os
os.environ.setdefault("OUT", ".")
import gen, pantallas, pantallas2, pantallas3, moviles
from gen import guardar

archivos = [
    ("UI-001_iniciar-sesion.svg", pantallas.ui001()),
    ("UI-001b_iniciar-sesion_error.svg", pantallas.ui001e()),
    ("UI-002_crear-cuenta.svg", pantallas.ui002()),
    ("UI-003_panel.svg", pantallas.ui003()),
    ("UI-004_movimientos.svg", pantallas2.ui004()),
    ("UI-004b_movimientos_vacio.svg", pantallas2.ui004v()),
    ("UI-005_registrar-movimiento.svg", pantallas2.ui005()),
    ("UI-006_presupuestos.svg", pantallas2.ui006()),
    ("UI-007_metas-de-ahorro.svg", pantallas3.ui007()),
    ("UI-008_cuentas.svg", pantallas3.ui008()),
    ("UI-009_administracion.svg", pantallas3.ui009()),
    ("UI-010_verificar-correo.svg", pantallas3.ui010()),
    ("UI-011_recuperar-contrasena.svg", pantallas3.ui011()),
    ("UI-012_restablecer-contrasena.svg", pantallas3.ui012()),
    ("UI-013_retorno-google.svg", pantallas3.ui013()),
    ("UI-014_obligaciones.svg", pantallas3.ui014()),
    ("UI-015_categorias.svg", pantallas3.ui015()),
    ("UI-003m_panel_movil.svg", moviles.m003()),
    ("UI-005m_registrar-movimiento_movil.svg", moviles.m005()),
    ("sistema-de-diseno.svg", moviles.sistema()),
]
for nombre, contenido in archivos:
    guardar(nombre, contenido)
print(f"\ntotal: {len(archivos)} archivos")
