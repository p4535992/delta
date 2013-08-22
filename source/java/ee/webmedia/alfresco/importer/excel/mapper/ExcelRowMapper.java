package ee.webmedia.alfresco.importer.excel.mapper;

import java.io.File;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.hssf.record.formula.eval.ErrorEval;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.util.Assert;

public abstract class ExcelRowMapper<G> {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(ExcelRowMapper.class);
    private static final String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
    private static final String dateSeparator = "\\.";

    /**
     * Patter to match first date inside input text
     */
    private static final Pattern dateMatcherPattern = Pattern.compile(
            "(^|.*?\\D)" + // before date: either only the start of the input or char-sequence that is not ending with a digit
                    "(" + // start capturing group
                    "(0[1-9]|[1-9]|[12][0-9]|3[01])" + // day part of the date
                    dateSeparator + //
                    "(0[1-9]|1[012]|[1-9])" + // month part of the date
                    dateSeparator + //
                    "\\d{4}" + // year part of the date
                    ")" + // end capturing group
                    "($|\\D.*)" // after date: either only the end of input or char-sequence that doesn't start with a digit
            );

    public abstract G mapRow(Row row, long rowNr, File excelFile, String string);

    public void setMapperContext(@SuppressWarnings("unused") Map<String, Object> mapperContext) {
        // could be overridden in subclassses
    }

    protected String get(Row row, int colIndex) {
        return StringUtils.trimToNull(get(row, colIndex, String.class));
    }

    /**
     * @param row
     * @param colIndex
     * @return date value of cell or if cell is not date try to extract date from text. If text contains no date, null is returned.
     */
    protected Date getDate(Row row, int colIndex) {
        try {
            return get(row, colIndex, Date.class);
        } catch (FieldMismatchException e) {
            // final String cellTextValue = getCellValue(cell);
            final String cellTextValue = get(row, colIndex, String.class);
            final Date extractedDate = extractDate(cellTextValue);
            if (extractedDate == null) {
                final FieldMismatchException fieldMismatchException = new FieldMismatchException("Also failed to extract date from cell textvalue '"
                        + cellTextValue + "'", e);
                fieldMismatchException.setColumnIndex(colIndex);
                throw fieldMismatchException;
            }
            if (log.isTraceEnabled()) {
                log.trace((row.getRowNum() + 1) + ". row " + (colIndex + 1) + ". column is expected to be Date, but is text '" //
                        + cellTextValue + "'. Using extracted date from text: " + extractedDate);
            }
            return extractedDate;
        }
    }

    protected <T> T get(Row row, int colIndex, Class<T> clazz) {
        final Cell cell = row.getCell(colIndex);
        if (cell == null) {
            return null;
        }
        if (clazz == null || clazz == String.class) {
            final int cellType = cell.getCellType();
            if (cellType == Cell.CELL_TYPE_STRING) {
                @SuppressWarnings("unchecked")
                T res = (T) cell.getStringCellValue();
                return res;
            } else if (cellType == Cell.CELL_TYPE_BLANK) {
                return null;
            } else if (cellType == Cell.CELL_TYPE_NUMERIC) {
                final double numericCellValue = cell.getNumericCellValue();
                final long longValue = (long) numericCellValue;
                @SuppressWarnings("unchecked")
                final T stringVal = (T) ((longValue == numericCellValue) ? String.valueOf(longValue) : Double.valueOf(numericCellValue).toString());
                return stringVal;
            } else if (cellType == Cell.CELL_TYPE_BOOLEAN) {
                @SuppressWarnings("unchecked")
                T res = (T) Boolean.valueOf(cell.getBooleanCellValue()).toString();
                return res;
            } else if (cellType == Cell.CELL_TYPE_FORMULA) {
                throw new RuntimeException("cell with formula: " + cell.getCellFormula());
            } else if (cellType == Cell.CELL_TYPE_ERROR) {
                throw new RuntimeException("cell with error: " + cell);
            }
            // } else {
        } else if (clazz == Date.class) {
            Date dateCellValue;
            try {
                dateCellValue = cell.getDateCellValue();
            } catch (Exception e) {
                final FieldMismatchException fieldMismatchException = new FieldMismatchException("Can't get " + clazz + " value from " + (colIndex + 1)
                        + ". column with type "
                        + getCellTypeName(cell.getCellType())
                        + ". Cell value:\n'" + getCellValue(cell) + "'", e);
                fieldMismatchException.setColumnIndex(colIndex);
                throw fieldMismatchException;
            }
            @SuppressWarnings("unchecked")
            T result = (T) dateCellValue;
            return result;
        }
        throw new RuntimeException("unimplemented");
    }

