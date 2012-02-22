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
        cell.setCellValue(textToWrite);
    }

}
