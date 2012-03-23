package co.diji.cloud9.exceptions.type;

public class TypeExistsException extends TypeException {

    public TypeExistsException(String message) {
        super(message);
    }

    public TypeExistsException(Throwable cause) {
        super(cause);
    }

    public TypeExistsException(String message, Throwable cause) {
        super(message, cause);
    }

}