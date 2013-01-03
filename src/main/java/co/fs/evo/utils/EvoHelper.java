package co.fs.evo.utils;

public class EvoHelper {

    /**
     * Determines if the name is a valid name or not. A valid name is contains only lowercase characters and numbers.
     * 
     * @param name the name to check
     * @return if it is valid or not
     */
    public static boolean isValidName(String name) {
        return name.matches("[a-z0-9]+(?:\\.app)?");
    }
}
