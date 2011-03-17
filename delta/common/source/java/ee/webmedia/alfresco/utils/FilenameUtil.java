package ee.webmedia.alfresco.utils;

import org.alfresco.repo.content.MimetypeMap;
import org.apache.commons.lang.StringUtils;

/**
 * Helps to strip illegal characters from filenames
 * 
 * @author Kaarel Jõgeva
 */
public class FilenameUtil {

    public static String buildFileName(String title, String extension) {
        return buildFileName(title, extension, true);
    }

    public static String buildFileName(String title, String extension, boolean extensionRequired) {
        if (extension == null) {
            extension = "";
        }
        if (StringUtils.isBlank(extension) && extensionRequired) {
            extension = MimetypeMap.EXTENSION_BINARY;
        }
        extension = StringUtils.deleteWhitespace(stripForbiddenWindowsCharacters(extension));
        int maxLength = 254 - extension.length();
        String nameWithoutExtension = stripDotsAndSpaces(stripForbiddenWindowsCharacters(title));
        if (nameWithoutExtension.length() > maxLength) {
            nameWithoutExtension = nameWithoutExtension.substring(0, maxLength);
        }
        return nameWithoutExtension + "." + extension;
    }

    public static String stripForbiddenWindowsCharacters(String filename) {
        // Windows kernel forbids the use of characters in range 1-31 (i.e., 0x01-0x1F) and
        // characters " * : < > ? \ / |
        String name = filename.replaceAll("\\p{Cntrl}", "");
        // On some server environments(concrete case with GlassFish on Linux server - on other Linux/Windows machine there were no such problem) when using encoded "+" ("%2B") in url's request.getRequestURI() returns unEncoded value of "+" (instead of "%2B") and
        // further decoding will replace + with space. Hence when looking for file by name there is " " instead of "+" and file will not be found.
        name = name.replace('+', '-');
        name = name.replace(':', '_');
        name = name.replace('"', '\'');
        name = name.replace('/', '-');
        // [\"\*\<\>?\|]
        name = name.replaceAll("[\\*\\\\\\>\\<\\?\\|]", "").replaceAll("\\s+", " ");
        return name;
    }

    public static String stripDotsAndSpaces(String filename) {
        // In Windows the space and the period are not allowed as the final character of a filename

        // remove dots and spaces from beginning and end of string
        return filename.replaceAll("^([ \\.])+", "").replaceAll("([ \\.])+$", "");
    }

}
