package ee.webmedia.alfresco.document.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.springframework.util.Assert;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.app.AppConstants;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.CssStylable;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel.Props;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.document.type.model.DocumentType;
import ee.webmedia.alfresco.document.type.service.DocumentTypeHelper;
import ee.webmedia.alfresco.document.type.service.DocumentTypeService;

public class Document extends Node implements Comparable<Document>, CssStylable, CreatedAndRegistered {
    private static final long serialVersionUID = 1L;

    public static final String LIST_SEPARATOR = ", ";
    public static final int SHORT_PROP_LENGTH = 20;
    public static FastDateFormat dateFormat = FastDateFormat.getInstance("dd.MM.yyyy");

    private List<File> files; // load lazily
    private Map<QName, Serializable> searchableProperties;
    private boolean initialized;

    /** To be only accessed using {@link #getDocumentType()} */
    private DocumentType _documentType;
    private transient DocumentTypeService documentTypeService;

    /**
     * Copy constructory
     * 
     * @param source
     */
    public Document(Document source) {
        super(source.nodeRef);
        nodeRef = source.nodeRef;
        Assert.notNull(source, "Source document is mandatory");
        files = source.getFiles();
        searchableProperties = new HashMap<QName, Serializable>(source.getSearchableProperties());
        initialized = source.initialized;
        _documentType = source.getDocumentType();
    }

    public Document(NodeRef nodeRef) {
        super(nodeRef);
        this.nodeRef = nodeRef;
    }

    protected void lazyInit() {
        if (!initialized) {
            if (searchableProperties == null) {
                searchableProperties = new HashMap<QName, Serializable>();
            }
            initialized = true;
        }
    }

    private Map<QName, Serializable> getSearchableProperties() {
        lazyInit();
        return searchableProperties;
    }

    public void setSearchableProperty(QName property, Serializable value) {
        getSearchableProperties().put(property, value);
    }

    public Node getNode() {
        lazyInit();
        return this;
    }

    public DocumentType getDocumentType() {
        lazyInit();
        if (_documentType == null) {
            _documentType = getDocumentTypeService().getDocumentType(getType());
        }
        return _documentType;
    }

    public String getDocumentTypeName() {
        String documentTypeId = objectTypeId();
        return BeanHelper.getDocumentAdminService().getDocumentTypeName(documentTypeId);
    }

    private String objectTypeId() {
        return (String) getProperties().get(Props.OBJECT_TYPE_ID);
    }

    @Override
    public String getCssStyleClass() {
        return objectTypeId();
    }

    // Basic properties that are used in document-list-dialog.jsp

    public String getRegNumber() {
        return (String) getNode().getProperties().get(DocumentCommonModel.Props.REG_NUMBER);
    }

    @Override
    public Date getRegDateTime() {
        return (Date) getNode().getProperties().get(DocumentCommonModel.Props.REG_DATE_TIME);
    }

    public String getRegDateTimeStr() {
        return getRegDateTime() != null ? dateFormat.format(getRegDateTime()) : "";
    }

    public String getSender() {
        QName docType = getType();
        if (DocumentTypeHelper.isIncomingLetter(docType)) {
            return (String) getProperties().get(DocumentSpecificModel.Props.SENDER_DETAILS_NAME);
        } else if (DocumentSubtypeModel.Types.INVOICE.equals(docType)) {
            return (String) getProperties().get(DocumentSpecificModel.Props.SELLER_PARTY_NAME);
        }
        return (String) getProperties().get(DocumentCommonModel.Props.OWNER_NAME);
    }

    public String getRecipients() {
        return join(DocumentCommonModel.Props.RECIPIENT_NAME, DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_NAME);
    }

    public String getAllRecipients() {
        return join(DocumentCommonModel.Props.RECIPIENT_NAME, DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_NAME,
                DocumentSpecificModel.Props.SECOND_PARTY_NAME, DocumentSpecificModel.Props.THIRD_PARTY_NAME, DocumentCommonModel.Props.SEARCHABLE_PARTY_NAME);
    }

