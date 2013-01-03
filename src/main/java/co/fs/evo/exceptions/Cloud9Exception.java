package co.fs.evo.exceptions;

public class Cloud9Exception extends Exception {

	private static final long serialVersionUID = 1L;

	public Cloud9Exception(String message) {
        super(message);
    }

    public Cloud9Exception(Throwable cause) {
        super(cause);
    }

    public Cloud9Exception(String message, Throwable cause) {
        super(message, cause);
    }

}
