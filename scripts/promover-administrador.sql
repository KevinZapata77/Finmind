-- ---------------------------------------------------------------------------
--  FinMind — arranque del primer administrador
--
--  POR QUE HACE FALTA ESTE ARCHIVO
--  Ninguna migracion crea un usuario administrador, y es a proposito: sembrar
--  un admin con contrasena conocida en el control de versiones es publicar una
--  llave maestra. El registro tampoco sirve para esto — AuthService.registrar
--  asigna ROLE_USUARIO siempre, y no acepta el rol desde el cliente, porque si
--  lo aceptara cualquiera se registraria como administrador cambiando un campo
--  del JSON.
--
--  Queda entonces un hueco real: el rol ROLE_ADMIN existe en la tabla roles
--  desde la migracion V1, pero nadie lo tiene. El primer administrador se
--  promueve a mano, una sola vez, y esa decision queda con nombre y fecha.
--
--  DONDE SE CORRE
--  En la consola SQL de Neon: console.neon.tech -> el proyecto -> SQL Editor.
--  Tambien sirve psql con la misma cadena de conexion del .env.
--
--  QUE NO HACE ESTE ARCHIVO
--  No crea el usuario. La cuenta tiene que existir Y tener el correo
--  verificado: un usuario sin verificar no puede iniciar sesion, y promoverlo
--  solo produce un administrador que no puede entrar.
-- ---------------------------------------------------------------------------


-- PASO 1. Mirar a quien se va a promover, antes de tocar nada.
--
-- correo_verificado tiene que salir true. Si sale false, hay que verificar la
-- cuenta primero con el codigo que llega al correo.
SELECT u.id,
       u.correo,
       u.nombre,
       u.apellido,
       u.correo_verificado,
       u.activo,
       r.nombre AS rol
FROM usuarios u
JOIN roles r ON r.id = u.rol_id
ORDER BY u.id;


-- PASO 2. Promover.
--
-- Cambiar el correo por el de la cuenta elegida. Va entre comillas simples.
--
-- NO promuevas la cuenta que tiene los datos de demostracion: esa se necesita
-- como usuario normal para las figuras del Manual de Usuario. Usa una segunda.
UPDATE usuarios
SET rol_id = (SELECT id FROM roles WHERE nombre = 'ROLE_ADMIN')
WHERE correo = 'CAMBIA-ESTO@ejemplo.com';


-- PASO 3. Comprobar que quedo.
--
-- Tiene que aparecer ROLE_ADMIN junto al correo elegido, y ROLE_USUARIO en los
-- demas. Si el UPDATE dice "0 rows", el correo estaba mal escrito.
SELECT u.correo, r.nombre AS rol
FROM usuarios u
JOIN roles r ON r.id = u.rol_id
ORDER BY r.nombre, u.correo;


-- ---------------------------------------------------------------------------
--  PASO 4, Y ES EL QUE SE OLVIDA: CERRAR SESION Y VOLVER A ENTRAR
--
--  El rol viaja DENTRO del token, no se consulta en cada peticion. Si ya tenias
--  la sesion abierta con esa cuenta, tu token sigue diciendo ROLE_USUARIO
--  aunque la base ya diga otra cosa, y /api/v1/admin/** te va a responder 403.
--
--  Cerrar sesion y entrar de nuevo emite un token con el rol nuevo.
--
--  Es el precio de una API sin estado: se gana no tener que consultar la base
--  en cada peticion, se pierde poder cambiar permisos en caliente. Con una
--  vigencia de una hora, el desfase dura como mucho una hora.
-- ---------------------------------------------------------------------------


-- ---------------------------------------------------------------------------
--  PARA DEVOLVER A USUARIO NORMAL
--
--  Util si se promovio la cuenta equivocada. Mismo aviso del paso 4: hay que
--  volver a iniciar sesion para que el token pierda el rol.
-- ---------------------------------------------------------------------------
-- UPDATE usuarios
-- SET rol_id = (SELECT id FROM roles WHERE nombre = 'ROLE_USUARIO')
-- WHERE correo = 'CAMBIA-ESTO@ejemplo.com';


-- ---------------------------------------------------------------------------
--  QUE VE Y QUE NO VE UN ADMINISTRADOR  (RF-023, RF-024, RNF-004)
--
--  Ve:     el listado de usuarios, el resumen agregado de la plataforma, y la
--          auditoria de acciones administrativas.
--  Puede:  activar y desactivar cuentas.
--  NO ve:  ningun dato financiero de ningun usuario. Ni movimientos, ni saldos,
--          ni presupuestos, ni metas. No es una limitacion de la interfaz: los
--          endpoints de /api/v1/admin no consultan esas tablas.
--
--  Desactivar no borra. Una cuenta desactivada no puede entrar, y su historico
--  financiero queda intacto: borrarla se llevaria por delante el registro
--  financiero de una persona.
-- ---------------------------------------------------------------------------
