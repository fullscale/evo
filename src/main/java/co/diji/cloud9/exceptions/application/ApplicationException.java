package co.diji.cloud9.exceptions.application;

import co.diji.cloud9.exceptions.Cloud9Exception;

public class ApplicationException extends Cloud9Exception {

	private static final long serialVersionUID = 1L;

	public ApplicationException(String message) {
        super(message);
    }

    public ApplicationException(Throwable cause) {
        super(cause);
    }

    public ApplicationException(String message, Throwable cause) {
        super(message, cause);
    }
}