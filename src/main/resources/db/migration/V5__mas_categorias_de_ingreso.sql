-- =============================================================================
-- V5: mas categorias de ingreso del sistema
--
-- POR QUE
-- El catalogo tenia dos categorias de ingreso: 'Salario' y 'Otros ingresos'.
-- Quien tiene mas de una fuente de plata -- un empleo y algo por su cuenta, o
-- dos trabajos distintos -- no podia distinguirlas, y todo caia en el mismo
-- monton. Justo la informacion que hace util un registro de ingresos.
--
-- No se resuelve solo con esto: el usuario puede crear las suyas (RF-009). Estas
-- son las que cubren la mayoria de casos sin que tenga que inventarlas.
--
-- Con usuario_id NULO son del sistema: las ven todos, nadie las edita.
-- El indice uk_categorias_sistema evita duplicados si la migracion se repite.
-- =============================================================================

INSERT INTO categorias (usuario_id, nombre, tipo, icono, color_hex) VALUES
    (NULL, 'Ventas',             'INGRESO', 'tag',      '#0E8368'),
    (NULL, 'Trabajo por horas',  'INGRESO', 'clock',    '#0B6B57'),
    (NULL, 'Servicios prestados','INGRESO', 'briefcase','#15803D'),
    (NULL, 'Arriendos',          'INGRESO', 'building', '#0A5647'),
    (NULL, 'Prestamo recibido',  'INGRESO', 'handshake','#374151')
ON CONFLICT DO NOTHING;

-- Dos gastos que faltaban y aparecen en casi cualquier presupuesto real.
INSERT INTO categorias (usuario_id, nombre, tipo, icono, color_hex) VALUES
    (NULL, 'Deudas y cuotas',    'GASTO',   'receipt',  '#B91C1C'),
    (NULL, 'Ahorro',             'GASTO',   'piggy',    '#0E8368')
ON CONFLICT DO NOTHING;
