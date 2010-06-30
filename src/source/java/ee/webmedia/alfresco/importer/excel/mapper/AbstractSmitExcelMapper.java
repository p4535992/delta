package ee.webmedia.alfresco.importer.excel.mapper;

import static org.apache.commons.lang.StringUtils.isBlank;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.Row;

import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.classificator.enums.StorageType;
import ee.webmedia.alfresco.document.model.LetterDocument;
import ee.webmedia.alfresco.importer.excel.service.DocumentImportServiceImpl;
import ee.webmedia.alfresco.importer.excel.vo.ImportDocument;
import ee.webmedia.alfresco.importer.excel.vo.IncomingLetter;
import ee.webmedia.alfresco.importer.excel.vo.OutgoingLetter;
import ee.webmedia.alfresco.importer.excel.vo.SendInfo;

/**
 * Abstract baseclass for reading excel files into documents. This class takes advantage of {@link ExcelColumn} annotation, that can be used to define what
 * column is desired to be mapped into corresponding field.
 * 
 * @author Ats Uiboupin
 */
public abstract class AbstractSmitExcelMapper<IDoc extends ImportDocument> extends ExcelRowMapper<IDoc> {

    // START: fields that are don't change the location for different importable excel sheets
    /** A: Sarja tähis (function&seires) */
    @ExcelColumn('A')
    private Integer SeriesMark;
    /** B: Toimiku tähis (volume) */
    @ExcelColumn('B')
    private Integer Volume;
    /** C: Reg Nr. (regNumber) */
    @ExcelColumn('C')
    private Integer RegNumber;
    // END: fields that are don't change the location for different importable excel sheets

    // START: fields that change the location for different importable excel sheets(Value set by )
    /** D: Kuupäev (regDateTime) - süsteemis dokumendi registreerimise kuupäev */
    @ExcelColumn('D')
    private Integer RegDateTime;
    /** Pealkiri (docName) */
    @ExcelColumn(/* value set by subclass */)
    private Integer DocName;
    /** Dokumendi link */
    @ExcelColumn(/* value set by subclass */)
    private Integer Link;
    /** Märkused (comment) */
    @ExcelColumn(/* value set by subclass */)
    private Integer Comment;
    /** Juurdepääsupiirang (accessRestrictionEndDate&accessRestrictionEndDesc) */
    @ExcelColumn(/* value set by subclass */)
    private Integer AccessRestriction;
    // END: fields that change the location for different importable excel sheets

    /** Used to write nodeRef of imported document back to repo */
    @ExcelColumn('Z')
    private Integer DocNodeRefInRepo;
    private static long orderOfAppearance;

    protected Map<String, Object> mapperContext;
    public static final String ATTACHMENT_FILES_LOCATION_BASE = "ATTACHMENT_FILES_LOCATION_BASE";

    abstract protected IDoc createDocument(Row row);

    @Override
    public IDoc mapRow(Row row, long rowNr, File rowSourceFile, String rowSourceSheetName) {
        orderOfAppearance++;
        final IDoc doc = createDocument(row);
        { // ühised
            fillRowOriginInformation(doc, rowSourceFile, rowSourceSheetName, row);
            fillCommonFields(row, doc);
        }

        setDocStatus(doc);
        postProcess(doc);
        return doc;
    }

    @Override
    public void setMapperContext(Map<String, Object> mapperContext) {
        this.mapperContext = mapperContext;
    }

    protected void postProcess(@SuppressWarnings("unused") IDoc doc) {
        // could override in subclass
    }

    /** Save the origin of the row */
    private void fillRowOriginInformation(IDoc doc, File rowSourceFile, String rowSourceSheetName, Row row) {
        doc.setRowSourceFile(rowSourceFile);
        doc.setRowSourceSheet(rowSourceSheetName);
        doc.setRowSourceNumber(row.getRowNum());
        final String docNodeRefInRepo = get(row, DocNodeRefInRepo);
        if (StringUtils.startsWith(docNodeRefInRepo, "workspace://")) {
            doc.setNodeRefInRepo(docNodeRefInRepo);
        } else if (StringUtils.isNotBlank(docNodeRefInRepo)) {
            final FieldMismatchException fieldMismatchException = new FieldMismatchException(
                    "Column, that is supposed to be reserved for document location after import, already contains data : '" + docNodeRefInRepo + "'");
            fieldMismatchException.setColumnName("Z");
            throw fieldMismatchException;
        }
        doc.setOrderOfAppearance(orderOfAppearance);
    }

