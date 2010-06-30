package ee.webmedia.alfresco.importer.excel.bootstrap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.faces.context.FacesContext;

import org.alfresco.service.namespace.QName;
import org.apache.commons.io.IOUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import ee.webmedia.alfresco.document.web.evaluator.SmitExcelImportEvaluator;
import ee.webmedia.alfresco.importer.excel.mapper.AbstractSmitExcelMapper;
import ee.webmedia.alfresco.importer.excel.mapper.ContractSmitMapper;
import ee.webmedia.alfresco.importer.excel.mapper.ExcelRowMapper;
import ee.webmedia.alfresco.importer.excel.mapper.FieldMismatchException;
import ee.webmedia.alfresco.importer.excel.mapper.LetterMapper;
import ee.webmedia.alfresco.importer.excel.mapper.MinutesDocumentMapper;
import ee.webmedia.alfresco.importer.excel.mapper.RegulationMapper;
import ee.webmedia.alfresco.importer.excel.mapper.ExcelRowMapper.SheetFinder;
import ee.webmedia.alfresco.importer.excel.service.DocumentImportService;
import ee.webmedia.alfresco.importer.excel.vo.ImportDocument;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * @author Ats Uiboupin
 *         There is quite a lot of hardCoded configuration in this file, as it is only supposed to be used once and hence there is not much point of
 *         dividing configuration (such as the names and the sheets of excel files - that are agreed not to change) into separate files.
 */
public class SmitExcelImporter {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(SmitExcelImporter.class);

    private DocumentImportService documentImportService;
    private String importFolderLocation;

    private int batchSize = 100;
    private boolean processRunning;
    private long nrOfDocsFromFirstLaunch;
    private long nrOfDocsTotalToImport;
    private String attachmentFilesLocationBase;

    public <IDoc extends ImportDocument> void importSmitDocList() {
        if (!new SmitExcelImportEvaluator().evaluate(null)) {
            throw new RuntimeException("Import already executed or no rights to import!");
        }
        if (processRunning) {
            MessageUtil.addInfoMessage(FacesContext.getCurrentInstance(),
                    "docList_import_smitExcel_processRunning", nrOfDocsFromFirstLaunch, nrOfDocsTotalToImport);
            return;
        }
        executeInternal(true);// first test (not much point of optimizing, as reading from excel is very fast compared to importing to repository)
        processRunning = true;
        AbstractSmitExcelMapper.resetOrderOfAppearance();
        nrOfDocsTotalToImport = 0;
        executeInternal(false); // second real import
    }

