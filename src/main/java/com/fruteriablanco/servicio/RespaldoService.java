package com.fruteriablanco.servicio;

import com.fruteriablanco.modelo.Respaldo;
import com.fruteriablanco.modelo.Usuario;
import com.fruteriablanco.repositorio.RespaldoRepository;
import com.fruteriablanco.util.Conexion;
import com.fruteriablanco.util.SesionActual;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * BD-01: respaldo y restauracion de la base de datos con mysqldump / mysql.
 * Solo el Administrador puede usarlo. Cada operacion queda registrada en la tabla respaldos.
 */
public class RespaldoService {

    private static final Pattern URL_MYSQL = Pattern.compile("jdbc:mysql://([^:/?]+)(?::(\\d+))?/([^?]+).*");
    private static final String[] TABLAS_OBLIGATORIAS = {"usuarios", "productos", "ventas", "respaldos"};
    private static final boolean WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");

    private final RespaldoRepository repositorio = new RespaldoRepository();

    // ---------------------- Respaldo ----------------------

    public Path respaldar(Path destino) throws RespaldoException {
        Usuario usuario = exigirAdministrador();
        if (destino == null) {
            throw new RespaldoException("Elige donde guardar el respaldo.");
        }
        Path temporal = destino.resolveSibling(destino.getFileName() + ".tmp");
        try {
            ejecutarDump(temporal);
            Files.move(temporal, destino, StandardCopyOption.REPLACE_EXISTING);
            registrar(usuario, "RESPALDO", destino, true);
            return destino;
        } catch (RespaldoException e) {
            borrarSilencio(temporal);
            registrar(usuario, "RESPALDO", destino, false);
            throw e;
        } catch (IOException e) {
            borrarSilencio(temporal);
            registrar(usuario, "RESPALDO", destino, false);
            throw new RespaldoException("No se pudo guardar el archivo en esa ubicacion: " + e.getMessage(), e);
        }
    }

    private void ejecutarDump(Path salida) throws RespaldoException {
        Conn c = parsearUrl();
        List<String> cmd = new ArrayList<>();
        cmd.add(buscarEjecutable("mysqldump"));
        cmd.add("--host=" + c.host);
        cmd.add("--port=" + c.puerto);
        cmd.add("--user=" + Conexion.getUsuario());
        cmd.add("--single-transaction");
        cmd.add("--routines");
        cmd.add("--triggers");
        cmd.add("--events");
        cmd.add("--no-tablespaces");
        cmd.add("--default-character-set=utf8mb4");
        cmd.add("--result-file=" + salida.toAbsolutePath());
        cmd.add(c.base);
        correr(cmd, null, "No se pudo crear el respaldo");
        try {
            if (!Files.exists(salida) || Files.size(salida) == 0) {
                throw new RespaldoException("El respaldo quedo vacio. Revisa la conexion a MySQL.");
            }
        } catch (IOException e) {
            throw new RespaldoException("No se pudo verificar el respaldo creado.", e);
        }
    }

    // ---------------------- Restauracion ----------------------

    public void restaurar(Path archivo) throws RespaldoException {
        Usuario usuario = exigirAdministrador();
        Path seguridad = null;
        boolean tocoLaBase = false;
        try {
            validarArchivo(archivo);

            // Copia de seguridad previa: si la restauracion falla se regresa al estado anterior.
            seguridad = Files.createTempFile("pre_restauracion_", ".sql");
            ejecutarDump(seguridad);

            tocoLaBase = true;
            ejecutarRestauracion(archivo);

            registrar(usuario, "RESTAURACION", archivo, true);
        } catch (RespaldoException e) {
            String extra = "";
            if (tocoLaBase && seguridad != null) {
                try {
                    ejecutarRestauracion(seguridad);
                    extra = " Se devolvio la base de datos a su estado anterior.";
                } catch (RespaldoException ex) {
                    extra = " ATENCION: la base pudo quedar incompleta. Restaura un respaldo valido.";
                }
            }
            registrar(usuario, "RESTAURACION", archivo, false);
            throw new RespaldoException(e.getMessage() + extra, e);
        } catch (IOException e) {
            registrar(usuario, "RESTAURACION", archivo, false);
            throw new RespaldoException("No se pudo preparar la restauracion: " + e.getMessage(), e);
        } finally {
            borrarSilencio(seguridad);
        }
    }

    /** Rechaza archivos que no sean un respaldo de esta base antes de tocarla. */
    private void validarArchivo(Path archivo) throws RespaldoException, IOException {
        if (archivo == null || !Files.isRegularFile(archivo)) {
            throw new RespaldoException("El archivo seleccionado no existe.");
        }
        if (!archivo.getFileName().toString().toLowerCase().endsWith(".sql")) {
            throw new RespaldoException("Archivo invalido: debe ser un respaldo con extension .sql.");
        }
        if (Files.size(archivo) == 0) {
            throw new RespaldoException("Archivo invalido: esta vacio.");
        }

        boolean cabecera = false;
        boolean[] encontradas = new boolean[TABLAS_OBLIGATORIAS.length];
        try (BufferedReader lector = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
            String linea;
            int leidas = 0;
            while ((linea = lector.readLine()) != null) {
                if (leidas++ < 5 && linea.contains("MySQL dump")) {
                    cabecera = true;
                }
                if (linea.startsWith("CREATE TABLE")) {
                    for (int i = 0; i < TABLAS_OBLIGATORIAS.length; i++) {
                        if (linea.contains("`" + TABLAS_OBLIGATORIAS[i] + "`")) {
                            encontradas[i] = true;
                        }
                    }
                }
            }
        } catch (java.nio.charset.CharacterCodingException e) {
            throw new RespaldoException("Archivo invalido: no es un respaldo de texto SQL.");
        }
        boolean completo = true;
        for (boolean b : encontradas) {
            completo &= b;
        }
        if (!cabecera || !completo) {
            throw new RespaldoException(
                    "Archivo invalido: no es un respaldo de la base de datos de Fruteria Blanco.");
        }
    }

