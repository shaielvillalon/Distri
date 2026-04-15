package servicio;

import java.util.List;
import java.util.Scanner;

public class Cliente {

    private List<Proceso> procesos;
    private Scanner scanner;	

    // Cliente recibe los procesos 
    public Cliente(List<Proceso> procesos) {
        this.procesos = procesos;
        this.scanner = new Scanner(System.in);
    }

    public void iniciar() {
        mostrarAyuda();	// empezamos mostrando la ayuda de primeras

        while (true) {
            System.out.print("> ");
            String entrada = scanner.nextLine().trim();

            if (entrada.equalsIgnoreCase("h")) {
                mostrarAyuda();
            } else if (entrada.equalsIgnoreCase("s")) {
                mostrarEstado();
            } else if (entrada.startsWith("f")) {	//solo el primer caracter independiente el proceso
                cambiarFallo(entrada);
            } else if (entrada.startsWith("s") && entrada.length() > 1) {	// que no sea la opcion de antes
                cambiarValor(entrada);
            } else {
                System.out.println("Entrada no reconocida. Pulsa h para ayuda.");
            }
        }
    }

    private void mostrarAyuda() {
        System.out.println("Opciones disponibles:");
        System.out.println("h  -> Ayuda");
        System.out.println("s  -> Mostrar estado");
        System.out.println("fN -> Cambiar estado de fallo del proceso N");
        System.out.println("sX -> Proponer cambio del valor a X");
    }

    private void mostrarEstado() {
        System.out.println("id\tvar\tcompromisos\terror");
        
        // no se muestra comisiones porque no se pide en el enunciado
        for (Proceso p : procesos) {
            System.out.println(
                p.getIdProceso() + "\t" +
                p.getVariable() + "\t" +
                p.getCompromisos() + "\t" +
                p.isError()
            );
        }
    }

    private void cambiarFallo(String entrada) {
        try {
        	// limpiamos la entrada
            int id = Integer.parseInt(entrada.substring(1));

            Proceso p = buscarProceso(id);

            if (p == null) {
                System.out.println("No existe el proceso " + id);
                return;
            }
            // cambiamos el error
            p.setError(!p.isError());
            System.out.println("Proceso " + id + " ahora tiene el error =" + p.isError());

        } catch (NumberFormatException e) {
            System.out.println("Formato incorrecto -> Usa fN (EJ:f2)");
        }
    }

    private void cambiarValor(String entrada) {
        try {
            int valor = Integer.parseInt(entrada.substring(1));
            if (procesos.isEmpty()) {
                System.out.println("No hay procesos");
                return;
            }

            // 1a: el cliente envía la propuesta a TODOS los procesos
            for (Proceso p : procesos) {
                p.resetear(valor);   // solo resetea y guarda el valor propuesto
            }
            for (Proceso p : procesos) {
                p.propuesta();       // ahora multidifunde compromiso a todos
            }

            System.out.println("Cambio propuesto con valor " + valor);
        } catch (NumberFormatException e) {
            System.out.println("Formato incorrecto -> Usa sX (EJ:s6)");
        }
    }

    private Proceso buscarProceso(int id) {
        for (Proceso p : procesos) {
            if (p.getIdProceso() == id) {
                return p;
            }
        }
        return null;
    }
}