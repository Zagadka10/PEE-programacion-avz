/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package comun;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 *
 * @author hecto
 */
public interface InterfazFederacion extends Remote{
    // --- CONSULTAS GLOBALES DE HILOS Y ZONAS ---
    int getTotalPatrullas() throws RemoteException;
    int getNumeroDelegadosEnZona(String nombreZona) throws RemoteException;
    int getNumeroPatrullasEnZona(String nombreZona) throws RemoteException;
    
    // --- CONSULTAS DE DEPÓSITOS Y RECURSOS ---
    int getDelegadosEnDeposito(int numeroDeposito) throws RemoteException;
    int getRecursoAlmacenado(int numeroDeposito) throws RemoteException;
    
    // --- ACCIONES REMOTAS (BOTONERA) ---
    void vaciarDeposito(int numeroDeposito) throws RemoteException;
    void pausarSimulacion() throws RemoteException;
    void reanudarSimulacion() throws RemoteException;
}
