
package com.fruteriablanco.servicio;

import com.fruteriablanco.modelo.Producto;
import com.fruteriablanco.repositorio.ProductoRepository;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Reglas de validacion del catalogo y movimientos de inventario.
 */
public class ProductoService {

    private final ProductoRepository repository = new ProductoRepository();

    public List<Producto> listar(String filtro) throws SQLException {
        return repository.listar(filtro);
    }

    public List<String> listarMotivos(String tipo) throws SQLException {
        return repository.listarMotivos(tipo);
    }

    public void crear(Producto producto, BigDecimal stockInicial, String proveedor,
                      String telefonoProveedor, LocalDate vencimiento, int idUsuario)
            throws SQLException {

        validarProducto(producto);
        validarCantidad(stockInicial, "La existencia inicial");

        if (stockInicial.compareTo(BigDecimal.ZERO) > 0
                && vencimiento != null
                && vencimiento.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "La fecha de vencimiento no puede ser anterior a hoy."
            );
        }

        validarProveedor(proveedor, telefonoProveedor);

        repository.insertar(
                producto, stockInicial, proveedor,
                telefonoProveedor, vencimiento, idUsuario
        );
    }

    public void actualizar(Producto producto, int idUsuario) throws SQLException {
        validarProducto(producto);
        repository.actualizar(producto, idUsuario);
    }

    public void desactivar(int idProducto, int idUsuario) throws SQLException {
        repository.desactivar(idProducto, idUsuario);
    }

    // NUEVO: REACTIVAR PRODUCTO
    public void activar(int idProducto, int idUsuario) throws SQLException {

        if (idProducto <= 0) {
            throw new IllegalArgumentException("El producto seleccionado no es valido.");
        }

        if (idUsuario <= 0) {
            throw new IllegalArgumentException("No se identifico al usuario administrador.");
        }

        repository.activar(idProducto, idUsuario);
    }

    public void registrarMovimiento(int idProducto, String tipo,
                                    BigDecimal cantidad, String motivo,
                                    String proveedor, String telefono,
                                    LocalDate vencimiento,
                                    String observaciones, int idUsuario)
            throws SQLException {

        validarCantidad(cantidad, "La cantidad del movimiento");

        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "La cantidad del movimiento debe ser mayor que cero."
            );
        }

        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException(
                    "Selecciona el motivo del movimiento."
            );
        }

        if ("ENTRADA".equals(tipo)) {
            validarProveedor(proveedor, telefono);

            if (vencimiento != null && vencimiento.isBefore(LocalDate.now())) {
                throw new IllegalArgumentException(
                        "La fecha de vencimiento no puede ser anterior a hoy."
                );
            }
        }

        repository.registrarMovimiento(
                idProducto, tipo, cantidad, motivo,
                proveedor, telefono, vencimiento,
                observaciones, idUsuario
        );
    }

    private void validarProducto(Producto p) {

        if (p == null) {
            throw new IllegalArgumentException(
                    "No se recibieron los datos del producto."
            );
        }

        if (vacio(p.getCodigo())) {
            throw new IllegalArgumentException("Ingresa el codigo del producto.");
        }

        if (p.getCodigo().trim().length() > 30) {
            throw new IllegalArgumentException(
                    "El codigo permite hasta 30 caracteres."
            );
        }

        if (vacio(p.getNombre())) {
            throw new IllegalArgumentException("Ingresa el nombre del producto.");
        }

        if (p.getNombre().trim().length() > 120) {
            throw new IllegalArgumentException(
                    "El nombre permite hasta 120 caracteres."
            );
        }

        if (vacio(p.getCategoria())) {
            throw new IllegalArgumentException(
                    "Selecciona o escribe una categoria."
            );
        }

        if (p.getCategoria().trim().length() > 60) {
            throw new IllegalArgumentException(
                    "La categoria permite hasta 60 caracteres."
            );
        }

        if (p.getPrecio() == null
                || p.getPrecio().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "El precio debe ser mayor que cero."
            );
        }

        if (p.getPrecio().scale() > 2) {
            throw new IllegalArgumentException(
                    "El precio permite hasta 2 decimales."
            );
        }

        if (p.getStockMinimo() == null
                || p.getStockMinimo().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "El stock minimo no puede ser negativo."
            );
        }

        if (p.getStockMinimo().scale() > 3) {
            throw new IllegalArgumentException(
                    "El stock minimo permite hasta 3 decimales."
            );
        }

        if (p.getUnidadMedida() == null) {
            throw new IllegalArgumentException(
                    "Selecciona la unidad de medida."
            );
        }

        if (p.getDiasAvisoVencimiento() < 0
                || p.getDiasAvisoVencimiento() > 365) {
            throw new IllegalArgumentException(
                    "Los dias de aviso deben estar entre 0 y 365."
            );
        }
    }

    private void validarCantidad(BigDecimal cantidad, String campo) {

        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    campo + " no puede ser negativa."
            );
        }

        if (cantidad.scale() > 3) {
            throw new IllegalArgumentException(
                    campo + " permite hasta 3 decimales."
            );
        }
    }

    private void validarProveedor(String proveedor, String telefono) {

        if (proveedor != null && proveedor.trim().length() > 120) {
            throw new IllegalArgumentException(
                    "El nombre del proveedor permite hasta 120 caracteres."
            );
        }

        if (telefono != null && telefono.trim().length() > 20) {
            throw new IllegalArgumentException(
                    "El telefono permite hasta 20 caracteres."
            );
        }
    }

    private boolean vacio(String valor) {
        return valor == null || valor.trim().isEmpty();
    }
}
