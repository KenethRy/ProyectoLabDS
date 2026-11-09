package com.fruteriablanco.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/** Entrega conexiones a MySQL usando la configuracion local de cada integrante. */
public final class Conexion {

    private static final Properties CONFIG = cargarConfiguracion();

    private Conexion() {
    }

    private static Properties cargarConfiguracion() {
        Properties propiedades = new Properties();
        Path archivoExterno = Path.of("db.properties");

        // Permite configurar el servidor sin modificar ni volver a compilar el proyecto.
        if (Files.exists(archivoExterno)) {
            try (InputStream in = Files.newInputStream(archivoExterno)) {
                propiedades.load(in);
                return propiedades;
            } catch (IOException e) {
                throw new IllegalStateException("No se pudo leer el archivo db.properties externo.", e);
            }
        }

        // Alternativa para ejecuciones desde el IDE con el archivo en src/main/resources.
        try (InputStream in = Conexion.class.getResourceAsStream("/db.properties")) {
            if (in != null) {
                propiedades.load(in);
                return propiedades;
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer db.properties.", e);
        }

        throw new IllegalStateException(
                "No se encontro db.properties. Copia db.properties.example como db.properties "
                        + "en la raiz del proyecto o en src/main/resources y configura el servidor MySQL.");
    }

    /** Abre una conexion nueva. Quien la pida debe cerrarla con try-with-resources. */
    public static Connection getConnection() throws SQLException {
        String url = CONFIG.getProperty("db.url");
        String usuario = CONFIG.getProperty("db.user");
        String clave = CONFIG.getProperty("db.password");
        if (url == null || usuario == null || clave == null) {
            throw new SQLException("Faltan db.url, db.user o db.password en db.properties.");
        }
        return DriverManager.getConnection(url.trim(), usuario.trim(), clave);
    }
}
