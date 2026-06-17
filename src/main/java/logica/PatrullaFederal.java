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
public class PatrullaFederal extends Thread{
    private final String id;
    private final Zona[] planetas;
    private final Deposito[] depositos;
    private final Zona hangar;
    private final Zona zonaRecuperacion;
    
    private final FederacionLog log;
    private final GestorEventos gestor;
    private final Random random = new Random();

    // Bandera para saber si el saqueador nos ha ganado el combate
    private volatile boolean derrotada = false;

    public PatrullaFederal(String id, Zona[] planetas, Deposito[] depositos, Zona hangar, Zona zonaRecuperacion, FederacionLog log, GestorEventos gestor) {
        this.id = id;
        this.planetas = planetas;
        this.depositos = depositos;
        this.hangar = hangar;
        this.zonaRecuperacion = zonaRecuperacion;
        this.log = log;
        this.gestor = gestor;
    }

    public String getIdPatrulla() {
        return id;
    }

    // Método que invocará el Saqueador si gana el combate (50% probabilidad)
    public void serDerrotada() {
        this.derrotada = true;
    }
    
    public boolean isDerrotada() {
        return derrotada;
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
            gestor.comprobarPausa();
            hangar.entrarPatrulla(this); 
            log.escribir(id + " preparando sistemas en el Hangar de Patrullas.");
            dormirConPausa(3000 + random.nextInt(3001));
            hangar.salirPatrulla(this); // Salen a patrullar

            while (!Thread.currentThread().isInterrupted()) {
                
                Zona zonaDestino;
                if (random.nextBoolean()) {
                    zonaDestino = planetas[random.nextInt(planetas.length)];
                } else {
                    zonaDestino = depositos[random.nextInt(depositos.length)];
                }

                gestor.comprobarPausa();
                zonaDestino.entrarPatrulla(this); 
                log.escribir(id + " patrullando en " + zonaDestino.getId() + ".");
                
                dormirConPausa(2000 + random.nextInt(2001)); 

                while (zonaDestino.isBajoAtaque() && !derrotada) {
                    dormirConPausa(500);
                }

                zonaDestino.salirPatrulla(this);

                if (derrotada) {
                    log.escribir(id + " sistemas críticos. Entrando en Zona de Recuperación.");
                    zonaRecuperacion.entrarPatrulla(this); // Fichan en recuperación
                    dormirConPausa(5000 + random.nextInt(5001)); 
                    zonaRecuperacion.salirPatrulla(this);
                    
                    derrotada = false; 
                    
                    log.escribir(id + " reparada. Volviendo al Hangar.");
                    hangar.entrarPatrulla(this); // Vuelven a fichar en hangar
                    dormirConPausa(3000 + random.nextInt(3001)); 
                    hangar.salirPatrulla(this);
                }
            }
        } catch (InterruptedException e) {
            log.escribir("Hilo interrumpido para la patrulla " + id);
        }
    }
}
