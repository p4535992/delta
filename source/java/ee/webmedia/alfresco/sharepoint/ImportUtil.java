package ee.webmedia.alfresco.sharepoint;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;

public class ImportUtil {

    private static final String TEXT_NULL = "NULL";

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy");

    private static final DateFormat DATE_TIME_SHORT_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm");

    private static final DateFormat DATE_TIME_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    private static final DateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    public static final Charset CHARSET_UTF8 = Charset.forName("UTF-8");

    public static final Charset CHARSET_ISO = Charset.forName("ISO-8859-4");

    static {
        DATE_FORMAT.setLenient(false);
    }

    public static void checkMandatory(CsvReader reader, int... cols) throws ImportValidationException, IOException {
        for (int col : cols) {
            if (reader.get(col - 1).isEmpty()) {
                error("%s must be valued", reader, col);
            }
        }
    }

    public static void checkInteger(CsvReader reader, int... cols) throws ImportValidationException, IOException {
        for (int col : cols) {
            try {
                String value = reader.get(col - 1);
                if (!value.isEmpty()) {
                    Integer.parseInt(value);
                }
            } catch (NumberFormatException e) {
                error("%s must be integer", reader, col);
            }
        }
    }

    public static void checkDate(CsvReader reader, int... cols) throws ImportValidationException, IOException {
        for (int col : cols) {
            try {
                String value = reader.get(col - 1);
                if (!value.isEmpty()) {
                    DATE_FORMAT.parse(value);
                }
            } catch (ParseException e) {
                error("%s date must be in format dd.mm.yyyy", reader, col);
            }
        }
    }

    public static void checkAnyOf(CsvReader reader, String[] choices, int... cols) throws ImportValidationException, IOException {
        for (int col : cols) {
            if (Arrays.binarySearch(choices, reader.get(col - 1)) < 0) {
                error("Unknown %s", reader, col);
            }
        }
    }

    private static void error(String errorMsg, CsvReader reader, int col) throws ImportValidationException, IOException {
        throw new ImportValidationException(errorMsg, reader, col - 1);
    }

    public static String getString(CsvReader reader, int col) throws IOException {
        String value = reader.get(col - 1);
        return value.isEmpty() || value.equals(TEXT_NULL) ? null : value.trim();
    }

    public static Integer getInteger(CsvReader reader, int col) throws IOException {
        String value = reader.get(col - 1);
        return value.isEmpty() || value.equals(TEXT_NULL) ? null : Integer.valueOf(value);
    }

    public static java.sql.Date getSqlDate(CsvReader reader, int col) throws IOException {
        final Date date = getDate(reader.get(col - 1));
        return date == null ? null : new java.sql.Date(date.getTime());
    }

    public static java.sql.Date getSqlDateTS(CsvReader reader, int col) throws IOException {
        final Date date = getTimestamp(reader.get(col - 1));
        return date == null ? null : new java.sql.Date(date.getTime());
    }

    public static Date getDate(CsvReader reader, int col) throws IOException {
        return getDate(reader.get(col - 1));
    }

    public static Date getDate(String date) {
        return parseDate(date, DATE_FORMAT);
    }

    public static Date getDateTime(CsvReader reader, int col) throws IOException {
        return getDateTime(reader.get(col - 1));
    }

    public static Date getDateTime(String dateTime) {
        return parseDate(dateTime, DATE_TIME_FORMAT);
    }

    public static Date getDateTimeShort(String dateTime) {
        return parseDate(dateTime, DATE_TIME_SHORT_FORMAT);
    }

    public static Timestamp getTimestamp(CsvReader reader, int col) throws IOException {
        return getTimestamp(reader.get(col - 1));
    }

    public static Timestamp getTimestamp(String timestamp) {
        Date dateTime = parseDate(timestamp, TIMESTAMP_FORMAT);
        return dateTime != null ? new Timestamp(dateTime.getTime()) : null;
    }

    public static String formatDate(Date date) {
        return date != null ? DATE_FORMAT.format(date) : "";
    }

    public static String formatDateTime(Date date) {
        return date != null ? DATE_TIME_FORMAT.format(date) : "";
    }

