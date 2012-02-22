package ee.webmedia.alfresco.document.model;

import static ee.webmedia.alfresco.utils.TextUtil.LIST_SEPARATOR;

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
import ee.webmedia.alfresco.cases.model.Case;
import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.CssStylable;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel.Props;
import ee.webmedia.alfresco.docconfig.bootstrap.SystematicDocumentType;
import ee.webmedia.alfresco.docconfig.generator.systematic.DocumentLocationGenerator;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.document.type.service.DocumentTypeService;
import ee.webmedia.alfresco.functions.model.Function;
import ee.webmedia.alfresco.series.model.Series;
import ee.webmedia.alfresco.utils.TextUtil;
import ee.webmedia.alfresco.utils.UserUtil;
import ee.webmedia.alfresco.volume.model.Volume;

public class Document extends Node implements Comparable<Document>, CssStylable, CreatedAndRegistered {
    private static final long serialVersionUID = 1L;

    public static final int SHORT_PROP_LENGTH = 20;
    public static FastDateFormat dateFormat = FastDateFormat.getInstance("dd.MM.yyyy");

    private List<File> files; // load lazily
    private String workflowStatus;
    private Map<QName, Serializable> searchableProperties;
    private boolean initialized;

    /** To be only accessed using {@link #getDocumentType()} */
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

    public String getDocumentTypeName() {
        String documentTypeId = objectTypeId();
        return BeanHelper.getDocumentAdminService().getDocumentTypeName(documentTypeId);
    }

    public String objectTypeId() {
        return (String) getProperties().get(Props.OBJECT_TYPE_ID);
    }

    @Override
    public String getCssStyleClass() {
        String cssStyleClass = objectTypeId();
        if (SystematicDocumentType.INCOMING_LETTER.isSameType(cssStyleClass)
                || SystematicDocumentType.OUTGOING_LETTER.isSameType(cssStyleClass)) {
            return cssStyleClass;
        }
        return "genericDocument";
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
        String docDynType = objectTypeId();
        if (SystematicDocumentType.INCOMING_LETTER.isSameType(docDynType)) {
            return (String) getProperties().get(DocumentSpecificModel.Props.SENDER_DETAILS_NAME);
        } else if (SystematicDocumentType.INVOICE.isSameType(docDynType)) {
            return (String) getProperties().get(DocumentSpecificModel.Props.SELLER_PARTY_NAME);
        }
        return (String) getProperties().get(DocumentCommonModel.Props.OWNER_NAME);
    }

    public String getRecipients() {
        lazyInit();
        return TextUtil.join(getProperties(), DocumentCommonModel.Props.RECIPIENT_NAME, DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_NAME);
    }

    public String getAllRecipients() {
        lazyInit();
        return TextUtil.join(getProperties(), DocumentCommonModel.Props.RECIPIENT_NAME, DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_NAME,
                DocumentSpecificModel.Props.SECOND_PARTY_NAME, DocumentSpecificModel.Props.THIRD_PARTY_NAME, DocumentSpecificModel.Props.PARTY_NAME);
    }

    public String getSenderOrRecipients() {
        String docDynType = objectTypeId();
        if (SystematicDocumentType.INCOMING_LETTER.isSameType(docDynType)) {
            return (String) getProperties().get(DocumentSpecificModel.Props.SENDER_DETAILS_NAME);
        } else if (SystematicDocumentType.INVOICE.isSameType(docDynType)) {
            return (String) getProperties().get(DocumentSpecificModel.Props.SELLER_PARTY_NAME);
        }
        return getAllRecipients();
    }

    public String getWorkflowStatus() {
        return workflowStatus;
    }

    public void setWorkflowStatus(String workflowStatus) {
        this.workflowStatus = workflowStatus;
    }

    public String getDocName() {
        return (String) getNode().getProperties().get(DocumentCommonModel.Props.DOC_NAME);
    }

    public String getVolumeLabel() {
        NodeRef nodeRef = (NodeRef) getNode().getProperties().get(DocumentCommonModel.Props.VOLUME);
        if (nodeRef != null) {
            Volume volume = BeanHelper.getVolumeService().getVolumeByNodeRef(nodeRef);
            if (volume != null) {
                return DocumentLocationGenerator.getVolumeLabel(volume);
            }
        }
        return null;
    }

    public String getFunctionLabel() {
        NodeRef nodeRef = (NodeRef) getNode().getProperties().get(DocumentCommonModel.Props.FUNCTION);
        if (nodeRef != null) {
            Function function = BeanHelper.getFunctionsService().getFunctionByNodeRef(nodeRef);
            if (function != null) {
                return DocumentLocationGenerator.getFunctionLabel(function);
            }
        }
        return null;
    }

    public String getSeriesLabel() {
        NodeRef nodeRef = (NodeRef) getNode().getProperties().get(DocumentCommonModel.Props.SERIES);
        if (nodeRef != null) {
            Series series = BeanHelper.getSeriesService().getSeriesByNodeRef(nodeRef);
            if (series != null) {
                return DocumentLocationGenerator.getSeriesLabel(series);
            }
        }
        return null;
    }

