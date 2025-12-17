package DAO;

import Modelo.Usuario;
import Controlador.Conexion;
import Modelo.EnumRoles;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDAO implements Serializable {

    private static final long serialVersionUID = 1L;

    public List<Usuario> listar() throws SQLException {
        List<Usuario> listaUsuarios = new ArrayList<>();
        String sql = "SELECT * FROM usuario";

        try (Connection con = Conexion.conectar(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Usuario u = new Usuario();
                u.setId(rs.getInt("id"));
                u.setRol(EnumRoles.valueOf(rs.getString("rol").trim().toUpperCase()));
                u.setNombre(rs.getString("nombre"));
                u.setCorreo(rs.getString("correo"));
                u.setCelular(rs.getString("celular"));

                Timestamp tsActualizacion = rs.getTimestamp("fecha_actualizacion");
                if (tsActualizacion != null) {
                    u.setFecha_actualizacion(tsActualizacion.toLocalDateTime());
                }

                Timestamp tsCreacion = rs.getTimestamp("fecha_creacion");
                if (tsCreacion != null) {
                    u.setFecha_creacion(tsCreacion.toLocalDateTime());
                }

                u.setDireccion(rs.getString("direccion"));
                u.setPassword(rs.getString("password"));
                u.setEstado(rs.getString("estado"));
                u.setFotoPerfil(rs.getString("fotoPerfil"));
                u.setBiografia(rs.getString("biografia"));

                listaUsuarios.add(u);
            }

        } catch (SQLException e) {
            System.out.println("Error al listar usuarios: " + e.getMessage());
        }
        return listaUsuarios;
    }

    public void agregar(Usuario u) throws SQLException {
        String sql = "INSERT INTO usuario (rol, nombre, correo, celular, fecha_actualizacion, fecha_creacion, direccion, password, estado, fotoPerfil, biografia) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = Conexion.conectar(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, u.getRol().name().toLowerCase());
            ps.setString(2, u.getNombre());
            ps.setString(3, u.getCorreo());
            ps.setString(4, u.getCelular());
            ps.setTimestamp(5, Timestamp.valueOf(u.getFecha_actualizacion()));
            ps.setTimestamp(6, Timestamp.valueOf(u.getFecha_creacion()));
            ps.setString(7, u.getDireccion());
            ps.setString(8, u.getPassword());
            ps.setString(9, u.getEstado() != null ? u.getEstado() : "ACTIVO");
            ps.setString(10, u.getFotoPerfil());
            ps.setString(11, u.getBiografia());

            ps.executeUpdate();
            System.out.println("Usuario agregado con éxito");

        } catch (SQLException e) {
            System.out.println("Error al registrar usuario: " + e.getMessage());
            throw e;
        }
    }

    public void actualizar(Usuario u) {
        String sql = "UPDATE usuario SET rol=?, nombre=?, correo=?, celular=?, direccion=?, estado=? WHERE id=?";

        try (Connection con = Conexion.conectar(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, u.getRol().name().toLowerCase());
            ps.setString(2, u.getNombre());
            ps.setString(3, u.getCorreo());
            ps.setString(4, u.getCelular());
            ps.setString(5, u.getDireccion());
            ps.setString(6, u.getEstado());
            ps.setInt(7, u.getId());

            ps.executeUpdate();

        } catch (SQLException e) {
            System.out.println("Error al actualizar usuario: " + e.getMessage());
        }
    }

    public boolean actualizarEstado(int idUsuario, String estado) {
        String sql = "UPDATE usuario SET estado=? WHERE id=?";

        try (Connection con = Conexion.conectar(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, estado);
            ps.setInt(2, idUsuario);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("Error al actualizar estado de usuario: " + e.getMessage());
            return false;
        }
    }

    public Usuario obtenerPorId(int idUsuario) {
        String sql = "SELECT * FROM usuario WHERE id = ?";

        try (Connection con = Conexion.conectar(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idUsuario);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Usuario u = new Usuario();
                    u.setId(rs.getInt("id"));
                    u.setRol(EnumRoles.valueOf(rs.getString("rol").trim().toUpperCase()));
                    u.setNombre(rs.getString("nombre"));
                    u.setCorreo(rs.getString("correo"));
                    u.setCelular(rs.getString("celular"));
                    u.setDireccion(rs.getString("direccion"));
                    u.setPassword(rs.getString("password"));
                    u.setEstado(rs.getString("estado"));
                    u.setFotoPerfil(rs.getString("fotoPerfil"));
                    u.setBiografia(rs.getString("biografia"));

                    Timestamp tsActualizacion = rs.getTimestamp("fecha_actualizacion");
                    if (tsActualizacion != null) {
                        u.setFecha_actualizacion(tsActualizacion.toLocalDateTime());
                    }

                    Timestamp tsCreacion = rs.getTimestamp("fecha_creacion");
                    if (tsCreacion != null) {
                        u.setFecha_creacion(tsCreacion.toLocalDateTime());
                    }

                    return u;
                }
            }

        } catch (SQLException e) {
            System.out.println("Error al obtener usuario: " + e.getMessage());
        }
        return null;
    }

    public boolean actualizarPerfil(Usuario u) {
    // Mantengo la firma original por compatibilidad
    // (pero desde UsuarioBean vamos a llamar al método con boolean)
    return actualizarPerfil(u, true);
}

public boolean actualizarPerfil(Usuario u, boolean actualizarPassword) {

    String sql = actualizarPassword
            ? "UPDATE usuario SET nombre=?, correo=?, celular=?, direccion=?, biografia=?, fotoPerfil=?, password=?, fecha_actualizacion = NOW() WHERE id=?"
            : "UPDATE usuario SET nombre=?, correo=?, celular=?, direccion=?, biografia=?, fotoPerfil=?, fecha_actualizacion = NOW() WHERE id=?";

    Connection con = null;
    try {
        con = Conexion.conectar();
        if (con == null) return false;

        int filas;

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, u.getNombre());
            ps.setString(i++, u.getCorreo());
            ps.setString(i++, u.getCelular());
            ps.setString(i++, u.getDireccion());
            ps.setString(i++, u.getBiografia());
            ps.setString(i++, u.getFotoPerfil());

            if (actualizarPassword) {
                ps.setString(i++, u.getPassword());
            }

            ps.setInt(i++, u.getId());

            filas = ps.executeUpdate();
        }

        // Si filas == 0 puede ser “sin cambios”, lo tratamos como OK si el usuario existe
        if (filas > 0) return true;

        String check = "SELECT 1 FROM usuario WHERE id = ?";
        try (PreparedStatement psCheck = con.prepareStatement(check)) {
            psCheck.setInt(1, u.getId());
            try (ResultSet rs = psCheck.executeQuery()) {
                return rs.next();
            }
        }

    } catch (SQLException e) {
        System.out.println("Error al actualizar perfil: " + e.getMessage());
        return false;
    } finally {
        if (con != null) {
            try { con.close(); } catch (SQLException ignored) {}
        }
    }
}


}
