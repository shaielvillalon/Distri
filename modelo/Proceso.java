package modelo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/* Clase que representa un proceso o nodo participante en el consenso PBFT
 * Cada proceso mantiene su id, el valor decidido, si actúa con 
 * fallo bizantino y la información recibida en las fases de compromiso y comisión
 * 
 * Esta versión implementa la lógica local del algoritmo, sin comunicación REST real
 * entre máquinas todavía. La lista "procesos" simula la red de nodos
 * */

public class Proceso extends Thread {

	private int id; // Identificador único del proceso dentro del sistema
	private int variable; // Valor sobre el que se aplica el consenso; -1 indica "no decidido"
	private boolean error; // Indica si el proceso está actuando con comportamiento bizantino
	
	private List<Integer> compromisos; // Valores recibidos en la fase de compromiso
	private List<Integer> comisiones; // Valores recibidos en la fase de comisión
	
	//private List<Proceso> procesos; // Lista de procesos del sistema; se usa para simular la red localmente
	
	private boolean comisionEmitida; // Evita emitir varias veces la comisión cuando ya se alcanzó mayoría
	private boolean confirmacionEmitida; //Evita confirmar varias veces cuando ya se alcanzó mayoría en comisiones
	
	private Random random; //Generador aleatorio para simular fallos bizantinos
	
	//private int valorPropuesto; //Valor propuesto en la ronda actual
	
	private String[] urls; //URLs de los servicios REST de las máquinas
	private int indiceMiUrl; //Índica de la URL de esta máquina en el array 'urls'
	private int totalProcesos; //Nº total de procesos del sistema
	
	/* Constructor del proceso
	 * Inicializa el id, deja la variable sin decidir y crea
	 * las estructuras internas necesarias para la ejecución del consenso
	 */
	public Proceso(int id, int totalProcesos, int indiceMiUrl) {
		this.id = id;
		this.totalProcesos = totalProcesos;
		this.indiceMiUrl = indiceMiUrl;
		this.variable = -1;
		this.error = false;
		this.compromisos = new ArrayList<>();
		this.comisiones = new ArrayList<>();
		//this.procesos = new ArrayList<>();
		this.comisionEmitida = false;
		this.confirmacionEmitida = false;
		this.random = new Random();
		//this.valorPropuesto = -1;
	}
	
	public int getProcesoId() { // Devuelve el id del proceso
		return id;
	}
	

	public int getVariable() { //Devuelve el valor actual almacenado por el proceso
		return variable;
	}
	
	/*public void setVariable(int v) { // Modifica el valor actual del proceso
		this.variable = v;
	}*/
	
	public boolean isError() { // Indica si el proceso está en modo fallo bizantino
		return error;
	}
	
	public void setError (boolean error) { // Cambia el estado de fallo del proceso
		this.error = error;
	}
	
	public List<Integer> getCompromisos() { // Devuelve una copia de la lista de compromisos recibidos
		return new ArrayList<>(compromisos);
	}
	
	public List<Integer> getComisiones() { // Devuelve una copia de la lista de comisiones recibidas
		return new ArrayList<>(comisiones);
	}
	
	public void setServicios(String[] urls) {
		this.urls = urls;
	}
	
	/*public void setProcesos(List<Proceso> procesos) { //Asigna la red de procesos con la que este nodo interactúa
		this.procesos = procesos;
	}*/
	
	
	@Override
	public void run() {
		// No se implementa comportamiento activo del hilo en esta fase
		// La lógica del consenso se activa mediante lamadas a métodos
	}
	
	/* Reinicia el estado del proceso para comenzar una nueva ronda de consenso.
	 * Se limpia la información temporal y se guarda el nuevo valor propuesto
	 */
	public synchronized void resetear(int v) {
		this.variable = -1;
		this.compromisos.clear();
		this.comisiones.clear();
		this.comisionEmitida = false;
		this.confirmacionEmitida = false;
		//this.valorPropuesto = v;
	}
	
	
	/* Calcula el quórum necesario para alcanzar la mayoría simple
	 * P.ej. con 6 procesos el quórum será 4
	 */
	private int quorum() {
		return (totalProcesos / 2) + 1;
	}
	
