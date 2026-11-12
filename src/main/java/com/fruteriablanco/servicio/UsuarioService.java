package com.fruteriablanco.servicio;

import com.fruteriablanco.modelo.Rol;
import com.fruteriablanco.modelo.Usuario;
import com.fruteriablanco.repositorio.UsuarioRepository;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * USR-01: reglas para registrar y administrar usuarios.
 * - No se permiten datos obligatorios vacíos.
 * - No se permiten correos duplicados.
 * - La contraseña se guarda cifrada con BCrypt (igual que en el login).
 */
public class UsuarioService {

    private static final Pattern CORREO = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final int MIN_CONTRASENA = 6;
    private static final String DUPLICADO = "Ya existe un usuario registrado con ese correo.";

    private final UsuarioRepository repository = new UsuarioRepository();

    public List<Usuario> listar(String filtro) throws SQLException {
        return repository.listar(filtro);
    }

    public List<Rol> listarRoles() throws SQLException {
        return repository.listarRoles();
    }

    /** Alta de usuario. La contraseña es obligatoria. */
    public void crear(Usuario u, String contrasena, String confirmacion, int idAdmin) throws SQLException {
        normalizar(u);
        validarDatos(u);
        validarContrasena(contrasena, confirmacion);
        validarNoDuplicado(u.getCorreo(), 0);

        u.setContrasenaHash(BCrypt.hashpw(contrasena, BCrypt.gensalt()));
        try {
            repository.insertar(u, idAdmin);
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new IllegalArgumentException(DUPLICADO, e);
        }
    }

    /** Modificación. Si contrasena viene vacía, se conserva la actual. */
    public void actualizar(Usuario u, String contrasena, String confirmacion, int idAdmin) throws SQLException {
        normalizar(u);
        validarDatos(u);
        validarNoDuplicado(u.getCorreo(), u.getIdUsuario());

        // El administrador no puede quitarse a sí mismo el rol (se quedaría sin acceso a este módulo).
        if (u.getIdUsuario() == idAdmin && !u.esAdministrador()) {
            throw new IllegalArgumentException("No puedes quitarte a ti mismo el rol Administrador.");
        }

        boolean cambiaClave = (contrasena != null && !contrasena.isEmpty())
                || (confirmacion != null && !confirmacion.isEmpty());
        if (cambiaClave) {
            validarContrasena(contrasena, confirmacion);
            u.setContrasenaHash(BCrypt.hashpw(contrasena, BCrypt.gensalt()));
        } else {
            u.setContrasenaHash(null);
        }

        try {
            repository.actualizar(u, idAdmin);
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new IllegalArgumentException(DUPLICADO, e);
        }
    }

    /** "Eliminar" = baja lógica: el usuario ya no puede iniciar sesión y se conserva su historial. */
    public void eliminar(int idUsuario, int idAdmin) throws SQLException {
        if (idUsuario == idAdmin) {
            throw new IllegalArgumentException("No puedes eliminar tu propia cuenta mientras tienes la sesión abierta.");
        }
        repository.desactivar(idUsuario, idAdmin);
    }

    public void reactivar(int idUsuario, int idAdmin) throws SQLException {
        repository.activar(idUsuario, idAdmin);
    }

    private void normalizar(Usuario u) {
        if (u == null) {
            throw new IllegalArgumentException("No se recibieron los datos del usuario.");
        }
        u.setNombre(u.getNombre() == null ? "" : u.getNombre().trim());
        u.setCorreo(u.getCorreo() == null ? "" : u.getCorreo().trim().toLowerCase(Locale.ROOT));
    }

    private void validarDatos(Usuario u) {
        if (u.getNombre().isEmpty()) {
            throw new IllegalArgumentException("Ingresa el nombre del usuario.");
        }
        if (u.getNombre().length() > 100) {
            throw new IllegalArgumentException("El nombre permite hasta 100 caracteres.");
        }
        if (u.getCorreo().isEmpty()) {
            throw new IllegalArgumentException("Ingresa el correo del usuario.");
        }
        if (u.getCorreo().length() > 120) {
            throw new IllegalArgumentException("El correo permite hasta 120 caracteres.");
        }
        if (!CORREO.matcher(u.getCorreo()).matches()) {
            throw new IllegalArgumentException("El correo no tiene un formato válido (ej. nombre@dominio.com).");
        }
        if (u.getRol() == null) {
            throw new IllegalArgumentException("Selecciona el rol del usuario.");
        }
    }

    private void validarContrasena(String contrasena, String confirmacion) {
        if (contrasena == null || contrasena.isEmpty()) {
            throw new IllegalArgumentException("Ingresa la contraseña.");
        }
        if (contrasena.length() < MIN_CONTRASENA) {
            throw new IllegalArgumentException("La contraseña debe tener al menos " + MIN_CONTRASENA + " caracteres.");
        }
        if (contrasena.length() > 72) {
            throw new IllegalArgumentException("La contraseña permite hasta 72 caracteres.");
        }
        if (!contrasena.equals(confirmacion)) {
            throw new IllegalArgumentException("Las contraseñas no coinciden.");
        }
    }

    private void validarNoDuplicado(String correo, int idExcluir) throws SQLException {
        if (repository.existeCorreo(correo, idExcluir)) {
            throw new IllegalArgumentException(DUPLICADO);
        }
    }
}
