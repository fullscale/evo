package co.diji.cloud9.exceptions.mapping;

public class MappingExistsException extends MappingException {

    public MappingExistsException(String message) {
        super(message);
    }

    public MappingExistsException(Throwable cause) {
        super(cause);
    }

    public MappingExistsException(String message, Throwable cause) {
        super(message, cause);
    }

}