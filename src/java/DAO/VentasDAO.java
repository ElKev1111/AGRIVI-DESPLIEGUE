package DAO;

import Controlador.Conexion;
import Modelo.Producto;
import Modelo.Usuario;
import Modelo.Ventas;
import Modelo.VentaAgrupadaDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * DAO de Ventas.
 *
 * Este archivo concentra TODO: 1) Listados legacy (para no romper vistas
 * antiguas). 2) Registro de ventas. 3) Historial del cliente. 4) NUEVO:
 * agrupado admin por codigoCompra. 5) NUEVO: detalle de compra por codigoGrupo
 * (codigoCompra o LEG-...).
 */
public class VentasDAO {

    // ========================= LEGACY =========================
    /**
     * Lista todas las ventas como líneas individuales. Mantener este método
     * evita romper pantallas viejas.
     */
    public List<Ventas> listar() {
        List<Ventas> listaVentas = new ArrayList<>();

        String sql = "SELECT idVenta, fechaVenta, idProducto, idUsuario, cantidad, totalPagar, codigoCompra "
                + "FROM ventas ORDER BY fechaVenta DESC";

        try (Connection con = Conexion.conectar(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Ventas venta = new Ventas();
                Producto producto = new Producto();
                Usuario usuario = new Usuario();

                venta.setIdVenta(rs.getInt("idVenta"));

                Timestamp ts = rs.getTimestamp("fechaVenta");
                if (ts != null) {
                    venta.setFechaVenta(ts.toLocalDateTime());
                }

                producto.setIdProducto(rs.getInt("idProducto"));
                venta.setProducto(producto);

                usuario.setId(rs.getInt("idUsuario"));
                venta.setUsuario(usuario);

                venta.setCantidad(rs.getInt("cantidad"));
                venta.setTotalPagar(rs.getFloat("totalPagar"));
                venta.setCodigoCompra(rs.getString("codigoCompra"));

                listaVentas.add(venta);
            }

        } catch (SQLException e) {
            System.out.println("Error al listar ventas: " + e.getMessage());
        }

        return listaVentas;
    }

