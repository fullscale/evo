package co.fs.evo.exceptions.type;

import co.fs.evo.exceptions.EvoException;

public class TypeException extends EvoException {

	private static final long serialVersionUID = 1L;

	public TypeException(String message) {
        super(message);
    }

    public TypeException(Throwable cause) {
        super(cause);
    }

    public TypeException(String message, Throwable cause) {
        super(message, cause);
    }

}