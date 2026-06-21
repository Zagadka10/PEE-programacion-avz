/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package logica;

import java.util.Random;

/**
 *
 * @author hecto
 */
public class DelegadoComercial extends Thread {

    private final String id;

    // Zonas del mapa que necesita conocer
    private final Zona centroCoordinacion;
    private final Zona[] planetasCristal;
    private final Zona[] planetasMineral;
    private final Zona planetaPlasma;
    private final Deposito depositoCristal;
    private final Deposito depositoMineral;
    private final Deposito depositoPlasma;
    private final Zona zonaRecuperacion;

    // Herramientas de control
    private final FederacionLog log;
    private final GestorEventos gestor;
    private final Random random = new Random();

    // Bandera para saber si fue expulsado por un saqueador
    private boolean expulsado = false;

    public DelegadoComercial(int idNumerico, Zona centro, Zona[] pCristal, Zona[] pMineral, Zona pPlasma,
            Deposito dCristal, Deposito dMineral, Deposito dPlasma, Zona zRecuperacion,
            FederacionLog log, GestorEventos gestor) {
        // Formateamos el ID como pide el enunciado: D001, D015, etc.
        this.id = String.format("D%03d", idNumerico);
        this.centroCoordinacion = centro;
        this.planetasCristal = pCristal;
        this.planetasMineral = pMineral;
        this.planetaPlasma = pPlasma;
        this.depositoCristal = dCristal;
        this.depositoMineral = dMineral;
        this.depositoPlasma = dPlasma;
        this.zonaRecuperacion = zRecuperacion;
        this.log = log;
        this.gestor = gestor;
    }

    public String getIdDelegado() {
        return id;
    }

    // Método que llamará el Saqueador para expulsarlo del planeta
    public synchronized void serExpulsado() {
        this.expulsado = true;
    }

    public synchronized boolean isExpulsado() {
        return expulsado;
    }
    
    public synchronized void resetearExpulsion() {
        this.expulsado = false;
    }

    private void dormirConPausa(long ms) throws InterruptedException {
        long iteraciones = ms / 500;
        for (int i = 0; i < iteraciones; i++) {
            gestor.comprobarPausa();
            Thread.sleep(500);
        }
    }
    
    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                // CENTRO DE COORDINACIÓN
                gestor.comprobarPausa();
                centroCoordinacion.entrarDelegado(this);
                log.escribir(id + " preparando solicitud en el Centro de Coordinación.");
                dormirConPausa(2000 + random.nextInt(2001)); // 2 a 4 segundos
                centroCoordinacion.salirDelegado(this);

                // SELECCIÓN DE RECURSO Y PLANETA
                // Mejora para que no acapare los delegados un deposito con un limite en la cola de 6.
                // pequeña ayuda para deposito de plasma.
                Zona planetaDestino = null;
                Deposito depositoDestino = null;
                boolean objetivoEncontrado = false;
                int limiteCola = 6; 

                while (!objetivoEncontrado) {
                    int seleccion = random.nextInt(5); 
                    
                    if (seleccion < 2) { 
                        planetaDestino = planetasCristal[seleccion];
                        depositoDestino = depositoCristal;
                    } else if (seleccion < 4) { 
                        planetaDestino = planetasMineral[seleccion - 2];
                        depositoDestino = depositoMineral;
                    } else { 
                        planetaDestino = planetaPlasma;
                        depositoDestino = depositoPlasma;
                    }

                    if (planetaDestino.getNumeroDelegadosEnCola() < limiteCola && 
                        depositoDestino.getNumeroDelegadosEnCola() < limiteCola) {
                        
                        objetivoEncontrado = true;
                    } else {
                        log.escribir(id + " ve demasiada cola en " + planetaDestino.getId() + ". Sorteando nuevo destino...");
                        dormirConPausa(500); 
                    }
                }

                // VIAJE AL PLANETA Y EXTRACCIÓN
                gestor.comprobarPausa();
                planetaDestino.entrarDelegado(this); // Aquí se bloquea si hay 4 delegados o hay ataque
                log.escribir(id + " comienza extracción en " + planetaDestino.getId() + ".");

                resetearExpulsion(); // Reseteamos el estado antes de extraer
                dormirConPausa(3000 + random.nextInt(2001)); // 3 a 5 segundos de extracción

                planetaDestino.salirDelegado(this);

                // ¿FUE ATACADO DURANTE LA EXTRACCIÓN?
                if (isExpulsado()) {
                    log.escribir(id + " huye a la Zona de Recuperación tras el ataque.");
                    zonaRecuperacion.entrarDelegado(this);
                    dormirConPausa(5000 + random.nextInt(5001)); // 10 a 15 segundos
                    zonaRecuperacion.salirDelegado(this);
                    continue; // Vuelve al Centro de Coordinación (inicio del while) sin depositar
                }

                // SI NO HUBO ATAQUE: DEPOSITAR RECURSOS
                int cantidadExtraida = 10 + random.nextInt(16); // 10 a 25 unidades
                log.escribir(id + " extrae " + cantidadExtraida + " uds y viaja a " + depositoDestino.getId());

                gestor.comprobarPausa();
                depositoDestino.entrarDelegado(this); // Controla máximo 3 delegados

                // depositarRecurso bloquea al hilo si no hay hueco físico en el almacén
                depositoDestino.depositarRecurso(cantidadExtraida);
                log.escribir(id + " depositando " + cantidadExtraida + " uds en " + depositoDestino.getId() + ".");

                dormirConPausa(2000 + random.nextInt(1001)); // 2 a 3 segundos depositando

                depositoDestino.salirDelegado(this);

                // Tras salir, el gestor comprobará si hay recursos para crear Patrullas 
                // Lo invocamos de forma segura
                gestor.comprobarRefuerzos();
                gestor.comprobarNuevosDelegados();
            }
        } catch (InterruptedException ie) {
            log.escribir("Hilo interrumpido para el delegado " + id);
        }
    }
}
