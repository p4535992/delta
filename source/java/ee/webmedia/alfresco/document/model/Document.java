package ee.webmedia.alfresco.document.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.event.ActionEvent;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.servlet.DownloadContentServlet;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.app.AppConstants;
import ee.webmedia.alfresco.classificator.enums.AccessRestriction;
import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.common.service.BulkLoadNodeService;
import ee.webmedia.alfresco.common.service.CreateSimpleFileCallback;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.CssStylable;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel.Props;
import ee.webmedia.alfresco.docconfig.bootstrap.SystematicDocumentType;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.file.model.FileModel;
import ee.webmedia.alfresco.document.file.model.MDeltaFile;
import ee.webmedia.alfresco.document.file.model.SimpleFile;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.document.type.service.DocumentTypeService;
import ee.webmedia.alfresco.dvk.model.DvkModel;
import ee.webmedia.alfresco.privilege.model.Privilege;
import ee.webmedia.alfresco.privilege.service.PrivilegeService;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.TextUtil;
import ee.webmedia.alfresco.utils.UserUtil;
import ee.webmedia.alfresco.volume.model.UnmodifiableVolume;

public class Document extends Node implements Comparable<Document>, CssStylable, CreatedAndRegistered, DocumentListRowLink {
    public static final String GENERIC_DOCUMENT_STYLECLASS = "genericDocument";

    private static final QName MAIN_DOCUMENT_PROP = RepoUtil.createTransientProp("mainDocument");
    private static final QName DOCUMENT_TO_SIGN_PROP = RepoUtil.createTransientProp("documentToSign");
    private static final Set<QName> MDELTA_FILE_PROPS = new HashSet<QName>(Arrays.asList(FileModel.Props.DISPLAY_NAME, DvkModel.Props.DVK_ID,
            ContentModel.PROP_NAME, ContentModel.PROP_CONTENT, FileModel.Props.ACTIVE));

    private static final long serialVersionUID = 1L;

    public static final int SHORT_PROP_LENGTH = 20;
    public static FastDateFormat dateFormat = FastDateFormat.getInstance("dd.MM.yyyy");
    public static FastDateFormat dateTimeFormat = FastDateFormat.getInstance("dd.MM.yyyy HH:mm");

    private List<SimpleFile> files; // load lazily
    private List<File> inactiveFiles; // load lazily
    private String workflowStatus;
    private String documentCachedTypeId;
    private String documentTypeName;
    // true if link to document details should be displayed in document list.
    // At present used only in compound worklfow associated documents list.
    private boolean showLink;
    private UnmodifiableVolume volume;
    private String ownerOrgStructUnit;
    private boolean isMDeltaFiles;

    private String functionLabel;
    private String seriesLabel;
    private String volumeLabel;
    private String caseLabel;

    /** To be only accessed using {@link #getDocumentType()} */
    private transient DocumentTypeService documentTypeService;
    private static final Map<QName, QName> PROPS_WITH_ALTERNATIVES;

    static {
        Map<QName, QName> props = new LinkedHashMap<>();
        props.put(DocumentSpecificModel.Props.SECOND_PARTY_NAME, null);
        props.put(DocumentSpecificModel.Props.THIRD_PARTY_NAME, null);
        props.put(DocumentSpecificModel.Props.PARTY_NAME, null);
        props.put(DocumentCommonModel.Props.RECIPIENT_NAME, DocumentDynamicModel.Props.RECIPIENT_PERSON_NAME);
        props.put(DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_NAME, DocumentDynamicModel.Props.ADDITIONAL_RECIPIENT_PERSON_NAME);
        PROPS_WITH_ALTERNATIVES = Collections.unmodifiableMap(props);
    }

    /**
     * Copy constructory
     *
     * @param source
     */
    public Document(Document source) {
        super(source.nodeRef);
        nodeRef = source.nodeRef;
        Assert.notNull(source, "Source document is mandatory");
        files = source.getFiles(null);
        inactiveFiles = source.getInactiveFiles();
    }

    public Document(NodeRef nodeRef) {
        super(nodeRef);
        this.nodeRef = nodeRef;
    }

    public Document(NodeRef nodeRef, Map<String, Object> properties) {
        this(nodeRef);
        if (properties != null && !properties.isEmpty()) {
            this.properties.putAll(properties);
            propsRetrieved = true;
        }
    }

    public Node getNode() {
        return this;
    }

