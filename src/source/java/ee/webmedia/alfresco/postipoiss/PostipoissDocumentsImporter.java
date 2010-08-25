package ee.webmedia.alfresco.postipoiss;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.TransactionListenerAdapter;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.util.Assert;

import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;

import ee.webmedia.alfresco.cases.model.Case;
import ee.webmedia.alfresco.cases.service.CaseService;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.sendout.service.SendOutService;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.functions.model.Function;
import ee.webmedia.alfresco.functions.service.FunctionsService;
import ee.webmedia.alfresco.postipoiss.PostipoissDocumentsMapper.ConvertException;
import ee.webmedia.alfresco.postipoiss.PostipoissDocumentsMapper.DocumentValue;
import ee.webmedia.alfresco.postipoiss.PostipoissDocumentsMapper.Mapping;
import ee.webmedia.alfresco.postipoiss.PostipoissDocumentsMapper.Pair;
import ee.webmedia.alfresco.postipoiss.PostipoissDocumentsMapper.PropMapping;
import ee.webmedia.alfresco.postipoiss.PostipoissDocumentsMapper.PropertyValue;
import ee.webmedia.alfresco.series.model.Series;
import ee.webmedia.alfresco.series.service.SeriesService;
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.volume.service.VolumeService;

/**
 * Imports documents and files from postipoiss.
 * 
 * @author Aleksei Lissitsin
 */
public class PostipoissDocumentsImporter implements BeanFactoryAware {

    protected static final int BATCH_SIZE = 50;
    protected static final String OPEN_VOLUME_YEAR = "10";
    protected static final char CSV_SEPARATOR = ';';

    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(PostipoissDocumentsImporter.class);

    private boolean started = false;

    protected SAXReader xmlReader = new SAXReader();
    protected File mappingsFile;

    // [SPRING BEANS
    private boolean enabled = false;
    private String inputFolderPath;
    private int stopAfterDocumentId = 999999;
    private String mappingsFileName;
    private String functionName;

    private TransactionService transactionService;
    private DocumentService documentService;
    private GeneralService generalService;
    private FileFolderService fileFolderService;
    private NamespaceService namespaceService;
    private DictionaryService dictionaryService;
    private SendOutService sendOutService;
    private NodeService nodeService;
    private PostipoissDocumentsMapper postipoissDocumentsMapper;
    private String inputFolderCsv;

    // INJECTORS
    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setFileFolderService(FileFolderService fileFolderService) {
        this.fileFolderService = fileFolderService;
    }

