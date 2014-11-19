package ee.webmedia.alfresco.adr.service;

import java.io.Serializable;
<<<<<<< HEAD
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
=======
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
import java.util.Map;

import javax.activation.DataHandler;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.alfresco.service.cmr.model.FileFolderService;
<<<<<<< HEAD
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentStreamListener;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.Pair;
import org.apache.commons.collections.list.SynchronizedList;
=======
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.adr.model.AdrModel;
import ee.webmedia.alfresco.common.service.GeneralService;
<<<<<<< HEAD
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.utils.ContentReaderDataSource;

/**
 * @author Alar Kvell
 */
=======
import ee.webmedia.alfresco.utils.ContentReaderDataSource;

>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
public abstract class BaseAdrServiceImpl implements AdrService {

    protected FileFolderService fileFolderService;
    protected GeneralService generalService;
    protected NodeService nodeService;
<<<<<<< HEAD
    protected FileService fileService;
    protected TransactionService transactionService;

    @SuppressWarnings("unchecked")
    private final List<NodeRef> tempFiles = SynchronizedList.decorate(new ArrayList<NodeRef>());
    private static DatatypeFactory datatypeFactory; // JAXP RI implements DatatypeFactory in a thread-safe way
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(BaseAdrServiceImpl.class);
=======

    private static DatatypeFactory datatypeFactory; // JAXP RI implements DatatypeFactory in a thread-safe way
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5

