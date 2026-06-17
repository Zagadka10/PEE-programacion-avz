/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package logica;

import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 * @author hecto
 */
public class GestorEventos extends Thread {

    private boolean pausado = false;
    private final FederacionLog log; // Usa tu HawkinsLog renombrado
    private final Random random = new Random();

    // Contadores para asignar IDs únicos (P001, S001...)
    private int contadorPatrullas = 1;
    private int contadorSaqueadores = 1;

    // Referencias a los recursos para comprobar refuerzos
    private final Deposito depositoCristal;
    private final Deposito depositoMineral;
    private final Deposito depositoPlasma;

    // Referencias a las zonas para inyectarlas en los nuevos hilos que nazcan
    private final Zona[] planetas;
    private final Deposito[] depositos;
    private final Zona hangar;
    private final Zona baseSaqueadores;
    private final Zona zonaRecuperacion;

    // Listas concurrentes para controlar los aforos máximos globales
    private final CopyOnWriteArrayList<PatrullaFederal> listaPatrullas;
    private final CopyOnWriteArrayList<Saqueador> listaSaqueadores;
    private final CopyOnWriteArrayList<DelegadoComercial> listaDelegados;
    private int contadorDelegados = 11;

    public GestorEventos(FederacionLog log, Deposito dCristal, Deposito dMineral, Deposito dPlasma,
            Zona[] planetas, Deposito[] depositos, Zona hangar, Zona baseSaqueadores, Zona zonaRecuperacion,
            CopyOnWriteArrayList<PatrullaFederal> listaPatrullas,
            CopyOnWriteArrayList<Saqueador> listaSaqueadores, CopyOnWriteArrayList<DelegadoComercial> listaDelegados) {
        this.log = log;
        this.depositoCristal = dCristal;
        this.depositoMineral = dMineral;
        this.depositoPlasma = dPlasma;
        this.planetas = planetas;
        this.depositos = depositos;
        this.hangar = hangar;
        this.baseSaqueadores = baseSaqueadores;
        this.zonaRecuperacion = zonaRecuperacion;
        this.listaPatrullas = listaPatrullas;
        this.listaSaqueadores = listaSaqueadores;
        this.listaDelegados = listaDelegados;
        // Inicializamos el contador de patrullas asumiendo que el Main creará las 2 primeras
        this.contadorPatrullas = 3;  
    }

    // ---  CONTROL DE PAUSA GLOBAL ---
    public synchronized void comprobarPausa() throws InterruptedException {
        while (pausado) {
            this.wait();
        }
    }

    public synchronized void setPausado(boolean estado) {
        this.pausado = estado;
        if (!pausado) {
            this.notifyAll(); // Despierta a todo el universo
        }
    }
    
    public synchronized void comprobarNuevosDelegados() {
        boolean hayDesabastecimiento =
            depositoCristal.getCantidadAlmacenada() < 25 ||
            depositoMineral.getCantidadAlmacenada() < 20 ||
            depositoPlasma.getCantidadAlmacenada()  < 15;

        // Contar los delegados que NO están en zona de recuperación
        long activos = listaDelegados.stream()
            .filter(d -> !zonaRecuperacion.getDelegadosPresentes().contains(d))
            .count();

        if (hayDesabastecimiento && activos < 20) {
            String idNuevo = String.format("D%03d", contadorDelegados++);
            DelegadoComercial nuevo = new DelegadoComercial(contadorDelegados - 1,
                planetas[0], planetas[1], planetas[3], planetas[4],
                depositoCristal, depositoMineral, depositoPlasma,
                zonaRecuperacion, log, this);
            listaDelegados.add(nuevo);
            nuevo.start();
            log.escribir("Nuevo delegado generado por desabastecimiento: " + idNuevo);
        }
    }

    // --- GESTIÓN DE REFUERZOS AUTOMÁTICOS ---
    // Este método lo llama el DelegadoComercial justo después de depositar
    public synchronized void comprobarRefuerzos() {
        // Comprobamos los umbrales exactos del enunciado y el límite de 20 patrullas
        if (depositoCristal.getCantidadAlmacenada() >= 150
                && depositoMineral.getCantidadAlmacenada() >= 100
                && depositoPlasma.getCantidadAlmacenada() >= 75
                && listaPatrullas.size() < 20) {

            // Si hay recursos, los "robamos" (consumimos)
            depositoCristal.robarRecurso(150);
            depositoMineral.robarRecurso(100);
            depositoPlasma.robarRecurso(75);

            // Creamos e iniciamos la nueva patrulla
            String idNueva = String.format("P%03d", contadorPatrullas++);
            PatrullaFederal nuevaPatrulla = new PatrullaFederal(idNueva, planetas, depositos, hangar, zonaRecuperacion, log, this);
            listaPatrullas.add(nuevaPatrulla);
            nuevaPatrulla.start();

            log.escribir("Se incorpora una nueva patrulla federal " + idNueva + ".");
        }
    }

    // --- BUCLE DE GENERACIÓN DE SAQUEADORES ---
    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                comprobarPausa();

                // El sistema crea un saqueador cada 10-20 segundos
                int tiempoEspera = 10 + random.nextInt(11);

                // Troceamos la espera en siestas de 1 segundo para que reaccione a la pausa
                for (int i = 0; i < tiempoEspera; i++) {
                    comprobarPausa();
                    Thread.sleep(1000);
                }

                // Generamos si no hemos llegado al límite de 40
                if (listaSaqueadores.size() < 40) {
                    String idSaqueador = String.format("S%03d", contadorSaqueadores++);
                    Saqueador nuevoSaqueador = new Saqueador(idSaqueador, planetas, depositos, baseSaqueadores, log, this);
                    listaSaqueadores.add(nuevoSaqueador); 
                    nuevoSaqueador.start();
                }
            }
        } catch (InterruptedException e) {
            log.escribir("Gestor de Eventos interrumpido.");
        }
    }
}
