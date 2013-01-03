package co.fs.evo.exceptions.application;

import co.fs.evo.exceptions.Cloud9Exception;

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
