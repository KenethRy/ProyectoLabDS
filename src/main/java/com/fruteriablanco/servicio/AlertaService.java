package com.fruteriablanco.servicio;

import com.fruteriablanco.modelo.Alerta;
import com.fruteriablanco.repositorio.AlertaRepository;

import java.sql.SQLException;
import java.util.List;

public class AlertaService {

    private final AlertaRepository repository = new AlertaRepository();

    public List<Alerta> listarVigentes() throws SQLException {
        return repository.listarVigentes();
    }

    public int contarPendientes() throws SQLException {
        return repository.contarPendientes();
    }

    public void marcarAtendida(int idAlerta, int idUsuario) throws SQLException {
        if (idAlerta <= 0) {
            throw new IllegalArgumentException("La alerta seleccionada no es valida.");
        }
        if (idUsuario <= 0) {
            throw new IllegalArgumentException("No se identifico al usuario.");
        }
        repository.marcarAtendida(idAlerta, idUsuario);
    }
}