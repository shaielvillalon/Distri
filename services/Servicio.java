package services;

import java.util.ArrayList;
import java.util.List;

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
	
	// Lista estática de procesos del sistema
	static List<Proceso> procesos = new ArrayList<>();

	/* Inicialización de los 6 procesos del sistema
	 * Cada proceso se crea con un id del 1 al 6
	 * Después, a cada proceso se le pasa la lista completa para que pueda
	 * interactuar con el resto de nodos
	 */
	static {
		for (int i=0; i<6; i++) {
			procesos.add(new Proceso(i+1));
		}
		
		for (Proceso p : procesos) {
			p.setProcesos(procesos);
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
		String salida = "id\tvalor\te\tcompromisos\tcomisiones\n";
		
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
		
		// Reinicia todos los procesos para comenzar una nueva ronda
		for (Proceso p : procesos) {
			p.resetear(v);
		}
		
		// El cliente propone el valor a todos los procesos
		for (Proceso p : procesos) {
			p.propuesta(v);
		}
		
		return "Propuesta lanzada con valor " + v;
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
	
	
}
