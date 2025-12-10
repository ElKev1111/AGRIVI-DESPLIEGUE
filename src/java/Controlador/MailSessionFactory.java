package Controlador;

import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;

public class MailSessionFactory {

    private static String getenv(String key, String def) {
        String v = System.getenv(key);
        return (v == null || v.trim().isEmpty()) ? def : v.trim();
    }

    public static Session createSession() {
        final String host = getenv("MAIL_HOST", "smtp.gmail.com");
        final String port = getenv("MAIL_PORT", "587");
        final String user = getenv("MAIL_USER", "");
        final String pass = getenv("MAIL_PASS", "");

        if (user.isEmpty() || pass.isEmpty()) {
            System.err.println("❌ MAIL_USER o MAIL_PASS no definidos.");
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.ssl.trust", host);

        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, pass);
            }
        });
    }

    public static String getFrom() {
        String from = getenv("MAIL_FROM", "");
        if (!from.isEmpty()) {
            return from;
        }
        return getenv("MAIL_USER", "");
    }
}
