package com.fruteriablanco.util;

import com.fruteriablanco.modelo.Usuario;

/**
 * Guarda el usuario que inició sesión mientras la aplicación está abierta.
 * Las demás pantallas la consultan para saber quién es y qué rol tiene (SEG-02).
 */
public final class SesionActual {

    private static Usuario usuario;

    private SesionActual() {
    }

    public static void iniciar(Usuario u) {
        usuario = u;
    }

    public static void cerrar() {
        usuario = null;
    }

    public static Usuario getUsuario() {
        return usuario;
    }

    public static boolean haySesion() {
        return usuario != null;
    }
}
