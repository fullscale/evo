package co.diji.cloud9.javascript;

import java.lang.StringBuffer;

/**
 * A buffered reader that wraps the servlet inpuy stream.
 * We expose this to the SSJS controllers as part of the
 * JSGI specification v0.3 but it's nothing more than a 
 * more Javascript friendly Java BufferReader.
 */
public class BufferedReader {

	public static final long serialVersionUID = 1L;
	private java.io.BufferedReader reader = null;

	/**
	 * Constructs an empty buffer.
	 */
	public BufferedReader() {
		reader = new java.io.BufferedReader(new java.io.StringReader(""));
	}

	/**
	 * Constructs a BufferedReader based on a Java BufferedReader.
	 */
	public BufferedReader(java.io.BufferedReader buffer) {
		reader = buffer;
	}

	/*
	 * Returns the entire buffer as a string
	 */
    public String read() {
		StringBuffer requestBody = new StringBuffer();
		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				requestBody.append(line);
			}
		} catch (Exception e) {}
		return requestBody.toString();
	}

	/*
	 * Reads a block of chars from the input stream.
	 */
	public String read(int len) {
		char[] buffer = new char[len];
		try {
			reader.read(buffer, 0, len);
		} catch (java.io.IOException e) {}
		return new String(buffer);
	}

	/*
	 * Reads a single line from the input stream.
	 */
	public String readLine() {
		try {
			return reader.readLine();
		} catch (java.io.IOException e) {
			return "";
		}
	}

}