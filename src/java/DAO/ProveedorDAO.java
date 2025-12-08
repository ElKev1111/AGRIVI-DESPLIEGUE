package DAO;

import Modelo.Proveedor;
import Controlador.Conexion;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date; 
import java.util.ArrayList;
import java.util.List;

public class ProveedorDAO implements Serializable {
private static final long serialVersionUID = 1L;

    PreparedStatement ps;
    ResultSet rs;

    public List<Proveedor> listar() throws SQLException {
        List<Proveedor> listaProveedores = new ArrayList<>();
        Connection con = null;
        try {
            String sql = "SELECT * FROM proveedor ORDER BY idProveedor DESC";
            con = Conexion.conectar();
            ps = con.prepareStatement(sql);
            rs = ps.executeQuery();

            while (rs.next()) {
                Proveedor p = new Proveedor();
                p.setIdProveedor(rs.getInt("idProveedor"));
                p.setNombreProveedor(rs.getString("nombreProveedor"));
                p.setCorreo(rs.getString("correo"));
                p.setCelular(rs.getString("celular"));
                p.setDireccion(rs.getString("direccion"));
                p.setProducto(rs.getString("producto"));
                p.setPrecio(rs.getDouble("precio"));
                p.setFechaRegistro(rs.getTimestamp("fechaRegistro"));
                Timestamp fechaActualizacion = rs.getTimestamp("fechaActualizacion");
                p.setFechaActualizacion(fechaActualizacion);
                p.setEstado(rs.getString("estado"));
                listaProveedores.add(p);
            }

        } catch (SQLException e) {
            System.out.println("Error al listar proveedores: " + e.getMessage());
            throw e;
        } finally {
            if (rs != null) try {
                rs.close();
            } catch (SQLException ignored) {
            }
            if (ps != null) try {
                ps.close();
            } catch (SQLException ignored) {
            }
            if (con != null) {
                con.close();
            }
        }
        return listaProveedores;
    }

    public List<Proveedor> listarActivos() throws SQLException {
        List<Proveedor> listaProveedores = new ArrayList<>();
        Connection con = null;
        try {
            String sql = "SELECT * FROM proveedor WHERE estado = ? ORDER BY idProveedor DESC";
            con = Conexion.conectar();
            ps = con.prepareStatement(sql);
            ps.setString(1, "Activo");
            rs = ps.executeQuery();

            while (rs.next()) {
                Proveedor p = new Proveedor();
                p.setIdProveedor(rs.getInt("idProveedor"));
                p.setNombreProveedor(rs.getString("nombreProveedor"));
                p.setCorreo(rs.getString("correo"));
                p.setCelular(rs.getString("celular"));
                p.setDireccion(rs.getString("direccion"));
                p.setProducto(rs.getString("producto"));
                p.setPrecio(rs.getDouble("precio"));
                p.setFechaRegistro(rs.getTimestamp("fechaRegistro"));
                Timestamp fechaActualizacion = rs.getTimestamp("fechaActualizacion");
                p.setFechaActualizacion(fechaActualizacion);
                p.setEstado(rs.getString("estado"));
                listaProveedores.add(p);
            }

        } catch (SQLException e) {
            System.out.println("Error al listar proveedores activos: " + e.getMessage());
            throw e;
        } finally {
            if (rs != null) try {
                rs.close();
            } catch (SQLException ignored) {
            }
            if (ps != null) try {
                ps.close();
            } catch (SQLException ignored) {
            }
            if (con != null) {
                con.close();
            }
        }
        return listaProveedores;
    }

    public boolean agregar(Proveedor proveedor) throws SQLException {
        String sql = "INSERT INTO proveedor (nombreProveedor, correo, celular, direccion, producto, precio, fechaRegistro, estado) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        Connection con = null;
        try {
            con = Conexion.conectar();
            ps = con.prepareStatement(sql);
            ps.setString(1, proveedor.getNombreProveedor());
            ps.setString(2, proveedor.getCorreo());
            ps.setString(3, proveedor.getCelular());
            ps.setString(4, proveedor.getDireccion());
            ps.setString(5, proveedor.getProducto());
            ps.setDouble(6, proveedor.getPrecio());
            ps.setTimestamp(7, new Timestamp(new Date().getTime()));

            ps.setString(8, proveedor.getEstado() != null ? proveedor.getEstado() : "Activo");
            int filasAfectadas = ps.executeUpdate();
            return filasAfectadas > 0;
        } catch (SQLException e) {
            System.out.println("Error al agregar proveedor: " + e.getMessage());
            throw e;
        } finally {
            if (ps != null) try {
                ps.close();
            } catch (SQLException ignored) {
            }
            if (con != null) {
                con.close();
            }
        }
    }

