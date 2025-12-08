package Controlador;

import DAO.VentasDAO;
import Modelo.Ventas;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

@ManagedBean(name = "historialComprasBean")
@ViewScoped
public class HistorialComprasBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<Ventas> compras = new ArrayList<>();
    private transient VentasDAO ventasDAO = new VentasDAO();

    @PostConstruct
    public void init() {
        cargarHistorial();
    }

    public void cargarHistorial() {
        try {
            UsuarioBean usuarioBean = FacesContext.getCurrentInstance().getApplication()
                    .evaluateExpressionGet(FacesContext.getCurrentInstance(), "#{usuarioBean}", UsuarioBean.class);

            if (usuarioBean != null && usuarioBean.isAutenticado()) {
                compras = ventasDAO.listarPorUsuario(usuarioBean.getUsuario().getId());
            } else {
                compras = new ArrayList<>();
            }
        } catch (Exception e) {
            compras = new ArrayList<>();
        }
    }

    public List<Ventas> getCompras() {
        return compras;
    }
}
