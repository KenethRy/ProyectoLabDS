package com.fruteriablanco.modelo;

import java.time.LocalDateTime;

/** Registro de una operacion de respaldo o restauracion (tabla respaldos). */
public class Respaldo {

    private int idRespaldo;
    private String usuario;
    private String tipo;
    private String rutaArchivo;
    private boolean exitoso;
    private LocalDateTime fecha;

    public int getIdRespaldo() { return idRespaldo; }
    public void setIdRespaldo(int idRespaldo) { this.idRespaldo = idRespaldo; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getRutaArchivo() { return rutaArchivo; }
    public void setRutaArchivo(String rutaArchivo) { this.rutaArchivo = rutaArchivo; }
    public boolean isExitoso() { return exitoso; }
    public void setExitoso(boolean exitoso) { this.exitoso = exitoso; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}
