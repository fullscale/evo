package co.fs.evo;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class Constants {

	public static final Set<String> STATIC_RESOURCES = Collections.unmodifiableSet(
	    		new HashSet<String>(Arrays.asList(new String[]{
	    				"css", "img", "js", "html", "partials", "lib"
	    		})));	
}
