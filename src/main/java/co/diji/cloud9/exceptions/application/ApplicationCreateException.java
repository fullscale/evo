package co.diji.cloud9.exceptions.application;

public class ApplicationCreateException extends ApplicationException {
	private static final long serialVersionUID = 1L;
	
	public ApplicationCreateException(String message) {
        super(message);
    }

    public ApplicationCreateException(Throwable cause) {
        super(cause);
    }

    public ApplicationCreateException(String message, Throwable cause) {
        super(message, cause);
    } 
}
