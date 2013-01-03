package co.fs.evo.exceptions.type;

public class TypeMissingException extends TypeException {

	private static final long serialVersionUID = 1L;

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