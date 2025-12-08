package Controlador;

import Modelo.PedidoProveedor;
import Modelo.Proveedor;
import Modelo.Producto;
import DAO.PedidoProveedorDAO;
import DAO.ProveedorDAO;
import DAO.ProductoDAO;
import java.io.Serializable;
import java.util.Date; 
import java.util.List;
import java.util.ArrayList;
import java.sql.SQLException;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

@ManagedBean(name = "pedidoProveedorBean")
@ViewScoped
public class PedidoProveedorBean implements Serializable {
    private static final long serialVersionUID =1L;

    private transient PedidoProveedorDAO pedidoDAO = new PedidoProveedorDAO();
    private transient ProveedorDAO proveedorDAO = new ProveedorDAO();
    private transient ProductoDAO productoDAO = new ProductoDAO();
    //private EmailService emailService = new EmailService(); 

    private List<PedidoProveedor> listaPedidos = new ArrayList<>();
    private List<Proveedor> listaProveedores = new ArrayList<>();
    private List<Producto> listaProductosPorProveedor = new ArrayList<>();
    private PedidoProveedor nuevoPedido = new PedidoProveedor();

    private int totalPedidos;
    private int totalPendientes;
    private int totalEnProceso;
    private int totalCompletados;
    private int totalCancelados;

    // Getters lazy para DAOs
    private PedidoProveedorDAO getPedidoDAO() {
        if (pedidoDAO == null) {
            pedidoDAO = new PedidoProveedorDAO();
        }
        return pedidoDAO;
    }

    private ProveedorDAO getProveedorDAO() {
        if (proveedorDAO == null) {
            proveedorDAO = new ProveedorDAO();
        }
        return proveedorDAO;
    }

    private ProductoDAO getProductoDAO() {
        if (productoDAO == null) {
            productoDAO = new ProductoDAO();
        }
        return productoDAO;
    }
   
    @PostConstruct
    public void init() {
        cargarProveedores();
        cargarPedidos();
    }

    public void cargarProveedores() {
        try {
            this.listaProveedores = getProveedorDAO().listarActivos();
        } catch (SQLException e) {
            System.err.println("Error al cargar proveedores: " + e.getMessage());
        }
    }

    public void cargarPedidos() {
        try {
            this.listaPedidos = getPedidoDAO().listar();
            calcularEstadisticas();
        } catch (SQLException e) {
            System.err.println("Error al cargar pedidos: " + e.getMessage());
        }
    }

