package services;

import java.util.ArrayList;
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

/* Servicio REST del sistema PBFT
 * Expone operaciones accesibles por HTTP para interactuar con los procesos:
 * comprobar si el servidor funciona, consultar el estado, proponer un valor
 * y activar o desactivar fallos bizantinos
 */

public class Servicio {
	
	private static final String[] URLS = {
		"http://192.168.1.10:8080/practicaObligatoria/rest/servicio/",
		"http://192.168.1.10:8080/practicaObligatoria/rest/servicio/",
		"http://192.168.1.10:8080/practicaObligatoria/rest/servicio/"
	};
	
	// Lista estática de procesos del sistema
	private static final int TOTAL = 6;
	
	static List<Proceso> procesos = new ArrayList<>();

	/* Inicialización de los 6 procesos del sistema
	 * Cada proceso se crea con un id del 1 al 6
	 * Después, a cada proceso se le pasa la lista completa para que pueda
	 * interactuar con el resto de nodos
	 */
	
	static {
		for (int i=0; i<6; i++) {
			procesos.add(new Proceso(i+1, TOTAL));
		}
		
		for (Proceso p : procesos) {
			p.setServicios(URLS);
		}
	}
	
	@GET
	@Path("hola")
	@Produces(MediaType.TEXT_PLAIN)
	public String hola() {
		// Método de prueba para verificar que el servicio REST está levantado
		return "Servidor funcionando";
	}
	
	@GET
	@Path("estado")
	@Produces(MediaType.TEXT_PLAIN)
	public String estado() {
		// Cadena que almacenará el estado de todos los procesos
		String salida = "id\tvalor\terror\tcompromisos\t\t\tcomisiones\n";
		
		// Recorre todos los procesos y construye una línea con su información
		for (Proceso p : procesos) {
			salida += p.getProcesoId() + "\t"
					+ p.getVariable() + "\t"
					+ p.isError() + "\t"
					+ p.getCompromisos() + "\t\t"
					+ p.getComisiones() + "\n";
		}
		
		// Devuelve el estado completo en texto plano
		return salida;
	}
	
	@GET
	@Path("propuesta")
	@Produces(MediaType.TEXT_PLAIN)
	public String propuesta(@QueryParam("v") int v) {
		
		//Reinicia todos los procesos para comenzar una nueva ronda
		for (Proceso p : procesos) {
			p.resetear(v);
		}
		
		//El cliente propone el valor a todos los procesos
		for (Proceso p : procesos) {
			p.propuesta(v);
		}
		
		//Comprobar consenso final
		/*Map<Integer, Integer> contador = new HashMap<>();
		
		for (Proceso p : procesos) {
			int val = p.getVariable();
			if (val != -1) {
				contador.put(val, contador.getOrDefault(val, 0) + 1);
			}
		}
		
		//Buscar mayoría
		for (Integer val : contador.keySet()) {
			if (contador.get(val) >= (procesos.size() / 2 + 1)) {
				return "CONSENSO ALCANZADO para valor " + val;
			}
		}*/
		
		return "NO se alcanzó consenso";
	}
	
	
	@GET
	@Path("compromiso")
	@Produces(MediaType.TEXT_PLAIN)
	public String compromiso(@QueryParam("v") int v) {
		for (Proceso p : procesos) {
			p.compromiso(v);
		}
		return "OK compromiso " + v;
	}
	
	
	@GET
	@Path("comision")
	@Produces(MediaType.TEXT_PLAIN)
	public String comision(@QueryParam("v") int v) {
		for (Proceso p : procesos) {
			p.comision(v);
		}
		return "OK comision " + v;
	}
	
	
	
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
	
	@GET
	@Path("reset")
	@Produces(MediaType.TEXT_PLAIN)
	public String reset() {
		for (Proceso p: procesos) {
			p.resetear(-1);
			p.setError(false);
		}
		
		return "Sistema reiniciado";
	}
	
	
}
