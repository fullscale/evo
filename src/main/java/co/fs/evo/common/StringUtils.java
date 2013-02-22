package co.fs.evo.common;

import static co.fs.evo.Constants.APP_INDEX;
import static co.fs.evo.Constants.INVALID_INDEX_NAMES;
import static co.fs.evo.Constants.SYSTEM_INDEX;

import java.util.Arrays;
import java.util.List;

public class StringUtils {
	
	private static List<String> invalidIndexNames = Arrays.asList(INVALID_INDEX_NAMES);

    /**
     * Determines if the given string contains only 
     * lowercase characters and numbers.
     * 
     * @param name the name to check
     * @return if it is valid or not
     */
    private static boolean isValidIdentifier(String name) {
        return name.matches("[a-z0-9]+(?:\\.app)?");
    }
    
    public static boolean isValidTypeName(String name) {
    	return isValidIdentifier(name);
    }
    
    public static boolean isValidIndexName(String name) {
    	return isValidIdentifier(name) && !invalidIndexNames.contains(name);
    }
    
    public static boolean isSystemIndex(String name) {
    	return name.equals(SYSTEM_INDEX);
    }
    
    public static boolean isAppIndex(String name) {
    	return name.equals(APP_INDEX);
    }
}