    private <IDoc extends ImportDocument> void executeInternal(boolean onlyTestReading) {
        try {
            log.info("Starting " + (onlyTestReading ? "testing" : "importing from") + " excel files");
            long startTime = System.currentTimeMillis();
            final HashMap<ExcelRowMapper<IDoc>, Map<File, SheetFinder>> sheetsByMappers = getSheetsByMappers();
            long duration = 0;
            long nrOfDocsTotal = 0;
            long nrOfSheetsImported = 0;
            for (Entry<ExcelRowMapper<IDoc>, Map<File, SheetFinder>> mapperSheetsEntry : sheetsByMappers.entrySet()) {
                // documents that are handled the same way (by same excelRowMapper)
                final ExcelRowMapper<IDoc> excelRowMapper = mapperSheetsEntry.getKey();
                for (Entry<File, SheetFinder> entry : mapperSheetsEntry.getValue().entrySet()) {
                    // process documents from the same file
                    long startTimeFile = System.currentTimeMillis();
                    long numberOfDocumentsInFile = 0;
                    final File importFile = entry.getKey();
                    final SheetFinder sheetFinder = entry.getValue();
                    ensureFileIsWritable(importFile);
                    List<List<IDoc>> documentsBySheets = getDocuments(importFile, sheetFinder, excelRowMapper);
                    for (List<IDoc> documents : documentsBySheets) {
                        // documents from the same sheet
                        String sheetName = null;
                        long startTimeSheet = System.currentTimeMillis();
                        long numberOfDocumentsOnSheet = 0;
                        final List<List<IDoc>> documentBatches = splitIntoBatches(documents, batchSize);
                        for (List<? extends ImportDocument> documentsBatch : documentBatches) {
                            // part of documents from the same sheet, to decrease the size of transaction (to increase the processing speed)
                            final int currentBatchSize = documentsBatch.size();
                            if (currentBatchSize == 0) {
                                continue;
                            }
                            long batchStart = System.currentTimeMillis();
                            if (!onlyTestReading) {
                                documentImportService.importDocuments(documentsBatch);
                            }
                            numberOfDocumentsOnSheet += currentBatchSize;
                            if (!onlyTestReading) {
                                nrOfDocsFromFirstLaunch += currentBatchSize;
                            }
                            duration = System.currentTimeMillis() - batchStart;
                            if (log.isInfoEnabled()) {
                                sheetName = documentsBatch.get(0).getRowSourceSheet();
                                log.info("Batch completed: " + (onlyTestReading ? "tested " : "imported ") + currentBatchSize + " documents from "
                                        + importFile.getName() + "[" + sheetName + "] sheet in " + duration + "ms - avg " + (duration / currentBatchSize)
                                        + "ms per doc");
                            }
                        }
                        numberOfDocumentsInFile += numberOfDocumentsOnSheet;
                        printResultsInformation(importFile.getName(), sheetFinder, documents);
                        duration = System.currentTimeMillis() - startTimeSheet;
                        if (log.isInfoEnabled() && documents.size() > 0) {
                            log.info("Batches completed: completed " + (onlyTestReading ? "testing" : "importing") + " sheet " + importFile.getName() + "["
                                    + sheetName + "] in " + duration + "ms - processed " + numberOfDocumentsOnSheet + " documents, avg "
                                    + (duration / documents.size()) + "ms per doc");
                        }
                        nrOfSheetsImported++;
                    }
                    duration = System.currentTimeMillis() - startTimeFile;
                    if (log.isInfoEnabled() && numberOfDocumentsInFile > 0) {
                        log.info("Sheet completed: completed " + (onlyTestReading ? "testing" : "importing") + importFile.getName() + "[" + sheetFinder
                                + "] in " + duration + "ms - processed " + numberOfDocumentsInFile
                                + (numberOfDocumentsInFile == 0 ? "" : " documents, avg " + (duration / numberOfDocumentsInFile) + "ms per doc"));
                    }
                    nrOfDocsTotal += numberOfDocumentsInFile;
                }
            }
            if (!onlyTestReading) {
                nrOfDocsTotalToImport = nrOfDocsTotal;
                documentImportService.createAssocs();
                if (nrOfSheetsImported > 0) {
                    MessageUtil.addInfoMessage(FacesContext.getCurrentInstance(), "docList_import_smitExcel_success", nrOfDocsTotalToImport);
                }
            }

            duration = System.currentTimeMillis() - startTime;
            if (log.isInfoEnabled() && nrOfDocsTotal > 0) {
                log.info("All files completed: Completed " + (onlyTestReading ? "testing" : "importing") + " all documents " + duration + "ms - processed "
                        + nrOfDocsTotal + " documents from " + nrOfSheetsImported + " sheets, avg " + (duration / nrOfDocsTotal) + "ms per doc");
            }
        } catch (RuntimeException e) {
            log.error("importing paused due to problems - fix them and try again");
            throw e;
        } finally {
            if (!onlyTestReading) {
                processRunning = false;
            }
        }
    }

    private void ensureFileIsWritable(File importFile) {
        if (!importFile.exists()) {
            throw new RuntimeException("File doesn't exist! " + importFile.getAbsolutePath());
        }
        FileOutputStream fileOut = null;
        FileInputStream inp = null;
        try {
            inp = new FileInputStream(importFile);
            final Workbook wb = WorkbookFactory.create(inp);
            fileOut = new FileOutputStream(importFile);
            wb.write(fileOut);
        } catch (FileNotFoundException e) {
            final String msg = "Can't write to file as it is used by another process";
            log.debug(msg + " (the cause exception being loged is misleading)", e);
            throw new RuntimeException(msg, e);
        } catch (Exception e) {
            throw new RuntimeException("Can't write to file", e);
        } finally {
            IOUtils.closeQuietly(inp);
            IOUtils.closeQuietly(fileOut);
        }
    }

