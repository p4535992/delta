package ee.webmedia.alfresco.document.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.springframework.util.Assert;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.document.type.model.DocumentType;

public class Document implements Serializable, Comparable<Document> {
    private static final long serialVersionUID = 1L;

    public static final String LIST_SEPARATOR = ", ";

    public static FastDateFormat dateFormat = FastDateFormat.getInstance("dd.MM.yyyy");

    private Node node;
    private DocumentType documentType;
    private List<File> files; // load lazily
    private Map<QName, Serializable> searchableProperties;

    public Document(Document source) {
        Assert.notNull(source, "Source document is mandatory");
        this.node = source.getNode();
        this.documentType = source.getDocumentType();
        this.files = source.getFiles();
        this.searchableProperties = new HashMap<QName, Serializable>(source.getSearchableProperties());
    }

    public Document(Node document, DocumentType documentType) {
        Assert.notNull(document, "Document node is mandatory");
        this.node = document;
        this.documentType = documentType;
        this.searchableProperties = new HashMap<QName, Serializable>();
    }

    private Map<QName, Serializable> getSearchableProperties() {
        return searchableProperties;
    }

    public void setSearchableProperty(QName property, Serializable value) {
        getSearchableProperties().put(property, value);
    }

    public Node getNode() {
        return node;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public String getDocumentTypeName() {
        return documentType != null ? documentType.getName() : null;
    }

    public String getDocTypeLocalName() {
        return documentType.getId().getLocalName();
    }

    // Basic properties that are used in document-list-dialog.jsp

    public String getRegNumber() {
        return (String) getNode().getProperties().get(DocumentCommonModel.Props.REG_NUMBER);
    }

    public Date getRegDateTime() {
        return (Date) getNode().getProperties().get(DocumentCommonModel.Props.REG_DATE_TIME);
    }

    public String getSender() {
        if (documentType.getId().equals(DocumentSubtypeModel.Types.INCOMING_LETTER)) {
            return (String) getNode().getProperties().get(DocumentSpecificModel.Props.SENDER_DETAILS_NAME);
        }
        return (String) getNode().getProperties().get(DocumentCommonModel.Props.OWNER_NAME);
    }

    public String getAllRecipients() {
        return join(DocumentCommonModel.Props.RECIPIENT_NAME, DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_NAME);
    }

    public String getDocName() {
        return (String) getNode().getProperties().get(DocumentCommonModel.Props.DOC_NAME);
    }

    public Date getDueDate() {
        // Only docsub:incomingLetter has this property
        return (Date) getNode().getProperties().get(DocumentSpecificModel.Props.DUE_DATE);
    }

    public Date getComplienceDate() {
        // Only docsub:incomingLetter has this property
        return (Date) getNode().getProperties().get(DocumentSpecificModel.Props.COMPLIENCE_DATE);
    }

    // Additional properties that are used in document-search-extended-results-dialog.jsp

    public String getDocStatus() {
        return (String) getNode().getProperties().get(DocumentCommonModel.Props.DOC_STATUS);
    }

    public Date getSenderRegDate() {
        // Only docsub:incomingLetter and docsub:outgoingLetter have this property
        return (Date) getNode().getProperties().get(DocumentSpecificModel.Props.SENDER_REG_DATE);
    }

    public String getSenderRegNumber() {
        // Only docsub:incomingLetter and docsub:outgoingLetter have this property
        return (String) getNode().getProperties().get(DocumentSpecificModel.Props.SENDER_REG_NUMBER);
    }

    public Date getDueDate2() {
        if (getDocumentType().getId().equals(DocumentSubtypeModel.Types.INCOMING_LETTER)) {
            return (Date) getNode().getProperties().get(DocumentSpecificModel.Props.DUE_DATE);
        } else if (getDocumentType().getId().equals(DocumentSubtypeModel.Types.MANAGEMENTS_ORDER)) {
            return (Date) getNode().getProperties().get(DocumentSpecificModel.Props.MANAGEMENTS_ORDER_DUE_DATE);
        } else if (getDocumentType().getId().equals(DocumentSubtypeModel.Types.CONTRACT_SIM)) {
            return (Date) getNode().getProperties().get(DocumentSpecificModel.Props.CONTRACT_SIM_END_DATE);
        } else if (getDocumentType().getId().equals(DocumentSubtypeModel.Types.CONTRACT_SMIT)) {
            return (Date) getNode().getProperties().get(DocumentSpecificModel.Props.CONTRACT_SMIT_END_DATE);
        }
        return null;
    }

    public String getAccessRestriction() {
        return (String) getNode().getProperties().get(DocumentCommonModel.Props.ACCESS_RESTRICTION);
    }

    public String getAccessRestrictionReason() {
        return (String) getNode().getProperties().get(DocumentCommonModel.Props.ACCESS_RESTRICTION_REASON);
    }

    public Date getAccessRestrictionBeginDate() {
        return (Date) getNode().getProperties().get(DocumentCommonModel.Props.ACCESS_RESTRICTION_BEGIN_DATE);
    }

    public Date getAccessRestrictionEndDate() {
        return (Date) getNode().getProperties().get(DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DATE);
    }

    public String getAccessRestrictionEndDesc() {
        return (String) getNode().getProperties().get(DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DESC);
    }
    
    public String getOwnerId() {
        return (String) getNode().getProperties().get(DocumentCommonModel.Props.OWNER_ID);
    }

    public String getOwnerName() {
        return (String) getNode().getProperties().get(DocumentCommonModel.Props.OWNER_NAME);
    }

    public String getOwnerOrgStructUnit() {
        return (String) getNode().getProperties().get(DocumentCommonModel.Props.OWNER_ORG_STRUCT_UNIT);
    }

    public String getOwnerJobTitle() {
        return (String) getNode().getProperties().get(DocumentCommonModel.Props.OWNER_JOB_TITLE);
    }

    public String getSignerName() {
        return (String) getNode().getProperties().get(DocumentCommonModel.Props.SIGNER_NAME);
    }

    public String getSignerJobTitle() {
        return (String) getNode().getProperties().get(DocumentCommonModel.Props.SIGNER_JOB_TITLE);
    }

    public String getKeywords() {
        return (String) getNode().getProperties().get(DocumentCommonModel.Props.KEYWORDS);
    }

    public String getStorageType() {
        return (String) getNode().getProperties().get(DocumentCommonModel.Props.STORAGE_TYPE);
    }

    public String getSendMode() {
        return (String) getSearchableProperties().get(DocumentCommonModel.Props.SEARCHABLE_SEND_MODE);
    }

    public String getCostManager() {
        return (String) getSearchableProperties().get(DocumentCommonModel.Props.SEARCHABLE_COST_MANAGER);
    }

    public String getApplicantName() {
        return (String) getSearchableProperties().get(DocumentCommonModel.Props.SEARCHABLE_APPLICANT_NAME);
    }

    public String getErrandBeginDate() {
        return (String) getSearchableProperties().get(DocumentCommonModel.Props.SEARCHABLE_ERRAND_BEGIN_DATE);
    }

    public String getErrandEndDate() {
        return (String) getSearchableProperties().get(DocumentCommonModel.Props.SEARCHABLE_ERRAND_END_DATE);
    }

    public String getErrandCountry() {
        return (String) getSearchableProperties().get(DocumentCommonModel.Props.SEARCHABLE_ERRAND_COUNTRY);
    }

    public String getErrandCounty() {
        return (String) getSearchableProperties().get(DocumentCommonModel.Props.SEARCHABLE_ERRAND_COUNTY);
    }

    public String getErrandCity() {
        return (String) getSearchableProperties().get(DocumentCommonModel.Props.SEARCHABLE_ERRAND_CITY);
    }

    public String getResponsibleName() {
        // Only docsub:managementsOrder has this property
        return (String) getNode().getProperties().get(DocumentSpecificModel.Props.RESPONSIBLE_NAME);
    }

    public String getCoResponsibles() {
        // Only docsub:managementsOrder has this property
        return (String) getNode().getProperties().get(DocumentSpecificModel.Props.CO_RESPONSIBLES);
    }

    public String getContactPerson() {
        // Only docsub:contractSim and docsub:contractSmit have these properties
        return join(DocumentSpecificModel.Props.FIRST_PARTY_CONTACT_PERSON,
                DocumentSpecificModel.Props.SECOND_PARTY_CONTACT_PERSON,
                DocumentSpecificModel.Props.THIRD_PARTY_CONTACT_PERSON);
    }

    public String getProcurementType() {
        // Only docsub:tenderingApplication has this property
        return (String) getNode().getProperties().get(DocumentSpecificModel.Props.PROCUREMENT_TYPE);
    }

    // Other

    public List<File> getFiles() {
        if (files == null) {
            // probably not the best idea to call service from model, but alternatives get probably too complex
            FileService fileService = (FileService) FacesContextUtils.getRequiredWebApplicationContext( //
                    FacesContext.getCurrentInstance()).getBean(FileService.BEAN_NAME);
            files = fileService.getAllActiveFiles(getNode().getNodeRef());
        }
        return files;
    }

    @Override
    public int compareTo(Document other) {
        if (StringUtils.equals(getRegNumber(), other.getRegNumber())) {
            if (getRegDateTime() != null) {
                if (other.getRegDateTime() == null) {
                    return 1;
                }
                return getRegDateTime().compareTo(other.getRegDateTime());
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
                .append("\n\tregNumber = " + getRegNumber())
                .append("\n\tdocName = " + getDocName())
                .toString();
    }

    private String join(QName... props) {
        StringBuilder result = new StringBuilder();
        for (QName prop : props) {
            Object item = getNode().getProperties().get(prop);
            if (item instanceof Collection<?>) {
                @SuppressWarnings("unchecked")
                Collection<String> list = (Collection<String>) item;
                for (String textItem : list) {
                    if (StringUtils.isNotBlank(textItem)) {
                        if (result.length() > 0) {
                            result.append(LIST_SEPARATOR);
                        }
                        result.append(textItem);
                    }
                }
            } else {
                String textItem = (String) item;
                if (StringUtils.isNotBlank(textItem)) {
                    if (result.length() > 0) {
                        result.append(LIST_SEPARATOR);
                    }
                    result.append(textItem);
                }
            }
        }
        return result.toString();
    }

    public static String join(Serializable propValue) {
        StringBuilder result = new StringBuilder();
        if (propValue == null) {
            return "";
        }
        if (propValue instanceof List<?>) {
            @SuppressWarnings("unchecked")
            List<Serializable> list = (List<Serializable>) propValue;
            for (Serializable value : list) {
                String textItem = join(value);
                if (StringUtils.isNotBlank(textItem)) {
                    if (result.length() > 0) {
                        result.append(LIST_SEPARATOR);
                    }
                    result.append(textItem);
                }
            }
        } else if (propValue instanceof Date) {
            String textItem = dateFormat.format((Date) propValue);
            if (StringUtils.isNotBlank(textItem)) {
                if (result.length() > 0) {
                    result.append(LIST_SEPARATOR);
                }
                result.append(textItem);
            }
        } else if (propValue instanceof String) {
            String textItem = (String) propValue;
            if (StringUtils.isNotBlank(textItem)) {
                if (result.length() > 0) {
                    result.append(LIST_SEPARATOR);
                }
                result.append(textItem);
            }
        }
        return result.toString();
    }

}
