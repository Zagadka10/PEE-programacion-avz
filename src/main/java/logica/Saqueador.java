/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package logica;

import java.util.ArrayList;
import java.util.Random;

/**
 *
 * @author hecto
 */
public class Saqueador extends Thread {

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

    public String getIdSaqueador() {
        return id;
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
                // ESPERA EN LA BASE
                gestor.comprobarPausa();
                baseSaqueadores.entrarSaqueador(this); // Ficha la entrada a la base
                log.escribir(id + " esperando en Base de Saqueadores.");
                dormirConPausa(3000 + random.nextInt(3001));
                baseSaqueadores.salirSaqueador(this);  // Ficha la salida

                // SELECCIÓN DE OBJETIVO
                boolean atacaDeposito = random.nextInt(100) < 70;
                Zona[] opciones = atacaDeposito ? depositos : planetas;
                Zona objetivo = null;

                List<Zona> disponibles = new ArrayList<>();
                for (Zona z : opciones) {
                    if (!z.isBajoAtaque()) {
                        disponibles.add(z);
                    }
                }
                if (!disponibles.isEmpty()) {
                    objetivo = disponibles.get(random.nextInt(disponibles.size()));
                } else {
                    objetivo = opciones[random.nextInt(opciones.length)]; // entra en cola
                }

                // INICIAR ATAQUE
                gestor.comprobarPausa();
                objetivo.entrarSaqueador(this); // El saqueador ficha en el planeta/depósito
                objetivo.iniciarAtaque();
                log.escribir(id + " inicia el ataque en " + objetivo.getId());

                // FASE DE COMBATE
                boolean ataqueExitoso = true;
                PatrullaFederal patrullaDefensora = objetivo.obtenerPatrullaDefensora();

                if (patrullaDefensora != null) {
                    log.escribir(id + " entra en combate con " + patrullaDefensora.getIdPatrulla() + " en " + objetivo.getId());
                    dormirConPausa(6000);

                    if (random.nextBoolean()) {
                        ataqueExitoso = false;
                        log.escribir(id + " ha sido derrotado por " + patrullaDefensora.getIdPatrulla());
                    } else {
                        patrullaDefensora.serDerrotada();
                        log.escribir(id + " ha destruido a " + patrullaDefensora.getIdPatrulla());
                    }
                } else {
                    dormirConPausa(1000);
                }

                // FASE DE ASALTO
                if (ataqueExitoso) {
                    for (DelegadoComercial delegado : objetivo.getDelegadosPresentes()) {
                        delegado.serExpulsado();
                    }
                    if (atacaDeposito) {
                        int cantidadARobar = 10 + random.nextInt(21);
                        int robado = ((Deposito) objetivo).robarRecurso(cantidadARobar);
                        log.escribir(id + " ha saqueado " + robado + " uds en " + objetivo.getId());
                    } else {
                        log.escribir(id + " ha interrumpido la extracción en " + objetivo.getId());
                    }

                    objetivo.finalizarAtaque();
                    objetivo.salirSaqueador(this); // Sale de la zona de ataque

                    log.escribir(id + " regresa victorioso a la base.");
                    baseSaqueadores.entrarSaqueador(this); // Vuelve a la base
                    dormirConPausa(10000);
                    baseSaqueadores.salirSaqueador(this);

                } else {
                    objetivo.finalizarAtaque();
                    objetivo.salirSaqueador(this); // Sale de la zona de ataque

                    log.escribir(id + " huye a la base para reparaciones.");
                    baseSaqueadores.entrarSaqueador(this); // Vuelve a la base
                    dormirConPausa(20000);
                    baseSaqueadores.salirSaqueador(this);
                }
            }
        } catch (InterruptedException e) {
            log.escribir("Hilo interrumpido para el saqueador " + id);
        }
    }
}
