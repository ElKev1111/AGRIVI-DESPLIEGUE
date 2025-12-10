package Controlador;

import DAO.UsuarioDAO;
import Modelo.CifradoAES;
import Modelo.Usuario;
import Modelo.EnumRoles;

import java.io.Serializable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.time.LocalDateTime;

import java.util.List;
import java.util.Locale;
//import java.util.Properties;
import java.security.SecureRandom;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;

import javax.mail.Message;
//import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import org.primefaces.model.file.UploadedFile;

@ManagedBean
@SessionScoped
public class UsuarioBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private Usuario usuario = new Usuario();
    private UsuarioDAO usuarioDAO = new UsuarioDAO();

    private UploadedFile archivoFoto;
    private boolean autenticado = false;

    // ---------------- RECUPERAR CONTRASEÑA ----------------
    private String correoRecuperacion;

    // ---------------- CAMBIO DE CONTRASEÑA DESDE PERFIL ----------------
    private String passwordActual;
    private String nuevaPassword;
    private String confirmarNuevaPassword;

    // ---------------- GETTERS/SETTERS BÁSICOS ----------------
    public boolean isAutenticado() {
        return usuario != null && usuario.getId() > 0;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public UploadedFile getArchivoFoto() {
        return archivoFoto;
    }

    public void setArchivoFoto(UploadedFile archivoFoto) {
        this.archivoFoto = archivoFoto;
    }

    public String getCorreoRecuperacion() {
        return correoRecuperacion;
    }

    public void setCorreoRecuperacion(String correoRecuperacion) {
        this.correoRecuperacion = correoRecuperacion;
    }

    public String getPasswordActual() {
        return passwordActual;
    }

    public void setPasswordActual(String passwordActual) {
        this.passwordActual = passwordActual;
    }

    public String getNuevaPassword() {
        return nuevaPassword;
    }

    public void setNuevaPassword(String nuevaPassword) {
        this.nuevaPassword = nuevaPassword;
    }

    public String getConfirmarNuevaPassword() {
        return confirmarNuevaPassword;
    }

    public void setConfirmarNuevaPassword(String confirmarNuevaPassword) {
        this.confirmarNuevaPassword = confirmarNuevaPassword;
    }

    // ---------------- LISTADO / ADMIN ----------------
    public List<Usuario> getListaUsuarios() {
        try {
            return usuarioDAO.listar();
        } catch (SQLException e) {
            System.out.println("Erro al listar usuarios");
            return null;
        }
    }

    public String editar(Usuario u) {
        this.usuario = u;
        return "editarUsuario?faces-redirect=true";
    }

    public String actualizar(Usuario u) {
        try {
            u.setFecha_actualizacion(LocalDateTime.now());
            usuarioDAO.actualizar(u);

            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                            "Éxito", "Habitaciónactualizada correctamente"));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Error", "No se pudo actualizar la habitación"));
        }
        return "HomeAdmin1?faces-redirect=true";
    }

    // ---------------- AUTENTICACIÓN ----------------
    public String autenticar() {
        String destino = null;

        try (Connection con = Conexion.conectar()) {
            String sql = "SELECT * FROM usuario WHERE correo = ? AND password = ? ";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, usuario.getCorreo());

            String password = CifradoAES.encriptar(usuario.getPassword());
            ps.setString(2, password);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                this.usuario = new Usuario();
                usuario.setId(rs.getInt("id"));
                usuario.setNombre(rs.getString("nombre"));
                usuario.setCorreo(rs.getString("correo"));
                usuario.setEstado(rs.getString("estado"));
                usuario.setCelular(rs.getString("celular"));
                usuario.setDireccion(rs.getString("direccion"));
                usuario.setPassword(rs.getString("password"));
                usuario.setFotoPerfil(rs.getString("fotoPerfil"));
                usuario.setBiografia(rs.getString("biografia"));

                String rolDb = rs.getString("rol");
                EnumRoles rol = EnumRoles.valueOf(rolDb.trim().toUpperCase(Locale.ROOT));
                usuario.setRol(rol);

                Usuario usuarioCompleto = usuarioDAO.obtenerPorId(usuario.getId());
                if (usuarioCompleto != null) {
                    usuario = usuarioCompleto;
                    rol = usuario.getRol();
                }

                if (usuario.getEstado() != null && !"ACTIVO".equalsIgnoreCase(usuario.getEstado())) {
                    this.autenticado = false;
                    FacesContext.getCurrentInstance().addMessage(null,
                            new FacesMessage(FacesMessage.SEVERITY_WARN, "Aviso",
                                    "Tu usuario está inactivo. Contacta con el administrador."));
                    return null;
                }

                this.autenticado = true;
                FacesContext.getCurrentInstance().getExternalContext()
                        .getSessionMap().put("user", usuario.getNombre());

                if (rol == EnumRoles.ADMINISTRADOR || rol == EnumRoles.EMPLEADO) {
                    destino = "HomeAdmin1?faces-redirect=true";
                } else {
                    destino = "dashboardCliente?faces-redirect=true";
                }

            } else {
                this.autenticado = false;
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN, "Aviso",
                                "Id de Usuario y/o Contraseña no válidos"));
            }

        } catch (SQLException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_FATAL, "Error",
                            "Error en Conexión a Base de Datos"));
        }

        return destino;
    }

    public String logout() {
        try {
            FacesContext context = FacesContext.getCurrentInstance();
            context.getExternalContext().invalidateSession();

            this.usuario = new Usuario();
            this.autenticado = false;

            return "dashboardPresentacion?faces-redirect=true";

        } catch (Exception e) {
            e.printStackTrace();
            return "login?faces-redirect=true";
        }
    }

    // ---------------- REGISTRO ----------------
    public void agregar() throws IOException {
        try {
            usuario.setFecha_creacion(LocalDateTime.now());
            usuario.setFecha_actualizacion(LocalDateTime.now());
            usuario.setEstado("ACTIVO");

            String passEncriptada = CifradoAES.encriptar(usuario.getPassword());
            usuario.setPassword(passEncriptada);

            usuario.setRol(EnumRoles.CLIENTE);
            usuarioDAO.agregar(usuario);

            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                            "Éxito", "Usuario registrado correctamente."));
            this.autenticado = false;

            usuario = new Usuario();
            FacesContext.getCurrentInstance().getExternalContext()
                    .redirect("login.xhtml");

        } catch (SQLException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                            "Error", "Usuario no registrado ."));
        }
    }

    // ---------------- PERFIL (CON CAMBIO DE CONTRASEÑA) ----------------
    public String actualizarPerfil() {
        FacesContext context = FacesContext.getCurrentInstance();

        try {
            // 0) Procesar cambio de contraseña si el usuario llenó los campos
            if (!procesarCambioContrasena(context)) {
                return null;
            }

            // 1) Si el usuario subió una nueva foto
            if (archivoFoto != null && archivoFoto.getFileName() != null && !archivoFoto.getFileName().isEmpty()) {

                String nombreArchivo = System.currentTimeMillis() + "_"
                        + Paths.get(archivoFoto.getFileName()).getFileName().toString();

                String rutaPerfiles = context.getExternalContext()
                        .getRealPath("/resources/images/perfiles");

                File directorio = new File(rutaPerfiles);
                if (!directorio.exists()) {
                    directorio.mkdirs();
                }

                Path destino = Paths.get(directorio.getAbsolutePath(), nombreArchivo);

                try (InputStream input = archivoFoto.getInputStream()) {
                    Files.copy(input, destino, StandardCopyOption.REPLACE_EXISTING);
                }

                usuario.setFotoPerfil(nombreArchivo);
            }

            // 2) Actualizar datos en BD (incluye password)
            boolean actualizado = usuarioDAO.actualizarPerfil(usuario);
            if (actualizado) {
                context.addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO,
                                "Éxito", "Perfil actualizado correctamente."));
                archivoFoto = null;
                limpiarCamposPassword();

                if (usuario.getRol() == EnumRoles.CLIENTE) {
                    return "perfilCliente?faces-redirect=true";
                } else {
                    return "perfil?faces-redirect=true";
                }
            }

            context.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN,
                            "Aviso", "No se pudo actualizar el perfil."));

        } catch (Exception e) {
            e.printStackTrace();
            context.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Error", "Ocurrió un problema al actualizar el perfil."));
        }

        return null;
    }

    private boolean procesarCambioContrasena(FacesContext context) {

        boolean algunCampo
                = !isBlank(passwordActual)
                || !isBlank(nuevaPassword)
                || !isBlank(confirmarNuevaPassword);

        // Si no quiere cambiar contraseña, no hacemos nada.
        if (!algunCampo) {
            return true;
        }

        // Si empezó, debe completar los 3
        if (isBlank(passwordActual) || isBlank(nuevaPassword) || isBlank(confirmarNuevaPassword)) {
            context.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_WARN,
                    "Atención",
                    "Para cambiar la contraseña debes completar los tres campos."));
            return false;
        }

        // Validar contraseña actual (comparando hash MD5)
        String hashIngresado = CifradoAES.encriptar(passwordActual);
        if (usuario.getPassword() == null || !hashIngresado.equals(usuario.getPassword())) {
            context.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Contraseña actual incorrecta",
                    "La contraseña actual no coincide con la registrada."));
            return false;
        }

        // Validar seguridad de la nueva contraseña
        String errorSeguridad = validarSeguridadContrasena(nuevaPassword);
        if (errorSeguridad != null) {
            context.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_WARN,
                    "Nueva contraseña débil",
                    errorSeguridad));
            return false;
        }

        // Confirmación
        if (!nuevaPassword.equals(confirmarNuevaPassword)) {
            context.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Confirmación incorrecta",
                    "La nueva contraseña y su confirmación no coinciden."));
            return false;
        }

        // OK: actualizar hash en el usuario
        usuario.setPassword(CifradoAES.encriptar(nuevaPassword));
        return true;
    }

    private String validarSeguridadContrasena(String pass) {
        if (pass == null) {
            return "La contraseña no puede estar vacía.";
        }
        if (pass.length() < 8) {
            return "Debe tener al menos 8 caracteres.";
        }
        boolean mayus = pass.matches(".*[A-Z].*");
        boolean minus = pass.matches(".*[a-z].*");
        boolean num = pass.matches(".*\\d.*");

        if (!mayus || !minus || !num) {
            return "Debe incluir mayúscula, minúscula y número.";
        }
        return null;
    }

    private void limpiarCamposPassword() {
        passwordActual = null;
        nuevaPassword = null;
        confirmarNuevaPassword = null;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    // ---------------- SESIÓN CLIENTE ----------------
    public void verificarSesionCliente() {
        try {
            if (!isAutenticado() || usuario.getRol() != EnumRoles.CLIENTE) {
                FacesContext.getCurrentInstance().getExternalContext()
                        .redirect("login.xhtml");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void verificarSesionAdmin() {
        try {
            // 1) Si no está autenticado -> login
            if (!isAutenticado()) {
                FacesContext.getCurrentInstance().getExternalContext()
                        .redirect("login.xhtml");
                return;
            }

            // 2) Si es cliente -> bloquear acceso a admin
            if (usuario.getRol() == EnumRoles.CLIENTE) {
                FacesContext.getCurrentInstance().getExternalContext()
                        .redirect("dashboardCliente.xhtml");
            }

            // 3) Si es ADMINISTRADOR o EMPLEADO -> permitido
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void verificarAcceso() {
        try {
            FacesContext fc = FacesContext.getCurrentInstance();
            String viewId = fc.getViewRoot().getViewId(); // ruta real de la vista

            // Páginas públicas que no deben bloquearse
            if (esVistaPublica(viewId)) {
                return;
            }

            // Si no está autenticado -> login
            if (!isAutenticado()) {
                fc.getExternalContext().redirect("login.xhtml");
                return;
            }

            EnumRoles rol = usuario.getRol();

            // ADMIN puede todo
            if (rol == EnumRoles.ADMINISTRADOR) {
                return;
            }

            // Determinar tipo de vista según tu convención de nombres
            boolean esAdmin = esVistaAdmin(viewId);
            boolean esCliente = esVistaCliente(viewId);

            // CLIENTE: solo módulo cliente
            if (rol == EnumRoles.CLIENTE) {
                if (esAdmin) {
                    fc.getExternalContext().redirect("dashboardCliente.xhtml");
                }
                return;
            }

            // EMPLEADO: admin sí, cliente no (según lo que describiste)
            if (rol == EnumRoles.EMPLEADO) {

                // Bloqueo especial
                if (viewId.endsWith("HomeAdmin5.xhtml")) {
                    fc.getExternalContext().redirect("HomeAdmin1.xhtml");
                    return;
                }

                // Si intenta entrar a módulo cliente, lo devolvemos a admin
                if (esCliente && !esAdmin) {
                    fc.getExternalContext().redirect("HomeAdmin1.xhtml");
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean esVistaAdmin(String viewId) {
        // Ajustado a tu patrón real
        // Si tus páginas admin se llaman HomeAdmin*.xhtml esto funciona perfecto:
        return viewId != null && viewId.toLowerCase().contains("homeadmin");
    }

    private boolean esVistaCliente(String viewId) {
        // Ajusta si tus páginas cliente están en carpeta /cliente/ o tienen nombres particulares
        String v = (viewId == null) ? "" : viewId.toLowerCase();
        return v.contains("cliente") || v.contains("dashboardcliente") || v.contains("perfilcliente");
    }

    private boolean esVistaPublica(String viewId) {
        if (viewId == null) {
            return true;
        }
        String v = viewId.toLowerCase();

        // Ajusta a tus páginas públicas reales
        return v.contains("login.xhtml")
                || v.contains("register.xhtml")
                || v.contains("dashboardpresentacion.xhtml")
                || v.contains("recuperarcontrasena.xhtml");
    }

    // ---------------- ESTADO USUARIO ----------------
    public void cambiarEstado(Usuario u) {
        try {
            String nuevoEstado = "ACTIVO".equalsIgnoreCase(u.getEstado()) ? "INACTIVO" : "ACTIVO";
            if (usuarioDAO.actualizarEstado(u.getId(), nuevoEstado)) {
                u.setEstado(nuevoEstado);
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO,
                                "Estado actualizado", "El usuario ahora está " + nuevoEstado));
            }
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Error", "No se pudo cambiar el estado del usuario"));
        }
    }

    // ---------------- RECUPERACIÓN DE CONTRASEÑA ----------------
    private String generarContrasenaTemporal(int longitud) {
        String caracteres = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < longitud; i++) {
            int index = random.nextInt(caracteres.length());
            sb.append(caracteres.charAt(index));
        }
        return sb.toString();
    }

    public void enviarNuevaContrasena() {
        FacesContext context = FacesContext.getCurrentInstance();

        System.out.println("🟦 [RECUPERACION] Click en Enviar Nueva Contraseña");
        System.out.println("🟦 [RECUPERACION] Correo ingresado: " + correoRecuperacion);

        if (correoRecuperacion == null || correoRecuperacion.trim().isEmpty()) {
            context.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_WARN,
                    "Atención",
                    "Debes ingresar un correo."
            ));
            return;
        }

        String correoIngresado = correoRecuperacion.trim();

        try (Connection con = Conexion.conectar()) {

            String sql = "SELECT id, nombre, correo FROM usuario WHERE correo = ?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, correoIngresado);

                try (ResultSet rs = ps.executeQuery()) {

                    if (!rs.next()) {
                        System.out.println("🟨 [RECUPERACION] Correo NO encontrado en BD: " + correoIngresado);
                        context.addMessage(null, new FacesMessage(
                                FacesMessage.SEVERITY_WARN,
                                "Correo no encontrado",
                                "No existe un usuario registrado con ese correo."
                        ));
                        return;
                    }

                    int idUsuario = rs.getInt("id");
                    String nombreUsuario = rs.getString("nombre");
                    String correoBD = rs.getString("correo");

                    System.out.println("🟩 [RECUPERACION] Usuario encontrado: id=" + idUsuario
                            + ", nombre=" + nombreUsuario + ", correoBD=" + correoBD);

                    // 1) Generar nueva contraseña temporal
                    String nuevaPasswordPlano = generarContrasenaTemporal(8);

                    // 2) Encriptar igual que en tu flujo de login
                    String nuevaPasswordEncriptada = CifradoAES.encriptar(nuevaPasswordPlano);

                    // 3) Actualizar en BD
                    String sqlUpdate = "UPDATE usuario SET password = ? WHERE id = ?";
                    try (PreparedStatement ps2 = con.prepareStatement(sqlUpdate)) {
                        ps2.setString(1, nuevaPasswordEncriptada);
                        ps2.setInt(2, idUsuario);

                        int filas = ps2.executeUpdate();
                        System.out.println("🟩 [RECUPERACION] Filas actualizadas: " + filas);

                        if (filas == 0) {
                            context.addMessage(null, new FacesMessage(
                                    FacesMessage.SEVERITY_ERROR,
                                    "Error",
                                    "No se pudo actualizar la contraseña en la base de datos."
                            ));
                            return;
                        }
                    }

                    System.out.println("🟩 [RECUPERACION] Password actualizada en BD para id=" + idUsuario);

                    // 4) Enviar correo con contraseña temporal en texto plano
                    boolean enviado = enviarCorreoRecuperacion(correoBD, nombreUsuario, nuevaPasswordPlano);
                    System.out.println("🟦 [RECUPERACION] Resultado envío correo: " + enviado);

                    if (enviado) {
                        context.addMessage(null, new FacesMessage(
                                FacesMessage.SEVERITY_INFO,
                                "Correo enviado",
                                "Se ha enviado una nueva contraseña temporal a tu correo."
                        ));
                        correoRecuperacion = null;
                    } else {
                        context.addMessage(null, new FacesMessage(
                                FacesMessage.SEVERITY_ERROR,
                                "Error",
                                "No se pudo enviar el correo. Intenta más tarde o contacta al administrador."
                        ));
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            context.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Error interno",
                    "Ocurrió un error al procesar la recuperación de contraseña."
            ));
        }
    }

    private boolean enviarCorreoRecuperacion(String correoDestino,
            String nombreUsuario,
            String nuevaPassword) {
        try {
            // --------------------------------------------------------------------
            // ✅ ELIMINADO (hardcode):
            // final String user = "TU_CORREO_GMAIL";
            // final String pass = "TU_APP_PASSWORD";
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
            // Session sesion = Session.getInstance(props, new javax.mail.Authenticator() {
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
            System.out.println("🟦 [MAIL-RECUPERACION] FROM detectado: " + from);

            // Validación para evitar fallos silenciosos
            if (from == null || from.trim().isEmpty()) {
                System.err.println("❌ Configuración de correo no encontrada. "
                        + "Define MAIL_HOST, MAIL_PORT, MAIL_USER, MAIL_PASS y MAIL_FROM.");
                return false;
            }

            MimeMessage mensaje = new MimeMessage(sesion);
            mensaje.setFrom(new InternetAddress(from, "AgriviApp"));
            mensaje.setRecipient(Message.RecipientType.TO, new InternetAddress(correoDestino));
            mensaje.setSubject("Recuperación de contraseña - AgriviApp", "UTF-8");

            String html = construirHtmlRecuperacion(nombreUsuario, nuevaPassword);

            MimeBodyPart cuerpoHtml = new MimeBodyPart();
            cuerpoHtml.setContent(html, "text/html; charset=UTF-8");

            MimeMultipart multipart = new MimeMultipart("mixed");
            multipart.addBodyPart(cuerpoHtml);

            mensaje.setContent(multipart);

            Transport.send(mensaje);
            System.out.println("✅ Correo de recuperación enviado a: " + correoDestino);
            return true;

        } catch (Exception e) {
            System.err.println("❌ ERROR al enviar correo de recuperación a "
                    + correoDestino + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private String construirHtmlRecuperacion(String nombreUsuario, String nuevaPassword) {
        String saludo = (nombreUsuario != null && !nombreUsuario.trim().isEmpty())
                ? "Hola, " + escapeHtml(nombreUsuario) + ","
                : "Hola,";

        return "<!DOCTYPE html>"
                + "<html lang='es'>"
                + "<head>"
                + "  <meta charset='UTF-8'/>"
                + "  <title>Recuperación de contraseña</title>"
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
                + "                <td align='right' style='font-size:12px;'>Seguridad</td>"
                + "              </tr></table>"
                + "            </td>"
                + "          </tr>"
                + "          <tr>"
                + "            <td style='padding:24px 24px 8px 24px;'>"
                + "              <h1 style='margin:0;font-size:20px;color:#111827;'>Recuperación de contraseña</h1>"
                + "            </td>"
                + "          </tr>"
                + "          <tr>"
                + "            <td style='padding:8px 24px 8px 24px;font-size:14px;color:#4b5563;line-height:1.6;'>"
                + "              <p style='margin-top:0;'>" + saludo + "</p>"
                + "              <p style='margin:0;'>Has solicitado recuperar tu contraseña en Agrivi.</p>"
                + "            </td>"
                + "          </tr>"
                + "          <tr>"
                + "            <td style='padding:0 24px 16px 24px;'>"
                + "              <div style='background:#f9fafb;border:1px solid #e5e7eb;border-radius:10px;padding:14px 16px;'>"
                + "                <p style='margin:0;font-size:12px;color:#6b7280;'>Tu contraseña temporal es:</p>"
                + "                <p style='margin:6px 0 0 0;font-size:18px;font-weight:bold;color:#111827;letter-spacing:0.5px;'>"
                + escapeHtml(nuevaPassword)
                + "                </p>"
                + "              </div>"
                + "            </td>"
                + "          </tr>"
                + "          <tr>"
                + "            <td style='padding:0 24px 20px 24px;font-size:13px;color:#374151;line-height:1.6;'>"
                + "              <p style='margin:0 0 8px 0;'>Te recomendamos cambiarla después de iniciar sesión desde tu perfil.</p>"
                + "              <p style='margin:0;'>Si tú no solicitaste este cambio, por favor contacta al administrador.</p>"
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

    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
