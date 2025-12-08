package Controlador;

import DAO.MovInventarioDAO;
import DAO.ProductoDAO;
import Modelo.EnumRoles;
import Modelo.MovInventario;
import Modelo.Producto;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Named;

import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.file.UploadedFile;

import javax.faces.component.UIComponent;
import javax.faces.validator.ValidatorException;

@Named("movInventarioBean")
@SessionScoped
public class MovInventarioBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(MovInventarioBean.class.getName());

    // --------- MODELO PRINCIPAL ----------
    private MovInventario movInventario;
    private MovInventario movSeleccionado;
    private List<MovInventario> listaMovInventarios;
    private List<MovInventario> movInventariosFiltrados;

    // --------- DAOs ----------
    private MovInventarioDAO movInventarioDAO;
    private ProductoDAO productoDAO;

    // --------- PRODUCTOS ----------
    private List<Producto> listaProductos;
    private Integer idProductoSeleccionado;
    private Producto productoDetalle;

    // --------- UPLOADS ----------
    private UploadedFile archivoFactura;   // factura PDF u otro
    private UploadedFile imagenProducto;   // opcional

    // --------- CONTEXTO / ESTADO ----------
    private String tipoMovimiento; // "Entrada" o "Salida"

    // --------- FILTROS PRINCIPALES ----------
    private boolean usarFiltroTrimestre;
    private Date fechaInicioFiltro;
    private Date fechaFinFiltro;
    private int añoFiltro;
    private int trimestreFiltro;

    // --------- FILTROS ADICIONALES ----------
    private String filtroTipo;             // Entrada / Salida
    private Integer filtroProductoId;      // idProducto específico
    private String filtroTipoEspecifico;   // COMPRA, DEVOLUCION, VENTA, CADUCADO, OTRO
    private String filtroCategoria;        // Comestible / No comestible
    private String filtroProveedorCliente;

    private Integer mesFiltro;             // 1-12
    private Integer diaFiltro;             // 1-31
    private Date fechaEspecificaFiltro;    // un día exacto

    private Date fechaVencimientoProducto;

    // ==============================================
    // INIT
    // ==============================================
    @PostConstruct
    public void init() {
        movInventarioDAO = new MovInventarioDAO();
        productoDAO = new ProductoDAO();

        movInventario = new MovInventario();
        movSeleccionado = null;

        cargarProductos();
        recargarMovimientos();

        Calendar cal = Calendar.getInstance();
        añoFiltro = cal.get(Calendar.YEAR);
        trimestreFiltro = (cal.get(Calendar.MONTH) / 3) + 1;

        usarFiltroTrimestre = false;
        filtroTipo = null;
        filtroProductoId = null;
        filtroTipoEspecifico = null;
        filtroProveedorCliente = null;
        fechaVencimientoProducto = null;
    }

    private void cargarProductos() {
        try {
            // Ajusta si tu ProductoDAO tiene otro método para listar
            listaProductos = productoDAO.listar();
        } catch (Exception e) {
            listaProductos = new ArrayList<>();
            LOGGER.log(Level.SEVERE, "Error cargando productos para MovInventarioBean", e);
        }
    }

    private void recargarMovimientos() {
        try {
            listaMovInventarios = movInventarioDAO.listar();
        } catch (Exception e) {
            listaMovInventarios = new ArrayList<>();
            LOGGER.log(Level.SEVERE, "Error cargando movimientos de inventario", e);
        }
        movInventariosFiltrados = new ArrayList<>(listaMovInventarios);
    }

    // ==============================================
    // PREPARAR FORMULARIOS
    // ==============================================
    // ==============================================