    private void calcularEstadisticas() {
        this.totalPedidos = listaPedidos.size();
        this.totalPendientes = (int) listaPedidos.stream()
                .filter(p -> {
                    String estado = p.getEstado();
                    return "PENDIENTE".equalsIgnoreCase(estado) || "ESPERA".equalsIgnoreCase(estado);
                })
                .count();
        this.totalEnProceso = (int) listaPedidos.stream()
                .filter(p -> "EN_PROCESO".equalsIgnoreCase(p.getEstado()) || "ACEPTADO".equalsIgnoreCase(p.getEstado()))
                .count();
        this.totalCompletados = (int) listaPedidos.stream()
                .filter(p -> "COMPLETADO".equalsIgnoreCase(p.getEstado()))
                .count();
        this.totalCancelados = (int) listaPedidos.stream()
                .filter(p -> "CANCELADO".equalsIgnoreCase(p.getEstado()) || "RECHAZADO".equalsIgnoreCase(p.getEstado()))
                .count();
    }

//    public void cargarProductosPorProveedor() {
//        FacesContext context = FacesContext.getCurrentInstance();
//        int idProveedorSeleccionado = nuevoPedido.getIdProveedor();
//
//        this.listaProductosPorProveedor.clear();
//        this.nuevoPedido.setIdProducto(0); 
//
//        if (idProveedorSeleccionado > 0) {
//            try {
//                // Requiere que ProductoDAO tenga un método listarPorProveedor(int idProveedor)
//                this.listaProductosPorProveedor = productoDAO.listarPorProveedor(idProveedorSeleccionado);
//            } catch (Exception e) {
//                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error BD",
//                        "Fallo al cargar productos: " + e.getMessage()));
//                e.printStackTrace();
//            }
//        }
//    }
     public void cargarProductosPorProveedor() {
        FacesContext context = FacesContext.getCurrentInstance();
        int idProveedorSeleccionado = nuevoPedido.getIdProveedor();
        
        this.listaProductosPorProveedor.clear();
        this.nuevoPedido.setIdProducto(0);

        if (idProveedorSeleccionado > 0) {
            try {
                // MÉTODO TEMPORAL - DEBES IMPLEMENTAR ProductoDAO.listarPorProveedor()
                this.listaProductosPorProveedor = getProductoDAO().listarPorProveedor(idProveedorSeleccionado);
            } catch (Exception e) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error",
                    "No se pudieron cargar los productos: " + e.getMessage()));
            }
        }
    }

    public String registrarNuevoPedido() {
    FacesContext context = FacesContext.getCurrentInstance();
    
    if (nuevoPedido.getIdProducto() <= 0 || nuevoPedido.getIdProveedor() <= 0) {
        context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Validación", "Debe seleccionar Proveedor y Producto."));
        return null;
    }
    if (nuevoPedido.getCantidad() <= 0) {
        context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Validación", "La Cantidad debe ser mayor a cero."));
        return null;
    }

    try {
        Proveedor proveedorSeleccionado = getProveedorDAO().obtenerPorId(nuevoPedido.getIdProveedor());
        Producto productoSeleccionado = getProductoDAO().buscar(nuevoPedido.getIdProducto());

        if (proveedorSeleccionado == null || productoSeleccionado == null) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Proveedor o Producto no encontrado."));
            return null;
        }

        nuevoPedido.setNombreProveedor(proveedorSeleccionado.getNombreProveedor());
        nuevoPedido.setNombreProducto(productoSeleccionado.getNombreProducto());

        if (nuevoPedido.getDescripcionPedido() == null || nuevoPedido.getDescripcionPedido().trim().isEmpty()) {
            String descripcionGenerada = "Pedido de " + nuevoPedido.getCantidad() + " de " 
                    + nuevoPedido.getNombreProducto() + " a " + nuevoPedido.getNombreProveedor();
            nuevoPedido.setDescripcionPedido(descripcionGenerada);
        }

        nuevoPedido.setEstado("PENDIENTE");

        if (getPedidoDAO().registrar(nuevoPedido)) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Éxito",
                    "Pedido de " + nuevoPedido.getNombreProducto() + " creado."));

            this.nuevoPedido = new PedidoProveedor();
            this.listaProductosPorProveedor.clear();
            cargarPedidos();

            // 👇 Igual que haces con HomeAdmin3
            return "PedidosProveedores?faces-redirect=true";
        } else {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fallo", "No se pudo registrar el pedido (Error DAO)."));
            return null;
        }

    } catch (Exception e) {
        context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_FATAL, "Error Grave", "Error al registrar el pedido: " + e.getMessage()));
        e.printStackTrace();
        return null;
    }
}


    public void marcarPendiente(PedidoProveedor pedido) {
        cambiarEstado(pedido, "PENDIENTE", false);
    }

    public void marcarEnProceso(PedidoProveedor pedido) {
        cambiarEstado(pedido, "EN_PROCESO", false);
    }

    public void marcarCompletado(PedidoProveedor pedido) {
        cambiarEstado(pedido, "COMPLETADO", true);
    }

    public void marcarCancelado(PedidoProveedor pedido) {
        cambiarEstado(pedido, "CANCELADO", false);
    }

    private void cambiarEstado(PedidoProveedor pedido, String nuevoEstado, boolean actualizarInventario) {
        FacesContext context = FacesContext.getCurrentInstance();
        try {
            pedido.setEstado(nuevoEstado);
            pedido.setFechaActualizacion(new Date());

            if (getPedidoDAO().actualizarEstado(pedido)) {
                if (actualizarInventario) {
                    try {
                        Producto producto = getProductoDAO().buscar(pedido.getIdProducto());
                        if (producto != null) {
                            int nuevoStock = producto.getStock() + pedido.getCantidad();
                            getProductoDAO().actualizarStock(producto.getIdProducto(), nuevoStock);
                        }
                    } catch (SQLException ex) {
                        context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Inventario",
                                "Estado cambiado, pero no se pudo actualizar el inventario: " + ex.getMessage()));
                    }
                }

                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Éxito",
                        "Pedido ID " + pedido.getIdPedido() + " ahora está " + nuevoEstado + "."));
            } else {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error BD",
                        "No se pudo actualizar el estado del pedido."));
            }
            cargarPedidos();
        } catch (Exception e) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_FATAL, "Error Grave",
                    "Fallo al actualizar el pedido: " + e.getMessage()));
        }
    }

   
    public int getTotalPedidos() {
        return totalPedidos;
    }

    public void setTotalPedidos(int totalPedidos) {
        this.totalPedidos = totalPedidos;
    }

    public int getTotalPendientes() {
        return totalPendientes;
    }

    public void setTotalPendientes(int totalPendientes) {
        this.totalPendientes = totalPendientes;
    }

    public int getTotalEnProceso() {
        return totalEnProceso;
    }

    public void setTotalEnProceso(int totalEnProceso) {
        this.totalEnProceso = totalEnProceso;
    }

    public int getTotalCompletados() {
        return totalCompletados;
    }

    public void setTotalCompletados(int totalCompletados) {
        this.totalCompletados = totalCompletados;
    }

    public int getTotalCancelados() {
        return totalCancelados;
    }

    public void setTotalCancelados(int totalCancelados) {
        this.totalCancelados = totalCancelados;
    }

    public List<PedidoProveedor> getListaPedidos() {
        return listaPedidos;
    }

    public void setListaPedidos(List<PedidoProveedor> listaPedidos) {
        this.listaPedidos = listaPedidos;
    }

    public List<Proveedor> getListaProveedores() {
        return listaProveedores;
    }

    public void setListaProveedores(List<Proveedor> listaProveedores) {
        this.listaProveedores = listaProveedores;
    }

    public PedidoProveedor getNuevoPedido() {
        return nuevoPedido;
    }

    public void setNuevoPedido(PedidoProveedor nuevoPedido) {
        this.nuevoPedido = nuevoPedido;
    }

    public List<Producto> getListaProductosPorProveedor() {
        return listaProductosPorProveedor;
    }

    public void setListaProductosPorProveedor(List<Producto> listaProductosPorProveedor) {
        this.listaProductosPorProveedor = listaProductosPorProveedor;
    }
}