    public BaseAdrServiceImpl() {
        try {
            datatypeFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    protected static class AdrDocument {
        protected NodeRef nodeRef;
        protected String regNumber;
        protected Date regDateTime;
        protected boolean compareByNodeRef;

        public AdrDocument(NodeRef nodeRef, String regNumber, Date regDateTime, boolean compareByNodeRef) {
            if (compareByNodeRef) {
                Assert.notNull(nodeRef);
            }
            this.nodeRef = nodeRef;
            Assert.notNull(regNumber);
            this.regNumber = regNumber;
            Assert.notNull(regDateTime);
            this.regDateTime = regDateTime;
            this.compareByNodeRef = compareByNodeRef;
        }

        @Override
        public boolean equals(Object other) {
            if (other == this) {
                return true;
            }
            if (!(other instanceof AdrDocument)) {
                return false;
            }
            AdrDocument otherDoc = (AdrDocument) other;
            if (compareByNodeRef) {
                return nodeRef.equals(otherDoc.nodeRef);
            }
            return regNumber.equals(otherDoc.regNumber) && regDateTime.equals(otherDoc.regDateTime);
        }

        @Override
        public int hashCode() {
            if (compareByNodeRef) {
                return nodeRef.hashCode();
            }
            return regNumber.hashCode() + regDateTime.hashCode();
        }

        @Override
        public String toString() {
            if (compareByNodeRef) {
                return nodeRef.toString();
            }
            return regNumber + ", " + regDateTime;
        }
    }

    protected NodeRef addDeletedDocument(NodeRef nodeRef, String regNumber, Date regDateTime) {
        if (nodeRef == null || StringUtils.isEmpty(regNumber) || regDateTime == null) {
            return null;
        }
        NodeRef root = generalService.getNodeRef(AdrModel.Repo.ADR_DELETED_DOCUMENTS);
        Map<QName, Serializable> props = new HashMap<QName, Serializable>();
        props.put(AdrModel.Props.NODEREF, nodeRef.toString());
        props.put(AdrModel.Props.REG_NUMBER, regNumber);
        props.put(AdrModel.Props.REG_DATE_TIME, regDateTime);
        props.put(AdrModel.Props.DELETED_DATE_TIME, new Date());
        return nodeService.createNode(root, AdrModel.Types.ADR_DELETED_DOCUMENT, AdrModel.Types.ADR_DELETED_DOCUMENT, AdrModel.Types.ADR_DELETED_DOCUMENT, props).getChildRef();
    }

    @Override
    public void deleteDocumentType(QName documentType) {
        NodeRef root = generalService.getNodeRef(AdrModel.Repo.ADR_DELETED_DOCUMENT_TYPES);
        Map<QName, Serializable> props = new HashMap<QName, Serializable>();
        props.put(AdrModel.Props.DOCUMENT_TYPE, documentType);
        props.put(AdrModel.Props.DELETED_DATE_TIME, new Date());
        nodeService.createNode(root, AdrModel.Types.ADR_DELETED_DOCUMENT_TYPE, AdrModel.Types.ADR_DELETED_DOCUMENT_TYPE, AdrModel.Types.ADR_DELETED_DOCUMENT_TYPE, props);
    }

    @Override
    public void addDocumentType(QName documentType) {
        NodeRef root = generalService.getNodeRef(AdrModel.Repo.ADR_ADDED_DOCUMENT_TYPES);
        Map<QName, Serializable> props = new HashMap<QName, Serializable>();
        props.put(AdrModel.Props.DOCUMENT_TYPE, documentType);
        props.put(AdrModel.Props.DELETED_DATE_TIME, new Date());
        nodeService.createNode(root, AdrModel.Types.ADR_ADDED_DOCUMENT_TYPE, AdrModel.Types.ADR_ADDED_DOCUMENT_TYPE, AdrModel.Types.ADR_ADDED_DOCUMENT_TYPE, props);
    }

<<<<<<< HEAD
    protected Pair<Boolean, DataHandler> getFileDataHandler(NodeRef nodeRef, String filename) {
        boolean transformedToPdf = false;
        ContentReader fileReader = fileFolderService.getReader(nodeRef);
        if (fileReader == null) {
            return Pair.newInstance(transformedToPdf, null);
        }

        NodeRef previouslyGeneratedPdf = fileService.getPreviouslyGeneratedPdf(nodeRef);
        if (fileService.isPdfUpToDate(nodeRef, previouslyGeneratedPdf)) {
            fileReader = fileFolderService.getReader(previouslyGeneratedPdf);
        } else {

            // If it is possible to transform to PDF, then we must create a temporary file
            if (fileService.isTransformableToPdf(fileReader.getMimetype())) {
                // Transform the file to PDF and replace current file reader
                FileInfo pdfFile = fileService.transformToPdf(generalService.getNodeRef(DocumentCommonModel.Repo.TEMP_FILES_SPACE), nodeRef, fileReader, filename, filename, null);
                if (pdfFile != null) {
                    final NodeRef pdfRef = pdfFile.getNodeRef();
                    fileReader = fileFolderService.getReader(pdfRef);
                    fileReader.addListener(new ContentStreamListener() {

                        @Override
                        public void contentStreamClosed() throws ContentIOException {
                            // We cannot delete directly here, add to a queue
                            tempFiles.add(pdfRef);
                        }
                    });
                    fileReader.setRetryingTransactionHelper(transactionService.getRetryingTransactionHelper());
                    transformedToPdf = true;
                } else {
                    // Get same reader again, because last instance has been used for reading; and one reader cannot be opened multiple times
                    fileReader = fileFolderService.getReader(nodeRef);
                }
            }
        }
        ContentReaderDataSource dataSource = new ContentReaderDataSource(fileReader, filename);
        return Pair.newInstance(transformedToPdf, new DataHandler(dataSource));
=======
    protected DataHandler getFileDataHandler(NodeRef nodeRef, String filename) {
        ContentReader fileReader = fileFolderService.getReader(nodeRef);
        if (fileReader == null) {
            return null;
        }
        ContentReaderDataSource dataSource = new ContentReaderDataSource(fileReader, filename);
        return new DataHandler(dataSource);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    }

    protected static XMLGregorianCalendar convertToXMLGergorianCalendar(Date date) {
        if (date == null) {
            return null;
        }
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(date);
        return datatypeFactory.newXMLGregorianCalendar(cal);
    }

    protected static String getNullIfEmpty(String input) {
        if (StringUtils.isEmpty(input)) {
            return null;
        }
        return input;
    }

<<<<<<< HEAD
    protected void cleanTempFiles() {
        List<NodeRef> files = new ArrayList<NodeRef>(tempFiles);
        tempFiles.clear();

        for (NodeRef nodeRef : files) {
            if (!nodeService.exists(nodeRef)) {
                continue;
            }
            nodeService.deleteNode(nodeRef);
        }
    }

=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    // START: getters / setters

    public void setFileFolderService(FileFolderService fileFolderService) {
        this.fileFolderService = fileFolderService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

<<<<<<< HEAD
    public void setFileService(FileService fileService) {
        this.fileService = fileService;
    }

    public void setTransactionService(TransactionService transactionService) {
        this.transactionService = transactionService;
    }
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
}
