package me.connor.justdesserts.exceptions;

public class SerialException extends RuntimeException {
	
	private static final long serialVersionUID = 724609112619269066L;

	public SerialException() {
		super();
	}
	
	public SerialException(String msg) {
		super(msg);
	}
	
	public SerialException(Throwable t) {
		super(t);
	}
	
	public SerialException(String msg, Throwable t) {
		super(msg, t);
	}
}
