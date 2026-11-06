package com.fruteriablanco.servicio;

/** Error de autenticación con un mensaje listo para mostrarse al usuario. */
public class AuthException extends Exception {

    public AuthException(String mensaje) {
        super(mensaje);
    }

    public AuthException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
