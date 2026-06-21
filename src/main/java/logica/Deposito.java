/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package logica;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;

/**
 *
 * @author hecto
 */
public class Deposito extends Zona {

    private final AtomicInteger cantidadAlmacenada;
    private final int capacidadAlmacen;

    // Condición extra para cuando el depósito no tiene hueco físico para los recursos
    private final Condition esperaEspacioFisico;

    // El depósito siempre permite 3 delegados simultáneos según el enunciado
    public Deposito(String id, int capacidadAlmacen, int cantidadInicial) {
        super(id, 3);
        this.capacidadAlmacen = capacidadAlmacen;
        this.cantidadAlmacenada = new AtomicInteger(cantidadInicial);

        // Inicializamos la condición usando el cerrojo heredado de Zona
        this.esperaEspacioFisico = getCerrojo().newCondition();
    }

    public int getCantidadAlmacenada() {
        return cantidadAlmacenada.get();
    }

    public int getCapacidadAlmacen() {
        return capacidadAlmacen;
    }

    // Método que usará el Delegado Comercial para guardar lo extraído
    public void depositarRecurso(int cantidadAIntroducir) throws InterruptedException {
        getCerrojo().lock();
        try {
            // Si lo que quiero meter supera el hueco disponible, me bloqueo en orden
            while (cantidadAlmacenada.get() + cantidadAIntroducir > capacidadAlmacen) {
                esperaEspacioFisico.await();
            }

            // Si hay hueco, lo sumo de forma atómica
            cantidadAlmacenada.addAndGet(cantidadAIntroducir);

        } finally {
            getCerrojo().unlock();
        }
    }

    // Método que usará el Saqueador para robar
    public int robarRecurso(int cantidadARobar) {
        getCerrojo().lock();
        try {
            int robado = 0;
            // Si hay menos de lo que quiere robar, se lleva solo lo que hay
            if (cantidadAlmacenada.get() < cantidadARobar) {
                robado = cantidadAlmacenada.get();
                cantidadAlmacenada.set(0);
            } else {
                robado = cantidadARobar;
                cantidadAlmacenada.addAndGet(-cantidadARobar);
            }

            // Al robar, hemos hecho hueco físico en el almacén. 
            // Avisamos a los delegados que estaban esperando para depositar
            esperaEspacioFisico.signalAll();

            return robado;
        } finally {
            getCerrojo().unlock();
        }
    }

    // Método para la botonera RMI (vaciar depósito)
    public void vaciarDeposito() {
        getCerrojo().lock();
        try {
            cantidadAlmacenada.set(0);
            esperaEspacioFisico.signalAll();
        } finally {
            getCerrojo().unlock();
        }
    }
}
