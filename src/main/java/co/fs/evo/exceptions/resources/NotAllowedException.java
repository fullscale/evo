package co.fs.evo.exceptions.resources;


public class NotAllowedException extends ResourceException {

    private static final long serialVersionUID = 1L;

    public NotAllowedException(String message) {
        super(message);
    }

    public NotAllowedException(Throwable cause) {
        super(cause);
    }

    public NotAllowedException(String message, Throwable cause) {
        super(message, cause);
    }

}