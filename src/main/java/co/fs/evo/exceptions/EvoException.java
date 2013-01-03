package co.fs.evo.exceptions;

public class EvoException extends Exception {

	private static final long serialVersionUID = 1L;

	public EvoException(String message) {
        super(message);
    }

    public EvoException(Throwable cause) {
        super(cause);
    }

    public EvoException(String message, Throwable cause) {
        super(message, cause);
    }

}
