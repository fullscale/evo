package co.fs.evo.exceptions.application;

public class ApplicationExistsException extends ApplicationException {

	private static final long serialVersionUID = 1L;
	
	public ApplicationExistsException(String message) {
        super(message);
    }

    public ApplicationExistsException(Throwable cause) {
        super(cause);
    }

    public ApplicationExistsException(String message, Throwable cause) {
        super(message, cause);
    } 
}
