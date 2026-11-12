-- =====================================================================
-- Sistema de inventario y ventas - Fruteria Blanco
-- Esquema completo (3 sprints) para MySQL 8.0.16 o superior
-- Ejecutar completo en MySQL Workbench o con: mysql -u root -p < schema.sql
-- =====================================================================

CREATE DATABASE IF NOT EXISTS fruteria_blanco CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE fruteria_blanco;

-- ---------------------------------------------------------------------
-- SPRINT 1: seguridad, usuarios, inventario, trazabilidad
-- ---------------------------------------------------------------------

CREATE TABLE roles (
                       id_rol       INT AUTO_INCREMENT PRIMARY KEY,
                       nombre       VARCHAR(50)  NOT NULL UNIQUE,
                       descripcion  VARCHAR(200)
) ENGINE=InnoDB;

CREATE TABLE usuarios (
                          id_usuario          INT AUTO_INCREMENT PRIMARY KEY,
                          nombre              VARCHAR(100) NOT NULL,
                          correo              VARCHAR(120) NOT NULL UNIQUE,
                          contrasena_hash     VARCHAR(100) NOT NULL,
                          id_rol              INT NOT NULL,
                          activo              BOOLEAN  NOT NULL DEFAULT TRUE,
                          fecha_creacion      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          creado_por          INT NULL,
                          fecha_actualizacion DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
                          actualizado_por     INT NULL,
                          fecha_baja          DATETIME NULL,
                          dado_de_baja_por    INT NULL,
                          CONSTRAINT fk_usuarios_rol        FOREIGN KEY (id_rol)           REFERENCES roles(id_rol),
                          CONSTRAINT fk_usuarios_creado     FOREIGN KEY (creado_por)       REFERENCES usuarios(id_usuario),
                          CONSTRAINT fk_usuarios_actualiza  FOREIGN KEY (actualizado_por)  REFERENCES usuarios(id_usuario),
                          CONSTRAINT fk_usuarios_baja       FOREIGN KEY (dado_de_baja_por) REFERENCES usuarios(id_usuario)
) ENGINE=InnoDB;

CREATE TABLE productos (
                           id_producto            INT AUTO_INCREMENT PRIMARY KEY,
                           codigo                 VARCHAR(30)  NOT NULL UNIQUE,
                           nombre                 VARCHAR(120) NOT NULL,
                           categoria              VARCHAR(60) NOT NULL DEFAULT 'General',
                           descripcion            TEXT,
                           precio                 DECIMAL(10,2) NOT NULL,
                           stock                  DECIMAL(10,3) NOT NULL DEFAULT 0,
                           stock_minimo           DECIMAL(10,3) NOT NULL DEFAULT 0,
                           unidad_medida          ENUM('KG','PIEZA','CAJA','BOLSA') NOT NULL DEFAULT 'KG',
                           dias_aviso_vencimiento INT NOT NULL DEFAULT 3,
                           activo                 BOOLEAN  NOT NULL DEFAULT TRUE,
                           fecha_creacion         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           creado_por             INT NULL,
                           fecha_actualizacion    DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
                           actualizado_por        INT NULL,
                           fecha_baja             DATETIME NULL,
                           dado_de_baja_por       INT NULL,
                           CONSTRAINT chk_productos_precio        CHECK (precio > 0),
                           CONSTRAINT chk_productos_stock         CHECK (stock >= 0),
                           CONSTRAINT chk_productos_stock_minimo  CHECK (stock_minimo >= 0),
                           CONSTRAINT fk_productos_creado     FOREIGN KEY (creado_por)       REFERENCES usuarios(id_usuario),
                           CONSTRAINT fk_productos_actualiza  FOREIGN KEY (actualizado_por)  REFERENCES usuarios(id_usuario),
                           CONSTRAINT fk_productos_baja       FOREIGN KEY (dado_de_baja_por) REFERENCES usuarios(id_usuario)
) ENGINE=InnoDB;

CREATE INDEX idx_productos_nombre ON productos(nombre);