    private <IDoc extends ImportDocument> HashMap<ExcelRowMapper<IDoc>, Map<File, SheetFinder>> getSheetsByMappers() {
        final HashMap<ExcelRowMapper<IDoc>, Map<File, SheetFinder>> sheetsByMappers = new HashMap<ExcelRowMapper<IDoc>, Map<File, SheetFinder>>();
        final File excelFileSmit_dokumentRegister = new File(importFolderLocation, "SMIT_dokumendiregister.xls");
        addLetterMapperAndSheets(excelFileSmit_dokumentRegister, sheetsByMappers);
        addRegulationMapperAndSheets(excelFileSmit_dokumentRegister, sheetsByMappers);
        addMinutesMapperAndSheets(excelFileSmit_dokumentRegister, sheetsByMappers);
        addContractMapperAndSheets(sheetsByMappers);
        return sheetsByMappers;
    }

    @SuppressWarnings("unchecked")
    private <IDoc extends ImportDocument> void addLetterMapperAndSheets(final File excelFile,
            final HashMap<ExcelRowMapper<IDoc>, Map<File, SheetFinder>> sheetsByMappers) {
        LetterMapper letterMapper = new LetterMapper();
        final Map<File, SheetFinder> sheetsToImport = new HashMap<File, SheetFinder>();
        sheetsToImport.put(excelFile, new SheetFinder("Kirjavahetus", "Riigisaladuse juurdepääs"));
        sheetsByMappers.put((ExcelRowMapper<IDoc>) letterMapper, sheetsToImport);
    }

    @SuppressWarnings("unchecked")
    private <IDoc extends ImportDocument> void addRegulationMapperAndSheets(final File excelFile,
            final HashMap<ExcelRowMapper<IDoc>, Map<File, SheetFinder>> sheetsByMappers) {
        RegulationMapper mapper = new RegulationMapper();
        final Map<File, SheetFinder> sheetsToImport = new HashMap<File, SheetFinder>();
        sheetsToImport.put(excelFile, new SheetFinder("Üldtegevuse käskkirjad"));
        sheetsByMappers.put((ExcelRowMapper<IDoc>) mapper, sheetsToImport);
    }

    @SuppressWarnings("unchecked")
    private <IDoc extends ImportDocument> void addMinutesMapperAndSheets(final File excelFile,
            HashMap<ExcelRowMapper<IDoc>, Map<File, SheetFinder>> sheetsByMappers) {
        MinutesDocumentMapper mapper = new MinutesDocumentMapper();
        final Map<File, SheetFinder> sheetsToImport = new HashMap<File, SheetFinder>();
        sheetsToImport.put(excelFile, new SheetFinder("Operatiivsete nõup. protokollid"));
        sheetsToImport.put(new File(importFolderLocation, "Erilahenduste konsolideerimise protokollide register.xls"), new SheetFinder());
        sheetsToImport.put(new File(importFolderLocation, "Juhtkonna noupidamiste protokollide register.xls"), new SheetFinder());
        sheetsToImport.put(new File(importFolderLocation, "Operatiivsete noupidamiste protokollide register 2008-2009.xls"), new SheetFinder());
        sheetsToImport.put(new File(importFolderLocation, "ORS-i protokollide register.xls"), new SheetFinder());
        sheetsToImport.put(new File(importFolderLocation, "Projektinoukogu noupidamiste protokollide register.xls"), new SheetFinder());
        sheetsToImport.put(new File(importFolderLocation, "SIM valitsemisala IKT konsolideerimise protokollide register.xls"), new SheetFinder());
        sheetsByMappers.put((ExcelRowMapper<IDoc>) mapper, sheetsToImport);
    }

