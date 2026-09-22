-- V11 — Una cuenta puede tener contrasena Y Google a la vez.
--
-- EL PROBLEMA (DEF-024)
-- Alguien creaba su cuenta con correo y contrasena, la verificaba, y despues
-- pulsaba "Entrar con Google" con ese mismo correo. La aplicacion lo rechazaba:
-- "Ese correo ya tiene una cuenta en FinMind". Desde afuera se lee como un
-- error, porque es la misma persona y el mismo buzon.
--
-- POR QUE ESTABA ASI
-- No fue un descuido. Fusionar cuentas por el correo es un agujero conocido:
-- si cualquiera puede registrar victima@gmail.com en FinMind y esperar a que
-- la persona real entre con Google, el atacante termina con acceso a la cuenta
-- de la victima —sabe la contrasena que el mismo puso—. Rechazar era la
-- decision segura mientras no hubiera forma de distinguir los dos casos.
--
-- QUE CAMBIA AHORA
-- Si hay forma de distinguirlos, y es la verificacion del correo. Para vincular
-- se exigen DOS pruebas independientes de que quien vincula controla ese buzon:
--
--   1. La cuenta local tiene correo_verificado = TRUE. Se verifica con un
--      codigo que llega al buzon; el atacante del parrafo anterior nunca lo
--      recibe, asi que su cuenta usurpada se queda sin verificar para siempre.
--   2. Google afirma email_verified = true para ese correo.
--
-- Las dos apuntan al mismo buzon por caminos distintos. Con las dos, es la
-- misma persona. Sin alguna de las dos, se sigue rechazando.
--
-- La regla queda registrada como RN-033 y la comprueba ServicioUsuarioGoogle.

-- ------------------------------------------------------------------ credencial
-- La restriccion anterior ataba el origen de la cuenta a la credencial:
--
--     (proveedor = 'LOCAL'  AND contrasena_hash IS NOT NULL) OR
--     (proveedor = 'GOOGLE' AND contrasena_hash IS NULL)
--
-- Eso vuelve la vinculacion imposible por definicion: una cuenta nacida LOCAL
-- que suma Google seguiria teniendo su hash, y ninguna de las dos ramas la
-- admite. La base rechazaria el UPDATE aunque el codigo lo intentara.
ALTER TABLE usuarios DROP CONSTRAINT IF EXISTS ck_usuarios_credencial;

-- Lo que de verdad hay que garantizar es mas simple, y es lo unico que importa:
-- que ninguna cuenta se quede sin NINGUNA forma de entrar. Una fila sin hash y
-- sin identificador de Google es una cuenta a la que nadie puede acceder nunca
-- y que tampoco se puede recuperar.
--
-- Cuantas credenciales tenga por encima de una es asunto del usuario.
ALTER TABLE usuarios
    ADD CONSTRAINT ck_usuarios_credencial CHECK (
        contrasena_hash IS NOT NULL OR proveedor_id IS NOT NULL
    );

-- ------------------------------------------------------------------- identidad
-- El indice unico estaba sobre (proveedor, proveedor_id), y con la vinculacion
-- eso deja de proteger lo que decia proteger.
--
-- Al vincular, una cuenta nacida LOCAL conserva proveedor = 'LOCAL' y recibe el
-- identificador de Google. Su par seria ('LOCAL', '1234'), mientras que una
-- cuenta nacida con Google con ESE MISMO identificador seria ('GOOGLE', '1234').
-- Son tuplas distintas, asi que el indice las aceptaria a las dos: el mismo
-- usuario de Google apuntando a dos cuentas de FinMind, que es justo el
-- duplicado que el indice existia para impedir.
--
-- La unicidad tiene que ser del identificador solo. Un usuario de Google es uno,
-- sin importar como nacio la cuenta en FinMind.
DROP INDEX IF EXISTS uk_usuarios_proveedor_id;

CREATE UNIQUE INDEX uk_usuarios_proveedor_id
    ON usuarios (proveedor_id) WHERE proveedor_id IS NOT NULL;

-- --------------------------------------------------------------- documentacion
-- 'proveedor' pasa a significar otra cosa y conviene que quede escrito en la
-- base, no solo en el codigo: deja de ser "como entra" para ser "como nacio".
-- Quien pregunte "esta cuenta entra con Google?" debe mirar proveedor_id, y
-- "entra con contrasena?" se responde con contrasena_hash. Consultar proveedor
-- para eso da una respuesta equivocada en cuanto hay cuentas vinculadas.
COMMENT ON COLUMN usuarios.proveedor IS
    'Como nacio la cuenta: LOCAL o GOOGLE. Historico, no indica como entra hoy';

COMMENT ON COLUMN usuarios.proveedor_id IS
    'Identificador estable en Google. No nulo = la cuenta tiene Google vinculado';
