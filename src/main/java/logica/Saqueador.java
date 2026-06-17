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
public class Saqueador extends Thread{
    private final String id;
    private final Zona[] planetas;
    private final Deposito[] depositos;
    private final Zona baseSaqueadores;
    private final FederacionLog log;
    private final GestorEventos gestor;
    private final Random random = new Random();

    public Saqueador(String id, Zona[] planetas, Deposito[] depositos, Zona baseSaqueadores, FederacionLog log, GestorEventos gestor) {
        this.id = id;
        this.planetas = planetas;
        this.depositos = depositos;
        this.baseSaqueadores = baseSaqueadores;
        this.log = log;
        this.gestor = gestor;
    }

    private void dormirConPausa(long milisegundos) throws InterruptedException {
        long iteraciones = milisegundos / 500;
        for (int i = 0; i < iteraciones; i++) {
            gestor.comprobarPausa();
            Thread.sleep(500);
        }
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                // 1. ESPERA EN LA BASE (3 a 6 segundos)
                gestor.comprobarPausa();
                log.escribir(id + " esperando en Base de Saqueadores.");
                dormirConPausa(3000 + random.nextInt(3001));

                // 2. SELECCIÓN DE OBJETIVO (70% Depósito, 30% Planeta)
                boolean atacaDeposito = random.nextInt(100) < 70;
                Zona[] opciones = atacaDeposito ? depositos : planetas;
                Zona objetivo = null;

                // Buscamos una zona que no esté bajo ataque
                for (Zona z : opciones) {
                    if (!z.isBajoAtaque()) {
                        objetivo = z;
                        break;
                    }
                }
                // Si todas están ocupadas, elegimos una al azar para hacer cola en ella
                if (objetivo == null) {
                    objetivo = opciones[random.nextInt(opciones.length)];
                }

                // 3. INICIAR ATAQUE (Si está atacada, se bloquea aquí haciendo cola)
                gestor.comprobarPausa();
                objetivo.iniciarAtaque();
                log.escribir(id + " inicia el ataque en " + objetivo.getId());

                // 4. FASE DE COMBATE
                boolean ataqueExitoso = true;
                PatrullaFederal patrullaDefensora = objetivo.obtenerPatrullaDefensora();

                if (patrullaDefensora != null) {
                    log.escribir(id + " entra en combate con " + patrullaDefensora.getIdPatrulla() + " en " + objetivo.getId());
                    dormirConPausa(6000); // El combate dura 6 segundos

                    if (random.nextBoolean()) {
                        // 50% de probabilidad: El saqueador PIERDE
                        ataqueExitoso = false;
                        log.escribir(id + " ha sido derrotado por " + patrullaDefensora.getIdPatrulla());
                    } else {
                        // 50% de probabilidad: El saqueador GANA
                        // Cambiamos el estado de la patrulla para que su hilo se vaya a la Zona de Recuperación
                        patrullaDefensora.serDerrotada();
                        log.escribir(id + " ha destruido a " + patrullaDefensora.getIdPatrulla());
                    }
                } else {
                    // Si no hay patrullas, espera 1 segundo y asalta
                    dormirConPausa(1000);
                }

                // 5. FASE DE ASALTO (Solo si ganó el combate o no había defensa)
                if (ataqueExitoso) {
                    // Expulsa a todos los delegados (ellos solos se irán a Recuperación en sus respectivos hilos)
                    for (DelegadoComercial delegado : objetivo.getDelegadosPresentes()) {
                        delegado.serExpulsado();
                    }

                    // Roba recursos si es un depósito
                    if (atacaDeposito) {
                        int cantidadARobar = 10 + random.nextInt(21); // Entre 10 y 30
                        int robado = ((Deposito) objetivo).robarRecurso(cantidadARobar);
                        log.escribir(id + " ha saqueado " + robado + " uds en " + objetivo.getId());
                    } else {
                        log.escribir(id + " ha interrumpido la extracción en " + objetivo.getId());
                    }

                    objetivo.finalizarAtaque();
                    log.escribir(id + " regresa victorioso a la base.");
                    dormirConPausa(10000); // Espera 10s tras ataque exitoso

                } else {
                    // Si perdió el combate, se retira
                    objetivo.finalizarAtaque();
                    log.escribir(id + " huye a la base para reparaciones.");
                    dormirConPausa(20000); // Espera 20s tras ser derrotado
                }
            }
        } catch (InterruptedException e) {
            log.escribir("Hilo interrumpido para el saqueador " + id);
        }
    }
}
