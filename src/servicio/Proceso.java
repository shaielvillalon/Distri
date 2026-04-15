package servicio;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Proceso extends Thread {

    // Atributos del enunciado
	
    private int id;
    private int variable;                 	// -1 significa no decidido
    private boolean error;                	// true es fallo bizantino
    private List<Integer> compromisos;   	// valores recibidos en fase 1b
    private List<Integer> comisiones;     	// valores recibidos en fase 2a

    // Atributos auxiliares
    
    private List<Proceso> procesos;       	// red local para pruebas sin REST
    private boolean comisionEmitida;		// evitar que una vez llegada la mayoria, se repita el proceso
    private boolean confirmacionEmitida;
    private Random random;					// simulacion del fallo bizantino
    private int valorPropuesto;
    
    // Constructor
    
    public Proceso(int id) {
        this.id = id;
        this.variable = -1;
        this.error = false;
        this.compromisos = new ArrayList<>();
        this.comisiones = new ArrayList<>();
        this.procesos = new ArrayList<>();
        this.comisionEmitida = false;
        this.confirmacionEmitida = false;
        this.random = new Random();
    }

    // Getters y setters
    
    public int getIdProceso() {
        return id;
    }

    public int getVariable() {
        return variable;
    }

    public boolean isError() {
        return error;
    }

    public void setError(boolean error) {
        this.error = error;
    }

    public List<Integer> getCompromisos() {
        return new ArrayList<>(compromisos);
    }

    public List<Integer> getComisiones() {
        return new ArrayList<>(comisiones);
    }

    public void setProcesos(List<Proceso> procesos) {
        this.procesos = procesos;
    }

    // Método run
    
    @Override
    public void run() {
        // Lo dejamos vacío, asi queda sin espera activa
    }

   /* // Preparar el proceso para una ronda nueva
    public synchronized void reiniciarRonda() {
        this.variable = -1;
        this.compromisos.clear();
        this.comisiones.clear();
        this.comisionEmitida = false;
        this.confirmacionEmitida = false;
    }*/
    
    // Cada proceso resetea su estado
    public synchronized void resetear(int v) {
        this.variable = -1;
        this.compromisos.clear();
        this.comisiones.clear();
        this.comisionEmitida = false;
        this.confirmacionEmitida = false;
        this.valorPropuesto = v;
    }
    
    // Calculo del quórum
    
    private int quorum() {
        return (procesos.size() / 2) + 1;
    }

    // Devuelve el valor que ha alcanzado quórum o null si ninguno lo ha alcanzado
    
    private Integer valorMayoritario(List<Integer> valores) {
        Map<Integer, Integer> contador = new HashMap<>();

        for (Integer v : valores) {
            int repeticiones = contador.getOrDefault(v, 0) + 1;
            contador.put(v, repeticiones);

            if (repeticiones >= quorum()) {
                return v;	// el valor que ha alcanzado quorum
            }
        }

        return null;	// si no alcanza quorum aun
    }
    
 // 1a - El cliente llama a propuesta(v) en TODOS los procesos
    // multidifunde compromiso(v) a todos
    public synchronized void propuesta() {
        for (Proceso p : procesos) {
            int valorAEnviar = (this.error && p.getIdProceso() != this.id)
                ? random.nextInt(101)
                : valorPropuesto;
            p.compromiso(valorAEnviar);
        }
    }

    // 1b - Recibe un compromiso (de cualquier proceso, incluido uno mismo)
    // Si hay quórum, emite comisión a todos
    public synchronized void compromiso(int v) {
        compromisos.add(v);

        Integer valorConQuorum = valorMayoritario(compromisos);

        if (valorConQuorum != null && !comisionEmitida) {
            comisionEmitida = true;
            for (Proceso p : procesos) {
                p.comision(valorConQuorum);
            }
        }
    }

    // 2a - Recibe una comisión (de cualquier proceso)
    // Si hay quórum, decide el valor y notifica al cliente
    public synchronized void comision(int v) {
        comisiones.add(v);

        Integer valorConQuorum = valorMayoritario(comisiones);

        if (valorConQuorum != null && !confirmacionEmitida) {
            this.variable = valorConQuorum;
            confirmacionEmitida = true;
            confirmacion();
        }
    }

    // 2b - Notifica al cliente que el valor ha sido decidido
    public synchronized void confirmacion() {
        System.out.println("Proceso " + id + " confirma valor " + variable);
    }
}