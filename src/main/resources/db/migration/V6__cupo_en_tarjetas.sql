-- =============================================================================
-- V6: cupo de las tarjetas de credito (RF-043, RN-021, CR-009)
--
-- POR QUE
-- Una tarjeta de credito se registraba como una cuenta mas, y el saldo se
-- calculaba igual que en las demas: inicial + ingresos - gastos. En una cuenta
-- de ahorros eso es correcto; en una tarjeta esta al reves, porque comprar con
-- la tarjeta AUMENTA lo que se debe. Con suficientes compras el saldo llegaba a
-- negativo, o sea que la aplicacion afirmaba que el banco le debia al usuario.
--
-- Ademas esa deuda no aparecia en el patrimonio: totalActivos excluye las
-- tarjetas (correcto), pero lo adeudado solo miraba el modulo de obligaciones.
-- La deuda de la tarjeta no sumaba ni restaba en ningun lado.
--
-- El signo se corrige en el codigo, que es donde se calcula el saldo. Esta
-- migracion agrega lo unico que falta en la base: el cupo.
-- =============================================================================

ALTER TABLE cuentas ADD COLUMN cupo NUMERIC(15,2);

COMMENT ON COLUMN cuentas.cupo IS
    'Cupo total de la tarjeta de credito. NULL en los demas tipos de cuenta';

-- Un cupo de cero o negativo no significa nada. Se admite NULL porque las
-- cuentas que no son tarjeta no tienen cupo, y porque una tarjeta puede
-- registrarse sin saber el cupo todavia.
ALTER TABLE cuentas
    ADD CONSTRAINT ck_cuentas_cupo CHECK (cupo IS NULL OR cupo > 0);

-- Solo las tarjetas llevan cupo. Sin esta restriccion, una cuenta de ahorros
-- con cupo pasaria sin error y despues nadie sabria si fue un dato valido o
-- un descuido de quien lo escribio.
ALTER TABLE cuentas
    ADD CONSTRAINT ck_cuentas_cupo_solo_tarjeta CHECK (
        cupo IS NULL OR tipo = 'TARJETA_CREDITO'
    );

-- Para el aviso de cupo casi agotado sobre las tarjetas activas.
CREATE INDEX idx_cuentas_tarjetas
    ON cuentas (usuario_id) WHERE tipo = 'TARJETA_CREDITO' AND activa = TRUE;
