package co.fs.evo.exceptions.application;

import co.fs.evo.exceptions.application.ApplicationException;

public class InvalidApplicationNameException extends ApplicationException {
	private static final long serialVersionUID = 1L;

	public InvalidApplicationNameException(String message) {
        super(message);
    }

    public InvalidApplicationNameException(Throwable cause) {
        super(cause);
    }

    public InvalidApplicationNameException(String message, Throwable cause) {
        super(message, cause);
    }
}
