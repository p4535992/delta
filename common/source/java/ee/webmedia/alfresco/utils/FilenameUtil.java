package ee.webmedia.alfresco.utils;

import static ee.webmedia.alfresco.utils.ISOLatin1Util.removeAccents;
import static org.apache.commons.io.FileUtils.ONE_GB;
import static org.apache.commons.io.FileUtils.ONE_KB;
import static org.apache.commons.io.FileUtils.ONE_MB;

import java.text.DecimalFormat;
import java.util.List;
import java.util.regex.Pattern;

import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.Pair;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.WorkflowDbService;
import ee.webmedia.xtee.client.dhl.types.ee.sk.digiDoc.v13.DataFileType;

/**
 * Helps to strip illegal characters from filenames
 * 
 * @author Kaarel Jõgeva
 */
public class FilenameUtil {

    public static final String ERR_INVALID_FILE_NAME = "add_file_invalid_file_name";

    private static final int FILE_MAX_LENGTH = 50;
    private static final String FILE_MAX_LENGTH_SUFFIX = "....";
    private static final String FILE_NON_ASCII_REPLACEMENT = "_";
    private static final String FILE_AMPERSAND_REPLACEMENT = " ja ";
    private static final Pattern NON_ASCII = Pattern.compile("[^\\x00-\\x7f]");

    public static String buildFileName(String title, String extension) {
        return buildFileName(title, extension, true);
    }

    private static String buildFileName(String title, String extension, boolean extensionRequired) {
        if (extension == null) {
            extension = "";
        }
        if (StringUtils.isBlank(extension) && extensionRequired) {
            extension = MimetypeMap.EXTENSION_BINARY;
        }
        extension = StringUtils.deleteWhitespace(stripForbiddenWindowsCharacters(extension));
        int maxLength = 254 - extension.length();
        String nameWithoutExtension = trimDotsAndSpaces(stripForbiddenWindowsCharacters(title));
        if (nameWithoutExtension.length() > maxLength) {
            nameWithoutExtension = nameWithoutExtension.substring(0, maxLength);
        }
        return nameWithoutExtension + "." + extension;
    }

    public static String stripForbiddenWindowsCharacters(String filename) {
        // Windows kernel forbids the use of characters in range 1-31 (i.e., 0x01-0x1F) and
        // characters " * : < > ? \ / |
        String name = filename.replaceAll("\\p{Cntrl}", "");
        // On some server environments(concrete case with GlassFish on Linux server - on other Linux/Windows machine there were no such problem) when using
        // encoded "+" ("%2B") in url's request.getRequestURI() returns unEncoded value of "+" (instead of "%2B") and
        // further decoding will replace + with space. Hence when looking for file by name there is " " instead of "+" and file will not be found.
        name = name.replace('+', '-');
        name = name.replace(':', '_');
        name = name.replace("\"", "");
        name = name.replace("/", "");
        // [\"\*\<\>?\|]
        name = name.replaceAll("[\\*\\\\\\>\\<\\?\\|]", "").replaceAll("\\s+", " ");
        return name;
    }

    public static String trimDotsAndSpaces(String filename) {
        // In Windows the space and the period are not allowed as the final character of a filename

        // remove dots and spaces from beginning and end of string
        return filename.replaceAll("^([ \\.])+", "").replaceAll("([ \\.])+$", "");
    }

    public static String replaceAmpersand(String filename) {
        return filename.replaceAll(" & ", FILE_AMPERSAND_REPLACEMENT).replaceAll("&", FILE_AMPERSAND_REPLACEMENT);
    }

    public static String generateUniqueFileDisplayName(String displayName, List<String> existingDisplayNames) {
        String baseName = FilenameUtils.removeExtension(displayName);
        String extension = FilenameUtils.getExtension(displayName);
        if (StringUtils.isBlank(extension)) {
            extension = MimetypeMap.EXTENSION_BINARY;
        }
        String suffix = "";
        int i = 1;

        while (existingDisplayNames.contains(baseName + suffix + "." + extension)) {
            suffix = " (" + i + ")";
            i++;
        }
        return baseName + suffix + "." + extension;
    }

