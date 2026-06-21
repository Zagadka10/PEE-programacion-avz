package logica;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Zona {

    private final String id;
    private final int capacidadMaximaDelegados;

    // Colecciones seguras para los hilos presentes
    private final CopyOnWriteArrayList<DelegadoComercial> delegadosPresentes;
    private final CopyOnWriteArrayList<Saqueador> saqueadoresPresentesList = new CopyOnWriteArrayList<>();
    private int patrullasPresentes = 0; // Máximo 3 por zona según el enunciado

    // --- HERRAMIENTAS DE SINCRONIZACIÓN ---
    // con true activamos modo fair, cola FIFO estricta.
    private final ReentrantLock cerrojo = new ReentrantLock(true);
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
    
    public void entrarSaqueador(Saqueador s) {
        saqueadoresPresentesList.add(s);
    }

    public void salirSaqueador(Saqueador s) {
        saqueadoresPresentesList.remove(s);
    }

    public int getNumeroSaqueadores() {
        return saqueadoresPresentesList.size();
    }

    // Método para que un Delegado intente entrar a la zona
    public void entrarDelegado(DelegadoComercial delegado) throws InterruptedException {
        cerrojo.lock();
        try {
            // Si hay un ataque, nadie entra. Se quedan esperando.
            while (bajoAtaque) {
                colaAtaque.await();
            }

            // Si la zona está llena, esperan en orden estricto de llegada.
            while (delegadosPresentes.size() >= capacidadMaximaDelegados) {
                colaDelegados.await();
            }

            // Entra en la zona
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
        cerrojo.lock();
        try {
            return bajoAtaque;
        } finally {
            cerrojo.unlock();
        }
    }

    public CopyOnWriteArrayList<DelegadoComercial> getDelegadosPresentes() {
        return delegadosPresentes;
    }

    // El Saqueador usa esto para coger a la primera patrulla VÁLIDA que pille y pelear
    public PatrullaFederal obtenerPatrullaDefensora() {
        for (PatrullaFederal p : patrullasPresentesList) {
            // Solo devuelve la patrulla si aún no ha sido derrotada
            if (!p.isDerrotada()) {
                return p;
            }
        }
        return null;
    }

    // Ahora los saqueadores hacen cola si la zona ya está bajo ataque
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
    
    public CopyOnWriteArrayList<PatrullaFederal> getPatrullasPresentesList() {
        return patrullasPresentesList;
    }
    
    public int getNumeroDelegadosEnCola() {
        cerrojo.lock();
        try {
            // Sumamos los que esperan por aforo + los que esperan a que acabe un ataque
            return cerrojo.getWaitQueueLength(colaDelegados) + cerrojo.getWaitQueueLength(colaAtaque);
        } finally {
            cerrojo.unlock();
        }
    }
    
    // PARA GUI
    // Devuelve un texto con los IDs de los delegados presentes
    public String getListaIdsDelegados() {
        if (delegadosPresentes.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < delegadosPresentes.size(); i++) {
            sb.append(delegadosPresentes.get(i).getIdDelegado());
            if (i < delegadosPresentes.size() - 1) sb.append(", ");
        }
        return sb.toString();
    }

    // Devuelve un texto con los IDs de las patrullas presentes
    public String getListaIdsPatrullas() {
        if (patrullasPresentesList.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < patrullasPresentesList.size(); i++) {
            sb.append(patrullasPresentesList.get(i).getIdPatrulla());
            if (i < patrullasPresentesList.size() - 1) sb.append(", ");
        }
        return sb.toString();
    }

    // Devuelve un texto con los IDs de los saqueadores presentes
    public String getListaIdsSaqueadores() {
        if (saqueadoresPresentesList.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < saqueadoresPresentesList.size(); i++) {
            sb.append(saqueadoresPresentesList.get(i).getIdSaqueador());
            if (i < saqueadoresPresentesList.size() - 1) sb.append(", ");
        }
        return sb.toString();
    }
    
}
