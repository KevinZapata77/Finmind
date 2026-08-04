-- =====================================================================
-- FinMind - Esquema inicial
-- Proyecto ADSO - SENA Centro Tecnologico de Manufactura Avanzada (CTMA)
-- Ficha 3114227
-- Responsable de base de datos: Kelin Montoya
-- Motor: MySQL 8.x  /  Charset: utf8mb4  /  Zona horaria: America/Bogota
-- NOTA: borrador de arranque. Debe validarse contra el modelo E-R del DOC-04
--       antes de considerarse definitivo.
-- =====================================================================

CREATE TABLE roles (
    id          TINYINT UNSIGNED NOT NULL AUTO_INCREMENT,
    nombre      VARCHAR(30)  NOT NULL,
    descripcion VARCHAR(150) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_roles_nombre (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO roles (nombre, descripcion) VALUES
    ('ROLE_USUARIO', 'Usuario final: gestiona unicamente sus propios datos financieros'),
    ('ROLE_ADMIN',   'Administrador: gestion de plataforma. NO accede a datos financieros de usuarios (RF-26)');

CREATE TABLE usuarios (
    id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    nombre            VARCHAR(80)  NOT NULL,
    apellido          VARCHAR(80)  NOT NULL,
    correo            VARCHAR(120) NOT NULL,
    contrasena_hash   VARCHAR(100) NOT NULL,
    rol_id            TINYINT UNSIGNED NOT NULL,
    activo            BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    ultimo_acceso     DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_usuarios_correo (correo),
    CONSTRAINT fk_usuarios_rol FOREIGN KEY (rol_id) REFERENCES roles (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE cuentas (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    usuario_id     BIGINT UNSIGNED NOT NULL,
    nombre         VARCHAR(80) NOT NULL,
    tipo           ENUM('EFECTIVO','AHORROS','CORRIENTE','TARJETA_CREDITO','BILLETERA_DIGITAL','OTRO') NOT NULL,
    saldo_inicial  DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    moneda         CHAR(3) NOT NULL DEFAULT 'COP',
    activa         BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_cuentas_usuario (usuario_id),
    UNIQUE KEY uk_cuentas_usuario_nombre (usuario_id, nombre),
    CONSTRAINT fk_cuentas_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE categorias (
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    usuario_id   BIGINT UNSIGNED NULL COMMENT 'NULL = categoria del sistema, visible para todos',
    nombre       VARCHAR(60) NOT NULL,
    tipo         ENUM('INGRESO','GASTO') NOT NULL,
    icono        VARCHAR(40) NULL,
    color_hex    CHAR(7) NULL,
    activa       BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    KEY idx_categorias_usuario (usuario_id),
    UNIQUE KEY uk_categorias_usuario_nombre_tipo (usuario_id, nombre, tipo),
    CONSTRAINT fk_categorias_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE transacciones (
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    usuario_id   BIGINT UNSIGNED NOT NULL,
    cuenta_id    BIGINT UNSIGNED NOT NULL,
    categoria_id BIGINT UNSIGNED NOT NULL,
    tipo         ENUM('INGRESO','GASTO') NOT NULL,
    monto        DECIMAL(15,2) NOT NULL,
    fecha        DATE NOT NULL,
    descripcion  VARCHAR(255) NULL,
    fecha_registro DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_transacciones_usuario_fecha (usuario_id, fecha),
    KEY idx_transacciones_cuenta (cuenta_id),
    KEY idx_transacciones_categoria (categoria_id),
    CONSTRAINT fk_transacciones_usuario   FOREIGN KEY (usuario_id)   REFERENCES usuarios (id)   ON DELETE CASCADE,
    CONSTRAINT fk_transacciones_cuenta    FOREIGN KEY (cuenta_id)    REFERENCES cuentas (id)    ON DELETE CASCADE,
    CONSTRAINT fk_transacciones_categoria FOREIGN KEY (categoria_id) REFERENCES categorias (id),
    CONSTRAINT ck_transacciones_monto CHECK (monto > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE presupuestos (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    usuario_id    BIGINT UNSIGNED NOT NULL,
    categoria_id  BIGINT UNSIGNED NOT NULL,
    monto_limite  DECIMAL(15,2) NOT NULL,
    periodo       ENUM('MENSUAL','QUINCENAL','SEMANAL') NOT NULL DEFAULT 'MENSUAL',
    anio          SMALLINT UNSIGNED NOT NULL,
    mes           TINYINT UNSIGNED NOT NULL,
    activo        BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    UNIQUE KEY uk_presupuestos_periodo (usuario_id, categoria_id, anio, mes),
    CONSTRAINT fk_presupuestos_usuario   FOREIGN KEY (usuario_id)   REFERENCES usuarios (id) ON DELETE CASCADE,
    CONSTRAINT fk_presupuestos_categoria FOREIGN KEY (categoria_id) REFERENCES categorias (id),
    CONSTRAINT ck_presupuestos_monto CHECK (monto_limite > 0),
    CONSTRAINT ck_presupuestos_mes   CHECK (mes BETWEEN 1 AND 12)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE metas_ahorro (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    usuario_id    BIGINT UNSIGNED NOT NULL,
    nombre        VARCHAR(80) NOT NULL,
    monto_objetivo DECIMAL(15,2) NOT NULL,
    monto_actual   DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    fecha_limite   DATE NULL,
    estado         ENUM('EN_CURSO','COMPLETADA','CANCELADA') NOT NULL DEFAULT 'EN_CURSO',
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_metas_usuario (usuario_id),
    CONSTRAINT fk_metas_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE,
    CONSTRAINT ck_metas_objetivo CHECK (monto_objetivo > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Auditoria de acciones administrativas. Sustenta RF-26: se registra la
-- accion del admin sin exponer datos financieros del usuario.
CREATE TABLE auditoria_admin (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    admin_id        BIGINT UNSIGNED NOT NULL,
    accion          VARCHAR(60) NOT NULL,
    entidad         VARCHAR(60) NOT NULL,
    entidad_id      BIGINT UNSIGNED NULL,
    detalle         VARCHAR(255) NULL,
    fecha           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_auditoria_admin_fecha (admin_id, fecha),
    CONSTRAINT fk_auditoria_admin FOREIGN KEY (admin_id) REFERENCES usuarios (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Categorias del sistema (usuario_id NULL)
INSERT INTO categorias (usuario_id, nombre, tipo, icono, color_hex) VALUES
    (NULL, 'Salario',          'INGRESO', 'wallet',    '#2E7D32'),
    (NULL, 'Otros ingresos',   'INGRESO', 'plus',      '#66BB6A'),
    (NULL, 'Alimentacion',     'GASTO',   'utensils',  '#EF6C00'),
    (NULL, 'Transporte',       'GASTO',   'bus',       '#1565C0'),
    (NULL, 'Vivienda',         'GASTO',   'home',      '#6A1B9A'),
    (NULL, 'Servicios',        'GASTO',   'bolt',      '#00838F'),
    (NULL, 'Salud',            'GASTO',   'heart',     '#C62828'),
    (NULL, 'Educacion',        'GASTO',   'book',      '#4527A0'),
    (NULL, 'Entretenimiento',  'GASTO',   'film',      '#AD1457'),
    (NULL, 'Otros gastos',     'GASTO',   'dots',      '#546E7A');
