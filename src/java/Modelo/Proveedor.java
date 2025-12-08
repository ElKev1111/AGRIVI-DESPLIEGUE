package Modelo;

import java.io.Serializable;
import java.util.Date; // 🚨 CORRECCIÓN: Usar java.util.Date para compatibilidad con JDBC

public class Proveedor implements Serializable {

    private int idProveedor;
    private String nombreProveedor;
    private String correo; // Clave para el envío de email
    private String celular;
    private String direccion;
    private String producto;
    private double precio;
    private Date fechaRegistro; // 🔄 Cambiado de LocalDateTime a Date
    private Date fechaActualizacion; // 🔄 Cambiado de LocalDateTime a Date
    private String estado;

    // Constructor vacío
    public Proveedor() {
    }

    // Constructor con parámetros (actualizado)
    public Proveedor(int idProveedor, String nombreProveedor, String correo, String celular,
            String direccion, String producto, double precio, Date fechaRegistro,
            Date fechaActualizacion, String estado) {
        this.idProveedor = idProveedor;
        this.nombreProveedor = nombreProveedor;
        this.correo = correo;
        this.celular = celular;
        this.direccion = direccion;
        this.producto = producto;
        this.precio = precio;
        this.fechaRegistro = fechaRegistro;
        this.fechaActualizacion = fechaActualizacion;
        this.estado = estado;
    }

    // Getters y Setters (Actualizados para Date)
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

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getCelular() {
        return celular;
    }

    public void setCelular(String celular) {
        this.celular = celular;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getProducto() {
        return producto;
    }

    public void setProducto(String producto) {
        this.producto = producto;
    }

    public double getPrecio() {
        return precio;
    }

    public void setPrecio(double precio) {
        this.precio = precio;
    }

    public Date getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(Date fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public Date getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(Date fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
