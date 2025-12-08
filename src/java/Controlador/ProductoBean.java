package Controlador;

import DAO.ProductoDAO;
import DAO.ProveedorDAO;
import Modelo.Producto;
import Modelo.Proveedor;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import org.primefaces.model.file.UploadedFile;
import javax.faces.context.FacesContext;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Calendar;

@ManagedBean
@ViewScoped
public class ProductoBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private Producto producto = new Producto();
    private ProductoDAO productoDAO = new ProductoDAO();

    private ProveedorDAO proveedorDAO = new ProveedorDAO();
    private List<Proveedor> listaProveedores;
    private Integer idProveedorSeleccionado;

    private UploadedFile archivoFotoProducto;

    public UploadedFile getArchivoFotoProducto() {
        return archivoFotoProducto;
    }

    public void setArchivoFotoProducto(UploadedFile archivoFotoProducto) {
        this.archivoFotoProducto = archivoFotoProducto;
    }

    @PostConstruct
    public void init() {
        try {
            listaProveedores = proveedorDAO.listarActivos();
            System.out.println(">>> Proveedores cargados: " + listaProveedores.size());
        } catch (SQLException e) {
            System.out.println("Error al cargar proveedores: " + e.getMessage());
            mostrarError("Error al cargar proveedores: " + e.getMessage());
        }
    }

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }

    public List<Producto> getListaProductos() {
        try {
            return productoDAO.listar();
        } catch (SQLException e) {
            System.out.println("Error al listar los producto");
            return null;
        }
    }

    // NUEVO MÉTODO PARA OBTENER STOCK ACTUAL
    public int getStockActual(int idProducto) {
        try {
            Producto p = productoDAO.buscar(idProducto);
            return p != null ? p.getStock() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public String agregar() {
        try {
            // 🔹 Validar nombre
            if (producto.getNombreProducto() == null || producto.getNombreProducto().trim().isEmpty()) {
                mostrarError("El nombre del producto es requerido");
                return null;
            }

            // 🔹 Validar proveedor
            if (idProveedorSeleccionado == null) {
                mostrarError("Debe seleccionar un proveedor");
                return null;
            }

            // 🔹 Validar precio > 0
            if (producto.getPrecioProducto() <= 0) {
                mostrarError("El precio debe ser mayor que 0");
                return null;
            }

            // 🔹 Validar fecha de vencimiento (al menos 1 mes después de hoy)
            Date fechaVenc = producto.getFechaVencimiento();
            if (fechaVenc == null) {
                mostrarError("La fecha de vencimiento es obligatoria");
                return null;
            }

            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date());
            cal.add(Calendar.MONTH, 1); // hoy + 1 mes
            Date minimo = cal.getTime();

            if (!fechaVenc.after(minimo)) {
                mostrarError("La fecha de vencimiento debe ser al menos un mes después de hoy.");
                return null;
            }

            // 🔹 Fecha de ingreso = ahora
            producto.setFechaIngreso(new Date());

            // 🔹 Cargar proveedor desde la BD
            Proveedor proveedor = proveedorDAO.buscar(idProveedorSeleccionado);
            if (proveedor == null) {
                mostrarError("No se encontró el proveedor seleccionado");
                return null;
            }

            producto.setIdProveedor(proveedor.getIdProveedor());
            producto.setNombreProveedor(proveedor.getNombreProveedor());

            // 🔹 Stock inicial SIEMPRE 0
            producto.setStock(0);
            // 🔹 Guardar imagen (si se subió)
            if (archivoFotoProducto != null
                    && archivoFotoProducto.getFileName() != null
                    && !archivoFotoProducto.getFileName().isEmpty()) {

                String nombreArchivo = System.currentTimeMillis() + "_"
                        + Paths.get(archivoFotoProducto.getFileName())
                                .getFileName().toString();

                String rutaProducto = FacesContext.getCurrentInstance()
                        .getExternalContext()
                        .getRealPath("/resources/images/products");

                File directorio = new File(rutaProducto);
                if (!directorio.exists()) {
                    directorio.mkdirs();
                }

                Path destino = Paths.get(directorio.getAbsolutePath(), nombreArchivo);

                try (InputStream input = archivoFotoProducto.getInputStream()) {
                    Files.copy(input, destino, StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    mostrarError("Error al guardar la imagen del producto: " + ex.getMessage());
                    return null;
                }

                // Guardamos el nombre de archivo en el producto
                producto.setImagen(nombreArchivo);
            }

            // 🔹 Guardar en la BD
            productoDAO.agregar(producto);

            // 🔹 Limpiar formulario
            producto = new Producto();
            idProveedorSeleccionado = null;
            archivoFotoProducto = null;
            producto.setCategoria("No comestible");


            mostrarExito("Producto creado correctamente");
            return "HomeAdmin3?faces-redirect=true";

        } catch (SQLException e) {
            System.out.println("Error al insertar el producto: " + e.getMessage());
            mostrarError("Error al crear producto: " + e.getMessage());
            return null;
        }
    }

    public void editar(Producto p) {
        this.producto = p;

    }

    public void actualizar() {
        try {
            // Si se subió una nueva imagen en la edición
            if (archivoFotoProducto != null
                    && archivoFotoProducto.getFileName() != null
                    && !archivoFotoProducto.getFileName().isEmpty()) {

                String nombreArchivo = System.currentTimeMillis() + "_"
                        + Paths.get(archivoFotoProducto.getFileName())
                                .getFileName().toString();

                String rutaProducto = FacesContext.getCurrentInstance()
                        .getExternalContext()
                        .getRealPath("/resources/images/products");

                System.out.println("Ruta física producto (actualizar) = " + rutaProducto);

                if (rutaProducto == null) {
                    FacesContext.getCurrentInstance().addMessage(null,
                            new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                    "Error",
                                    "No se pudo resolver la ruta física para las imágenes de producto."));
                    return;
                }

                File directorio = new File(rutaProducto);
                if (!directorio.exists()) {
                    directorio.mkdirs();
                }

                Path destino = Paths.get(directorio.getAbsolutePath(), nombreArchivo);

                try (InputStream input = archivoFotoProducto.getInputStream()) {
                    Files.copy(input, destino, StandardCopyOption.REPLACE_EXISTING);
                }

                // Guardamos el nuevo nombre en el producto
                producto.setImagen(nombreArchivo);
            }

            System.out.println("STOCK ANTES DE ACTUALIZAR = " + producto.getStock());

            productoDAO.actualizar(producto);

            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                            "Éxito", "Producto actualizado correctamente"));

            // limpiar
            producto = new Producto();
            archivoFotoProducto = null;
            

        } catch (Exception e) {
            System.out.println("Error al actualizar producto: " + e.getMessage());
            e.printStackTrace();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Error", "No se pudo actualizar el producto: " + e.getMessage()));
        }
    }

    public void eliminar(Producto p) {
        try {
            // 1. Verificar si tiene movimientos de inventario
            if (productoDAO.tieneMovimientosInventario(p.getIdProducto())) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN,
                                "Aviso",
                                "No se puede eliminar el producto '" + p.getNombreProducto()
                                + "' porque tiene movimientos de inventario registrados."));
                return; // Salimos sin eliminar
            }

            // 2. Si no tiene movimientos, sí se elimina
            productoDAO.eliminar(p);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                            "Éxito", "Producto eliminado correctamente"));

        } catch (SQLException e) {
            System.out.println("Error al eliminar producto: " + e.getMessage());
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Error", "No se pudo eliminar el producto: " + e.getMessage()));
        }
    }

    public List<Producto> getProductosPorProveedor(int idProveedor) {
        try {
            return productoDAO.listarPorProveedor(idProveedor);
        } catch (SQLException e) {
            mostrarError("Error al listar productos por proveedor");
            return null;
        }
    }

    private void mostrarExito(String mensaje) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Éxito", mensaje));
    }

    private void mostrarError(String mensaje) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", mensaje));
    }

    public Integer getIdProveedorSeleccionado() {
        return idProveedorSeleccionado;
    }

    public void setIdProveedorSeleccionado(Integer idProveedorSeleccionado) {
        this.idProveedorSeleccionado = idProveedorSeleccionado;
    }

    public List<Proveedor> getListaProveedores() {
        try {
            listaProveedores = proveedorDAO.listar();
        } catch (SQLException e) {
            mostrarError("Error al cargar proveedores: " + e.getMessage());
        }
        return listaProveedores;
    }

}
