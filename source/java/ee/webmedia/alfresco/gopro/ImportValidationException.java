package ee.webmedia.alfresco.gopro;

import java.io.IOException;

import com.csvreader.CsvReader;

public class ImportValidationException extends Exception {

    private static final long serialVersionUID = 1L;

    private boolean reportAsSuccess;

    public ImportValidationException(String errorMsg) {
        this(errorMsg, false);
    }

    public ImportValidationException(String errorMsg, boolean reportAsSuccess) {
        super(errorMsg);
        this.reportAsSuccess = reportAsSuccess;
    }

    public boolean isReportAsSuccess() {
        return reportAsSuccess;
    }

    public ImportValidationException(String errorMsg, CsvReader reader) {
        super(String.format("%s on row %d", errorMsg, reader.getCurrentRecord() + 2)); // record numbering starts from 0 and after headers.
    }

    public ImportValidationException(String errorMsg, CsvReader reader, int col) throws IOException {
        this(String.format(errorMsg, reader.getHeader(col)), reader);
    }
}
