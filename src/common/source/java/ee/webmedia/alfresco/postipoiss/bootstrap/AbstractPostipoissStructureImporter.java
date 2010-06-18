package ee.webmedia.alfresco.postipoiss.bootstrap;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.repo.transaction.TransactionListenerAdapter;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;

import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;

import ee.webmedia.alfresco.common.service.GeneralService;

public abstract class AbstractPostipoissStructureImporter extends AbstractModuleComponent {
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(AbstractPostipoissStructureImporter.class);

    protected static final String ATTR_MULTIPLE_YEARS = "mitu asjaajamisaastat";
    protected static final String ATTR_SINGLE_YEAR = "üks asjaajamis aasta";
    protected static final String INPUT_ENCODING = "ISO-8859-1";
    protected static final String OUTPUT_ENCODING = "UTF-8";
    protected static final char OUTPUT_SEPARATOR = ';';

    protected GeneralService generalService;
    protected FileFolderService fileFolderService;

    protected boolean enabled = false;
    protected StoreRef store;
    protected String parentPath;
    protected String inputFolderPath;

    protected Map<String, NodeRef> functionCache = new LinkedHashMap<String, NodeRef>();
    protected Map<Integer, NodeRef> seriesCache = new LinkedHashMap<Integer, NodeRef>();
    protected DateFormat ppDateFormat = new SimpleDateFormat("dd/MM/yy");
    {
        ppDateFormat.setLenient(false);
    }

    @Override
    protected void executeInternal() throws Throwable {
        if (!enabled) {
            return;
        }

        createFunctions();
        createSeries();
    }

    protected void createFunctions() throws Exception {
        final String inputFilePath = inputFolderPath + "/struktuur.csv";
        log.info("Reading Postipoiss functions file '" + inputFilePath + "' with encoding " + INPUT_ENCODING);
        CsvReader reader = new CsvReader(inputFilePath, ';', Charset.forName(INPUT_ENCODING));
        try {
            while (reader.readRecord()) {
                try {
                    createFunction(reader);
                } catch (Exception e) {
                    throw new RuntimeException("Error while importing function from row index " + reader.getCurrentRecord() + " in file " + inputFilePath, e);
                }
            }
        } finally {
            reader.close();
        }
        AlfrescoTransactionSupport.bindListener(new TransactionListenerAdapter() {
            @Override
            public void afterCommit() {
                CsvWriter writer = null;
                try {
                    String logFilePath = inputFilePath + "_log.csv";
                    log.info("Creating log file " + logFilePath);
                    writer = getCsvWriter(logFilePath);
                    writer.writeRecord(new String[] { "functionId", "nodeRef" });
                    for (Entry<String, NodeRef> entry : functionCache.entrySet()) {
                        writer.writeRecord(new String[] { entry.getKey(), entry.getValue().toString() });
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    if (writer != null) {
                        writer.close();
                    }
                }
                log.info("Completed importing Postipoiss functions file and committed successfully. Please DISABLE FUNCTIONS IMPORT from now on!");
            }
        });
    }

    protected CsvWriter getCsvWriter(String filename) throws IOException {
        OutputStream outputStream = new FileOutputStream(filename);

        // the Unicode value for UTF-8 BOM, is needed so that Excel would recognise the file in correct encoding
        outputStream.write("\ufeff".getBytes(OUTPUT_ENCODING));

        CsvWriter writer = new CsvWriter(outputStream, OUTPUT_SEPARATOR, Charset.forName(OUTPUT_ENCODING));
        return writer;
    }

    protected void createFunction(CsvReader reader) throws IOException {
        String functionId = reader.get(0);
        String title = reader.get(1);
        String parentFunctionId = reader.get(2);
        NodeRef functionRef = createFunction(functionId, title, parentFunctionId);
        functionCache.put(functionId, functionRef);
        log.debug("Created functionId=" + functionId + " nodeRef=" + functionRef);
    }

    protected abstract NodeRef createFunction(String functionId, String title, String parentFunctionId);

    protected void createSeries() throws Exception {
        final String inputFilePath = inputFolderPath + "/toimikud.csv";
        log.info("Reading Postipoiss series file '" + inputFilePath + "' with encoding " + INPUT_ENCODING);
        CsvReader reader = new CsvReader(inputFilePath, ';', Charset.forName(INPUT_ENCODING));
        try {
            while (reader.readRecord()) {
                try {
                    createSeries(reader);
                } catch (Exception e) {
                    throw new RuntimeException("Error while importing series from row index " + reader.getCurrentRecord() + " in file " + inputFilePath, e);
                }
            }
        } finally {
            reader.close();
        }
        AlfrescoTransactionSupport.bindListener(new TransactionListenerAdapter() {
            @Override
            public void afterCommit() {
                CsvWriter writer = null;
                try {
                    String logFilePath = inputFilePath + "_log.csv";
                    log.info("Creating log file " + logFilePath);
                    writer = getCsvWriter(logFilePath);
                    writer.writeRecord(new String[] { "seriesId", "nodeRef" });
                    for (Entry<Integer, NodeRef> entry : seriesCache.entrySet()) {
                        writer.writeRecord(new String[] { Integer.toString(entry.getKey()), entry.getValue().toString() });
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    if (writer != null) {
                        writer.close();
                    }
                }
                log.info("Completed importing Postipoiss series file and committed successfully. Please DISABLE SERIES IMPORT from now on!");
            }
        });
    }

    protected void createSeries(CsvReader reader) throws IOException, ParseException {
        if (reader.getColumnCount() < 5) {
            return;
        }
        int seriesId = Integer.parseInt(reader.get(0));
        String functionId = reader.get(1);
        String registrationNumber = reader.get(2);
        String title = reader.get(3);
        String multipleYearsValue = reader.get(4);
        boolean multipleYears;
        if (ATTR_MULTIPLE_YEARS.equals(multipleYearsValue)) {
            multipleYears = true;
        } else if (ATTR_SINGLE_YEAR.equals(multipleYearsValue)) {
            multipleYears = false;
        } else {
            throw new RuntimeException("Invalid value in column index 4: " + multipleYearsValue);
        }
        Date validFrom = null;
        Date validTo = null;
        if (!multipleYears) {
            validFrom = ppDateFormat.parse(reader.get(5));
            validTo = ppDateFormat.parse(reader.get(6));
        }

        NodeRef functionRef = functionCache.get(functionId);
        NodeRef seriesRef = createSeries(seriesId, functionRef, registrationNumber, title, multipleYears, validFrom, validTo);
        seriesCache.put(seriesId, seriesRef);
        log.debug("Created seriesId=" + seriesId + " nodeRef=" + seriesRef);
    }

    protected abstract NodeRef createSeries(int seriesId, NodeRef functionRef, String registrationNumber, String title, boolean multipleYears, Date validFrom, Date validTo);

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setFileFolderService(FileFolderService fileFolderService) {
        this.fileFolderService = fileFolderService;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setStore(String store) {
        this.store = new StoreRef(store);
    }

    public void setParentPath(String parentPath) {
        this.parentPath = parentPath;
    }

    public void setInputFolderPath(String inputFolderPath) {
        this.inputFolderPath = inputFolderPath;
    }

}
