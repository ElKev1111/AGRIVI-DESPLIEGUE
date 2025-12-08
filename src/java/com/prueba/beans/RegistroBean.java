package com.prueba.beans;


import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.context.FacesContext;

@ManagedBean(name = "registroBean")
public class RegistroBean implements Serializable {

    private String nombre;
    private String correo;
    private String celular;
    private String direccion;
    private String rol;
    private String password;

    // Configuración de conexión
    private final String URL = "jdbc:mysql://localhost:3306/agrivijsf3"; // cambia por tu base
    private final String USER = "root"; // tu usuario
    private final String PASS = "1234"; // tu contraseña

    public String registrar() {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            // Conectar
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(URL, USER, PASS);

            // Insert
            String sql = "INSERT INTO usuarios " +
                    "(id, rol, nombre, correo, celular, fecha_actualizacion, fecha_creacion, direccion, password) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

            stmt = conn.prepareStatement(sql);

            String idGenerado = UUID.randomUUID().toString();
            LocalDateTime ahora = LocalDateTime.now();

            stmt.setString(1, idGenerado);
            stmt.setString(2, rol);
            stmt.setString(3, nombre);
            stmt.setString(4, correo);
            stmt.setString(5, celular);
            stmt.setObject(6, ahora);
            stmt.setObject(7, ahora);
            stmt.setString(8, direccion);
            stmt.setString(9, password);

            int filas = stmt.executeUpdate();

            if (filas > 0) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO,
                                "Registro exitoso", "El usuario fue creado correctamente."));
                return "login?faces-redirect=true"; // redirige al login
            } else {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                "Error", "No se pudo registrar el usuario."));
            }

        } catch (Exception e) {
            e.printStackTrace();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Error en registro", e.getMessage()));
        } finally {
            try {
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    // Getters y Setters
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getCelular() { return celular; }
    public void setCelular(String celular) { this.celular = celular; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