    public String getCaseLabel() {
        NodeRef nodeRef = (NodeRef) getNode().getProperties().get(DocumentCommonModel.Props.CASE);
        if (nodeRef != null) {
            Case theCase = BeanHelper.getCaseService().getCaseByNoderef(nodeRef);
            if (theCase != null) {
                return DocumentLocationGenerator.getCaseLabel(theCase);
            }
        }
        return null;
    }

    public Date getDueDate() {
        // Only docsub:incomingLetter has this property
        return (Date) getNode().getProperties().get(DocumentSpecificModel.Props.DUE_DATE);
    }

    public String getDueDateStr() {
        final Date dueDate = getDueDate();
        return dueDate != null ? dateFormat.format(dueDate) : "";
    }

    public String getCreatedDateStr() {
        final Date created = getCreated();
        return created != null ? dateFormat.format(created) : "";
    }

    public String getSenderRegDateStr() {
        final Date senderRegDate = getSenderRegDate();
        return senderRegDate != null ? dateFormat.format(senderRegDate) : "";
    }

    public String getComplienceDateStr() {
        final Date complienceDate = getComplienceDate();
        return complienceDate != null ? dateFormat.format(complienceDate) : "";
    }

    public Date getComplienceDate() {
        // Only docsub:incomingLetter has this property
        return (Date) getNode().getProperties().get(DocumentSpecificModel.Props.COMPLIENCE_DATE);
    }

    // Additional properties that are used in document-search-extended-results-dialog.jsp

    public String getDocStatus() {
        return (String) getNode().getProperties().get(DocumentCommonModel.Props.DOC_STATUS);
    }

    public boolean isDocStatus(DocumentStatus status) {
        return status.getValueName().equals(getDocStatus());
    }

    public Date getSenderRegDate() {
        // Only docsub:incomingLetter and docsub:outgoingLetter have this property
        return (Date) getNode().getProperties().get(DocumentSpecificModel.Props.SENDER_REG_DATE);
    }

    public String getSenderRegNumber() {
        // Only docsub:incomingLetter and docsub:outgoingLetter have this property
        return (String) getNode().getProperties().get(DocumentSpecificModel.Props.SENDER_REG_NUMBER);
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

    @SuppressWarnings("unchecked")
    public String getOwnerOrgStructUnit() {
        return UserUtil.getDisplayUnit((List<String>) getNode().getProperties().get(DocumentCommonModel.Props.OWNER_ORG_STRUCT_UNIT));
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
        String transmittalMode = (String) getProperties().get(DocumentSpecificModel.Props.TRANSMITTAL_MODE);
        return TextUtil.joinStringAndStringWithComma(transmittalMode, (String) getSearchableProperties().get(DocumentCommonModel.Props.SEARCHABLE_SEND_MODE));
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
        lazyInit();
        return TextUtil.join(getProperties(), DocumentSpecificModel.Props.FIRST_PARTY_CONTACT_PERSON,
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

    public DateAndMultiPropsConvertedMap convertedPropsMap = new DateAndMultiPropsConvertedMap();

    /**
     * @author Vladimir Drozdik
     *         converts date type or multivalue properties for binding from JSP.
     */
    public class DateAndMultiPropsConvertedMap extends HashMap<String, Object> {
        private static final long serialVersionUID = 1L;

        @Override
        public Object get(Object propKey) {
            Object propValue = getProperties().get(propKey);
            if (propValue instanceof Date) {
                return dateFormat.format(propValue);
            } else if (propValue instanceof Collection) {
                if (propValue instanceof List && !((List) propValue).isEmpty() && ((List) propValue).get(0) instanceof String) {
                    return UserUtil.getDisplayUnit((List<String>) propValue);
                }
                return propValue.toString();
            } else {
                return propValue;
            }
        }

    }

    public Map<String, Object> getConvertedPropsMap() {
        return convertedPropsMap;
    }

    public UnitStrucPropsConvertedMap unitStrucPropsConvertedMap = new UnitStrucPropsConvertedMap();

    /**
     * @author Vladimir Drozdik
     *         converts unit structure for binding from JSP.
     */
    public class UnitStrucPropsConvertedMap extends HashMap<String, Object> {
        private static final long serialVersionUID = 1L;

        @SuppressWarnings("unchecked")
        @Override
        public Object get(Object propKey) {
            Object propValue = getProperties().get(propKey);
            if (propValue == null) {
                return "";
            }
            if (!(propValue instanceof Iterable)) {
                return propValue;
            }
            return UserUtil.getDisplayUnit((Iterable<String>) propValue);
        }
    }

    public Map<String, Object> getUnitStrucPropsConvertedMap() {
        return unitStrucPropsConvertedMap;
    }

}
