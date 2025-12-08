package Controlador;

import DAO.VentasDAO;
import Modelo.Ventas;
import Modelo.VentaAgrupadaDTO;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

/**
 * Bean de Ventas.
 *
 * 1) LEGACY: listaVentas + ventaSeleccionada - Mantenerlo evita romper
 * pantallas anteriores.
 *
 * 2) NUEVO ADMIN AGRUPADO: - listaVentasAgrupadas muestra una fila por compra.
 * - ventaAgrupadaSeleccionada / compraSeleccionada apuntan a lo mismo para
 * compatibilidad con diferentes versiones del XHTML.
 *
 * 3) Manejo de fallback: - Si el codigoGrupo es "LEG-idVenta", trae el detalle
 * por idVenta.
 */
@ManagedBean(name = "ventasBean")
@ViewScoped
public class VentasBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final float IVA_PORCENTAJE = 0.015f; // 1.5 %

    private VentasDAO ventasDAO = new VentasDAO();

    // ================== LEGACY ==================
    private List<Ventas> listaVentas;
    private Ventas ventaSeleccionada;

    // ================== NUEVO AGRUPADO ADMIN ==================
    private List<VentaAgrupadaDTO> listaVentasAgrupadas;

    // Unificamos selección para evitar PropertyNotFoundException
    private VentaAgrupadaDTO ventaAgrupadaSeleccionada;  // nombre usado en algunas vistas
    private List<Ventas> detallesVenta = new ArrayList<>();

    public VentasBean() {
        // Mantener compatibilidad
        cargarVentas();

        // Nuevo admin agrupado
        cargarVentasAgrupadas();
    }

    // ------------------ LEGACY ------------------
    public void cargarVentas() {
        listaVentas = ventasDAO.listar();
        System.out.println("[VENTAS-BEAN] Ventas cargadas (legacy): "
                + (listaVentas != null ? listaVentas.size() : 0));
    }

    public void seleccionarVenta(Ventas v) {
        this.ventaSeleccionada = v;
    }

    public float getTotalProductosSeleccionado() {
        if (ventaSeleccionada == null) {
            return 0f;
        }
        return ventaSeleccionada.getTotalPagar();
    }

    public float getTotalIvaSeleccionado() {
        return getTotalProductosSeleccionado() * IVA_PORCENTAJE;
    }

    public float getTotalConIvaSeleccionado() {
        return getTotalProductosSeleccionado() + getTotalIvaSeleccionado();
    }

    // ------------------ AGRUPADO ADMIN ------------------
    public void cargarVentasAgrupadas() {
        listaVentasAgrupadas = ventasDAO.listarAgrupadoAdmin();
        System.out.println("[VENTAS-BEAN] Compras agrupadas cargadas: "
                + (listaVentasAgrupadas != null ? listaVentasAgrupadas.size() : 0));
    }

    /**
     * Método “principal” recomendado para el botón del admin.
     */
    public void seleccionarVentaAgrupada(VentaAgrupadaDTO dto) {
        seleccionarCompra(dto); // delega para mantener dos nombres válidos
    }

    /**
     * Alias de compatibilidad (si en tu XHTML quedó como seleccionarCompra).
     */
    public void seleccionarCompra(VentaAgrupadaDTO dto) {
        this.ventaAgrupadaSeleccionada = dto;
        this.detallesVenta = new ArrayList<>();

        if (dto == null || dto.getCodigoGrupo() == null) {
            return;
        }

        String codigo = dto.getCodigoGrupo();

        // Compras nuevas con codigoCompra real
        if (!codigo.startsWith("LEG-")) {
            detallesVenta = ventasDAO.listarDetallesPorCodigo(codigo);
            return;
        }

        // Compras viejas sin codigoCompra (fallback)
        try {
            int idVenta = Integer.parseInt(codigo.substring(4));
            Ventas unica = ventasDAO.buscarDetallePorIdVenta(idVenta);
            if (unica != null) {
                detallesVenta.add(unica);
            }
        } catch (Exception e) {
            System.out.println("[VENTAS-BEAN] Error parseando LEG-: " + e.getMessage());
        }
    }

    // IVA para compra agrupada seleccionada
    public float getSubtotalCompraSeleccionada() {
        return ventaAgrupadaSeleccionada != null ? ventaAgrupadaSeleccionada.getTotalPagar() : 0f;
    }

    public float getIvaCompraSeleccionada() {
        return getSubtotalCompraSeleccionada() * IVA_PORCENTAJE;
    }

    public float getTotalConIvaCompraSeleccionada() {
        return getSubtotalCompraSeleccionada() + getIvaCompraSeleccionada();
    }

    // ================== GETTERS/SETTERS ==================
    public List<Ventas> getListaVentas() {
        return listaVentas;
    }

    public Ventas getVentaSeleccionada() {
        return ventaSeleccionada;
    }

    public List<VentaAgrupadaDTO> getListaVentasAgrupadas() {
        return listaVentasAgrupadas;
    }

    // --- Compatibilidad de nombres en EL ---
    public VentaAgrupadaDTO getVentaAgrupadaSeleccionada() {
        return ventaAgrupadaSeleccionada;
    }

    public void setVentaAgrupadaSeleccionada(VentaAgrupadaDTO ventaAgrupadaSeleccionada) {
        this.ventaAgrupadaSeleccionada = ventaAgrupadaSeleccionada;
    }

    // Alias por si tu XHTML usa "compraSeleccionada"
    public VentaAgrupadaDTO getCompraSeleccionada() {
        return ventaAgrupadaSeleccionada;
    }

    public List<Ventas> getDetallesVenta() {
        return detallesVenta;
    }

    // Alias por si tu XHTML usa "detallesCompraSeleccionada"
    public List<Ventas> getDetallesCompraSeleccionada() {
        return detallesVenta;
    }
}
