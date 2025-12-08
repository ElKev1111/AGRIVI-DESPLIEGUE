package Modelo;

import javax.persistence.*;
import java.util.Date;
import java.io.Serializable;

@Entity
@Table(name = "movimientos_inventario")
public class MovInventario implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idMovInventario")
    private Integer idMovInventario;

    @Column(name = "tipoString")
    private String tipoString;

    @Column(name = "cantidadEntrada")
    private Integer cantidadEntrada;

    @Column(name = "cantidadSalida")
    private Integer cantidadSalida;

    @Temporal(TemporalType.DATE)
    @Column(name = "fechaEntrada")
    private Date fechaEntrada;

    @Temporal(TemporalType.DATE)
    @Column(name = "fechaSalida")
    private Date fechaSalida;

    @Column(name = "tipoEntrada")
    private String tipoEntrada;

    @Column(name = "tipoSalida")
    private String tipoSalida;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "proveedor")
    private String proveedor;

    @Column(name = "cliente")
    private String cliente;

    @Column(name = "precioCompra")
    private Double precioCompra;

    @Column(name = "precioVenta")
    private Double precioVenta;

    @Column(name = "numeroFactura")
    private String numeroFactura;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "fechaRegistro", insertable = false, updatable = false)
    private Date fechaRegistro;

    @Column(name = "idProducto")
    private Integer idProducto;

    @Column(name = "stockAnterior")
    private Integer stockAnterior;

    @Column(name = "stockNuevo")
    private Integer stockNuevo;

    @Column(name = "imagenProducto")
    private String imagenProducto;

    @Column(name = "archivoFactura")
    private String archivoFactura;

    @Column(name = "usuarioRegistro")
    private String usuarioRegistro;

    // Constructores
    public MovInventario() {
    }

    // Getters y Setters
    public Integer getIdMovInventario() {
        return idMovInventario;
    }

    public void setIdMovInventario(Integer idMovInventario) {
        this.idMovInventario = idMovInventario;
    }

    public String getTipoString() {
        return tipoString;
    }

    public void setTipoString(String tipoString) {
        this.tipoString = tipoString;
    }

    public Integer getCantidadEntrada() {
        return cantidadEntrada;
    }

    public void setCantidadEntrada(Integer cantidadEntrada) {
        this.cantidadEntrada = cantidadEntrada;
    }

    public Integer getCantidadSalida() {
        return cantidadSalida;
    }

    public void setCantidadSalida(Integer cantidadSalida) {
        this.cantidadSalida = cantidadSalida;
    }

    public Date getFechaEntrada() {
        return fechaEntrada;
    }

    public void setFechaEntrada(Date fechaEntrada) {
        this.fechaEntrada = fechaEntrada;
    }

    public Date getFechaSalida() {
        return fechaSalida;
    }

    public void setFechaSalida(Date fechaSalida) {
        this.fechaSalida = fechaSalida;
    }

    public String getTipoEntrada() {
        return tipoEntrada;
    }

    public void setTipoEntrada(String tipoEntrada) {
        this.tipoEntrada = tipoEntrada;
    }

    public String getTipoSalida() {
        return tipoSalida;
    }

    public void setTipoSalida(String tipoSalida) {
        this.tipoSalida = tipoSalida;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getProveedor() {
        return proveedor;
    }

    public void setProveedor(String proveedor) {
        this.proveedor = proveedor;
    }

    public String getCliente() {
        return cliente;
    }

    public void setCliente(String cliente) {
        this.cliente = cliente;
    }

    public Double getPrecioCompra() {
        return precioCompra;
    }

    public void setPrecioCompra(Double precioCompra) {
        this.precioCompra = precioCompra;
    }

    public Double getPrecioVenta() {
        return precioVenta;
    }

    public void setPrecioVenta(Double precioVenta) {
        this.precioVenta = precioVenta;
    }

    public String getNumeroFactura() {
        return numeroFactura;
    }

    public void setNumeroFactura(String numeroFactura) {
        this.numeroFactura = numeroFactura;
    }

    public Date getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(Date fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public Integer getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(Integer idProducto) {
        this.idProducto = idProducto;
    }

    public Integer getStockAnterior() {
        return stockAnterior;
    }

    public void setStockAnterior(Integer stockAnterior) {
        this.stockAnterior = stockAnterior;
    }

    public Integer getStockNuevo() {
        return stockNuevo;
    }

    public void setStockNuevo(Integer stockNuevo) {
        this.stockNuevo = stockNuevo;
    }

    public String getImagenProducto() {
        return imagenProducto;
    }

    public void setImagenProducto(String imagenProducto) {
        this.imagenProducto = imagenProducto;
    }

    public String getArchivoFactura() {
        return archivoFactura;
    }

    public void setArchivoFactura(String archivoFactura) {
        this.archivoFactura = archivoFactura;
    }

    public String getUsuarioRegistro() {
        return usuarioRegistro;
    }

    public void setUsuarioRegistro(String usuarioRegistro) {
        this.usuarioRegistro = usuarioRegistro;
    }
}
