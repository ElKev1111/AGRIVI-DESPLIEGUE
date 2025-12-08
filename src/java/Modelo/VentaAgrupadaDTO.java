package Modelo;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO de apoyo para la vista de ADMIN.
 *
 * Representa una compra "agrupada" por codigoCompra. Si una venta antigua no
 * tiene codigoCompra, el DAO generará un codigoGrupo de respaldo tipo:
 * "LEG-{idVenta}".
 *
 * Este DTO NO se persiste en BD.
 */
public class VentaAgrupadaDTO implements Serializable {

    private String codigoGrupo;        // codigoCompra real o fallback LEG-idVenta
    private LocalDateTime fechaVenta;  // fecha mínima del grupo
    private int idUsuario;

    private String nombreUsuario;
    private String correoUsuario;

    private int totalItems;            // suma de cantidades del grupo
    private int totalLineas;           // número de filas/lineas en ventas
    private float totalPagar;          // suma del subtotal (sin IVA)

    public VentaAgrupadaDTO() {
    }

    public String getCodigoGrupo() {
        return codigoGrupo;
    }

    public void setCodigoGrupo(String codigoGrupo) {
        this.codigoGrupo = codigoGrupo;
    }

    public LocalDateTime getFechaVenta() {
        return fechaVenta;
    }

    public void setFechaVenta(LocalDateTime fechaVenta) {
        this.fechaVenta = fechaVenta;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public String getCorreoUsuario() {
        return correoUsuario;
    }

    public void setCorreoUsuario(String correoUsuario) {
        this.correoUsuario = correoUsuario;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(int totalItems) {
        this.totalItems = totalItems;
    }

    public int getTotalLineas() {
        return totalLineas;
    }

    public void setTotalLineas(int totalLineas) {
        this.totalLineas = totalLineas;
    }

    public float getTotalPagar() {
        return totalPagar;
    }

    public void setTotalPagar(float totalPagar) {
        this.totalPagar = totalPagar;
    }

    @Override
    public String toString() {
        return "VentaAgrupadaDTO{"
                + "codigoGrupo=" + codigoGrupo
                + ", fechaVenta=" + fechaVenta
                + ", idUsuario=" + idUsuario
                + ", totalItems=" + totalItems
                + ", totalLineas=" + totalLineas
                + ", totalPagar=" + totalPagar
                + '}';
    }
}
