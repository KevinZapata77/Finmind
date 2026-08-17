-- =============================================================================
-- V4: CHAR(n) pasa a VARCHAR(n) en las tres columnas de texto de longitud fija
--
-- POR QUE
-- El esquema declaraba CHAR(n) y las entidades JPA declaran String con length.
-- Hibernate espera VARCHAR y en produccion corre con ddl-auto=validate, asi que
-- la aplicacion NO ARRANCABA:
--
--   Schema-validation: wrong column type encountered in column [color_hex]
--   found [bpchar (Types#CHAR)], but expecting [varchar(7) (Types#VARCHAR)]
--
-- Se corrige la base y no las entidades porque VARCHAR es lo correcto aqui:
-- CHAR rellena con espacios hasta completar la longitud, y un valor con espacios
-- invisibles al final es una fuente clasica de comparaciones que fallan sin
-- motivo aparente. Ninguno de los tres campos gana nada con el relleno.
--
-- La conversion es segura: los valores existentes ya ocupan exactamente la
-- longitud declarada ('COP', '#0E8368', seis digitos), asi que no hay espacios
-- que arrastrar.
-- =============================================================================

ALTER TABLE cuentas
    ALTER COLUMN moneda TYPE VARCHAR(3);

ALTER TABLE categorias
    ALTER COLUMN color_hex TYPE VARCHAR(7);

ALTER TABLE codigos_verificacion
    ALTER COLUMN codigo TYPE VARCHAR(6);
