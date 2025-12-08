package Modelo;

import java.io.Serializable;

public class CarritoItem implements Serializable {
    
    private Producto producto;
    private int cantidad;
    
    public CarritoItem(Producto producto, int cantidad) {
        this.producto = producto;
        this.cantidad = cantidad;
    }
    
    public float getSubtotal() {
        return this.cantidad * this.producto.getPrecioVenta();
    }

    // Getters y Setters
    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }
    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
}
