package co.fs.evo.exceptions.resources;

import co.fs.evo.exceptions.EvoException;

public class ResourceException extends EvoException {

    private static final long serialVersionUID = 1L;

    public ResourceException(String message) {
        super(message);
    }

    public ResourceException(Throwable cause) {
        super(cause);
    }

    public ResourceException(String message, Throwable cause) {
        super(message, cause);
    }

}