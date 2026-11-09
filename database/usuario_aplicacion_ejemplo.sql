-- Ejecutar en el servidor MySQL con una cuenta administradora.
-- CAMBIA 192.168.1.% por la subred real del equipo y define una clave fuerte.
-- No abras el puerto 3306 a Internet ni compartas este usuario como administrador.

CREATE USER IF NOT EXISTS 'fruteria_app'@'192.168.1.%'
IDENTIFIED BY 'REEMPLAZA_POR_UNA_CLAVE_SEGURA';

GRANT SELECT, INSERT, UPDATE ON fruteria_blanco.*
TO 'fruteria_app'@'192.168.1.%';

FLUSH PRIVILEGES;
