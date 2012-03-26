package co.diji.cloud9.exceptions.mapping;

public class MappingExistsException extends MappingException {

	private static final long serialVersionUID = 1L;

	public MappingExistsException(String message) {
        super(message);
    }

    public MappingExistsException(Throwable cause) {
        super(cause);
    }

    public MappingExistsException(String message, Throwable cause) {
        super(message, cause);
    }

}