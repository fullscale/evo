package co.diji.cloud9.exceptions.index;

import co.diji.cloud9.exceptions.Cloud9Exception;

public class IndexException extends Cloud9Exception {

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