    public static String replaceNonAsciiCharacters(String filename) {
        return NON_ASCII.matcher(filename).replaceAll(FILE_NON_ASCII_REPLACEMENT);
    }

    public static String getDvkFilename(DataFileType dataFile) {
        return dataFile.getId() + " " + dataFile.getFilename();
    }

    /**
     * Shortens the given filename by taking first part that fits in limit and inserts the marker between base name and extension
     * 
     * @param filename name to shorten
     * @param maxLength maximum length of the name including extension
     * @return shortened filename or <code>null</code> if given name is <code>null</code>
     */
    public static String limitFileNameLength(String filename) {
        if (filename != null && filename.length() > FILE_MAX_LENGTH) {
            String baseName = FilenameUtils.removeExtension(filename);
            String extension = FilenameUtils.getExtension(filename);
            baseName = baseName.substring(0, FILE_MAX_LENGTH - extension.length() - FILE_MAX_LENGTH_SUFFIX.length());
            filename = baseName + FILE_MAX_LENGTH_SUFFIX + extension;
        }
        return filename;
    }

    public static String makeSafeFilename(String name) {
        return makeSafeFilename(name, null);
    }

    public static String makeSafeUniqueFilename(String name, List<String> existingFileNames) {
        return makeSafeFilename(name, existingFileNames);
    }

    private static String makeSafeFilename(String name, List<String> existingFileNames) {
        String safeName = limitFileNameLength(
                replaceNonAsciiCharacters(
                removeAccents(
                replaceAmpersand(
                trimDotsAndSpaces(
                stripForbiddenWindowsCharacters(
                name))))));

        if (existingFileNames != null && !existingFileNames.isEmpty()) {
            safeName = generateUniqueFileDisplayName(safeName, existingFileNames);
        }

        return safeName;
    }

    public static Pair<String, String> getFilenameFromDisplayname(NodeRef documentNodeRef, List<String> existingDisplayNames, String displayName, GeneralService generalService) {
        displayName = generateUniqueFileDisplayName(displayName, existingDisplayNames);
        String name = checkAndGetUniqueFilename(documentNodeRef, displayName, generalService);
        return new Pair<String, String>(name, displayName);
    }

    public static Pair<String, String> getTaskFilenameFromDisplayname(Task task, List<String> existingDisplayNames, String displayName,
            GeneralService generalService, WorkflowDbService workflowDbService) {
        displayName = generateUniqueFileDisplayName(displayName, existingDisplayNames);
        String name = generalService.getUniqueFileName(makeSafeFilename(displayName),
                workflowDbService.getTaskFileNodeRefs(task.getNodeRef()), task.getParent().getNodeRef());
        return new Pair<String, String>(name, displayName);
    }

    public static String getDiplayNameFromName(String originalFileName) {
        return FilenameUtils.removeExtension(originalFileName) + "." + FilenameUtils.getExtension(originalFileName);
    }

    /**
     * NB! this method is intended only for cm:name property!
     */
    private static String checkAndGetUniqueFilename(NodeRef documentNodeRef, String displayName, GeneralService generalService) {
        String safeFilename = makeSafeFilename(displayName);
        return generalService.getUniqueFileName(documentNodeRef, safeFilename);
    }

    public static String byteCountToDisplaySize(long size) {
        DecimalFormat df = new DecimalFormat("#.##");

        String displaySize;
        if (size / ONE_GB > 0) {
            displaySize = df.format((double) size / ONE_GB) + " GB";
        } else if (size / ONE_MB > 0) {
            displaySize = df.format((double) size / ONE_MB) + " MB";
        } else if (size / ONE_KB > 0) {
            displaySize = df.format((double) size / ONE_KB) + " KB";
        } else {
            displaySize = String.valueOf(size) + " B";
        }
        return displaySize;
    }

    public static boolean isEncryptedFile(String fileName) {
        return fileName.toLowerCase().endsWith(".cdoc");
    }

}
