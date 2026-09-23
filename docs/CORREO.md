# El envío de correo en producción

## El problema, y por qué costó encontrarlo

Los códigos de verificación dejaron de llegar. El error en el servidor era:

```
No se pudo enviar el correo a ... desde kevxnz2007@gmail.com:
Mail server connection failed. Couldn't connect to host, port: smtp.gmail.com, 587; timeout 5000
```

Lo importante de esa línea es lo que **no** dice. No dice "contraseña incorrecta", no dice
"remitente rechazado". Dice que la conexión no se pudo establecer y que expiró el tiempo de
espera. Con las credenciales correctas y el remitente correcto.

**La causa no está en la aplicación: Render bloquea el tráfico saliente a los puertos SMTP
25, 465 y 587 en los servicios del plan gratuito, desde el 26 de septiembre de 2025.** No
rechaza la conexión —eso daría un error inmediato—, descarta los paquetes en silencio, y por
eso lo que se ve es un timeout seco.

Es la clase de fallo que hace perder horas, porque los dos primeros lugares donde uno mira
—credenciales y configuración— están bien.

## La solución: puerto 2525

El puerto 2525 no está bloqueado. No es un puerto oficial de la IETF, pero los proveedores de
correo lo ofrecen justamente como alternativa para redes restringidas como esta.

Se cambia la configuración, **no el código**: el servidor, el puerto y las credenciales ya se
leen de variables de entorno.

### Pasos

1. Crear una cuenta gratuita en [brevo.com](https://www.brevo.com/free-smtp-server/): 300
   correos por día, sin tarjeta, y ofrece el puerto 2525.
2. **Verificar el remitente.** Menú de la cuenta (arriba a la derecha) → *Senders, Domains &
   Dedicated IPs* → *Senders* → *Add a sender*. Poner `kevinzapatajega@gmail.com` y escribir
   el código de 6 dígitos que llega a ese buzón. **Sin este paso los envíos se rechazan**: un
   relay no deja enviar en nombre de una dirección que nadie comprobó.

   > El remitente tiene que ser una dirección verificada **en esa misma cuenta de Brevo**.
   > Durante un rato se configuró `kevxnz2007@gmail.com`, que es otra dirección del mismo
   > dueño pero no la de la cuenta: Brevo aceptaba el mensaje y después no lo entregaba,
   > porque no puede firmar en nombre de algo que no comprobó. Desde afuera se veía como un
   > correo que "sale bien" y nunca llega.
3. **Copiar las credenciales SMTP.** Menú de la cuenta → *SMTP & API* → pestaña *SMTP*. Ahí
   aparecen el servidor, el *login* —con forma de `7abcde@smtp-brevo.com`, **no** el correo de
   la cuenta— y la clave. Si no hay ninguna, se genera con *Generate a new SMTP key*.
4. En Render, ajustar las variables de entorno:

```
MAIL_HOST     = smtp-relay.brevo.com
MAIL_PORT     = 2525
MAIL_USERNAME = (el usuario SMTP que muestra el panel)
MAIL_PASSWORD = (la clave SMTP generada)
MAIL_FROM     = kevinzapatajega@gmail.com
MAIL_ENABLED  = true
```

> `MAIL_USERNAME` y `MAIL_FROM` **no son lo mismo** en un relay. El usuario suele ser un
> identificador como `8a1b2c001@smtp-brevo.com`, y el remitente es la dirección verificada.
> Con Gmail coincidían, y por eso una corrección anterior los igualaba a la fuerza: eso ya
> está arreglado y ahora se respeta lo que diga `MAIL_FROM`.

5. Desplegar y revisar la primera línea del log:

```
Correo ACTIVO. Servidor=smtp-relay.brevo.com:2525 remitente=... usuario=definido clave=definida
```

Si el puerto fuera 587 o 465, el arranque avisa que probablemente esté bloqueado.

## Cómo verificar que quedó

Registrar una cuenta con un correo real. Si llega el código, listo. Si no, buscar en el log
del servidor la línea `No se pudo enviar el correo`: ahí sale el motivo exacto.

## Qué se hizo en el código

Nada para este arreglo, y es el punto. Lo único que se agregó fue **diagnóstico**: la
aplicación informa al arrancar cómo quedó configurado el correo y avisa si el puerto está en
la lista de los bloqueados. De la clave solo dice si existe, nunca su valor.

Ese aviso es la lección que dejó el defecto: cuando un correo no llega hay varios sospechosos
y desde afuera todos se ven igual —no pasa nada—. Sin una señal en el arranque, diagnosticarlo
es cambiar cosas al azar y volver a probar.

## Para la sustentación

Si preguntan por el envío de correo, esta es una buena historia y conviene contarla como es:

> El correo dejó de funcionar en producción y no era un error nuestro: la plataforma donde
> está desplegado el backend bloquea los puertos SMTP en el plan gratuito. Lo diagnosticamos
> agregando un aviso de configuración en el arranque, y lo resolvimos cambiando a un relay por
> el puerto 2525. No hubo que tocar el código, solo la configuración, porque el servidor y las
> credenciales ya estaban afuera en variables de entorno.

Eso último —que se pudiera arreglar sin recompilar— no es casualidad: es la razón por la que
la configuración no va escrita dentro del código.
