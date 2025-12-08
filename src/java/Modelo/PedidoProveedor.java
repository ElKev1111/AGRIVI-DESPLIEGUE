package Modelo;

import java.io.Serializable;
import java.util.Date; 

public class PedidoProveedor implements Serializable {
    
    // CAMPOS DE PEDIDO
    private int idPedido;
    private int idProveedor;
    private String nombreProveedor;
    private String descripcionPedido;    
    
    // CAMPOS DE DETALLE DE PRODUCTO
    private int idProducto;    
    private String nombreProducto;    
    private int cantidad;    
    
    // CAMPOS DE TIEMPO Y ESTADO
    private Date fechaPedido; 
    private String estado;
    private Date fechaActualizacion;

    // CONSTRUCTORES
    public PedidoProveedor() {
    }


    public int getIdPedido() {
        return idPedido;
    }

    public void setIdPedido(int idPedido) {
        this.idPedido = idPedido;
    }

    public int getIdProveedor() {
        return idProveedor;
    }

    public void setIdProveedor(int idProveedor) {
        this.idProveedor = idProveedor;
    }

    public String getNombreProveedor() {
        return nombreProveedor;
    }

    public void setNombreProveedor(String nombreProveedor) {
        this.nombreProveedor = nombreProveedor;
    }

    public String getDescripcionPedido() {
        return descripcionPedido;
    }

    public void setDescripcionPedido(String descripcionPedido) {
        this.descripcionPedido = descripcionPedido;
    }

    public int getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(int idProducto) {
        this.idProducto = idProducto;
    }

    public String getNombreProducto() {
        return nombreProducto;
    }

    public void setNombreProducto(String nombreProducto) {
        this.nombreProducto = nombreProducto;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    // --- MÉTODOS DE FECHA CORREGIDOS ---
    
    public Date getFechaPedido() {
        return fechaPedido;
    }

    public void setFechaPedido(Date fechaPedido) {
        this.fechaPedido = fechaPedido;
    }

    public Date getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(Date fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }
    
    // --- FIN MÉTODOS DE FECHA CORREGIDOS ---

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}