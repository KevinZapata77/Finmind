-- V12 — La cuenta de efectivo que les falta a los usuarios ya existentes.
--
-- EL PROBLEMA (DEF-030, segunda parte)
-- El registro con Google no creaba la cuenta de efectivo inicial. Eso ya quedo
-- corregido en el codigo, pero corregir el codigo solo arregla los registros
-- que vengan de ahora en adelante: las cuentas que se crearon mientras el
-- defecto existia siguen sin ninguna cuenta, y para esas personas la
-- aplicacion sigue rota igual que antes.
--
-- Un arreglo que no alcanza a los datos que el defecto ya produjo no esta
-- terminado. Esto los alcanza.
--
-- QUE HACE
-- A cada usuario que no tenga NI UNA cuenta, le crea la de efectivo con saldo
-- cero, identica a la que habria recibido al registrarse.
--
-- POR QUE "NI UNA" Y NO "ninguna llamada Efectivo"
-- Porque quien ya se creo sus propias cuentas no necesita que le agreguemos
-- una: seria material ajeno apareciendo en su pantalla sin que lo pidiera. La
-- condicion busca al que quedo sin nada, que es el unico que esta bloqueado.
--
-- ES IDEMPOTENTE
-- Si se corriera dos veces, la segunda no insertaria nada: despues de la
-- primera ya nadie cumple la condicion. Ademas existe la restriccion unica
-- (usuario_id, nombre), que impediria un duplicado aunque la logica fallara.

INSERT INTO cuentas (usuario_id, nombre, tipo, saldo_inicial, moneda, activa, fecha_creacion)
SELECT u.id, 'Efectivo', 'EFECTIVO', 0.00, 'COP', TRUE, CURRENT_TIMESTAMP
FROM usuarios u
WHERE NOT EXISTS (
    SELECT 1 FROM cuentas c WHERE c.usuario_id = u.id
);
