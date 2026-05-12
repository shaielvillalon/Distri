package modelo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


public class Proceso extends Thread {

	private int id; // Identificador único del proceso dentro del sistema
	private int variable; // Valor sobre el que se aplica el consenso
	private boolean error; // Indica si el proceso está actuando con comportamiento bizantino
	
	private List<Integer> compromisos; // Valores recibidos en la fase de compromiso
	private List<Integer> comisiones; // Valores recibidos en la fase de comisión
		
	private boolean comisionEmitida; // Evita emitir varias veces la comisión cuando ya se alcanzó mayoría
	private boolean confirmacionEmitida; //Evita confirmar varias veces cuando ya se alcanzó mayoría en comisiones
	
	private Random random; //Generador aleatorio para simular fallos bizantinos
		
	private String[] urls; //URLs de los servicios REST de las máquinas
	private int indiceMiUrl; //Índica de la URL de esta máquina en el array 'urls'
	private int totalProcesos; //Nº total de procesos del sistema
	
	
	private BlockingQueue<Mensaje> colaMensajes;
	private boolean activo;
	
	
	// Clase interna para representar mensajes
	private static class Mensaje {
		String tipo;
		int valor;
		 
		Mensaje(String tipo, int valor) {
			this.tipo = tipo;
			this.valor = valor;
		}
	}
	
	
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
		
		this.comisionEmitida = false;
		this.confirmacionEmitida = false;
		
		this.random = new Random();
	
		this.colaMensajes = new LinkedBlockingQueue<>();
		this.activo = true;
	}
	
	public int getProcesoId() { // Devuelve el id del proceso
		return id;
	}
	

	public int getVariable() { //Devuelve el valor actual almacenado por el proceso
		return variable;
	}
	
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
	
	public void setServicios(String[] urls) { //Asigna las URLs de los REST
		this.urls = urls;
	}
	
	// Inserta una propuesta en la cola de mensajes del proceso
	public void recibirPropuesta(int v) {
		colaMensajes.offer(new Mensaje("PROPUESTA", v));
	}
	
	//Inserta un compromiso recibido en la cola del proceso
	public void recibirCompromiso(int v) {
		colaMensajes.offer(new Mensaje("COMPROMISO", v));
	}
	
	//Inserta un comision recibido en la cola del proceso
	public void recibirComision(int v) {
		colaMensajes.offer(new Mensaje("COMISION", v));
	}
	
	/*
	 * Bucle principal del hilo.
	 * El proceso espera mensajes en su cola interna y ejecuta la fase
	 * correspondiente según el tipo de mensaje recibido.
	 */
	@Override
	public void run() {
		
		while (activo) {
			try {
				Mensaje mensaje = colaMensajes.take();
				
				switch(mensaje.tipo) {
					
					case "PROPUESTA":
						propuesta(mensaje.valor);
						break;
				
					case "COMPROMISO":
						compromiso(mensaje.valor);
						break;
						
					case "COMISION":
						comision(mensaje.valor);
						break;
				}
			
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				activo = false;
			}
			
		}
		
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
	}
	
	
	/* Calcula el quórum necesario para alcanzar la mayoría simple
	 * P.ej. con 6 procesos el quórum será 4
	 */
	private int quorum() {
		return (totalProcesos / 2) + 1;
	}
	
	//Determina qué URL corresponde al proceso indicado.
	private String obtenerURLDeProceso(int idProceso) {
		int procesosXMaquina = totalProcesos / urls.length;
		int indice = (idProceso - 1) / procesosXMaquina;
		return urls[indice];
	}
		
	
	
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
		
		this.variable = -1;
		this.compromisos.clear();
		this.comisiones.clear();
		this.comisionEmitida = false;
		this.confirmacionEmitida = false;
		
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
	 * Cuando el proceso decide un valor, envía una confirmación
	 * a los servicios REST. El cliente consultará posteriormente
	 * el resultado confirmado por quórum.
	 */
	public synchronized void confirmacion() {
		//System.out.println("Proceso " + id + " confirma valor " + variable);
		
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
