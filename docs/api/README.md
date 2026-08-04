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

## Convenciones de la API

- Prefijo de todos los endpoints: `/api/v1`
- Autenticación: `Authorization: Bearer <token JWT>`
- Un usuario autenticado solo accede a sus propios recursos. El identificador de usuario
  se toma del token, **nunca** de la URL ni del cuerpo de la petición.
- Errores: respuesta uniforme (`ApiError`) con `timestamp`, `status`, `error`, `message`,
  `path` y `fieldErrors` cuando corresponde.
- Fechas en formato ISO-8601. Montos como `DECIMAL(15,2)`, nunca `float` ni `double`.
- Listados paginados con `?page=&size=&sort=`.
