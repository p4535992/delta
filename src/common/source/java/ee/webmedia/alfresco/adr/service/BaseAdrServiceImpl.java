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
import ee.webmedia.alfresco.adr.util.ContentReaderDataSource;
import ee.webmedia.alfresco.common.service.GeneralService;

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
        protected String regNumber;
        protected Date regDateTime;
        public AdrDocument(String regNumber, Date regDateTime) {
            Assert.notNull(regNumber);
            this.regNumber = regNumber;
            Assert.notNull(regDateTime);
            this.regDateTime = regDateTime;
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
            return regNumber.equals(otherDoc.regNumber) && regDateTime.equals(otherDoc.regDateTime);
        }
        @Override
        public int hashCode() {
            return regNumber.hashCode() + regDateTime.hashCode();
        }
        @Override
        public String toString() {
            return regNumber + ", " + regDateTime;
        }
    }

    protected void addDeletedDocument(String regNumber, Date regDateTime) {
        if (StringUtils.isEmpty(regNumber) || regDateTime == null) {
            return;
        }
        NodeRef root = generalService.getNodeRef(AdrModel.Repo.ADR_DELETED_DOCUMENTS);
        Map<QName, Serializable> props = new HashMap<QName, Serializable>();
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

    protected static Date getDate(XMLGregorianCalendar aeg) {
        if (aeg == null) {
            return null;
        }
        return aeg.toGregorianCalendar().getTime();
    }

    protected static String getNullIfEmpty(String input) {
        if (StringUtils.isEmpty(input)) {
            return null;
        }
        return input;
    }

    protected static String getWithParenthesis(String first, String second) {
        StringBuilder s = new StringBuilder();
        if (StringUtils.isNotEmpty(first)) {
            s.append(first);
        }
        if (StringUtils.isNotEmpty(second)) {
            if (s.length() > 0) {
                s.append(" ");
            }
            s.append("(").append(second).append(")");
        }
        return s.toString();
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
