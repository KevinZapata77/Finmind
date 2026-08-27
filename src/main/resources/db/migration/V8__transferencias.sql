-- =============================================================================
-- V8: transferencias entre cuentas (RF-045, RN-022, CR-011, DEF-16)
--
-- EL PROBLEMA
-- No habia forma de abonar a una tarjeta de credito. La unica manera era
-- registrar un INGRESO sobre ella, y eso tenia dos defectos:
--
--   1. Inflaba los ingresos del mes. El balance suma todos los movimientos de
--      tipo INGRESO sin mirar la cuenta, asi que un abono de 200.000 hacia que
--      la aplicacion reportara 2.800.000 de ingresos cuando el usuario habia
--      ganado 2.600.000. Pagar una deuda no es ganar dinero.
--
--   2. El dinero aparecia de la nada. El abono salia de la cuenta de ahorros,
--      pero eso no quedaba registrado en ningun lado y el saldo de ahorros no
--      bajaba.
--
-- LA CAUSA
-- Pagar una tarjeta no es un ingreso ni un gasto: es mover dinero de una cuenta
-- a otra. El sistema solo conocia esas dos categorias de movimiento.
--
-- LA SOLUCION
-- Un tercer tipo de movimiento, TRANSFERENCIA, con cuenta de origen y cuenta de
-- destino. Se registra en una sola fila:
--
--     cuenta_id           de donde sale
--     cuenta_destino_id   a donde entra
--     categoria_id        NULL: una transferencia no es un gasto de nada
--
-- Como el balance, la composicion del gasto y el consumo de presupuesto filtran
-- por tipo INGRESO o GASTO, la transferencia queda fuera de los tres sin tocar
-- ninguna de esas consultas. Eso es lo que la hace neutral.
-- =============================================================================

-- ------------------------------------------------------ cuenta de destino
ALTER TABLE transacciones ADD COLUMN cuenta_destino_id BIGINT;

ALTER TABLE transacciones
    ADD CONSTRAINT fk_transacciones_cuenta_destino
        FOREIGN KEY (cuenta_destino_id) REFERENCES cuentas (id) ON DELETE CASCADE;

COMMENT ON COLUMN transacciones.cuenta_destino_id IS
    'Solo en las transferencias: la cuenta que recibe. NULL en ingresos y gastos';

-- ------------------------------------------------------ categoria opcional
-- Una transferencia no pertenece a ninguna categoria de gasto: el dinero no se
-- consumio, cambio de sitio. Los ingresos y gastos SI siguen exigiendola, y eso
-- lo garantiza la restriccion de coherencia de mas abajo.
ALTER TABLE transacciones ALTER COLUMN categoria_id DROP NOT NULL;

-- ------------------------------------------------------ nuevo tipo
-- La columna era VARCHAR(10), suficiente para INGRESO y GASTO. TRANSFERENCIA
-- son 13 caracteres, asi que hay que ampliarla ANTES de admitir el valor. Sin
-- esto la aplicacion ni arrancaria: Hibernate valida la longitud contra la
-- entidad y se detiene, que es exactamente lo que paso en la V4 con CHAR.
ALTER TABLE transacciones ALTER COLUMN tipo TYPE VARCHAR(13);

ALTER TABLE transacciones DROP CONSTRAINT ck_transacciones_tipo;

ALTER TABLE transacciones
    ADD CONSTRAINT ck_transacciones_tipo
        CHECK (tipo IN ('INGRESO','GASTO','TRANSFERENCIA'));

-- ------------------------------------------------------ coherencia
-- Cada tipo exige exactamente sus campos. Sin esto se podria guardar una
-- transferencia con categoria, o un gasto con cuenta de destino, y despues
-- ninguna consulta sabria como interpretar esa fila.
ALTER TABLE transacciones
    ADD CONSTRAINT ck_transacciones_coherencia CHECK (
        (tipo IN ('INGRESO','GASTO')
            AND categoria_id IS NOT NULL
            AND cuenta_destino_id IS NULL)
        OR
        (tipo = 'TRANSFERENCIA'
            AND categoria_id IS NULL
            AND cuenta_destino_id IS NOT NULL)
    );

-- Transferirse dinero a la misma cuenta no significa nada y dejaria el saldo
-- igual, dando la impresion de que el movimiento no se guardo.
ALTER TABLE transacciones
    ADD CONSTRAINT ck_transacciones_destino_distinto
        CHECK (cuenta_destino_id IS NULL OR cuenta_destino_id <> cuenta_id);

-- Para calcular el saldo de la cuenta que recibe.
CREATE INDEX idx_transacciones_destino
    ON transacciones (cuenta_destino_id) WHERE cuenta_destino_id IS NOT NULL;
