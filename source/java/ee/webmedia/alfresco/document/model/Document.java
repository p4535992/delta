package ee.webmedia.alfresco.document.model;

import static ee.webmedia.alfresco.utils.TextUtil.LIST_SEPARATOR;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

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
import ee.webmedia.alfresco.classificator.enums.AccessRestriction;
import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.CssStylable;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel.Props;
import ee.webmedia.alfresco.docconfig.bootstrap.SystematicDocumentType;
import ee.webmedia.alfresco.docconfig.generator.systematic.DocumentLocationGenerator;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.document.type.service.DocumentTypeService;
import ee.webmedia.alfresco.functions.model.Function;
import ee.webmedia.alfresco.series.model.Series;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.TextUtil;
import ee.webmedia.alfresco.utils.UserUtil;
import ee.webmedia.alfresco.volume.model.Volume;

public class Document extends Node implements Comparable<Document>, CssStylable, CreatedAndRegistered, DocumentListRowLink {
    public static final String GENERIC_DOCUMENT_STYLECLASS = "genericDocument";

    private static final QName MAIN_DOCUMENT_PROP = RepoUtil.createTransientProp("mainDocument");
    private static final QName DOCUMENT_TO_SIGN_PROP = RepoUtil.createTransientProp("documentToSign");

    private static final long serialVersionUID = 1L;

    public static final int SHORT_PROP_LENGTH = 20;
    public static FastDateFormat dateFormat = FastDateFormat.getInstance("dd.MM.yyyy");
    public static FastDateFormat dateTimeFormat = FastDateFormat.getInstance("dd.MM.yyyy HH:mm");

