# Pasos para dejar el repositorio funcionando

Repositorio: https://github.com/KevinZapata77/Finmind (hoy está vacío)

## 1. Subir la estructura (hoy, 10 minutos)

Descomprimí `finmind-estructura.zip` y desde la carpeta resultante:

```bash
git init
git branch -M main
git remote add origin https://github.com/KevinZapata77/Finmind.git

git add .
git commit -m "chore(common): estructura inicial del proyecto Spring Boot"
git push -u origin main
```

Verificá en GitHub que `.env` **no** aparezca (solo `.env.example`).

## 2. Crear la rama `develop`

```bash
git checkout -b develop
git push -u origin develop
```

Dejá `develop` como rama por defecto en GitHub:
**Settings → General → Default branch → Switch to `develop`**.
Así nadie hace un PR contra `main` por descuido.

## 3. Proteger `main` (evidencia de buenas prácticas)

**Settings → Branches → Add branch protection rule**, patrón `main`:

- Require a pull request before merging
- Require approvals: 1
- Require status checks to pass → `Compilar y probar`
- Do not allow bypassing the above settings

Repetí lo mismo para `develop` con approvals: 1.

## 4. Invitar a Luis y Kelin (mañana)

**Settings → Collaborators and teams → Add people**

- Luis Méndez — permiso **Write**
- Kelin Montoya — permiso **Write**

Necesitás sus usuarios de GitHub o los correos con los que se registraron.
Ojo: la invitación caduca a los 7 días, avisales que la acepten el mismo día.

## 5. Configurar la base de datos local

```sql
CREATE DATABASE finmind CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'finmind_user'@'localhost' IDENTIFIED BY 'la_clave_que_elijas';
GRANT ALL PRIVILEGES ON finmind.* TO 'finmind_user'@'localhost';
FLUSH PRIVILEGES;
```

Después:

```bash
cp .env.example .env    # completar con esas credenciales
mvn clean install
mvn spring-boot:run
```

Flyway aplica `V1__esquema_inicial.sql` automáticamente al arrancar.
Si arranca bien, entrá a http://localhost:8080/swagger-ui.html

## 6. Limpieza pendiente

Borrá el archivo `src/test/resources_placeholder.txt` si aparece en el zip
(quedó por una restricción del entorno donde armé la estructura).

## Advertencia sobre lo que NO está verificado

En mi entorno solo hay JDK 11 y no pude instalar MySQL, así que **no compilé el proyecto
ni ejecuté las migraciones**. Validé el `pom.xml` como XML bien formado, los tres YAML
como YAML válido, y el SQL con un parser de dialecto MySQL. Eso no equivale a un
`mvn clean install` verde. El primer arranque local es la prueba real: si algo falla,
lo más probable es una versión de dependencia, y se corrige en minutos.
