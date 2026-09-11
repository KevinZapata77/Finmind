# Contrato de API — FinMind

El contrato se genera desde el código con springdoc-openapi.

- Interfaz interactiva: `http://localhost:8080/swagger-ui.html`
- Especificación JSON: `http://localhost:8080/v3/api-docs`

## Exportar el contrato para el frontend

Con la aplicación corriendo:

```bash
curl http://localhost:8080/v3/api-docs -o docs/api/openapi.json
```

Este archivo es el acuerdo entre backend y frontend. Cada vez que cambie un endpoint,
se exporta de nuevo y se sube en el mismo PR, para que Luis no quede bloqueado
esperando información verbal.

## Colección de pruebas manuales (Postman / Bruno)

`FinMind.postman_collection.json` — los 62 endpoints, organizados en las once carpetas
del sistema, con el cuerpo de ejemplo y la explicación de cada regla de negocio que
sostiene el endpoint.

**Postman:** `Import` → arrastrar el archivo.
**Bruno:** `Collection` → `Import` → `Postman Collection` → elegir el archivo.

Cómo se usa:

1. Arrancar el backend en `http://localhost:8080`.
2. Ejecutar **00 Identidad y acceso → Iniciar sesión**. El script de prueba de esa
   petición guarda el token en la variable `{{token}}`, así que el resto de la
   colección ya sale autenticada sin copiar nada a mano.
3. Ejecutar cualquier otra petición. Las que crean algo guardan el id devuelto en su
   propia variable (`{{cuentaId}}`, `{{categoriaId}}`, `{{gastoFijoId}}`…), de modo que
   recorrer las carpetas de arriba abajo funciona sin editar URLs.

La colección manda el token en el encabezado `Authorization`. El navegador no lo hace:
usa la cookie `HttpOnly`, que ningún script puede leer. El backend acepta los dos
caminos a propósito — la cookie protege al navegador de un XSS, y el encabezado mantiene
vivos los clientes que no son navegador, como esta colección, Swagger y las 189 pruebas
automatizadas.

Las variables `correo` y `contrasena` vienen con valores de ejemplo. Se cambian en
`Variables` de la colección, no dentro de cada petición.

> No se versiona ninguna credencial real en este archivo. Las claves viven en los `.env`,
> que están ignorados por git.

## Convenciones de la API

- Prefijo de todos los endpoints: `/api/v1`
- Autenticación: `Authorization: Bearer <token JWT>`
- Un usuario autenticado solo accede a sus propios recursos. El identificador de usuario
  se toma del token, **nunca** de la URL ni del cuerpo de la petición.
- Errores: respuesta uniforme (`ApiError`) con `timestamp`, `status`, `error`, `message`,
  `path` y `fieldErrors` cuando corresponde.
- Fechas en formato ISO-8601. Montos como `DECIMAL(15,2)`, nunca `float` ni `double`.
- Listados paginados con `?page=&size=&sort=`.
