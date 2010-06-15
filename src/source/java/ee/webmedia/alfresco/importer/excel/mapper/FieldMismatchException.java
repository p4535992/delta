package ee.webmedia.alfresco.importer.excel.mapper;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

public class FieldMismatchException extends RuntimeException {

    private static final long serialVersionUID = -836740320124625142L;

    private String columnName;
    private int columnIndex = -1;
    private int rowIndex = -1;
    private String sheetName;
    private String file;
    private boolean renderDetailedMsg = true;

    private List<String> causeMessages = new ArrayList<String>(3);

    public FieldMismatchException(String message) {
        this(message, null);
    }

    public FieldMismatchException(String message, Throwable cause) {
        super(message, cause);
        causeMessages.add(message);
        if (cause instanceof FieldMismatchException) {
            FieldMismatchException fieldEx = (FieldMismatchException) cause;
            final List<FieldMismatchException> stack = new ArrayList<FieldMismatchException>();
            stack.add(this);
            do {
                if (cause instanceof FieldMismatchException) {
                    stack.add((FieldMismatchException) cause);
                }
                cause = cause.getCause();
            } while (cause != null);
            if (stack.size() > 1) {
                for (int i = stack.size() - 1; i > 0; i--) {
                    FieldMismatchException ex = stack.get(i);
                    final FieldMismatchException surroundingEx = stack.get(i - 1);
                    if (surroundingEx != null) {
                        surroundingEx.addDataFromCause(ex);
                        surroundingEx.populateCauseMessages(fieldEx);
                        fieldEx.renderDetailedMsg = false;
                    }
                }
            }
        }
    }

    private void populateCauseMessages(FieldMismatchException fieldEx) {
        List<String> causeMessagesOfCause = fieldEx.overTakeCauseMessages();
        if (causeMessagesOfCause != null && causeMessagesOfCause.size() > 0) {
            causeMessages.addAll(causeMessagesOfCause);
        }
    }

    private List<String> overTakeCauseMessages() {
        final List<String> causeMessages2passOver = causeMessages;
        this.causeMessages = null;
        return causeMessages2passOver;
    }

    private void addDataFromCause(FieldMismatchException fieldEx) {
        // if (true)
        // throw new RuntimeException("test addDataFromCause " + (this == fieldEx) + "--" + getMessage() + " \n\n\nchildEx:" + fieldEx.getMessage());
        if (StringUtils.isBlank(columnName)) {
            this.columnName = fieldEx.columnName;
        }
        if (columnIndex == -1) {
            this.columnIndex = fieldEx.columnIndex;
        }
        if (rowIndex == -1) {
            this.rowIndex = fieldEx.rowIndex;
        }
        if (StringUtils.isBlank(sheetName)) {
            this.sheetName = fieldEx.sheetName;
        }
        if (StringUtils.isBlank(file)) {
            this.file = fieldEx.file;
        }
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public void setColumnIndex(int columnIndex) {
        this.columnIndex = columnIndex;
    }

    @Override
    public String getMessage() {
        String unknown = "<unknown>";
        String detailedMsg = "";
        if (renderDetailedMsg) {
            StringBuilder sb = new StringBuilder("Problem reading data:\n")
                    .append("file\t\t=" + (StringUtils.isNotBlank(file) ? file : unknown))
                    .append("\nsheetName\t=" + (StringUtils.isNotBlank(sheetName) ? sheetName : unknown))
                    .append("\nrowNumber\t=" + (rowIndex != -1 ? (rowIndex + 1) : unknown))
                    .append("\ncolumnNumber\t=" + (columnIndex != -1 ? (columnIndex + 1) : unknown))
                    .append("\ncolumnName\t=" + (StringUtils.isNotBlank(columnName) ? columnName : unknown))
                    .append("\n---------------------------------------");
            if (causeMessages != null) {
                for (String causeMsg : causeMessages) {
                    sb.append("\n+").append(causeMsg);
                }
            }
            detailedMsg = sb.append("\n---------------------------------------\n\n").toString();
        }
        return renderDetailedMsg ? detailedMsg : super.getMessage();
    }

    public void setRowIndex(int i) {
        this.rowIndex = i;
    }

    public void setSheetName(String sheetName) {
        this.sheetName = sheetName;
    }

    public void setFile(String file) {
        this.file = file;
    }
}
