/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package logica;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 * @author hecto
 */
public class Main {

    public static void main(String[] args) {
        // INICIALIZAR EL SISTEMA DE LOGS
        FederacionLog log = new FederacionLog(); 
        log.escribir("--- INICIANDO SIMULACIÓN DE LA FEDERACIÓN GALÁCTICA ---");

        // CREAR LAS ZONAS DE AFORO ILIMITADO
        Zona centroCoordinacion = new Zona("Centro de Coordinación Federal", 1000);
        Zona hangar = new Zona("Hangar de Patrullas", 1000);
        Zona baseSaqueadores = new Zona("Base de Saqueadores", 1000);
        Zona zonaRecuperacion = new Zona("Zona de Reparación y Recuperación", 1000);

        // CREAR LOS PLANETAS MINEROS (Aforo máximo de 4 delegados)
        Zona cryon = new Zona("Cryon", 4);
        Zona velora = new Zona("Velora", 4);
        Zona ferrum = new Zona("Ferrum", 4);
        Zona drax = new Zona("Drax", 4);
        Zona ignis = new Zona("Ignis", 4);

        // Agrupamos los planetas por tipo de recurso para pasárselos al Delegado
        Zona[] planetasCristal = {cryon, velora};
        Zona[] planetasMineral = {ferrum, drax};
        Zona planetaPlasma = ignis; 
        Zona[] todosLosPlanetas = {cryon, velora, ferrum, drax, ignis};

        // CREAR LOS DEPÓSITOS ORBITALES (Con sus capacidades y recursos iniciales)
        Deposito depositoCristal = new Deposito("Depósito de Cristal", 250, 100);
        Deposito depositoMineral = new Deposito("Depósito de Mineral", 200, 80);
        Deposito depositoPlasma = new Deposito("Depósito de Plasma", 150, 60);
        
        Deposito[] todosLosDepositos = {depositoCristal, depositoMineral, depositoPlasma};

        // --- CREAR LA LISTA DE ZONAS COMPLETA PARA EL SERVIDOR ---
        ArrayList<Zona> listaZonasCompleta = new ArrayList<>();
        listaZonasCompleta.add(centroCoordinacion);
        listaZonasCompleta.add(hangar);
        listaZonasCompleta.add(baseSaqueadores);
        listaZonasCompleta.add(zonaRecuperacion);
        listaZonasCompleta.add(cryon);
        listaZonasCompleta.add(velora);
        listaZonasCompleta.add(ferrum);
        listaZonasCompleta.add(drax);
        listaZonasCompleta.add(ignis);
        listaZonasCompleta.add(depositoCristal);
        listaZonasCompleta.add(depositoMineral);
        listaZonasCompleta.add(depositoPlasma);

        // LISTAS CONCURRENTES PARA EL CONTROL GLOBAL (Corrección de SaqueadorEspacial)
        CopyOnWriteArrayList<PatrullaFederal> listaPatrullas = new CopyOnWriteArrayList<>();
        CopyOnWriteArrayList<Saqueador> listaSaqueadores = new CopyOnWriteArrayList<>();
        CopyOnWriteArrayList<DelegadoComercial> listaDelegados = new CopyOnWriteArrayList<>(); 

        // CREAR Y ARRANCAR EL CEREBRO DEL SISTEMA
        GestorEventos gestor = new GestorEventos(log, depositoCristal, depositoMineral, depositoPlasma, 
                todosLosPlanetas, todosLosDepositos, hangar, baseSaqueadores, zonaRecuperacion, 
                listaPatrullas, listaSaqueadores, listaDelegados, centroCoordinacion);
        gestor.start();
        
        ServidorGUI ventanaServidor = new ServidorGUI(listaZonasCompleta, todosLosDepositos);
        ventanaServidor.setVisible(true);

        // --- GENERACIÓN INICIAL DE DELEGADOS COMERCIALES (1 cada 2 segundos) ---
        for (int i = 1; i <= 10; i++) {
            DelegadoComercial delegado = new DelegadoComercial(i, centroCoordinacion, planetasCristal, 
                    planetasMineral, planetaPlasma, depositoCristal, depositoMineral, depositoPlasma, 
                    zonaRecuperacion, log, gestor);
            listaDelegados.add(delegado);
            delegado.start();
            
            log.escribir("Generado delegado comercial: D" + String.format("%03d", i));
            
            try {
                Thread.sleep(2000); // Espera de 2s exigida por el enunciado
            } catch (InterruptedException e) {
                System.out.println("Interrupción en la creación de delegados");
            }
        }

        // --- GENERACIÓN INICIAL DE PATRULLAS (1 cada 5 segundos) ---
        for (int i = 1; i <= 2; i++) {
            String idPatrulla = String.format("P%03d", i);
            PatrullaFederal patrulla = new PatrullaFederal(idPatrulla, todosLosPlanetas, todosLosDepositos, 
                    hangar, zonaRecuperacion, log, gestor);
            
            listaPatrullas.add(patrulla);
            patrulla.start();
            
            log.escribir("Generada patrulla inicial: " + idPatrulla);
            
            try {
                Thread.sleep(5000); // Espera de 5s exigida por el enunciado
            } catch (InterruptedException e) {
                System.out.println("Interrupción en la creación de patrullas");
            }
        }
        
        // INICIALIZAR Y PUBLICAR EL SERVIDOR RMI
        try {
            LocateRegistry.createRegistry(1099); // Crea el rmiregistry en el puerto estándar 1099
            Servidor servidorRMI = new Servidor(listaZonasCompleta, todosLosDepositos, gestor, listaPatrullas);
            Naming.rebind("//localhost:1099/Federacion", servidorRMI);
            log.escribir("Servidor RMI registrado correctamente bajo el nombre '/Federacion'.");
        } catch (Exception e) {
            System.err.println("Error al levantar el servicio RMI: " + e.getMessage());
        }
        
    }
    
}
