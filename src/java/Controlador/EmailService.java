package Controlador;

import Modelo.PedidoProveedor;
import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailService {
    
    // --- Configuración SMTP de Gmail (Usada en CorreoBean) ---
    private final String USER_EMAIL = "kalvinalonzo@gmail.com";
    // NOTA: Usa la "Contraseña de aplicación" generada por Google, no la contraseña de tu cuenta.
    private final String APP_PASSWORD = "fjre wsvh lsbn exyw"; 
    
    private Properties props;
    
    public EmailService() {
        // Inicializar las propiedades SMTP
        props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
    }

    /**
     * Envía un correo con los detalles del pedido a un proveedor específico.
     * @param pedido El objeto PedidoProveedor con la información.
     * @param emailDestino La dirección de correo del proveedor.
     * @return true si el envío es exitoso, false en caso contrario.
     */
    public boolean enviarPedido(PedidoProveedor pedido, String emailDestino) {
        
        // 1. Autenticación de Usuario
        Session sesion = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(USER_EMAIL, APP_PASSWORD);
            }
        });
        
        // 2. Preparar el Mensaje
        try {
            Message mensaje = new MimeMessage(sesion);
            mensaje.setFrom(new InternetAddress(USER_EMAIL));
            mensaje.setRecipient(Message.RecipientType.TO, new InternetAddress(emailDestino));
            
            // Asunto
            mensaje.setSubject("Solicitud de Pedido N° " + pedido.getIdPedido() + " | Proveedor: " + pedido.getNombreProveedor());
            
            // Cuerpo del Mensaje (Simple)
            String cuerpo = "Estimado(a) Proveedor(a) " + pedido.getNombreProveedor() + ",\n\n"
                          + "Se ha generado una nueva solicitud de pedido con los siguientes detalles:\n\n"
                          + "ID del Pedido: " + pedido.getIdPedido() + "\n"
                          + "Fecha de Solicitud: " + pedido.getFechaPedido() + "\n"
                          + "Descripción:\n" + pedido.getDescripcionPedido() + "\n\n"
                          + "Por favor, confirme la recepción y la disponibilidad. Nos comunicaremos para coordinar la entrega.\n\n"
                          + "Atentamente,\n"
                          + "Administración del Sistema";
            
            mensaje.setText(cuerpo);
            
            // 3. Enviar
            Transport.send(mensaje);
            System.out.println("✅ Correo de pedido enviado a: " + emailDestino);
            return true;
            
        } catch (MessagingException e) {
            System.err.println("❌ ERROR al enviar correo de pedido a " + emailDestino + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}