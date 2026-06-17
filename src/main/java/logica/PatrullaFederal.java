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
    private boolean derrotada = false;

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
            // INICIO DE LA PATRULLA: Empieza en el Hangar
            gestor.comprobarPausa();
            log.escribir(id + " preparando sistemas en el Hangar de Patrullas.");
            dormirConPausa(3000 + random.nextInt(3001)); // 3 a 6 segundos

            while (!Thread.currentThread().isInterrupted()) {
                
                // 1. ELEGIR ZONA A PATRULLAR (Planeta o Depósito aleatorio)
                Zona zonaDestino;
                if (random.nextBoolean()) {
                    zonaDestino = planetas[random.nextInt(planetas.length)];
                } else {
                    zonaDestino = depositos[random.nextInt(depositos.length)];
                }

                // 2. ENTRAR Y PATRULLAR
                gestor.comprobarPausa();
                zonaDestino.entrarPatrulla(this); // Aquí se bloquea si ya hay 3 patrullas
                log.escribir(id + " patrullando en " + zonaDestino.getId() + ".");
                
                dormirConPausa(2000 + random.nextInt(2001)); // Patrulla de 2 a 4 segundos

                // 3. GESTIÓN DE COMBATE
                // Si la zona es atacada mientras patrulla, no se va hasta que el ataque termine
                while (zonaDestino.isBajoAtaque()) {
                    dormirConPausa(500); // Espera expectante a que acabe el combate
                }

                // 4. SALIR DE LA ZONA
                zonaDestino.salirPatrulla(this);

                // 5. COMPROBAR SI FUE DERROTADA EN EL COMBATE
                if (derrotada) {
                    log.escribir(id + " sistemas críticos. Entrando en Zona de Recuperación.");
                    
                    dormirConPausa(8000 + random.nextInt(4001)); // Recuperación de 8 a 12 segundos
                    
                    derrotada = false; // Sistemas reparados
                    
                    log.escribir(id + " reparada. Volviendo al Hangar.");
                    dormirConPausa(3000 + random.nextInt(3001)); // Vuelve a preparar sistemas en el Hangar de 3 a 6s
                }
            }
        } catch (InterruptedException e) {
            log.escribir("Hilo interrumpido para la patrulla " + id);
        }
    }
}