    public boolean actualizar(Proveedor proveedor) throws SQLException {
        String sql = "UPDATE proveedor SET nombreProveedor = ?, correo = ?, celular = ?, direccion = ?, "
                + "producto = ?, precio = ?, fechaActualizacion = ?, estado = ? WHERE idProveedor = ?";
        Connection con = null;
        try {
            con = Conexion.conectar();
            ps = con.prepareStatement(sql);
            ps.setString(1, proveedor.getNombreProveedor());
            ps.setString(2, proveedor.getCorreo());
            ps.setString(3, proveedor.getCelular());
            ps.setString(4, proveedor.getDireccion());
            ps.setString(5, proveedor.getProducto());
            ps.setDouble(6, proveedor.getPrecio());
            ps.setTimestamp(7, new Timestamp(new Date().getTime()));
            ps.setString(8, proveedor.getEstado());
            ps.setInt(9, proveedor.getIdProveedor());
            int filasAfectadas = ps.executeUpdate();
            return filasAfectadas > 0;
        } catch (SQLException e) {
            System.out.println("Error al actualizar proveedor: " + e.getMessage());
            throw e;
        } finally {
            if (ps != null) try {
                ps.close();
            } catch (SQLException ignored) {
            }
            if (con != null) {
                con.close();
            }
        }
    }

    public boolean eliminar(int idProveedor) throws SQLException {
        return cambiarEstado(idProveedor, "Inactivo");
    }

    public Proveedor obtenerPorId(int idProveedor) throws SQLException {
        String sql = "SELECT * FROM proveedor WHERE idProveedor = ?";
        Connection con = null;
        try {
            con = Conexion.conectar();
            ps = con.prepareStatement(sql);
            ps.setInt(1, idProveedor);
            rs = ps.executeQuery();
            if (rs.next()) {
                Proveedor p = new Proveedor();
                p.setIdProveedor(rs.getInt("idProveedor"));
                p.setNombreProveedor(rs.getString("nombreProveedor"));
                p.setCorreo(rs.getString("correo"));
                p.setCelular(rs.getString("celular"));
                p.setDireccion(rs.getString("direccion"));
                p.setProducto(rs.getString("producto"));
                p.setPrecio(rs.getDouble("precio"));
                p.setFechaRegistro(rs.getTimestamp("fechaRegistro"));
                Timestamp fechaActualizacion = rs.getTimestamp("fechaActualizacion");
                p.setFechaActualizacion(fechaActualizacion);
                p.setEstado(rs.getString("estado"));
                return p;
            }
            return null;
        } catch (SQLException e) {
            System.out.println("Error al obtener proveedor por ID: " + e.getMessage());
            throw e;
        } finally {
            if (rs != null) try {
                rs.close();
            } catch (SQLException ignored) {
            }
            if (ps != null) try {
                ps.close();
            } catch (SQLException ignored) {
            }
            if (con != null) {
                con.close();
            }
        }
    }

    public boolean cambiarEstado(int idProveedor, String nuevoEstado) throws SQLException {
        String sql = "UPDATE proveedor SET estado = ?, fechaActualizacion = ? WHERE idProveedor = ?";
        Connection con = null;
        try {
            con = Conexion.conectar();
            ps = con.prepareStatement(sql);
            ps.setString(1, nuevoEstado);
            ps.setTimestamp(2, new Timestamp(new Date().getTime()));
            ps.setInt(3, idProveedor);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error al cambiar estado del proveedor: " + e.getMessage());
            throw e;
        } finally {
            if (ps != null) try {
                ps.close();
            } catch (SQLException ignored) {
            }
            if (con != null) {
                con.close();
            }
        }
    }
    public Proveedor buscar(int idProveedor) throws SQLException {
    String sql = "SELECT * FROM proveedor WHERE idProveedor = ?";
    Connection con = null;
    try {
        con = Conexion.conectar();
        ps = con.prepareStatement(sql);
        ps.setInt(1, idProveedor);
        rs = ps.executeQuery();

        if (rs.next()) {
            Proveedor p = new Proveedor();
            p.setIdProveedor(rs.getInt("idProveedor"));
            p.setNombreProveedor(rs.getString("nombreProveedor"));
            p.setCorreo(rs.getString("correo"));
            p.setCelular(rs.getString("celular"));
            p.setDireccion(rs.getString("direccion"));
            p.setProducto(rs.getString("producto"));
            p.setPrecio(rs.getDouble("precio"));

            p.setFechaRegistro(rs.getTimestamp("fechaRegistro"));
            Timestamp fechaActualizacion = rs.getTimestamp("fechaActualizacion");
            p.setFechaActualizacion(fechaActualizacion);

            p.setEstado(rs.getString("estado"));
            return p;
        }

        return null;
    } catch (SQLException e) {
        System.out.println("Error al buscar proveedor por id: " + e.getMessage());
        throw e;
    } finally {
        if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
        if (ps != null) try { ps.close(); } catch (SQLException ignored) {}
        if (con != null) con.close();
    }
}

}
