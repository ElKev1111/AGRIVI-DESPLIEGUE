package Controlador;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Conexion {

    // URL de la base de datos (con usuario y SSL deshabilitado)
    private static final String URL = "jdbc:mysql://yamanote.proxy.rlwy.net:38385/agrivijsf3?useSSL=false";
    private static final String USUARIO = "root";
    private static final String PASSWORD = "jWnIXKqHDwlFMKavWgldkmqWnMIBoXkI";
    
    public static Connection conectar() throws SQLException {
        Connection conn = null;

        try {
            // 1. Cargar el Driver (ya no es estrictamente necesario, pero es buena práctica en algunos entornos)
            Class.forName("com.mysql.cj.jdbc.Driver");

            // 2. Establecer la conexión
            conn = DriverManager.getConnection(URL, USUARIO, PASSWORD);

            // Opcional: Imprime mensaje si la conexión fue exitosa (solo para debugging)
            // System.out.println("Conexión a la base de datos exitosa.");
        } catch (ClassNotFoundException e) {
            System.err.println("Error: No se encontró el Driver de MySQL.");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("Error de Conexión o Credenciales.");
            e.printStackTrace();
        }

        return conn;
    }
}
