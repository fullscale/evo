package co.diji.cloud9.exceptions.index;

public class IndexCreationException extends IndexException {

    public IndexCreationException(String message) {
        super(message);
    }

    public IndexCreationException(Throwable cause) {
        super(cause);
    }

    public IndexCreationException(String message, Throwable cause) {
        super(message, cause);
    }

}