package Controlador;

import Modelo.Usuario;
import Modelo.Ventas;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import org.primefaces.model.file.UploadedFile;
import java.io.Serializable;
import java.sql.Connection;
import javax.mail.Multipart;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

@ManagedBean(name = "correoBean")
@ViewScoped
public class CorreoBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private String asunto;
    private String contmensaje;
    private List<String> dest;
    private List<Usuario> listaUsr;

    // Archivo adjunto opcional
    private transient UploadedFile archivoAdjunto;

    // IVA usado para el cálculo del total en el correo de factura (1.5%)
    private static final double IVA_PORCENTAJE = 0.015;

    public CorreoBean() {
        dest = new ArrayList<>();
        listaUsr = new ArrayList<>();
    }

    // ---------------- LÓGICA DE USUARIOS ----------------
    public void listarUsuarios() {
        listaUsr = new ArrayList<>();
        String sql = "SELECT correo, nombre FROM usuario WHERE estado = 'ACTIVO'";

        Connection con = null;
        try {
            con = Conexion.conectar();
            if (con == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "BD", "No se pudo conectar a la base de datos."));
                return;
            }

            try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    Usuario u = new Usuario();
                    u.setCorreo(rs.getString("correo"));
                    u.setNombre(rs.getString("nombre"));
                    listaUsr.add(u);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "No se pudieron cargar usuarios."));
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    // ---------------- ENVÍO DE CORREO GENERAL ----------------
    public void enviarCorreo() {
        FacesContext fc = FacesContext.getCurrentInstance();

        try {
            if (dest == null || dest.isEmpty()) {
                fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Atención", "Selecciona destinatarios."));
                return;
            }
            if (asunto == null || asunto.trim().isEmpty()) {
                fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Atención", "El asunto es obligatorio."));
                return;
            }

            // Copia segura (evita listas inmutables)
            List<String> destinatarios = new ArrayList<>(dest);

            Session sesion = MailSessionFactory.createSession();
            if (sesion == null) {
                fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Mail", "No se pudo crear la sesión de correo."));
                return;
            }

            int enviados = 0;
            int invalidos = 0;

            for (String correoDestino : destinatarios) {

                // Validación básica
                if (correoDestino == null || correoDestino.trim().isEmpty()) {
                    invalidos++;
                    continue;
                }

                // Validación estricta del formato de email (para saltar “correos fake”)
                try {
                    InternetAddress ia = new InternetAddress(correoDestino, true);
                    ia.validate();
                } catch (AddressException ex) {
                    invalidos++;
                    continue;
                }

                try {
                    Message mensaje = new MimeMessage(sesion);
                    mensaje.setFrom(new InternetAddress(MailSessionFactory.getFrom()));

                    mensaje.setRecipients(Message.RecipientType.TO, InternetAddress.parse(correoDestino));
                    mensaje.setSubject(asunto);

                    // Contenido HTML o texto
                    MimeBodyPart cuerpo = new MimeBodyPart();
                    cuerpo.setContent(contmensaje, "text/html; charset=UTF-8");

                    Multipart multipart = new MimeMultipart();
                    multipart.addBodyPart(cuerpo);

                    if (archivoAdjunto != null && archivoAdjunto.getFileName() != null) {
                        File temp = File.createTempFile("adjunto_", "_" + archivoAdjunto.getFileName());
                        temp.deleteOnExit(); // evita basura en disco

                        try (InputStream in = archivoAdjunto.getInputStream(); FileOutputStream out = new FileOutputStream(temp)) {
                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            while ((bytesRead = in.read(buffer)) != -1) {
                                out.write(buffer, 0, bytesRead);
                            }
                        }

                        MimeBodyPart adjunto = new MimeBodyPart();
                        adjunto.attachFile(temp);
                        multipart.addBodyPart(adjunto);
                    }

                    mensaje.setContent(multipart);
                    Transport.send(mensaje);
                    enviados++;

                } catch (Exception mailEx) {
                    // IMPORTANTE: NO romper el envío masivo por 1 correo malo
                    mailEx.printStackTrace();
                }
            }

            fc.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_INFO,
                    "Correos",
                    "Enviados: " + enviados + " | Saltados inválidos: " + invalidos
            ));

            // Limpieza segura (no uses dest.clear())
            // Limpiar campos (seguro)
            asunto = "";
            contmensaje = "";
            dest = new ArrayList<>();
            archivoAdjunto = null;

        } catch (Exception e) {
            // Esto evita el Error500 post-envío
            e.printStackTrace();
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Falló el envío masivo: " + e.getMessage()));
        }
    }

    // ---------------- HTML DEL CORREO GENERAL ----------------
    private String construirHtmlCorreo(String nombreDestinatario) {
        String titulo = (asunto != null && !asunto.trim().isEmpty())
                ? asunto.trim()
                : "Notificación Agrivi";

        String mensajeTexto = (contmensaje != null) ? contmensaje.trim() : "";
        String mensajeHtml = mensajeTexto.replace("\n", "<br/>");

        String saludo;
        if (nombreDestinatario != null && !nombreDestinatario.trim().isEmpty()) {
            saludo = "Hola, " + escapeHtml(nombreDestinatario) + ",";
        } else {
            saludo = "Hola,";
        }

        return "<!DOCTYPE html>"
                + "<html lang='es'>"
                + "<head>"
                + "  <meta charset='UTF-8'/>"
                + "  <title>" + escapeHtml(titulo) + "</title>"
                + "</head>"
                + "<body style='margin:0;padding:0;background-color:#f4f6f9;font-family:Arial,sans-serif;'>"
                + "  <table width='100%' cellpadding='0' cellspacing='0' style='background-color:#f4f6f9;padding:20px 0;'>"
                + "    <tr>"
                + "      <td align='center'>"
                + "        <table width='600' cellpadding='0' cellspacing='0' "
                + "               style='background-color:#ffffff;border-radius:12px;overflow:hidden;"
                + "                      box-shadow:0 6px 18px rgba(0,0,0,0.08);'>"
                + "          <tr>"
                + "            <td style='background-color:#008631;padding:16px 24px;color:#ffffff;'>"
                + "              <table width='100%'><tr>"
                + "                <td style='font-size:20px;font-weight:bold;'>🍃 Agrivi</td>"
                + "                <td align='right' style='font-size:12px;'>Panel Admin</td>"
                + "              </tr></table>"
                + "            </td>"
                + "          </tr>"
                + "          <tr>"
                + "            <td style='padding:24px 24px 8px 24px;'>"
                + "              <h1 style='margin:0;font-size:20px;color:#111827;'>" + escapeHtml(titulo) + "</h1>"
                + "            </td>"
                + "          </tr>"
                + "          <tr>"
                + "            <td style='padding:8px 24px 16px 24px;font-size:14px;color:#4b5563;line-height:1.6;'>"
                + "              <p style='margin-top:0;'>" + saludo + "</p>"
                + "              <p style='margin:0;'>" + mensajeHtml + "</p>"
                + "            </td>"
                + "          </tr>"
                + "          <tr>"
                + "            <td style='padding:0 24px 20px 24px;'>"
                + "              <table width='100%' cellpadding='0' cellspacing='0' "
                + "                     style='background-color:#f9fafb;border-radius:10px;border:1px solid #e5e7eb;'>"
                + "                <tr>"
                + "                  <td style='padding:12px 16px;font-size:12px;color:#374151;'>"
                + "                    <p style='margin:0 0 4px 0;'><strong>Este mensaje fue enviado desde Agrivi.</strong></p>"
                + "                    <p style='margin:0;'>Si no reconoces este correo, puedes ignorarlo.</p>"
                + "                  </td>"
                + "                </tr>"
                + "              </table>"
                + "            </td>"
                + "          </tr>"
                + "          <tr>"
                + "            <td style='background-color:#f9fafb;padding:12px 24px;font-size:11px;color:#9ca3af;'>"
                + "              © " + java.time.Year.now() + " Agrivi. Todos los derechos reservados."
                + "            </td>"
                + "          </tr>"
                + "        </table>"
                + "      </td>"
                + "    </tr>"
                + "  </table>"
                + "</body>"
                + "</html>";
    }

    // ---------------- ADJUNTO ----------------
    private MimeBodyPart crearParteAdjunto(UploadedFile file) throws IOException, MessagingException {
        if (file == null || file.getSize() <= 0) {
            return null;
        }

        MimeBodyPart adjunto = new MimeBodyPart();

        File temp = File.createTempFile("agrivi_adj_", "_" + file.getFileName());
        try (InputStream is = file.getInputStream(); FileOutputStream os = new FileOutputStream(temp)) {

            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
        }

        adjunto.attachFile(temp);
        adjunto.setFileName(file.getFileName());
        return adjunto;
    }

    // ---------------- UTIL ----------------
    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    /**
     * Busca el nombre del usuario por su correo en la lista de usuarios
     * cargada.
     */
    private String obtenerNombrePorCorreo(String correo) {
        if (correo == null || listaUsr == null) {
            return null;
        }
        for (Usuario u : listaUsr) {
            if (u != null && u.getCorreo() != null
                    && u.getCorreo().equalsIgnoreCase(correo)) {
                return u.getNombre();
            }
        }
        return null;
    }

    // ---------------- CORREO AUTOMÁTICO DE FACTURA ----------------
    /**
     * Versión antigua: se mantiene para compatibilidad, pero ahora llama a la
     * nueva con datos extra nulos.
     */
    public void enviarFacturaVenta(List<Ventas> ventasUltimoPago) {
        enviarFacturaVenta(ventasUltimoPago, null, null, null);
    }

    /**
     * Nueva versión: recibe además dirección de envío, método de pago y tarjeta
     * enmascarada. Es la que llama CarritoBean.
     */
    /**
     * Nueva versión: recibe además dirección de envío, método de pago y tarjeta
     * enmascarada. Es la que llama CarritoBean.
     */
    public void enviarFacturaVenta(List<Ventas> ventasUltimoPago,
            String direccionEnvio,
            String metodoPago,
            String tarjetaMasc) {
        if (ventasUltimoPago == null || ventasUltimoPago.isEmpty()) {
            return;
        }

        // Tomamos la primera venta para obtener datos del cliente
        Ventas v0 = ventasUltimoPago.get(0);
        if (v0 == null || v0.getUsuario() == null) {
            return;
        }

        String correoDestino = v0.getUsuario().getCorreo();
        String nombreCliente = v0.getUsuario().getNombre();

        if (correoDestino == null || correoDestino.trim().isEmpty()) {
            return;
        }

        // --------------------------------------------------------------------
        // ✅ ELIMINADO (hardcode):
        // final String user = "kalvinalonzo@gmail.com";
        // final String pass = "fjre wsvh lsbn exyw";
        //
        // ✅ ELIMINADO (Gmail fijo):
        // Properties props = new Properties();
        // props.put("mail.smtp.auth", "true");
        // props.put("mail.smtp.starttls.enable", "true");
        // props.put("mail.smtp.host", "smtp.gmail.com");
        // props.put("mail.smtp.port", "587");
        // props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
        //
        // ✅ ELIMINADO:
        // Session sesion = Session.getInstance(props, new Authenticator() {
        //     @Override
        //     protected PasswordAuthentication getPasswordAuthentication() {
        //         return new PasswordAuthentication(user, pass);
        //     }
        // });
        //
        // ✅ REEMPLAZADO POR:
        // Sesión dinámica por variables de entorno (local/prod)
        // --------------------------------------------------------------------
        Session sesion = MailSessionFactory.createSession();
        String from = MailSessionFactory.getFrom();

        try {
            MimeMessage mensaje = new MimeMessage(sesion);
            mensaje.setFrom(new InternetAddress(from));
            mensaje.setRecipient(Message.RecipientType.TO, new InternetAddress(correoDestino));
            mensaje.setSubject("Resumen de tu compra en Agrivi", "UTF-8");

            String html = construirHtmlFactura(ventasUltimoPago, nombreCliente,
                    direccionEnvio, metodoPago, tarjetaMasc);

            MimeBodyPart cuerpoHtml = new MimeBodyPart();
            cuerpoHtml.setContent(html, "text/html; charset=UTF-8");

            MimeMultipart multipart = new MimeMultipart("mixed");
            multipart.addBodyPart(cuerpoHtml);

            mensaje.setContent(multipart);

            Transport.send(mensaje);

        } catch (MessagingException e) {
            e.printStackTrace();
            // No mostramos mensaje en pantalla porque esto se ejecuta en el flujo del pago.
        }
    }

    /**
     * Construye el HTML de la factura/resumen de compra con datos adicionales.
     */
    private String construirHtmlFactura(List<Ventas> ventas,
            String nombreCliente,
            String direccionEnvio,
            String metodoPago,
            String tarjetaMasc) {
        String saludo;
        if (nombreCliente != null && !nombreCliente.trim().isEmpty()) {
            saludo = "Hola, " + escapeHtml(nombreCliente) + ",";
        } else {
            saludo = "Hola,";
        }

        // Tabla de productos
        StringBuilder tabla = new StringBuilder();
        tabla.append("<table width='100%' cellpadding='0' cellspacing='0' style='border-collapse:collapse;margin-top:10px;'>")
                .append("<tr>")
                .append("<th style='padding:8px;background:#f3f4f6;border-bottom:1px solid #e5e7eb;text-align:left;font-size:13px;'>Producto</th>")
                .append("<th style='padding:8px;background:#f3f4f6;border-bottom:1px solid #e5e7eb;text-align:center;font-size:13px;'>Cantidad</th>")
                .append("<th style='padding:8px;background:#f3f4f6;border-bottom:1px solid #e5e7eb;text-align:right;font-size:13px;'>Subtotal</th>")
                .append("</tr>");

        double totalProductos = 0;

        for (Ventas v : ventas) {
            if (v == null || v.getProducto() == null) {
                continue;
            }
            String nombreProd = v.getProducto().getNombreProducto();
            int cantidad = v.getCantidad();
            double subtotal = v.getTotalPagar();

            totalProductos += subtotal;

            tabla.append("<tr>")
                    .append("<td style='padding:8px;border-bottom:1px solid #f3f4f6;font-size:13px;'>")
                    .append(escapeHtml(nombreProd))
                    .append("</td>")
                    .append("<td style='padding:8px;border-bottom:1px solid #f3f4f6;text-align:center;font-size:13px;'>")
                    .append(cantidad)
                    .append("</td>")
                    .append("<td style='padding:8px;border-bottom:1px solid #f3f4f6;text-align:right;font-size:13px;'>$")
                    .append(String.format("%,.2f", subtotal))
                    .append("</td>")
                    .append("</tr>");
        }

        tabla.append("</table>");

        double totalIva = totalProductos * IVA_PORCENTAJE;
        double totalConIva = totalProductos + totalIva;

        String dir = (direccionEnvio != null && !direccionEnvio.trim().isEmpty())
                ? escapeHtml(direccionEnvio)
                : "No disponible";

        String mpago = (metodoPago != null && !metodoPago.trim().isEmpty())
                ? escapeHtml(metodoPago)
                : "No disponible";

        String tarjeta = (tarjetaMasc != null && !tarjetaMasc.trim().isEmpty())
                ? escapeHtml(tarjetaMasc)
                : "No aplica";

        return "<!DOCTYPE html>"
                + "<html lang='es'>"
                + "<head>"
                + "  <meta charset='UTF-8'/>"
                + "  <title>Resumen de tu compra</title>"
                + "</head>"
                + "<body style='margin:0;padding:0;background-color:#f4f6f9;font-family:Arial,sans-serif;'>"
                + "  <table width='100%' cellpadding='0' cellspacing='0' style='background-color:#f4f6f9;padding:20px 0;'>"
                + "    <tr>"
                + "      <td align='center'>"
                + "        <table width='600' cellpadding='0' cellspacing='0' "
                + "               style='background-color:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 6px 18px rgba(0,0,0,0.08);'>"
                + "          <tr>"
                + "            <td style='background-color:#008631;padding:16px 24px;color:#ffffff;'>"
                + "              <table width='100%'><tr>"
                + "                <td style='font-size:20px;font-weight:bold;'>🍃 Agrivi</td>"
                + "                <td align='right' style='font-size:12px;'>Panel Cliente</td>"
                + "              </tr></table>"
                + "            </td>"
                + "          </tr>"
                + "          <tr>"
                + "            <td style='padding:24px 24px 8px 24px;'>"
                + "              <h1 style='margin:0;font-size:20px;color:#111827;'>Resumen de tu compra</h1>"
                + "            </td>"
                + "          </tr>"
                + "          <tr>"
                + "            <td style='padding:8px 24px 16px 24px;font-size:14px;color:#4b5563;line-height:1.6;'>"
                + "              <p style='margin-top:0;'>" + saludo + "</p>"
                + "              <p style='margin:0;'>Hemos registrado tu pedido correctamente. A continuación encontrarás el detalle de tu compra.</p>"
                + "            </td>"
                + "          </tr>"
                // Tabla de productos
                + "          <tr>"
                + "            <td style='padding:0 24px 16px 24px;font-size:14px;color:#4b5563;'>"
                + tabla.toString()
                + "            </td>"
                + "          </tr>"
                // Totales
                + "          <tr>"
                + "            <td style='padding:0 24px 16px 24px;font-size:14px;color:#111827;'>"
                + "              <p style='margin:4px 0;'>Total productos: <strong>$" + String.format("%,.2f", totalProductos) + "</strong></p>"
                + "              <p style='margin:4px 0;'>IVA (1.5%): <strong>$" + String.format("%,.2f", totalIva) + "</strong></p>"
                + "              <p style='margin:4px 0;'>Total pagado: <strong>$" + String.format("%,.2f", totalConIva) + "</strong></p>"
                + "            </td>"
                + "          </tr>"
                // Datos de envío y pago
                + "          <tr>"
                + "            <td style='padding:0 24px 16px 24px;'>"
                + "              <table width='100%' cellpadding='0' cellspacing='0' "
                + "                     style='background-color:#f9fafb;border-radius:10px;border:1px solid #e5e7eb;'>"
                + "                <tr>"
                + "                  <td style='padding:12px 16px;font-size:13px;color:#374151;'>"
                + "                    <p style='margin:0 0 4px 0;'><strong>Datos de envío y pago</strong></p>"
                + "                    <p style='margin:2px 0;'><strong>Dirección:</strong> " + dir + "</p>"
                + "                    <p style='margin:2px 0;'><strong>Método de pago:</strong> " + mpago + "</p>"
                + "                    <p style='margin:2px 0;'><strong>Tarjeta:</strong> " + tarjeta + "</p>"
                + "                  </td>"
                + "                </tr>"
                + "              </table>"
                + "            </td>"
                + "          </tr>"
                // Nota final
                + "          <tr>"
                + "            <td style='padding:0 24px 20px 24px;font-size:13px;color:#4b5563;'>"
                + "              <p style='margin:0;'>Tu pedido está siendo preparado y llegará entre <strong>3 a 8 días hábiles</strong> a la dirección registrada.</p>"
                + "            </td>"
                + "          </tr>"
                + "          <tr>"
                + "            <td style='background-color:#f9fafb;padding:12px 24px;font-size:11px;color:#9ca3af;'>"
                + "              © " + java.time.Year.now() + " Agrivi. Todos los derechos reservados."
                + "            </td>"
                + "          </tr>"
                + "        </table>"
                + "      </td>"
                + "    </tr>"
                + "  </table>"
                + "</body>"
                + "</html>";
    }

    // ---------------- GETTERS / SETTERS ----------------
    public String getAsunto() {
        return asunto;
    }

    public void setAsunto(String asunto) {
        this.asunto = asunto;
    }

    public String getContmensaje() {
        return contmensaje;
    }

    public void setContmensaje(String contmensaje) {
        this.contmensaje = contmensaje;
    }

    public List<String> getDest() {
        return dest;
    }

    public void setDest(List<String> dest) {
        this.dest = (dest == null) ? new ArrayList<>() : new ArrayList<>(dest);
    }

    public List<Usuario> getListaUsr() {
        return listaUsr;
    }

    public void setListaUsr(List<Usuario> listaUsr) {
        this.listaUsr = listaUsr;
    }

    public UploadedFile getArchivoAdjunto() {
        return archivoAdjunto;
    }

    public void setArchivoAdjunto(UploadedFile archivoAdjunto) {
        this.archivoAdjunto = archivoAdjunto;
    }
}
