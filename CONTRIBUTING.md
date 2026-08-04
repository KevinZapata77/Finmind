# Flujo de trabajo — FinMind

Estas reglas son de cumplimiento obligatorio para el equipo. El historial del repositorio
es evidencia evaluable del proyecto.

## Ramas

| Rama | Propósito | Quién escribe |
|---|---|---|
| `main` | Código estable y desplegable. Solo recibe merge desde `develop`. | Solo el líder |
| `develop` | Rama de integración. Base de todo trabajo nuevo. | Vía Pull Request |
| `feature/<módulo>-<descripción>` | Una funcionalidad o historia de usuario. | Cada integrante |
| `fix/<descripción>` | Corrección de un defecto. | Cada integrante |
| `docs/<descripción>` | Documentación y artefactos. | Cada integrante |

**Nadie hace commit directo a `main` ni a `develop`.**

Ejemplos válidos:

```
feature/transacciones-registrar-gasto
feature/auth-login-jwt
fix/presupuestos-calculo-saldo
docs/doc-03-alcance-requisitos
```

### Ciclo de una funcionalidad

```bash
git checkout develop
git pull origin develop
git checkout -b feature/transacciones-registrar-gasto
# ... trabajar, commits pequeños ...
git push -u origin feature/transacciones-registrar-gasto
# abrir Pull Request contra develop en GitHub
```

## Convención de commits (Conventional Commits)

```
<tipo>(<alcance>): <descripción en imperativo, minúscula, sin punto final>
```

Tipos permitidos:

| Tipo | Uso |
|---|---|
| `feat` | Nueva funcionalidad |
| `fix` | Corrección de un defecto |
| `docs` | Documentación |
| `test` | Pruebas |
| `refactor` | Cambio interno sin alterar el comportamiento |
| `style` | Formato, sin efecto funcional |
| `chore` | Configuración, dependencias, build |
| `db` | Migraciones y cambios de esquema |

Alcances: `auth`, `usuarios`, `cuentas`, `categorias`, `transacciones`, `presupuestos`,
`metas`, `reportes`, `admin`, `common`, `ci`, `docs`.

Ejemplos:

```
feat(transacciones): registrar gasto asociado a categoria y cuenta
fix(presupuestos): corregir suma de gastos al cambiar de mes
db(transacciones): agregar indice por usuario y fecha
docs(docs): completar DOC-03 alcance y requisitos
test(auth): cubrir login con credenciales invalidas
```

Si el commit implementa una historia de usuario o un requisito, se referencia en el cuerpo:

```
feat(metas): permitir abonar a una meta de ahorro

Implementa HU-14 / RF-18.
```

## Pull Requests

- Se abren contra `develop`, nunca contra `main`.
- Título con la misma convención del commit.
- Requieren **al menos una revisión aprobada** de otro integrante.
- No se hace merge si las pruebas fallan.
- Se usa **Squash and merge** para mantener el historial legible.

## Reglas de base de datos

- Todo cambio de esquema entra como una nueva migración Flyway: `V<n>__descripcion.sql`.
- Una migración ya publicada en `develop` **no se edita nunca**.
- Kelin Montoya revisa y aprueba cualquier PR que toque `db/migration/`.

## Reglas de seguridad

- Ningún secreto, credencial ni clave se sube al repositorio. Usar `.env` (ignorado por git).
- Si un secreto se filtra por error: rotarlo de inmediato, no basta con borrarlo del commit.
