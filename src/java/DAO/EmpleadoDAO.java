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
import java.util.Date; 
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EmpleadoDAO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private transient PreparedStatement ps;
    private transient ResultSet rs;

    public List<Usuario> listar() throws SQLException {
        List<Usuario> listaUsuarios = new ArrayList<>();
        try {
            String sql = "SELECT * FROM usuario";

            ps = Conexion.conectar().prepareStatement(sql);

            rs = ps.executeQuery();

            while (rs.next()) {
                if("Empleado".equalsIgnoreCase(rs.getString("rol"))){
                Usuario e = new Usuario();
                e.setId(rs.getInt("id"));
                e.setRol(EnumRoles.EMPLEADO);
                e.setNombre(rs.getString("nombre"));
                e.setCorreo(rs.getString("correo"));
                e.setCelular(rs.getString("celular"));
                e.setFecha_actualizacion(rs.getTimestamp("fecha_actualizacion").toLocalDateTime());
                e.setFecha_creacion(rs.getTimestamp("fecha_creacion").toLocalDateTime());
                e.setDireccion(rs.getString("direccion"));
                e.setPassword(rs.getString("password"));

                listaUsuarios.add(e);
                }
            }

        } catch (SQLException e) {

        }
        return listaUsuarios;
    }
    public void agregar(Usuario u) throws SQLException {
        String sql = "INSERT INTO usuario (rol, nombre, correo, celular, fecha_actualizacion, fecha_creacion, direccion, password) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = Conexion.conectar(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, u.getRol().name().toLowerCase());
            ps.setString(2, u.getNombre());
            ps.setString(3, u.getCorreo());
            ps.setString(4, u.getCelular());
            ps.setTimestamp(5, Timestamp.valueOf(u.getFecha_actualizacion())); // 🔹 posición correcta
            ps.setTimestamp(6, Timestamp.valueOf(u.getFecha_creacion()));     // 🔹 posición correcta
            ps.setString(7, u.getDireccion());
            ps.setString(8, u.getPassword());

            ps.executeUpdate();

            System.out.println("Empleado agregado con éxito");
            
        } catch (SQLException e) {
            System.out.println("Error al registrar empleado: " + e.getMessage());
            throw e;
        }
    }
    public void eliminar ( Usuario u) throws SQLException{
        try{
        String sql ="DELETE FROM usuario WHERE id =?";
        ps = Conexion.conectar().prepareStatement(sql);
        ps.setInt(1, u.getId());

        
        
        ps.executeUpdate(); 
        }catch(SQLException e){
            System.out.println("Error al eliminar producto" + e.getMessage());
        }        
    }

}
