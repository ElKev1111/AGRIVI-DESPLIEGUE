package Modelo;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Entidad de ventas (una fila por producto vendido).
 *
 * NOTA IMPORTANTE DEL PROCESO: - Se agregó soporte para "codigoCompra" para
 * poder agrupar varias filas en una sola compra lógica. - totalPagar se guarda
 * sin IVA (subtotal).
 */
public class Ventas implements Serializable {

    private int idVenta;
    private LocalDateTime fechaVenta;
    private Producto producto;
    private Usuario usuario;
    private int cantidad;
    private float totalPagar;

    // NUEVO: Identificador de compra para agrupar varias líneas
    private String codigoCompra;

    public Ventas() {
    }

    public int getIdVenta() {
        return idVenta;
    }

    public void setIdVenta(int idVenta) {
        this.idVenta = idVenta;
    }

    public LocalDateTime getFechaVenta() {
        return fechaVenta;
    }

    public void setFechaVenta(LocalDateTime fechaVenta) {
        this.fechaVenta = fechaVenta;
    }

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public float getTotalPagar() {
        return totalPagar;
    }

    public void setTotalPagar(float totalPagar) {
        this.totalPagar = totalPagar;
    }

    public String getCodigoCompra() {
        return codigoCompra;
    }

    public void setCodigoCompra(String codigoCompra) {
        this.codigoCompra = codigoCompra;
    }

    /**
     * Útil para mostrar fecha en tablas cuando no quieres convertir en XHTML.
     */
    public String getFechaVentaFormateada() {
        if (fechaVenta == null) {
            return "";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return fechaVenta.format(formatter);
    }

    @Override
    public String toString() {
        return "Ventas{"
                + "idVenta=" + idVenta
                + ", fechaVenta=" + fechaVenta
                + ", producto=" + (producto != null ? producto.getIdProducto() : null)
                + ", usuario=" + (usuario != null ? usuario.getId() : null)
                + ", cantidad=" + cantidad
                + ", totalPagar=" + totalPagar
                + ", codigoCompra=" + codigoCompra
                + '}';
    }
}
