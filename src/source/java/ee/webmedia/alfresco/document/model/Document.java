package ee.webmedia.alfresco.document.model;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.document.type.model.DocumentType;
import ee.webmedia.alfresco.utils.beanmapper.AlfrescoModelProperty;
import ee.webmedia.alfresco.utils.beanmapper.AlfrescoModelType;

@AlfrescoModelType(uri = DocumentCommonModel.URI)
public class Document implements Serializable, Comparable<Document> {

    private static final long serialVersionUID = 1L;
    private static final String LIST_SEPARATOR = ", ";

    private String regNumber;
    private String title;
    private String docName;
    private Date regDateTime;
    // START: not mappable fields
    @AlfrescoModelProperty(isMappable = false)
    private DocumentType documentType;

    @AlfrescoModelProperty(isMappable = false)
    private NodeRef volumeNodeRef;

    @AlfrescoModelProperty(isMappable = false)
    private Node node;

    public NodeRef getVolumeNodeRef() {
        return volumeNodeRef;
    }

    public void setVolumeNodeRef(NodeRef volumeNodeRef) {
        this.volumeNodeRef = volumeNodeRef;
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public String getDocType() {
        return documentType != null ? documentType.getName() : null;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public String getSender() {
        if (documentType.getId().equals(DocumentSubtypeModel.Types.INCOMING_LETTER)) {
            return (String) getNode().getProperties().get(DocumentSpecificModel.Props.SENDER_DETAILS_NAME.toString());
        }
        return (String) getNode().getProperties().get(DocumentCommonModel.Props.OWNER_NAME.toString());
    }

    public String getAllRecipients() {
        @SuppressWarnings("unchecked")
        final List<String> recipients = (List<String>) getNode().getProperties().get(DocumentCommonModel.Props.RECIPIENT_NAME.toString());
        if (recipients == null) {
            return "";
        }
        String allRecipients = StringUtils.join(recipients.iterator(), LIST_SEPARATOR);
        @SuppressWarnings("unchecked")
        final List<String> additionalRecipient = (List<String>) getNode().getProperties().get(DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_NAME.toString());
        if (additionalRecipient == null) {
            return allRecipients;
        }
        return allRecipients + (StringUtils.isNotBlank(allRecipients) ? LIST_SEPARATOR : "") + StringUtils.join(recipients.iterator(), LIST_SEPARATOR);
    }

    public Date getDueDate() {
        if (documentType.getId().equals(DocumentSubtypeModel.Types.INCOMING_LETTER)) {
            return (Date) getNode().getProperties().get(DocumentSpecificModel.Props.DUE_DATE.toString());
        }
        return null;
    }
    
    public Date getComplienceDate() {
        if (documentType.getId().equals(DocumentSubtypeModel.Types.INCOMING_LETTER)) {
            return (Date) getNode().getProperties().get(DocumentSpecificModel.Props.COMPLIENCE_DATE.toString());
        }
        return null;
    }

    public String getDocTypeLocalName() {
        return documentType.getId().getLocalName();
    }

    public List<File> getFiles() {
        // probably not the best idea to call service from model, but alternatives get probably too complex
        FileService fileService = (FileService) FacesContextUtils.getRequiredWebApplicationContext( //
                FacesContext.getCurrentInstance()).getBean(FileService.BEAN_NAME);
        return fileService.getAllFilesExcludingDigidocSubitems(getNode().getNodeRef());
    }

    // END: not mappable fields

    public String getRegNumber() {
        return regNumber;
    }

    public void setRegNumber(String regNumber) {
        this.regNumber = regNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDocName() {
        return docName;
    }

    public void setDocName(String docName) {
        this.docName = docName;
    }

    public Date getRegDateTime() {
        return regDateTime;
    }

    public void setRegDateTime(Date regDateTime) {
        this.regDateTime = regDateTime;
    }

    @Override
    public int compareTo(Document other) {
        if (StringUtils.equals(getRegNumber(), other.getRegNumber())) {
            if (regDateTime != null) {
                if (other.getRegDateTime() == null) {
                    return 1;
                }
                return regDateTime.compareTo(other.getRegDateTime());
            }
            return 0;
        }
        if (getRegNumber() == null) {
            return -1;
        } else if (other.getRegNumber() == null) {
            return 1;
        }
        return getRegNumber().compareTo(other.getRegNumber());
    }

    @Override
    public String toString() {
        return new StringBuilder("Document:")//
                .append("\n\tregNumber = " + regNumber)
                .append("\n\ttitle = " + title)
                .toString();
    }

}
