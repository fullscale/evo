package co.diji.cloud9.exceptions.type;

import co.diji.cloud9.exceptions.Cloud9Exception;

public class TypeException extends Cloud9Exception {

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