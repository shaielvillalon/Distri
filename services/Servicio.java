package services;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
//import java.util.HashMap;
import java.util.List;
//import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;


import modelo.Proceso;

@Path("servicio")


public class Servicio {
	
	private static final String[] URLS = {
			"http://192.168.1.253:8080/practicaObligatoria/rest/servicio/",
			"http://192.168.1.188:8080/practicaObligatoria/rest/servicio/",
			"http://192.168.1.200:8080/practicaObligatoria/rest/servicio/"
		};
		
		// Lista estática de procesos del sistema
		private static final int TOTAL = 6;
		
		
		static List<Proceso> procesos = new ArrayList<>();
		private static int indiceMaquina;
		
		static List<Integer> confirmaciones = new ArrayList<>();
		private static boolean consensoNotificado = false;
		private static int valorConsenso = -1;
		
		static {
			indiceMaquina = detectarIndice();
			// Máquina 1 -> procesos 1 y 2
			// Máquina 2 -> procesos 3 y 4
			// Máquina 3 -> procesos 5 y 6
			
			int idLocal = indiceMaquina * 2 + 1; 
			
			for (int i = idLocal; i <= idLocal + 1; i++) {
				Proceso p = new Proceso(i, TOTAL, indiceMaquina);
				p.setServicios(URLS);
				procesos.add(p);
				p.start();
			}
		}
		
		
		/* Detecta el índice de la máquina comparando sus direcciones IP con las URLs configuradas 
		 * Si no encuentra coincidencia, usa la primera máquina como valor por defecto */
		private static int detectarIndice() {
			try {
				Enumeration<NetworkInterface> interfaces =
						NetworkInterface.getNetworkInterfaces();
				
				for (NetworkInterface ni : Collections.list(interfaces)) {
					if (!ni.isUp() || ni.isLoopback()) continue;
					
					for (InetAddress addr : Collections.list(ni.getInetAddresses())) {
						String ip = addr.getHostAddress();
						for (int i = 0; i<URLS.length; i++) {
							if (URLS[i].contains(ip)) return i;
						}
					}
				}
			} catch (Exception e) {
				System.err.println("Error detectando IP: " + e.getMessage());
			}
			System.err.println("IP no reconocida, usando máquina 1 por defecto");
			return 0;
		}
		
		// Convierte una lista de enteros en una cadena separada por comas.
		private String listaToString(List<Integer> lista) {
			if (lista.isEmpty()) return "-";
			
			StringBuilder sb = new StringBuilder();
			for (int i=0; i<lista.size(); i++) {
				sb.append(lista.get(i));
				if (i<lista.size()-1) sb.append(",");
			}
			return sb.toString();
		}
		
	//Endpoint de prueba
	@GET
	@Path("hola")
	@Produces(MediaType.TEXT_PLAIN)
	public String hola() {
		// Método de prueba para verificar que el servicio REST está levantado
		return "Servidor funcionando -> maquina " + (indiceMaquina + 1);
	}
	
	// Devuelve el estado de todos los procesos locales.
	@GET
	@Path("estado")
	@Produces(MediaType.TEXT_PLAIN)
	public String estado() {

		StringBuilder salida = new StringBuilder();
				
		// Filas -> Recorre todos los procesos y construye una línea con su información
		for (Proceso p : procesos) {
			salida.append(String.format("%-4s %-5s %-20s %-6s\n", 
					p.getProcesoId(),
					(p.getVariable() == -1 ? "-" : p.getVariable()),
					listaToString(p.getCompromisos()),
					p.isError()));
		}
		
		// Devuelve el estado completo en texto plano
		return salida.toString();
	}
	
	/*
	 * Recibe una propuesta de valor enviada por el cliente.
	 * La propuesta se reenvía a todos los procesos locales para que inicien
	 * una nueva ronda de consenso.
	 * */
	@GET
	@Path("propuesta")
	@Produces(MediaType.TEXT_PLAIN)
	public String propuesta(@QueryParam("v") int v) {
		
		//El cliente propone el valor a todos los procesos
		for (Proceso p : procesos) {
			p.recibirPropuesta(v);
		}
		
		
		return "Propuesta enviada: " + v;
	}
	
