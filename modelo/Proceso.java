package modelo;

public class Proceso extends Thread {

	private int id;
	private int variable;
	private boolean error;
	
	public Proceso(int id) {
		this.id = id;
		this.variable = -1;
		this.error = false;
	}
	
	public int getProcesoId() {
		return id;
	}
	
	public int getVariable() {
		return variable;
	}
	
	public void setVariable(int v) {
		this.variable = v;
	}
	
	public boolean isError() {
		return error;
	}
	
	public void setError (boolean error) {
		this.error = error;
	}
	
}
