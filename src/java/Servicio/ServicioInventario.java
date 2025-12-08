package Servicio;

import DAO.ProductoDAO;
import DAO.MovInventarioDAO;
import Modelo.Producto;
import Modelo.MovInventario;
import java.sql.SQLException;
import java.util.Date;
import java.sql.Timestamp;

public class ServicioInventario {
    
    private ProductoDAO productoDAO = new ProductoDAO();
    private MovInventarioDAO movimientoDAO = new MovInventarioDAO();
    
    
    public void registrarEntrada(int idProducto, int cantidad, String motivo, 
                                Date fechaEntrada, String tipoEntrada, String proveedor,
                                Double precioCompra, String numeroFactura) throws SQLException {
        
        Producto producto = productoDAO.buscar(idProducto);
        if (producto == null) {
            throw new SQLException("Producto no encontrado con ID: " + idProducto);
        }
        
        int stockAnterior = producto.getStock();
        int nuevoStock = stockAnterior + cantidad;
        producto.setStock(nuevoStock);
        
        productoDAO.actualizar(producto);
        
        MovInventario movimiento = new MovInventario();
        movimiento.setTipoString("Entrada");
        movimiento.setCantidadEntrada(cantidad);
        movimiento.setFechaEntrada(fechaEntrada);
        movimiento.setTipoEntrada(tipoEntrada);
        movimiento.setProveedor(proveedor);
        movimiento.setDescripcion(motivo);
        movimiento.setPrecioCompra(precioCompra);
        movimiento.setNumeroFactura(numeroFactura);
        movimiento.setIdProducto(idProducto);
        movimiento.setStockAnterior(stockAnterior);
        movimiento.setStockNuevo(nuevoStock);
        
        movimientoDAO.registrarMovimiento(movimiento);
    }
    
   
    public void registrarSalida(int idProducto, int cantidad, String motivo,
                               Date fechaSalida, String tipoSalida, String cliente,
                               Double precioVenta) throws SQLException {
        
     
        Producto producto = productoDAO.buscar(idProducto);
        if (producto == null) {
            throw new SQLException("Producto no encontrado con ID: " + idProducto);
        }
        
      
        if (producto.getStock() < cantidad) {
            throw new SQLException("Stock insuficiente. Stock actual: " + producto.getStock() + ", solicitado: " + cantidad);
        }
        
        
        
        if (producto.getFechaVencimiento() != null && fechaSalida != null) {
            Date fechaVencimiento = producto.getFechaVencimiento();
            if (fechaSalida.after(fechaVencimiento)) {
                throw new SQLException("⚠️ ADVERTENCIA: El producto está vencido. Fecha vencimiento: " + fechaVencimiento);
            }
        }
        
        int stockAnterior = producto.getStock();
        int nuevoStock = stockAnterior - cantidad;
        producto.setStock(nuevoStock);
        
        productoDAO.actualizar(producto);
        
        MovInventario movimiento = new MovInventario();
        movimiento.setTipoString("Salida");
        movimiento.setCantidadSalida(cantidad);
        movimiento.setFechaSalida(fechaSalida);
        movimiento.setTipoSalida(tipoSalida);
        movimiento.setCliente(cliente);
        movimiento.setDescripcion(motivo);
        movimiento.setPrecioVenta(precioVenta);
        movimiento.setIdProducto(idProducto);
        movimiento.setStockAnterior(stockAnterior);
        movimiento.setStockNuevo(nuevoStock);
        
        movimientoDAO.registrarMovimiento(movimiento);
    }
    
   
    public int obtenerStockActual(int idProducto) throws SQLException {
        Producto producto = productoDAO.buscar(idProducto);
        if (producto == null) {
            throw new SQLException("Producto no encontrado");
        }
        return producto.getStock();
    }
    
    
    public boolean estaProximoVencer(int idProducto) throws SQLException {
        Producto producto = productoDAO.buscar(idProducto);
        if (producto == null || producto.getFechaVencimiento() == null) {
            return false;
        }
       
        Date fechaVencimiento = producto.getFechaVencimiento();
        java.util.Date ahora = new java.util.Date();
        java.util.Date en30Dias = new java.util.Date(ahora.getTime() + (30L * 24 * 60 * 60 * 1000));
        
        return fechaVencimiento.after(ahora) && fechaVencimiento.before(en30Dias);
    }
}