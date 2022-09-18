package lt.lukasa.proguardviewer.util;

/**
 * @author Lukas Alt
 * @since 16.09.2022
 */
public class StringUtil {
    public static String mapClassName(String className) {
        return className.replace('$', '.');
    }
}
