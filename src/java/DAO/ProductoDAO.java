package DAO;

import Modelo.Producto;
import Controlador.Conexion;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ProductoDAO implements Serializable {

    private static final long serialversionUID = 1;

    private transient PreparedStatement ps;
    private transient ResultSet rs;

    public List<Producto> listar() throws SQLException {
        List<Producto> listaProducto = new ArrayList<>();
        
        String sql = "SELECT idProducto, nombreProducto, idProveedor, nombreProveedor, precioProducto, " +
                     "descripcion, imagen, tipo, fechaIngreso, fechaVencimiento, stock, categoria " +
                     "FROM producto";

        try (Connection con = Conexion.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Producto p = mapearProductoDesdeResultSet(rs);
                listaProducto.add(p);
            }
        } catch (SQLException e) {
            System.out.println("Error al listar los producto: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }

        return listaProducto;
    }

    public List<Producto> listarPorProveedor(int idProveedor) throws SQLException {
        List<Producto> lista = new ArrayList<>();
        String sql = "SELECT * FROM producto WHERE idProveedor = ?";

        try (Connection con = Conexion.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idProveedor);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Producto p = new Producto();
                    p.setIdProducto(rs.getInt("idProducto"));
                    p.setNombreProducto(rs.getString("nombreProducto"));
                    p.setIdProveedor(rs.getInt("idProveedor"));
                    p.setNombreProveedor(rs.getString("nombreProveedor"));
                    p.setPrecioProducto(rs.getFloat("precioProducto"));
                    p.setDescripcion(rs.getString("descripcion"));
                    p.setImagen(rs.getString("imagen")); 
                    p.setFechaVencimiento(rs.getTimestamp("fechaVencimiento"));
                    p.setTipo(rs.getString("tipo"));
                    p.setFechaIngreso(rs.getTimestamp("fechaIngreso"));
                    p.setStock(rs.getInt("stock"));
                    p.setCategoria(rs.getString("categoria"));
                    lista.add(p);
                }
            }
        }
        return lista;
    }

    public void agregar(Producto producto) throws SQLException {

    String sql = "INSERT INTO producto "
            + "(nombreProducto, idProveedor, nombreProveedor, precioProducto, descripcion, "
            + "imagen, tipo, stock, fechaIngreso, fechaVencimiento, categoria) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    try (Connection con = ConnBD.getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setString(1, producto.getNombreProducto());
        ps.setInt(2, producto.getIdProveedor());
        ps.setString(3, producto.getNombreProveedor());
        ps.setFloat(4, producto.getPrecioProducto());
        ps.setString(5, producto.getDescripcion());

        // imagen puede ser NULL
        if (producto.getImagen() == null || producto.getImagen().trim().isEmpty()) {
            ps.setNull(6, java.sql.Types.VARCHAR);
        } else {
            ps.setString(6, producto.getImagen());
        }

        ps.setString(7, producto.getTipo());

        // stock inicial (tu regla: siempre 0)
        ps.setInt(8, producto.getStock());

        // fechaIngreso
        if (producto.getFechaIngreso() != null) {
            ps.setDate(9, new java.sql.Date(producto.getFechaIngreso().getTime()));
        } else {
            ps.setNull(9, java.sql.Types.DATE);
        }

        // fechaVencimiento
        if (producto.getFechaVencimiento() != null) {
            ps.setDate(10, new java.sql.Date(producto.getFechaVencimiento().getTime()));
        } else {
            ps.setNull(10, java.sql.Types.DATE);
        }
        ps.setString(11, producto.getCategoria());

        ps.executeUpdate();
    }
}


   public void actualizar(Producto producto) throws SQLException {

    String sql = "UPDATE producto SET "
            + "nombreProducto = ?, "
            + "idProveedor = ?, "
            + "nombreProveedor = ?, "
            + "precioProducto = ?, "
            + "descripcion = ?, "
            + "imagen = ?, "
            + "tipo = ?, "
            + "stock = ?, "
            + "fechaVencimiento = ?, "
            + "categoria = ? "
            + "WHERE idProducto = ?";

    try (Connection con = ConnBD.getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setString(1, producto.getNombreProducto());
        ps.setInt(2, producto.getIdProveedor());
        ps.setString(3, producto.getNombreProveedor());
        ps.setFloat(4, producto.getPrecioProducto());
        ps.setString(5, producto.getDescripcion());

        // imagen puede ser NULL
        if (producto.getImagen() == null || producto.getImagen().trim().isEmpty()) {
            ps.setNull(6, java.sql.Types.VARCHAR);
        } else {
            ps.setString(6, producto.getImagen());
        }

        ps.setString(7, producto.getTipo());

        // si en tu edición NO quieres tocar stock desde el dashboard,
        // deja este campo igual en el Bean o usa el UPDATE alternativo sin stock (abajo).
        ps.setInt(8, producto.getStock());

        // fechaVencimiento
        if (producto.getFechaVencimiento() != null) {
            ps.setDate(9, new java.sql.Date(producto.getFechaVencimiento().getTime()));
        } else {
            ps.setNull(9, java.sql.Types.DATE);
        }

        // NUEVO: categoria
        ps.setString(10, producto.getCategoria());

        ps.setInt(11, producto.getIdProducto());

        ps.executeUpdate();
    }
}


    public void eliminar(Producto p) throws SQLException {
        String sql = "DELETE FROM producto WHERE idProducto = ?";

        try (Connection con = Conexion.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, p.getIdProducto());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error al eliminar producto: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public boolean actualizarStock(int idProducto, int nuevoStock) throws SQLException {
        String sql = "UPDATE producto SET stock = ? WHERE idProducto = ?";

        try (Connection con = Conexion.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, nuevoStock);
            ps.setInt(2, idProducto);
            return ps.executeUpdate() > 0;
        }
    }

    public Producto buscar(int idProducto) throws SQLException {
        String sql = "SELECT * FROM producto WHERE idProducto = ?";

        try (Connection con = Conexion.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Producto p = new Producto();
                    p.setIdProducto(rs.getInt("idProducto"));
                    p.setNombreProducto(rs.getString("nombreProducto"));
                    p.setIdProveedor(rs.getInt("idProveedor"));
                    p.setNombreProveedor(rs.getString("nombreProveedor"));
                    p.setPrecioProducto(rs.getFloat("precioProducto"));
                    p.setDescripcion(rs.getString("descripcion"));
                    p.setCategoria(rs.getString("categoria"));
                    p.setImagen(rs.getString("imagen")); 
                    p.setFechaVencimiento(rs.getTimestamp("fechaVencimiento"));
                    p.setTipo(rs.getString("tipo"));
                    p.setCategoria(rs.getString("categoria"));
                    p.setFechaIngreso(rs.getTimestamp("fechaIngreso"));
                    p.setStock(rs.getInt("stock"));
                    return p;
                }
            }
        }
        return null;
    }

    private Producto mapearProductoDesdeResultSet(ResultSet rs) throws SQLException {
        Producto p = new Producto();

        p.setIdProducto(rs.getInt("idProducto"));
        p.setNombreProducto(rs.getString("nombreProducto"));
        p.setIdProveedor(rs.getInt("idProveedor"));
        p.setNombreProveedor(rs.getString("nombreProveedor"));
        p.setPrecioProducto(rs.getFloat("precioProducto"));
        p.setDescripcion(rs.getString("descripcion"));

        try {
            p.setImagen(rs.getString("imagen"));  
        } catch (SQLException ex) {
            p.setImagen(null);
        }

        p.setTipo(rs.getString("tipo"));
        p.setCategoria(rs.getString("categoria"));

        Timestamp tsIngreso = rs.getTimestamp("fechaIngreso");
        if (tsIngreso != null) {
            p.setFechaIngreso(new Date(tsIngreso.getTime()));
        } else {
            p.setFechaIngreso(null);
        }

        Timestamp tsVenc = null;
        try {
            tsVenc = rs.getTimestamp("fechaVencimiento");
        } catch (SQLException ex) {
            tsVenc = null;
        }

        if (tsVenc != null) {
            p.setFechaVencimiento(new Date(tsVenc.getTime()));
        } else {
            p.setFechaVencimiento(null);
        }

        try {
            p.setStock(rs.getInt("stock"));
        } catch (SQLException ex) {
            p.setStock(0);
        }

        return p;
    }

    public boolean tieneMovimientosInventario(int idProducto) throws SQLException {
        String sql = "SELECT 1 FROM movimientos_inventario WHERE idProducto = ? LIMIT 1";
        try (Connection con = Conexion.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
