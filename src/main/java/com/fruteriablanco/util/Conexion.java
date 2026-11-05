package com.fruteriablanco.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Entrega conexiones a la base de datos MySQL.
 * Lee los datos de /db.properties (carpeta src/main/resources).
 * Se usa solo desde la capa de repositorio.
 */
public final class Conexion {

    private static final Properties CONFIG = new Properties();

    static {
        try (InputStream in = Conexion.class.getResourceAsStream("/db.properties")) {
            if (in == null) {
                throw new IllegalStateException(
                        "No se encontro db.properties. Copia db.properties.example como "
                                + "db.properties en src/main/resources y ajusta tus datos.");
            }
            CONFIG.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer db.properties", e);
        }
    }

    private Conexion() {
    }

    /** Abre una conexion nueva. Quien la pida debe cerrarla (try-with-resources). */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                CONFIG.getProperty("db.url"),
                CONFIG.getProperty("db.user"),
                CONFIG.getProperty("db.password"));
    }
}
