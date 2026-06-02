    package cliente;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class Cliente {
	
	private static final String[] URLS = {
			"http://192.168.1.253:8080/practicaObligatoria/rest/servicio/",
			"http://192.168.1.188:8080/practicaObligatoria/rest/servicio/"
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
				consultarEstado();
			} else if (entrada.equalsIgnoreCase("r")) {
				realizarPeticion("reset");
			} else if (entrada.matches("f\\d+")) {
				cambiarFallo(entrada);
			} else if (entrada.matches("s\\d+")) {
				cambiarValor(entrada);
			} else if (entrada.equalsIgnoreCase("ft")) {
				realizarPeticion("resetTotal");
			} else {
				System.out.println("Entrada no reconocida. Pulsa 'h' para ayuda");
			}
		}
	}
	
	//Muestra las opciones disponibles para el usuario
	private static void ayuda() {
		System.out.println("Opciones disponibles: ");
		System.out.println("h	 -> Ayuda");
		System.out.println("s	 -> Mostrar estado");
		System.out.println("fN	 -> Cambiar estado de fallo del proceso N");
		System.out.println("sX	 -> Proponer cambio del valor a X");
		System.out.println("r\t  -> Reiniciar sistema");
		System.out.println("ft\t -> Reiniciar sistema y fallos");
	}
	
	private static void consultarEstado() {
		
		System.out.printf("%-4s %-5s %-20s %-20s %-6s\n", 
				"id", "var", "compromisos", "comisiones", "error");
		
		for (String s : URLS) {
			String respuesta = obtenerRespuesta(s + "estado");
			
			if(!respuesta.isEmpty()) {
				System.out.print(respuesta);
			}
		}
		
	}
	
	private static void cambiarFallo(String entrada) {
		try {
			int id = Integer.parseInt(entrada.substring(1));

			for (String s : URLS) {
				String respuesta = obtenerRespuesta(s + "fallo?id=" + id);
				
				if (respuesta.startsWith("Proceso " + id)) {
					System.out.printf(respuesta);
					return;
				}
			}
			System.out.println("No existe un proceso con id " + id);
		} catch (NumberFormatException e) {
			System.out.println("Formato incorrecto -> Usa fN (ejemplo: f2)");
		}
	}
	
	private static void cambiarValor(String entrada) {
		try {
			int valor = Integer.parseInt(entrada.substring(1));
			realizarPeticionSinTexto("reset");
			realizarPropuesta(valor);
			
			try {
				// Modelo asíncrono simplificado -> espera un tiempo para que los procesos
				// intercambien compromisos, comisiones y confirmaciones antes de consultar
				// el resultado
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			
			consultarResultado();
		} catch (NumberFormatException e) {
			System.out.println("Formato incorrecto -> Usa sX (ejemplo: s6)");
		}
	}
	

	private static void realizarPropuesta(int valor) {
		
		for (String s : URLS) {
			obtenerRespuesta(s + "propuesta?v=" + valor);
		}
		
		for (String s : URLS) {
			obtenerRespuesta(s + "emitirCompromisos");
		}
		
		System.out.println("Propuesta enviada: " + valor);
		
	}
	

	private static void realizarPeticion(String ruta) {
		for (String s : URLS) {
			realizarPeticionCompleta(s + ruta);
		}
	}
	

	private static void realizarPeticionSinTexto(String ruta) {
		for (String s : URLS) {
			obtenerRespuesta(s + ruta);
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
	

	private static String obtenerRespuesta(String urlStr) {
		
		StringBuilder respuesta = new StringBuilder();
		
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
				respuesta.append(linea).append("\n");
			}
			
			lector.close();
			conexion.disconnect();
		} catch (Exception e) {
			return "";
		}
		return respuesta.toString();
	}
	

	private static void consultarResultado() {
		
		boolean consenso = false;
		
		for (String s : URLS) {
			String respuesta = obtenerRespuesta(s + "resultado");
		
			if (respuesta.contains("CONSENSO ALCANZADO") && !consenso) {
				System.out.println(respuesta);
				consenso = true;
			}
		}
		if (!consenso) {
			System.out.println("Todavía no hay consenso confirmado");
		}
		
	}
	
}

    