    private void fillCommonFields(Row row, final IDoc doc) {
        fillDocLocation(doc, row);
        final String docName = get(row, DocName);
        assertFieldIsFilled("DocName", docName);
        doc.setDocName(docName);
        final String fileLocation = get(row, Link);
        addFileIfExists(doc, fileLocation, Link);
        addToComment(doc, "Märkused", get(row, Comment));
        setStorageType(doc);
        doc.setRegNumber(get(row, RegNumber));
        setRegDateTime(row, doc);
        { // ühised va. lepingud
            fillAccessRestrictions(row, doc);
        }
    }

    protected void addFileIfExists(final IDoc doc, String fileLocation, int columnNumber) {
        if (StringUtils.isBlank(fileLocation)) {
            return;
        }
        fileLocation = fileLocation.replace('\\', '/');
        final String attachmentFilesLocationBase = (String) mapperContext.get("ATTACHMENT_FILES_LOCATION_BASE");
        File file = DocumentImportServiceImpl.getFile(attachmentFilesLocationBase, fileLocation);
        handleMissingFileReference(attachmentFilesLocationBase + fileLocation, file, columnNumber);
        doc.addFileLocation(fileLocation);
    }

    public static void main(String[] args) {
        final String base = "C:\\tmp\\atstest\\importExcel\\attachments\\";
        final String dir = "Dokumentide asukoht\\Kirjad\\Kirjavahetus riigiasutuste, org. ja kodanikega. 1.2-6\\kirjavahetus 2008\\";
        final String dir2 = base + dir;
        final String file = dir2 + ".žõäöüš ŠÕÄÖÜŽ 12.06.08_reg_nr_51_Siseministri 26.08.05 kk nr 348 'VIS töörühma moodustamine' muutmine.msgx";
        handleMissingFileReference(file, new File(file), 3);
    }

    private static void handleMissingFileReference(String fileLocation, File file, int columnIndex) {
        if (file == null) {
            file = new File(fileLocation);
        }
        if (!file.exists()) {
            final String END_CONST = "\"";
            final String START_CONST = "\n-\t\"";
            final StringBuilder sb = new StringBuilder("File doesn't exist:" + START_CONST)
                    .append(file).append(END_CONST);
            do {
                file = file.getParentFile();
                if (file == null || file.exists()) {
                    break;
                }
                sb.append(START_CONST).append(file.getAbsolutePath()).append(END_CONST);
            } while (!file.exists());
            if (file != null) {
                sb.append("\nFollowing parent exists:\n+\t\"").append(file.getAbsolutePath()).append(END_CONST);
                sb.append("\nfiles in existing parent dir:");
                final File[] listFiles = file.listFiles();
                for (File file2 : listFiles) {
                    sb.append("\n+" + (file2.isDirectory() ? "D" : "") + "\t\"").append(file2.getAbsolutePath()).append(END_CONST);
                }
            } else {
                sb.append("\n-\t\"" + "Even root doesn't exists");
            }
            final FieldMismatchException fieldMismatchException = new FieldMismatchException(sb.toString());
            fieldMismatchException.setColumnIndex(columnIndex);//
            throw fieldMismatchException;
        }
    }

    private void setRegDateTime(Row row, final IDoc doc) {
        doc.setRegDateTime(get(row, RegDateTime, Date.class));
    }

    protected void addToComment(final IDoc doc, String commentFieldName, String comment) {
        if (StringUtils.isBlank(comment)) {
            return;
        }
        String existingComment = StringUtils.trimToEmpty(doc.getComment());
        if (StringUtils.isNotBlank(existingComment)) {
            existingComment = existingComment + "\n";
        }
        comment = commentFieldName + ": " + comment;
        doc.setComment(existingComment + comment);
    }

    private void fillDocLocation(IDoc doc, Row row) {
        final String seriesMark = get(row, SeriesMark);
        assertFieldIsFilled("SeriesMark", seriesMark);
        final int splitIndex = seriesMark.indexOf("-");
        if (splitIndex >= 0) {
            doc.setFunction(seriesMark.substring(0, splitIndex).trim());
            assertFieldIsFilled("Function (parsed from seriesMark '" + seriesMark + "')", doc.getFunction());
            doc.setSeries(seriesMark);
            assertFieldIsFilled("Series (parsed from seriesMark '" + seriesMark + "')", doc.getSeries());
        }
        String vol = get(row, Volume);
        assertFieldIsFilled("Volume", vol);
        setVolume(doc, seriesMark, vol);
    }

