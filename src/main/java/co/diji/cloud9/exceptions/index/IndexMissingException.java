package co.diji.cloud9.exceptions.index;


public class IndexMissingException extends IndexException {

    public IndexMissingException(String message) {
        super(message);
    }

    public IndexMissingException(Throwable cause) {
        super(cause);
    }

    public IndexMissingException(String message, Throwable cause) {
        super(message, cause);
    }

}