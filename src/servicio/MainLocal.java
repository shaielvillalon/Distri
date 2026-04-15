package servicio;

import java.util.ArrayList;
import java.util.List;

public class MainLocal {

    public static void main(String[] args) {
        List<Proceso> procesos = new ArrayList<>();

        int numeroProcesos = 4;

        for (int i = 0; i < numeroProcesos; i++) {
            procesos.add(new Proceso(i));
        }

        for (Proceso p : procesos) {
            p.setProcesos(procesos);
        }

        Cliente cliente = new Cliente(procesos);
        cliente.iniciar();
    }
}