    private Object getCellValue(final Cell cell) {
        final Object value;
        final int cellType = cell.getCellType();
        if (cellType == Cell.CELL_TYPE_STRING) {
            value = cell.getStringCellValue();
        } else if (cellType == Cell.CELL_TYPE_BLANK) {
            value = null;
        } else if (cellType == Cell.CELL_TYPE_NUMERIC) {
            value = cell.getNumericCellValue();
        } else if (cellType == Cell.CELL_TYPE_BOOLEAN) {
            value = cell.getBooleanCellValue();
        } else if (cellType == Cell.CELL_TYPE_FORMULA) {
            value = evaluateFormula(cell);
        } else if (cellType == Cell.CELL_TYPE_ERROR) {
            // throw new FieldMismatchException("cell with error: " + cell);
            return cell.toString();
        } else {
            throw new RuntimeException("unknown cell type: " + cellType + "; cell:\n" + cell);
        }
        return value;
    }

    protected Date extractDate(String textContainingDate) {
        if (StringUtils.isNotBlank(textContainingDate)) {
            Matcher matcher = dateMatcherPattern.matcher(textContainingDate.trim());
            if (matcher.find()) {
                final String dateString = matcher.group(2);
                if (StringUtils.isNotBlank(dateString)) {
                    try {
                        final Date parse = dateFormat.parse(dateString);
                        return parse;
                    } catch (ParseException e) {
                        final String msg = "Failed to parse '" + dateString + "' to Date";
                        log.error(msg, e);
                        throw new RuntimeException(msg, e);
                    }
                }
            }
        }
        return null;
    }

    private Object evaluateFormula(final Cell cell) {
        if (cell instanceof HSSFCell) {
            final int cachedFormulaResultType = cell.getCachedFormulaResultType();
            switch (cachedFormulaResultType) {
            case Cell.CELL_TYPE_STRING:
                StringBuffer text = new StringBuffer();
                HSSFRichTextString str = ((HSSFCell) cell).getRichStringCellValue();
                if (str != null && str.length() > 0) {
                    text.append(str.toString());
                }
                return text.toString();
            case Cell.CELL_TYPE_NUMERIC:
                return cell.getNumericCellValue();
            case Cell.CELL_TYPE_BOOLEAN:
                return cell.getBooleanCellValue();
            case Cell.CELL_TYPE_ERROR:
                return ErrorEval.getText(cell.getErrorCellValue());
            default:
                throw new RuntimeException("Unimplemented reading formula from cell with formula type " + cachedFormulaResultType);
            }
        }
        throw new RuntimeException("Unimplemented reading formula from cell with class " + cell.getClass());
    }

    /**
     * Method copied from private method {@link HSSFCell#getCellType()}
     */
    private static String getCellTypeName(int cellTypeCode) {
        switch (cellTypeCode) {
        case Cell.CELL_TYPE_BLANK:
            return "blank";
        case Cell.CELL_TYPE_STRING:
            return "text";
        case Cell.CELL_TYPE_BOOLEAN:
            return "boolean";
        case Cell.CELL_TYPE_ERROR:
            return "error";
        case Cell.CELL_TYPE_NUMERIC:
            return "numeric";
        case Cell.CELL_TYPE_FORMULA:
            return "formula";
        }
        return "#unknown cell type (" + cellTypeCode + ")#";
    }

    public void setExcelColumnsFromAnnotations() {
        final HashMap<String/* fieldName */, Integer/* colNr */> fieldToColNrMap = new HashMap<String, Integer>();
        getFields(this, this.getClass(), fieldToColNrMap);
    }

