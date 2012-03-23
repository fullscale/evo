package co.diji.cloud9.exceptions.type;

public class TypeMissingException extends TypeException {

    public TypeMissingException(String message) {
        super(message);
    }

    public TypeMissingException(Throwable cause) {
        super(cause);
    }

    public TypeMissingException(String message, Throwable cause) {
        super(message, cause);
    }

}