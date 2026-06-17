/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package logica;

import comun.InterfazFederacion;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 * @author hecto
 */
public class Servidor extends UnicastRemoteObject implements InterfazFederacion{
    private final ArrayList<Zona> zonas;
    private final Deposito[] depositos;
    private final GestorEventos gestor;
    private final CopyOnWriteArrayList<PatrullaFederal> listaPatrullas;

    public Servidor(ArrayList<Zona> zonas, Deposito[] depositos, GestorEventos gestor, 
                    CopyOnWriteArrayList<PatrullaFederal> listaPatrullas) throws RemoteException {
        super(); 
        this.zonas = zonas;
        this.depositos = depositos;
        this.gestor = gestor;
        this.listaPatrullas = listaPatrullas;
    }

    @Override
    public int getTotalPatrullas() throws RemoteException {
        return listaPatrullas.size();
    }

    @Override
    public int getDelegadosEnDeposito(int numeroDeposito) throws RemoteException {
        // depósitos: 1=Cristal, 2=Mineral, 3=Plasma (índices 0, 1, 2)
        if (numeroDeposito >= 1 && numeroDeposito <= depositos.length) {
            return depositos[numeroDeposito - 1].getNumeroDelegados();
        }
        return 0;
    }

    @Override
    public int getNumeroDelegadosEnZona(String nombreZona) throws RemoteException {
        // Busca en el ArrayList la zona por su nombre y devuelve su aforo
        for (Zona z : zonas) {
            if (z.getId().equalsIgnoreCase(nombreZona)) {
                return z.getNumeroDelegados();
            }
        }
        return 0;
    }

    @Override
    public int getNumeroPatrullasEnZona(String nombreZona) throws RemoteException {
        // OJO: Para que esto compile, debes haber añadido un método getNumeroPatrullas() a la clase Zona
        for (Zona z : zonas) {
            if (z.getId().equalsIgnoreCase(nombreZona)) {
                return z.getNumeroPatrullas();
            }
        }
        return 0;
    }

    @Override
    public int getRecursoAlmacenado(int numeroDeposito) throws RemoteException {
        if (numeroDeposito >= 1 && numeroDeposito <= depositos.length) {
            return depositos[numeroDeposito - 1].getCantidadAlmacenada();
        }
        return 0;
    }

    @Override
    public void vaciarDeposito(int numeroDeposito) throws RemoteException {
        if (numeroDeposito >= 1 && numeroDeposito <= depositos.length) {
            depositos[numeroDeposito - 1].vaciarDeposito();
        }
    }

    @Override
    public void pausarSimulacion() throws RemoteException {
        gestor.setPausado(true);
    }

    @Override
    public void reanudarSimulacion() throws RemoteException {
        gestor.setPausado(false);
    }
}