    @SuppressWarnings("unchecked")
    private <IDoc extends ImportDocument> void addContractMapperAndSheets(HashMap<ExcelRowMapper<IDoc>, Map<File, SheetFinder>> sheetsByMappers) {
        ContractSmitMapper mapper = new ContractSmitMapper();
        final Map<File, SheetFinder> sheetsToImport = new HashMap<File, SheetFinder>();
        sheetsToImport.put(new File(importFolderLocation, "Lepingute register.xls"), new SheetFinder());
        sheetsByMappers.put((ExcelRowMapper<IDoc>) mapper, sheetsToImport);
    }

    @SuppressWarnings("unchecked")
    private <IDoc extends ImportDocument> List<List<IDoc>> splitIntoBatches(List<? extends ImportDocument> documents, int batchSize) {
        final ArrayList<List<IDoc>> allDocuments = new ArrayList<List<IDoc>>((documents.size() / batchSize) + 1);
        ArrayList<ImportDocument> batchDocuments = new ArrayList<ImportDocument>();
        for (int i = 0; i < documents.size(); i++) {
            batchDocuments.add(documents.get(i));
            if (i != 0 && i % batchSize == 0 || ((i + 1 == documents.size()))) {
                allDocuments.add((List<IDoc>) batchDocuments);
                batchDocuments = new ArrayList<ImportDocument>();
            }
        }
        return allDocuments;
    }

    /**
     * @param <IDoc>
     * @param excelFile - source file of documents to be read
     * @param sheetFinder - determines the sheet or sheets to be read from the file
     * @param excelRowMapper - handles each row in the file resulting an instance of &lt;IDoc&gt; in returned list
     * @return instances of &lt;IDoc&gt; read from the sheets of <code>excelFile</code>
     */
    private <IDoc extends ImportDocument> List<List<IDoc>> getDocuments(final File excelFile, SheetFinder sheetFinder, ExcelRowMapper<IDoc> excelRowMapper) {
        InputStream inp = null;
        try {
            inp = new FileInputStream(excelFile);
            final Workbook wb = WorkbookFactory.create(inp);
            final ArrayList<Sheet> sheets = sheetFinder.findSheetsToImport(wb);

            excelRowMapper.setExcelColumnsFromAnnotations(); // do the magic with special fields

            final List<List<IDoc>> documentsBySheets = new ArrayList<List<IDoc>>();
            final HashMap<String, Set<RuntimeException>> exceptionsBySheets = new HashMap<String, Set<RuntimeException>>();
            for (Sheet sheet : sheets) {
                Set<RuntimeException> exceptions = new HashSet<RuntimeException>();
                final List<IDoc> documents = new ArrayList<IDoc>();
                final String fileAndSheet = processSheet(documents, excelFile, excelRowMapper, sheet, exceptions);
                final int nrOfExceptions = exceptions.size();
                if (nrOfExceptions > 0) {
                    final String msg = "Got " + nrOfExceptions + " exceptions while reading " + fileAndSheet;
                    log.error(msg);
                    exceptionsBySheets.put(sheet.getSheetName(), exceptions); // throw exceptions later
                }
                documentsBySheets.add(documents);
            }
            handleExceptions(excelFile, exceptionsBySheets);
            return documentsBySheets;
        } catch (Exception e) {
            throw new RuntimeException("Failed to read from file " + excelFile, e);
        } finally {
            IOUtils.closeQuietly(inp);
        }
    }

