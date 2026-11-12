
package com.fruteriablanco.servicio;

/** Error de venta con un mensaje listo para mostrarse al usuario. */
public class VentaException extends Exception {

    public VentaException(String mensaje) {
        super(mensaje);
    }

    public VentaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}