    private List<File> files; // load lazily
    private String workflowStatus;
    private Map<QName, Serializable> searchableProperties;
    private boolean initialized;
    // true if link to document details should be displayed in document list.
    // At present used only in compound worklfow associated documents list.
    private boolean showLink;

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
        return GENERIC_DOCUMENT_STYLECLASS;
    }

    // Basic properties that are used in document-list-dialog.jsp

    public String getRegNumber() {
        return (String) getNode().getProperties().get(DocumentCommonModel.Props.REG_NUMBER);
    }

    public String getAkString() {
        if (AccessRestriction.AK.getValueName().equalsIgnoreCase(getAccessRestriction())) {
            return "[AK] ";
        }
        return "";
    }

    @Override
    public Date getRegDateTime() {
        return (Date) getNode().getProperties().get(DocumentCommonModel.Props.REG_DATE_TIME);
    }

    public String getRegDateTimeStr() {
        return formatDate(getRegDateTime());
    }

    public String getEmailDateTimeStr() {
        return formatDate((Date) getNode().getProperties().get(DocumentCommonModel.Props.EMAIL_DATE_TIME));
    }

    private String formatDate(Date date) {
        return date != null ? dateFormat.format(date) : "";
    }

    public String getSender() {
        String docDynType = objectTypeId();
        if (SystematicDocumentType.INCOMING_LETTER.isSameType(docDynType)) {
            return (String) getProperties().get(DocumentSpecificModel.Props.SENDER_DETAILS_NAME);
        } else if (SystematicDocumentType.INVOICE.isSameType(docDynType)) {
            return (String) getProperties().get(DocumentSpecificModel.Props.SELLER_PARTY_NAME);
        }
        return TextUtil.join(
                getProperties()
                , DocumentSpecificModel.Props.SECOND_PARTY_NAME
                , DocumentSpecificModel.Props.THIRD_PARTY_NAME
                , DocumentSpecificModel.Props.PARTY_NAME
                , DocumentCommonModel.Props.RECIPIENT_NAME
                , DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_NAME
                );
    }

    public String getSenderNameOrEmail() {
        String senderDetails = (String) getProperties().get(DocumentSpecificModel.Props.SENDER_DETAILS_NAME);
        if (StringUtils.isBlank(senderDetails)) {
            String senderEmail = (String) getProperties().get(DocumentSpecificModel.Props.SENDER_DETAILS_EMAIL);
            senderDetails = senderEmail != null ? senderEmail : "";
        }
        return senderDetails;
    }

    public String getRecipients() {
        lazyInit();
        return TextUtil.join(getProperties(), DocumentCommonModel.Props.RECIPIENT_NAME, DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_NAME);
    }

    public String getSenderOrRecipient() {
        String docDynType = objectTypeId();
        if (SystematicDocumentType.INCOMING_LETTER.isSameType(docDynType)) {
            return (String) getProperties().get(DocumentSpecificModel.Props.SENDER_DETAILS_NAME);
        } else if (SystematicDocumentType.INVOICE.isSameType(docDynType)) {
            return (String) getProperties().get(DocumentSpecificModel.Props.SELLER_PARTY_NAME);
        }
        return getAllRecipients();
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

    // BEGIN properties collected from child nodes
    @SuppressWarnings("unchecked")
    public List<String> getPartyNames() {
        return getListOrNull(DocumentSpecificModel.Props.PARTY_NAME);
    }

    @SuppressWarnings("unchecked")
    public List<String> getPartyContactPersons() {
        return getListOrNull(DocumentSpecificModel.Props.PARTY_CONTACT_PERSON);
    }

    @SuppressWarnings("unchecked")
    public List<String> getApplicantNames() {
        return getListOrNull(DocumentSpecificModel.Props.APPLICANT_NAME);
    }

    @SuppressWarnings("unchecked")
    public List<String> getCostManagers() {
        return getListOrNull(DocumentSpecificModel.Props.COST_MANAGER);
    }

    @SuppressWarnings("unchecked")
    public List<Date> getErrandBeginDates() {
        return getListOrNull(DocumentSpecificModel.Props.ERRAND_BEGIN_DATE);
    }

    @SuppressWarnings("unchecked")
    public List<Date> getErrandEndDates() {
        return getListOrNull(DocumentSpecificModel.Props.ERRAND_END_DATE);
    }

    @SuppressWarnings("unchecked")
    public List<String> getCountries() {
        return getListOrNull(DocumentSpecificModel.Props.ERRAND_COUNTRY);
    }

    @SuppressWarnings("unchecked")
    public List<String> getCounties() {
        return getListOrNull(DocumentSpecificModel.Props.ERRAND_COUNTY);
    }

    @SuppressWarnings("unchecked")
    public List<String> getCities() {
        return getListOrNull(DocumentSpecificModel.Props.ERRAND_CITY);
    }

    @SuppressWarnings("rawtypes")
    private List getListOrNull(QName propName) {
        Object value = getProperties().get(propName);
        return value instanceof List ? (List) value : null;
    }

    // END properties collected from child nodes

    public String getFirstPartyContactPerson() {
        return (String) getProperties().get(DocumentDynamicModel.Props.FIRST_PARTY_CONTACT_PERSON_NAME);
    }

    public String getDelivererName() {
        return (String) getProperties().get(DocumentSpecificModel.Props.DELIVERER_NAME);
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
        Volume volume = getDocumentVolume();
        if (volume != null) {
            return DocumentLocationGenerator.getVolumeLabel(volume);
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

    public String getCreatedDateTimeStr() {
        final Date created = getCreated();
        return created != null ? dateTimeFormat.format(created) : "";
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

    @SuppressWarnings("unchecked")
    public String getHierarchicalKeywords() {
        return TextUtil.joinStringLists((List<String>) getProperties().get(DocumentDynamicModel.Props.FIRST_KEYWORD_LEVEL),
                (List<String>) getProperties().get(DocumentDynamicModel.Props.SECOND_KEYWORD_LEVEL));
    }

    public String getStorageType() {
        return (String) getNode().getProperties().get(DocumentCommonModel.Props.STORAGE_TYPE);
    }

    public String getSendMode() {
        List<String> searchableSendMode = getSearchableSendModeFromGeneralProps();
        return TextUtil.joinStringAndStringWithComma(getTransmittalMode(), searchableSendMode != null ? TextUtil.joinNonBlankStringsWithComma(searchableSendMode) : "");
    }

    @SuppressWarnings("unchecked")
    public List<String> getSearchableSendMode() {
        return (List<String>) getSearchableProperties().get(DocumentCommonModel.Props.SEARCHABLE_SEND_MODE);
    }

    @SuppressWarnings("unchecked")
    public List<String> getSearchableSendModeFromGeneralProps() {
        return (List<String>) getProperties().get(DocumentCommonModel.Props.SEARCHABLE_SEND_MODE);
    }

    public String getTransmittalMode() {
        return (String) getProperties().get(DocumentSpecificModel.Props.TRANSMITTAL_MODE);
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

    public String getSellerPartyName() {
        return (String) getNode().getProperties().get(DocumentSpecificModel.Props.SELLER_PARTY_NAME);
    }

    public String getInvoiceNumber() {
        return (String) getNode().getProperties().get(DocumentSpecificModel.Props.INVOICE_NUMBER);
    }

    public String getTotalSum() {
        return (String) getNode().getProperties().get(DocumentSpecificModel.Props.TOTAL_SUM);
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

    public Boolean getMainDocument() {
        return Boolean.TRUE.equals(getNode().getProperties().get(MAIN_DOCUMENT_PROP.toString()));
    }

    public void setMainDocument(Boolean mainDocument) {
        getProperties().put(MAIN_DOCUMENT_PROP.toString(), mainDocument);
    }

    public Boolean getDocumentToSign() {
        return Boolean.TRUE.equals(getNode().getProperties().get(DOCUMENT_TO_SIGN_PROP.toString()));
    }

    public void setDocumentToSign(Boolean documentToSign) {
        getProperties().put(DOCUMENT_TO_SIGN_PROP.toString(), documentToSign);
    }

    public Volume getDocumentVolume() {
        NodeRef nodeRef = (NodeRef) getNode().getProperties().get(DocumentCommonModel.Props.VOLUME);
        if (nodeRef != null) {
            return BeanHelper.getVolumeService().getVolumeByNodeRef(nodeRef);
        }
        return null;
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

    public PropsConvertedMap convertedPropsMap;

    public Map<String, Object> getConvertedPropsMap() {
        if (convertedPropsMap == null) {
            convertedPropsMap = new PropsConvertedMap(getProperties(), false);
        }
        return convertedPropsMap;
    }

    public PropsConvertedMap propsConvertedMap;

    public Map<String, Object> getUnitStrucPropsConvertedMap() {
        if (propsConvertedMap == null) {
            propsConvertedMap = new PropsConvertedMap(getProperties(), true);
        }
        return propsConvertedMap;
    }

    public void setShowLink(boolean showLink) {
        this.showLink = showLink;
    }

    public boolean isShowLink() {
        return showLink;
    }

    @Override
    public String getAction() {
        return BeanHelper.getDocumentDialog().action();
    }

    @Override
    public void open(ActionEvent event) {
        BeanHelper.getDocumentDialog().open(event);
    }
}