    protected void setVolume(IDoc doc, final String seriesMark, String vol) {
        vol = seriesMark + "/" + vol;
        doc.setVolume(vol);
    }

    /**
     * NB! assumes that:
     * if doc is letter - then sendmode is set
     * if doc is not letter - then file location is set(if found)
     */
    private void setStorageType(final IDoc doc) {
        String sendMode = null;
        String storageType = null;
        if (doc instanceof LetterDocument) {
            if (doc instanceof IncomingLetter) {
                IncomingLetter in = (IncomingLetter) doc;
                sendMode = in.getTransmittalMode();
            } else if (doc instanceof OutgoingLetter) {
                OutgoingLetter out = (OutgoingLetter) doc;
                SendInfo sendInfo = out.getSendInfo();
                if (sendInfo != null) {
                    sendMode = sendInfo.getSendMode();
                }
            }
            if (ee.webmedia.alfresco.classificator.enums.SendMode.EMAIL.equals(sendMode)) {
                storageType = StorageType.DIGITAL.getValueName();
            } else {
                storageType = StorageType.PAPER.getValueName();
            }
        } else {
            final List<String> fileLocations = doc.getFileLocations();
            if (fileLocations == null || fileLocations.size() == 0) {
                storageType = StorageType.PAPER.getValueName();
            } else {
                storageType = StorageType.DIGITAL.getValueName();
            }
        }
        doc.setStorageType(storageType);
    }

    protected void fillAccessRestrictions(Row row, final IDoc doc) {
        fillAccessRestriction(doc, get(row, AccessRestriction));
        if (StringUtils.isBlank(doc.getRegNumber()) != (null == doc.getRegDateTime())) {
            final FieldMismatchException fieldMismatchException = new FieldMismatchException(
                    "Document registration date and number must both be filled or both empty. Doc=\n" + doc);
            fieldMismatchException.setColumnName("RegNr/Kuupäev");
            throw fieldMismatchException;
        }
    }

    /**
     * NB! Assumes that doc.setRegDateTime() is already called
     */
    protected void fillAccessRestriction(IDoc doc, String accessRestriction) {
        if (StringUtils.isBlank(accessRestriction)) {
            doc.setAccessRestriction(ee.webmedia.alfresco.classificator.enums.AccessRestriction.OPEN.getValueName());
        } else {
            doc.setAccessRestriction(accessRestriction); // changed when importing - then series is known and accessRestriction is taken from there
        }
        final Date accessRestrictionDate = extractDate(accessRestriction);
        if (accessRestrictionDate != null) {
            doc.setAccessRestrictionEndDate(accessRestrictionDate);
        } else {
            doc.setAccessRestrictionEndDesc(accessRestriction);
        }
        doc.setAccessRestrictionBeginDate(doc.getRegDateTime());
    }

    /**
     * NB! if IncomingLetter, dueDate must be set before.
     * otherwise regNumber must be set before.
     */
    private void setDocStatus(final IDoc doc) {
        final DocumentStatus docStatus;
        if (doc instanceof IncomingLetter) {
            boolean hasDueDate = null != ((IncomingLetter) doc).getDueDate();
            docStatus = hasDueDate ? DocumentStatus.FINISHED : DocumentStatus.WORKING;
        } else {
            if (StringUtils.isBlank(doc.getRegNumber())) {
                docStatus = DocumentStatus.WORKING;
            } else {
                docStatus = DocumentStatus.FINISHED;
            }
        }
        doc.setDocStatus(docStatus.getValueName());
    }

    private void assertFieldIsFilled(String field, String value) {
        if (isBlank(value)) {
            final FieldMismatchException fieldMismatchException = new FieldMismatchException(field + " must be filled, but value is empty: '" + value + "'");
            fieldMismatchException.setColumnName(field);
            throw fieldMismatchException;
        }
    }

    protected void assertNotNull(String field, Object value) {
        if (value == null) {
            final FieldMismatchException fieldMismatchException = new FieldMismatchException(field + " must be filled, but value is empty: '" + value + "'");
            fieldMismatchException.setColumnName(field);
            throw fieldMismatchException;
        }
    }

    public static void resetOrderOfAppearance() {
        orderOfAppearance = 0;
    }

}