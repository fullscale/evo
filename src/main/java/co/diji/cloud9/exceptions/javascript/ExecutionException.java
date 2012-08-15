package co.diji.cloud9.exceptions.javascript;

import co.diji.cloud9.exceptions.Cloud9Exception;

public class ExecutionException extends Cloud9Exception {

    private static final long serialVersionUID = 1L;

    public ExecutionException(String message) {
        super(message);
    }

    public ExecutionException(Throwable cause) {
        super(cause);
    }

    public ExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

}