    public String getDocName() {
        return (String) getNode().getProperties().get(DocumentCommonModel.Props.DOC_NAME);
    }

    public Date getDueDate() {
        // Only docsub:incomingLetter has this property
        return (Date) getNode().getProperties().get(DocumentSpecificModel.Props.DUE_DATE);
    }

    public String getDueDateStr() {
        final Date dueDate = getDueDate();
        return dueDate != null ? dateFormat.format(dueDate) : "";
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
        lazyInit();
        if (DocumentTypeHelper.isIncomingLetter(getType())) {
            return (Date) getProperties().get(DocumentSpecificModel.Props.DUE_DATE);
        } else if (getType().equals(DocumentSubtypeModel.Types.MANAGEMENTS_ORDER)) {
            return (Date) getProperties().get(DocumentSpecificModel.Props.MANAGEMENTS_ORDER_DUE_DATE);
        } else if (getType().equals(DocumentSubtypeModel.Types.CONTRACT_SIM)) {
            return (Date) getProperties().get(DocumentSpecificModel.Props.CONTRACT_SIM_END_DATE);
        } else if (getType().equals(DocumentSubtypeModel.Types.CONTRACT_SMIT)) {
            return (Date) getProperties().get(DocumentSpecificModel.Props.CONTRACT_SMIT_END_DATE);
        } else if (getType().equals(DocumentSubtypeModel.Types.CONTRACT_MV)) {
            return (Date) getProperties().get(DocumentSpecificModel.Props.CONTRACT_MV_END_DATE);
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

    public String getSellerPartyRegNumber() {
        return (String) getNode().getProperties().get(DocumentSpecificModel.Props.SELLER_PARTY_REG_NUMBER);
    }

    public String getInvoiceNumber() {
        return (String) getNode().getProperties().get(DocumentSpecificModel.Props.INVOICE_NUMBER);
    }

    public Date getInvoiceDate() {
        return (Date) getNode().getProperties().get(DocumentSpecificModel.Props.INVOICE_DATE);
    }

    public String getInvoiceDateStr() {
        return getInvoiceDate() != null ? dateFormat.format(getInvoiceDate()) : "";
    }

    @Override
    public Date getCreated() {
        return (Date) getNode().getProperties().get(ContentModel.PROP_CREATED);
    }

    // Other

    public List<File> getFiles() {
        if (files == null) {
            // probably not the best idea to call service from model, but alternatives get probably too complex
            FileService fileService = (FileService) FacesContextUtils.getRequiredWebApplicationContext( //
                    FacesContext.getCurrentInstance()).getBean(FileService.BEAN_NAME);
            files = fileService.getAllActiveFiles(getNodeRef());
        }
        return files;
    }

    @Override
    public int compareTo(Document other) {
        lazyInit();
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
        return AppConstants.DEFAULT_COLLATOR.compare(getRegNumber(), other.getRegNumber());
    }

    // XXX: performance hit... if need to init other Document as well that is otherwise uninitialized
    // @Override
    // public boolean equals(Object obj) {
    // if (obj instanceof Document) {
    // return this.compareTo((Document) obj) == 0;
    // }
    // return false;
    // }

    @Override
    public String toString() {
        return new StringBuilder("Document:")//
                .append("\n\tregNumber = " + getRegNumber())
                .append("\n\tdocName = " + getDocName())
                .toString();
    }

    private String join(QName... props) {
        lazyInit();
        StringBuilder result = new StringBuilder();
        for (QName prop : props) {
            Object item = getProperties().get(prop);
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

    protected DocumentTypeService getDocumentTypeService() {
        if (documentTypeService == null) {
            documentTypeService = (DocumentTypeService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance()).getBean(
                    DocumentTypeService.BEAN_NAME);
        }
        return documentTypeService;
    }

}
