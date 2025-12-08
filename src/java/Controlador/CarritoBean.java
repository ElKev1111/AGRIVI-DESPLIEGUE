package Controlador;

import Modelo.Producto;
import Modelo.CarritoItem;
import DAO.ProductoDAO;
import DAO.VentasDAO;
import DAO.MovInventarioDAO;
import Modelo.EnumRoles;
import Modelo.MovInventario;
import Modelo.Ventas;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;

import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

@ManagedBean
@SessionScoped
public class CarritoBean implements Serializable {

    private static final float IVA_PORCENTAJE = 0.015f; // 1.5%

    private List<CarritoItem> items = new ArrayList<>();
    private Integer cantidadTemporal = 1;
    private Producto productoTemporal;
    private CarritoItem itemEdicion;

    private List<Ventas> ventasUltimoPago = new ArrayList<>();

    private transient ProductoDAO productoDAO = new ProductoDAO();
    private transient VentasDAO ventasDAO = new VentasDAO();
    private transient MovInventarioDAO movInventarioDAO = new MovInventarioDAO();
    private boolean simulacionPublicaInicializada;

    // === DATOS PARA SIMULACIÓN DE PAGO EN LÍNEA (TARJETA) ===
    private String nombreTitular;
    private String numeroTarjeta;
    private String fechaExpiracion;
    private String cvv;
    private String tipoTarjeta;

    // MEDIO DE PAGO (CRÉDITO / DÉBITO / PSE)
    private String medioPago; // CREDITO, DEBITO, PSE

    // Banco PSE
    private String bancoPse;

    // Datos específicos para PSE
    private String tipoPersona;   // NATURAL / JURIDICA
    private String tipoDocumento; // CC, NIT, CE...
    private String pseNumero;     // número de documento
    private String pseNombre;     // nombre del titular
    private String pseCorreo;     // correo electrónico

    // Datos del último pago (solo para mostrar info en la vista)
    private String codigoOperacionUltimoPago;
    private LocalDateTime fechaUltimoPago;

    // ====================== LÓGICA DE SIMULACIÓN PÚBLICA ======================
    public void prepararSimulacionPublica() {
        if (!usuarioAutenticado()
                && !FacesContext.getCurrentInstance().isPostback()
                && !simulacionPublicaInicializada) {
            items.clear();
            simulacionPublicaInicializada = true;
        }
    }

    // ====================== AGREGAR DESDE CATÁLOGO/MODAL ======================
    public void prepararAgregarProducto(Producto producto) {
        this.productoTemporal = producto;
        this.cantidadTemporal = 1;
        System.out.println("✅ Producto preparado: " + producto.getNombreProducto());
    }