    public String getDocumentTypeName() {
        String objectTypeId = getObjectTypeId();
        if (documentTypeName == null || !objectTypeId.equals(documentCachedTypeId)) {
            documentCachedTypeId = objectTypeId;
            documentTypeName = BeanHelper.getDocumentAdminService().getDocumentTypeName(documentCachedTypeId);
        }
        return documentTypeName;
    }

    public void setDocumentTypeName(String documentTypeName) {
        this.documentTypeName = documentTypeName;
    }

    public String getObjectTypeId() {
        return (String) getProperties().get(Props.OBJECT_TYPE_ID);
    }

    @Override
    public String getCssStyleClass() {
        String cssStyleClass = getObjectTypeId();
        if (SystematicDocumentType.INCOMING_LETTER.isSameType(cssStyleClass)
                || SystematicDocumentType.OUTGOING_LETTER.isSameType(cssStyleClass)) {
            return cssStyleClass;
        }
        return GENERIC_DOCUMENT_STYLECLASS;
    }

    // Basic properties that are used in document-list-dialog.jsp

    public String getRegNumber() {
        return (String) getProperties().get(DocumentCommonModel.Props.REG_NUMBER);
    }

    public String getAkString() {
        if (AccessRestriction.AK.getValueName().equalsIgnoreCase(getAccessRestriction())) {
            return "[AK] ";
        }
        return "";
    }

    @Override
    public Date getRegDateTime() {
        return (Date) getProperties().get(DocumentCommonModel.Props.REG_DATE_TIME);
    }

    public String getRegDateTimeStr() {
        return formatDate(getRegDateTime());
    }

    public String getEmailDateTimeStr() {
        return formatDate((Date) getProperties().get(DocumentCommonModel.Props.EMAIL_DATE_TIME));
    }

    private String formatDate(Date date) {
        return date != null ? dateFormat.format(date) : "";
    }

    public String getSender() {
        return getPropOrAlternativeIfPropIsBlank(getProperties(), DocumentSpecificModel.Props.SENDER_DETAILS_NAME, DocumentDynamicModel.Props.SENDER_PERSON_NAME);
    }

    public String getSenderOrOwner() {
        String docDynType = getObjectTypeId();
        if (SystematicDocumentType.INCOMING_LETTER.isSameType(docDynType)) {
            return (String) getProperties().get(DocumentSpecificModel.Props.SENDER_DETAILS_NAME);
        }
        return (String) getProperties().get(DocumentCommonModel.Props.OWNER_NAME);
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
        return TextUtil.join(getProperties(), DocumentCommonModel.Props.RECIPIENT_NAME, DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_NAME);
    }

    public String getSenderOrRecipient() {
        String docDynType = getObjectTypeId();
        if (SystematicDocumentType.INCOMING_LETTER.isSameType(docDynType)) {
            return getPropOrAlternativeIfPropIsBlank(getProperties(), DocumentSpecificModel.Props.SENDER_DETAILS_NAME, DocumentDynamicModel.Props.SENDER_PERSON_NAME);
        } else if (SystematicDocumentType.INVOICE.isSameType(docDynType)) {
            return (String) getProperties().get(DocumentSpecificModel.Props.SELLER_PARTY_NAME);
        }
        return TextUtil.join(getProperties(), PROPS_WITH_ALTERNATIVES);
    }

    private String getPropOrAlternativeIfPropIsBlank(Map<String, Object> props, QName primaryChoice, QName alternative) {
        String result = (String) props.get(primaryChoice);
        if (StringUtils.isBlank(result)) {
            result = (String) props.get(alternative);
        }
        return result;
    }

    public String getAllRecipients() {
        return TextUtil.join(getProperties(), PROPS_WITH_ALTERNATIVES, TextUtil.SEMICOLON_SEPARATOR);
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
        return (String) getProperties().get(DocumentCommonModel.Props.DOC_NAME);
    }

    public String getVolumeLabel() {
        if (volumeLabel == null) {
            NodeRef nodeRef = (NodeRef) getNode().getProperties().get(DocumentCommonModel.Props.VOLUME);
            if (nodeRef != null) {
                volumeLabel = BeanHelper.getVolumeService().getVolumeLabel(nodeRef);
            } else {
                volumeLabel = "";
            }
        }
        return volumeLabel;
    }

    public String getFunctionLabel() {
        if (functionLabel == null) {
            NodeRef nodeRef = (NodeRef) getProperties().get(DocumentCommonModel.Props.FUNCTION);
            if (nodeRef != null) {
                functionLabel = BeanHelper.getFunctionsService().getFunctionLabel(nodeRef);
            } else {
                functionLabel = "";
            }
        }
        return functionLabel;
    }