	private String obtenerURLDeProceso(int idProceso) {
		int procesosXMaquina = totalProcesos / urls.length;
		int indice = (idProceso - 1) / procesosXMaquina;
		return urls[indice];
	}
		
	
	/*private int f() {
		return (procesos.size() - 1) / 3;
	}
	
	private int quorumPBFT() {
		return 2 * f() + 1;
	}*/
	
	
	/* Devuelve el valor que ha alcanzado quórum dentro de una lista de valores
	 * Si ningún valor llega a mayoría, devuelve null
	 */
	private Integer valorMayoritario(List<Integer> valores) {
		Map<Integer, Integer> contador = new HashMap<>();
		
		for (Integer v : valores) {
			int repeticiones = contador.getOrDefault(v, 0) + 1;
			contador.put(v, repeticiones);
			
			if (repeticiones >= quorum()) {
				return v;
			}
		}
		return null;
	}
	
	
	/* Fase 1a: propuesta.
	 * El cliente propone un valor y este proceso lo multidifunde al resto
	 * mediante mensajes de compromiso.
	 * 
	 * Si el proceso está en fallo bizantino, envía valores aleatorios a los
	 * demás nodos. A sí mismo se puede mandar el valor correcto.
	 */
	public synchronized void propuesta(int v) {
		
		if (urls == null) return;
		
		for (int destino=1; destino <= totalProcesos; destino++) {
			int valorEnviar;
			
			if (this.error && destino!=this.id) {
				valorEnviar = random.nextInt(101); // valor aleatorio
			} else {
				valorEnviar = v;
			}
			
			String urlDestino = obtenerURLDeProceso(destino);
			enviar(urlDestino + "compromiso?id=" + destino + "&v=" + valorEnviar);
		}
	}
	
	
	/*
	 * Fase 1b: compromiso.
	 * El proceso recibe un valor comprometido. Si algún valor alcanza quórum,
	 * emite una comisión a todos los nodos.
	 */
	public synchronized void compromiso(int v) {
		compromisos.add(v);
		
		Integer valorConQuorum = valorMayoritario(compromisos);
		
		if (valorConQuorum != null && !comisionEmitida) {
			comisionEmitida = true;
			
			
			for (int destino=1; destino<=totalProcesos; destino++) {
				String urlDestino = obtenerURLDeProceso(destino);
				enviar(urlDestino + "comision?id=" + destino + "&v=" + valorConQuorum);
			}
			
		}
	}
	
	
	/*
	 * Fase 2a: comisión.
	 * El proceso recibe una comisión. Si algún valor alcanza quórum en esta fase,
	 * el proceso decide dicho valor y emite confirmación.
	 */
	public synchronized void comision (int v) {
		comisiones.add(v);
		
		Integer valorConQuorum = valorMayoritario(comisiones);
		
		if (valorConQuorum != null && !confirmacionEmitida) {
			this.variable = valorConQuorum;
			confirmacionEmitida = true;
			confirmacion();
		}
	}
	
	
	/*
	 * Fase 2b: confirmación.
	 * En esta versión local simplemente se muestra por consola que el proceso
	 * ha decidido un valor. Más adelante esto se conectará con el cliente.
	 */
	public synchronized void confirmacion() {
		System.out.println("Proceso " + id + " confirma valor " + variable);
		
		for (String url : urls) {
			enviar(url + "confirmacion?v=" + variable);
		}
	}

	
	private void enviar(String urlStr) {
		new Thread(() -> {
			try {
				java.net.URL url = new java.net.URL(urlStr);
				java.net.HttpURLConnection c = (java.net.HttpURLConnection) url.openConnection();
				c.setConnectTimeout(3000);
				c.setReadTimeout(3000);
				c.setRequestMethod("GET");
				c.getInputStream().close();
				c.disconnect();
			} catch (Exception e) {
				System.out.println("Error enviado a " + urlStr);
			}
		}).start();
	}
	
	
}
