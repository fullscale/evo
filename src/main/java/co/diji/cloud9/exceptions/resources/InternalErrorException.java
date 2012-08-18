package co.diji.cloud9.exceptions.resources;


public class InternalErrorException extends ResourceException {

    private static final long serialVersionUID = 1L;

    public InternalErrorException(String message) {
        super(message);
    }

    public InternalErrorException(Throwable cause) {
        super(cause);
    }

    public InternalErrorException(String message, Throwable cause) {
        super(message, cause);
    }

}