    public String getSeriesLabel() {
        if (seriesLabel == null) {
            NodeRef seriesRef = (NodeRef) getNode().getProperties().get(DocumentCommonModel.Props.SERIES);
            if (seriesRef != null) {
                seriesLabel = BeanHelper.getSeriesService().getSeriesLabel(seriesRef);
            } else {
                seriesLabel = "";
            }
        }
        return seriesLabel;
    }

    public String getCaseLabel() {
        if (caseLabel == null) {
            NodeRef caseRef = (NodeRef) getNode().getProperties().get(DocumentCommonModel.Props.CASE);
            if (caseRef != null) {
                caseLabel = BeanHelper.getCaseService().getCaseLabel(caseRef);
            } else {
                caseLabel = "";
            }
        }
        return caseLabel;
    }

    public Date getDueDate() {
        // Only docsub:incomingLetter has this property
        return (Date) getProperties().get(DocumentSpecificModel.Props.DUE_DATE);
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
        return (Date) getProperties().get(DocumentSpecificModel.Props.COMPLIENCE_DATE);
    }

    // Additional properties that are used in document-search-extended-results-dialog.jsp

    public String getDocStatus() {
        return (String) getProperties().get(DocumentCommonModel.Props.DOC_STATUS);
    }

    public boolean isDocStatus(DocumentStatus status) {
        return status.getValueName().equals(getDocStatus());
    }

    public Date getSenderRegDate() {
        // Only docsub:incomingLetter and docsub:outgoingLetter have this property
        return (Date) getProperties().get(DocumentSpecificModel.Props.SENDER_REG_DATE);
    }

    public String getSenderRegNumber() {
        // Only docsub:incomingLetter and docsub:outgoingLetter have this property
        return (String) getProperties().get(DocumentSpecificModel.Props.SENDER_REG_NUMBER);
    }

    public String getAccessRestriction() {
        return (String) getProperties().get(DocumentCommonModel.Props.ACCESS_RESTRICTION);
    }

    public String getAccessRestrictionReason() {
        return (String) getProperties().get(DocumentCommonModel.Props.ACCESS_RESTRICTION_REASON);
    }

    public Date getAccessRestrictionBeginDate() {
        return (Date) getProperties().get(DocumentCommonModel.Props.ACCESS_RESTRICTION_BEGIN_DATE);
    }

    public Date getAccessRestrictionEndDate() {
        return (Date) getProperties().get(DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DATE);
    }

    public String getAccessRestrictionEndDesc() {
        return (String) getProperties().get(DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DESC);
    }

    public String getOwnerId() {
        return (String) getProperties().get(DocumentCommonModel.Props.OWNER_ID);
    }

    public String getOwnerName() {
        return (String) getProperties().get(DocumentCommonModel.Props.OWNER_NAME);
    }

    @SuppressWarnings("unchecked")
    public String getOwnerOrgStructUnit() {
        if (ownerOrgStructUnit == null) {
            ownerOrgStructUnit = UserUtil.getDisplayUnit((List<String>) getProperties().get(DocumentCommonModel.Props.OWNER_ORG_STRUCT_UNIT));
        }
        return ownerOrgStructUnit;
    }

    public String getOwnerJobTitle() {
        return (String) getProperties().get(DocumentCommonModel.Props.OWNER_JOB_TITLE);
    }

    public String getSignerName() {
        return (String) getProperties().get(DocumentCommonModel.Props.SIGNER_NAME);
    }

    public String getSignerJobTitle() {
        return (String) getProperties().get(DocumentCommonModel.Props.SIGNER_JOB_TITLE);
    }

    public String getKeywords() {
        return (String) getProperties().get(DocumentCommonModel.Props.KEYWORDS);
    }

    @SuppressWarnings("unchecked")
    public String getHierarchicalKeywords() {
        return TextUtil.joinStringLists((List<String>) getProperties().get(DocumentDynamicModel.Props.FIRST_KEYWORD_LEVEL),
                (List<String>) getProperties().get(DocumentDynamicModel.Props.SECOND_KEYWORD_LEVEL));
    }

    public String getStorageType() {
        return (String) getProperties().get(DocumentCommonModel.Props.STORAGE_TYPE);
    }

    public String getSendMode() {
        List<String> searchableSendMode = getSearchableSendModeFromGeneralProps();
        return TextUtil.joinStringAndStringWithComma(getTransmittalMode(), searchableSendMode != null ? TextUtil.joinNonBlankStringsWithComma(searchableSendMode) : "");
    }

