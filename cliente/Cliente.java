package cliente;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

/* Clase cliente del sistema PBFT -> Punto de entrada de la aplicación
 * Permite al usuario:
 * - mostrar ayuda
 * - consultar el estado del sistema
 * - activar o desactivar fallos bizantinos
 * - proponer cambios de valor
 * 
 * El cliente se comunica con el servicio REST mediante peticiones HTTP
 */

public class Cliente {
	
	// URL base del servicio REST
	private static final String[] URLS = {
			"http://172.20.7.254:8080/practicaObligatoria/rest/servicio/",
			"http://172.20.7.191:8080/practicaObligatoria/rest/servicio/",
			"http://172.20.7.126:8080/practicaObligatoria/rest/servicio/"
	};

	public static void main(String[] args) {
		Scanner scan = new Scanner(System.in);
		
		System.out.println("Cliente PBFT iniciado");
		ayuda();
		
		while (true) {
			System.out.print("> ");
			String entrada = scan.nextLine().trim();
			
			if (entrada.equalsIgnoreCase("h")) {
				ayuda();
			} else if (entrada.equalsIgnoreCase("s")) {
				realizarPeticion("estado");
			} else if (entrada.equalsIgnoreCase("r")) {
				realizarPeticion("reset");
			} else if (entrada.matches("f\\d+")) {
				cambiarFallo(entrada);
			} else if (entrada.matches("s\\d+")) {
				cambiarValor(entrada);
			} else if (entrada.equalsIgnoreCase("rt")) {
				realizarPeticion("resetTotal");
			} else {
				System.out.println("Entrada no reconocida. Pulsa 'h' para ayuda");
			}
		}
	}
	
	//Muestra las opciones disponibles para el usuario
	private static void ayuda() {
		System.out.println("Opciones disponibles: ");
		System.out.println("h	-> Ayuda");
		System.out.println("s	-> Mostrar estado");
		System.out.println("fN	-> Cambiar estado de fallo del proceso N");
		System.out.println("sX	-> Proponer cambio del valor a X");
		System.out.println("r\t -> Reiniciar sistema");
		System.out.println("ft\t -> Reiniciar sistema y fallos");
	}
	
	// Cambia el estado de fallo de un proceso
	private static void cambiarFallo(String entrada) {
		try {
			int id = Integer.parseInt(entrada.substring(1));
			realizarPeticion("fallo?id=" + id);
		} catch (NumberFormatException e) {
			System.out.println("Formato incorrecto -> Usa fN (ejemplo: f2");
		}
	}
	
	//Lanza una propuesta de cambio de valor
	private static void cambiarValor(String entrada) {
		try {
			int valor = Integer.parseInt(entrada.substring(1));
			
			realizarPeticion("reset");
			realizarPeticion("propuesta?v=" + valor);
		} catch (NumberFormatException e) {
			System.out.println("Formato incorrecto -> Usa sX (ejemplo: s6");
		}
	}
	
	//Realiza una petición GET al servicio REST y muestra la respuesta 
	private static void realizarPeticion(String ruta) {
		for (String s : URLS) {
			realizarPeticionCompleta(s + ruta);
		}
	}
	
	private static void realizarPeticionCompleta(String urlStr) {
		try {
			URL url = new URL(urlStr);
			HttpURLConnection conexion = (HttpURLConnection) url.openConnection();
			conexion.setConnectTimeout(3000);
			conexion.setReadTimeout(5000);
			conexion.setRequestMethod("GET");
			
			BufferedReader lector = new BufferedReader(
					new InputStreamReader(conexion.getInputStream()));
			
			String linea;
			while ((linea = lector.readLine()) != null) {
				System.out.println(linea);
			}
			
			lector.close();
			conexion.disconnect();
		} catch (Exception e) {
			System.out.println("Error: " + e.getMessage());
		}
	}
	
}
