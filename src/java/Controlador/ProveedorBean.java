package Controlador;

import DAO.ProveedorDAO;
import Modelo.Proveedor;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import org.primefaces.PrimeFaces;

@ManagedBean
@ViewScoped
public class ProveedorBean implements Serializable {

    private List<Proveedor> listaProveedores;
    private Proveedor proveedorSeleccionado;
    private Proveedor nuevoProveedor;
    private ProveedorDAO proveedorDAO;

    public ProveedorBean() {
        proveedorDAO = new ProveedorDAO();
        nuevoProveedor = new Proveedor();
        cargarProveedores();
    }

    public void cargarProveedores() {
        try {
            listaProveedores = proveedorDAO.listar();
        } catch (SQLException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "No se pudieron cargar los proveedores: " + e.getMessage()));
        }
    }

    public void agregarProveedor() {
        try {
            if (nuevoProveedor.getEstado() == null || nuevoProveedor.getEstado().isEmpty()) {
                nuevoProveedor.setEstado("Activo");
            }
            if (proveedorDAO.agregar(nuevoProveedor)) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Éxito", "Proveedor agregado correctamente"));
                nuevoProveedor = new Proveedor();
                cargarProveedores();
                PrimeFaces.current().executeScript("PF('dialogoAgregar').hide();");
            }
        } catch (SQLException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "No se pudo agregar el proveedor: " + e.getMessage()));
        }
    }

    public void actualizarProveedor() {
        try {
            if (proveedorDAO.actualizar(proveedorSeleccionado)) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Éxito", "Proveedor actualizado correctamente"));
                cargarProveedores();
                PrimeFaces.current().executeScript("PF('dialogoEditar').hide();");
            }
        } catch (SQLException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "No se pudo actualizar el proveedor: " + e.getMessage()));
        }
    }

    public void eliminarProveedor() {
        try {
            if (proveedorDAO.eliminar(proveedorSeleccionado.getIdProveedor())) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Éxito", "Proveedor inactivado correctamente"));
                proveedorSeleccionado = null;
                cargarProveedores();
            }
        } catch (SQLException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "No se pudo eliminar el proveedor: " + e.getMessage()));
        }
    }

    public void alternarEstadoProveedor(Proveedor proveedor) {
        try {
            String nuevoEstado = "Activo".equalsIgnoreCase(proveedor.getEstado()) ? "Inactivo" : "Activo";
            if (proveedorDAO.cambiarEstado(proveedor.getIdProveedor(), nuevoEstado)) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO, "Éxito",
                                "Estado del proveedor actualizado a " + nuevoEstado));
                cargarProveedores();
            }
        } catch (SQLException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error",
                            "No se pudo cambiar el estado: " + e.getMessage()));
        }
    }

    // Métodos para las estadísticas del dashboard
    public int getTotalProveedores() {
        return listaProveedores != null ? listaProveedores.size() : 0;
    }

    public double getValorTotalInventario() {
        if (listaProveedores == null) return 0;
        double total = 0;
        for (Proveedor p : listaProveedores) {
            total += p.getPrecio();
        }
        return total;
    }

    public int getProveedoresActivos() {
        if (listaProveedores == null) return 0;
        int count = 0;
        for (Proveedor p : listaProveedores) {
            if ("Activo".equals(p.getEstado())) {
                count++;
            }
        }
        return count;
    }

    public int getProductosDiferentes() {
        if (listaProveedores == null) return 0;
        Set<String> productos = new HashSet<>();
        for (Proveedor p : listaProveedores) {
            if (p.getProducto() != null && !p.getProducto().isEmpty()) {
                productos.add(p.getProducto());
            }
        }
        return productos.size();
    }
    

    // Getters y Setters
    public List<Proveedor> getListaProveedores() {
        return listaProveedores;
    }

    public void setListaProveedores(List<Proveedor> listaProveedores) {
        this.listaProveedores = listaProveedores;
    }

    public Proveedor getProveedorSeleccionado() {
        return proveedorSeleccionado;
    }

    public void setProveedorSeleccionado(Proveedor proveedorSeleccionado) {
        this.proveedorSeleccionado = proveedorSeleccionado;
    }

    public Proveedor getNuevoProveedor() {
        return nuevoProveedor;
    }

    public void setNuevoProveedor(Proveedor nuevoProveedor) {
        this.nuevoProveedor = nuevoProveedor;
    }
}