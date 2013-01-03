package co.fs.evo.exceptions.mapping;

import co.fs.evo.exceptions.EvoException;

public class MappingException extends EvoException {

	private static final long serialVersionUID = 1L;

	public MappingException(String message) {
        super(message);
    }

    public MappingException(Throwable cause) {
        super(cause);
    }

    public MappingException(String message, Throwable cause) {
        super(message, cause);
    }

}