package Controlador;

import Modelo.PedidoProveedor;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailService {

    public EmailService() {
        // --------------------------------------------------------------------
        // ✅ ELIMINADO:
        // Configuración SMTP fija de Gmail en constructor con Properties
        // props.put("mail.smtp.host", "smtp.gmail.com");
        // props.put("mail.smtp.port", "587");
        // --------------------------------------------------------------------
        // Ahora la configuración vive en MailSessionFactory (ENV local/prod)
    }

    /**
     * Envía un correo con los detalles del pedido a un proveedor específico.
     *
     * @param pedido El objeto PedidoProveedor con la información.
     * @param emailDestino La dirección de correo del proveedor.
     * @return true si el envío es exitoso, false en caso contrario.
     */
    public boolean enviarPedido(PedidoProveedor pedido, String emailDestino) {

        // --------------------------------------------------------------------
        // ✅ ELIMINADO (hardcode):
        // private final String USER_EMAIL = "kalvinalonzo@gmail.com";
        // private final String APP_PASSWORD = "fjre wsvh lsbn exyw";
        //
        // ✅ ELIMINADO:
        // private Properties props;
        //
        // ✅ ELIMINADO:
        // Session sesion = Session.getInstance(props, new Authenticator() {
        //     @Override
        //     protected PasswordAuthentication getPasswordAuthentication() {
        //         return new PasswordAuthentication(USER_EMAIL, APP_PASSWORD);
        //     }
        // });
        //
        // ✅ REEMPLAZADO POR:
        // Sesión dinámica por variables de entorno (local/prod)
        // --------------------------------------------------------------------
        Session sesion = MailSessionFactory.createSession();
        String from = MailSessionFactory.getFrom();

        // Validación para evitar fallos silenciosos
        if (from == null || from.trim().isEmpty()) {
            System.err.println("❌ Configuración de correo no encontrada. "
                    + "Define MAIL_HOST, MAIL_PORT, MAIL_USER, MAIL_PASS y MAIL_FROM.");
            return false;
        }

        try {
            MimeMessage mensaje = new MimeMessage(sesion);
            mensaje.setFrom(new InternetAddress(from, "AgriviApp"));
            mensaje.setRecipient(Message.RecipientType.TO, new InternetAddress(emailDestino));

            String asunto = "Solicitud de Pedido N° " + safe(pedido.getIdPedido())
                    + " | Proveedor: " + safe(pedido.getNombreProveedor());
            mensaje.setSubject(asunto, "UTF-8");

            String cuerpo = "Estimado(a) Proveedor(a) " + safe(pedido.getNombreProveedor()) + ",\n\n"
                    + "Se ha generado una nueva solicitud de pedido con los siguientes detalles:\n\n"
                    + "ID del Pedido: " + safe(pedido.getIdPedido()) + "\n"
                    + "Fecha de Solicitud: " + safe(pedido.getFechaPedido()) + "\n"
                    + "Descripción:\n" + safe(pedido.getDescripcionPedido()) + "\n\n"
                    + "Por favor, confirme la recepción y la disponibilidad. "
                    + "Nos comunicaremos para coordinar la entrega.\n\n"
                    + "Atentamente,\n"
                    + "Administración del Sistema";

            mensaje.setText(cuerpo, "UTF-8");

            Transport.send(mensaje);
            System.out.println("✅ Correo de pedido enviado a: " + emailDestino);
            return true;

        } catch (MessagingException e) {
            System.err.println("❌ ERROR al enviar correo de pedido a " + emailDestino + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            System.err.println("❌ ERROR general al enviar correo de pedido a " + emailDestino + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Evita NPE al construir el texto del correo
    private String safe(Object o) {
        return (o == null) ? "No disponible" : String.valueOf(o);
    }
}
