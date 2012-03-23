package co.diji.cloud9.exceptions.index;


public class IndexExistsException extends IndexException {

    public IndexExistsException(String message) {
        super(message);
    }

    public IndexExistsException(Throwable cause) {
        super(cause);
    }

    public IndexExistsException(String message, Throwable cause) {
        super(message, cause);
    }

}