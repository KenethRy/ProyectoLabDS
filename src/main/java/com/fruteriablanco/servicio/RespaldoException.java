package com.fruteriablanco.servicio;

/** Error de negocio en respaldos o restauraciones (mensaje listo para mostrar al usuario). */
public class RespaldoException extends Exception {

    public RespaldoException(String mensaje) {
        super(mensaje);
    }

    public RespaldoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
