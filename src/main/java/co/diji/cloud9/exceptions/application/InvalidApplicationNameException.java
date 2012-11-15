package co.diji.cloud9.exceptions.application;

import co.diji.cloud9.exceptions.application.ApplicationException;

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
