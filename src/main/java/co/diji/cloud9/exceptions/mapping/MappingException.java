package co.diji.cloud9.exceptions.mapping;

import co.diji.cloud9.exceptions.Cloud9Exception;

public class MappingException extends Cloud9Exception {

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