    /**
     * Registra una línea de venta.
     *
     * Nuevo soporte: inserta codigoCompra. Este campo viene desde CarritoBean,
     * donde se genera 1 por checkout. :contentReference[oaicite:5]{index=5}
     */
    public boolean registrar(Ventas venta) {
        String sql = "INSERT INTO ventas (fechaVenta, idProducto, idUsuario, cantidad, totalPagar, codigoCompra) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection con = Conexion.conectar(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setTimestamp(1, new Timestamp(new Date().getTime()));
            ps.setInt(2, venta.getProducto().getIdProducto());
            ps.setInt(3, venta.getUsuario().getId());
            ps.setInt(4, venta.getCantidad());
            ps.setFloat(5, venta.getTotalPagar());
            ps.setString(6, venta.getCodigoCompra());

            System.out.println("[VENTA-DAO] Insert preparado -> " + venta);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("Error al registrar venta: " + e.getMessage());
            return false;
        }
    }

    // ========================= HISTORIAL CLIENTE =========================
    /**
     * Historial del cliente (líneas individuales). Usa precioProducto, que es
     * el nombre correcto del modelo/tabla.
     */
    public List<Ventas> listarPorUsuario(int idUsuario) {
        List<Ventas> listaVentas = new ArrayList<>();

        String sql = "SELECT v.idVenta, v.fechaVenta, v.idProducto, v.idUsuario, v.cantidad, v.totalPagar, "
                + "       p.nombreProducto, p.precioProducto "
                + "FROM ventas v "
                + "JOIN producto p ON p.idProducto = v.idProducto "
                + "WHERE v.idUsuario = ? "
                + "ORDER BY v.fechaVenta DESC";

        try (Connection con = Conexion.conectar(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idUsuario);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Ventas venta = new Ventas();
                    Producto producto = new Producto();
                    Usuario usuario = new Usuario();

                    venta.setIdVenta(rs.getInt("idVenta"));

                    Timestamp ts = rs.getTimestamp("fechaVenta");
                    if (ts != null) {
                        venta.setFechaVenta(ts.toLocalDateTime());
                    }

                    producto.setIdProducto(rs.getInt("idProducto"));
                    producto.setNombreProducto(rs.getString("nombreProducto"));
                    producto.setPrecioProducto(rs.getFloat("precioProducto"));
                    venta.setProducto(producto);

                    usuario.setId(rs.getInt("idUsuario"));
                    venta.setUsuario(usuario);

                    venta.setCantidad(rs.getInt("cantidad"));
                    venta.setTotalPagar(rs.getFloat("totalPagar"));

                    listaVentas.add(venta);
                }
            }

        } catch (SQLException e) {
            System.out.println("Error al listar ventas del usuario: " + e.getMessage());
        }

        return listaVentas;
    }

    // ========================= NUEVO: AGRUPADO ADMIN =========================
    /**
     * Lista compras agrupadas.
     *
     * - Si codigoCompra existe, agrupa por él. - Si no existe (ventas
     * antiguas), crea un codigoGrupo falso "LEG-idVenta".
     *
     * Esto te permite mostrar TODO en una sola tabla sin perder historial.
     */
    public List<VentaAgrupadaDTO> listarAgrupadoAdmin() {
        List<VentaAgrupadaDTO> lista = new ArrayList<>();

        String sql = "SELECT "
                + "COALESCE(v.codigoCompra, CONCAT('LEG-', v.idVenta)) AS codigoGrupo, "
                + "MIN(v.fechaVenta) AS fechaVenta, "
                + "v.idUsuario, "
                + "u.nombre AS nombreUsuario, "
                + "u.correo AS correoUsuario, "
                + "SUM(v.totalPagar) AS totalPagar, "
                + "SUM(v.cantidad) AS totalItems, "
                + "COUNT(*) AS totalLineas "
                + "FROM ventas v "
                + "JOIN usuario u ON u.id = v.idUsuario "
                + "GROUP BY codigoGrupo, v.idUsuario, u.nombre, u.correo "
                + "ORDER BY fechaVenta DESC";

        try (Connection con = Conexion.conectar(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                VentaAgrupadaDTO dto = new VentaAgrupadaDTO();
                dto.setCodigoGrupo(rs.getString("codigoGrupo"));

                Timestamp ts = rs.getTimestamp("fechaVenta");
                if (ts != null) {
                    dto.setFechaVenta(ts.toLocalDateTime());
                }

                dto.setIdUsuario(rs.getInt("idUsuario"));
                dto.setNombreUsuario(rs.getString("nombreUsuario"));
                dto.setCorreoUsuario(rs.getString("correoUsuario"));

                dto.setTotalPagar(rs.getFloat("totalPagar"));
                dto.setTotalItems(rs.getInt("totalItems"));
                dto.setTotalLineas(rs.getInt("totalLineas"));

                lista.add(dto);
            }

            System.out.println("[VENTA-DAO] Admin agrupado cargado -> filas=" + lista.size());

        } catch (Exception e) {
            System.out.println("[VENTA-DAO] ERROR listarAgrupadoAdmin: " + e.getMessage());
        }

        return lista;
    }

    /**
     * Detalles de compra por codigoCompra real.
     *
     * CAMBIO CLAVE DEL PROCESO: - antes estaba usando p.precioVenta
     * (incorrecto). - ahora debe ser p.precioProducto.
     * :contentReference[oaicite:6]{index=6}
     */
    public List<Ventas> listarDetallesPorCodigo(String codigoCompra) {
        List<Ventas> detalles = new ArrayList<>();

        String sql = "SELECT v.idVenta, v.fechaVenta, v.idProducto, v.idUsuario, "
                + "       v.cantidad, v.totalPagar, v.codigoCompra, "
                + "       p.nombreProducto, p.precioProducto, p.imagen "
                + "FROM ventas v "
                + "JOIN producto p ON p.idProducto = v.idProducto "
                + "WHERE v.codigoCompra = ? "
                + "ORDER BY p.nombreProducto";

        try (Connection con = Conexion.conectar(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, codigoCompra);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Ventas ven = new Ventas();
                    Producto p = new Producto();
                    Usuario u = new Usuario();

                    ven.setIdVenta(rs.getInt("idVenta"));

                    Timestamp ts = rs.getTimestamp("fechaVenta");
                    if (ts != null) {
                        ven.setFechaVenta(ts.toLocalDateTime());
                    }

                    p.setIdProducto(rs.getInt("idProducto"));
                    p.setNombreProducto(rs.getString("nombreProducto"));
                    p.setPrecioProducto(rs.getFloat("precioProducto"));
                    p.setImagen(rs.getString("imagen"));
                    ven.setProducto(p);

                    u.setId(rs.getInt("idUsuario"));
                    ven.setUsuario(u);

                    ven.setCantidad(rs.getInt("cantidad"));
                    ven.setTotalPagar(rs.getFloat("totalPagar"));
                    ven.setCodigoCompra(rs.getString("codigoCompra"));

                    detalles.add(ven);
                }
            }

            System.out.println("[VENTA-DAO] Detalles cargados codigoCompra=" + codigoCompra
                    + " -> lineas=" + detalles.size());

        } catch (Exception e) {
            System.out.println("[VENTA-DAO] ERROR listarDetallesPorCodigo: " + e.getMessage());
        }

        return detalles;
    }

    /**
     * Detalle legacy cuando la compra no tenía codigoCompra.
     */
    public List<Ventas> listarDetallesPorIdVenta(int idVenta) {
        List<Ventas> detalles = new ArrayList<>();

        String sql = "SELECT v.idVenta, v.fechaVenta, v.idProducto, v.idUsuario, v.cantidad, v.totalPagar, v.codigoCompra, "
                + "       p.nombreProducto, p.precioProducto, p.imagen "
                + "FROM ventas v "
                + "JOIN producto p ON p.idProducto = v.idProducto "
                + "WHERE v.idVenta = ?";

        try (Connection con = Conexion.conectar(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idVenta);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Ventas ven = new Ventas();
                    Producto p = new Producto();
                    Usuario u = new Usuario();

                    ven.setIdVenta(rs.getInt("idVenta"));

                    Timestamp ts = rs.getTimestamp("fechaVenta");
                    if (ts != null) {
                        ven.setFechaVenta(ts.toLocalDateTime());
                    }

                    p.setIdProducto(rs.getInt("idProducto"));
                    p.setNombreProducto(rs.getString("nombreProducto"));
                    p.setPrecioProducto(rs.getFloat("precioProducto"));
                    p.setImagen(rs.getString("imagen"));
                    ven.setProducto(p);

                    u.setId(rs.getInt("idUsuario"));
                    ven.setUsuario(u);

                    ven.setCantidad(rs.getInt("cantidad"));
                    ven.setTotalPagar(rs.getFloat("totalPagar"));
                    ven.setCodigoCompra(rs.getString("codigoCompra"));

                    detalles.add(ven);
                }
            }

            System.out.println("[VENTA-DAO] Detalle legacy cargado idVenta=" + idVenta
                    + " -> lineas=" + detalles.size());

        } catch (Exception e) {
            System.out.println("[VENTA-DAO] ERROR listarDetallesPorIdVenta: " + e.getMessage());
        }

        return detalles;
    }

    /**
     * Devuelve una sola línea legacy por idVenta. Útil para fallback rápido
     * desde el Bean.
     */
    public Ventas buscarDetallePorIdVenta(int idVenta) {
        Ventas ven = null;

        String sql = "SELECT v.idVenta, v.fechaVenta, v.idProducto, v.idUsuario, v.cantidad, v.totalPagar, v.codigoCompra, "
                + "       p.nombreProducto, p.precioProducto, p.imagen "
                + "FROM ventas v "
                + "JOIN producto p ON p.idProducto = v.idProducto "
                + "WHERE v.idVenta = ?";

        try (Connection con = Conexion.conectar(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idVenta);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ven = new Ventas();
                    Producto p = new Producto();
                    Usuario u = new Usuario();

                    ven.setIdVenta(rs.getInt("idVenta"));

                    Timestamp ts = rs.getTimestamp("fechaVenta");
                    if (ts != null) {
                        ven.setFechaVenta(ts.toLocalDateTime());
                    }

                    p.setIdProducto(rs.getInt("idProducto"));
                    p.setNombreProducto(rs.getString("nombreProducto"));
                    p.setPrecioProducto(rs.getFloat("precioProducto"));
                    p.setImagen(rs.getString("imagen"));
                    ven.setProducto(p);

                    u.setId(rs.getInt("idUsuario"));
                    ven.setUsuario(u);

                    ven.setCantidad(rs.getInt("cantidad"));
                    ven.setTotalPagar(rs.getFloat("totalPagar"));
                    ven.setCodigoCompra(rs.getString("codigoCompra"));
                }
            }

        } catch (Exception e) {
            System.out.println("[VENTA-DAO] ERROR buscarDetallePorIdVenta: " + e.getMessage());
        }

        return ven;
    }
}
