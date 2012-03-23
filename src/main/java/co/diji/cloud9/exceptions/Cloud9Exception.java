package co.diji.cloud9.exceptions;

public class Cloud9Exception extends Exception {

    public Cloud9Exception(String message) {
        super(message);
    }

    public Cloud9Exception(Throwable cause) {
        super(cause);
    }

    public Cloud9Exception(String message, Throwable cause) {
        super(message, cause);
    }

}