    private void ejecutarRestauracion(Path archivo) throws RespaldoException {
        Conn c = parsearUrl();
        List<String> cmd = new ArrayList<>();
        cmd.add(buscarEjecutable("mysql"));
        cmd.add("--host=" + c.host);
        cmd.add("--port=" + c.puerto);
        cmd.add("--user=" + Conexion.getUsuario());
        cmd.add("--default-character-set=utf8mb4");
        cmd.add(c.base);
        correr(cmd, archivo, "No se pudo restaurar el respaldo");
    }

    // ---------------------- Historial ----------------------

    public List<Respaldo> historial() throws RespaldoException {
        exigirAdministrador();
        try {
            return repositorio.listarRecientes();
        } catch (SQLException e) {
            throw new RespaldoException("No se pudo cargar el historial: " + e.getMessage(), e);
        }
    }

    // ---------------------- Utilidades ----------------------

    private Usuario exigirAdministrador() throws RespaldoException {
        Usuario u = SesionActual.getUsuario();
        if (u == null || !u.esAdministrador()) {
            throw new RespaldoException("Solo el rol Administrador puede respaldar o restaurar la base de datos.");
        }
        return u;
    }

    /** El registro es secundario: si falla (por ejemplo tras restaurar un respaldo viejo) no se interrumpe. */
    private void registrar(Usuario usuario, String tipo, Path ruta, boolean exitoso) {
        try {
            repositorio.registrar(usuario.getIdUsuario(), tipo, String.valueOf(ruta), exitoso);
        } catch (SQLException ignorada) {
            // el usuario pudo no existir en el respaldo restaurado
        }
    }

    private void correr(List<String> comando, Path entrada, String mensajeError) throws RespaldoException {
        ProcessBuilder pb = new ProcessBuilder(comando);
        pb.environment().put("MYSQL_PWD", Conexion.getClave()); // la clave no aparece en la linea de comando
        pb.redirectErrorStream(true);
        if (entrada != null) {
            pb.redirectInput(entrada.toFile());
        }
        try {
            Process proceso = pb.start();
            String salida = new String(proceso.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (!proceso.waitFor(10, TimeUnit.MINUTES)) {
                proceso.destroyForcibly();
                throw new RespaldoException(mensajeError + ": la operacion tardo demasiado.");
            }
            if (proceso.exitValue() != 0) {
                throw new RespaldoException(mensajeError + ": " + resumir(salida));
            }
        } catch (IOException e) {
            throw new RespaldoException(mensajeError + ": " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RespaldoException(mensajeError + ": operacion cancelada.", e);
        }
    }

    private String resumir(String salida) {
        if (salida.isEmpty()) {
            return "error desconocido.";
        }
        return salida.length() > 400 ? salida.substring(0, 400) + "..." : salida;
    }

    private Conn parsearUrl() throws RespaldoException {
        Matcher m = URL_MYSQL.matcher(Conexion.getUrl());
        if (!m.matches()) {
            throw new RespaldoException("No se entendio db.url en db.properties.");
        }
        return new Conn(m.group(1), m.group(2) == null ? "3306" : m.group(2), m.group(3));
    }

    /** Busca mysqldump/mysql: db.mysql.bin, luego PATH y por ultimo carpetas tipicas de Windows. */
    private String buscarEjecutable(String nombre) throws RespaldoException {
        String archivo = WINDOWS ? nombre + ".exe" : nombre;
        List<File> carpetas = new ArrayList<>();

        String configurada = Conexion.getPropiedad("db.mysql.bin");
        if (configurada != null) {
            carpetas.add(new File(configurada));
        }
        String path = System.getenv("PATH");
        if (path != null) {
            for (String p : path.split(File.pathSeparator)) {
                carpetas.add(new File(p));
            }
        }
        if (WINDOWS) {
            for (String raiz : new String[]{"C:\\Program Files\\MySQL", "C:\\Program Files (x86)\\MySQL"}) {
                File[] hijas = new File(raiz).listFiles(File::isDirectory);
                if (hijas == null) {
                    continue;
                }
                // Primero MySQL Server (misma version que la base); Workbench al final.
                for (File h : hijas) {
                    if (h.getName().contains("Server")) {
                        carpetas.add(new File(h, "bin"));
                    }
                }
                for (File h : hijas) {
                    if (!h.getName().contains("Server")) {
                        carpetas.add(new File(h, "bin"));
                        carpetas.add(h);
                    }
                }
            }
        }
        for (File carpeta : carpetas) {
            File f = new File(carpeta, archivo);
            if (f.isFile()) {
                return f.getAbsolutePath();
            }
        }
        throw new RespaldoException("No se encontro " + archivo + ". Agrega en db.properties la linea "
                + "db.mysql.bin=C:/Program Files/MySQL/MySQL Server 9.4/bin (carpeta donde esta instalado MySQL).");
    }

    private void borrarSilencio(Path ruta) {
        if (ruta != null) {
            try {
                Files.deleteIfExists(ruta);
            } catch (IOException ignorada) {
                // archivo temporal: no importa
            }
        }
    }

    private record Conn(String host, String puerto, String base) {
    }
}
