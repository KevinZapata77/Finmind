# FinMind — Sistema de Gestión Financiera Personal

Aplicación web para que las personas registren, organicen y controlen sus finanzas personales:
movimientos de ingreso y gasto, presupuestos por categoría, metas de ahorro y reportes de balance.

Proyecto final — Tecnología en Análisis y Desarrollo de Software (ADSO)
SENA · Centro Tecnológico de Manufactura Avanzada (CTMA) · Ficha **3114227** · Medellín
Instructor de seguimiento: Juan Carlos Quintero

## Equipo

| Integrante | Rol | Responsabilidad técnica |
|---|---|---|
| Kevin Andrés Zapata Murillo | Líder de proyecto · Backend | API REST, Java 21 + Spring Boot |
| Luis Méndez | Frontend | SPA en React, consumo de la API |
| Kelin Montoya | Base de datos · Testing | Modelo MySQL, migraciones, pruebas |

## Stack

- **Backend:** Java 21, Spring Boot 3.4, Spring Web, Spring Data JPA, Spring Security + JWT
- **Base de datos:** MySQL 8, migraciones versionadas con Flyway
- **Documentación de API:** OpenAPI 3 / Swagger UI (springdoc)
- **Pruebas:** JUnit 5, Spring Boot Test, H2 en memoria
- **Frontend:** React (repositorio/carpeta a cargo de Luis)
- **Build:** Maven

## Requisitos previos

- JDK 21
- Maven 3.9+
- MySQL 8 con una base de datos `finmind` creada

## Puesta en marcha

```bash
git clone https://github.com/KevinZapata77/Finmind.git
cd Finmind
cp .env.example .env      # completar credenciales locales
mvn clean install
mvn spring-boot:run
```

Variables de entorno: ver `.env.example`. **Nunca** se suben credenciales al repositorio.

Una vez arriba:

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Contrato OpenAPI: `http://localhost:8080/v3/api-docs`

## Estructura del proyecto

```
src/main/java/com/finmind/
├── FinmindApplication.java
├── auth/            autenticación, registro, JWT
├── usuarios/        perfil del usuario
├── cuentas/         cuentas y saldos
├── categorias/      categorías de ingreso y gasto
├── transacciones/   registro de movimientos
├── presupuestos/    límites de gasto por categoría y periodo
├── metas/           metas de ahorro
├── reportes/        balances y consolidados
├── admin/           gestión de plataforma (sin acceso a datos financieros, RF-26)
└── common/          config, seguridad, manejo de errores, utilidades
```

Cada módulo sigue el mismo corte por capas: `controller` → `service` → `repository`, con
`entity`, `dto` y `mapper` propios.

Migraciones de base de datos: `src/main/resources/db/migration/`.
Se aplican automáticamente con Flyway al arrancar. **Una migración publicada no se edita:**
se corrige con una nueva (`V2__...`, `V3__...`).

## Decisión de diseño: rol Administrador (RF-26)

El administrador **no puede acceder ni modificar los datos financieros de ningún usuario**.
Sus funciones se limitan a la gestión de la plataforma, y toda acción administrativa queda
registrada en la tabla `auditoria_admin`.

## Flujo de trabajo

Ramas, convención de commits y proceso de Pull Request: ver [CONTRIBUTING.md](CONTRIBUTING.md).

## Estado

Fase de construcción. Congelamiento de código: **8 de septiembre de 2026**.
Entrega y sustentación: **15 de septiembre de 2026**.
