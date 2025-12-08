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

        try (Connection con = Conexion.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

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

        try (Connection con = Conexion.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

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

        try (Connection con = Conexion.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

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

        try (Connection con = Conexion.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

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

        try (Connection con = Conexion.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

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
        // IMPORTANTE: ahora también actualiza password
        String sql = "UPDATE usuario SET nombre=?, correo=?, celular=?, direccion=?, biografia=?, fotoPerfil=?, password=? WHERE id=?";

        try (Connection con = Conexion.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, u.getNombre());
            ps.setString(2, u.getCorreo());
            ps.setString(3, u.getCelular());
            ps.setString(4, u.getDireccion());
            ps.setString(5, u.getBiografia());
            ps.setString(6, u.getFotoPerfil());
            ps.setString(7, u.getPassword());
            ps.setInt(8, u.getId());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("Error al actualizar perfil: " + e.getMessage());
            return false;
        }
    }
}
