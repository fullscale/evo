package co.fs.evo.exceptions.index;

import co.fs.evo.exceptions.EvoException;

public class IndexException extends EvoException {

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