    public void setDictionaryService(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    public void setDocumentService(DocumentService documentService) {
        this.documentService = documentService;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setInputFolderPath(String inputFolderPath) {
        this.inputFolderPath = inputFolderPath;
    }

    public void setMappingsFileName(String mappingsFileName) {
        this.mappingsFileName = mappingsFileName;
    }

    public void setStopAfterDocumentId(int stopAfterDocumentId) {
        this.stopAfterDocumentId = stopAfterDocumentId;
    }

    public void setTransactionService(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    public void setPostipoissDocumentsMapper(PostipoissDocumentsMapper postipoissDocumentsMapper) {
        this.postipoissDocumentsMapper = postipoissDocumentsMapper;
    }

    public void setNamespaceService(NamespaceService namespaceService) {
        this.namespaceService = namespaceService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setSendOutService(SendOutService sendOutService) {
        this.sendOutService = sendOutService;
    }

    public void setInputFolderCsv(String inputFolderCsv) {
        this.inputFolderCsv = inputFolderCsv;
    }

    // SPRING BEANS]

    // [TEMPORARY STUFF
    private BeanFactory beanFactory;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    private static void lo(Object o) {
        log.info("\n\n" + o + "\n\n");
    }

    private Object b(String name) {
        return beanFactory.getBean(name);
    }

    @SuppressWarnings("unchecked")
    private <T> T b(Class<T> clazz) {
        String simpleName = clazz.getSimpleName();
        if (!beanFactory.containsBean(simpleName)) {
            simpleName = simpleName.substring(0, 1).toLowerCase() + simpleName.substring(1);
        }
        return (T) b(simpleName);
    }

    // TEMPORARY STUFF]

    /**
     * Runs documents import process
     */
    public void runImport() throws Exception {
        started = true;
        if (!enabled) {
            return;
        }

        try {
            runInternal();
            log.info("\nImport completed\n");
        } finally {
            started = false;
        }
    }

    private void tryAssocs() {
        NodeRef volume = new NodeRef("workspace://SpacesStore/56603519-7de2-440c-b0cd-9bc06f2e4425");

        Case asi = b(CaseService.class).createCase(volume);
        asi.setTitle("BLA");
        b(CaseService.class).saveOrUpdate(asi, false);

    }

    private void init() {
        inputFolder = new File(inputFolderPath);
    }

    private void runInternal() throws Exception {
        init();
        try {
            loadDocuments();
            loadCompletedDocuments();
            mappings = postipoissDocumentsMapper.loadMetadataMappings(new File(mappingsFileName));
            loadToimiks();
            loadPostponedAssocs();
            createDocuments();
            loadFiles();
            loadCompletedFiles();
            importFiles();
        } catch (Exception e) {
            log.info("IMPORT FAILED");
            throw e;
        } finally {
            writePostponedAssocs();
            reset();
        }
    }
    
    private void reset(){
        documentsMap = new TreeMap<Integer, File>();
        filesMap = new TreeMap<Integer, List<File>>();
        completedDocumentsMap = new TreeMap<Integer, NodeRef>();
        completedFiles = new HashSet<Integer>();
        mappings = null;
        toimikud = null;
        normedToimikud = null;
    }

    protected NavigableMap<Integer /* documentId */, File> documentsMap = new TreeMap<Integer, File>();
    protected NavigableMap<Integer /* documentId */, List<File>> filesMap = new TreeMap<Integer, List<File>>();
    protected NavigableMap<Integer /* documentId */, NodeRef> completedDocumentsMap = new TreeMap<Integer, NodeRef>();

    protected Set<Integer> completedFiles = new HashSet<Integer>();
    protected NavigableSet<Integer> filesToProceed;
    protected File completedDocumentsFile;
    protected File completedFilesFile;

    private Map<String, Mapping> mappings;

    protected void loadDocuments() {
        documentsMap = new TreeMap<Integer, File>();
        completedDocumentsFile = new File(inputFolder, "completed_docs.csv");
        log.info("Getting xml entries of " + inputFolder);
        File[] files = inputFolder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return StringUtils.isNotBlank(name) && name.endsWith(".xml");
            }
        });

        log.info("Directory listing contains " + files.length + " xml entries, parsing them to documents");
        for (File file : files) {
            String name = file.getName();
            if (name.endsWith(".xml")) {
                // Document
                int i = name.lastIndexOf("_");
                if (i == -1)
                    continue;
                String documentType = name.substring(0, i);
                if (StringUtils.isBlank(documentType))
                    continue;
                try {
                    Integer documentId = new Integer(name.substring(i + 1, name.length() - 4));
                    log.debug("Found documentId=" + documentId + " documentType='" + documentType + "'");
                    documentsMap.put(documentId, file);
                } catch (NumberFormatException e) {
                }
            }
        }
        log.info("Completed parsing directory listing, got " + documentsMap.size() + " documents");
    }

    protected void loadFiles() {
        filesMap = new TreeMap<Integer, List<File>>();
        completedFilesFile = new File(inputFolder, "completed_files.csv");
        log.info("Getting non-xml entries of " + inputFolder);
        File[] files = inputFolder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File arg0, String name) {
                return StringUtils.isNotBlank(name) && !name.endsWith(".xml");
            }
        });

        log.info("There are " + files.length + " non-xml entries, parsing them to files");
        int count = 0;
        for (File file : files) {
            String name = file.getName();
            int i = name.lastIndexOf(".");
            if (i == -1)
                continue;
            String filename = name.substring(0, i); // TODO file names are probably in wrong encoding. -17-65 -67
            if (StringUtils.isBlank(filename))
                continue;
            try {
                Integer documentId = new Integer(name.substring(i + 1));
                log.info("Found documentId=" + documentId + " filename='" + filename + "'");
                List<File> fileList = filesMap.get(documentId);
                if (fileList == null) {
                    fileList = new ArrayList<File>();
                    filesMap.put(documentId, fileList);
                }
                fileList.add(file);
                count++;
            } catch (NumberFormatException e) {
            }
        }
        log.info("Completed parsing directory listing, got " + count + " files");
    }

    protected void loadCompletedFiles() throws Exception {
        completedFiles = new HashSet<Integer>();

        if (!completedFilesFile.exists()) {
            log.info("Skipping loading previously completed files, file does not exist: " + completedDocumentsFile);
        } else {

            log.info("Loading previously completed files from file " + completedFilesFile);

            CsvReader reader = new CsvReader(new BufferedInputStream(new FileInputStream(completedFilesFile)), CSV_SEPARATOR, Charset.forName("UTF-8"));
            try {
                reader.readHeaders();
                while (reader.readRecord()) {
                    Integer documentId = new Integer(reader.get(0));
                    completedFiles.add(documentId);
                }
            } finally {
                reader.close();
            }
            log.info("Loaded " + completedFiles.size() + " documents with previously completed files");

            int removedFilesCount = 0;
            for (Integer documentId : completedFiles) {
                List<File> filesList = filesMap.remove(documentId);
                if (filesList != null) {
                    removedFilesCount += filesList.size();
                }
            }
            log.info("Removed those files from current file list (total " + removedFilesCount + " files)");
        }

        filesToProceed = new TreeSet<Integer>(filesMap.keySet());
        filesToProceed.retainAll(completedDocumentsMap.keySet());

        log.info("Total documents with files to proceed: " + filesToProceed.size());
    }

    private abstract class BatchProgress<E> {
        Collection<E> origin;
        List<E> batchList;
        int batchSize = BATCH_SIZE;
        int totalSize;
        int i;
        int completedSize;;
        long totalStartTime;
        long startTime;
        String processName;

        private void init() {
            totalSize = origin.size();
            totalStartTime = System.currentTimeMillis();
            startTime = totalStartTime;
            i = 0;
            completedSize = 0;
            batchList = new ArrayList<E>(batchSize);
        }

        abstract void executeBatch() throws Exception;

        void executeInTransaction() {
            getTransactionHelper().doInTransaction(new RetryingTransactionCallback<Object>() {
                @Override
                public Object execute() throws Throwable {
                    executeBatch();
                    return null;
                }
            });
        }

        private void step() {
            executeInTransaction();
            completedSize += batchList.size();
            batchList = new ArrayList<E>(batchSize);
            long endTime = System.currentTimeMillis();
            double completedPercent = completedSize * 100 / ((double) totalSize);
            double lastDocsPerSec = i * 1000 / ((double) (endTime - startTime));
            double totalDocsPerSec = completedSize * 1000 / ((double) (endTime - totalStartTime));
            i = 0;
            log.info(String.format("%s: %6.2f%% completed - %7d of %7d, %5.1f docs per second (last), %5.1f (total)", processName,
                    completedPercent, completedSize, totalSize, lastDocsPerSec, totalDocsPerSec));
            startTime = endTime;
        }

        public void run() {
            init();
            for (E e : origin) {
                batchList.add(e);
                i++;
                if (i >= batchSize) {
                    step();
                }
            }
            step();
        }

    }

    private class FilesBatchProgress extends BatchProgress<Integer> {

        public FilesBatchProgress(Set<Integer> origin) {
            this.origin = origin;
            processName = "Files import";
        }

        @Override
        void executeBatch() {
            createFilesBatch(batchList);
        }
    }

    private void importFiles() {
        int previousSize = filesToProceed.size();
        NavigableSet<Integer> headSet = filesToProceed.headSet(stopAfterDocumentId, true);
        log.info("Removed documentId-s after stopAfterDocumentId from current doc list: " + previousSize + " -> " + headSet.size());
        if (headSet.size() == 0) {
            log.info("There are no files to import.");
        } else {
            log.info("Starting files import. First documentId=" + headSet.first() + " stopAfterDocumentId=" + stopAfterDocumentId);
            FilesBatchProgress batchProgress = new FilesBatchProgress(headSet);
            batchProgress.run();
            log.info("Files IMPORT COMPLETE :)");
        }
    }

    protected void createFilesBatch(final List<Integer> batchList) {
        final Set<Integer> batchCompletedFiles = new HashSet<Integer>(BATCH_SIZE);
        for (Integer documentId : batchList) {
            if (log.isTraceEnabled()) {
                log.trace("Processing files with docId = " + documentId);
            }
            NodeRef documentRef = completedDocumentsMap.get(documentId);
            addFiles(documentId, documentRef);
            batchCompletedFiles.add(documentId);
        }
        bindCsvWriteAfterCommit(completedFilesFile, new CsvWriterClosure() {

            @Override
            public void execute(CsvWriter writer) throws IOException {
                for (Integer documentId : batchCompletedFiles) {
                    writer.writeRecord(new String[] {
                            documentId.toString()
                    });
                    completedFiles.add(documentId);
                }
            }

            @Override
            public String[] getHeaders() {
                return new String[] { "documentId" };
            }
        });
    }

    private static interface CsvWriterClosure {
        void execute(CsvWriter writer) throws IOException;

        String[] getHeaders();
    }

    private static void bindCsvWriteAfterCommit(final File file, final CsvWriterClosure closure) {
        AlfrescoTransactionSupport.bindListener(new TransactionListenerAdapter() {
            @Override
            public void afterCommit() {
                try {
                    // Write created documents
                    boolean exists = file.exists();
                    if (!exists) {
                        OutputStream outputStream = new FileOutputStream(file);
                        try {
                            // the Unicode value for UTF-8 BOM, is needed so that Excel would recognise the file in correct encoding
                            outputStream.write("\ufeff".getBytes("UTF-8"));
                        } finally {
                            outputStream.close();
                        }
                    }
                    CsvWriter writer = new CsvWriter(new FileWriter(file, true), CSV_SEPARATOR);
                    try {
                        if (!exists) {
                            writer.writeRecord(closure.getHeaders());
                        }
                        closure.execute(writer);
                    } finally {
                        writer.close();
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Error writing file '" + file + "': " + e.getMessage(), e);
                }
            }
        });
    }

    protected void loadCompletedDocuments() throws Exception {
        completedDocumentsMap = new TreeMap<Integer, NodeRef>();

        if (!completedDocumentsFile.exists()) {
            log.info("Skipping loading previously completed documentId-s, file does not exist: " + completedDocumentsFile);
            return;
        }

        log.info("Loading previously completed documentId-s from file " + completedDocumentsFile);

        CsvReader reader = new CsvReader(new BufferedInputStream(new FileInputStream(completedDocumentsFile)), CSV_SEPARATOR, Charset.forName("UTF-8"));
        try {
            reader.readHeaders();
            while (reader.readRecord()) {
                Integer documentId = new Integer(reader.get(0));
                NodeRef documentRef = new NodeRef(reader.get(1));
                completedDocumentsMap.put(documentId, documentRef);
            }
        } finally {
            reader.close();
        }
        log.info("Loaded " + completedDocumentsMap.size() + " previously completed documentId-s");

        int previousSize = documentsMap.size();
        documentsMap.keySet().removeAll(completedDocumentsMap.keySet());
        /*
         * Assert.isTrue(previousSize - documentsMap.size() == completedDocumentsMap.size(), previousSize + " " + documentsMap.size() + " "
         * + completedDocumentsMap.size());
         */
        log.info("Removed previously completed documentId-s from current document list: " + previousSize + " -> " + documentsMap.size());
    }

    private static class Toimik {
        String normedMark;
        NodeRef nodeRef;
        int year;

        @Override
        public String toString() {
            return "Toimik [nodeRef=" + nodeRef + ", normedMark=" + normedMark + ", year=" + year + "]";
        }
    }

    private Map<Integer, Map<String, Toimik>> toimikud;
    private Map<Integer, Map<String, Toimik>> normedToimikud;

    private Toimik getToimik(int year, String mark, boolean normed) {
        Map<Integer, Map<String, Toimik>> bmap = normed ? normedToimikud : toimikud;
        Map<String, Toimik> map = bmap.get(year);
        if (map == null)
            return null;
        return map.get(mark);
    }

    private void putToimik(String mark, Toimik t, boolean normed) {
        Map<Integer, Map<String, Toimik>> bmap = normed ? normedToimikud : toimikud;
        Map<String, Toimik> map = bmap.get(t.year);
        if (map == null) {
            map = new HashMap<String, Toimik>();
            bmap.put(t.year, map);
        }
        map.put(mark, t);
    }

    private void loadToimiks() throws IOException {
        toimikud = new HashMap<Integer, Map<String, Toimik>>();
        normedToimikud = new HashMap<Integer, Map<String, Toimik>>();
        CsvReader reader = new CsvReader(new BufferedInputStream(new FileInputStream(new File(inputFolderCsv, "completed_toimikud.csv"))), CSV_SEPARATOR,
                Charset.forName("UTF-8"));
        try {
            reader.readHeaders();
            while (reader.readRecord()) {
                int year = Integer.parseInt(reader.get(0));
                String normedMark = reader.get(1);
                String mark = reader.get(2);
                Toimik toimik = getToimik(year, normedMark, true);
                if (toimik == null) {
                    toimik = new Toimik();
                    toimik.year = year;
                    toimik.normedMark = normedMark;
                    toimik.nodeRef = new NodeRef(reader.get(3));
                    putToimik(normedMark, toimik, true);
                }
                putToimik(mark, toimik, false);
            }
        } finally {
            reader.close();
        }
    }

    private class DocumensBatchProgress extends BatchProgress<Entry<Integer, File>> {
        {
            origin = documentsMap.entrySet();
            processName = "Documents import";
        }

        @Override
        void executeBatch() throws Exception {
            createDocumentsBatch(batchList);
        }
    }

    protected void createDocuments() throws Exception {
        int previousSize = documentsMap.size();
        documentsMap = documentsMap.headMap(stopAfterDocumentId, true);
        log.info("Removed documentId-s after stopAfterDocumentId from current document list: " + previousSize + " -> " + documentsMap.size());

        if (documentsMap.size() == 0) {
            log.info("There are no documents to import.");
        } else {
            log.info("Starting documents import. First documentId=" + documentsMap.keySet().iterator().next() + " stopAfterDocumentId=" + stopAfterDocumentId);
            DocumensBatchProgress batchProgress = new DocumensBatchProgress();
            batchProgress.run();
            log.info("Documents and files IMPORT COMPLETE :)");
        }

    }

    static class ImportedDocument {
        Integer documentId;
        NodeRef nodeRef;

        public ImportedDocument(Integer documentId, NodeRef nodeRef) {
            this.documentId = documentId;
            this.nodeRef = nodeRef;
        }
    }

    protected void createDocumentsBatch(final List<Entry<Integer, File>> batchList) throws DocumentException, ParseException {
        final Map<Integer, ImportedDocument> batchCompletedDocumentsMap = new TreeMap<Integer, ImportedDocument>();
        // FOR ASSOCS
        this.batchCompletedDocumentsMap = batchCompletedDocumentsMap;
        for (Entry<Integer, File> entry : batchList) {
            Integer documentId = entry.getKey();
            if (log.isTraceEnabled()) {
                log.trace("Processing documentId=" + documentId);
            }
            File file = entry.getValue();
            ImportedDocument doc = createDocument(documentId, file);
            if (doc == null)
                continue;
            batchCompletedDocumentsMap.put(documentId, doc);
        }
        bindCsvWriteAfterCommit(completedDocumentsFile, new CsvWriterClosure() {

            @Override
            public String[] getHeaders() {
                return new String[] {
                        "documentId",
                        "nodeRef"
                };
            }

            @Override
            public void execute(CsvWriter writer) throws IOException {
                for (Entry<Integer, ImportedDocument> entry : batchCompletedDocumentsMap.entrySet()) {
                    ImportedDocument doc = entry.getValue();
                    writer.writeRecord(new String[] {
                            doc.documentId.toString(),
                            doc.nodeRef.toString()
                    });
                    completedDocumentsMap.put(entry.getKey(), doc.nodeRef);
                }
            }
        });
    }

    private ImportedDocument createDocument(Integer documentId, File file) throws DocumentException, ParseException {
        Node importDoc = importDoc(file, mappings, documentId);
        if (importDoc == null)
            return null;
        return new ImportedDocument(documentId, importDoc.getNodeRef());
    }

    private void addFiles(Integer documentId, NodeRef importDocRef) {
        List<File> fileList = filesMap.get(documentId);
        if (fileList != null) {
            for (File file : fileList) {
                String fileName = file.getName();
                int i = fileName.lastIndexOf('.');
                fileName = fileName.substring(0, i);
                addFile(fileName, file, importDocRef, null);
            }
        }
    }

    private Map<QName, Serializable> mapProperties(Element root, Mapping mapping) {
        DocumentValue docValue = new DocumentValue(mapping.typeInfo);

        for (PropMapping pm : mapping.props) {
            String value = null;
            if (pm.expression == null) {
                Element el = root.element(pm.from);
                if (el != null) {
                    value = el.getStringValue();
                }
            } else {
                if (pm.expression.equals("xpath")) {
                    value = PostipoissUtil.findAnyValue(root, pm.from);
                } else if (pm.expression.equals("const")) {
                    value = pm.from;
                } else {
                    throw new RuntimeException("Bad expression type " + pm.expression);
                }
            }
            if (value != null) {
                if (pm.splitter == null) {
                    docValue.put(value, pm.to, pm.prefix);
                } else {
                    try {
                        Pair pair = pm.splitter.split(value);
                        docValue.putObject(pair.first, pm.toFirst);
                        docValue.putObject(pair.second, pm.toSecond);
                    } catch (ConvertException e) {
                        if (pm.to != null) {
                            docValue.put(value, pm.to, pm.prefix);
                        }
                    }
                }
            }
        }

        HashMap<QName, Serializable> propsMap = new HashMap<QName, Serializable>();

        for (PropertyValue propertyValue : docValue.props.values()) {
            propsMap.put(propertyValue.qname, propertyValue.get());

        }

        return propsMap;
    }

    private static class VolumeIndex {
        String mark;
        String regNumber;
        int year;

        @Override
        public String toString() {
            return "VolumeIndex [mark=" + mark + ", regNumber=" + regNumber + ", year=" + year + "]";
        }
    }

    private VolumeIndex inferVolumeIndex(Element root, Mapping m) {
        VolumeIndex vi = new VolumeIndex();
        String toimikSari = root.elementText("toimik_sari");
        vi.regNumber = root.elementText("regNr");

        if (toimikSari != null) {
            toimikSari = toimikSari.trim();
        }

        if (StringUtils.isBlank(toimikSari)) {
            if (m.defaultVolume != null) {
                vi.mark = m.defaultVolume;
                vi.year = 10;
            } else {
                return null;
            }
        } else {
            int i = toimikSari.lastIndexOf("/");
            if (i == -1) {
                return null;
            }
            String rn2 = toimikSari.substring(i + 1);
            if (vi.regNumber == null) {
                vi.regNumber = rn2;
            }
            if (StringUtils.isBlank(vi.regNumber)) {
                return null;
            }

            String rest = toimikSari.substring(0, i);

            if (rest.length() > 4 && rest.matches(".*/0\\d")) {
                vi.year = (rest.charAt(rest.length() - 1) - '0');
                rest = rest.substring(0, rest.length() - 3);
            } else {
                String regKpv = root.elementText("reg_kpv");
                vi.year = inferYearFromRegKpv(regKpv);
            }

            if (StringUtils.isBlank(rest)) {
                return null;
            }
            vi.mark = rest.trim();
        }
        return vi;
    }

    private int inferYearFromRegKpv(String regKpv) {
        if (regKpv == null)
            return 10;
        try {
            Date regDate = dateFormat.parse(regKpv);
            int year = PostipoissUtil.getYear(regDate) - 2000;
            if (year < 0 || year > 10) {
                return 10;
            }
            return year;
        } catch (ParseException e) {
            return 10;
        }
    }

    private Node importDoc(File xml, Map<String, Mapping> mappings, Integer documentId) throws DocumentException,
            ParseException {
        SAXReader xmlReader = new SAXReader();

        String fileName = xml.getName();
        String type = fileName.substring(0, fileName.lastIndexOf('_'));
        Element root = xmlReader.read(xml).getRootElement();
        if ("Kiri".equals(type)) {
            Element el = root.element("suund");
            String suund = null;
            if (el != null) {
                suund = el.getStringValue();
            }
            if (!"sissetulev".equals(suund)) {
                suund = "väljaminev";
            }
            type = "Kiri-" + suund;
        }

        Mapping mapping = mappings.get(type);
        Assert.notNull(mapping, type);
        // long time = System.currentTimeMillis();
        VolumeIndex volumeIndex = inferVolumeIndex(root, mapping);
        // long curTime = System.currentTimeMillis();
        // log.info("infer: " + (curTime - time));

        if (volumeIndex == null) {
            log.info("Could not parse toimik_sari nor infer by exception for doc " + documentId);
            return null;
        }

        // time = System.currentTimeMillis();
        Toimik t = getToimik(volumeIndex.year, volumeIndex.mark, false);

        if (t == null) {
            log.info("Could not load toimik by mark for " + volumeIndex);
            t = getToimik(volumeIndex.year, volumeIndex.mark, true);
        }

        if (t == null) {
            log.info("Could not load toimik by normed mark for " + volumeIndex);
            volumeIndex.mark = StringUtils.deleteWhitespace(volumeIndex.mark);
            log.info("Cleaned mark to " + volumeIndex.mark);
            t = getToimik(volumeIndex.year, volumeIndex.mark, false);
        }

        if (t == null) {
            log.info("Could not load toimik by mark for " + volumeIndex);
            t = getToimik(volumeIndex.year, volumeIndex.mark, true);
        }

        if (t == null) {
            log.info("Could not load toimik by normed mark for " + volumeIndex);
            return null;
        }
        String viit = t.normedMark + "/" + volumeIndex.regNumber;
        // curTime = System.currentTimeMillis();
        // log.info("getToimik: " + (curTime - time));

        // time = System.currentTimeMillis();

        Map<QName, Serializable> propsMap = mapProperties(root, mapping);

        propsMap.put(DocumentCommonModel.Props.REG_NUMBER, viit);

        if (!propsMap.containsKey(DocumentCommonModel.Props.OWNER_NAME)) {
            String ownerName = PostipoissUtil.findAnyValue(root, "/document/tegevused/tegevus[tegevus_liik=1]/kellelt_tekst");
            if (StringUtils.isNotBlank(ownerName)) {
                propsMap.put(DocumentCommonModel.Props.OWNER_NAME, ownerName);
            }
        }

        if (!propsMap.containsKey(DocumentCommonModel.Props.DOC_NAME)) {
            String ownerName = root.elementText("dok_liik");
            if (StringUtils.isNotBlank(ownerName)) {
                propsMap.put(DocumentCommonModel.Props.DOC_NAME, ownerName);
            }
        }

        if (!propsMap.containsKey(DocumentCommonModel.Props.STORAGE_TYPE)) {
            Element el = root.element("fail");
            String storageType = "paberil";
            if (el != null) {
                if (StringUtils.isNotBlank(el.getStringValue())) {
                    storageType = "digitaalne";
                }
            }
            propsMap.put(DocumentCommonModel.Props.STORAGE_TYPE, storageType);
        }

        // curTime = System.currentTimeMillis();
        // log.info("beforeCreate: " + (curTime - time));

        // time = System.currentTimeMillis();
        Node document = documentService.createPPImportDocument(mapping.to, t.nodeRef, null);
        // curTime = System.currentTimeMillis();
        // log.info("create: " + (curTime - time));

        // time = System.currentTimeMillis();
        nodeService.addProperties(document.getNodeRef(), propsMap);

        // curTime = System.currentTimeMillis();
        // log.info("addprops: " + (curTime - time));

        // time = System.currentTimeMillis();

        for (Mapping subMapping : mapping.subMappings) {
            fillChild(root, document.getNodeRef(), subMapping);
        }

        // curTime = System.currentTimeMillis();
        // log.info("subs: " + (curTime - time));

        // time = System.currentTimeMillis();
        addHistoryItems(document.getNodeRef(), root);

        // curTime = System.currentTimeMillis();
        // log.info("history: " + (curTime - time));

        // time = System.currentTimeMillis();
        addAssociations(document.getNodeRef(), documentId, root);
        // curTime = System.currentTimeMillis();
        // log.info("assocs: " + (curTime - time));
        return document;
    }

    private void fillChild(Element root, NodeRef nodeRef, Mapping mapping) {
        NodeRef childRef = findChild(nodeRef, mapping.to);
        if (mapping.to.equals(DocumentCommonModel.Types.SEND_INFO)) {
            childRef = sendOutService.addSendinfo(nodeRef, new HashMap<QName, Serializable>());
        }
        if (childRef == null) {
            Assert.notNull(childRef, "No child of type " + mapping.to + " found!");
        }
        Map<QName, Serializable> properties = mapProperties(root, mapping);
        nodeService.addProperties(childRef, properties);
        for (Mapping subMapping : mapping.subMappings) {
            fillChild(root, childRef, subMapping);
        }
    }

    private NodeRef findChild(NodeRef nodeRef, QName to) {
        List<ChildAssociationRef> assocs = nodeService.getChildAssocs(nodeRef,
                Collections.singleton(to));
        if ((assocs == null) || (assocs.isEmpty())) {
            return null;
        }
        return assocs.get(0).getChildRef();
    }

    private NodeRef findCompletedDoc(Integer id) {
        NodeRef res = completedDocumentsMap.get(id);
        if (res == null) {
            ImportedDocument importedDocument = batchCompletedDocumentsMap.get(id);
            if (importedDocument != null) {
                res = importedDocument.nodeRef;
            }
        }
        return res;
    }

    private DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
    private File inputFolder;
    private Map<Integer, ImportedDocument> batchCompletedDocumentsMap;

    protected void addHistoryItems(NodeRef documentRef, Element root) throws ParseException {
        Element tegevused = root.element("tegevused");
        if (tegevused != null) {
            for (Object o : root.element("tegevused").elements()) {
                Element el = (Element) o;
                String kuupaev = el.elementText("kuupaev");
                Date kpv = null;
                try {
                    kpv = dateFormat.parse(kuupaev);
                } catch (ParseException e) {
                }
                String kellelt_tekst = el.elementText("kellelt_tekst");
                String nimetus = el.elementText("nimetus");
                if (nimetus.startsWith("Dokumendi eksportimine")) {
                    continue;
                }
                String resolutsioon = el.elementText("resolutsioon");
                String kes = el.elementText("kes");
                Map<QName, Serializable> props = new HashMap<QName, Serializable>();
                props.put(DocumentCommonModel.Props.CREATED_DATETIME, kpv);
                props.put(DocumentCommonModel.Props.CREATOR_NAME, kellelt_tekst);
                props.put(DocumentCommonModel.Props.EVENT_DESCRIPTION, PostipoissDocumentsMapper.join(",", nimetus, resolutsioon, kes));
                nodeService.createNode(documentRef, DocumentCommonModel.Types.DOCUMENT_LOG, DocumentCommonModel.Types.DOCUMENT_LOG,
                            DocumentCommonModel.Types.DOCUMENT_LOG, props);
            }
        }
    }

    private List<Volume> getVolumes() {
        FunctionsService functionsService = b(FunctionsService.class);
        List<Function> allFunctions = functionsService.getAllFunctions();
        SeriesService seriesService = b(SeriesService.class);
        VolumeService volumeService = b(VolumeService.class);
        List<Volume> volumes = new ArrayList<Volume>();
        for (Function function : allFunctions) {
            if (functionName.equals(function.getTitle())) {
                List<Series> allSeriesByFunction = seriesService.getAllSeriesByFunction(function.getNodeRef());

                for (Series series : allSeriesByFunction) {
                    List<Volume> volumesBySeries = volumeService.getAllVolumesBySeries(series.getNode().getNodeRef());
                    for (Volume vol : volumesBySeries) {
                        if (vol.getVolumeMark() == null) {
                            vol.setVolumeMark("UNKNOWN");
                            volumeService.saveOrUpdate(vol, false);
                        }
                        volumes.add(vol);
                    }
                }
                break;
            }
        }

        return volumes;
    }

    // [ASSOCS

    protected void loadPostponedAssocs() throws Exception {
        postponedAssocsFile = new File(inputFolder, "postponed_assocs.csv");

        postponedAssocs = new TreeMap<Integer, List<PostponedAssoc>>();

        if (!postponedAssocsFile.exists()) {
            log.info("Skipping loading postponed assocs, file does not exist: " + postponedAssocsFile);
            return;
        }

        log.info("Loading postponed assocs from file " + postponedAssocsFile);

        CsvReader reader = new CsvReader(new BufferedInputStream(new FileInputStream(postponedAssocsFile)), CSV_SEPARATOR, Charset.forName("UTF-8"));
        try {
            reader.readHeaders();
            while (reader.readRecord()) {
                Integer documentId = new Integer(reader.get(0));
                NodeRef assocDocumentRef = new NodeRef(reader.get(1));
                Integer isReply = new Integer(reader.get(2));
                putPostponedAssoc(documentId, assocDocumentRef, isReply.equals(1));
            }
        } finally {
            reader.close();
        }
        log.info("Loaded postponed assocs.");
    }

    static class PostponedAssoc {
        Integer doc_id;
        NodeRef assocDocRef;
        Boolean isReply;

        public PostponedAssoc(Integer docId, NodeRef assocDocRef, Boolean isReply) {
            doc_id = docId;
            this.assocDocRef = assocDocRef;
            this.isReply = isReply;
        }
    }

    protected File postponedAssocsFile;

    protected NavigableMap<Integer, List<PostponedAssoc>> postponedAssocs = new TreeMap<Integer, List<PostponedAssoc>>();

    private void writePostponedAssocs() {
        try {
            // Write created documents
            boolean exists = postponedAssocsFile.exists();
            if (exists) {
                postponedAssocsFile.delete();
            }
            CsvWriter writer = new CsvWriter(new FileWriter(postponedAssocsFile, false), CSV_SEPARATOR);
            try {
                writer.writeRecord(new String[] {
                            "documentId",
                            "assocNodeRef",
                            "isReply"
                    });
                for (List<PostponedAssoc> list : postponedAssocs.values()) {
                    for (PostponedAssoc postponedAssoc : list) {
                        writer.writeRecord(new String[] {
                                postponedAssoc.doc_id.toString(),
                                postponedAssoc.assocDocRef.toString(),
                                postponedAssoc.isReply ? "1" : "0"
                        });
                    }
                }
            } finally {
                writer.close();
            }
        } catch (IOException e) {
            throw new RuntimeException("Error writing file '" + postponedAssocsFile + "': " + e.getMessage(), e);
        }
    }

    protected void addAssociations(NodeRef documentRef, Integer documentId, Element root) {
        Element seosed = root.element("seosed");
        if (seosed != null) {
            for (Object object : seosed.elements()) {
                Element element = (Element) object;
                String elementText = element.elementText("dok_nr");
                if (StringUtils.isBlank(elementText)) {
                    continue;
                }
                Integer targetDocumentId = new Integer(elementText);
                NodeRef targetDocumentRef = findCompletedDoc(targetDocumentId);
                if (targetDocumentRef == null) {
                    log.warn("Association to non-existent documentId=" + targetDocumentId + " in " + documentId + ".xml");
                    continue;
                }
                // for doc2doc assoc, it doesn't matter which is source and which is target
                nodeService.createAssociation(documentRef, targetDocumentRef, DocumentCommonModel.Assocs.DOCUMENT_2_DOCUMENT);
            }
        }

        // for reply/followup, initial document is source of assoc
        String elementText = root.elementText("alus_dok_nr");

        Integer initialDocumentId = null;
        if (elementText != null) {
            initialDocumentId = new Integer(root.elementText("alus_dok_nr"));
        }
        if (initialDocumentId != null && !documentId.equals(initialDocumentId)) {
            String origin = root.elementText("paritolu");
            boolean isReply = origin.equals("Vastusdokument");
            if (!isReply && StringUtils.isNotBlank(origin)) {
                log.warn("Unknown value of paritolu '" + origin + "' in " + documentId + ".xml, ignoring association");
                return;
            }
            NodeRef initialDocumentRef = findCompletedDoc(initialDocumentId);
            if (initialDocumentRef == null) {
                putPostponedAssoc(initialDocumentId, documentRef, isReply);
                return;
            }
            addReplyOrFollowUp(initialDocumentRef, documentRef, isReply);

        }
        List<PostponedAssoc> list = postponedAssocs.get(documentId);
        if (list != null) {
            for (PostponedAssoc postponedAssoc : list) {
                addReplyOrFollowUp(documentRef, postponedAssoc.assocDocRef, postponedAssoc.isReply);
            }
            postponedAssocs.remove(documentId);
        }
    }

    private void addReplyOrFollowUp(NodeRef initialDocRef, NodeRef docRef, boolean isReply) {
        QName assoc = isReply ? DocumentCommonModel.Assocs.DOCUMENT_REPLY : DocumentCommonModel.Assocs.DOCUMENT_FOLLOW_UP;
        List<AssociationRef> targetAssocs = nodeService.getSourceAssocs(docRef, assoc);
        if (targetAssocs == null || targetAssocs.isEmpty()) {
            nodeService.createAssociation(docRef, initialDocRef, assoc);
        }
    }

    private void putPostponedAssoc(Integer initialDocId, NodeRef docRef, boolean isReply) {
        List<PostponedAssoc> list = postponedAssocs.get(initialDocId);
        if (list == null) {
            list = new ArrayList<PostponedAssoc>();
            postponedAssocs.put(initialDocId, list);
        }
        list.add(new PostponedAssoc(initialDocId, docRef, isReply));
    }

    // ASSOCS]
    public NodeRef addFile(String fileName, File file, NodeRef parentNodeRef, String mimeType) {
        final FileInfo fileInfo = fileFolderService.create(parentNodeRef, fileName, ContentModel.TYPE_CONTENT);
        final NodeRef fileRef = fileInfo.getNodeRef();
        final ContentWriter writer = fileFolderService.getWriter(fileRef);
        generalService.writeFile(writer, file, fileName, mimeType);
        return fileRef;
    }

    public NodeRef quasiAddFile(String fileName, File file, NodeRef parentNodeRef) throws IOException {
        FileInfo fileInfo = fileFolderService.create(parentNodeRef, "k2.pdf", ContentModel.TYPE_CONTENT);
        ContentWriter writer = fileFolderService.getWriter(fileInfo.getNodeRef());
        String contentUrl = writer.getContentUrl();
        writer.putContent("DUMMY");
        lo("CONTENT URL = " + contentUrl);

        String storePath = "C:/wm/alfdata/contentstore/";
        String path = storePath + contentUrl.substring("store://".length());

        lo("PATH TO FILE = " + path);

        File newFile = new File(path);

        lo("NEWFILE EXISTS = " + newFile.exists() + "\n" + newFile.canWrite() + "\n" + newFile.getAbsolutePath());
        lo("FILE EXISTS = " + file.exists() + "\n" + file.canWrite() + "\n" + file.getAbsolutePath());

        // boolean success = file.renameTo(newFile);

        String cmd = "C:/cygwin/bin/mv.exe -f \"" + file.getAbsolutePath() + "\" \"" + newFile.getAbsolutePath() + "\"";
        lo(cmd);
        Runtime.getRuntime().exec(cmd);
        return fileInfo.getNodeRef();
    }

    private void sendEmail(String title, String content, String to, String cc) {
        String command = String.format("/bin/echo \"%s\" | /bin/mail -s \"%s\" -c %s %s", content, title, cc, to);
        try {
            Runtime.getRuntime().exec(command);
        } catch (IOException e) {
            lo("Could not send email with command " + command);
        }
    }

    // HELPER METHODS
    /** RetryingTransactionHelper that only tries to do things once. */
    private RetryingTransactionHelper getTransactionHelper() {
        RetryingTransactionHelper helper = new RetryingTransactionHelper();
        helper.setMaxRetries(1);
        helper.setTransactionService(transactionService);
        return helper;
    }

    // CONFIGURATION INFORMATION
    /**
     * Returns true when postipoiss import functionality is enabled in an application config --
     * this flag is used e.g. to give access to button that starts the import.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Returns true whenever {@link #runImport()} has been called at least once since
     * last application restart/deployment -- regardless of whether it is still running
     * or (un)successfully finished.
     */
    public boolean isStarted() {
        return started;
    }
}
