package ee.webmedia.alfresco.adr.service;

import java.io.Serializable;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import javax.activation.DataHandler;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.adr.model.AdrModel;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.utils.ContentReaderDataSource;

/**
 * @author Alar Kvell
 */
public abstract class BaseAdrServiceImpl implements AdrService {

    protected FileFolderService fileFolderService;
    protected GeneralService generalService;
    protected NodeService nodeService;

    private static DatatypeFactory datatypeFactory; // JAXP RI implements DatatypeFactory in a thread-safe way

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

    protected void addDeletedDocument(NodeRef nodeRef, String regNumber, Date regDateTime) {
        if (nodeRef == null || StringUtils.isEmpty(regNumber) || regDateTime == null) {
            return;
        }
        NodeRef root = generalService.getNodeRef(AdrModel.Repo.ADR_DELETED_DOCUMENTS);
        Map<QName, Serializable> props = new HashMap<QName, Serializable>();
        props.put(AdrModel.Props.NODEREF, nodeRef);
        props.put(AdrModel.Props.REG_NUMBER, regNumber);
        props.put(AdrModel.Props.REG_DATE_TIME, regDateTime);
        props.put(AdrModel.Props.DELETED_DATE_TIME, new Date());
        nodeService.createNode(root, AdrModel.Types.ADR_DELETED_DOCUMENT, AdrModel.Types.ADR_DELETED_DOCUMENT, AdrModel.Types.ADR_DELETED_DOCUMENT, props);
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

    protected DataHandler getFileDataHandler(NodeRef nodeRef, String filename) {
        ContentReader fileReader = fileFolderService.getReader(nodeRef);
        if (fileReader == null) {
            return null;
        }
        ContentReaderDataSource dataSource = new ContentReaderDataSource(fileReader, filename);
        return new DataHandler(dataSource);
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

}
