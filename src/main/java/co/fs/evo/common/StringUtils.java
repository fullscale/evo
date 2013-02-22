package co.fs.evo.common;

public class StringUtils {

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
    	return isValidIdentifier(name);
    }
}
