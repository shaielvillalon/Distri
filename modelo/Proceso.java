package modelo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


public class Proceso extends Thread {

	private int id;
	private int variable;
	private boolean error;

	private List<Integer> compromisos;
	private List<Integer> comisiones;
		
	private boolean comisionEmitida;
	private boolean confirmacionEmitida;
	
	private Random random;
	private String[] urls;
	private int indiceMiUrl;
	private int totalProcesos;
	
	private int valorPropuesto = -1;
	
	
	private BlockingQueue<Mensaje> colaMensajes;
	private boolean activo;
	
	
	private static class Mensaje {
		String tipo;
		int valor;
		 
		Mensaje(String tipo, int valor) {
			this.tipo = tipo;
			this.valor = valor;
		}
	}
	
	

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
	
	public int getProcesoId() {
		return id;
	}
	

	public int getVariable() {
		return variable;
	}
	
	public boolean isError() {
		return error;
	}
	
	public void setError (boolean error) {
		this.error = error;
	}
	
	public List<Integer> getCompromisos() {
		return new ArrayList<>(compromisos);
	}
	
	public List<Integer> getComisiones() {
		return new ArrayList<>(comisiones);
	}
	
	public void setServicios(String[] urls) {
		this.urls = urls;
	}
	

	public void recibirPropuesta(int v) {
		colaMensajes.offer(new Mensaje("PROPUESTA", v));
	}
	
	public void recibirCompromiso(int v) {
		colaMensajes.offer(new Mensaje("COMPROMISO", v));
	}
	
	public void recibirComision(int v) {
		colaMensajes.offer(new Mensaje("COMISION", v));
	}
	
	
	
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
	


	public synchronized void resetear(int v) {
		this.variable = -1;
		
		this.compromisos.clear();
		this.comisiones.clear();
		
		this.comisionEmitida = false;
		this.confirmacionEmitida = false;
	}
	
	


	private int quorum() {
		return (totalProcesos / 2) + 1;
	}
	

	private String obtenerURLDeProceso(int idProceso) {
		int procesosXMaquina = totalProcesos / urls.length;
		int indice = (idProceso - 1) / procesosXMaquina;
		return urls[indice];
	}
		
	

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
	

	public synchronized void propuesta(int v) {
		
		this.variable = -1;
		this.compromisos.clear();
		this.comisiones.clear();
		this.comisionEmitida = false;
		this.confirmacionEmitida = false;
		this.valorPropuesto = v;
	
	}
	
	
	public synchronized void emitirCompromisos() {
		
		if (urls == null || valorPropuesto == -1) return;
		
			for (int destino=1; destino <= totalProcesos; destino++) {
				int valorEnviar;
			
				if (this.error && destino!=this.id) {
					valorEnviar = random.nextInt(101); // valor aleatorio
				} else {
					valorEnviar = valorPropuesto;
				}
			
				String urlDestino = obtenerURLDeProceso(destino);
				enviar(urlDestino + "compromiso?id=" + destino + "&v=" + valorEnviar);
			}
	}
	
	

	public synchronized void compromiso(int v) {
		compromisos.add(v);
		
		if (compromisos.size() < totalProcesos) {
			return;
		}
		
		Integer valorConQuorum = valorMayoritario(compromisos);
		
		if (valorConQuorum != null && !comisionEmitida) {
			this.variable = valorConQuorum;
			comisionEmitida = true;
			
			
			for (int destino=1; destino<=totalProcesos; destino++) {
				String urlDestino = obtenerURLDeProceso(destino);
				enviar(urlDestino + "comision?id=" + destino + "&v=" + valorConQuorum);
			}
			
		} else if (!comisionEmitida) {
			comisionEmitida = true;
			
			for (String url : urls) {
				enviar(url + "sinConsenso?id=" + id);
			}
		}
	}
	
	

	public synchronized void comision (int v) {
		comisiones.add(v);
		
		
		Integer valorConQuorum = valorMayoritario(comisiones);
		
		if (valorConQuorum != null && !confirmacionEmitida) {
			//this.variable = valorConQuorum;
			confirmacionEmitida = true;
			confirmacion();
			return;
		}
			
		int restantes = totalProcesos - comisiones.size();
		
		if (valorConQuorum == null && !confirmacionEmitida && maxRepeticiones(comisiones) + restantes < quorum()) {
			
			confirmacionEmitida = true;
			
			for (String url : urls) {
				enviar(url + "sinConsenso?id=" + id);
			}
			
		}
		
	}
		
		
	private int maxRepeticiones(List<Integer> valores) {
		Map<Integer, Integer> contador = new HashMap<>();
		int maxx = 0;
		
		for (Integer v : valores) {
			int repeticiones = contador.getOrDefault(v, 0) + 1;
			contador.put(v, repeticiones);
			
			if(repeticiones > maxx) {
				maxx = repeticiones;
			}
		}
		
		return maxx;
	}
	
	

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
				System.out.println("Causa: " + e.getClass().getName() + " - " + e.getMessage());
			}
		}).start();
	}
	
	
}