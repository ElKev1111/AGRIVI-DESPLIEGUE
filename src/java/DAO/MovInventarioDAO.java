package DAO;

import Modelo.MovInventario;
import java.io.Serializable;
import java.sql.*;
import java.util.*;
import java.util.Date;

public class MovInventarioDAO implements Serializable {

    private static final long serialVersionUID = 1L;
    private transient PreparedStatement ps;
    private transient ResultSet rs;

    private Connection conexion;

    public MovInventarioDAO() {
        this.conexion = ConnBD.getConnection();
    }

    // LISTAR TODOS LOS MOVIMIENTOS
    public List<MovInventario> listar() {
        List<MovInventario> lista = new ArrayList<>();
        // Usamos fechaRegistro si existe; es el comportamiento del proyecto "prueba2 - copia"
        String sql = "SELECT * FROM movimientos_inventario ORDER BY fechaRegistro DESC";

        try (PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                MovInventario mov = mapearMovimiento(rs);
                lista.add(mov);
            }
        } catch (SQLException e) {
            System.err.println("Error al listar movimientos: " + e.getMessage());
        }
        return lista;
    }

    // REGISTRAR MOVIMIENTO (con archivoFactura y usuarioRegistro)
    public boolean registrarMovimiento(MovInventario mov) {
        String sql = "INSERT INTO movimientos_inventario ("
                + "tipoString, cantidadEntrada, cantidadSalida, "
                + "fechaEntrada, fechaSalida, tipoEntrada, tipoSalida, descripcion, "
                + "proveedor, cliente, precioCompra, precioVenta, numeroFactura, "
                + "idProducto, stockAnterior, stockNuevo, archivoFactura, usuarioRegistro"
                + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, mov.getTipoString());
            ps.setObject(2, mov.getCantidadEntrada(), Types.INTEGER);
            ps.setObject(3, mov.getCantidadSalida(), Types.INTEGER);

            ps.setDate(4, mov.getFechaEntrada() != null
                    ? new java.sql.Date(mov.getFechaEntrada().getTime())
                    : null);
            ps.setDate(5, mov.getFechaSalida() != null
                    ? new java.sql.Date(mov.getFechaSalida().getTime())
                    : null);

            ps.setString(6, mov.getTipoEntrada());
            ps.setString(7, mov.getTipoSalida());
            ps.setString(8, mov.getDescripcion());
            ps.setString(9, mov.getProveedor());
            ps.setString(10, mov.getCliente());

            ps.setObject(11, mov.getPrecioCompra(), Types.DOUBLE);
            ps.setObject(12, mov.getPrecioVenta(), Types.DOUBLE);
            ps.setString(13, mov.getNumeroFactura());

            ps.setObject(14, mov.getIdProducto(), Types.INTEGER);
            ps.setObject(15, mov.getStockAnterior(), Types.INTEGER);
            ps.setObject(16, mov.getStockNuevo(), Types.INTEGER);

            ps.setString(17, mov.getArchivoFactura());
            ps.setString(18, mov.getUsuarioRegistro());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al registrar movimiento: " + e.getMessage());
            return false;
        }
    }

    // ELIMINAR MOVIMIENTO
    public boolean eliminarMovimiento(int idMovInventario) {
        String sql = "DELETE FROM movimientos_inventario WHERE idMovInventario = ?";

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, idMovInventario);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al eliminar movimiento: " + e.getMessage());
            return false;
        }
    }

    // FILTRAR POR RANGO DE FECHAS
    public List<MovInventario> filtrarPorFechas(Date fechaInicio, Date fechaFin) {
        List<MovInventario> lista = new ArrayList<>();

        String sql = "SELECT * FROM movimientos_inventario "
                + "WHERE (fechaEntrada BETWEEN ? AND ? OR fechaSalida BETWEEN ? AND ?) "
                + "ORDER BY COALESCE(fechaEntrada, fechaSalida) DESC";

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setDate(1, new java.sql.Date(fechaInicio.getTime()));
            ps.setDate(2, new java.sql.Date(fechaFin.getTime()));
            ps.setDate(3, new java.sql.Date(fechaInicio.getTime()));
            ps.setDate(4, new java.sql.Date(fechaFin.getTime()));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MovInventario mov = mapearMovimiento(rs);
                    lista.add(mov);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al filtrar por fechas: " + e.getMessage());
        }
        return lista;
    }

    // TOTALES
    public int getTotalEntradas() {
        String sql = "SELECT COALESCE(SUM(cantidadEntrada), 0) "
                + "FROM movimientos_inventario WHERE tipoString = 'Entrada'";
        return contarSuma(sql);
    }

    public int getTotalSalidas() {
        String sql = "SELECT COALESCE(SUM(cantidadSalida), 0) "
                + "FROM movimientos_inventario WHERE tipoString = 'Salida'";
        return contarSuma(sql);
    }

    // SUMA GENÉRICA
    private int contarSuma(String sql) {
        try (PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error al sumar cantidades: " + e.getMessage());
        }
        return 0;
    }

    // FILTRAR POR TRIMESTRE
    public List<MovInventario> filtrarPorTrimestre(int año, int trimestre) {
        List<MovInventario> lista = new ArrayList<>();

        int mesInicio = (trimestre - 1) * 3 + 1; // Enero=1, Abril=4, etc.
        int mesFin = mesInicio + 2;

        String sql = "SELECT * FROM movimientos_inventario "
                + "WHERE (YEAR(fechaEntrada) = ? AND MONTH(fechaEntrada) BETWEEN ? AND ?) "
                + "OR (YEAR(fechaSalida) = ? AND MONTH(fechaSalida) BETWEEN ? AND ?) "
                + "ORDER BY COALESCE(fechaEntrada, fechaSalida) DESC";

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, año);
            ps.setInt(2, mesInicio);
            ps.setInt(3, mesFin);
            ps.setInt(4, año);
            ps.setInt(5, mesInicio);
            ps.setInt(6, mesFin);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MovInventario mov = mapearMovimiento(rs);
                    lista.add(mov);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al filtrar por trimestre: " + e.getMessage());
        }
        return lista;
    }

    // MAPEO COMÚN DE RESULTSET -> OBJETO
    private MovInventario mapearMovimiento(ResultSet rs) throws SQLException {
        MovInventario mov = new MovInventario();
        mov.setIdMovInventario(rs.getInt("idMovInventario"));
        mov.setTipoString(rs.getString("tipoString"));
        mov.setCantidadEntrada(rs.getInt("cantidadEntrada"));
        mov.setCantidadSalida(rs.getInt("cantidadSalida"));
        mov.setFechaEntrada(rs.getDate("fechaEntrada"));
        mov.setFechaSalida(rs.getDate("fechaSalida"));
        mov.setTipoEntrada(rs.getString("tipoEntrada"));
        mov.setTipoSalida(rs.getString("tipoSalida"));
        mov.setDescripcion(rs.getString("descripcion"));
        mov.setProveedor(rs.getString("proveedor"));
        mov.setCliente(rs.getString("cliente"));
        mov.setPrecioCompra(rs.getDouble("precioCompra"));
        mov.setPrecioVenta(rs.getDouble("precioVenta"));
        mov.setNumeroFactura(rs.getString("numeroFactura"));
        mov.setIdProducto(rs.getInt("idProducto"));
        mov.setStockAnterior(rs.getInt("stockAnterior"));
        mov.setStockNuevo(rs.getInt("stockNuevo"));

        // Campos adicionales (pueden no existir si la BD aún no fue actualizada)
        try {
            mov.setArchivoFactura(rs.getString("archivoFactura"));
        } catch (SQLException e) {
            // columna aún no creada, ignoramos
        }

        try {
            mov.setUsuarioRegistro(rs.getString("usuarioRegistro"));
        } catch (SQLException e) {
            // columna aún no creada, ignoramos
        }

        try {
            mov.setImagenProducto(rs.getString("imagenProducto"));
        } catch (SQLException e) {
            // por si no existe la columna
        }

        try {
            mov.setFechaRegistro(rs.getTimestamp("fechaRegistro"));
        } catch (SQLException e) {
            // por si la columna es null o no existe
        }

        return mov;
    }
}
