
package com.fruteriablanco.servicio;

import com.fruteriablanco.modelo.DetalleVenta;
import com.fruteriablanco.modelo.MetodoPago;
import com.fruteriablanco.modelo.Producto;
import com.fruteriablanco.modelo.Usuario;
import com.fruteriablanco.modelo.Venta;
import com.fruteriablanco.repositorio.ProductoRepository;
import com.fruteriablanco.repositorio.VentaRepository;
import com.fruteriablanco.util.SesionActual;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import com.fruteriablanco.modelo.VentaResumen;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

/**
 * Capa de servicio de VEN-01: reglas para registrar ventas.
 * La pantalla solo llama a estos metodos; aqui viven las validaciones.
 */
public class VentaService {

    private final ProductoRepository productoRepository = new ProductoRepository();
    private final VentaRepository ventaRepository = new VentaRepository();

    /** Productos activos que coinciden con el texto (codigo, nombre o categoria). */
    public List<Producto> buscarProductos(String filtro) throws VentaException {
        try {
            List<Producto> activos = new ArrayList<>();
            for (Producto p : productoRepository.listar(filtro)) {
                if (p.isActivo()) {
                    activos.add(p);
                }
            }
            return activos;
        } catch (SQLException e) {
            throw new VentaException("No se pudo conectar con la base de datos. Verifica que MySQL este encendido.", e);
        }
    }

    /**
     * Agrega un producto al carrito (la venta en proceso).
     * Si el producto ya estaba, suma las cantidades.
     */
    public void agregarProducto(Venta venta, Producto producto, String cantidadTexto) throws VentaException {
        if (producto == null) {
            throw new VentaException("Selecciona un producto.");
        }
        if (!producto.isActivo()) {
            throw new VentaException("El producto esta inactivo.");
        }

        BigDecimal cantidad = leerCantidad(cantidadTexto);

        // Piezas, cajas y bolsas no se venden partidas; el kilo si admite decimales.
        if (producto.getUnidadMedida() != Producto.UnidadMedida.KG
                && cantidad.stripTrailingZeros().scale() > 0) {
            throw new VentaException("Este producto se vende por " + producto.getUnidadMedida()
                    + ": la cantidad debe ser un numero entero.");
        }

        // Cantidad total de este producto en el carrito, contando lo que ya habia
        BigDecimal enCarrito = BigDecimal.ZERO;
        DetalleVenta existente = null;
        for (DetalleVenta d : venta.getDetalles()) {
            if (d.getIdProducto() == producto.getIdProducto()) {
                existente = d;
                enCarrito = d.getCantidad();
            }
        }
        BigDecimal total = enCarrito.add(cantidad);

        if (total.compareTo(producto.getStock()) > 0) {
            throw new VentaException("Stock insuficiente de " + producto.getNombre()
                    + " (disponible: " + producto.getStock() + ").");
        }

        if (existente != null) {
            venta.getDetalles().remove(existente);
        }
        venta.getDetalles().add(new DetalleVenta(
                producto.getIdProducto(), producto.getNombre(), total, producto.getPrecio()));
    }

    /** Quita un renglon del carrito. */
    public void quitarProducto(Venta venta, int idProducto) {
        venta.getDetalles().removeIf(d -> d.getIdProducto() == idProducto);
    }

    /** Confirma la venta: valida y la guarda con su folio, usuario y descuento de inventario. */
    public void confirmar(Venta venta, MetodoPago metodoPago) throws VentaException {
        // Criterio: se rechazan ventas sin productos
        if (venta == null || venta.getDetalles().isEmpty()) {
            throw new VentaException("Agrega al menos un producto a la venta.");
        }
        // Criterio: no existen descuentos duplicados
        if (venta.getIdVenta() != 0) {
            throw new VentaException("Esta venta ya fue registrada.");
        }
        // PAG-01: se debe seleccionar un metodo de pago valido
        if (metodoPago == null) {
            throw new VentaException("Selecciona un metodo de pago.");
        }

        Usuario usuario = SesionActual.getUsuario();
        if (usuario == null) {
            throw new VentaException("No hay una sesion iniciada.");
        }

        venta.setIdUsuario(usuario.getIdUsuario());
        venta.setFolio("V-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")));

        try {
            ventaRepository.guardar(venta, metodoPago);
        } catch (IllegalArgumentException e) {
            // Stock insuficiente o producto inactivo detectado al guardar
            throw new VentaException(e.getMessage(), e);
        } catch (SQLException e) {
            throw new VentaException("No se pudo registrar la venta: " + e.getMessage(), e);
        }
    }

    /** Criterio: se rechazan cantidades invalidas (vacia, texto, cero, negativa, demasiados decimales). */
    private BigDecimal leerCantidad(String texto) throws VentaException {
        String limpio = texto == null ? "" : texto.trim().replace(',', '.');
        if (limpio.isEmpty()) {
            throw new VentaException("Ingresa la cantidad.");
        }

        BigDecimal cantidad;
        try {
            cantidad = new BigDecimal(limpio);
        } catch (NumberFormatException e) {
            throw new VentaException("La cantidad debe ser un numero.");
        }

        if (cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new VentaException("La cantidad debe ser mayor a cero.");
        }
        if (cantidad.stripTrailingZeros().scale() > 3) {
            throw new VentaException("La cantidad admite maximo 3 decimales.");
        }
        return cantidad;
    }
    // ------------------------------------------------------------------
    // BUS-02: consultar ventas
    // ------------------------------------------------------------------

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    /**
     * Busca ventas por fecha (desde/hasta, formato dd/MM/aaaa), identificador
     * (folio o numero de venta) y producto (nombre o codigo). Todos son opcionales.
     * Si nada coincide devuelve una lista vacia: la pantalla informa que no hay resultados.
     */
    public List<VentaResumen> consultar(String desdeTexto, String hastaTexto,
                                        String identificador, String producto) throws VentaException {
        LocalDate desde = leerFecha(desdeTexto, "desde");
        LocalDate hasta = leerFecha(hastaTexto, "hasta");

        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new VentaException("La fecha \"desde\" no puede ser posterior a la fecha \"hasta\".");
        }

        try {
            return ventaRepository.buscar(desde, hasta, identificador, producto);
        } catch (SQLException e) {
            throw new VentaException("No se pudo consultar las ventas. Verifica que MySQL este encendido.", e);
        }
    }

    /** Productos de una venta ya registrada. */
    public List<DetalleVenta> detalleDe(int idVenta) throws VentaException {
        try {
            return ventaRepository.detalleDe(idVenta);
        } catch (SQLException e) {
            throw new VentaException("No se pudo cargar el detalle de la venta.", e);
        }
    }

    /** Fecha opcional: vacia = sin filtro; si se escribe, debe ser una fecha real dd/MM/aaaa. */
    private LocalDate leerFecha(String texto, String nombreCampo) throws VentaException {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(texto.trim(), FORMATO_FECHA);
        } catch (DateTimeParseException e) {
            throw new VentaException("La fecha \"" + nombreCampo
                    + "\" no es valida. Usa el formato dd/mm/aaaa (por ejemplo 18/11/2026).");
        }
    }
}
