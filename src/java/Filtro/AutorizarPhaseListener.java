package Filtro;

import Controlador.UsuarioBean;
import Modelo.EnumRoles;

import java.io.IOException;
import javax.faces.application.ViewExpiredException;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import javax.servlet.http.HttpServletResponse;

public class AutorizarPhaseListener implements PhaseListener {

    @Override
    public PhaseId getPhaseId() {
        // RESTORE_VIEW es temprano y funciona bien para bloquear navegación por URL
        return PhaseId.RESTORE_VIEW;
    }

    @Override
    public void beforePhase(PhaseEvent event) {
        // no usado
    }

    @Override
    public void afterPhase(PhaseEvent event) {
        FacesContext fc = event.getFacesContext();

        if (fc.getViewRoot() == null) return;

        String viewId = fc.getViewRoot().getViewId(); // Ej: /HomeAdmin5.xhtml

        // Permitir páginas públicas y también las páginas de error
        if (esVistaPublica(viewId)) return;

        UsuarioBean ub = fc.getApplication()
                .evaluateExpressionGet(fc, "#{usuarioBean}", UsuarioBean.class);

        // No autenticado -> login
        if (ub == null || !ub.isAutenticado() || ub.getUsuario() == null || ub.getUsuario().getRol() == null) {
            redirigir(fc, "login.xhtml");
            return;
        }

        EnumRoles rol = ub.getUsuario().getRol();

        boolean esAdminView = esVistaAdmin(viewId);
        boolean esClienteView = esVistaCliente(viewId);

        // ADMINISTRADOR -> TODO
        if (rol == EnumRoles.ADMINISTRADOR) {
            return;
        }

        // CLIENTE -> solo módulo cliente
        if (rol == EnumRoles.CLIENTE) {
            if (esAdminView) {
                enviar403(fc);
            }
            return;
        }

        // EMPLEADO -> admin sí, EXCEPTO HomeAdmin5
        if (rol == EnumRoles.EMPLEADO) {
            if (viewId.endsWith("HomeAdmin5.xhtml")) {
                enviar403(fc);
                return;
            }
            // Si intenta entrar al módulo cliente (si deseas bloquearlo)
            // Si en tu lógica el empleado puede ver cliente, elimina este bloque
            if (esClienteView && !esAdminView) {
                enviar403(fc);
            }
        }
    }

    private boolean esVistaAdmin(String viewId) {
        if (viewId == null) return false;
        String v = viewId.toLowerCase();
        // Basado en tu convención real: HomeAdmin*.xhtml
        return v.contains("homeadmin");
    }

    private boolean esVistaCliente(String viewId) {
        if (viewId == null) return false;
        String v = viewId.toLowerCase();
        // Ajusta si tus vistas cliente usan otra convención
        return v.contains("cliente")
                || v.contains("dashboardcliente")
                || v.contains("perfilcliente");
    }

    private boolean esVistaPublica(String viewId) {
        if (viewId == null) return true;

        String v = viewId.toLowerCase();
        return v.contains("login.xhtml")
                || v.contains("register.xhtml")
                || v.contains("dashboardpresentacion.xhtml")
                || v.contains("recuperarcontrasena.xhtml")
                || v.contains("error403.xhtml")
                || v.contains("error404.xhtml")
                || v.contains("error500.xhtml");
    }

    private void redirigir(FacesContext fc, String destino) {
        try {
            fc.getExternalContext().redirect(destino);
            fc.responseComplete();
        } catch (IOException e) {
            // fallback a 500 si falla redirect
            enviar500(fc);
        }
    }

    private void enviar403(FacesContext fc) {
        try {
            HttpServletResponse resp = (HttpServletResponse) fc.getExternalContext().getResponse();
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            fc.responseComplete();
        } catch (IOException e) {
            enviar500(fc);
        }
    }

    private void enviar500(FacesContext fc) {
        try {
            HttpServletResponse resp = (HttpServletResponse) fc.getExternalContext().getResponse();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            fc.responseComplete();
        } catch (IOException ex) {
            // ya no hay mucho más que hacer
        }
    }
}