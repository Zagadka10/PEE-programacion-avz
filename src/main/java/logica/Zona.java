package logica;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Zona {

    private final String id;
    private final int capacidadMaximaDelegados;

    // Colecciones seguras para los hilos presentes
    private final CopyOnWriteArrayList<DelegadoComercial> delegadosPresentes;
    private int patrullasPresentes = 0; // Máximo 3 por zona según el enunciado

    // --- HERRAMIENTAS DE SINCRONIZACIÓN ---
    // con true activamos modo fair, cola FIFO estricta.
    final ReentrantLock cerrojo = new ReentrantLock(true);
    private final Condition colaDelegados = cerrojo.newCondition();
    private final Condition colaAtaque = cerrojo.newCondition();

    // Lista para guardar las patrullas que están vigilando esta zona
    private final CopyOnWriteArrayList<PatrullaFederal> patrullasPresentesList = new CopyOnWriteArrayList<>();
    // Condición extra para la cola de saqueadores
    private final Condition esperaSaqueador = cerrojo.newCondition();

    private final Condition colaPatrullas = cerrojo.newCondition();

    private boolean bajoAtaque = false;

    public Zona(String id, int capacidadMaximaDelegados) {
        this.id = id;
        this.capacidadMaximaDelegados = capacidadMaximaDelegados;
        this.delegadosPresentes = new CopyOnWriteArrayList<>();
    }

    public String getId() {
        return id;
    }

    // Método para que un Delegado intente entrar a la zona
    public void entrarDelegado(DelegadoComercial delegado) throws InterruptedException {
        cerrojo.lock();
        try {
            // 1. Si hay un ataque, nadie entra. Se quedan esperando.
            while (bajoAtaque) {
                colaAtaque.await();
            }

            // 2. Si la zona está llena, esperan en orden estricto de llegada.
            while (delegadosPresentes.size() >= capacidadMaximaDelegados) {
                colaDelegados.await();
            }

            // 3. Entra en la zona
            delegadosPresentes.add(delegado);

        } finally {
            cerrojo.unlock();
        }
    }

    // Método para salir de la zona
    public void salirDelegado(DelegadoComercial delegado) {
        cerrojo.lock();
        try {
            delegadosPresentes.remove(delegado);
            // Al salir, avisamos al siguiente en la cola para que entre
            colaDelegados.signal();
        } finally {
            cerrojo.unlock();
        }
    }
    
    public ReentrantLock getCerrojo() {
        return cerrojo;
    }

    public boolean isBajoAtaque() {
        return bajoAtaque;
    }

    public CopyOnWriteArrayList<DelegadoComercial> getDelegadosPresentes() {
        return delegadosPresentes;
    }

    // El Saqueador usa esto para coger a la primera patrulla que pille y pelear
    public PatrullaFederal obtenerPatrullaDefensora() {
        if (!patrullasPresentesList.isEmpty()) {
            return patrullasPresentesList.get(0);
        }
        return null;
    }

    // MODIFICADO: Ahora los saqueadores hacen cola si la zona ya está bajo ataque
    public void iniciarAtaque() throws InterruptedException {
        cerrojo.lock();
        try {
            while (bajoAtaque) {
                esperaSaqueador.await(); // Hace cola si otro saqueador ya está atacando
            }
            this.bajoAtaque = true;
        } finally {
            cerrojo.unlock();
        }
    }

    // Al acabar el ataque, avisamos al siguiente saqueador en la cola
    public void finalizarAtaque() {
        cerrojo.lock();
        try {
            this.bajoAtaque = false;
            colaAtaque.signalAll();    // Despierta a los delegados
            esperaSaqueador.signal();  // Despierta al siguiente saqueador en la cola
        } finally {
            cerrojo.unlock();
        }
    }

    public void entrarPatrulla(PatrullaFederal patrulla) throws InterruptedException {
        cerrojo.lock();
        try {
            // Si ya hay 3 patrullas en esta zona, espera su turno en orden
            while (patrullasPresentesList.size() >= 3) {
                colaPatrullas.await();
            }
            patrullasPresentesList.add(patrulla);
        } finally {
            cerrojo.unlock();
        }
    }

    public void salirPatrulla(PatrullaFederal patrulla) {
        cerrojo.lock();
        try {
            patrullasPresentesList.remove(patrulla);
            colaPatrullas.signal(); // Avisa a la siguiente patrulla que quiera entrar
        } finally {
            cerrojo.unlock();
        }
    }
    
    public int getNumeroDelegados() {
        return delegadosPresentes.size();
    }

    public int getNumeroPatrullas() {
        return patrullasPresentesList.size();
    }
    
    public int getNumeroDelegadosEnCola() {
        cerrojo.lock();
        try {
            // Devuelve la longitud de la cola de la condición "colaDelegados"
            return cerrojo.getWaitQueueLength(colaDelegados);
        } finally {
            cerrojo.unlock();
        }
    }
    
}
