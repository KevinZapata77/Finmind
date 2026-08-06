# Sistema de diseno FinMind - tokens unicos de verdad.
# Los mockups se generan a partir de aqui: si cambia un token, cambian todas las pantallas.

TOKENS = {
    "color.primary.700":  "#0A5647",
    "color.primary.600":  "#0B6B57",
    "color.primary.500":  "#0E8368",
    "color.primary.100":  "#DCF2EC",
    "color.success.600":  "#15803D",
    "color.success.100":  "#DCFCE7",
    "color.warning.600":  "#B45309",
    "color.warning.100":  "#FEF3C7",
    "color.error.600":     "#B91C1C",
    "color.error.100":     "#FEE2E2",
    "color.neutral.900":  "#111827",
    "color.neutral.700":  "#374151",
    "color.neutral.500":  "#6B7280",
    "color.neutral.300":  "#D1D5DB",
    "color.neutral.200":  "#E5E7EB",
    "color.neutral.100":  "#F3F4F6",
    "color.surface":      "#FFFFFF",
    "color.canvas":       "#F7F8FA",
}

FONT = "Inter, 'Segoe UI', system-ui, sans-serif"
TYPO = {
    "font.display":     (28, 700),
    "font.heading.lg":  (22, 700),
    "font.heading.md":  (17, 600),
    "font.body.md":     (14, 400),
    "font.label":       (12, 600),
    "font.caption":     (11, 400),
    "font.numero.lg":   (26, 700),
}
SPACE_BASE = 8
RADIUS = {"radius.sm": 4, "radius.md": 8, "radius.lg": 12}


def _lin(c):
    c = c / 255.0
    return c / 12.92 if c <= 0.03928 else ((c + 0.055) / 1.055) ** 2.4


def luminancia(hexcolor):
    h = hexcolor.lstrip("#")
    r, g, b = (int(h[i:i + 2], 16) for i in (0, 2, 4))
    return 0.2126 * _lin(r) + 0.7152 * _lin(g) + 0.0722 * _lin(b)


def contraste(c1, c2):
    l1, l2 = luminancia(c1), luminancia(c2)
    claro, oscuro = max(l1, l2), min(l1, l2)
    return round((claro + 0.05) / (oscuro + 0.05), 2)
