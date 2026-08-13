# -*- coding: utf-8 -*-
"""Verifica los ratios de contraste WCAG de la paleta. Evidencia para la seccion 12.3."""
from tokens import TOKENS, contraste

BLANCO, CANVAS = TOKENS["color.surface"], TOKENS["color.canvas"]
PARES = [
    ("color.neutral.900", "texto principal sobre superficie", "color.neutral.900", BLANCO),
    ("color.neutral.700", "texto secundario sobre superficie", "color.neutral.700", BLANCO),
    ("color.neutral.500", "texto de ayuda sobre superficie", "color.neutral.500", BLANCO),
    ("color.primary.600", "texto y enlaces sobre superficie", "color.primary.600", BLANCO),
    ("color.primary.600", "texto blanco sobre boton primario", BLANCO, "color.primary.600"),
    ("color.primary.700", "texto blanco sobre boton oscuro", BLANCO, "color.primary.700"),
    ("color.error.600", "texto de error sobre superficie", "color.error.600", BLANCO),
    ("color.success.600", "texto de exito sobre superficie", "color.success.600", BLANCO),
    ("color.warning.600", "texto de advertencia sobre superficie", "color.warning.600", BLANCO),
    ("color.neutral.900", "texto principal sobre lienzo", "color.neutral.900", CANVAS),
]
MINIMO = 4.5

print(f"{'TOKEN':22} {'USO':42} {'RATIO':>7}  AA")
print("-" * 82)
todos = True
for token, uso, fg, bg in PARES:
    r = contraste(TOKENS.get(fg, fg), TOKENS.get(bg, bg))
    if r < MINIMO:
        todos = False
    print(f"{token:22} {uso:42} {r:>7}  {'Si' if r >= MINIMO else 'NO'}")
print("-" * 82)
print(f"Resultado: {'todos los pares cumplen AA 4.5:1' if todos else 'HAY PARES QUE NO CUMPLEN'}")