    private void handleExceptions(final File excelFile, final HashMap<String, Set<RuntimeException>> exceptionsBySheets) {
        String msg = null;
        RuntimeException lastException = null;
        int totalNrOfExceptions = 0;
        for (Entry<String, Set<RuntimeException>> entry : exceptionsBySheets.entrySet()) {
            final String sheetName = entry.getKey();
            final Set<RuntimeException> exceptions = entry.getValue();
            final int nrOfExceptions = exceptions.size();
            if (nrOfExceptions > 0) {
                final String fileAndSheet = excelFile.getAbsolutePath() + "'[" + sheetName + "]";
                msg = "Got " + nrOfExceptions + " exceptions while reading " + fileAndSheet;
                log.error(msg);
                long exceptionNr = 0;
                for (Exception exception : exceptions) {
                    exceptionNr++;
                    log.error(exceptionNr + ". problem with document, that prevented importing. Exception :\n", exception);
                    // XXX: could also paint columns red where the problem is(if exception indirectly caused by FieldMismatchException)
                    /*
                     * kliendil kulus vigade leidmiseks ca kaks täis tööpäeva...
                     * CellStyle style = wb.createCellStyle();
                     * style.setFillBackgroundColor(IndexedColors.RED.getIndex());
                     * cell.setCellStyle(style);
                     */
                }
                log.error("Correct input files to be imported!");
                lastException = exceptions.iterator().next();
            }
            totalNrOfExceptions += nrOfExceptions;
        }
        if (lastException != null) {
            throw new RuntimeException(
                    msg
                            + ". Correct input files to be imported! Last exception is bellow(look into log file to see all problems with sheets of this file being imported - there were "
                            + totalNrOfExceptions + " problems in total with documents in this file).",
                    lastException);
        }
    }

    private <IDoc> String processSheet(final List<IDoc> documents, final File excelFile, ExcelRowMapper<IDoc> excelRowMapper, final Sheet sheet,
            Set<RuntimeException> exceptions) {
        int i = 0;
        Row lastRow = null;

        final String fileAndSheet = excelFile.getAbsolutePath() + "'[" + sheet.getSheetName() + "]";
        excelRowMapper.setMapperContext(getMapperContext());
        for (Row row : sheet) {
            try {
                lastRow = row;
                if (i++ == 0) {
                    continue;
                }
                documents.add(excelRowMapper.mapRow(row, i, excelFile, sheet.getSheetName()));
            } catch (RuntimeException e) {
                final FieldMismatchException fieldMismatchException = new FieldMismatchException("Failed to parse " + i + ".row from '" + fileAndSheet
                        + "; rowNum=" + (lastRow != null ? (lastRow.getRowNum() + 1) : -1) + " ", e);
                fieldMismatchException.setRowIndex(i - 1);
                fieldMismatchException.setSheetName(sheet.getSheetName());
                fieldMismatchException.setFile(excelFile.getAbsolutePath());
                exceptions.add(fieldMismatchException);
            }
        }
        return fileAndSheet;
    }

    private Map<String, Object> getMapperContext() {
        Map<String, Object> mapperContext = new HashMap<String, Object>(1);
        mapperContext.put(AbstractSmitExcelMapper.ATTACHMENT_FILES_LOCATION_BASE, attachmentFilesLocationBase);
        return mapperContext;
    }

    private <IDoc extends ImportDocument> void printResultsInformation(String fileName, final SheetFinder sheetFinder, final List<IDoc> documents) {
        final HashMap<QName, Integer> documentTypes = new HashMap<QName, Integer>();
        int i = 0;
        for (IDoc importDoc : documents) {
            i++;
            final QName documentTypeId = importDoc.getDocumentTypeId();
            Integer counter = documentTypes.get(documentTypeId);
            if (counter == null) {
                documentTypes.put(documentTypeId, 1);
            } else {
                documentTypes.put(documentTypeId, ++counter);
            }
        }
        final Set<Entry<QName, Integer>> entrySet = documentTypes.entrySet();
        log.debug("imported " + documents.size() + " documents from '" + fileName + "'[" + sheetFinder + "] document types are:");
        for (Entry<QName, Integer> entry : entrySet) {
            log.info("\t" + entry.getKey().getLocalName() + ": " + entry.getValue());
        }
    }

    public void setDocumentImportService(DocumentImportService documentImportService) {
        this.documentImportService = documentImportService;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public void setImportFolderLocation(String importFolderLocation) {
        this.importFolderLocation = importFolderLocation;
    }
    
    public void setAttachmentFilesLocationBase(String attachmentFilesLocationBase) {
        attachmentFilesLocationBase = attachmentFilesLocationBase.replace('\\', '/');
        if (!attachmentFilesLocationBase.endsWith("/")) {
            attachmentFilesLocationBase = attachmentFilesLocationBase + "/";
        }
        this.attachmentFilesLocationBase = attachmentFilesLocationBase;
    }

}
