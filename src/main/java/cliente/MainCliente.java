/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package cliente;

import java.rmi.Naming;
import comun.InterfazFederacion;
/**
 *
 * @author hecto
 */
public class MainCliente {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        System.out.println("Buscando conexión con el Servidor de la Federación...");

        try {
            // Localizamos el objeto en la red bajo el nombre que publicamos en el servidor
            InterfazFederacion servidor = (InterfazFederacion) Naming.lookup("//localhost:1099/Federacion");
            System.out.println("¡Conexión establecida! Abriendo interfaz gráfica...");

            // Abrimos la ventana pasándole el acceso al servidor RMI
            ClienteGUI ventana = new ClienteGUI(servidor);
            ventana.setVisible(true);

        } catch (Exception e) {
            System.out.println("!!! Error de conexión: No se pudo contactar con el Servidor de la Federación.");
            e.printStackTrace();
        }

    }
    
}