    private static Date parseDate(String datetime, DateFormat dateFormat) {
        try {
            return datetime == null || datetime.isEmpty() || TEXT_NULL.equals(datetime) ? null : dateFormat.parse(datetime);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Logs general structure import error.
     * 
     * @param errorsFile The errors log file.
     * @param error The error message to be logged.
     */
    public static void structError(File errorsFile, String error) {
        writeError("Structure import failed ", errorsFile, error);
    }

    /**
     * Logs general structure import exception.
     * 
     * @param errorsFile The errors log file.
     * @param error The caught exception to be logged.
     */
    public static void structError(File errorsFile, Exception error) {
        if (error instanceof ImportValidationException) {
            structError(errorsFile, error.getMessage());
        } else {
            structError(errorsFile, error.toString());
        }
    }

    /**
     * Logs general document import error.
     * 
     * @param errorsFile The errors log file.
     * @param error The error message to be logged.
     */
    public static void docsError(File errorsFile, String error) {
        writeError("Document import failed ", errorsFile, error);
    }

    /**
     * Logs general document import exception.
     * 
     * @param errorsFile The errors log file.
     * @param error The caught exception to be logged.
     */
    public static void docsError(File errorsFile, Exception error) {
        if (error instanceof ImportValidationException) {
            docsError(errorsFile, error.getMessage());
        } else {
            docsError(errorsFile, error.toString());
        }
    }

    /**
     * Logs work-flow import exception.
     * 
     * @param errorsFile The errors log file.
     * @param csvFile The CSV which caused the error.
     * @param error The caught exception to be logged.
     */
    public static void workflowError(File errorsFile, File csvFile, Exception error) {
        if (error instanceof ImportValidationException) {
            writeError(csvFile.getName(), errorsFile, error.getMessage());
        } else {
            writeError(csvFile.getName(), errorsFile, error.toString());
        }
    }

    /**
     * Logs general work-flow import error.
     * 
     * @param errorsFile The errors log file.
     * @param error The error message to be logged.
     */
    public static void workflowError(File errorsFile, String error) {
        writeError("Workflow import failed ", errorsFile, error);
    }

    /**
     * Logs general work-flow import exception.
     * 
     * @param errorsFile The errors log file.
     * @param error The caught exception to be logged.
     */
    public static void workflowError(File errorsFile, Exception error) {
        if (error instanceof ImportValidationException) {
            workflowError(errorsFile, error.getMessage());
        } else {
            workflowError(errorsFile, error.toString());
        }
    }

    /**
     * Logs document import exception.
     * 
     * @param errorsFile The errors log file.
     * @param docXmlFile The document XML which caused the error.
     * @param error The caught exception to be logged.
     */
    public static void docsError(File errorsFile, File docXmlFile, Exception error) {
        if (error instanceof ImportValidationException) {
            writeError(docXmlFile.getName(), errorsFile, error.getMessage());
        } else {
            writeError(docXmlFile.getName(), errorsFile, error.toString());
        }
    }

    private static void writeError(String prefix, File errorsFile, String error) {
        CsvWriter logWriter = null;
        try {
            logWriter = createLogWriter(errorsFile, true);
            logWriter.write(prefix);
            logWriter.write(error);
            logWriter.endRecord();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (logWriter != null) {
                logWriter.close();
            }
        }
    }

    /**
     * Deletes the provided log file when it exists.
     * 
     * @param logFile The log file to be deleted.
     */
    public static void dropLogFile(File logFile) {
        if (logFile.exists()) {
            logFile.delete();
        }
    }

    public static CsvReader createDataReader(File logFile) throws FileNotFoundException {
        CsvReader reader = new CsvReader(new BufferedInputStream(new FileInputStream(logFile)), ';', CHARSET_UTF8);
        reader.setTrimWhitespace(true);
        reader.setSkipEmptyRecords(true);
        return reader;
    }

    public static CsvReader createLogReader(File logFile) throws FileNotFoundException {
        return new CsvReader(new BufferedInputStream(new FileInputStream(logFile)), ';', CHARSET_UTF8);
    }

    public static CsvWriter createLogWriter(File logFile, boolean append) throws FileNotFoundException {
        return new CsvWriter(new FileOutputStream(logFile, append), ';', CHARSET_UTF8);
    }

}
