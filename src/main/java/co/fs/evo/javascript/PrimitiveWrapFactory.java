package co.fs.evo.javascript;

import org.mozilla.javascript.WrapFactory;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

/**
 * An example WrapFactory that can be used to avoid wrapping of Java types
 * that can be converted to ECMA primitive values.
 * 
 * So java.lang.String is mapped to ECMA string, all java.lang.Numbers are
 * mapped to ECMA numbers, and java.lang.Booleans are mapped to ECMA booleans
 * instead of being wrapped as objects. Additionally java.lang.Character is
 * converted to ECMA string with length 1.
 * Other types have the default behavior.
 * 
 * Note that calling "new java.lang.String('foo')" in JavaScript with this
 * wrap factory enabled will still produce a wrapped Java object since the
 * WrapFactory.wrapNewObject method is not overridden.
 * 
 * The PrimitiveWrapFactory is enabled on a Context by calling setWrapFactory
 * on that context.
 */
public class PrimitiveWrapFactory extends WrapFactory {
    @Override
    public Object wrap(Context cx, 
                       Scriptable scope, 
                       Object obj, 
                       Class<?> staticType) {

        if (obj instanceof String || 
            obj instanceof Number || obj instanceof Boolean) {

            return obj;

        } else if (obj instanceof Character) {
            char[] a = { ((Character)obj).charValue() };
            return new String(a);
        }
        return super.wrap(cx, scope, obj, staticType);
    }
}