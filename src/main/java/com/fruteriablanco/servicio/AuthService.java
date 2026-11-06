package com.fruteriablanco.servicio;

import com.fruteriablanco.modelo.Usuario;
import com.fruteriablanco.repositorio.UsuarioRepository;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Capa de servicio de SEG-01: reglas del inicio de sesión.
 * La pantalla solo llama a iniciarSesion(); aquí viven las validaciones.
 */
public class AuthService {

    // Mismo mensaje para "no existe" y "contraseña incorrecta": no revela qué correos existen.
    private static final String CREDENCIALES_INVALIDAS = "Correo o contraseña incorrectos.";

    private final UsuarioRepository usuarioRepository = new UsuarioRepository();

    /**
     * @return el usuario autenticado
     * @throws AuthException con el mensaje a mostrar si el acceso se rechaza
     */
    public Usuario iniciarSesion(String correo, String contrasena) throws AuthException {
        String correoLimpio = correo == null ? "" : correo.trim();
        String clave = contrasena == null ? "" : contrasena;

        // Caso de prueba: campos vacíos
        if (correoLimpio.isEmpty() || clave.isEmpty()) {
            throw new AuthException("Ingresa tu correo y tu contraseña.");
        }

        try {
            Optional<Usuario> encontrado = usuarioRepository.buscarPorCorreo(correoLimpio);

            // Correo que no existe
            if (encontrado.isEmpty()) {
                usuarioRepository.registrarAcceso(null, correoLimpio, false);
                throw new AuthException(CREDENCIALES_INVALIDAS);
            }

            Usuario usuario = encontrado.get();

            // Usuario dado de baja
            if (!usuario.isActivo()) {
                usuarioRepository.registrarAcceso(usuario.getIdUsuario(), correoLimpio, false);
                throw new AuthException("Esta cuenta está inactiva. Contacta al administrador.");
            }

            // Caso de prueba: contraseña incorrecta / credenciales correctas
            boolean correcta = BCrypt.checkpw(clave, usuario.getContrasenaHash());
            usuarioRepository.registrarAcceso(usuario.getIdUsuario(), correoLimpio, correcta);

            if (!correcta) {
                throw new AuthException(CREDENCIALES_INVALIDAS);
            }
            return usuario;

        } catch (SQLException e) {
            throw new AuthException("No se pudo conectar con la base de datos. Verifica que MySQL esté encendido.", e);
        }
    }
}