    private void getFields(Object object, final Class<?> clazz, HashMap<String, Integer> fieldToColNrMap) {
        final Field[] fields = clazz.getDeclaredFields();
        for (final Field field : fields) {
            final ExcelColumn excelCol = field.getAnnotation(ExcelColumn.class);
            if (excelCol != null) {
                if (field.getType() != Integer.class) {
                    throw new RuntimeException("Field not integer");// at the moment only Integers are supported
                }
                try {
                    Integer colNr = null;
                    final String fieldName = field.getName();
                    if (!fieldToColNrMap.containsKey(fieldName)) {
                        final char colLetter = excelCol.value();
                        Arrays.asList(letters);
                        if ('-' == colLetter) {
                            colNr = excelCol.colNr();
                        } else {
                            colNr = letters.indexOf(colLetter);
                        }
                        if (colNr == Integer.MIN_VALUE) {
                            colNr = null; // default value of annotation can't be null, using Integer.MIN_VALUE to denote it
                        }
                        fieldToColNrMap.put(fieldName, colNr);
                    } else {
                        colNr = fieldToColNrMap.get(fieldName); // prefer annotation value of the corresponding filed from childClass
                    }
                    Object fieldValBefore = null;
                    field.setAccessible(true);
                    if (log.isDebugEnabled()) {
                        fieldValBefore = field.get(object);
                    }
                    field.set(object, colNr);
                    if (log.isTraceEnabled()) {
                        Object fieldValAfter = field.get(object);
                        if ((colNr == null) != (fieldValAfter == null)) {
                            Assert.isTrue(false, "Expected that the value after setting field is equal to value set to field");
                        }
                        if (fieldValAfter != null) {
                            Assert.isTrue(fieldValAfter.equals(colNr), "Expected that the value after setting field is equal to value set to field");
                        }
                        log.trace("Setting value of field '" + field + "' based on @ExcelColumn from '" + fieldValBefore + "' to '" + colNr + "'");
                    }
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException("failed  ", e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("failed to access filed " + field, e);
                } catch (SecurityException e) {
                    throw new RuntimeException("failed to get field - no access", e);
                    // } catch (NoSuchFieldException e) {
                    // throw new RuntimeException("failed to get field - no field", e);
                }
            }
        }
        final Class<?> superclass = clazz.getSuperclass();
        if (superclass != null) {
            setFields(object, superclass, fieldToColNrMap);
            getFields(object, superclass, fieldToColNrMap);
        }
    }

    private void setFields(Object object, Class<?> clazz, HashMap<String, Integer> fieldToColNrMap) {
        for (Entry<String, Integer> entry : fieldToColNrMap.entrySet()) {
            try {
                final Field field = clazz.getDeclaredField(entry.getKey());
                final ExcelColumn excelCol = field.getAnnotation(ExcelColumn.class);
                if (excelCol != null) { // only overwrite values of fields in parentClass if field is annotated
                    field.setAccessible(true);
                    field.set(object, entry.getValue());
                }
            } catch (SecurityException e) {
                throw new RuntimeException("Failed", e);
            } catch (NoSuchFieldException e) {
                // ignore
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Failed to set", e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to set", e);
            }
        }
    }

    public static class SheetFinder {
        private String[] sheetNames;

        public SheetFinder(String... sheetNames) {
            if (sheetNames == null || sheetNames.length == 0) {
                throw new RuntimeException("At least one sheet name is required");
            }
            this.sheetNames = sheetNames;
        }

        /** find from all sheets */
        public SheetFinder() {
            // find from all sheets
        }

        public ArrayList<Sheet> findSheetsToImport(Workbook wb) {
            final ArrayList<Sheet> sheets = new ArrayList<Sheet>();
            if (sheetNames == null) {
                final int numberOfSheets = wb.getNumberOfSheets();
                for (int i = 0; i < numberOfSheets; i++) {
                    sheets.add(wb.getSheetAt(i));
                }
            } else {
                for (String sheet : sheetNames) {
                    sheets.add(wb.getSheet(sheet));
                }
            }
            return sheets;
        }

        @Override
        public String toString() {
            return sheetNames != null ? Arrays.asList(sheetNames).toString() : "ALL SHEETS";
        }

    }
}