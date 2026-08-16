# Contrato de API — Modulo de cuentas

Version 1.0 · 16/08/2026 · Requisitos RF-006, RF-007, RF-008

Este documento existe para que el backend y el frontend se construyan al mismo
tiempo sin esperarse. Es la unica fuente de verdad del modulo: si algo cambia
aqui, cambia en los dos lados.

- Backend (Kevin): `finmind-estructura`, rama `feature/cuentas`
- Frontend (Luis): `Finmind-luis`, rama `feature/pantalla-cuentas`

---

## 1. Regla que atraviesa todo el modulo

**RN-005 — Aislamiento de datos financieros.** El usuario sale del token, nunca
del cuerpo ni de la URL. Un `usuarioId` enviado por el cliente se ignora.

Consecuencia en las respuestas: pedir una cuenta ajena devuelve **404**, no 403.
Un 403 confirmaria que esa cuenta existe y de quien es. El 404 no dice nada.

---

## 2. Endpoints

| ID | Metodo | Ruta | Proposito |
|----|--------|------|-----------|
| API-15 | POST | `/api/v1/cuentas` | Crear una cuenta |
| API-04 | GET | `/api/v1/cuentas` | Listar las cuentas propias con saldo |
| API-16 | GET | `/api/v1/cuentas/{id}` | Consultar una cuenta |
| API-17 | PUT | `/api/v1/cuentas/{id}` | Editar nombre y tipo |
| API-18 | PATCH | `/api/v1/cuentas/{id}/desactivar` | Desactivar |
| API-19 | PATCH | `/api/v1/cuentas/{id}/activar` | Reactivar |

Todos exigen `Authorization: Bearer <token>`.

---

## 3. Cuerpos

### CuentaResponse — lo que devuelve el servidor

```json
{
  "id": 12,
  "nombre": "Cuenta de ahorros Bancolombia",
  "tipo": "AHORROS",
  "saldoInicial": 1500000.00,
  "saldoActual": 1735000.00,
  "moneda": "COP",
  "activa": true,
  "fechaCreacion": "2026-08-16T14:32:10"
}
```

`saldoInicial` es lo que el usuario declaro al crearla y no cambia.
`saldoActual` lo calcula el servidor: saldo inicial + ingresos − gastos.
El frontend **no** hace esa cuenta; muestra lo que llega.

### Crear — POST

```json
{ "nombre": "Cuenta de ahorros", "tipo": "AHORROS", "saldoInicial": 1500000.00, "moneda": "COP" }
```

| Campo | Obligatorio | Regla |
|-------|-------------|-------|
| `nombre` | si | 1 a 80 caracteres, unico dentro de las cuentas del usuario |
| `tipo` | si | uno de los seis valores de la seccion 4 |
| `saldoInicial` | no (0 por defecto) | 0 o mayor, maximo 2 decimales (RN-010) |
| `moneda` | no (COP por defecto) | exactamente 3 letras mayusculas |

### Editar — PUT

```json
{ "nombre": "Ahorros Bancolombia", "tipo": "AHORROS" }
```

Solo nombre y tipo. **El saldo inicial y la moneda no se editan**: cambiarlos
alteraria retroactivamente todos los saldos ya calculados y los movimientos
registrados dejarian de cuadrar.

---

## 4. Valores de `tipo`

`EFECTIVO` · `AHORROS` · `CORRIENTE` · `TARJETA_CREDITO` · `BILLETERA_DIGITAL` · `OTRO`

Etiquetas para mostrar en pantalla:

| Valor | Etiqueta |
|-------|----------|
| EFECTIVO | Efectivo |
| AHORROS | Cuenta de ahorros |
| CORRIENTE | Cuenta corriente |
| TARJETA_CREDITO | Tarjeta de credito |
| BILLETERA_DIGITAL | Billetera digital |
| OTRO | Otro |

---

## 5. Codigos de respuesta

| Codigo | Cuando |
|--------|--------|
| 200 | Consulta, edicion, activacion o desactivacion correcta |
| 201 | Cuenta creada |
| 400 | Datos invalidos. El cuerpo trae `fieldErrors` con el detalle por campo |
| 401 | Falta el token o no sirve |
| 404 | La cuenta no existe **o es de otro usuario** (ver seccion 1) |
| 409 | Ya existe una cuenta propia con ese nombre |

Formato de error (el mismo de toda la API):

```json
{
  "timestamp": "2026-08-16T14:32:10-05:00",
  "status": 409,
  "error": "Conflict",
  "message": "Ya tienes una cuenta con ese nombre",
  "path": "/api/v1/cuentas",
  "fieldErrors": null
}
```

---

## 6. Parametros del listado

`GET /api/v1/cuentas?incluirInactivas=true`

Por defecto **false**: solo cuentas activas. La pantalla necesita poder mostrar
las inactivas para reactivarlas.

---

## 7. Por que se desactiva y no se borra

Las cuentas inactivas no se eliminan. Los movimientos apuntan a la cuenta; si se
borrara, el historial financiero del usuario quedaria incompleto y los reportes
de meses anteriores cambiarian solos. Desactivar la saca de las listas de
seleccion sin tocar lo ya registrado.
