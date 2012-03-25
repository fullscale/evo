package co.diji.cloud9.javascript;

/**
 * An output stream writer for application errors.
 * We expose this to the SSJS controllers as part of the
 * JSGI specification v0.3 but it's nothing more than a 
 * more Javascript friendly Java ByteArrayOutputStream.
 */
public class ByteArrayOutputStream {

	public static final long serialVersionUID = 1L;
	private java.io.ByteArrayOutputStream writer = null;

	public ByteArrayOutputStream() {
 		writer = new java.io.ByteArrayOutputStream();
 	}

 	/**
 	 * Writes a string to internal output stream.
 	 */
	public void write(String s) {
		byte buf[] = s.getBytes();
		try {
			writer.write(buf);
		} catch (java.io.IOException e) {}
	}

	/**
	 * Returns the output stream as a string.
	 */
	public String toString() {
		return writer.toString();
	}

	/**
	 * Returns the output stream as a byte array.
	 */
	public byte[] toByteArray() {
		return writer.toByteArray();
	}

}