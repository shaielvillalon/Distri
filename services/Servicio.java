package services;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import modelo.Proceso;

@Path("servicio")

public class Servicio {

	static List<Proceso> procesos = new ArrayList<>();
	static {
		for (int i=0; i<6; i++) {
			procesos.add(new Proceso(i+1));
		}
	}
	
	@GET
	@Path("hola")
	@Produces(MediaType.TEXT_PLAIN)
	public String hola() {
		return "Servidor funcionando";
	}
	
	@GET
	@Path("estado")
	@Produces(MediaType.TEXT_PLAIN)
	public String estado() {
		String salida = "";
		
		for (Proceso p : procesos) {
			salida += "Proceso " + p.getProcesoId() + " -> valor=" + p.getVariable() + "\n";
		}
		
		return salida;
	}
	
}
