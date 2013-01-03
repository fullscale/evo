package co.fs.evo.exceptions.type;

public class TypeExistsException extends TypeException {

	private static final long serialVersionUID = 1L;

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