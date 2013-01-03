package co.fs.evo.exceptions.resources;

import co.fs.evo.exceptions.Cloud9Exception;

public class ResourceException extends Cloud9Exception {

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