	/*
	 * Recibe un compromiso destinado a un proceso concreto.
	 * Busca el proceso local con el id indicado y le entrega el valor recibido.
	 */
	@GET
	@Path("compromiso")
	@Produces(MediaType.TEXT_PLAIN)
	public String compromiso(@QueryParam("id") int id, @QueryParam("v") int v) {
		for (Proceso p : procesos) {
			if (p.getProcesoId() == id) {
				p.recibirCompromiso(v);
				return "OK compromiso " + v + " para proceso " + id;
			}
		}
		return "No existe un proceso con id  " + id;
	}
	
	/*
	 * Recibe una comisión destinada a un proceso concreto.
	 * Busca el proceso local con el id indicado y le entrega el valor recibido.
	 */
	@GET
	@Path("comision")
	@Produces(MediaType.TEXT_PLAIN)
	public String comision(@QueryParam("id") int id, @QueryParam("v") int v) {
		for (Proceso p : procesos) {
			if (p.getProcesoId() == id) {
				p.recibirComision(v);
				return "OK comision " + v + " para proceso " + id;
			}
		}
		return "No existe un proceso con id " + id;
	}
	
	
	/*
	 * Registra una confirmación de un valor decidido por un proceso.
	 * Cuando un mismo valor alcanza quórum de confirmaciones, se marca como
	 * consenso global confirmado.
	 */
	@GET
	@Path("confirmacion")
	@Produces(MediaType.TEXT_PLAIN)
	public synchronized String confirmacion(@QueryParam("v") int v) {
		confirmaciones.add(v);
		
		int cont = 0;
		for (Integer valor : confirmaciones) {
			if (valor == v) {
				cont ++;
			}
		}
		
		int quorum = (TOTAL / 2) + 1;
		
		if (cont >= quorum && !consensoNotificado) {
			consensoNotificado = true;
			valorConsenso = v;
			return "Consenso confirmado para valor " + v;
		}
		
		return "Ok confirmacion " + v;
	}
	
	
	/*
	 * Activa o desactiva el fallo bizantino de un proceso concreto.
	 * Si el proceso existe en esta máquina, invierte su estado de error.
	 */
	@GET
	@Path("fallo")
	@Produces(MediaType.TEXT_PLAIN)
	public String fallo(@QueryParam("id") int id) {
		
		for (Proceso p : procesos) {
			if (p.getProcesoId() == id) {
				p.setError(!p.isError());
				return "Proceso " + id + " -> error=" + p.isError();
			}
		}
		
		return "No existe un proceso con id " + id;
	}
	
	/*
	 * Reinicia la ronda de consenso sin modificar los fallos bizantinos activos.
	 * Limpia el estado temporal de los procesos y las confirmaciones globales.
	 */
	@GET
	@Path("reset")
	@Produces(MediaType.TEXT_PLAIN)
	public String reset() {
		for (Proceso p: procesos) {
			p.resetear(-1);
			//p.setError(false);
		}
		
		confirmaciones.clear();
		consensoNotificado = false;
		valorConsenso = -1;
		
		return "Sistema reiniciado";
	}
	
	/*
	 * Reinicia completamente el sistema.
	 * Además de limpiar la ronda actual, desactiva todos los fallos bizantinos.
	 */
	@GET
	@Path("resetTotal")
	@Produces(MediaType.TEXT_PLAIN)
	public String resetTotal() {
		for (Proceso p: procesos) {
			p.resetear(-1);
			p.setError(false);
		}
		
		confirmaciones.clear();
		consensoNotificado = false;
		valorConsenso = -1;
		
		return "Sistema reiniciado completamente";
	}
	
	/*
	 * Devuelve el resultado global del consenso.
	 * Si ya se alcanzó quórum de confirmaciones, informa del valor decidido.
	 */
	@GET
	@Path("resultado")
	@Produces(MediaType.TEXT_PLAIN)
	public synchronized String resultado() {
		if (consensoNotificado) {
			return ("CONSENSO ALCANZADO -> valor " + valorConsenso);
		}
		return "Todavía no hay consenso confirmado";
	}
	
	
}
