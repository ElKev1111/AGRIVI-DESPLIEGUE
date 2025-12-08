package Controlador;

import java.io.Serializable;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

@ManagedBean // Nombre implícito: pagoBean
@ViewScoped
public class PagoBean implements Serializable {

    /**
     * Stub para el método de inicio de pago.
     */
    public String iniciarPago() {
        // Por ahora, solo redirige. La lógica compleja irá aquí después.
        return "pago?faces-redirect=true";
    }
}