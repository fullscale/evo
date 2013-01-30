package co.fs.evo;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class Constants {
	
    public static final String SYSTEM_INDEX = "sys";
    public static final String APP_INDEX = "app";
    public static final String[] INVALID_INDEX_NAMES = {"css", "js", "img", "partials", "lib"};
    public static final String[] VALID_TYPES = {"conf", "html", "css", "img", "js", "server-side", "partials", "lib"};

	public static final Set<String> STATIC_RESOURCES = Collections.unmodifiableSet(
	    		new HashSet<String>(Arrays.asList(new String[]{
	    				"css", "img", "js", "html", "partials", "lib"
	    		})));	
}
