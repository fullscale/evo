package co.diji.cloud9.exceptions.type;

public class TypeCreationException extends TypeException {

	private static final long serialVersionUID = 1L;

	public TypeCreationException(String message) {
        super(message);
    }

    public TypeCreationException(Throwable cause) {
        super(cause);
    }

    public TypeCreationException(String message, Throwable cause) {
        super(message, cause);
    }

}