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
    private volatile boolean expulsado = false;

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
    public void serExpulsado() {
        this.expulsado = true;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                // CENTRO DE COORDINACIÓN
                gestor.comprobarPausa();
                centroCoordinacion.entrarDelegado(this);
                log.escribir(id + " preparando solicitud en el Centro de Coordinación.");
                Thread.sleep(2000 + random.nextInt(2001)); // 2 a 4 segundos
                centroCoordinacion.salirDelegado(this);

                // SELECCIÓN DE RECURSO Y PLANETA
                int seleccion = random.nextInt(5); // Número del 0 al 4
                Zona planetaDestino;
                Deposito depositoDestino;
                
                if (seleccion < 2) { 
                    // Sale 0 o 1 (40% de viajes para Cristal)
                    planetaDestino = planetasCristal[seleccion];
                    depositoDestino = depositoCristal;
                } else if (seleccion < 4) { 
                    // Sale 2 o 3 (40% de viajes para Mineral)
                    planetaDestino = planetasMineral[seleccion - 2];
                    depositoDestino = depositoMineral;
                } else { 
                    // Sale 4 (20% de viajes para Plasma)
                    planetaDestino = planetaPlasma;
                    depositoDestino = depositoPlasma;
                }

                // VIAJE AL PLANETA Y EXTRACCIÓN
                gestor.comprobarPausa();
                planetaDestino.entrarDelegado(this); // Aquí se bloquea si hay 4 delegados o hay ataque
                log.escribir(id + " comienza extracción en " + planetaDestino.getId() + ".");

                expulsado = false; // Reseteamos el estado antes de extraer
                Thread.sleep(3000 + random.nextInt(2001)); // 3 a 5 segundos de extracción

                planetaDestino.salirDelegado(this);

                // ¿FUE ATACADO DURANTE LA EXTRACCIÓN?
                if (expulsado) {
                    log.escribir(id + " huye a la Zona de Recuperación tras el ataque.");
                    zonaRecuperacion.entrarDelegado(this);
                    Thread.sleep(5000 + random.nextInt(5001)); // 10 a 15 segundos
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

                Thread.sleep(2000 + random.nextInt(1001)); // 2 a 3 segundos depositando

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
