-- Ejecutar UNA SOLA VEZ si la base fruteria_blanco ya existia antes
-- de agregar el campo categoria. No ejecutar si ya existe esa columna.
USE fruteria_blanco;

ALTER TABLE productos
    ADD COLUMN categoria VARCHAR(60) NOT NULL DEFAULT 'General' AFTER nombre;
