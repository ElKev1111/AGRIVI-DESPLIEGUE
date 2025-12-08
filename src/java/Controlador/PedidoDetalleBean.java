package Controlador;

import DAO.PedidoProveedorDAO;
import Modelo.PedidoProveedor;
import java.io.Serializable;
import java.sql.SQLException;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

@ManagedBean(name = "pedidoDetalleBean")
@ViewScoped
public class PedidoDetalleBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private int pedidoId;
    private PedidoProveedor pedido;
    private transient PedidoProveedorDAO pedidoDAO = new PedidoProveedorDAO();

    public void cargarDetallePedido() {
        if (pedidoId <= 0) {
            pedido = null;
            return;
        }

        try {
            pedido = getPedidoDAO().obtenerPorId(pedidoId);

            if (pedido == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN, "Aviso", "Pedido no encontrado"));
            }
        } catch (SQLException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "No se pudo cargar el pedido: " + e.getMessage()));
            pedido = null;
        }
    }

    private PedidoProveedorDAO getPedidoDAO() {
        if (pedidoDAO == null) {
            pedidoDAO = new PedidoProveedorDAO();
        }
        return pedidoDAO;
    }

    // Getters y Setters
    public int getPedidoId() {
        return pedidoId;
    }

    public void setPedidoId(int pedidoId) {
        this.pedidoId = pedidoId;
    }

    public PedidoProveedor getPedido() {
        return pedido;
    }

    public void setPedido(PedidoProveedor pedido) {
        this.pedido = pedido;
    }
}
