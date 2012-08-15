package co.diji.cloud9.exceptions.javascript;

import co.diji.cloud9.exceptions.Cloud9Exception;

public class NotFoundException extends Cloud9Exception {

    private static final long serialVersionUID = 1L;

    public NotFoundException(String message) {
        super(message);
    }

    public NotFoundException(Throwable cause) {
        super(cause);
    }

    public NotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

}