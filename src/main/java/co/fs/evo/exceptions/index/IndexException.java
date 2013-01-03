package co.fs.evo.exceptions.index;

import co.fs.evo.exceptions.Cloud9Exception;

public class IndexException extends Cloud9Exception {

	private static final long serialVersionUID = 1L;

	public IndexException(String message) {
        super(message);
    }

    public IndexException(Throwable cause) {
        super(cause);
    }

    public IndexException(String message, Throwable cause) {
        super(message, cause);
    }

}