    @SuppressWarnings("unchecked")
    public String getSendInfoRecipient() {
        return TextUtil.joinNonBlankStringsWithComma((List<String>) getProperties().get(DocumentCommonModel.Props.SEARCHABLE_SEND_INFO_RECIPIENT));
    }

    public String getSendInfoSendDateTime() {
        @SuppressWarnings("unchecked")
        List<Date> dates = (List<Date>) getProperties().get(DocumentCommonModel.Props.SEARCHABLE_SEND_INFO_SEND_DATE_TIME);
        String result = "";
        if (dates != null && !dates.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            boolean firstAppended = false;
            for (int i = 0; i < dates.size(); i++) {
                Date date = dates.get(i);
                if (date == null) {
                    continue;
                }
                if (firstAppended) {
                    sb.append(", ");
                }
                sb.append(dateTimeFormat.format(date));
                firstAppended = true;
            }
            result = sb.toString();
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public String getSendInfoResolution() {
        return TextUtil.joinNonBlankStringsWithComma((List<String>) getProperties().get(DocumentCommonModel.Props.SEARCHABLE_SEND_INFO_RESOLUTION));
    }

    public List<String> getSendModesAsList() {
        List<String> modes = new ArrayList<String>();
        modes.addAll(getSearchableSendModeFromGeneralProps());
        modes.add(getTransmittalMode());
        return modes;
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
        return (String) getProperties().get(DocumentSpecificModel.Props.RESPONSIBLE_NAME);
    }

    public String getCoResponsibles() {
        // Only docsub:managementsOrder has this property
        return (String) getProperties().get(DocumentSpecificModel.Props.CO_RESPONSIBLES);
    }

    public String getContactPerson() {
        // Only docsub:contractSim and docsub:contractSmit have these properties
        return TextUtil.join(getProperties(), DocumentSpecificModel.Props.FIRST_PARTY_CONTACT_PERSON,
                DocumentSpecificModel.Props.SECOND_PARTY_CONTACT_PERSON,
                DocumentSpecificModel.Props.THIRD_PARTY_CONTACT_PERSON);
    }

    public String getProcurementType() {
        // Only docsub:tenderingApplication has this property
        return (String) getProperties().get(DocumentSpecificModel.Props.PROCUREMENT_TYPE);
    }

    public String getSellerPartyRegNumber() {
        return (String) getProperties().get(DocumentSpecificModel.Props.SELLER_PARTY_REG_NUMBER);
    }

    public String getSellerPartyName() {
        return (String) getProperties().get(DocumentSpecificModel.Props.SELLER_PARTY_NAME);
    }

    public String getInvoiceNumber() {
        return (String) getProperties().get(DocumentSpecificModel.Props.INVOICE_NUMBER);
    }

    public String getTotalSum() {
        return (String) getProperties().get(DocumentSpecificModel.Props.TOTAL_SUM);
    }

    public Date getInvoiceDate() {
        return (Date) getProperties().get(DocumentSpecificModel.Props.INVOICE_DATE);
    }

    public String getInvoiceDateStr() {
        return getInvoiceDate() != null ? dateFormat.format(getInvoiceDate()) : "";
    }

    @Override
    public Date getCreated() {
        return (Date) getProperties().get(ContentModel.PROP_CREATED);
    }

    public Boolean getMainDocument() {
        return Boolean.TRUE.equals(getProperties().get(MAIN_DOCUMENT_PROP.toString()));
    }

    public void setMainDocument(Boolean mainDocument) {
        getProperties().put(MAIN_DOCUMENT_PROP.toString(), mainDocument);
    }

    public Boolean getDocumentToSign() {
        return Boolean.TRUE.equals(getProperties().get(DOCUMENT_TO_SIGN_PROP.toString()));
    }

    public void setDocumentToSign(Boolean documentToSign) {
        getProperties().put(DOCUMENT_TO_SIGN_PROP.toString(), documentToSign);
    }

    public UnmodifiableVolume getDocumentVolume() {
        return getDocumentVolume(null);
    }

    public UnmodifiableVolume getDocumentVolume(Map<Long, QName> propertyTypes) {
        if (volume == null) {
            NodeRef nodeRef = (NodeRef) getNode().getProperties().get(DocumentCommonModel.Props.VOLUME);
            if (nodeRef != null) {
                volume = BeanHelper.getVolumeService().getUnmodifiableVolume(nodeRef, propertyTypes);
            }
        }
        return volume;
    }

    public void setVolume(UnmodifiableVolume volume) {
        this.volume = volume;
    }

    public void setFiles(List<SimpleFile> files) {
        this.files = files;
    }

    /**
     * Never call this method and {@link #getFiles()} on the same object.
     *
     * @see #getFiles()
     */
    public List<SimpleFile> getFiles(Map<Long, QName> propertyTypes) {
        if (isMDeltaFiles || files == null) {
            isMDeltaFiles = false;
            // probably not the best idea to call service from model, but alternatives get probably too complex
            BulkLoadNodeService bulkLoadNodeService = BeanHelper.getBulkLoadNodeService();
            try {
                files = bulkLoadNodeService.loadActiveFiles(getNodeRef(), propertyTypes);
            } catch (InvalidNodeRefException e) {
                // Document has been deleted between initial transaction (that constructed document list)
                // and this transaction (JSF rendering phase, value-binding from JSP is being resolved).
                // Removing a row at current stage would be too complicated and displaying an error message too confusing,
                // so just silence the exception - user sees document row with no file icons.
                files = new ArrayList<SimpleFile>();
            }
        }
        return files;
    }

    /**
     * Never call this method and {@link #getFiles(Map)} on the same object.
     *
     * @see #getFiles(Map)
     */
    public List<SimpleFile> getFiles() {
        if (!isMDeltaFiles || files == null) {
            isMDeltaFiles = true;
            BulkLoadNodeService bulkLoadNodeService = BeanHelper.getBulkLoadNodeService();
            final PrivilegeService privilegeService = BeanHelper.getPrivilegeService();
            final String userName = AuthenticationUtil.getRunAsUser();
            try {
                CreateSimpleFileCallback<MDeltaFile> callback = new CreateSimpleFileCallback<MDeltaFile>() {
                    @Override
                    public MDeltaFile create(Map<QName, Serializable> fileProps, Serializable... objects) {
                        String displayName = (String) fileProps.get(FileModel.Props.DISPLAY_NAME);
                        if (StringUtils.isBlank(displayName)) {
                            displayName = (String) fileProps.get(ContentModel.PROP_NAME);
                        }
                        String readOnlyUrl = DownloadContentServlet.generateDownloadURL((NodeRef) fileProps.get(ContentModel.PROP_NODE_REF), displayName);
                        long size = DefaultTypeConverter.INSTANCE.convert(ContentData.class, fileProps.get(ContentModel.PROP_CONTENT)).getSize();
                        MDeltaFile file = new MDeltaFile(displayName, readOnlyUrl, size, nodeRef);
                        boolean viewDocumentFiles = privilegeService.hasPermission(nodeRef, userName, Privilege.VIEW_DOCUMENT_FILES);
                        file.setViewDocumentFilesPermission(viewDocumentFiles);
                        return file;
                    }
                };
                files = new ArrayList<>();
                List<MDeltaFile> filez = bulkLoadNodeService.loadActiveFiles(nodeRef, null, MDELTA_FILE_PROPS, callback);
                files.addAll(filez);
            } catch (InvalidNodeRefException e) {
                files = new ArrayList<>();
            }
        }
        return files;
    }

    public List<File> getInactiveFiles() {
        if (inactiveFiles == null) {
            // probably not the best idea to call service from model, but alternatives get probably too complex
            FileService fileService = BeanHelper.getFileService();
            try {
                inactiveFiles = (List<File>) CollectionUtils.select(fileService.getAllActiveAndInactiveFiles(nodeRef), new Predicate<File>() { // TODO - Can be optimized.

                            @Override
                            public boolean evaluate(File file) {
                                return !file.isActive();
                            }

                        });
            } catch (InvalidNodeRefException e) {
                // Document has been deleted between initial transaction (that constructed document list)
                // and this transaction (JSF rendering phase, value-binding from JSP is being resolved).
                // Removing a row at current stage would be too complicated and displaying an error message too confusing,
                // so just silence the exception - user sees document row with no file icons.
                inactiveFiles = new ArrayList<File>();
            }
        }
        return inactiveFiles;
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
        return AppConstants.getNewCollatorInstance().compare(getRegNumber(), other.getRegNumber());
    }

    @Override
    public String toString() {
        return new StringBuilder("Document:")//
                .append("\n\tregNumber = " + getRegNumber())
                .append("\n\tdocName = " + getDocName())
                .toString();
    }

    protected DocumentTypeService getDocumentTypeService() {
        if (documentTypeService == null) {
            documentTypeService = BeanHelper.getDocumentTypeService();
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

    public boolean filesLoaded() {
        return files != null;
    }

}