CREATE TABLE motivos_movimiento (
                                    id_motivo        INT AUTO_INCREMENT PRIMARY KEY,
                                    nombre           VARCHAR(60) NOT NULL UNIQUE,
                                    tipo_movimiento  ENUM('ENTRADA','SALIDA') NOT NULL,
                                    activo           BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB;

-- Se crean aqui porque movimientos_inventario depende de ellas
CREATE TABLE proveedores (
                             id_proveedor  INT AUTO_INCREMENT PRIMARY KEY,
                             nombre        VARCHAR(120) NOT NULL,
                             telefono      VARCHAR(20),
                             activo        BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB;

CREATE TABLE lotes (
                       id_lote           INT AUTO_INCREMENT PRIMARY KEY,
                       id_producto       INT NOT NULL,
                       id_proveedor      INT NULL,
                       fecha_entrada     DATE NOT NULL,
                       fecha_vencimiento DATE NULL,
                       cantidad_inicial  DECIMAL(10,3) NOT NULL,
                       cantidad_actual   DECIMAL(10,3) NOT NULL,
                       CONSTRAINT chk_lotes_inicial CHECK (cantidad_inicial > 0),
                       CONSTRAINT chk_lotes_actual  CHECK (cantidad_actual >= 0),
                       CONSTRAINT fk_lotes_producto  FOREIGN KEY (id_producto)  REFERENCES productos(id_producto),
                       CONSTRAINT fk_lotes_proveedor FOREIGN KEY (id_proveedor) REFERENCES proveedores(id_proveedor)
) ENGINE=InnoDB;

CREATE INDEX idx_lotes_vencimiento ON lotes(fecha_vencimiento);

-- Ventas se define antes de movimientos porque estos pueden referenciarla
CREATE TABLE ventas (
                        id_venta    INT AUTO_INCREMENT PRIMARY KEY,
                        folio       VARCHAR(30) NOT NULL UNIQUE,
                        id_usuario  INT NOT NULL,
                        subtotal    DECIMAL(12,2) NOT NULL DEFAULT 0,
                        total       DECIMAL(12,2) NOT NULL DEFAULT 0,
                        estado      ENUM('EN_PROCESO','COMPLETADA','CANCELADA') NOT NULL DEFAULT 'EN_PROCESO',
                        fecha_venta DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        CONSTRAINT chk_ventas_subtotal CHECK (subtotal >= 0),
                        CONSTRAINT chk_ventas_total    CHECK (total >= 0),
                        CONSTRAINT fk_ventas_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
) ENGINE=InnoDB;

CREATE INDEX idx_ventas_fecha ON ventas(fecha_venta);

CREATE TABLE movimientos_inventario (
                                        id_movimiento    INT AUTO_INCREMENT PRIMARY KEY,
                                        id_producto      INT NOT NULL,
                                        id_usuario       INT NOT NULL,
                                        id_motivo        INT NOT NULL,
                                        id_lote          INT NULL,
                                        id_venta         INT NULL,
                                        tipo_movimiento  ENUM('ENTRADA','SALIDA') NOT NULL,
                                        cantidad         DECIMAL(10,3) NOT NULL,
                                        stock_anterior   DECIMAL(10,3) NOT NULL,
                                        stock_nuevo      DECIMAL(10,3) NOT NULL,
                                        referencia       VARCHAR(60),
                                        observaciones    VARCHAR(255),
                                        fecha_movimiento DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                        CONSTRAINT chk_mov_cantidad    CHECK (cantidad > 0),
                                        CONSTRAINT chk_mov_stock_nuevo CHECK (stock_nuevo >= 0),
                                        CONSTRAINT fk_mov_producto FOREIGN KEY (id_producto) REFERENCES productos(id_producto),
                                        CONSTRAINT fk_mov_usuario  FOREIGN KEY (id_usuario)  REFERENCES usuarios(id_usuario),
                                        CONSTRAINT fk_mov_motivo   FOREIGN KEY (id_motivo)   REFERENCES motivos_movimiento(id_motivo),
                                        CONSTRAINT fk_mov_lote     FOREIGN KEY (id_lote)     REFERENCES lotes(id_lote),
                                        CONSTRAINT fk_mov_venta    FOREIGN KEY (id_venta)    REFERENCES ventas(id_venta)
) ENGINE=InnoDB;

CREATE INDEX idx_mov_producto_fecha ON movimientos_inventario(id_producto, fecha_movimiento);

-- Los movimientos son inmutables: no se editan ni se borran (se corrigen con otro movimiento)
DELIMITER //
CREATE TRIGGER trg_mov_no_update BEFORE UPDATE ON movimientos_inventario
    FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Los movimientos de inventario no se pueden modificar';
END//
CREATE TRIGGER trg_mov_no_delete BEFORE DELETE ON movimientos_inventario
    FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Los movimientos de inventario no se pueden eliminar';
END//
DELIMITER ;

CREATE TABLE bitacora_auditoria (
                                    id_bitacora    INT AUTO_INCREMENT PRIMARY KEY,
                                    id_usuario     INT NOT NULL,
                                    accion         ENUM('CREAR','MODIFICAR','BAJA','REACTIVAR','LOGIN','LOGOUT','RESPALDAR','RESTAURAR') NOT NULL,
                                    tabla_afectada VARCHAR(60),
                                    id_registro    INT,
                                    valor_anterior JSON NULL,
                                    valor_nuevo    JSON NULL,
                                    fecha_accion   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    CONSTRAINT fk_bitacora_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
) ENGINE=InnoDB;

CREATE TABLE historial_acceso (
                                  id_acceso        INT AUTO_INCREMENT PRIMARY KEY,
                                  id_usuario       INT NULL,
                                  correo_intentado VARCHAR(120) NOT NULL,
                                  exitoso          BOOLEAN NOT NULL,
                                  fecha_intento    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  CONSTRAINT fk_acceso_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- SPRINT 2: ventas, pagos, tickets, alertas, historial de precios
-- ---------------------------------------------------------------------

CREATE TABLE detalle_venta (
                               id_detalle      INT AUTO_INCREMENT PRIMARY KEY,
                               id_venta        INT NOT NULL,
                               id_producto     INT NOT NULL,
                               cantidad        DECIMAL(10,3) NOT NULL,
                               precio_unitario DECIMAL(10,2) NOT NULL,
                               subtotal        DECIMAL(12,2) NOT NULL,
                               CONSTRAINT chk_detalle_cantidad CHECK (cantidad > 0),
                               CONSTRAINT chk_detalle_precio   CHECK (precio_unitario > 0),
                               CONSTRAINT fk_detalle_venta    FOREIGN KEY (id_venta)    REFERENCES ventas(id_venta),
                               CONSTRAINT fk_detalle_producto FOREIGN KEY (id_producto) REFERENCES productos(id_producto)
) ENGINE=InnoDB;

CREATE TABLE metodos_pago (
                              id_metodo_pago INT AUTO_INCREMENT PRIMARY KEY,
                              nombre         VARCHAR(40) NOT NULL UNIQUE,
                              activo         BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB;

CREATE TABLE pagos (
                       id_pago        INT AUTO_INCREMENT PRIMARY KEY,
                       id_venta       INT NOT NULL,
                       id_metodo_pago INT NOT NULL,
                       monto          DECIMAL(12,2) NOT NULL,
                       referencia     VARCHAR(60),
                       fecha_pago     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       CONSTRAINT chk_pagos_monto CHECK (monto > 0),
                       CONSTRAINT fk_pagos_venta  FOREIGN KEY (id_venta)       REFERENCES ventas(id_venta),
                       CONSTRAINT fk_pagos_metodo FOREIGN KEY (id_metodo_pago) REFERENCES metodos_pago(id_metodo_pago)
) ENGINE=InnoDB;

CREATE TABLE tickets (
                         id_ticket     INT AUTO_INCREMENT PRIMARY KEY,
                         id_venta      INT NOT NULL UNIQUE,
                         folio         VARCHAR(30) NOT NULL UNIQUE,
                         ruta_archivo  VARCHAR(255),
                         impreso       BOOLEAN NOT NULL DEFAULT FALSE,
                         fecha_emision DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         CONSTRAINT fk_tickets_venta FOREIGN KEY (id_venta) REFERENCES ventas(id_venta)
) ENGINE=InnoDB;

CREATE TABLE alertas (
                         id_alerta       INT AUTO_INCREMENT PRIMARY KEY,
                         id_producto     INT NOT NULL,
                         id_lote         INT NULL,
                         tipo_alerta     ENUM('BAJO_STOCK','POR_VENCER','VENCIDO') NOT NULL,
                         estado          ENUM('ACTIVA','ATENDIDA','RESUELTA') NOT NULL DEFAULT 'ACTIVA',
                         atendida_por    INT NULL,
                         fecha_generada  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         fecha_resuelta  DATETIME NULL,
                         CONSTRAINT fk_alertas_producto FOREIGN KEY (id_producto)  REFERENCES productos(id_producto),
                         CONSTRAINT fk_alertas_lote     FOREIGN KEY (id_lote)      REFERENCES lotes(id_lote),
                         CONSTRAINT fk_alertas_usuario  FOREIGN KEY (atendida_por) REFERENCES usuarios(id_usuario)
) ENGINE=InnoDB;

CREATE TABLE historial_precios (
                                   id_historial    INT AUTO_INCREMENT PRIMARY KEY,
                                   id_producto     INT NOT NULL,
                                   id_usuario      INT NOT NULL,
                                   precio_anterior DECIMAL(10,2) NOT NULL,
                                   precio_nuevo    DECIMAL(10,2) NOT NULL,
                                   fecha_cambio    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   CONSTRAINT fk_hprecios_producto FOREIGN KEY (id_producto) REFERENCES productos(id_producto),
                                   CONSTRAINT fk_hprecios_usuario  FOREIGN KEY (id_usuario)  REFERENCES usuarios(id_usuario)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- SPRINT 3: respaldos
-- ---------------------------------------------------------------------

CREATE TABLE respaldos (
                           id_respaldo     INT AUTO_INCREMENT PRIMARY KEY,
                           id_usuario      INT NOT NULL,
                           tipo            ENUM('RESPALDO','RESTAURACION') NOT NULL,
                           ruta_archivo    VARCHAR(255) NOT NULL,
                           exitoso         BOOLEAN NOT NULL,
                           fecha_operacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           CONSTRAINT fk_respaldos_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
) ENGINE=InnoDB;

-- =====================================================================
-- DATOS INICIALES
-- =====================================================================

INSERT INTO roles (nombre, descripcion) VALUES
                                            ('Administrador', 'Acceso total: usuarios, productos, respaldos y reportes'),
                                            ('Empleado',      'Ventas, consulta de productos y movimientos de inventario'),
                                            ('Propietario',   'Reportes, alertas y consulta general');

INSERT INTO motivos_movimiento (nombre, tipo_movimiento) VALUES
                                                             ('Compra a proveedor',      'ENTRADA'),
                                                             ('Devolucion de cliente',   'ENTRADA'),
                                                             ('Ajuste de inventario (+)','ENTRADA'),
                                                             ('Venta',                   'SALIDA'),
                                                             ('Merma',                   'SALIDA'),
                                                             ('Producto vencido',        'SALIDA'),
                                                             ('Ajuste de inventario (-)','SALIDA');

INSERT INTO metodos_pago (nombre) VALUES ('Efectivo'), ('Tarjeta'), ('Transferencia');

-- Usuario administrador inicial
--   correo:     admin@fruteriablanco.com
--   contrasena: Admin123   (cambiarla despues del primer acceso)
INSERT INTO usuarios (nombre, correo, contrasena_hash, id_rol)
VALUES ('Administrador', 'admin@fruteriablanco.com',
        '$2a$10$.gAmoTBBVz9tVrw1eIxGW.g9A5s2C0OOjznytlPvxzgPcwcUhNPiK',
        (SELECT id_rol FROM roles WHERE nombre = 'Administrador'));

INSERT INTO usuarios (nombre, correo, contrasena_hash, id_rol)
VALUES ('Empleado', 'empleado@fruteriablanco.com', '$2a$10$0BDMaPJqQRwU3SblvrwKBeLmBaeiatA4M69/WyitBT8OP5CTz55Pm',
        (SELECT id_rol FROM roles WHERE nombre = 'Empleado'));


-- Productos de ejemplo para probar (se pueden borrar)
INSERT INTO productos (codigo, nombre, categoria, descripcion, precio, stock, stock_minimo, unidad_medida, creado_por) VALUES
                                                                                                                           ('FR-001', 'Manzana roja',  'Frutas', 'Manzana roja por kilo', 38.00, 50, 10, 'KG', 1),
                                                                                                                           ('FR-002', 'Platano',       'Frutas', 'Platano por kilo',      24.50, 80, 15, 'KG', 1),
                                                                                                                           ('FR-003', 'Aguacate Hass', 'Frutas', 'Aguacate por kilo',     89.00, 30,  8, 'KG', 1),
                                                                                                                           ('FR-004', 'Sandia',        'Frutas', 'Sandia por pieza',      65.00, 12,  3, 'PIEZA', 1);
