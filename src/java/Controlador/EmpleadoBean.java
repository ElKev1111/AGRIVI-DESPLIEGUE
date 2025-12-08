/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Controlador;

import DAO.UsuarioDAO;
import DAO.EmpleadoDAO;
import Modelo.CifradoAES;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import Modelo.Usuario;
import Modelo.EnumRoles;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import javax.faces.bean.ApplicationScoped;

@ManagedBean
@ApplicationScoped
public class EmpleadoBean {
      private Usuario usuario = new Usuario();
    private EmpleadoDAO empleadoDAO = new EmpleadoDAO();

    // Getter & Setter
    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }
    
    public List<Usuario> getListaEmpleados() {
        try {
            return empleadoDAO.listar();

        } catch (SQLException e) {
            System.out.println("Erro al listar usuarios");
            return null;
        }
    }

    

//    public String editar(Usuario u) {
//        this.usuario = u;
//        return "editarUsuario?faces-redirect=true";
//        
//    }
    
    
public void agregar() throws IOException {
        try {
            usuario.setFecha_creacion(LocalDateTime.now());
            usuario.setFecha_actualizacion(LocalDateTime.now());

            String passEncriptada = CifradoAES.encriptar(usuario.getPassword());
            usuario.setPassword(passEncriptada);

            usuario.setRol(EnumRoles.EMPLEADO);
            empleadoDAO.agregar(usuario);

            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                            "Éxito", "Usuario registrado correctamente."));

            // Limpiar formulario
            usuario = new Usuario();
            FacesContext.getCurrentInstance().getExternalContext()
                    .redirect("HomeAdmin5.xhtml");

        } catch (SQLException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                            "Error", "Usuario no registrado ."));
        }
    }
public void eliminar(Usuario u) {
    try {
        empleadoDAO.eliminar(u);
        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_INFO, 
                    "Éxito", "Empleado eliminado correctamente"));
    } catch (SQLException e) {
        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Error", "Error al eliminar empleado: " + e.getMessage()));
    }
}

}