    public void agregarProductoModal() {
        if (productoTemporal != null && cantidadTemporal != null && cantidadTemporal > 0) {
            agregarProductoConCantidad(productoTemporal, cantidadTemporal);
            System.out.println("✅ Producto agregado: " + productoTemporal.getNombreProducto()
                    + ", Cantidad: " + cantidadTemporal);
            productoTemporal = null;
        } else {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error",
                            "No se pudo agregar el producto al carrito."));
        }
    }

    public void agregarProductoConCantidad(Producto producto, int cantidad) {
        boolean encontrado = false;

        for (CarritoItem item : items) {
            if (item.getProducto().getIdProducto() == producto.getIdProducto()) {
                item.setCantidad(item.getCantidad() + cantidad);
                encontrado = true;
                break;
            }
        }

        if (!encontrado) {
            items.add(new CarritoItem(producto, cantidad));
        }

        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "¡Producto Añadido!",
                        cantidad + " unidad(es) de " + producto.getNombreProducto() + " agregado al carrito."));
    }

    // ====================== ELIMINAR ITEM ======================
    public void eliminarItem(CarritoItem item) {
        String nombreProducto = item.getProducto().getNombreProducto();
        items.remove(item);

        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_WARN, "Item Eliminado",
                        nombreProducto + " eliminado del carrito."));
    }

    // ====================== EDICIÓN DE CANTIDAD EN MODAL ======================
    public void prepararEditarCantidad(CarritoItem item) {
        this.itemEdicion = item;
        this.cantidadTemporal = item.getCantidad();
        System.out.println("📝 Preparando edición: " + item.getProducto().getNombreProducto()
                + ", Cantidad actual: " + item.getCantidad());
    }

    public void actualizarCantidad() {
        if (itemEdicion == null || cantidadTemporal == null || cantidadTemporal <= 0) {
            return;
        }

        int cantidadAnterior = itemEdicion.getCantidad();

        for (CarritoItem it : items) {
            if (it.getProducto().getIdProducto() == itemEdicion.getProducto().getIdProducto()) {
                it.setCantidad(cantidadTemporal);
                break;
            }
        }

        itemEdicion.setCantidad(cantidadTemporal);

        System.out.println("✔ Cantidad actualizada: " + itemEdicion.getProducto().getNombreProducto()
                + " - Anterior: " + cantidadAnterior
                + ", Nueva: " + cantidadTemporal);

        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Cantidad actualizada",
                        "La cantidad de " + itemEdicion.getProducto().getNombreProducto()
                        + " se actualizó a " + cantidadTemporal + " unidades."));

        itemEdicion = null;
    }

    // ====================== MÉTODOS RÁPIDOS PARA CATÁLOGO ======================
    public void agregarUnidad(Producto p) {
        try {
            this.prepararAgregarProducto(p);
            this.cantidadTemporal = 1;
            this.agregarProductoModal();
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error",
                            "No se pudo agregar el producto al carrito."));
        }
    }

    // === MÉTODOS PARA + / - EN carrito.xhtml SOBRE CarritoItem ===
    public void incrementarItem(CarritoItem item) {
        int nuevaCantidad = item.getCantidad() + 1;
        this.cantidadTemporal = nuevaCantidad;
        this.itemEdicion = item;
        this.actualizarCantidad();
    }

    public void decrementarItem(CarritoItem item) {
        if (item.getCantidad() <= 1) {
            return;
        }
        int nuevaCantidad = item.getCantidad() - 1;
        this.cantidadTemporal = nuevaCantidad;
        this.itemEdicion = item;
        this.actualizarCantidad();
    }

    // === MÉTODOS PARA + / - DESDE EL CATÁLOGO (dashboardCliente.xhtml) ===
    public int obtenerCantidadProducto(Producto p) {
        if (p == null) {
            return 0;
        }
        for (CarritoItem item : items) {
            if (item.getProducto() != null
                    && item.getProducto().getIdProducto() == p.getIdProducto()) {
                return item.getCantidad();
            }
        }
        return 0;
    }

    public void incrementarProducto(Producto p) {
        if (p == null) {
            return;
        }
        agregarProductoConCantidad(p, 1);
    }

    public void decrementarProducto(Producto p) {
        if (p == null) {
            return;
        }

        CarritoItem itemAEliminar = null;

        for (CarritoItem item : items) {
            if (item.getProducto() != null
                    && item.getProducto().getIdProducto() == p.getIdProducto()) {

                int nuevaCantidad = item.getCantidad() - 1;

                if (nuevaCantidad <= 0) {
                    itemAEliminar = item;
                } else {
                    item.setCantidad(nuevaCantidad);
                }
                break;
            }
        }

        if (itemAEliminar != null) {
            items.remove(itemAEliminar);
        }
    }

    // ====================== LÓGICA DE USUARIO ======================
    private UsuarioBean obtenerUsuarioBean() {
        return FacesContext.getCurrentInstance().getApplication()
                .evaluateExpressionGet(FacesContext.getCurrentInstance(), "#{usuarioBean}", UsuarioBean.class);
    }

    private boolean usuarioAutenticado() {
        UsuarioBean usuarioBean = obtenerUsuarioBean();
        return usuarioBean != null
                && usuarioBean.isAutenticado()
                && usuarioBean.getUsuario().getRol() == EnumRoles.CLIENTE;
    }

    // === NUEVO: obtener CorreoBean para enviar factura automática ===
    private CorreoBean obtenerCorreoBean() {
        return FacesContext.getCurrentInstance().getApplication()
                .evaluateExpressionGet(FacesContext.getCurrentInstance(), "#{correoBean}", CorreoBean.class);
    }

    private void redirigirALogin() {
        try {
            FacesContext.getCurrentInstance().getExternalContext().redirect("login.xhtml");
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Sesión requerida",
                            "Inicia sesión para continuar."));
        }
    }

    // ====================== PAGO / VALIDACIÓN ======================
    public void procederAlPago() {
        if (isVacio()) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Carrito vacío",
                            "No puedes proceder al pago con el carrito vacío."));
            return;
        }

        if (!usuarioAutenticado()) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Inicia sesión",
                            "Debes iniciar sesión como cliente para completar el pago."));
            redirigirALogin();
            return;
        }

        if (!validarDatosTarjeta()) {
            return;
        }

        if (FacesContext.getCurrentInstance().isValidationFailed()) {
            return;
        }

        try {
            procesarPago();

            this.codigoOperacionUltimoPago = "AGV-" + System.currentTimeMillis();
            this.fechaUltimoPago = LocalDateTime.now();

            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                            "Pago exitoso",
                            "Tu pago se procesó correctamente. Código de operación: " + codigoOperacionUltimoPago));

            limpiarDatosTarjeta();

        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Error en pago",
                            "No se pudo procesar el pago: " + e.getMessage()));
        }
    }

    private void limpiarDatosTarjeta() {
        // Datos de tarjeta
        nombreTitular = null;
        numeroTarjeta = null;
        fechaExpiracion = null;
        cvv = null;
        tipoTarjeta = null;

        // Medio de pago y PSE
        medioPago = null;
        bancoPse = null;
        tipoPersona = null;
        tipoDocumento = null;
        pseNumero = null;
        pseNombre = null;
        pseCorreo = null;

    }

    // ====================== PAGO / REGISTRO EN BD ======================
    private void procesarPago() {
        UsuarioBean usuarioBean = obtenerUsuarioBean();

        try {
            ventasUltimoPago = new ArrayList<>();
            double totalGeneral = 0;

            // ✅ PASO 4: generar un solo codigoCompra para TODO el checkout
            String codigoCompra = java.util.UUID.randomUUID().toString();
            System.out.println("[CARRITO] Iniciando checkout. codigoCompra=" + codigoCompra
                    + " | items=" + (items != null ? items.size() : 0));

            for (CarritoItem item : items) {
                Producto productoDB = productoDAO.buscar(item.getProducto().getIdProducto());
                item.setProducto(productoDB);

                if (productoDB == null) {
                    FacesContext.getCurrentInstance().addMessage(null,
                            new FacesMessage(FacesMessage.SEVERITY_ERROR, "Producto no encontrado",
                                    "No se pudo encontrar el producto en inventario."));
                    System.out.println("[CARRITO] ERROR: Producto no encontrado en BD.");
                    return;
                }

                if (productoDB.getStock() < item.getCantidad()) {
                    FacesContext.getCurrentInstance().addMessage(null,
                            new FacesMessage(FacesMessage.SEVERITY_WARN, "Stock insuficiente",
                                    "No hay stock suficiente de " + productoDB.getNombreProducto()));
                    System.out.println("[CARRITO] ERROR: Stock insuficiente para producto="
                            + productoDB.getNombreProducto()
                            + " | stock=" + productoDB.getStock()
                            + " | solicitado=" + item.getCantidad());
                    return;
                }

                int stockNuevo = productoDB.getStock() - item.getCantidad();
                productoDAO.actualizarStock(productoDB.getIdProducto(), stockNuevo);

                MovInventario mov = new MovInventario();
                mov.setTipoString("Salida");
                mov.setCantidadSalida(item.getCantidad());
                mov.setFechaSalida(new java.util.Date());
                mov.setTipoSalida("Venta");
                mov.setDescripcion("Venta de " + item.getCantidad() + " x " + productoDB.getNombreProducto());
                mov.setCliente(usuarioBean.getUsuario().getNombre());
                mov.setPrecioVenta((double) item.getProducto().getPrecioVenta());
                mov.setIdProducto(productoDB.getIdProducto());
                mov.setStockAnterior(productoDB.getStock());
                mov.setStockNuevo(stockNuevo);
                movInventarioDAO.registrarMovimiento(mov);

                Ventas venta = new Ventas();
                venta.setFechaVenta(LocalDateTime.now());
                venta.setProducto(productoDB);
                venta.setUsuario(usuarioBean.getUsuario());
                venta.setCantidad(item.getCantidad());
                // TotalPagar en BD se guarda sin IVA (solo subtotal)
                venta.setTotalPagar(item.getSubtotal());

                // ✅ PASO 4: asignar el MISMO codigoCompra a cada item comprado
                venta.setCodigoCompra(codigoCompra);

                System.out.println("[CARRITO] Item listo -> " + venta);

                ventasDAO.registrar(venta);

                ventasUltimoPago.add(venta);
                totalGeneral += venta.getTotalPagar();
            }

            // Datos adicionales para la factura por correo
            String direccionEnvio = null;
            if (usuarioBean != null && usuarioBean.getUsuario() != null) {
                direccionEnvio = usuarioBean.getUsuario().getDireccion();
            }

            String metodoPagoDesc;
            if ("PSE".equalsIgnoreCase(medioPago)) {
                metodoPagoDesc = "PSE - Banco " + (bancoPse != null ? bancoPse : "no especificado");
            } else if ("CREDITO".equalsIgnoreCase(medioPago) || "CRÉDITO".equalsIgnoreCase(medioPago)) {
                metodoPagoDesc = "Tarjeta de crédito" + (tipoTarjeta != null ? " (" + tipoTarjeta + ")" : "");
            } else if ("DEBITO".equalsIgnoreCase(medioPago) || "DÉBITO".equalsIgnoreCase(medioPago)) {
                metodoPagoDesc = "Tarjeta débito" + (tipoTarjeta != null ? " (" + tipoTarjeta + ")" : "");
            } else {
                metodoPagoDesc = "No especificado";
            }

            String tarjetaMasc = enmascararNumeroTarjeta(numeroTarjeta);

            items.clear();

            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Pago registrado",
                            "Se simuló el pago, se actualizó el inventario y se generó el comprobante."));

            System.out.println("[CARRITO] Checkout finalizado OK. codigoCompra=" + codigoCompra
                    + " | lineas=" + ventasUltimoPago.size()
                    + " | total(sin IVA)=" + totalGeneral);

            // === Enviar factura automática al correo del cliente con más detalle ===
            CorreoBean correoBean = obtenerCorreoBean();
            if (correoBean != null) {
                try {
                    correoBean.enviarFacturaVenta(ventasUltimoPago, direccionEnvio, metodoPagoDesc, tarjetaMasc);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error en pago",
                            "No se pudo procesar el pago: " + e.getMessage()));
            System.out.println("[CARRITO] ERROR GENERAL: " + e.getMessage());
        }
    }

    // ====================== COMPROBANTE PDF ======================
    public void descargarComprobantePdf() {
        FacesContext ctx = FacesContext.getCurrentInstance();

        try {
            if (ventasUltimoPago == null || ventasUltimoPago.isEmpty()) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN, "Sin comprobante",
                                "No hay un pago reciente para generar el comprobante."));
                return;
            }

            UsuarioBean usuarioBean = obtenerUsuarioBean();

            ExternalContext ec = ctx.getExternalContext();
            HttpServletResponse response = (HttpServletResponse) ec.getResponse();

            response.reset();
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=\"comprobante_pago.pdf\"");

            Document document = new Document();
            PdfWriter.getInstance(document, response.getOutputStream());

            document.open();

            document.add(new Paragraph("AgriviApp - Comprobante de Pago"));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Cliente: " + usuarioBean.getUsuario().getNombre()));
            document.add(new Paragraph("Correo: " + usuarioBean.getUsuario().getCorreo()));
            document.add(new Paragraph("Fecha: " + LocalDateTime.now().toString()));
            document.add(Chunk.NEWLINE);

            PdfPTable table = new PdfPTable(4);
            table.addCell("Producto");
            table.addCell("Cantidad");
            table.addCell("Precio unitario");
            table.addCell("Subtotal");

            double totalProductos = 0;
            for (Ventas v : ventasUltimoPago) {
                table.addCell(v.getProducto().getNombreProducto());
                table.addCell(String.valueOf(v.getCantidad()));
                table.addCell(String.format("%.2f", v.getProducto().getPrecioVenta()));
                table.addCell(String.format("%.2f", v.getTotalPagar()));
                totalProductos += v.getTotalPagar();
            }

            double totalIva = totalProductos * IVA_PORCENTAJE;
            double totalConIva = totalProductos + totalIva;

            document.add(table);
            document.add(Chunk.NEWLINE);
            document.add(new Paragraph("Total productos: $" + String.format("%.2f", totalProductos)));
            document.add(new Paragraph("IVA (1.5%): $" + String.format("%.2f", totalIva)));
            document.add(new Paragraph("Total pagado: $" + String.format("%.2f", totalConIva)));

            document.close();
            ctx.responseComplete();

        } catch (Exception e) {
            e.printStackTrace();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error",
                            "No se pudo generar el comprobante PDF: " + e.getMessage()));
        }
    }

    // ====================== VALIDACIÓN DE TARJETA / PSE ======================
    private boolean validarDatosTarjeta() {
        boolean valido = true;
        FacesContext ctx = FacesContext.getCurrentInstance();

        // Medio de pago obligatorio
        if (medioPago == null || medioPago.trim().isEmpty()) {
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_WARN,
                    "Dato requerido",
                    "Debes seleccionar un medio de pago."
            ));
            return false;
        }

        // === PSE ===
        if ("PSE".equals(medioPago)) {

            if (tipoPersona == null || tipoPersona.trim().isEmpty()) {
                ctx.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_WARN,
                        "Dato requerido",
                        "Debes seleccionar el tipo de persona para PSE."
                ));
                valido = false;
            }

            if (bancoPse == null || bancoPse.trim().isEmpty()) {
                ctx.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_WARN,
                        "Dato requerido",
                        "Debes seleccionar el banco para PSE."
                ));
                valido = false;
            }

            if (tipoDocumento == null || tipoDocumento.trim().isEmpty()) {
                ctx.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_WARN,
                        "Dato requerido",
                        "Debes seleccionar el tipo de documento para PSE."
                ));
                valido = false;
            }

            if (pseNumero == null || pseNumero.trim().isEmpty()) {
                ctx.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_WARN,
                        "Dato requerido",
                        "Debes ingresar el número de identificación para PSE."
                ));
                valido = false;
            }

            if (pseNombre == null || pseNombre.trim().isEmpty()) {
                ctx.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_WARN,
                        "Dato requerido",
                        "Debes ingresar el nombre del titular para PSE."
                ));
                valido = false;
            }

            if (pseCorreo == null || pseCorreo.trim().isEmpty()) {
                ctx.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_WARN,
                        "Dato requerido",
                        "Debes ingresar el correo electrónico para PSE."
                ));
                valido = false;
            }

            return valido;
        }

        // === TARJETA (CRÉDITO / DÉBITO) ===
        if (nombreTitular == null || nombreTitular.trim().isEmpty()) {
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_WARN,
                    "Dato requerido",
                    "El nombre del titular es obligatorio."
            ));
            valido = false;
        }

        if (numeroTarjeta == null || numeroTarjeta.trim().isEmpty()) {
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_WARN,
                    "Dato requerido",
                    "El número de tarjeta es obligatorio."
            ));
            valido = false;
        }

        if (fechaExpiracion == null || fechaExpiracion.trim().isEmpty()) {
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_WARN,
                    "Dato requerido",
                    "La fecha de expiración es obligatoria."
            ));
            valido = false;
        }

        if (cvv == null || cvv.trim().isEmpty()) {
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_WARN,
                    "Dato requerido",
                    "El CVV es obligatorio."
            ));
            valido = false;
        }

        if (tipoTarjeta == null || tipoTarjeta.trim().isEmpty()) {
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_WARN,
                    "Dato requerido",
                    "Selecciona un tipo de tarjeta."
            ));
            valido = false;
        }

        return valido;
    }

    // Enmascara el número de tarjeta dejando visibles solo los últimos 4 dígitos
    private String enmascararNumeroTarjeta(String numero) {
        if (numero == null) {
            return "No disponible";
        }
        String limpio = numero.replaceAll("\\s+", "");
        if (limpio.length() < 4) {
            return "No disponible";
        }
        String ultimos4 = limpio.substring(limpio.length() - 4);
        return "**** **** **** " + ultimos4;
    }

    // ====================== CÁLCULOS DE TOTALES ======================    
    public float getTotalCompra() {
        float total = 0;
        for (CarritoItem item : items) {
            total += item.getSubtotal();
        }
        return total;
    }

    public float getTotalProductos() {
        return getTotalCompra();
    }

    public float getTotalIva() {
        return getTotalProductos() * IVA_PORCENTAJE;
    }

    public float getSubtotalCompra() {
        return getTotalProductos() + getTotalIva();
    }

    // Totales del ÚLTIMO PAGO (para el resumen después de pagar)
    public double getUltimoTotalProductos() {
        double total = 0;
        if (ventasUltimoPago != null) {
            for (Ventas v : ventasUltimoPago) {
                total += v.getTotalPagar(); // subtotal sin IVA
            }
        }
        return total;
    }

    public double getUltimoTotalIva() {
        return getUltimoTotalProductos() * IVA_PORCENTAJE;
    }

    public double getUltimoSubtotalCompra() {
        return getUltimoTotalProductos() + getUltimoTotalIva();
    }

    public int getTotalItems() {
        return items.stream().mapToInt(CarritoItem::getCantidad).sum();
    }

    public boolean isVacio() {
        return items.isEmpty();
    }

    // ====================== GETTERS / SETTERS ======================    
    public List<CarritoItem> getItems() {
        return items;
    }

    public Integer getCantidadTemporal() {
        return cantidadTemporal;
    }

    public void setCantidadTemporal(Integer cantidadTemporal) {
        this.cantidadTemporal = cantidadTemporal;
    }

    public Producto getProductoTemporal() {
        return productoTemporal;
    }

    public void setProductoTemporal(Producto productoTemporal) {
        this.productoTemporal = productoTemporal;
    }

    public CarritoItem getItemEdicion() {
        return itemEdicion;
    }

    public void setItemEdicion(CarritoItem itemEdicion) {
        this.itemEdicion = itemEdicion;
    }

    public List<Ventas> getVentasUltimoPago() {
        return ventasUltimoPago;
    }

    public String getNombreTitular() {
        return nombreTitular;
    }

    public void setNombreTitular(String nombreTitular) {
        this.nombreTitular = nombreTitular;
    }

    public String getNumeroTarjeta() {
        return numeroTarjeta;
    }

    public void setNumeroTarjeta(String numeroTarjeta) {
        this.numeroTarjeta = numeroTarjeta;
    }

    public String getFechaExpiracion() {
        return fechaExpiracion;
    }

    public void setFechaExpiracion(String fechaExpiracion) {
        this.fechaExpiracion = fechaExpiracion;
    }

    public String getCvv() {
        return cvv;
    }

    public void setCvv(String cvv) {
        this.cvv = cvv;
    }

    public String getTipoTarjeta() {
        return tipoTarjeta;
    }

    public void setTipoTarjeta(String tipoTarjeta) {
        this.tipoTarjeta = tipoTarjeta;
    }

    public String getMedioPago() {
        return medioPago;
    }

    public void setMedioPago(String medioPago) {
        this.medioPago = medioPago;
    }

    public String getBancoPse() {
        return bancoPse;
    }

    public void setBancoPse(String bancoPse) {
        this.bancoPse = bancoPse;
    }

    public String getPseNumero() {
        return pseNumero;
    }

    public void setPseNumero(String pseNumero) {
        this.pseNumero = pseNumero;
    }

    public String getPseNombre() {
        return pseNombre;
    }

    public void setPseNombre(String pseNombre) {
        this.pseNombre = pseNombre;
    }

    public String getPseCorreo() {
        return pseCorreo;
    }

    public void setPseCorreo(String pseCorreo) {
        this.pseCorreo = pseCorreo;
    }

    public String getCodigoOperacionUltimoPago() {
        return codigoOperacionUltimoPago;
    }

    public LocalDateTime getFechaUltimoPago() {
        return fechaUltimoPago;
    }

    public String getTipoPersona() {
        return tipoPersona;
    }

    public void setTipoPersona(String tipoPersona) {
        this.tipoPersona = tipoPersona;
    }

    public String getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(String tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }
}
