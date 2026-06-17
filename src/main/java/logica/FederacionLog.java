package logica;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 *
 * @author hecto
 */
public class FederacionLog {

    private final BlockingQueue<String> colaMensajes;
    private PrintWriter escritor;
    private final DateTimeFormatter formatoFecha;

    //hilo que se va a encargar de escribir de la cola a el archivo
    private final Thread hiloEscritor;

    public FederacionLog() {
        this.colaMensajes = new LinkedBlockingQueue<>();
        this.formatoFecha = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        try {
            this.escritor = new PrintWriter(new FileWriter("federacion_galactica.txt", true), true);
        } catch (IOException e) {
            System.err.println("Error al abrir log: " + e.getMessage());
        }

        this.hiloEscritor = new Thread(this::maestroEscritor, "Master-Thread");

        this.hiloEscritor.setDaemon(true);
        this.hiloEscritor.start();
    }

    public void escribir(String msj) {
        String marcatiempo = LocalDateTime.now().format(formatoFecha);
        String msjfinal = "[" + marcatiempo + "] " + msj + ".";

        try {
            colaMensajes.put(msjfinal);

        } catch (InterruptedException ie) {
            System.out.println(ie.getMessage());
        }
    }

    public void maestroEscritor() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                String msj = colaMensajes.take();

                if (escritor != null) {
                    escritor.println(msj);
                }
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            System.out.println("Problema r/w de " + ie.getMessage());
        } finally {
            if (escritor != null) {
                escritor.close();
            }
        }
    }

}
