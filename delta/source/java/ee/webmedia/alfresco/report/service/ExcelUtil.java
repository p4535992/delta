package ee.webmedia.alfresco.report.service;

import org.apache.commons.logging.Log;
import org.apache.poi.ss.usermodel.Cell;

/**
 * @author Riina Tens
 */
public class ExcelUtil {

    private final static int EXCEL_CELL_MAX_SIZE = 32767;
    private final static String EXCEL_CELL_MAX_SIZE_NOTIFICATION_SUFFIX = "\n\n\nNB! end of the input was removed, as it exceeded maximum length that excel cell can hold ("
            + EXCEL_CELL_MAX_SIZE + " characters)";

    public static void setCellValueTruncateIfNeeded(final Cell cell, String textToWrite, Log log) {
        if (textToWrite == null || textToWrite.length() == 0) {
            return;
        }
        if (textToWrite.length() >= EXCEL_CELL_MAX_SIZE) {
            log.warn("Following text is too long to fit into excel cell (truncating it):\n" + textToWrite);
            textToWrite = textToWrite.substring(0, (EXCEL_CELL_MAX_SIZE - EXCEL_CELL_MAX_SIZE_NOTIFICATION_SUFFIX.length() - 1));
        }
        cell.setCellValue(stripNonValidXMLCharacters(textToWrite));
    }

    /**
     * Copied from http://benjchristensen.com/2008/02/07/how-to-strip-invalid-xml-characters/
     * This method ensures that the output String has only
     * valid XML unicode characters as specified by the
     * XML 1.0 standard. For reference, please see
     * <a href=”http://www.w3.org/TR/2000/REC-xml-20001006#NT-Char”>the
     * standard</a>. This method will return an empty
     * String if the input is null or empty.
     * 
     * @param in The String whose non-valid characters we want to remove.
     * @return The in String, stripped of non-valid characters.
     */
    public static String stripNonValidXMLCharacters(String in) {
        StringBuffer out = new StringBuffer(); // Used to hold the output.
        char current; // Used to reference the current character.

        if (in == null || ("".equals(in))) {
            return ""; // vacancy test.
        }
        for (int i = 0; i < in.length(); i++) {
            current = in.charAt(i);
            if ((current == 0x9) ||
                    (current == 0xA) ||
                    (current == 0xD) ||
                    ((current >= 0x20) && (current <= 0xD7FF)) ||
                    ((current >= 0xE000) && (current <= 0xFFFD)) ||
                    ((current >= 0x10000) && (current <= 0x10FFFF))) {
                out.append(current);
            }
        }
        return out.toString();
    }

}
