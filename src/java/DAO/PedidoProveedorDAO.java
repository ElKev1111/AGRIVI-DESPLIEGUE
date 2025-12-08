package DAO;

import Modelo.PedidoProveedor;
import Controlador.Conexion; // ✅ Importación de la clase de conexión unificada
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class PedidoProveedorDAO implements Serializable{
    private static final long serialVersionUID =1L;
    private transient PreparedStatement ps;
    private transient ResultSet rs;

    
    private Connection getConnection() throws SQLException {
        Connection conn = Conexion.conectar(); 
        
        if (conn == null) {
            // Lanza una excepción si la conexión falla (más robusto)
            throw new SQLException("Error: No se pudo establecer la conexión a la base de datos.");
        }
        return conn;
    }

    /**
     * Registra un nuevo PedidoProveedor en la base de datos.
     */
    public boolean registrar(PedidoProveedor pedido) throws SQLException {
        // Usa NOW() para establecer fechaPedido directamente en la base de datos
        String sql = "INSERT INTO pedido_proveedor (idProveedor, nombreProveedor, idProducto, nombreProducto, cantidad, descripcionPedido, estado, fechaPedido) " +
                      "VALUES (?, ?, ?, ?, ?, ?, ?, NOW())";    
        
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, pedido.getIdProveedor());
            ps.setString(2, pedido.getNombreProveedor());
            ps.setInt(3, pedido.getIdProducto());
            ps.setString(4, pedido.getNombreProducto());
            ps.setInt(5, pedido.getCantidad());
            ps.setString(6, pedido.getDescripcionPedido());
            ps.setString(7, pedido.getEstado());    
            
            return ps.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error al registrar el pedido: " + e.getMessage());
            throw e; // Relanza para que el Bean lo maneje
        }
    }

    /**
     * Lista todos los PedidoProveedor de la base de datos.
     */
    public List<PedidoProveedor> listar() throws SQLException {
        List<PedidoProveedor> lista = new ArrayList<>();
        String sql = "SELECT idPedido, idProveedor               , nombreProveedor, idProducto, nombreProducto, cantidad, descripcionPedido, estado, fechaPedido, fechaActualizacion " +
                      "FROM pedido_proveedor ORDER BY fechaPedido DESC";
        
        try (Connection conn = getConnection(); 
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                PedidoProveedor p = new PedidoProveedor();
                p.setIdPedido(rs.getInt("idPedido"));
                p.setIdProveedor(rs.getInt("idProveedor"));
                p.setNombreProveedor(rs.getString("nombreProveedor"));
                p.setIdProducto(rs.getInt("idProducto"));
                p.setNombreProducto(rs.getString("nombreProducto"));
                p.setCantidad(rs.getInt("cantidad"));
                p.setDescripcionPedido(rs.getString("descripcionPedido"));
                p.setEstado(rs.getString("estado"));
                
                
                // Asignación directa a java.util.Date (compatible con java.sql.Timestamp)
                p.setFechaPedido(rs.getTimestamp("fechaPedido"));
                p.setFechaActualizacion(rs.getTimestamp("fechaActualizacion"));
                
                lista.add(p);
            }
        } catch (SQLException e) {
            System.err.println("Error al listar pedidos: " + e.getMessage());
            throw e; // Relanza para que el Bean lo maneje
        }
        return lista;
    }

    /**
     * Actualiza el estado y la fecha de actualización de un pedido.
     */
    public boolean actualizarEstado(PedidoProveedor pedido) throws SQLException {
        // Usa NOW() para actualizar la fecha directamente en la base de datos
        String sql = "UPDATE pedido_proveedor SET estado = ?, fechaActualizacion = NOW() WHERE idPedido = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, pedido.getEstado());
            ps.setInt(2, pedido.getIdPedido());
            
            return ps.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error al actualizar el estado del pedido: " + e.getMessage());
            throw e; // Relanza para que el Bean lo maneje
        }
    }

    /**
     * Obtiene el detalle de un pedido por su identificador.
     */
    public PedidoProveedor obtenerPorId(int idPedido) throws SQLException {
        String sql = "SELECT idPedido, idProveedor, nombreProveedor, idProducto, nombreProducto, cantidad, descripcionPedido, estado, fechaPedido, fechaActualizacion "
                + "FROM pedido_proveedor WHERE idPedido = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idPedido);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    PedidoProveedor pedido = new PedidoProveedor();
                    pedido.setIdPedido(rs.getInt("idPedido"));
                    pedido.setIdProveedor(rs.getInt("idProveedor"));
                    pedido.setNombreProveedor(rs.getString("nombreProveedor"));
                    pedido.setIdProducto(rs.getInt("idProducto"));
                    pedido.setNombreProducto(rs.getString("nombreProducto"));
                    pedido.setCantidad(rs.getInt("cantidad"));
                    pedido.setDescripcionPedido(rs.getString("descripcionPedido"));
                    pedido.setEstado(rs.getString("estado"));
                    pedido.setFechaPedido(rs.getTimestamp("fechaPedido"));
                    pedido.setFechaActualizacion(rs.getTimestamp("fechaActualizacion"));
                    return pedido;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener el pedido por ID: " + e.getMessage());
            throw e;
        }

        return null;
    }
}