// PREPARAR FORMULARIOS
// ==============================================
    public void prepararNuevaEntrada() {
        movInventario = new MovInventario();
        tipoMovimiento = "Entrada";

        movInventario.setTipoString("Entrada");
        movInventario.setFechaEntrada(new Date());

        // limpiar campos propios de salida
        movInventario.setFechaSalida(null);
        movInventario.setTipoSalida(null);
        movInventario.setCantidadSalida(null);
        movInventario.setCliente(null);
        movInventario.setPrecioVenta(null);

        // comunes
        movInventario.setNumeroFactura(null);

        // estado de pantalla
        idProductoSeleccionado = null;
        fechaVencimientoProducto = null;
        archivoFactura = null;
        imagenProducto = null;
        productoDetalle = null;
    }

    public void prepararNuevaSalida() {
        movInventario = new MovInventario();
        tipoMovimiento = "Salida";

        movInventario.setTipoString("Salida");
        movInventario.setFechaSalida(new Date());
        movInventario.setTipoSalida("CADUCADO"); // por defecto en salida manual

        // limpiar campos propios de entrada
        movInventario.setFechaEntrada(null);
        movInventario.setTipoEntrada(null);
        movInventario.setCantidadEntrada(null);
        movInventario.setProveedor(null);
        movInventario.setPrecioCompra(null);

        // en salida manual no usamos factura / cliente / precioVenta
        movInventario.setNumeroFactura(null);
        movInventario.setCliente(null);
        movInventario.setPrecioVenta(null);

        // estado de pantalla
        idProductoSeleccionado = null;
        fechaVencimientoProducto = null;
        archivoFactura = null;
        imagenProducto = null;
        productoDetalle = null;
    }

    //HELPERS
    public Date getFechaMovimiento(MovInventario m) {
        return "Entrada".equalsIgnoreCase(m.getTipoString()) ? m.getFechaEntrada() : m.getFechaSalida();
    }

    public Integer getCantidadMovimiento(MovInventario m) {
        return "Entrada".equalsIgnoreCase(m.getTipoString()) ? m.getCantidadEntrada() : m.getCantidadSalida();
    }

    public String getTipoEspecifico(MovInventario m) {
        return "Entrada".equalsIgnoreCase(m.getTipoString()) ? m.getTipoEntrada() : m.getTipoSalida();
    }

    private String obtenerCategoriaProducto(Integer idProducto) {
        if (idProducto == null || listaProductos == null) {
            return null;
        }
        for (Producto p : listaProductos) {
            // p.getIdProducto() es int, idProducto es Integer (ya validamos que no sea null)
            if (p.getIdProducto() == idProducto) {
                return p.getCategoria();
            }
        }
        return null;
    }

    public String getRutaImagenProductoDetalle() {
        if (productoDetalle == null || productoDetalle.getImagen() == null) {
            return null;
        }

        String img = productoDetalle.getImagen().trim();
        if (img.isEmpty()) {
            return null;
        }

        // Si algún día se guarda una URL completa
        if (img.startsWith("http://") || img.startsWith("https://")) {
            return img;
        }

        String ctx = FacesContext.getCurrentInstance()
                .getExternalContext()
                .getRequestContextPath();

        // Si algún día guardas ruta relativa completa en BD tipo "/resources/..."
        if (img.startsWith("/")) {
            return ctx + img;
        }

        // Caso actual: solo nombreArchivo
        return ctx + "/resources/images/products/" + img;
    }

    // ==============================================
    // REGISTRAR ENTRADAS
    // ==============================================
    public void registrarEntrada() {
        try {
            if (idProductoSeleccionado == null) {
                mostrarError("Debe seleccionar un producto.");
                return;
            }

            if (movInventario.getCantidadEntrada() == null || movInventario.getCantidadEntrada() <= 0) {
                mostrarError("La cantidad de entrada debe ser mayor que cero.");
                return;
            }

            if (movInventario.getTipoEntrada() == null || movInventario.getTipoEntrada().trim().isEmpty()) {
                mostrarError("Debe seleccionar el tipo de entrada (COMPRA, DEVOLUCION, etc.).");
                return;
            }

            if (movInventario.getProveedor() == null || movInventario.getProveedor().trim().isEmpty()) {
                mostrarError("Debe especificar el proveedor.");
                return;
            }

            // Descripción condicional:
            // COMPRA -> opcional
            // DEVOLUCION -> obligatoria
            if ("DEVOLUCION".equalsIgnoreCase(movInventario.getTipoEntrada())) {
                if (movInventario.getDescripcion() == null || movInventario.getDescripcion().trim().isEmpty()) {
                    mostrarError("La descripción es obligatoria para DEVOLUCIÓN.");
                    return;
                }
            }

            // Fecha siempre es la actual del registro
            movInventario.setFechaEntrada(new java.util.Date());

            // Validación específica de cantidad por tipo de entrada
            if (!validarCantidad(movInventario.getTipoEntrada(), movInventario.getCantidadEntrada())) {
                return;
            }

            // precioVenta automático si hay precioCompra y aún no hay precioVenta
            if (movInventario.getPrecioCompra() != null && movInventario.getPrecioCompra() > 0) {
                if (movInventario.getPrecioVenta() == null || movInventario.getPrecioVenta() <= 0) {
                    movInventario.setPrecioVenta(movInventario.getPrecioCompra() * 1.15);
                }
            }

            Producto producto = productoDAO.buscar(idProductoSeleccionado);
            if (producto == null) {
                mostrarError("No se encontró el producto seleccionado.");
                return;
            }

            int stockAnterior = producto.getStock();
            int stockNuevo = stockAnterior + movInventario.getCantidadEntrada();

            // seteo de campos claves
            movInventario.setTipoString("Entrada");
            movInventario.setTipoSalida(null);
            movInventario.setCantidadSalida(null);
            movInventario.setFechaSalida(null);
            movInventario.setCliente(null);

            movInventario.setIdProducto(idProductoSeleccionado);
            movInventario.setStockAnterior(stockAnterior);
            movInventario.setStockNuevo(stockNuevo);
            movInventario.setUsuarioRegistro(obtenerUsuarioRegistro());

            // Opcional: aseguramos que no se use numeroFactura desde UI
            movInventario.setNumeroFactura(null);

            // archivoFactura ya fue seteado en subirFactura() si se subió algo
            movInventarioDAO.registrarMovimiento(movInventario);

            // actualizar stock real del producto
            productoDAO.actualizarStock(idProductoSeleccionado, stockNuevo);

            mostrarExito("Entrada de inventario registrada correctamente.");
            recargarMovimientos();
            prepararNuevaEntrada();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al registrar la entrada de inventario", e);
            mostrarError("Error al registrar la entrada de inventario: " + e.getMessage());
        }
    }

    // ==============================================
    // REGISTRAR SALIDAS
    // ==============================================
    public void registrarSalida() {
        try {
            if (idProductoSeleccionado == null) {
                mostrarError("Debe seleccionar un producto.");
                return;
            }

            if (movInventario.getCantidadSalida() == null || movInventario.getCantidadSalida() <= 0) {
                mostrarError("La cantidad de salida debe ser mayor que cero.");
                return;
            }

            // Tipo salida por defecto
            if (movInventario.getTipoSalida() == null || movInventario.getTipoSalida().trim().isEmpty()) {
                movInventario.setTipoSalida("CADUCADO");
            }

            // Validar que el tipo sea solo CADUCADO u OTRO (manual)
            if (!Arrays.asList("CADUCADO", "OTRO").contains(movInventario.getTipoSalida().toUpperCase())) {
                mostrarError("Tipo de salida no permitido para registro manual.");
                return;
            }

            // Reglas de descripción
            String desc = movInventario.getDescripcion();

            if ("OTRO".equalsIgnoreCase(movInventario.getTipoSalida())) {
                if (desc == null || desc.trim().isEmpty()) {
                    mostrarError("La descripción es obligatoria cuando el tipo de salida es OTRO.");
                    return;
                }
            } else if ("CADUCADO".equalsIgnoreCase(movInventario.getTipoSalida())) {
                // opcional: si no ponen nada, dejamos uno por defecto
                if (desc == null || desc.trim().isEmpty()) {
                    movInventario.setDescripcion("Producto vencido");
                }
            }

            Producto producto = productoDAO.buscar(idProductoSeleccionado);
            if (producto == null) {
                mostrarError("No se encontró el producto seleccionado.");
                return;
            }

            int stockActual = producto.getStock();
            int cantidadSalida = movInventario.getCantidadSalida();

            if (cantidadSalida > stockActual) {
                mostrarError("No hay stock suficiente. Stock actual: " + stockActual);
                return;
            }

            int stockAnterior = stockActual;
            int stockNuevo = stockActual - cantidadSalida;

            // Fijar fecha actual del movimiento
            movInventario.setFechaSalida(new Date());

            // Limpiar campos que NO aplican a salida manual
            movInventario.setCliente(null);
            movInventario.setPrecioVenta(null);
            movInventario.setNumeroFactura(null);

            // Seteo de campos clave
            movInventario.setTipoString("Salida");
            movInventario.setTipoEntrada(null);
            movInventario.setCantidadEntrada(null);
            movInventario.setFechaEntrada(null);

            movInventario.setIdProducto(idProductoSeleccionado);
            movInventario.setStockAnterior(stockAnterior);
            movInventario.setStockNuevo(stockNuevo);
            movInventario.setProveedor(producto.getNombreProveedor());
            movInventario.setUsuarioRegistro(obtenerUsuarioRegistro());

            movInventarioDAO.registrarMovimiento(movInventario);
            productoDAO.actualizarStock(idProductoSeleccionado, stockNuevo);

            mostrarExito("Salida de inventario registrada correctamente.");
            recargarMovimientos();
            prepararNuevaSalida();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al registrar la salida de inventario", e);
            mostrarError("Error al registrar la salida de inventario: " + e.getMessage());
        }
    }

    // ==============================================
    // UPLOAD DE FACTURA
    // ==============================================
    private String getFacturasBasePath() {
        ExternalContext ec = FacesContext.getCurrentInstance().getExternalContext();
        String configured = ec.getInitParameter("agrivi.facturas.path");

        if (configured != null && !configured.trim().isEmpty()) {
            return configured.trim();
        }

        // fallback
        return System.getProperty("user.home") + File.separator + "AGRIVI" + File.separator + "facturas";
    }

    public void subirFactura(FileUploadEvent event) {
        archivoFactura = event.getFile();

        if (archivoFactura == null || archivoFactura.getFileName() == null
                || archivoFactura.getFileName().trim().isEmpty()) {
            mostrarError("No se seleccionó ningún archivo de factura.");
            return;
        }

        try {
            String basePath = getFacturasBasePath();
            File dir = new File(basePath);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String nombreOriginal = archivoFactura.getFileName();
            String nombreLimpio = nombreOriginal.replaceAll("[^a-zA-Z0-9\\.\\-_]", "_");
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmssSSS").format(new Date());
            String nombreFinal = timeStamp + "_" + nombreLimpio;

            File destino = new File(dir, nombreFinal);

            try (InputStream in = archivoFactura.getInputStream(); FileOutputStream out = new FileOutputStream(destino)) {

                byte[] buffer = new byte[4096];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }

            // Guarda ruta REAL o una ruta lógica que tú controles
            movInventario.setArchivoFactura(destino.getAbsolutePath());

            mostrarExito("Factura subida correctamente.");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error subiendo archivo de factura", e);
            mostrarError("Error al subir la factura: " + e.getMessage());
        }
    }

    // ==============================================
    // REACCIONES A CAMBIO DE PRODUCTO
    // ==============================================
    public void onProductoChange() {
        try {
            if (idProductoSeleccionado == null) {
                return;
            }

            Producto p = productoDAO.buscar(idProductoSeleccionado);
            productoDetalle = p;

            movInventario.setProveedor(p.getNombreProveedor());
            movInventario.setPrecioCompra((double) p.getPrecioProducto());
            movInventario.setPrecioVenta(p.getPrecioProducto() * 1.15);
            movInventario.setIdProducto(idProductoSeleccionado);

            fechaVencimientoProducto = p.getFechaVencimiento();

        } catch (Exception e) {
            mostrarError("Error al cargar datos del producto.");
        }
    }

    public void onProductoCaducadoChange() {
        if (idProductoSeleccionado == null) {
            fechaVencimientoProducto = null;
            return;
        }

        try {
            Producto p = productoDAO.buscar(idProductoSeleccionado);
            productoDetalle = p;

            if (p != null) {
                fechaVencimientoProducto = p.getFechaVencimiento();
                movInventario.setProveedor(p.getNombreProveedor());
                movInventario.setIdProducto(idProductoSeleccionado);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al cargar producto caducado", e);
            mostrarError("Error al cargar datos del producto (caducado).");
        }
    }

    public String nombreProductoPorId(Integer idProducto) {
        if (idProducto == null || listaProductos == null) {
            return "";
        }
        for (Producto p : listaProductos) {
            if (p.getIdProducto() == idProducto) {
                return p.getNombreProducto();
            }
        }
        return "";
    }

    // ==============================================
    // FILTROS
    // ==============================================
    public void aplicarFiltro() {
        try {
            // 1) Trimestre (cuando el usuario lo activa)
            if (usarFiltroTrimestre && añoFiltro > 0 && trimestreFiltro > 0) {
                aplicarFiltroTrimestre();

                // 2) Fecha específica
            } else if (fechaEspecificaFiltro != null) {
                aplicarFiltroFechaEspecifica();

                // 3) Día (requiere año + mes + día)
            } else if (añoFiltro > 0 && mesFiltro != null && mesFiltro > 0
                    && diaFiltro != null && diaFiltro > 0) {
                aplicarFiltroDia();

                // 4) Mes (requiere año + mes)
            } else if (añoFiltro > 0 && mesFiltro != null && mesFiltro > 0) {
                aplicarFiltroMes();

                // 5) Año
            } else if (añoFiltro > 0) {
                aplicarFiltroAnio();

                // 6) Rango por fechas
            } else if (fechaInicioFiltro != null && fechaFinFiltro != null) {
                aplicarFiltroFechas();

            } else {
                recargarMovimientos();
            }

            aplicarFiltrosAdicionales();
            mostrarExito("Filtros aplicados.");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al aplicar filtros", e);
            mostrarError("Error al aplicar filtros: " + e.getMessage());
        }
    }

    public void aplicarFiltroFechas() {
        if (fechaInicioFiltro == null || fechaFinFiltro == null) {
            mostrarError("Debe seleccionar fecha inicial y final para filtrar.");
            return;
        }
        try {
            listaMovInventarios = movInventarioDAO.filtrarPorFechas(fechaInicioFiltro, fechaFinFiltro);
            movInventariosFiltrados = new ArrayList<>(listaMovInventarios);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al filtrar por fechas", e);
            mostrarError("Error al filtrar por fechas: " + e.getMessage());
        }
    }

    public void aplicarFiltroTrimestre() {
        if (añoFiltro <= 0 || trimestreFiltro <= 0) {
            mostrarError("Debe seleccionar año y trimestre válidos.");
            return;
        }
        try {
            listaMovInventarios = movInventarioDAO.filtrarPorTrimestre(añoFiltro, trimestreFiltro);
            movInventariosFiltrados = new ArrayList<>(listaMovInventarios);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al filtrar por trimestre", e);
            mostrarError("Error al filtrar por trimestre: " + e.getMessage());
        }
    }

    public void aplicarFiltrosAdicionales() {
        if (listaMovInventarios == null) {
            movInventariosFiltrados = new ArrayList<>();
            return;
        }

        List<MovInventario> filtrados = new ArrayList<>();

        for (MovInventario mov : listaMovInventarios) {
            boolean coincide = true;

            // Tipo Entrada/Salida
            if (filtroTipo != null && !filtroTipo.trim().isEmpty()) {
                if (mov.getTipoString() == null
                        || !filtroTipo.equalsIgnoreCase(mov.getTipoString())) {
                    coincide = false;
                }
            }

            // Producto
            if (coincide && filtroProductoId != null) {
                if (mov.getIdProducto() == null
                        || !filtroProductoId.equals(mov.getIdProducto())) {
                    coincide = false;
                }
            }

            // Tipo específico
            if (coincide && filtroTipoEspecifico != null && !filtroTipoEspecifico.trim().isEmpty()) {
                String tipoEsp = null;
                if ("Entrada".equalsIgnoreCase(mov.getTipoString())) {
                    tipoEsp = mov.getTipoEntrada();
                } else if ("Salida".equalsIgnoreCase(mov.getTipoString())) {
                    tipoEsp = mov.getTipoSalida();
                }
                if (tipoEsp == null || !filtroTipoEspecifico.equalsIgnoreCase(tipoEsp)) {
                    coincide = false;
                }
            }

            // Categoría
            if (coincide && filtroCategoria != null && !filtroCategoria.trim().isEmpty()) {
                String categoriaMov = obtenerCategoriaProducto(mov.getIdProducto());
                if (categoriaMov == null || !filtroCategoria.equalsIgnoreCase(categoriaMov)) {
                    coincide = false;
                }
            }

            // Proveedor/Cliente
            if (coincide && filtroProveedorCliente != null && !filtroProveedorCliente.trim().isEmpty()) {
                String valor = filtroProveedorCliente.toLowerCase();
                boolean ok = false;

                if (mov.getProveedor() != null && mov.getProveedor().toLowerCase().contains(valor)) {
                    ok = true;
                }
                if (!ok && mov.getCliente() != null && mov.getCliente().toLowerCase().contains(valor)) {
                    ok = true;
                }

                if (!ok) {
                    coincide = false;
                }
            }

            if (coincide) {
                filtrados.add(mov);
            }
        }

        movInventariosFiltrados = filtrados;
    }

    private Date inicioDelDia(Date fecha) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(fecha);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private Date finDelDia(Date fecha) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(fecha);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTime();
    }

    public void aplicarFiltroFechaEspecifica() {
        try {
            Date inicio = inicioDelDia(fechaEspecificaFiltro);
            Date fin = finDelDia(fechaEspecificaFiltro);

            listaMovInventarios = movInventarioDAO.filtrarPorFechas(inicio, fin);
            movInventariosFiltrados = new ArrayList<>(listaMovInventarios);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al filtrar por fecha específica", e);
            mostrarError("Error al filtrar por fecha específica: " + e.getMessage());
        }
    }

    public void aplicarFiltroAnio() {
        try {
            Calendar cal = Calendar.getInstance();

            cal.set(Calendar.YEAR, añoFiltro);
            cal.set(Calendar.MONTH, Calendar.JANUARY);
            cal.set(Calendar.DAY_OF_MONTH, 1);
            Date inicio = inicioDelDia(cal.getTime());

            cal.set(Calendar.MONTH, Calendar.DECEMBER);
            cal.set(Calendar.DAY_OF_MONTH, 31);
            Date fin = finDelDia(cal.getTime());

            listaMovInventarios = movInventarioDAO.filtrarPorFechas(inicio, fin);
            movInventariosFiltrados = new ArrayList<>(listaMovInventarios);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al filtrar por año", e);
            mostrarError("Error al filtrar por año: " + e.getMessage());
        }
    }

    public void aplicarFiltroMes() {
        try {
            Calendar cal = Calendar.getInstance();

            cal.set(Calendar.YEAR, añoFiltro);
            cal.set(Calendar.MONTH, mesFiltro - 1);
            cal.set(Calendar.DAY_OF_MONTH, 1);
            Date inicio = inicioDelDia(cal.getTime());

            int lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
            cal.set(Calendar.DAY_OF_MONTH, lastDay);
            Date fin = finDelDia(cal.getTime());

            listaMovInventarios = movInventarioDAO.filtrarPorFechas(inicio, fin);
            movInventariosFiltrados = new ArrayList<>(listaMovInventarios);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al filtrar por mes", e);
            mostrarError("Error al filtrar por mes: " + e.getMessage());
        }
    }

    public void aplicarFiltroDia() {
        try {
            Calendar cal = Calendar.getInstance();

            cal.set(Calendar.YEAR, añoFiltro);
            cal.set(Calendar.MONTH, mesFiltro - 1);
            cal.set(Calendar.DAY_OF_MONTH, diaFiltro);

            Date inicio = inicioDelDia(cal.getTime());
            Date fin = finDelDia(cal.getTime());

            listaMovInventarios = movInventarioDAO.filtrarPorFechas(inicio, fin);
            movInventariosFiltrados = new ArrayList<>(listaMovInventarios);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al filtrar por día", e);
            mostrarError("Error al filtrar por día: " + e.getMessage());
        }
    }

    public List<String> getTiposEntradaUI() {
        return Arrays.asList("COMPRA", "DEVOLUCION");
    }

    public List<String> getTiposSalidaUI() {
        return Arrays.asList("CADUCADO", "OTRO");
    }

    public boolean hayFiltrosActivos() {

        // Trimestre
        if (usarFiltroTrimestre) {
            return true;
        }

        // Fechas
        if (fechaEspecificaFiltro != null) {
            return true;
        }
        if (fechaInicioFiltro != null || fechaFinFiltro != null) {
            return true;
        }

        // Mes / Día
        if (mesFiltro != null || diaFiltro != null) {
            return true;
        }

        // Contenido
        if (filtroTipo != null && !filtroTipo.trim().isEmpty()) {
            return true;
        }
        if (filtroProductoId != null) {
            return true;
        }
        if (filtroTipoEspecifico != null && !filtroTipoEspecifico.trim().isEmpty()) {
            return true;
        }
        if (filtroCategoria != null && !filtroCategoria.trim().isEmpty()) {
            return true;
        }
        if (filtroProveedorCliente != null && !filtroProveedorCliente.trim().isEmpty()) {
            return true;
        }

        return false;
    }

    public void limpiarFiltro() {
        usarFiltroTrimestre = false;
        fechaInicioFiltro = null;
        fechaFinFiltro = null;
        fechaEspecificaFiltro = null;

        Calendar cal = Calendar.getInstance();
        añoFiltro = cal.get(Calendar.YEAR);
        trimestreFiltro = (cal.get(Calendar.MONTH) / 3) + 1;

        mesFiltro = null;
        diaFiltro = null;

        filtroTipo = null;
        filtroProductoId = null;
        filtroTipoEspecifico = null;
        filtroCategoria = null;
        filtroProveedorCliente = null;

        recargarMovimientos();
        mostrarExito("Filtros limpiados.");
    }

    // ==============================================
    // TOTALES
    // ==============================================
    public int getTotalEntradas() {
        try {
            return movInventarioDAO.getTotalEntradas();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error obteniendo total de entradas", e);
            return 0;
        }
    }

    public int getTotalSalidas() {
        try {
            return movInventarioDAO.getTotalSalidas();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error obteniendo total de salidas", e);
            return 0;
        }
    }

    // ==============================================
    // LISTAS PARA COMBOS / INFORMES
    // ==============================================
    public List<Integer> getProductoIdsUnicos() {
        Set<Integer> ids = new HashSet<>();
        if (listaMovInventarios != null) {
            for (MovInventario mov : listaMovInventarios) {
                if (mov.getIdProducto() != null) {
                    ids.add(mov.getIdProducto());
                }
            }
        }
        return new ArrayList<>(ids);
    }

    public List<String> getProveedoresClientesUnicos() {
        Set<String> nombres = new HashSet<>();
        if (listaMovInventarios != null) {
            for (MovInventario mov : listaMovInventarios) {
                if (mov.getProveedor() != null && !mov.getProveedor().trim().isEmpty()) {
                    nombres.add(mov.getProveedor());
                }
                if (mov.getCliente() != null && !mov.getCliente().trim().isEmpty()) {
                    nombres.add(mov.getCliente());
                }
            }
        }
        return new ArrayList<>(nombres);
    }

    public List<Integer> getAniosDisponibles() {
        Set<Integer> anios = new HashSet<>();
        if (listaMovInventarios != null) {
            Calendar cal = Calendar.getInstance();
            for (MovInventario mov : listaMovInventarios) {
                Date f = mov.getFechaEntrada();
                if (f == null) {
                    f = mov.getFechaSalida();
                }
                if (f == null) {
                    f = mov.getFechaRegistro();
                }
                if (f != null) {
                    cal.setTime(f);
                    anios.add(cal.get(Calendar.YEAR));
                }
            }
        }
        return new ArrayList<>(anios);
    }

    public List<Integer> getTrimestres() {
        return Arrays.asList(1, 2, 3, 4);
    }

    public List<String> getTiposMovimiento() {
        return Arrays.asList("Entrada", "Salida");
    }

    public String getDescripcionFiltro() {
        StringBuilder sb = new StringBuilder("Filtro: ");

        boolean hayAlgo = false;

        if (usarFiltroTrimestre) {
            sb.append("Año ").append(añoFiltro)
                    .append(", Trimestre ").append(trimestreFiltro);
            hayAlgo = true;
        } else if (fechaInicioFiltro != null && fechaFinFiltro != null) {
            sb.append("Fechas entre ").append(fechaInicioFiltro)
                    .append(" y ").append(fechaFinFiltro);
            hayAlgo = true;
        }

        if (filtroTipo != null && !filtroTipo.trim().isEmpty()) {
            if (hayAlgo) {
                sb.append(" | ");
            }
            sb.append("Tipo: ").append(filtroTipo);
            hayAlgo = true;
        }

        if (filtroProductoId != null) {
            if (hayAlgo) {
                sb.append(" | ");
            }
            sb.append("Producto ID: ").append(filtroProductoId);
            hayAlgo = true;
        }

        if (filtroTipoEspecifico != null && !filtroTipoEspecifico.trim().isEmpty()) {
            if (hayAlgo) {
                sb.append(" | ");
            }
            sb.append("Tipo específico: ").append(filtroTipoEspecifico);
            hayAlgo = true;
        }

        if (filtroProveedorCliente != null && !filtroProveedorCliente.trim().isEmpty()) {
            if (hayAlgo) {
                sb.append(" | ");
            }
            sb.append("Proveedor/Cliente: ").append(filtroProveedorCliente);
            hayAlgo = true;
        }

        if (!hayAlgo) {
            sb.append("sin filtros");
        }

        return sb.toString();
    }

    // ==============================================
    // UTILIDAD: USUARIO REGISTRO
    // ==============================================
    private String obtenerUsuarioRegistro() {
        try {
            FacesContext context = FacesContext.getCurrentInstance();
            if (context == null) {
                return "Usuario desconocido";
            }

            Object obj = context.getExternalContext().getSessionMap().get("usuarioBean");
            if (obj instanceof UsuarioBean) {
                UsuarioBean usuarioBean = (UsuarioBean) obj;
                if (usuarioBean.getUsuario() != null) {
                    String nombre = usuarioBean.getUsuario().getNombre();
                    EnumRoles rol = usuarioBean.getUsuario().getRol();

                    if (rol != null) {
                        return nombre + " (" + rol.name() + ")";
                    } else {
                        return nombre;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "No se pudo obtener el usuario en sesión para MovInventario", e);
        }
        return "Usuario desconocido";
    }

    // ==============================================
    // FLAGS / PROPIEDADES DERIVADAS PARA LA VISTA
    // ==============================================
    /**
     * Indica si la entrada actual es de tipo COMPRA. Se usa en el XHTML como
     * rendered="#{movInventarioBean.compra}".
     */
    public boolean isCompra() {
        return movInventario != null
                && movInventario.getTipoEntrada() != null
                && "COMPRA".equalsIgnoreCase(movInventario.getTipoEntrada());
    }

    /**
     * Setter vacío solo para que JSF no se queje al bindear la propiedad.
     */
    public void setCompra(boolean compra) {
        // no hacemos nada; la propiedad es calculada a partir del tipo de entrada
    }

    /**
     * Indica si la entrada actual es una devolución. Se usa en el XHTML como
     * rendered="#{movInventarioBean.devolucion}".
     */
    public boolean isDevolucion() {
        return movInventario != null
                && movInventario.getTipoEntrada() != null
                && "DEVOLUCION".equalsIgnoreCase(movInventario.getTipoEntrada());
    }

    /**
     * Setter vacío solo para que JSF no se queje al bindear la propiedad.
     */
    public void setDevolucion(boolean devolucion) {
        // no hacemos nada; la propiedad es calculada a partir del tipo de entrada
    }

    /**
     * Se invoca desde el checkbox de "usar filtro por trimestre". En el XHTML:
     * listener="#{movInventarioBean.toggleFiltroTrimestre}".
     */
    public void toggleFiltroTrimestre() {
        this.usarFiltroTrimestre = !this.usarFiltroTrimestre;
    }

    /**
     * Ruta de la imagen del producto asociada al movimiento seleccionado. Si en
     * BD se guarda algo como '/resources/uploads/imagenes/xxx.jpg', aquí se le
     * antepone el contextPath de la aplicación.
     */
    public String getRutaImagenMovimientoSeleccionado() {
        if (movSeleccionado != null
                && movSeleccionado.getImagenProducto() != null
                && !movSeleccionado.getImagenProducto().trim().isEmpty()) {

            String ctx = FacesContext.getCurrentInstance()
                    .getExternalContext()
                    .getRequestContextPath();
            return ctx + movSeleccionado.getImagenProducto();
        }
        return null;
    }

    /**
     * Alias para el botón de "ver detalle" en la tabla de movimientos. En el
     * XHTML: actionListener="#{movInventarioBean.prepararVerDetalle(mov)}".
     */
    public void prepararVerDetalle(MovInventario mov) {
        this.movSeleccionado = mov;
        this.productoDetalle = null;

        if (mov != null && mov.getIdProducto() != null) {
            try {
                this.productoDetalle = productoDAO.buscar(mov.getIdProducto());
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error cargando producto para detalle de movimiento", e);
            }
        }
    }

    /**
     * Validador JSF para la cantidad de entrada (se referencia en el XHTML como
     * validator="#{movInventarioBean.validarCantidad}").
     */
    public void validarCantidad(FacesContext context, UIComponent component, Object value)
            throws ValidatorException {

        Integer cantidad = null;
        if (value != null) {
            if (value instanceof Number) {
                cantidad = ((Number) value).intValue();
            } else {
                try {
                    cantidad = Integer.valueOf(value.toString());
                } catch (NumberFormatException e) {
                    // si no se puede parsear, dejamos cantidad en null para que falle la validación
                }
            }
        }

        String tipoEntrada = (movInventario != null) ? movInventario.getTipoEntrada() : null;

        // reutilizamos tu lógica de negocio
        if (!validarCantidad(tipoEntrada, cantidad)) {
            // El método privado ya mostró un FacesMessage de error,
            // pero JSF requiere una ValidatorException para marcar el componente como inválido.
            throw new ValidatorException(
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Cantidad inválida",
                            "Revise la cantidad ingresada según el tipo de movimiento.")
            );
        }
    }

    // ==============================================
    // VALIDACIÓN DE CANTIDAD (ENTRADAS)
    // ==============================================
    /**
     * Reglas: - Si tipoEntrada == "COMPRA": cantidad >= 10 - Si tipoEntrada ==
     * "DEVOLUCION": cantidad >= 1 - Caso contrario: solo verificar que no sea
     * null ni <= 0 (ya se validó antes)
     */
    private boolean validarCantidad(String tipoEntrada, Integer cantidad) {
        if (tipoEntrada == null || cantidad == null) {
            mostrarError("Debe especificar el tipo de entrada y la cantidad.");
            return false;
        }

        String tipo = tipoEntrada.toUpperCase();

        if ("COMPRA".equals(tipo) && cantidad < 10) {
            mostrarError("Para movimientos de tipo COMPRA, la cantidad mínima es 10.");
            return false;
        }

        if ("DEVOLUCION".equals(tipo) && cantidad < 1) {
            mostrarError("Para movimientos de tipo DEVOLUCION, la cantidad mínima es 1.");
            return false;
        }

        // otros tipos: ya validado que > 0
        return true;
    }

    // ==============================================
    // MENSAJES
    // ==============================================
    public void mostrarExito(String msg) {
        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, "Éxito", msg);
        FacesContext.getCurrentInstance().addMessage(null, message);
    }

    public void mostrarError(String msg) {
        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", msg);
        FacesContext.getCurrentInstance().addMessage(null, message);
    }

    // ==============================================
    // DETALLE
    // ==============================================
    public void verDetalle(MovInventario mov) {
        this.movSeleccionado = mov;
        this.productoDetalle = null;

        if (mov != null && mov.getIdProducto() != null) {
            try {
                this.productoDetalle = productoDAO.buscar(mov.getIdProducto());
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error cargando producto para detalle de movimiento", e);
            }
        }
    }

    // ==============================================
    // GETTERS / SETTERS
    // ==============================================
    public MovInventario getMovInventario() {
        return movInventario;
    }

    public void setMovInventario(MovInventario movInventario) {
        this.movInventario = movInventario;
    }

    public MovInventario getMovSeleccionado() {
        return movSeleccionado;
    }

    public void setMovSeleccionado(MovInventario movSeleccionado) {
        this.movSeleccionado = movSeleccionado;
    }

    public List<MovInventario> getListaMovInventarios() {
        return listaMovInventarios;
    }

    public void setListaMovInventarios(List<MovInventario> listaMovInventarios) {
        this.listaMovInventarios = listaMovInventarios;
    }

    public List<MovInventario> getMovInventariosFiltrados() {
        if (movInventariosFiltrados == null) {
            movInventariosFiltrados = new ArrayList<>(listaMovInventarios);
        }
        return movInventariosFiltrados;
    }

    public void setMovInventariosFiltrados(List<MovInventario> movInventariosFiltrados) {
        this.movInventariosFiltrados = movInventariosFiltrados;
    }

    public MovInventarioDAO getMovInventarioDAO() {
        return movInventarioDAO;
    }

    public void setMovInventarioDAO(MovInventarioDAO movInventarioDAO) {
        this.movInventarioDAO = movInventarioDAO;
    }

    public ProductoDAO getProductoDAO() {
        return productoDAO;
    }

    public void setProductoDAO(ProductoDAO productoDAO) {
        this.productoDAO = productoDAO;
    }

    public List<Producto> getListaProductos() {
        return listaProductos;
    }

    public void setListaProductos(List<Producto> listaProductos) {
        this.listaProductos = listaProductos;
    }

    public Integer getIdProductoSeleccionado() {
        return idProductoSeleccionado;
    }

    public void setIdProductoSeleccionado(Integer idProductoSeleccionado) {
        this.idProductoSeleccionado = idProductoSeleccionado;
    }

    public Producto getProductoDetalle() {
        return productoDetalle;
    }

    public void setProductoDetalle(Producto productoDetalle) {
        this.productoDetalle = productoDetalle;
    }

    public UploadedFile getArchivoFactura() {
        return archivoFactura;
    }

    public void setArchivoFactura(UploadedFile archivoFactura) {
        this.archivoFactura = archivoFactura;
    }

    public UploadedFile getImagenProducto() {
        return imagenProducto;
    }

    public void setImagenProducto(UploadedFile imagenProducto) {
        this.imagenProducto = imagenProducto;
    }

    public String getTipoMovimiento() {
        return tipoMovimiento;
    }

    public void setTipoMovimiento(String tipoMovimiento) {
        this.tipoMovimiento = tipoMovimiento;
    }

    public boolean isUsarFiltroTrimestre() {
        return usarFiltroTrimestre;
    }

    public void setUsarFiltroTrimestre(boolean usarFiltroTrimestre) {
        this.usarFiltroTrimestre = usarFiltroTrimestre;
    }

    public Date getFechaInicioFiltro() {
        return fechaInicioFiltro;
    }

    public void setFechaInicioFiltro(Date fechaInicioFiltro) {
        this.fechaInicioFiltro = fechaInicioFiltro;
    }

    public Date getFechaFinFiltro() {
        return fechaFinFiltro;
    }

    public void setFechaFinFiltro(Date fechaFinFiltro) {
        this.fechaFinFiltro = fechaFinFiltro;
    }

    public int getAñoFiltro() {
        return añoFiltro;
    }

    public void setAñoFiltro(int añoFiltro) {
        this.añoFiltro = añoFiltro;
    }

    public int getTrimestreFiltro() {
        return trimestreFiltro;
    }

    public void setTrimestreFiltro(int trimestreFiltro) {
        this.trimestreFiltro = trimestreFiltro;
    }

    public String getFiltroTipo() {
        return filtroTipo;
    }

    public void setFiltroTipo(String filtroTipo) {
        this.filtroTipo = filtroTipo;
    }

    public Integer getFiltroProductoId() {
        return filtroProductoId;
    }

    public void setFiltroProductoId(Integer filtroProductoId) {
        this.filtroProductoId = filtroProductoId;
    }

    public String getFiltroTipoEspecifico() {
        return filtroTipoEspecifico;
    }

    public void setFiltroTipoEspecifico(String filtroTipoEspecifico) {
        this.filtroTipoEspecifico = filtroTipoEspecifico;
    }

    public String getFiltroProveedorCliente() {
        return filtroProveedorCliente;
    }

    public void setFiltroProveedorCliente(String filtroProveedorCliente) {
        this.filtroProveedorCliente = filtroProveedorCliente;
    }

    public Date getFechaVencimientoProducto() {
        return fechaVencimientoProducto;
    }

    public void setFechaVencimientoProducto(Date fechaVencimientoProducto) {
        this.fechaVencimientoProducto = fechaVencimientoProducto;
    }

    public List<Integer> getMesesDisponibles() {
        return Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
    }

    public List<Integer> getDiasDisponibles() {
        List<Integer> dias = new ArrayList<>();
        for (int i = 1; i <= 31; i++) {
            dias.add(i);
        }
        return dias;
    }

    public String getFiltroCategoria() {
        return filtroCategoria;
    }

    public void setFiltroCategoria(String filtroCategoria) {
        this.filtroCategoria = filtroCategoria;
    }

    public Integer getMesFiltro() {
        return mesFiltro;
    }

    public void setMesFiltro(Integer mesFiltro) {
        this.mesFiltro = mesFiltro;
    }

    public Integer getDiaFiltro() {
        return diaFiltro;
    }

    public void setDiaFiltro(Integer diaFiltro) {
        this.diaFiltro = diaFiltro;
    }

    public Date getFechaEspecificaFiltro() {
        return fechaEspecificaFiltro;
    }

    public void setFechaEspecificaFiltro(Date fechaEspecificaFiltro) {
        this.fechaEspecificaFiltro = fechaEspecificaFiltro;
    }

}
