-- =====================================================================
-- DEF-20 y DEF-21. El dia de pago tiene que estar en el rango que su
-- periodicidad admite.
--
-- QUE PASO
-- V9 dejo dia_pago con una sola restriccion, BETWEEN 1 AND 28, porque
-- entonces el calculo del proximo pago ni siquiera miraba el campo en los
-- compromisos semanales. Al corregir ese calculo (DEF-19), el campo pasa a
-- significar cosas distintas segun la periodicidad:
--
--     MENSUAL     dia del mes,        1 a 28
--     QUINCENAL   primer pago del mes, 1 a 13  (el segundo es +15, o sea <= 28)
--     SEMANAL     dia de la semana,    1 a 7   (1 lunes ... 7 domingo)
--
-- Los registros creados antes quedaron con valores fuera de esos rangos: un
-- compromiso semanal podia tener dia_pago = 20. Al leerlo, DayOfWeek.of(20)
-- lanza excepcion y el listado entero responde 500, asi que el usuario deja
-- de ver TODOS sus compromisos por culpa de uno.
--
-- POR QUE SE ENVUELVE Y NO SE RECORTA
-- Recortar con LEAST amontonaria todos los valores altos en el mismo dia:
-- 8, 15 y 22 terminarian los tres en 7 (domingo). El modulo los reparte
-- (8 -> 1, 15 -> 1, 22 -> 1 ... en realidad 8->1, 9->2, ...), que no acierta
-- la intencion original —esa intencion nunca existio, el campo se ignoraba—
-- pero al menos no concentra todo en un dia.
--
-- El valor no se puede adivinar porque nunca significo nada: lo que importa
-- es que quede dentro de rango, que el listado vuelva a cargar, y que el
-- usuario pueda corregirlo desde la pantalla ahora que el campo si se usa.
-- =====================================================================

-- Semanales: cualquier cosa fuera de 1..7 se envuelve dentro de la semana.
UPDATE gastos_fijos
   SET dia_pago = ((dia_pago - 1) % 7) + 1
 WHERE periodicidad = 'SEMANAL'
   AND dia_pago NOT BETWEEN 1 AND 7;

-- Quincenales: el primer pago no puede pasar del 13, para que el segundo
-- (a los quince dias) caiga como maximo el 28 y exista en febrero.
UPDATE gastos_fijos
   SET dia_pago = ((dia_pago - 1) % 13) + 1
 WHERE periodicidad = 'QUINCENAL'
   AND dia_pago NOT BETWEEN 1 AND 13;

-- Mensuales: V9 ya los tenia acotados a 1..28; este UPDATE es por si acaso
-- y no deberia tocar ninguna fila.
UPDATE gastos_fijos
   SET dia_pago = ((dia_pago - 1) % 28) + 1
 WHERE periodicidad = 'MENSUAL'
   AND dia_pago NOT BETWEEN 1 AND 28;

-- La restriccion por periodicidad se deja escrita en la base y no solo en el
-- servicio: si algun dia entra un dato por otra via —un script, una carga
-- manual— la base lo rechaza en vez de dejar que reviente al leerlo.
ALTER TABLE gastos_fijos DROP CONSTRAINT IF EXISTS ck_fijos_dia;

ALTER TABLE gastos_fijos ADD CONSTRAINT ck_fijos_dia CHECK (
    (periodicidad = 'SEMANAL'   AND dia_pago BETWEEN 1 AND 7)  OR
    (periodicidad = 'QUINCENAL' AND dia_pago BETWEEN 1 AND 13) OR
    (periodicidad = 'MENSUAL'   AND dia_pago BETWEEN 1 AND 28)
);
