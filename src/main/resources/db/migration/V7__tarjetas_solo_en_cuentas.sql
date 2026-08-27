-- =============================================================================
-- V7: la tarjeta de credito deja de ser un tipo de obligacion (DEF-15, CR-010)
--
-- EL PROBLEMA
-- 'TARJETA_CREDITO' era un tipo valido en los dos modulos a la vez: como cuenta
-- y como obligacion. Mientras la deuda de la cuenta no se contaba en el
-- patrimonio, el solapamiento no se notaba. Al corregirlo en la V6, quedo
-- expuesto: quien registrara la misma tarjeta en los dos sitios veia su deuda
-- restada dos veces del patrimonio.
--
--   Ahorros                          1.000.000
--   Nu bank como cuenta                500.000  de deuda
--   Nu bank como obligacion            500.000  de deuda   <- la misma tarjeta
--   patrimonio calculado                     0
--   patrimonio real                    500.000
--
-- LA DECISION
-- La tarjeta vive solo en cuentas. Ahi tiene cupo, la deuda sube al comprar y
-- baja al pagar, los gastos quedan categorizados y su deuda ya entra en el
-- patrimonio. Los creditos y prestamos, que si tienen cuota fija y tasa
-- pactada, se quedan en el modulo de obligaciones.
--
-- Y no es solo por el conteo: una tarjeta de credito no funciona como un
-- prestamo. No tiene cuota fija ni un numero de cuotas; se paga lo que se
-- debe, todo o una parte. Forzarla al modelo de prestamo obligaba al usuario a
-- inventarse una cuota que no existe.
--
-- QUE PASA CON LOS DATOS QUE YA EXISTEN
-- Las obligaciones que hoy tengan tipo TARJETA_CREDITO pasan a 'OTRO'. No se
-- borra ninguna fila ni ningun pago: solo cambia la etiqueta del tipo, asi que
-- el historial, los intereses y el saldo se conservan intactos. Borrarlas seria
-- destruir informacion que el usuario registro a mano.
-- =============================================================================

-- Primero los datos, despues la restriccion: al revés, el UPDATE fallaria
-- contra la restriccion nueva.
UPDATE obligaciones
   SET tipo = 'OTRO'
 WHERE tipo = 'TARJETA_CREDITO';

ALTER TABLE obligaciones DROP CONSTRAINT ck_obligaciones_tipo;

ALTER TABLE obligaciones
    ADD CONSTRAINT ck_obligaciones_tipo CHECK (tipo IN
        ('PRESTAMO_BANCARIO','PRESTAMO_PERSONAL','CREDITO_HIPOTECARIO',
         'CREDITO_VEHICULO','OTRO'));

COMMENT ON COLUMN obligaciones.tipo IS
    'Creditos y prestamos con cuota pactada. Las tarjetas de credito NO van '
    'aqui: son cuentas de tipo TARJETA_CREDITO (V6)';
