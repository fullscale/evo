package co.fs.evo.javascript;

import org.mozilla.javascript.*;

import co.fs.evo.javascript.BufferedReader;
import co.fs.evo.javascript.ByteArrayOutputStream;

/*
 * Wraps Rhino's NativeObject to provide a more friendly
 * interface for creating objects we'll be pushing into
 * a Server-Side Javascript context.
 */
public class JavascriptObject {

    private NativeObject nObj;

	public JavascriptObject() {
        this.nObj = new NativeObject();    
    }

    public JavascriptObject(NativeObject obj) {
        this.nObj = obj;
    }

    /*
     * Returns a property from the underlying native object.
     */
	public String get(String property) {
        String value = "";
        try {
            Object result = NativeObject.getProperty(nObj, property);
            if (result != Scriptable.NOT_FOUND) {
                value = result.toString();
            }
        } catch (Exception e) {}

        return value;
    }

    /*
     * Returns an nested object from the underlying native object.
     */
    public JavascriptObject getObj(String property) {
        JavascriptObject value = null;
        try {
            NativeObject x = (NativeObject) NativeObject.getProperty(nObj, property);
            value = new JavascriptObject(x);
        } catch (Exception e) {}

        return value;
    }

    /*
     * Returns a native array from the underlying native object.
     */
    public NativeArray getArray(String property) {
        NativeArray value = null;
        try {
            value = (NativeArray) NativeObject.getProperty(nObj, property);
        } catch (Exception e) {}

        return value;
    }

    /*
     * Adds a string property to the underlying native object.
     */
	public void put(String property, String value) {
        nObj.defineProperty(property, value, NativeObject.READONLY);
    }

    /*
     * Adds a nested object to the underlying native object.
     */
    public void put(String property, JavascriptObject value) {
        nObj.defineProperty(property, value.value(), NativeObject.READONLY);
    }

    /*
     * Adds a native array to the underlying native object.
     */
    public void put(String property, NativeArray value) {
        nObj.defineProperty(property, value, NativeObject.READONLY);
    }

    /*
     * Adds a boolean property to the underlying native object.
     */
    public void put(String property, boolean value) {
        nObj.defineProperty(property, value, NativeObject.READONLY);
    }

    /*
     * Adds a JS BufferedReader to the underlying native object.
     */
    public void put(String property, BufferedReader value) {
        nObj.defineProperty(property, value, NativeObject.READONLY);
    }

    /*
     * Adds a JS ByteArrayOutputStream to the underlying native object.
     */
    public void put(String property, ByteArrayOutputStream value) {
        nObj.defineProperty(property, value, NativeObject.READONLY);
    }

    /*
     * True if the underlying native object has the given key.
     */
    public boolean has(String key) {
        return nObj.containsKey(key);
    }

    /*
     * Returns the underlying native object.
     */
    public NativeObject value() {
        return this.nObj;
    }
}