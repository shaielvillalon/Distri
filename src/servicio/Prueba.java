package servicio;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/prueba")
public class Prueba {

    @GET
    public String test() {
        return "FUNCIONA";
    }
}