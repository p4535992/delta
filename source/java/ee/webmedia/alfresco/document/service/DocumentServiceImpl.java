package ee.webmedia.alfresco.document.service;

import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.ACCESS_RESTRICTION;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_EMAIL;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_NAME;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.CASE;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.DOC_NAME;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.DOC_STATUS;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.FILE_CONTENTS;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.FILE_NAMES;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.FUNCTION;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.OWNER_ID;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.RECIPIENT_EMAIL;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.RECIPIENT_NAME;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.REG_DATE_TIME;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.REG_NUMBER;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.SEARCHABLE_APPLICANT_NAME;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.SEARCHABLE_COST_MANAGER;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.SEARCHABLE_ERRAND_BEGIN_DATE;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.SEARCHABLE_ERRAND_CITY;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.SEARCHABLE_ERRAND_COUNTRY;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.SEARCHABLE_ERRAND_COUNTY;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.SEARCHABLE_ERRAND_END_DATE;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.SEARCHABLE_PARTY_NAME;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.SEARCHABLE_SEND_MODE;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.SEARCHABLE_SUB_NODE_PROPERTIES;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.SERIES;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.SHORT_REG_NUMBER;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.VOLUME;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.faces.context.FacesContext;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.i18n.I18NUtil;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.content.transform.ContentTransformer;
import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.dictionary.AspectDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.CopyService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.util.EqualsHelper;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.ui.common.Utils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrBuilder;
import org.apache.commons.lang.time.FastDateFormat;
import org.hibernate.StaleObjectStateException;
import org.joda.time.Days;
import org.joda.time.Instant;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.adr.service.AdrService;
import ee.webmedia.alfresco.cases.model.Case;
import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.cases.service.CaseService;
import ee.webmedia.alfresco.classificator.enums.AccessRestriction;
import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.classificator.enums.LeaveType;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.document.associations.model.DocAssocInfo;
import ee.webmedia.alfresco.document.bootstrap.DocumentPrivilegesUpdater;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.file.model.GeneratedFileType;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.document.log.service.DocumentLogService;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel.Privileges;
import ee.webmedia.alfresco.document.model.DocumentParentNodesVO;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;
import ee.webmedia.alfresco.document.permissions.SeriesDocManagerDynamicAuthority;
import ee.webmedia.alfresco.document.search.model.DocumentSearchModel;
import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.document.sendout.model.SendInfo;
import ee.webmedia.alfresco.document.sendout.service.SendOutService;
import ee.webmedia.alfresco.document.sendout.web.DocumentSendOutDialog;
import ee.webmedia.alfresco.document.type.model.DocumentType;
import ee.webmedia.alfresco.document.type.service.DocumentTypeHelper;
import ee.webmedia.alfresco.document.type.service.DocumentTypeService;
import ee.webmedia.alfresco.functions.model.FunctionsModel;
import ee.webmedia.alfresco.imap.model.ImapModel;
import ee.webmedia.alfresco.maais.model.MaaisModel;
import ee.webmedia.alfresco.maais.service.MaaisService;
import ee.webmedia.alfresco.privilege.model.PrivilegeMappings;
import ee.webmedia.alfresco.privilege.model.PrivilegeModel;
import ee.webmedia.alfresco.privilege.model.UserPrivileges;
import ee.webmedia.alfresco.privilege.service.PrivilegeService;
import ee.webmedia.alfresco.privilege.service.PrivilegeService.PrivilegesChangedListener;
import ee.webmedia.alfresco.register.model.Register;
import ee.webmedia.alfresco.register.service.RegisterService;
import ee.webmedia.alfresco.series.model.Series;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.series.service.SeriesService;
import ee.webmedia.alfresco.signature.exception.SignatureException;
import ee.webmedia.alfresco.signature.model.SignatureDigest;
import ee.webmedia.alfresco.signature.service.SignatureService;
import ee.webmedia.alfresco.template.service.DocumentTemplateService;
import ee.webmedia.alfresco.user.model.Authority;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.FilenameUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.utils.UnableToPerformException.MessageSeverity;
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.volume.model.VolumeModel;
import ee.webmedia.alfresco.volume.service.VolumeService;
import ee.webmedia.alfresco.workflow.model.TaskAndDocument;
import ee.webmedia.alfresco.workflow.service.SignatureTask;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.WorkflowService;

/**
 * @author Alar Kvell
 */
public class DocumentServiceImpl implements DocumentService, NodeServicePolicies.OnCreateAssociationPolicy, BeanFactoryAware, PrivilegesChangedListener, InitializingBean {

    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DocumentServiceImpl.class);

    private DictionaryService dictionaryService;
    private NamespaceService namespaceService;
    protected NodeService nodeService;
    private CopyService copyService;
    protected GeneralService generalService;
    private DocumentTypeService documentTypeService;
    private DocumentTemplateService documentTemplateService;
    private RegisterService registerService;
    protected VolumeService volumeService;
    protected SeriesService seriesService;
    private FileFolderService fileFolderService;
    private ContentService contentService;
    private FileService fileService;
    private SignatureService signatureService;
    private WorkflowService workflowService;
    private DocumentLogService documentLogService;
    private PrivilegeService privilegeService;
    private AuthorityService authorityService;
    private PermissionService permissionService;
    protected SendOutService sendOutService;
    private UserService userService;
    /** NB! not injected - use getter to obtain instance of AdrService */
    private AdrService _adrService;
    private CaseService _caseService;
    private DocumentSearchService _documentSearchService;
    private MaaisService _maaisService;
    protected BeanFactory beanFactory;

    private String fromDvkXPath;
    private String incomingEmailPath;
    protected String receivedInvoicePath;
    private String sentEmailPath;

    // doesn't need to be synchronized, because it is not modified during runtime
    private final Map<QName/* nodeType/nodeAspect */, PropertiesModifierCallback> creationPropertiesModifierCallbacks = new LinkedHashMap<QName, PropertiesModifierCallback>();

    private static final String REGISTRATION_INDIVIDUALIZING_NUM_SUFFIX = "-1";
    private static final FastDateFormat userDateFormat = FastDateFormat.getInstance("dd.MM.yyyy");
    private static final String TEMP_LOGGING_DISABLED_REGISTERED_BY_USER = "{temp}logging_registeredByUser";
    private PropertyChangesMonitorHelper propertyChangesMonitorHelper = new PropertyChangesMonitorHelper();

    @Override
    public void afterPropertiesSet() throws Exception {
        privilegeService.registerListener(DocumentCommonModel.Types.DOCUMENT, this);
        PolicyComponent policyComponent = (PolicyComponent) beanFactory.getBean("policyComponent", PolicyComponent.class);
        policyComponent.bindAssociationBehaviour(QName.createQName(NamespaceService.ALFRESCO_URI, "onCreateAssociation"), this, new JavaBehaviour(this, "onCreateAssociation"));
    }

    @Override
    public Node getDocument(NodeRef nodeRef) {
        Node document = generalService.fetchNode(nodeRef);
        setTransientProperties(document, getAncestorNodesByDocument(nodeRef));
        return document;
    }

    @Override
    public NodeRef getDrafts() {
        return generalService.getNodeRef(DocumentCommonModel.Repo.DRAFTS_SPACE);
    }

    @Override
    public Node createDocument(QName documentTypeId) {
        return createDocument(documentTypeId, null, null);
    }

    @Override
    public Node createDocument(QName documentTypeId, NodeRef parentRef, Map<QName, Serializable> properties) {
        return createDocument(documentTypeId, parentRef, properties, false, null);
    }

    private Node createDocument(QName documentTypeId, NodeRef parentRef, Map<QName, Serializable> properties
            , boolean withoutPropModifyingCallbacks, PropertiesModifierCallback callback) {

        // XXX do we need to check if document type is used?
        if (!dictionaryService.isSubClass(documentTypeId, DocumentCommonModel.Types.DOCUMENT)) {
            throw new RuntimeException("DocumentTypeId '" + documentTypeId.toPrefixString(namespaceService) + "' must be a subclass of '"
                    + DocumentCommonModel.Types.DOCUMENT.toPrefixString(namespaceService) + "'");
        }
        if (parentRef == null) {
            parentRef = getDrafts();
        }
        if (properties == null) {
            properties = new HashMap<QName, Serializable>();
        }

        Set<QName> aspects = generalService.getDefaultAspects(documentTypeId);
        // Add document type id. Now it's possible to modify props by doc type
        aspects.add(documentTypeId);

        for (QName docAspect : aspects) {
            callbackAspectProperiesModifier(docAspect, properties);
        }

        NodeRef document = createDocumentNode(documentTypeId, parentRef, properties);

        final Node documentNode = getDocument(document);
        // first iterate over callbacks to be able to predict in which order callbacks will be called (that is registration order).
        if (!withoutPropModifyingCallbacks) {
            modifyNode(documentNode, aspects, "docConstruction");
        }

        if (withoutPropModifyingCallbacks && callback != null) {
            callback.doWithNode(documentNode, "docConstruction");
            callback.doWithProperties(properties);
            return getDocument(document);
        }

        return documentNode;
    }

    private NodeRef createDocumentNode(QName documentTypeId, NodeRef parentRef, Map<QName, Serializable> properties) {
        NodeRef document = nodeService.createNode(parentRef, DocumentCommonModel.Assocs.DOCUMENT, DocumentCommonModel.Assocs.DOCUMENT //
                , documentTypeId, properties).getChildRef();
        updateParentNodesContainingDocsCount(document, true);
        permissionService.setInheritParentPermissions(document, false);
        return document;
    }

    @Override
    public Node createPPImportDocument(QName documentTypeId, NodeRef parentRef, Map<QName, Serializable> importProps) {
        Map<QName, Serializable> props = new HashMap<QName, Serializable>();

        Set<QName> aspects = generalService.getDefaultAspects(documentTypeId);
        // Add document type id. Now it's possible to modify props by doc type
        aspects.add(documentTypeId);

        // Perform callbacks with initial properties
        // for (QName docAspect : aspects) {
        // callbackAspectProperiesModifier(docAspect, props);
        // }
        // Then overwrite initial properties with our properties
        props.putAll(importProps);

        NodeRef document = createDocumentNode(documentTypeId, parentRef, props);
        nodeService.addAspect(document, DocumentCommonModel.Aspects.SEARCHABLE, null);

        final Node documentNode = getDocument(document);

        // modifyNode(documentNode, aspects, "docConstruction");
        return documentNode;
    }

    private void modifyNode(final Node documentNode, Set<QName> aspects, String phase) {
        for (QName callbackAspect : creationPropertiesModifierCallbacks.keySet()) {
            for (QName docAspect : aspects) {
                if (dictionaryService.isSubClass(docAspect, callbackAspect)) {
                    PropertiesModifierCallback callback = creationPropertiesModifierCallbacks.get(docAspect);
                    callback.doWithNode(documentNode, phase);
                }
            }
        }
    }

    @Override
    public void callbackAspectProperiesModifier(QName docAspect, Map<QName, Serializable> properties) {
        for (QName callbackAspect : creationPropertiesModifierCallbacks.keySet()) {
            if (dictionaryService.isSubClass(docAspect, callbackAspect)) {
                PropertiesModifierCallback callback = creationPropertiesModifierCallbacks.get(docAspect);
                callback.doWithProperties(properties);
            }
        }
    }

    /**
     * First change the type of the node, then remove
     * unnecessary aspects left from the previous type.
     * The properties of the aspect should persist if the aspect
     * was not removed.
     */
    @Override
    public void changeType(Node node) {
        QName newType = node.getType();
        NodeRef nodeRef = node.getNodeRef();

        /** No need to change the type if it's the same */
        if (newType.equals(nodeService.getType(nodeRef))) {
            return;
        }

        /** Changes the type of the node and adds required aspects, but does not remove unnecessary aspects */
        nodeService.setType(nodeRef, newType);

        /** Get all aspects for the new type */
        Set<QName> typeAspects = generalService.getDefaultAspects(newType);

        Set<QName> aspects = nodeService.getAspects(nodeRef);
        for (QName aspect : aspects) {
            if (!typeAspects.contains(aspect)) {
                if (log.isDebugEnabled()) {
                    log.debug("Removing " + aspect.getLocalName() + ", because it not part of type " + newType);
                }
                nodeService.removeAspect(nodeRef, aspect);
            }
        }
    }

    @Override
    public void changeTypeInMemory(Node docNode, QName newType) {
        docNode.setType(newType);
        Set<QName> aspects = docNode.getAspects();
        aspects.clear();
        aspects.addAll(generalService.getDefaultAspects(newType));
        fillDefaultProperties(docNode);
        { // might need to create in-memory child associations or remove in-memory child-associations created when last time changed the document type
            docNode.getAllChildAssociationsByAssocType().clear();
            docNode.getRemovedChildAssociations().clear();
            modifyNode(docNode, aspects, "docTypeChangeing"); // create childNodes for subPropSheets etc..
        }
    }

    @Override
    public void endDocument(NodeRef documentRef) {
        if (log.isDebugEnabled()) {
            log.debug("Ending document:" + documentRef);
        }
        Assert.notNull(documentRef, "Reference to document must be provided");
        nodeService.setProperty(documentRef, DOC_STATUS, DocumentStatus.FINISHED.getValueName());
        documentLogService.addDocumentLog(documentRef, I18NUtil.getMessage("document_log_status_proceedingFinish"));
        if (log.isDebugEnabled()) {
            log.debug("Document ended");
        }
    }

    @Override
    public void reopenDocument(final NodeRef documentRef) {
        if (log.isDebugEnabled()) {
            log.debug("Reopening document:" + documentRef);
        }
        Assert.notNull(documentRef, "Reference to document must be provided");
        getAdrService().addDeletedDocument(documentRef);
        // XXX: pole vist kõige kavalam lahendus, aga kuna uut töövoogu käivitades pannakse dok-omanikuks esimene täitja,
        // siis dokumendi omaniku õigusi käivitajal enam pole
        AuthenticationUtil.runAs(new RunAsWork<NodeRef>() {
            @Override
            public NodeRef doWork() throws Exception {
                nodeService.setProperty(documentRef, DOC_STATUS, DocumentStatus.WORKING.getValueName());
                return null;
            }
        }, AuthenticationUtil.getSystemUserName());
        if (log.isDebugEnabled()) {
            log.debug("Document reopened");
        }
    }

    private void fillDefaultProperties(Node node) {
        Map<String, Object> props = node.getProperties();
        if (node.hasAspect(DocumentCommonModel.Aspects.RECIPIENT)) {
            @SuppressWarnings("unchecked")
            List<String> list1 = (List<String>) props.get(RECIPIENT_NAME);
            list1 = DocumentSendOutDialog.newListIfNull(list1, true);

            @SuppressWarnings("unchecked")
            List<String> list2 = (List<String>) props.get(RECIPIENT_EMAIL);
            list2 = DocumentSendOutDialog.newListIfNull(list2, true);

            props.put(DocumentCommonModel.Props.RECIPIENT_NAME.toString(), list1);
            props.put(DocumentCommonModel.Props.RECIPIENT_EMAIL.toString(), list2);
        }
        if (node.hasAspect(DocumentCommonModel.Aspects.ADDITIONAL_RECIPIENT)) {
            @SuppressWarnings("unchecked")
            List<String> list1 = (List<String>) props.get(ADDITIONAL_RECIPIENT_NAME);
            list1 = DocumentSendOutDialog.newListIfNull(list1, true);

            @SuppressWarnings("unchecked")
            List<String> list2 = (List<String>) props.get(ADDITIONAL_RECIPIENT_EMAIL);
            list2 = DocumentSendOutDialog.newListIfNull(list2, true);

            props.put(ADDITIONAL_RECIPIENT_NAME.toString(), list1);
            props.put(ADDITIONAL_RECIPIENT_EMAIL.toString(), list2);
        }
    }

    @Override
    public Node updateDocument(final Node docNode) {
        final NodeRef docNodeRef = docNode.getNodeRef();
        final Map<String, Object> docProps = docNode.getProperties();

        // Prepare caseNodeRef
        final NodeRef volumeNodeRef = (NodeRef) docProps.get(TransientProps.VOLUME_NODEREF);
        NodeRef caseNodeRef = getCaseNodeRef(docProps, volumeNodeRef);

        // Prepare existingParentNode and targetParentRef properties
        final NodeRef targetParentRef;
        Node existingParentNode = null;
        if (caseNodeRef != null) {
            targetParentRef = caseNodeRef;
            existingParentNode = getCaseByDocument(docNodeRef);
            if (existingParentNode == null) { // moving from volume to case?
                existingParentNode = getVolumeByDocument(docNodeRef);
            }
        } else {
            targetParentRef = volumeNodeRef;
            final Volume volume = volumeService.getVolumeByNodeRef(targetParentRef);
            if (volume.isContainsCases()) {
                throw new RuntimeException("Selected volume '" + volume.getTitle() + "' must contain cases, not directly documents. Invalid caseNodeRef: '"
                        + caseNodeRef + "'");
            }
            existingParentNode = getVolumeByDocument(docNodeRef);
            if (existingParentNode == null) { // moving from case to volume?
                existingParentNode = getCaseByDocument(docNodeRef);
            }
        }

        // Prepare series and function properties
        NodeRef series = nodeService.getPrimaryParent(volumeNodeRef).getParentRef();
        if (series == null) {
            throw new RuntimeException("Volume parent is null: " + volumeNodeRef);
        }
        QName seriesType = nodeService.getType(series);
        if (!seriesType.equals(SeriesModel.Types.SERIES)) {
            throw new RuntimeException("Volume parent is not series, but " + seriesType + " - " + series);
        }
        NodeRef function = nodeService.getPrimaryParent(series).getParentRef();
        if (function == null) {
            throw new RuntimeException("Series parent is null: " + series);
        }
        QName functionType = nodeService.getType(function);
        if (!functionType.equals(FunctionsModel.Types.FUNCTION)) {
            throw new RuntimeException("Series parent is not function, but " + functionType + " - " + function);
        }
        docProps.put(FUNCTION.toString(), function);
        docProps.put(SERIES.toString(), series);
        docProps.put(VOLUME.toString(), volumeNodeRef);
        docProps.put(CASE.toString(), caseNodeRef);

        // If document is updated for the first time, add SEARCHABLE aspect to document and it's children files.
        if (!nodeService.hasAspect(docNodeRef, DocumentCommonModel.Aspects.SEARCHABLE)) {
            nodeService.addAspect(docNodeRef, DocumentCommonModel.Aspects.SEARCHABLE, null);
            docProps.put(FILE_NAMES.toString(), getSearchableFileNames(docNodeRef));
            docProps.put(FILE_CONTENTS.toString(), getSearchableFileContents(docNodeRef));
        }
        if (docNode.hasAspect(DocumentSpecificModel.Aspects.COMPLIENCE)) {
            Date complienceDate = (Date) docProps.get(DocumentSpecificModel.Props.COMPLIENCE_DATE);
            if (complienceDate != null) {
                docProps.put(DOC_STATUS.toString(), DocumentStatus.FINISHED.getValueName());
            }
        }
        docProps.putAll(getSearchableOtherProps(docNode));
        docProps.put(DocumentCommonModel.Props.SEARCHABLE_SEND_MODE.toString(), sendOutService.buildSearchableSendMode(docNodeRef));

        boolean propsChanged = saveChildNodes(docNode);
        // add any associations added in the UI
        propsChanged |= generalService.saveAddedAssocs(docNode) > 0;
        for (Map<String, AssociationRef> typedAssoc : docNode.getAddedAssociations().values()) {
            for (AssociationRef assoc : typedAssoc.values()) {
                updateModifiedDateTime(assoc.getSourceRef(), assoc.getTargetRef());
            }
        }

        makeChildNodesSearchable(docNodeRef);

        // If accessRestriction changes from OPEN/AK to INTERNAL
        if (AccessRestriction.INTERNAL.equals((String) docProps.get(ACCESS_RESTRICTION))) {
            String oldAccessRestriction = (String) nodeService.getProperty(docNodeRef, ACCESS_RESTRICTION);
            if (!AccessRestriction.INTERNAL.equals(oldAccessRestriction)) {

                // And if document was FINISHED
                String oldStatus = (String) nodeService.getProperty(docNodeRef, DOC_STATUS);
                if (DocumentStatus.FINISHED.equals(oldStatus)) {
                    getAdrService().addDeletedDocument(docNodeRef);
                }
            }
        }

        boolean isDraft = RepoUtil.getPropertyBooleanValue(docProps, DocumentService.TransientProps.TEMP_DOCUMENT_IS_DRAFT);
        if (isDraft) {
            addPrivilegesBasedOnSeriesOnBackground(docNodeRef);
        }
        { // update properties and log changes made in properties
            final String previousAccessrestriction = (String) nodeService.getProperty(docNodeRef, ACCESS_RESTRICTION);

            // Write document properties to repository
            // XXX If owner is changed to another user, then after this call we don't have permissions any more to write document properties
            // ==================================================================================================================================
            // ==================================================================================================================================
            // XXX If owner is changed to another user, then after previous call we don't have permissions any more to write document properties

            propertyChangesMonitorHelper = new PropertyChangesMonitorHelper();// FIXME:
            propsChanged |= propertyChangesMonitorHelper.setPropertiesIgnoringSystemAndReturnIfChanged(docNodeRef, docProps //
                    , FUNCTION, SERIES, VOLUME, CASE // location changes
                    , REG_NUMBER, SHORT_REG_NUMBER, REG_DATE_TIME // registration changes
                    , ACCESS_RESTRICTION // access restriction changed
                    );
            if (!EventsLoggingHelper.isLoggingDisabled(docNode, DocumentService.TransientProps.TEMP_LOGGING_DISABLED_DOCUMENT_METADATA_CHANGED)) {
                if (isDraft) {
                    documentLogService.addDocumentLog(docNodeRef, MessageUtil.getMessage("document_log_status_created"));
                } else if (propsChanged) {
                    documentLogService.addDocumentLog(docNodeRef, MessageUtil.getMessage("document_log_status_changed"));
                }
                final String newAccessrestriction = (String) docProps.get(ACCESS_RESTRICTION);
                if (!isDraft && !StringUtils.equals(previousAccessrestriction, newAccessrestriction)) {
                    documentLogService.addDocumentLog(docNodeRef, I18NUtil.getMessage("document_log_status_accessRestrictionChanged"));
                }
            }
        }

        if (existingParentNode == null || !targetParentRef.equals(existingParentNode.getNodeRef())) {
            // was not saved (under volume nor case) or saved, but parent (volume or case) must be changed
            Node previousCase = getCaseByDocument(docNodeRef);
            Node previousVolume = getVolumeByDocument(docNodeRef, previousCase);
            try {
                // Moving is executed with System user rights, because this is not appropriate to implement in permissions model
                AuthenticationUtil.runAs(new RunAsWork<NodeRef>() {
                    @Override
                    public NodeRef doWork() throws Exception {
                        updateParentNodesContainingDocsCount(docNodeRef, false);
                        NodeRef newDocNodeRef = nodeService.moveNode(docNodeRef, targetParentRef //
                                , DocumentCommonModel.Assocs.DOCUMENT, DocumentCommonModel.Assocs.DOCUMENT).getChildRef();
                        if (!newDocNodeRef.equals(docNodeRef)) {
                            throw new RuntimeException("NodeRef changed while moving");
                        }
                        updateParentNodesContainingDocsCount(docNodeRef, true);
                        return null;
                    }
                }, AuthenticationUtil.getSystemUserName());
                if (existingParentNode != null && !targetParentRef.equals(existingParentNode.getNodeRef())) {
                    if (isReplyOrFollowupDoc(docNodeRef, null)) {
                        throw new UnableToPerformException(MessageSeverity.ERROR, "document_errorMsg_register_movingNotEnabled_isReplyOrFollowUp");
                    }
                    final boolean isInitialDocWithRepliesOrFollowUps //
                    = nodeService.getSourceAssocs(docNodeRef, DocumentCommonModel.Assocs.DOCUMENT_REPLY).size() > 0 //
                            || nodeService.getSourceAssocs(docNodeRef, DocumentCommonModel.Assocs.DOCUMENT_FOLLOW_UP).size() > 0;
                    if (isInitialDocWithRepliesOrFollowUps) {
                        throw new UnableToPerformException(MessageSeverity.ERROR, "document_errorMsg_register_movingNotEnabled_hasReplyOrFollowUp");
                    }
                    final String existingRegNr = (String) docProps.get(REG_NUMBER.toString());
                    if (StringUtils.isNotBlank(existingRegNr)) {
                        // reg. number is changed if function, series or volume is changed
                        if (!previousVolume.getNodeRef().equals(volumeNodeRef)) {
                            EventsLoggingHelper.disableLogging(docNode, DocumentService.TransientProps.TEMP_LOGGING_DISABLED_DOCUMENT_METADATA_CHANGED);
                            registerDocument(docNode, true);
                            EventsLoggingHelper.enableLogging(docNode, DocumentService.TransientProps.TEMP_LOGGING_DISABLED_DOCUMENT_METADATA_CHANGED);
                        }
                    }
                } else {
                    // Make sure that the node's volume is same as it's followUp's or reply's
                    List<AssociationRef> replies = nodeService.getTargetAssocs(docNodeRef, DocumentCommonModel.Assocs.DOCUMENT_REPLY);
                    List<AssociationRef> followUps = nodeService.getTargetAssocs(docNodeRef, DocumentCommonModel.Assocs.DOCUMENT_FOLLOW_UP);
                    AssociationRef assoc = replies.size() > 0 ? replies.get(0) : followUps.size() > 0 ? followUps.get(0) : null;
                    if (assoc != null) {
                        NodeRef baseRef = assoc.getTargetRef();
                        Node baseCase = getCaseByDocument(baseRef);
                        Node baseVol = getVolumeByDocument(baseRef, baseCase);

                        if (!baseVol.getNodeRef().equals(volumeNodeRef)) {
                            throw new UnableToPerformException(MessageSeverity.ERROR, "document_errorMsg_register_movingNotEnabled_isReplyOrFollowUp");
                        }
                    }
                }
            } catch (UnableToPerformException e) {
                throw e;
            } catch (StaleObjectStateException e) {
                log.error("Failed to move document to volumes folder", e);
                throw new UnableToPerformException(MessageSeverity.ERROR, e.getMessage(), e);// NOT translated - occurs sometimes while debugging
            } catch (RuntimeException e) {
                log.error("Failed to move document to volumes folder", e);
                throw new UnableToPerformException(MessageSeverity.ERROR, "document_errorMsg_register_movingNotEnabled_isReplyOrFollowUp", e);
            }

            if (!isDraft) {
                documentLogService.addDocumentLog(docNodeRef, MessageUtil.getMessage("document_log_location_changed"));
            }
        }
        getMaaisService().addMaaisChangedAspectIfNecessary(docNodeRef, true);
        return getDocument(docNodeRef);
    }

    @Override
    public Pair<Set<String> /* users */, Set<String> /* groups */> getSeriesAuthorities(NodeRef seriesRef) {
        Set<String> users = new HashSet<String>();
        Set<String> groups = new HashSet<String>();
        for (Authority authority : userService.getAuthorities(seriesRef, SeriesDocManagerDynamicAuthority.SERIES_MANAGEABLE_PERMISSION)) {
            if (authority.isGroup()) {
                groups.add(authority.getAuthority());
            } else {
                users.add(authority.getAuthority());
            }
        }
        return new Pair<Set<String>, Set<String>>(users, groups);
    }

    private NodeRef getCaseNodeRef(final Map<String, Object> docProps, final NodeRef volumeNodeRef) {
        NodeRef caseNodeRef = (NodeRef) docProps.get(TransientProps.CASE_NODEREF);
        String caseLabel = (String) docProps.get(TransientProps.CASE_LABEL_EDITABLE);
        if (StringUtils.isBlank(caseLabel)) {
            caseNodeRef = null;
        }
        if (caseNodeRef != null) {
            return caseNodeRef;
        }
        if (StringUtils.isNotBlank(caseLabel)) {
            // find case by casLabel
            List<Case> allCases = getCaseService().getAllCasesByVolume(volumeNodeRef);
            for (Case tmpCase : allCases) {
                if (caseLabel.equalsIgnoreCase(tmpCase.getTitle())) {
                    caseNodeRef = tmpCase.getNode().getNodeRef();
                    break;
                }
            }
            if (caseNodeRef == null) {
                // create case
                Case tmpCase = getCaseService().createCase(volumeNodeRef);
                tmpCase.setTitle(caseLabel);
                getCaseService().saveOrUpdate(tmpCase, false);
                caseNodeRef = tmpCase.getNode().getNodeRef();
            }
        }
        docProps.put(TransientProps.CASE_NODEREF, caseNodeRef);
        return caseNodeRef;
    }

    private boolean saveChildNodes(Node docNode) {
        boolean propsChanged = false;
        QName docNodeType = docNode.getType();
        final boolean isErrandDocAbroad = DocumentSubtypeModel.Types.ERRAND_ORDER_ABROAD.equals(docNodeType);
        final boolean isErrandMvDocAbroad = DocumentSubtypeModel.Types.ERRAND_ORDER_ABROAD_MV.equals(docNodeType);
        final boolean isErrandDocDomestic = DocumentSubtypeModel.Types.ERRAND_APPLICATION_DOMESTIC.equals(docNodeType);
        final boolean isTraining = DocumentSubtypeModel.Types.TRAINING_APPLICATION.equals(docNodeType);
        final boolean isContract = DocumentSubtypeModel.Types.CONTRACT_MV.equals(docNodeType)
                || DocumentSubtypeModel.Types.CONTRACT_SIM.equals(docNodeType) || DocumentSubtypeModel.Types.CONTRACT_SMIT.equals(docNodeType);
        propsChanged |= saveRemovedChildAssocsAndReturnCount(docNode) > 0;
        if (isErrandDocAbroad || isErrandMvDocAbroad || isErrandDocDomestic || isTraining) {
            final QName applicantAssoc;
            final QName errandAssocType;
            if (isErrandDocAbroad) {
                boolean v2 = docNode.hasAspect(DocumentSpecificModel.Aspects.ERRAND_ORDER_ABROAD_V2);
                applicantAssoc = v2 ? DocumentSpecificModel.Assocs.ERRAND_ORDER_APPLICANTS_ABROAD_V2
                        : DocumentSpecificModel.Assocs.ERRAND_ORDER_APPLICANTS_ABROAD;
                errandAssocType = v2 ? DocumentSpecificModel.Assocs.ERRAND_ABROAD_V2 : DocumentSpecificModel.Assocs.ERRAND_ABROAD;
            } else if (isErrandMvDocAbroad) {
                applicantAssoc = DocumentSpecificModel.Assocs.ERRAND_ORDER_ABROAD_MV_APPLICANTS;
                errandAssocType = DocumentSpecificModel.Assocs.ERRAND_ABROAD_MV;
            } else if (isErrandDocDomestic) {
                boolean v2 = docNode.hasAspect(DocumentSpecificModel.Aspects.ERRAND_APPLICATION_DOMESTIC_V2);
                applicantAssoc = v2 ? DocumentSpecificModel.Assocs.ERRAND_APPLICATION_DOMESTIC_APPLICANTS_V2
                        : DocumentSpecificModel.Assocs.ERRAND_APPLICATION_DOMESTIC_APPLICANTS;
                errandAssocType = v2 ? DocumentSpecificModel.Assocs.ERRAND_DOMESTIC_V2 : DocumentSpecificModel.Assocs.ERRAND_DOMESTIC;
            } else if (isTraining) {
                boolean v2 = docNode.hasAspect(DocumentSpecificModel.Aspects.TRAINING_APPLICATION_V2);
                applicantAssoc = v2 ? DocumentSpecificModel.Assocs.TRAINING_APPLICATION_APPLICANTS_V2
                        : DocumentSpecificModel.Assocs.TRAINING_APPLICATION_APPLICANTS;
                errandAssocType = null;
            } else {
                throw new RuntimeException("Unimplemented");
            }

            final List<Node> applicants = docNode.getAllChildAssociations(applicantAssoc);
            if (applicants != null && applicants.size() >= 0) {
                for (int i = 0; i < applicants.size(); i++) {
                    Node applicantNode = applicants.get(i);
                    propsChanged |= saveRemovedChildAssocsAndReturnCount(applicantNode) > 0;
                    Node newApplicantNode = saveChildNode(docNode, applicantNode, applicantAssoc, applicants, i);
                    final List<Node> errandNodes = errandAssocType == null ? null : applicantNode.getAllChildAssociations(errandAssocType);
                    if (newApplicantNode == null) {
                        propsChanged |= propertyChangesMonitorHelper.setPropertiesIgnoringSystemAndReturnIfChanged(applicantNode.getNodeRef(), applicantNode
                                .getProperties());
                    } else {
                        propsChanged = true;
                        applicantNode = newApplicantNode;
                    }
                    if (errandAssocType == null) {
                        continue;
                    }
                    for (int j = 0; j < errandNodes.size(); j++) {
                        Node errandNode = errandNodes.get(j);

                        propsChanged |= saveRemovedChildAssocsAndReturnCount(errandNode) > 0;
                        try {
                            Node newErrandNode = saveChildNode(applicantNode, errandNode, errandAssocType, errandNodes, j);
                            if (newErrandNode == null) {
                                propsChanged |= propertyChangesMonitorHelper.setPropertiesIgnoringSystemAndReturnIfChanged(errandNode.getNodeRef(), errandNode
                                        .getProperties());
                            } else {
                                propsChanged = true;
                            }
                        } catch (AlfrescoRuntimeException e) {
                            final String msg = "failed to set properties for nodeRef=" + errandNode.getNodeRef()
                                    + "; properties: " + errandNode.getProperties();
                            log.error(msg, e);
                            throw e;
                        }
                    }
                }
            }
        }
        if (isContract) {
            QName partyAssoc = (DocumentSubtypeModel.Types.CONTRACT_MV.equals(docNodeType))
                    ? DocumentSpecificModel.Assocs.CONTRACT_MV_PARTIES
                    : DocumentSpecificModel.Assocs.CONTRACT_PARTIES;
            final List<Node> parties = docNode.getAllChildAssociations(partyAssoc);
            if (parties != null && parties.size() >= 0) {
                for (int i = 0; i < parties.size(); i++) {
                    Node partyNode = parties.get(i);
                    propsChanged |= saveRemovedChildAssocsAndReturnCount(partyNode) > 0;
                    Node newPartyNode = saveChildNode(docNode, partyNode, partyAssoc, parties, i);
                    if (newPartyNode == null) {
                        propsChanged |= propertyChangesMonitorHelper.setPropertiesIgnoringSystemAndReturnIfChanged(partyNode.getNodeRef(), partyNode
                                .getProperties());
                    } else {
                        propsChanged = true;
                    }
                }
            }
        }
        return propsChanged;
    }

    private int saveRemovedChildAssocsAndReturnCount(Node applicantNode) {
        return generalService.saveRemovedChildAssocs(applicantNode);
    }

    private void makeChildNodesSearchable(final NodeRef docRef) {
        String childProps = getChildNodesPropsForIndexing(docRef, new StringBuilder()).toString();
        nodeService.setProperty(docRef, SEARCHABLE_SUB_NODE_PROPERTIES, childProps);
    }

    private Node saveChildNode(Node docNode, Node applicantNode, final QName assocTypeAndNameQName, final List<Node> applicants, int i) {
        if (applicantNode instanceof WmNode) {
            WmNode wmNode = (WmNode) applicantNode;
            if (wmNode.isUnsaved()) {
                final Map<QName, Serializable> props = RepoUtil.toQNameProperties(applicantNode.getProperties());
                final ChildAssociationRef applicantNode2 = nodeService.createNode(docNode.getNodeRef(), assocTypeAndNameQName
                        , assocTypeAndNameQName, applicantNode.getType(), props);
                final Node newApplicantNode = generalService.fetchNode(applicantNode2.getChildRef());
                applicants.remove(i);
                applicants.add(i, newApplicantNode);
                return newApplicantNode;
            }
        }
        return null;
    }

    @Override
    public StringBuilder getChildNodesPropsForIndexing(NodeRef parentRef, StringBuilder sb) {
        final List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(parentRef);
        for (ChildAssociationRef childAssocRef : childAssocs) {
            if (DocumentSpecificModel.URI.equals(childAssocRef.getQName().getNamespaceURI())) {
                final NodeRef childRef = childAssocRef.getChildRef();
                combineChildAssocProps(childRef, sb);
                getChildNodesPropsForIndexing(childRef, sb.append("\n"));
            }
        }
        return sb;
    }

    private void combineChildAssocProps(NodeRef nodeRef, StringBuilder sb) {
        final Map<QName, Serializable> nonSysProps = generalService.getPropertiesIgnoringSys(nodeService.getProperties(nodeRef));
        for (Entry<QName, Serializable> entry : nonSysProps.entrySet()) {
            final String propVal;
            final Serializable value = entry.getValue();
            if (value instanceof Date) {
                propVal = userDateFormat.format(value);
            } else if (value instanceof List<?>) {
                final StrBuilder sb2 = new StrBuilder();
                for (Object singleValue : (List<?>) value) {
                    sb2.append(DefaultTypeConverter.INSTANCE.convert(String.class, singleValue) + "\n");
                }
                propVal = sb2.toString();
            } else {
                propVal = DefaultTypeConverter.INSTANCE.convert(String.class, value);
            }
            sb.append(propVal + "\n");
        }
    }

    /**
     * Create copies of childAssociations from originalParentRef that have namespace equal to {@link DocumentSpecificModel#URI} and adds them to copyParentRef.
     */
    private void copyChildAssocs(NodeRef originalParentRef, NodeRef copyParentRef) {
        final List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(originalParentRef);
        for (ChildAssociationRef childAssocRef : childAssocs) {
            if (DocumentSpecificModel.URI.equals(childAssocRef.getQName().getNamespaceURI())) {
                final NodeRef childCopyRef = copyChildAssoc(childAssocRef, copyParentRef);
                copyChildAssocs(childAssocRef.getChildRef(), childCopyRef);
            }
        }
    }

    private NodeRef copyChildAssoc(ChildAssociationRef originalAssocRef, NodeRef parentRef) {
        final NodeRef originalRef = originalAssocRef.getChildRef();
        final Map<QName, Serializable> originalProps = generalService.getPropertiesIgnoringSys(nodeService.getProperties(originalRef));
        final QName nodeTypeQName = nodeService.getType(originalRef);
        return nodeService.createNode(parentRef, originalAssocRef.getTypeQName() //
                , originalAssocRef.getQName(), nodeTypeQName, originalProps).getChildRef();
    }

    @Override
    public void updateSearchableFiles(NodeRef document) {
        updateSearchableFiles(document, null);
    }

    @Override
    public void updateSearchableFiles(NodeRef document, Map<QName, Serializable> props) {
        if (nodeService.hasAspect(document, DocumentCommonModel.Aspects.SEARCHABLE)) {
            if (props == null) {
                props = new HashMap<QName, Serializable>();
            }
            props.put(FILE_NAMES, (Serializable) getSearchableFileNames(document));
            props.put(FILE_CONTENTS, getSearchableFileContents(document));
            nodeService.addProperties(document, props);
        }
    }

    @Override
    public List<String> getSearchableFileNames(NodeRef document) {
        List<FileInfo> files = fileFolderService.listFiles(document);
        List<String> fileNames = new ArrayList<String>(files.size());
        for (FileInfo file : files) {
            fileNames.add(file.getName());
        }
        return fileNames;
    }

    @Override
    public ContentData getSearchableFileContents(NodeRef document) {
        List<FileInfo> files = fileFolderService.listFiles(document);
        if (files.size() == 0) {
            return null;
        }
        ContentWriter allWriter = contentService.getWriter(document, FILE_CONTENTS, false);
        allWriter.setMimetype(MimetypeMap.MIMETYPE_TEXT_PLAIN);
        allWriter.setEncoding("UTF-8");
        OutputStream allOutput = allWriter.getContentOutputStream();

        for (FileInfo file : files) {
            if (log.isTraceEnabled()) {
                log.trace("Transforming fileName '" + file.getName() + "'");
            }
            ContentReader reader = fileFolderService.getReader(file.getNodeRef());
            if (reader != null && reader.exists()) {
                boolean readerReady = true;
                if (!EqualsHelper.nullSafeEquals(reader.getMimetype(), MimetypeMap.MIMETYPE_TEXT_PLAIN)
                        || !EqualsHelper.nullSafeEquals(reader.getEncoding(), "UTF-8")) {
                    ContentTransformer transformer = contentService.getTransformer(reader.getMimetype(), MimetypeMap.MIMETYPE_TEXT_PLAIN);
                    if (transformer == null) {
                        log.debug("No transformer found for " + reader.getMimetype());
                        continue;
                    }
                    ContentWriter writer = contentService.getTempWriter();
                    writer.setMimetype(MimetypeMap.MIMETYPE_TEXT_PLAIN);
                    writer.setEncoding("UTF-8");
                    try {
                        transformer.transform(reader, writer);
                        reader = writer.getReader();
                        if (!reader.exists()) {
                            if (log.isDebugEnabled()) {
                                log.debug("Transformation did not write any content, fileName '" + file.getName() + "', " + file.getNodeRef());
                            }
                            readerReady = false;
                        }
                    } catch (ContentIOException e) {
                        if (log.isDebugEnabled()) {
                            log.debug("Transformation failed, fileName '" + file.getName() + "', " + file.getNodeRef(), e);
                        }
                        readerReady = false;
                    }
                }
                if (readerReady) {
                    InputStream input = reader.getContentInputStream();
                    try {
                        IOUtils.copy(input, allOutput);
                        input.close();
                        allOutput.write('\n');
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        try {
            allOutput.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return allWriter.getContentData();
    }

    @Override
    public Node createFollowUp(QName followupType, NodeRef nodeRef) {
        Node followUpDoc = createDocument(followupType);
        Node baseDoc = getDocument(nodeRef);

        Map<String, Object> followupProps = followUpDoc.getProperties();
        Map<String, Object> docProps = baseDoc.getProperties();

        /** All types share common properties */
        Set<String> copiedProps = new HashSet<String>(DocumentPropertySets.commonProperties);

        /** Substitute and choose properties */
        setDocumentSpecificProperties(followupType, baseDoc, followupProps, docProps, copiedProps);

        copyChildNodes(baseDoc, followUpDoc);

        /** Copy common Properties */
        for (Map.Entry<String, Object> prop : docProps.entrySet()) {
            if (copiedProps.contains(prop.getKey())) {
                followupProps.put(prop.getKey(), prop.getValue());
            }
        }

        /** Copy Ancestors (function, series, volume, case) */
        setTransientProperties(followUpDoc, getAncestorNodesByDocument(baseDoc.getNodeRef()));

        addFollowupAssoc(followUpDoc.getNodeRef(), baseDoc.getNodeRef());

        if (log.isDebugEnabled()) {
            log.debug("Created followUp: " + followupType.getLocalName() + " from " + baseDoc.getType().getLocalName());
        }
        return followUpDoc;
    }

    private void copyChildNodes(Node baseDoc, Node followUpDoc) {
        if (DocumentSubtypeModel.Types.ERRAND_APPLICATION_DOMESTIC.equals(baseDoc.getType())) {
            // Remove empty child node
            List<Node> emptyApplicants = followUpDoc.getAllChildAssociations(DocumentSpecificModel.Assocs.ERRAND_APPLICATION_DOMESTIC_APPLICANTS_V2);
            followUpDoc.removeChildAssociations(DocumentSpecificModel.Assocs.ERRAND_APPLICATION_DOMESTIC_APPLICANTS_V2, emptyApplicants);

            // V2 -> V2
            if (baseDoc.hasAspect(DocumentSpecificModel.Aspects.ERRAND_APPLICATION_DOMESTIC_V2)) {
                Map<String, List<Node>> childAssocsByAssocType = baseDoc.getAllChildAssociationsByAssocType();
                copyChildAssocs(childAssocsByAssocType, followUpDoc, DocumentSpecificModel.Assocs.ERRAND_DOMESTIC_V2);
                copyChildAssocs(childAssocsByAssocType, followUpDoc, DocumentSpecificModel.Assocs.ERRAND_APPLICATION_DOMESTIC_APPLICANTS_V2);
            }

            // V1 -> V2
            else if (baseDoc.hasAspect(DocumentSpecificModel.Aspects.ERRAND_APPLICATION_DOMESTIC)) {
                // Add applicants
                for (Node applicant : baseDoc.getAllChildAssociations(DocumentSpecificModel.Assocs.ERRAND_APPLICATION_DOMESTIC_APPLICANTS)) {
                    Map<QName, Serializable> typeProperties = RepoUtil.copyTypeProperties(
                            generalService.getAnonymousType(DocumentSpecificModel.Types.ERRAND_APPLICATION_DOMESTIC_APPLICANT_TYPE_V2).getProperties(),
                            applicant);
                    NodeRef applicantRef = nodeService.createNode(followUpDoc.getNodeRef(),
                            DocumentSpecificModel.Assocs.ERRAND_APPLICATION_DOMESTIC_APPLICANTS_V2,
                            DocumentSpecificModel.Assocs.ERRAND_APPLICATION_DOMESTIC_APPLICANTS_V2,
                            DocumentSpecificModel.Types.ERRAND_APPLICATION_DOMESTIC_APPLICANT_TYPE_V2, typeProperties).getChildRef();
                    Node applicantNode = new Node(applicantRef);
                    followUpDoc.addChildAssociations(DocumentSpecificModel.Assocs.ERRAND_APPLICATION_DOMESTIC_APPLICANTS_V2, applicantNode);

                    // Add errands
                    List<Node> errands = applicant.getAllChildAssociations(DocumentSpecificModel.Assocs.ERRAND_DOMESTIC);
                    for (Node errand : errands) {
                        typeProperties = RepoUtil.copyTypeProperties(generalService.getAnonymousType(DocumentSpecificModel.Types.ERRAND_ABROAD_TYPE_V2)
                                .getProperties(), errand);
                        NodeRef errandRef = nodeService.createNode(applicantRef, DocumentSpecificModel.Assocs.ERRAND_DOMESTIC_V2,
                                DocumentSpecificModel.Assocs.ERRAND_DOMESTIC_V2, DocumentSpecificModel.Types.ERRANDS_DOMESTIC_TYPE_V2, typeProperties)
                                .getChildRef();
                        applicantNode.addChildAssociations(DocumentSpecificModel.Assocs.ERRAND_APPLICATION_DOMESTIC_APPLICANTS_V2, new Node(errandRef));
                    }
                }
            }
        }

        if (DocumentSubtypeModel.Types.ERRAND_ORDER_ABROAD.equals(baseDoc.getType())) {
            // Remove empty child node
            List<Node> emptyApplicants = followUpDoc.getAllChildAssociations(DocumentSpecificModel.Assocs.ERRAND_ORDER_APPLICANTS_ABROAD_V2);
            followUpDoc.removeChildAssociations(DocumentSpecificModel.Assocs.ERRAND_ORDER_APPLICANTS_ABROAD_V2, emptyApplicants);

            // V2 -> V2
            if (baseDoc.hasAspect(DocumentSpecificModel.Aspects.ERRAND_ORDER_ABROAD_V2)) {
                Map<String, List<Node>> childAssocsByAssocType = baseDoc.getAllChildAssociationsByAssocType();
                copyChildAssocs(childAssocsByAssocType, followUpDoc, DocumentSpecificModel.Assocs.ERRAND_ABROAD_V2);
                copyChildAssocs(childAssocsByAssocType, followUpDoc, DocumentSpecificModel.Assocs.ERRAND_ORDER_APPLICANTS_ABROAD_V2);
            }

            // V1 -> V2
            else if (baseDoc.hasAspect(DocumentSpecificModel.Aspects.ERRAND_ORDER_ABROAD)) {
                // Add applicants
                for (Node applicant : baseDoc.getAllChildAssociations(DocumentSpecificModel.Assocs.ERRAND_ORDER_APPLICANTS_ABROAD)) {
                    Map<QName, Serializable> typeProperties = RepoUtil.copyTypeProperties(
                            generalService.getAnonymousType(DocumentSpecificModel.Types.ERRAND_ORDER_APPLICANT_ABROAD_V2).getProperties(), applicant);
                    NodeRef applicantRef = nodeService.createNode(followUpDoc.getNodeRef(),
                            DocumentSpecificModel.Assocs.ERRAND_ORDER_APPLICANTS_ABROAD_V2,
                            DocumentSpecificModel.Assocs.ERRAND_ORDER_APPLICANTS_ABROAD_V2,
                            DocumentSpecificModel.Types.ERRAND_ORDER_APPLICANT_ABROAD_V2, typeProperties).getChildRef();
                    Node applicantNode = new Node(applicantRef);
                    followUpDoc.addChildAssociations(DocumentSpecificModel.Assocs.ERRAND_ORDER_APPLICANTS_ABROAD_V2, applicantNode);

                    // Add errands
                    List<Node> errands = applicant.getAllChildAssociations(DocumentSpecificModel.Assocs.ERRAND_ABROAD);
                    for (Node errand : errands) {
                        typeProperties = RepoUtil.copyTypeProperties(generalService.getAnonymousType(DocumentSpecificModel.Types.ERRAND_ABROAD_TYPE_V2)
                                .getProperties(), errand);
                        NodeRef errandRef = nodeService.createNode(applicantRef, DocumentSpecificModel.Assocs.ERRAND_ABROAD_V2,
                                DocumentSpecificModel.Assocs.ERRAND_ABROAD_V2, DocumentSpecificModel.Types.ERRAND_ABROAD_TYPE_V2, typeProperties)
                                .getChildRef();
                        applicantNode.addChildAssociations(DocumentSpecificModel.Assocs.ERRAND_ORDER_APPLICANTS_ABROAD_V2, new Node(errandRef));
                    }
                }
            }
        }

        if (DocumentSubtypeModel.Types.TRAINING_APPLICATION.equals(baseDoc.getType())) {
            // Remove empty child node
            List<Node> emptyApplicants = followUpDoc.getAllChildAssociations(DocumentSpecificModel.Assocs.TRAINING_APPLICATION_APPLICANTS_V2);
            followUpDoc.removeChildAssociations(DocumentSpecificModel.Assocs.TRAINING_APPLICATION_APPLICANTS_V2, emptyApplicants);

            // V2 -> V2
            if (baseDoc.hasAspect(DocumentSpecificModel.Aspects.TRAINING_APPLICATION_V2)) {
                Map<String, List<Node>> childAssocsByAssocType = baseDoc.getAllChildAssociationsByAssocType();
                copyChildAssocs(childAssocsByAssocType, followUpDoc, DocumentSpecificModel.Assocs.TRAINING_APPLICATION_APPLICANTS_V2);
            }

            // V1 -> V2
            else if (baseDoc.hasAspect(DocumentSpecificModel.Aspects.TRAINING_APPLICATION)) {
                // Add applicants
                for (Node applicant : baseDoc.getAllChildAssociations(DocumentSpecificModel.Assocs.TRAINING_APPLICATION_APPLICANTS)) {
                    Map<QName, Serializable> typeProperties = RepoUtil.copyTypeProperties(
                            generalService.getAnonymousType(DocumentSpecificModel.Types.TRAINING_APPLICATION_APPLICANT_TYPE_V2).getProperties(), applicant);
                    NodeRef applicantRef = nodeService.createNode(followUpDoc.getNodeRef(),
                            DocumentSpecificModel.Assocs.TRAINING_APPLICATION_APPLICANTS_V2,
                            DocumentSpecificModel.Assocs.TRAINING_APPLICATION_APPLICANTS_V2,
                            DocumentSpecificModel.Types.TRAINING_APPLICATION_APPLICANT_TYPE_V2, typeProperties).getChildRef();
                    Node applicantNode = new Node(applicantRef);
                    followUpDoc.addChildAssociations(DocumentSpecificModel.Assocs.TRAINING_APPLICATION_APPLICANTS_V2, applicantNode);
                }
            }
        }

        if ((DocumentSubtypeModel.Types.CONTRACT_SIM.equals(baseDoc.getType()) && DocumentSubtypeModel.Types.CONTRACT_SIM.equals(followUpDoc.getType()))
                || (DocumentSubtypeModel.Types.CONTRACT_SMIT.equals(baseDoc.getType()) && DocumentSubtypeModel.Types.CONTRACT_SMIT
                        .equals(followUpDoc.getType()))) {
            List<Node> emptyParty = followUpDoc.getAllChildAssociations(DocumentSpecificModel.Assocs.CONTRACT_PARTIES);
            followUpDoc.removeChildAssociations(DocumentSpecificModel.Assocs.CONTRACT_PARTIES, emptyParty);

            if (baseDoc.hasAspect(DocumentSpecificModel.Aspects.CONTRACT_DETAILS_V1)) {
                Map<String, Object> properties = baseDoc.getProperties();
                if (properties.containsKey(DocumentSpecificModel.Props.SECOND_PARTY_NAME.toString())
                        && StringUtils.isNotBlank((String) properties.get(DocumentSpecificModel.Props.SECOND_PARTY_NAME.toString()))) {
                    Map<QName, Serializable> partyProps = new HashMap<QName, Serializable>(4);
                    partyProps.put(DocumentSpecificModel.Props.PARTY_NAME, (Serializable) properties.get(DocumentSpecificModel.Props.SECOND_PARTY_NAME));
                    partyProps.put(DocumentSpecificModel.Props.PARTY_EMAIL, (Serializable) properties.get(DocumentSpecificModel.Props.SECOND_PARTY_EMAIL));
                    partyProps.put(DocumentSpecificModel.Props.PARTY_SIGNER, (Serializable) properties.get(DocumentSpecificModel.Props.SECOND_PARTY_SIGNER));
                    partyProps.put(DocumentSpecificModel.Props.PARTY_CONTACT_PERSON,
                            (Serializable) properties.get(DocumentSpecificModel.Props.SECOND_PARTY_CONTACT_PERSON));

                    NodeRef partyRef = nodeService.createNode(followUpDoc.getNodeRef(), DocumentSpecificModel.Assocs.CONTRACT_PARTIES,
                            DocumentSpecificModel.Assocs.CONTRACT_PARTIES, DocumentSpecificModel.Types.CONTRACT_PARTY_TYPE, partyProps).getChildRef();
                    followUpDoc.addChildAssociations(DocumentSpecificModel.Assocs.CONTRACT_PARTIES, new Node(partyRef));
                }

                if (properties.containsKey(DocumentSpecificModel.Props.THIRD_PARTY_NAME.toString())
                        && StringUtils.isNotBlank((String) properties.get(DocumentSpecificModel.Props.THIRD_PARTY_NAME.toString()))) {
                    Map<QName, Serializable> partyProps = new HashMap<QName, Serializable>(4);
                    partyProps.put(DocumentSpecificModel.Props.PARTY_NAME, (Serializable) properties.get(DocumentSpecificModel.Props.THIRD_PARTY_NAME));
                    partyProps.put(DocumentSpecificModel.Props.PARTY_EMAIL, (Serializable) properties.get(DocumentSpecificModel.Props.THIRD_PARTY_EMAIL));
                    partyProps.put(DocumentSpecificModel.Props.PARTY_SIGNER, (Serializable) properties.get(DocumentSpecificModel.Props.THIRD_PARTY_SIGNER));
                    partyProps.put(DocumentSpecificModel.Props.PARTY_CONTACT_PERSON,
                            (Serializable) properties.get(DocumentSpecificModel.Props.THIRD_PARTY_CONTACT_PERSON));

                    NodeRef partyRef = nodeService.createNode(followUpDoc.getNodeRef(), DocumentSpecificModel.Assocs.CONTRACT_PARTIES,
                            DocumentSpecificModel.Assocs.CONTRACT_PARTIES, DocumentSpecificModel.Types.CONTRACT_PARTY_TYPE, partyProps).getChildRef();
                    followUpDoc.addChildAssociations(DocumentSpecificModel.Assocs.CONTRACT_PARTIES, new Node(partyRef));
                }
            } else if (baseDoc.hasAspect(DocumentSpecificModel.Aspects.CONTRACT_DETAILS_V2)) {
                Map<String, List<Node>> childAssocsByAssocType = baseDoc.getAllChildAssociationsByAssocType();
                copyChildAssocs(childAssocsByAssocType, followUpDoc, DocumentSpecificModel.Assocs.CONTRACT_PARTIES);
            }

        }

    }

    private void copyChildAssocs(Map<String, List<Node>> childAssocsByAssocType, Node target, QName assocTypeQName) {
        List<Node> errandsDomestic = childAssocsByAssocType.get(assocTypeQName.toString());
        if (errandsDomestic != null) {
            for (Node errandDomesticNode : errandsDomestic) {
                NodeRef copyRef = copyService.copyAndRename(errandDomesticNode.getNodeRef(), target.getNodeRef(), assocTypeQName, assocTypeQName, true);
                target.addChildAssociations(assocTypeQName, new Node(copyRef));
            }
        }
    }

    private void setDocumentSpecificProperties(QName followupType, Node doc, Map<String, Object> followUpProps, Map<String, Object> initProps,
            Set<String> propsToCopy) {
        QName baseDocType = doc.getType();
        if (DocumentTypeHelper.isInstrumentOfDeliveryAndReciept(followupType)
                && (DocumentSubtypeModel.Types.CONTRACT_SIM.equals(baseDocType) || DocumentSubtypeModel.Types.CONTRACT_MV.equals(baseDocType))) {
            followUpProps.put(DocumentSpecificModel.Props.SECOND_PARTY_REG_NUMBER.toString(), initProps.get(
                    DocumentSpecificModel.Props.SECOND_PARTY_CONTRACT_NUMBER));
            followUpProps.put(DocumentSpecificModel.Props.SECOND_PARTY_REG_DATE.toString(), initProps.get(
                    DocumentSpecificModel.Props.SECOND_PARTY_CONTRACT_DATE));
        }
        if (DocumentSubtypeModel.Types.INSTRUMENT_OF_DELIVERY_AND_RECEIPT_MV.equals(followupType)
                && DocumentSubtypeModel.Types.INSTRUMENT_OF_DELIVERY_AND_RECEIPT_MV.equals(baseDocType)) {
            followUpProps.put(DocumentSpecificModel.Props.DELIVERER_NAME.toString(), initProps.get(DocumentSpecificModel.Props.DELIVERER_NAME.toString()));
            followUpProps.put(DocumentSpecificModel.Props.DELIVERER_JOB_TITLE.toString(),
                    initProps.get(DocumentSpecificModel.Props.DELIVERER_JOB_TITLE.toString()));
            followUpProps.put(DocumentSpecificModel.Props.DELIVERER_STRUCT_UNIT.toString(),
                    initProps.get(DocumentSpecificModel.Props.DELIVERER_STRUCT_UNIT.toString()));
            followUpProps.put(DocumentSpecificModel.Props.RECEIVER_NAME.toString(), initProps.get(DocumentSpecificModel.Props.RECEIVER_NAME.toString()));
            followUpProps.put(DocumentSpecificModel.Props.RECEIVER_JOB_TITLE.toString(),
                    initProps.get(DocumentSpecificModel.Props.RECEIVER_JOB_TITLE.toString()));
            followUpProps.put(DocumentSpecificModel.Props.RECEIVER_STRUCT_UNIT.toString(),
                    initProps.get(DocumentSpecificModel.Props.RECEIVER_STRUCT_UNIT.toString()));

        }
        if (DocumentSubtypeModel.Types.INCOMING_LETTER.equals(baseDocType)) { // only INCOMING_LETTER not INCOMING_LETTER_*
            propsToCopy.addAll(Arrays.asList(
                    DocumentSpecificModel.Props.SENDER_REG_NUMBER.toString()
                    , DocumentSpecificModel.Props.SENDER_REG_DATE.toString()
                    , DocumentSpecificModel.Props.SENDER_DETAILS_NAME.toString()
                    , DocumentSpecificModel.Props.SENDER_DETAILS_EMAIL.toString()
                    , DocumentSpecificModel.Props.DUE_DATE.toString()
                    , DocumentSpecificModel.Props.COMPLIENCE_NOTATION.toString()
                    , DocumentSpecificModel.Props.COMPLIENCE_DATE.toString()
                    , DocumentCommonModel.Props.COMMENT.toString()
                    ));
            userService.setOwnerPropsFromUser(followUpProps);
            if (DocumentSubtypeModel.Types.INCOMING_LETTER.equals(followupType)) {
                propsToCopy.add(DocumentSpecificModel.Props.TRANSMITTAL_MODE.toString());
            }
        }
        if (DocumentTypeHelper.isOutgoingLetter(baseDocType)) {
            userService.setOwnerPropsFromUser(followUpProps);
        }
        if (DocumentSubtypeModel.Types.OUTGOING_LETTER.equals(baseDocType)) { // only OUTGOING_LETTER not OUTGOING_LETTER_*
            propsToCopy.addAll(Arrays.asList(
                    DocumentSpecificModel.Props.SENDER_REG_NUMBER.toString()
                    , DocumentSpecificModel.Props.SENDER_REG_DATE.toString()
                    ));
            if (DocumentSubtypeModel.Types.OUTGOING_LETTER.equals(followupType)) { // only OUTGOING_LETTER not OUTGOING_LETTER_*
                propsToCopy.add(DocumentCommonModel.Props.RECIPIENT_NAME.toString());
                propsToCopy.add(DocumentCommonModel.Props.RECIPIENT_EMAIL.toString());
                propsToCopy.add(DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_NAME.toString());
                propsToCopy.add(DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_EMAIL.toString());
                propsToCopy.add(DocumentCommonModel.Props.SIGNER_NAME.toString());
                propsToCopy.add(DocumentCommonModel.Props.SIGNER_JOB_TITLE.toString());
            }
        }
        if (DocumentSubtypeModel.Types.TENDERING_APPLICATION.equals(baseDocType) && DocumentSubtypeModel.Types.TENDERING_APPLICATION.equals(followupType)) {
            propsToCopy.addAll(Arrays.asList(
                    DocumentSpecificModel.Props.PROCUREMENT_TYPE.toString()
                    , DocumentSpecificModel.Props.PROCUREMENT_LEGAL_BASIS.toString()
                    , DocumentSpecificModel.Props.PROCUREMENT_DESC.toString()
                    , DocumentSpecificModel.Props.PROCUREMENT_SUM_ESTIMATED.toString()
                    , DocumentSpecificModel.Props.PROCUREMENT_BUDGET_CLASSIFICATION.toString()
                    , DocumentSpecificModel.Props.PROCUREMENT_OBJECT_CLASS_CODE.toString()
                    , DocumentSpecificModel.Props.PROCUREMENT_TENDER_DATA.toString()
                    , DocumentSpecificModel.Props.PROCUREMENT_CONTRACT_DATE_ESTIMATED.toString()
                    , DocumentSpecificModel.Props.PROCUREMENT_OFFICIAL_RESPONSIBLE.toString()
                    , DocumentSpecificModel.Props.LINKED_TO_EU_PROJECT.toString()
                    , DocumentSpecificModel.Props.EU_PROJECT_DESC.toString()
                    , DocumentSpecificModel.Props.STRUCTURAL_AID_ID_OUTSIDE_PROJECT.toString()
                    , DocumentSpecificModel.Props.OBJECT_TECHNICAL_DESC.toString()
                    , DocumentSpecificModel.Props.EVALUATION_CRITERIA.toString()
                    , DocumentSpecificModel.Props.OFFERING_END_DATE.toString()
                    , DocumentSpecificModel.Props.QUALIFICATION_TERMS_FOR_TENDERS.toString()
                    , DocumentSpecificModel.Props.CONTRACT_BEGIN_DATE.toString()
                    , DocumentSpecificModel.Props.PROCUREMENT_NUMBER.toString()
                    , DocumentSpecificModel.Props.PROCUREMENT_APPLICANT_NAME.toString()
                    , DocumentSpecificModel.Props.PROCUREMENT_APPLICANT_JOB_TITLE.toString()
                    , DocumentSpecificModel.Props.PROCUREMENT_APPLICANT_ORG_STRUCT_UNIT.toString()
                    ));
        }
        if (DocumentSubtypeModel.Types.INTERNAL_APPLICATION.equals(baseDocType)) {
            propsToCopy.add(DocumentCommonModel.Props.SIGNER_NAME.toString());
            propsToCopy.add(DocumentCommonModel.Props.SIGNER_JOB_TITLE.toString());
        }
        if (DocumentSubtypeModel.Types.REPORT.equals(baseDocType)) {
            propsToCopy.add(DocumentSpecificModel.Props.RAPPORTEUR_NAME.toString());
        }
        if (DocumentSubtypeModel.Types.ERRAND_ORDER_ABROAD.equals(baseDocType)) {
            propsToCopy.add(DocumentSpecificModel.Props.LEGAL_BASIS_FOR_OFFICIALS.toString());
            propsToCopy.add(DocumentSpecificModel.Props.LEGAL_BASIS_FOR_SUPPORT_STAFF.toString());
        }
        if (DocumentSubtypeModel.Types.TRAINING_APPLICATION.equals(baseDocType)) {
            propsToCopy.addAll(Arrays.asList(
                    DocumentSpecificModel.Props.TRAINING_NAME.toString(),
                    DocumentSpecificModel.Props.TRAINING_ORGANIZER.toString(),
                    DocumentSpecificModel.Props.TRAINING_NEED.toString(),
                    DocumentSpecificModel.Props.TRAINING_BEGIN_DATE.toString(),
                    DocumentSpecificModel.Props.TRAINING_END_DATE.toString(),
                    DocumentSpecificModel.Props.TRAINING_HOURS.toString(),
                    DocumentSpecificModel.Props.TRAINING_LOCATION.toString()
                    ));
        }

        if (DocumentSubtypeModel.Types.CONTRACT_SIM.equals(baseDocType) && DocumentSubtypeModel.Types.INSTRUMENT_OF_DELIVERY_AND_RECEIPT.equals(followupType)) {
            followUpProps.put(DocumentSpecificModel.Props.DELIVERER_NAME.toString(), initProps.get(DocumentSpecificModel.Props.FIRST_PARTY_NAME));
            if (doc.hasAspect(DocumentSpecificModel.Aspects.CONTRACT_DETAILS_V1)) {
                followUpProps.put(DocumentSpecificModel.Props.RECEIVER_NAME.toString(), initProps.get(DocumentSpecificModel.Props.SECOND_PARTY_NAME));
            } else if (doc.hasAspect(DocumentSpecificModel.Aspects.CONTRACT_DETAILS_V2)) {
                List<Node> parties = doc.getAllChildAssociations(DocumentSpecificModel.Assocs.CONTRACT_PARTIES);
                if (parties != null && !parties.isEmpty()) {
                    followUpProps.put(DocumentSpecificModel.Props.RECEIVER_NAME.toString(),
                            parties.get(0).getProperties().get(DocumentSpecificModel.Props.PARTY_NAME));
                }
            }
        }

        if (DocumentSubtypeModel.Types.CONTRACT_SMIT.equals(baseDocType) && DocumentSubtypeModel.Types.INSTRUMENT_OF_DELIVERY_AND_RECEIPT.equals(followupType)) {
            followUpProps.put(DocumentSpecificModel.Props.RECEIVER_NAME.toString(), initProps.get(DocumentSpecificModel.Props.FIRST_PARTY_NAME));
            if (doc.hasAspect(DocumentSpecificModel.Aspects.CONTRACT_DETAILS_V1)) {
                followUpProps.put(DocumentSpecificModel.Props.DELIVERER_NAME.toString(), initProps.get(DocumentSpecificModel.Props.SECOND_PARTY_NAME));
            } else if (doc.hasAspect(DocumentSpecificModel.Aspects.CONTRACT_DETAILS_V2)) {
                List<Node> parties = doc.getAllChildAssociations(DocumentSpecificModel.Assocs.CONTRACT_PARTIES);
                if (parties != null && !parties.isEmpty()) {
                    followUpProps.put(DocumentSpecificModel.Props.DELIVERER_NAME.toString(),
                            parties.get(0).getProperties().get(DocumentSpecificModel.Props.PARTY_NAME));
                }
            }
        }

        if (DocumentSubtypeModel.Types.CONTRACT_SMIT.equals(baseDocType) && DocumentSubtypeModel.Types.CONTRACT_SMIT.equals(followupType)) {
            propsToCopy.addAll(Arrays.asList(
                     DocumentCommonModel.Props.SIGNER_NAME.toString()
                    , DocumentSpecificModel.Props.FIRST_PARTY_CONTACT_PERSON.toString()
                    , DocumentSpecificModel.Props.INCLUSIVE_PRICE_INCL_VAT.toString()
                    , DocumentSpecificModel.Props.COST_MANAGER.toString()
                    , DocumentSpecificModel.Props.FINANCING_SOURCE.toString()
                    , DocumentSpecificModel.Props.CONTRACT_SMIT_END_DATE.toString()
                    , DocumentSpecificModel.Props.CONTRACT_SMIT_END_DATE_DESC.toString()
                    , DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_NAME.toString()
                    , DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_EMAIL.toString()
                    ));
        }

        if (DocumentSubtypeModel.Types.CONTRACT_SIM.equals(baseDocType) && DocumentSubtypeModel.Types.CONTRACT_SIM.equals(followupType)) {
            propsToCopy.addAll(Arrays.asList(
                    DocumentCommonModel.Props.SIGNER_NAME.toString()
                    , DocumentSpecificModel.Props.FIRST_PARTY_CONTACT_PERSON.toString()
                    , DocumentSpecificModel.Props.INCLUSIVE_PRICE_EXCL_VAT.toString()
                    , DocumentSpecificModel.Props.COST_MANAGER.toString()
                    , DocumentSpecificModel.Props.FINANCING_SOURCE.toString()
                    , DocumentSpecificModel.Props.CONTRACT_SIM_END_DATE.toString()
                    , DocumentSpecificModel.Props.CONTRACT_SIM_END_DATE_DESC.toString()
                    , DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_NAME.toString()
                    , DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_EMAIL.toString()
                    ));
        }

        if ((DocumentSubtypeModel.Types.CONTRACT_SMIT.equals(baseDocType) || DocumentSubtypeModel.Types.CONTRACT_SIM.equals(baseDocType))
                && DocumentSubtypeModel.Types.REPORT.equals(followupType)) {
            if (doc.hasAspect(DocumentSpecificModel.Aspects.CONTRACT_DETAILS_V1)) {
                followUpProps.put(DocumentSpecificModel.Props.RAPPORTEUR_NAME.toString(), initProps.get(DocumentSpecificModel.Props.SECOND_PARTY_NAME));
            } else if (doc.hasAspect(DocumentSpecificModel.Aspects.CONTRACT_DETAILS_V2)) {
                List<Node> parties = doc.getAllChildAssociations(DocumentSpecificModel.Assocs.CONTRACT_PARTIES);
                if (parties != null && !parties.isEmpty()) {
                    followUpProps.put(DocumentSpecificModel.Props.RAPPORTEUR_NAME.toString(),
                            parties.get(0).getProperties().get(DocumentSpecificModel.Props.PARTY_NAME));
                }
            }
        }
    }

    /** Add association from new to original doc */
    protected void addFollowupAssoc(NodeRef followUpDocRef, NodeRef initialDocRef) {
        createAssoc(followUpDocRef, initialDocRef, DocumentCommonModel.Assocs.DOCUMENT_FOLLOW_UP);
    }

    protected void addReplyAssoc(NodeRef replyDocRef, NodeRef initialDocRef) {
        createAssoc(replyDocRef, initialDocRef, DocumentCommonModel.Assocs.DOCUMENT_REPLY);
    }

    @Override
    public Node createReply(QName docType, NodeRef nodeRef) {
        return createReplyDocumentFromExisting(docType, nodeRef);
    }

    private Node createReplyDocumentFromExisting(QName docType, NodeRef nodeRef) {
        Node replyDoc = createDocument(docType);
        Node doc = getDocument(nodeRef);

        Map<String, Object> props = replyDoc.getProperties();
        Map<String, Object> docProps = doc.getProperties();
        Set<String> copiedProps = null;

        /** Substitute and choose properties */
        if (DocumentTypeHelper.isOutgoingLetter(docType)) {
            @SuppressWarnings("unchecked")
            List<String> recipientNames = (List<String>) props.get(RECIPIENT_NAME);
            if (recipientNames.size() > 0) {
                recipientNames.remove(0);
            }
            recipientNames.add((String) docProps.get(DocumentSpecificModel.Props.SENDER_DETAILS_NAME.toString()));
            @SuppressWarnings("unchecked")
            List<String> recipientEmails = (List<String>) props.get(RECIPIENT_EMAIL);
            if (recipientEmails.size() > 0) {
                recipientEmails.remove(0);
            }
            recipientEmails.add((String) docProps.get(DocumentSpecificModel.Props.SENDER_DETAILS_EMAIL.toString()));
            copiedProps = DocumentPropertySets.incomingAndOutgoingLetterProperties;
        } else if (DocumentTypeHelper.isInstrumentOfDeliveryAndReciept(docType)) {
            props.put(DocumentSpecificModel.Props.SECOND_PARTY_REG_NUMBER.toString(), docProps.get(
                    DocumentSpecificModel.Props.SECOND_PARTY_CONTRACT_NUMBER));
            props.put(DocumentSpecificModel.Props.SECOND_PARTY_REG_DATE.toString(), docProps.get(
                    DocumentSpecificModel.Props.SECOND_PARTY_CONTRACT_DATE));
            copiedProps = DocumentPropertySets.commonProperties;
        } else {
            throw new RuntimeException("Unexpected docType: " + docType);
        }

        /** Copy Properties */
        for (Map.Entry<String, Object> prop : docProps.entrySet()) {
            if (copiedProps.contains(prop.getKey())) {
                props.put(prop.getKey(), prop.getValue());
            }
        }

        /** Copy Ancestors (function, series, volume, case) */
        setTransientProperties(replyDoc, getAncestorNodesByDocument(doc.getNodeRef()));

        /** Add association from new to original doc */
        addReplyAssoc(replyDoc.getNodeRef(), doc.getNodeRef());

        if (log.isDebugEnabled()) {
            log.debug("Created reply: " + docType.getLocalName() + " from " + doc.getType().getLocalName());
        }
        return replyDoc;
    }

    @Override
    public Node copyDocument(NodeRef nodeRef) {
        Node doc = getDocument(nodeRef);
        PropertiesModifierCallback callback = null;
        Map<QName, Serializable> properties = null;

        // Handle ContractV1 -> ContractV2 copy
        if (doc.getAspects().contains(DocumentSpecificModel.Aspects.CONTRACT_DETAILS_V1)) {
            callback = creationPropertiesModifierCallbacks.get(DocumentSpecificModel.Aspects.CONTRACT_DETAILS_V1);

            Map<String, Object> props = new HashMap<String, Object>(DocumentPropertySets.contractDetailsV1.size());
            for (Map.Entry<String, Object> prop : doc.getProperties().entrySet()) {
                if (DocumentPropertySets.contractDetailsV1.contains(prop.getKey())) {
                    props.put(prop.getKey(), prop.getValue());
                }
            }
            properties = RepoUtil.toQNameProperties(props, true);
        } else if (doc.getAspects().contains(DocumentSpecificModel.Aspects.VACATION_ORDER)) {
            Map<String, Object> props = doc.getProperties();
            List<String> leaveTypes = new ArrayList<String>(4);
            List<Date> leaveBeginDates = new ArrayList<Date>(4);
            List<Date> leaveEndDates = new ArrayList<Date>(4);
            List<Integer> leaveDays = new ArrayList<Integer>(4);

            if (BooleanUtils.isTrue((Boolean) props.get(DocumentSpecificModel.Props.LEAVE_ANNUAL))) {
                leaveTypes.add(LeaveType.LEAVE_ANNUAL.getValueName());
                fillLeaveDates(props, leaveBeginDates, leaveEndDates, leaveDays,
                        DocumentSpecificModel.Props.LEAVE_ANNUAL_BEGIN_DATE,
                        DocumentSpecificModel.Props.LEAVE_ANNUAL_END_DATE);
            }
            if (BooleanUtils.isTrue((Boolean) props.get(DocumentSpecificModel.Props.LEAVE_WITHOUT_PAY))) {
                leaveTypes.add(LeaveType.LEAVE_WITHOUT_PAY.getValueName());
                fillLeaveDates(props, leaveBeginDates, leaveEndDates, leaveDays,
                        DocumentSpecificModel.Props.LEAVE_WITHOUT_PAY_BEGIN_DATE,
                        DocumentSpecificModel.Props.LEAVE_WITHOUT_PAY_END_DATE);
            }
            if (BooleanUtils.isTrue((Boolean) props.get(DocumentSpecificModel.Props.LEAVE_CHILD))) {
                leaveTypes.add(LeaveType.LEAVE_CHILD.getValueName());
                fillLeaveDates(props, leaveBeginDates, leaveEndDates, leaveDays,
                        DocumentSpecificModel.Props.LEAVE_CHILD_BEGIN_DATE,
                        DocumentSpecificModel.Props.LEAVE_CHILD_END_DATE);
            }
            if (BooleanUtils.isTrue((Boolean) props.get(DocumentSpecificModel.Props.LEAVE_STUDY))) {
                leaveTypes.add(LeaveType.LEAVE_STUDY.getValueName());
                fillLeaveDates(props, leaveBeginDates, leaveEndDates, leaveDays,
                        DocumentSpecificModel.Props.LEAVE_STUDY_BEGIN_DATE,
                        DocumentSpecificModel.Props.LEAVE_STUDY_END_DATE);
            }

            properties = new HashMap<QName, Serializable>();
            properties.put(DocumentSpecificModel.Props.LEAVE_TYPE, (Serializable) leaveTypes);
            properties.put(DocumentSpecificModel.Props.LEAVE_BEGIN_DATES, (Serializable) leaveBeginDates);
            properties.put(DocumentSpecificModel.Props.LEAVE_END_DATES, (Serializable) leaveEndDates);
            properties.put(DocumentSpecificModel.Props.LEAVE_DAYS, (Serializable) leaveDays);

            if (BooleanUtils.isTrue((Boolean) props.get(DocumentSpecificModel.Props.LEAVE_CHANGE))) {
                properties.put(DocumentSpecificModel.Props.LEAVE_INITIAL_BEGIN_DATES,
                        (Serializable) Arrays.asList(props.get(DocumentSpecificModel.Props.LEAVE_INITIAL_BEGIN_DATE)));
                properties.put(DocumentSpecificModel.Props.LEAVE_INITIAL_END_DATES,
                        (Serializable) Arrays.asList(props.get(DocumentSpecificModel.Props.LEAVE_INITIAL_END_DATE)));
                Date beginDate = (Date) props.get(DocumentSpecificModel.Props.LEAVE_NEW_BEGIN_DATE);
                properties.put(DocumentSpecificModel.Props.LEAVE_NEW_BEGIN_DATES,
                        (Serializable) Arrays.asList(beginDate));
                Date endDate = (Date) props.get(DocumentSpecificModel.Props.LEAVE_NEW_END_DATE);
                properties.put(DocumentSpecificModel.Props.LEAVE_NEW_END_DATES, (Serializable) Arrays.asList(endDate));
                if (beginDate != null && endDate != null) {
                    properties.put(DocumentSpecificModel.Props.LEAVE_CHANGE_DAYS,
                            Days.daysBetween(new Instant(beginDate.getTime()), new Instant(endDate.getTime())).getDays() + 1);
                }
            }

            if (BooleanUtils.isTrue((Boolean) props.get(DocumentSpecificModel.Props.LEAVE_CANCEL))) {
                Date beginDate = (Date) props.get(DocumentSpecificModel.Props.LEAVE_CANCEL_BEGIN_DATE);
                properties.put(DocumentSpecificModel.Props.LEAVE_CANCEL_BEGIN_DATES,
                        (Serializable) Arrays.asList(beginDate));
                Date endDate = (Date) props.get(DocumentSpecificModel.Props.LEAVE_CANCEL_END_DATE);
                properties.put(DocumentSpecificModel.Props.LEAVE_CANCEL_END_DATES,
                        (Serializable) Arrays.asList(endDate));
                if (beginDate != null && endDate != null) {
                    properties.put(DocumentSpecificModel.Props.LEAVE_CANCELLED_DAYS,
                            Days.daysBetween(new Instant(beginDate.getTime()), new Instant(endDate.getTime())).getDays() + 1);
                }
            }
        }

        // create document without calling propertiesModifierCallbacks
        Node copiedDoc = createDocument(doc.getType(), null, properties, true, callback);
        // PROPERTIES
        for (Map.Entry<String, Object> prop : doc.getProperties().entrySet()) {
            if (!DocumentPropertySets.ignoredPropertiesWhenMakingCopy.contains(prop.getKey())) {
                copiedDoc.getProperties().put(prop.getKey(), prop.getValue());
            }
        }

        // CHILD ASSOCIATIONS (RECURSIVELY)
        copyChildAssocs(nodeRef, copiedDoc.getNodeRef());

        // ANCESTORS
        setTransientProperties(copiedDoc, getAncestorNodesByDocument(doc.getNodeRef()));
        // DEFAULT VALUES
        copiedDoc.getProperties().put(DOC_STATUS.toString(), DocumentStatus.WORKING.getValueName());

        if (log.isDebugEnabled()) {
            log.debug("Copied document: " + copiedDoc.toString());
        }
        return copiedDoc;
    }

    private void fillLeaveDates(Map<String, Object> props, List<Date> leaveBeginDates, List<Date> leaveEndDates, List<Integer> leaveDays, QName beginQName,
            QName endQName) {
        Date beginDate = (Date) props.get(beginQName);
        leaveBeginDates.add(beginDate);
        Date endDate = (Date) props.get(endQName);
        leaveEndDates.add(endDate);
        if (beginDate != null && endDate != null) {
            leaveDays.add(Days.daysBetween(new Instant(beginDate.getTime()), new Instant(endDate.getTime())).getDays() + 1);
        }
    }

    @Override
    public void setTransientProperties(Node document, DocumentParentNodesVO documentParentNodesVO) {
        Node functionNode = documentParentNodesVO.getFunctionNode();
        Node seriesNode = documentParentNodesVO.getSeriesNode();
        Node volumeNode = documentParentNodesVO.getVolumeNode();
        Node caseNode = documentParentNodesVO.getCaseNode();

        // put props with empty values if missing, otherwise use existing values
        final Map<String, Object> props = document.getProperties();
        props.put(TransientProps.FUNCTION_NODEREF, functionNode != null ? functionNode.getNodeRef() : null);
        props.put(TransientProps.SERIES_NODEREF, seriesNode != null ? seriesNode.getNodeRef() : null);
        props.put(TransientProps.VOLUME_NODEREF, volumeNode != null ? volumeNode.getNodeRef() : null);
        props.put(TransientProps.CASE_NODEREF, caseNode != null ? caseNode.getNodeRef() : null);

        // add labels
        String caseLbl = caseNode != null ? caseNode.getProperties().get(CaseModel.Props.TITLE).toString() : null;
        String volumeLbl = volumeNode != null ? volumeNode.getProperties().get(VolumeModel.Props.MARK).toString() //
                + " " + volumeNode.getProperties().get(VolumeModel.Props.TITLE).toString() : null;
        String seriesLbl = seriesNode != null ? seriesNode.getProperties().get(SeriesModel.Props.SERIES_IDENTIFIER).toString() //
                + " " + seriesNode.getProperties().get(SeriesModel.Props.TITLE).toString() : null;
        String functionLbl = functionNode != null ? functionNode.getProperties().get(FunctionsModel.Props.MARK).toString() //
                + " " + functionNode.getProperties().get(FunctionsModel.Props.TITLE).toString() : null;
        props.put(TransientProps.FUNCTION_LABEL, functionLbl);
        props.put(TransientProps.SERIES_LABEL, seriesLbl);
        props.put(TransientProps.VOLUME_LABEL, volumeLbl);
        props.put(TransientProps.CASE_LABEL, caseLbl);
        props.put(TransientProps.CASE_LABEL_EDITABLE, caseLbl);
    }

    @Override
    public void deleteDocument(NodeRef nodeRef) {
        log.debug("Deleting document: " + nodeRef);
        getAdrService().addDeletedDocument(nodeRef);
        List<AssociationRef> assocs = nodeService.getTargetAssocs(nodeRef, RegexQNamePattern.MATCH_ALL);
        assocs.addAll(nodeService.getSourceAssocs(nodeRef, RegexQNamePattern.MATCH_ALL));
        for (AssociationRef assoc : assocs) {
            nodeService.removeAssociation(assoc.getSourceRef(), assoc.getTargetRef(), assoc.getTypeQName());
            updateModifiedDateTime(assoc.getSourceRef(), assoc.getTargetRef());
        }
        updateParentNodesContainingDocsCount(nodeRef, false);
        nodeService.deleteNode(nodeRef);
    }

    @Override
    public List<Document> getAllDocumentsByVolume(NodeRef volumeRef) {
        return getAllDocumentsByParentNodeRef(volumeRef);
    }

    @Override
    public List<Document> getAllDocumentsByCase(NodeRef caseRef) {
        return getAllDocumentsByParentNodeRef(caseRef);
    }

    @Override
    public List<Document> getAllDocumentFromDvk() {
        List<Document> documents = getAllDocumentsByParentNodeRef(generalService.getNodeRef(fromDvkXPath));
        Collections.sort(documents);
        return documents;
    }

    @Override
    public int getAllDocumentFromDvkCount() {
        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(
                generalService.getNodeRef(fromDvkXPath), RegexQNamePattern.MATCH_ALL, RegexQNamePattern.MATCH_ALL);
        return childAssocs != null ? childAssocs.size() : 0;
    }

    @Override
    public int getAllDocumentFromIncomingInvoiceCount() {
        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(
                generalService.getNodeRef(receivedInvoicePath), RegexQNamePattern.MATCH_ALL, RegexQNamePattern.MATCH_ALL);
        return childAssocs != null ? childAssocs.size() : 0;
    }

    @Override
    public int getUserDocumentFromIncomingInvoiceCount(String username) {
        List<Document> allIncomingEinvoices = getIncomingEInvoices();
        int userIncomingEInvoices = 0;
        for (Document document : allIncomingEinvoices) {
            if (username.equalsIgnoreCase(document.getOwnerId())) {
                userIncomingEInvoices++;
            }
        }
        return userIncomingEInvoices;
    }

    @Override
    public List<Document> getReplyOrFollowUpDocuments(NodeRef base) {
        List<Document> docs = new ArrayList<Document>();
        // reply and follow up are source associations regarding the base document
        final List<AssociationRef> sourceAssocs = nodeService.getSourceAssocs(base, RegexQNamePattern.MATCH_ALL);
        for (AssociationRef srcAssocRef : sourceAssocs) {
            if (DocumentCommonModel.Assocs.DOCUMENT_FOLLOW_UP.equals(srcAssocRef.getTypeQName()) ||
                    DocumentCommonModel.Assocs.DOCUMENT_REPLY.equals(srcAssocRef.getTypeQName())) {
                docs.add(getDocumentByNodeRef(srcAssocRef.getSourceRef()));
            }
        }
        return docs;
    }

    @Override
    public List<DocAssocInfo> getAssocInfos(Node document) {
        final ArrayList<DocAssocInfo> assocInfos = new ArrayList<DocAssocInfo>();
        final List<AssociationRef> targetAssocs = nodeService.getTargetAssocs(document.getNodeRef(), RegexQNamePattern.MATCH_ALL);
        for (AssociationRef targetAssocRef : targetAssocs) {
            log.debug("targetAssocRef=" + targetAssocRef.getTypeQName());
            addDocAssocInfo(targetAssocRef, false, assocInfos);
        }
        final List<AssociationRef> sourceAssocs = nodeService.getSourceAssocs(document.getNodeRef(), RegexQNamePattern.MATCH_ALL);
        for (AssociationRef srcAssocRef : sourceAssocs) {
            log.debug("srcAssocRef=" + srcAssocRef.getTypeQName());
            addDocAssocInfo(srcAssocRef, true, assocInfos);
        }
        final Map<String, Map<String, AssociationRef>> addedAssocs = document.getAddedAssociations();
        for (Map<String, AssociationRef> typedAssoc : addedAssocs.values()) {
            for (AssociationRef addedAssoc : typedAssoc.values()) {
                log.debug("addedAssoc=" + addedAssoc.getTypeQName());
                addDocAssocInfo(addedAssoc, false, assocInfos);
            }
        }
        return assocInfos;
    }

    /*
     * NOTE: association with case is defined differently
     */
    @Override
    public void deleteAssoc(final NodeRef sourceNodeRef, final NodeRef targetNodeRef, QName assocQName) {
        if (assocQName == null) {
            assocQName = DocumentCommonModel.Assocs.DOCUMENT_2_DOCUMENT;
        }
        log.debug("Deleting " + assocQName + " association from document " + sourceNodeRef + " that points to " + targetNodeRef);
        if (assocQName.equals(CaseModel.Associations.CASE_DOCUMENT)) {
            QName srcType = nodeService.getType(targetNodeRef);
            if (srcType.equals(MaaisModel.Types.MAAIS_CASE)) {
                assocQName = MaaisModel.Associations.MAAIS_CASE_DOCUMENT;
                getMaaisService().addMaaisChangedAspectIfNecessary(sourceNodeRef, false);
            }
            nodeService.removeAssociation(targetNodeRef, sourceNodeRef, assocQName);
        } else {
            nodeService.removeAssociation(sourceNodeRef, targetNodeRef, assocQName);
        }
        updateModifiedDateTime(sourceNodeRef, targetNodeRef);
    }

    @Override
    public void createAssoc(final NodeRef sourceNodeRef, final NodeRef targetNodeRef, QName assocQName) {
        nodeService.createAssociation(sourceNodeRef, targetNodeRef, assocQName);
        if (assocQName.equals(MaaisModel.Associations.MAAIS_CASE_DOCUMENT)) {
            getMaaisService().addMaaisChangedAspectIfNecessary(targetNodeRef, false);
        }
        updateModifiedDateTime(sourceNodeRef, targetNodeRef);
    }

    /*
     * If associations between two documents are added/deleted, then update modified time of both documents.
     * Because we need ADR to detect changes based on modified time.
     */
    private void updateModifiedDateTime(final NodeRef firstDocNodeRef, final NodeRef secondDocNodeRef) {
        if (dictionaryService.isSubClass(nodeService.getType(firstDocNodeRef), DocumentCommonModel.Types.DOCUMENT)
                && dictionaryService.isSubClass(nodeService.getType(secondDocNodeRef), DocumentCommonModel.Types.DOCUMENT)) {
            AuthenticationUtil.runAs(new RunAsWork<Void>() {
                @Override
                public Void doWork() throws Exception {
                    nodeService.setProperty(firstDocNodeRef, ContentModel.PROP_MODIFIED, null);
                    nodeService.setProperty(secondDocNodeRef, ContentModel.PROP_MODIFIED, null);
                    return null;
                }
            }, AuthenticationUtil.getSystemUserName());
        }
    }

    @Override
    public List<Document> getIncomingEmails() {
        NodeRef incomingNodeRef = generalService.getNodeRef(incomingEmailPath);
        return getIncomingDocuments(incomingNodeRef);
    }

    @Override
    public List<Document> getIncomingEInvoices() {
        NodeRef incomingNodeRef = generalService.getNodeRef(receivedInvoicePath);
        return getIncomingDocuments(incomingNodeRef);
    }

    @Override
    public List<Document> getIncomingEInvoicesForUser(String username) {
        List<Document> allIncomingEinvoices = getIncomingEInvoices();
        List<Document> userIncomingEinvoices = new ArrayList<Document>(allIncomingEinvoices.size());
        for (Document document : allIncomingEinvoices) {
            if (username.equalsIgnoreCase(document.getOwnerId())) {
                userIncomingEinvoices.add(document);
            }
        }
        return userIncomingEinvoices;
    }

    private List<Document> getIncomingDocuments(NodeRef incomingNodeRef) {
        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(incomingNodeRef);
        List<Document> docs = new ArrayList<Document>(childAssocs.size());
        for (ChildAssociationRef assocRef : childAssocs) {
            docs.add(0, getDocumentByNodeRef(assocRef.getChildRef())); // flips the list, so newest are first
        }
        return docs;
    }

    @Override
    public int getIncomingEmailsCount() {
        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(generalService.getNodeRef(incomingEmailPath));
        return childAssocs != null ? childAssocs.size() : 0;
    }

    @Override
    public List<Document> getSentEmails() {
        NodeRef sentNodeRef = generalService.getNodeRef(sentEmailPath);
        return getIncomingDocuments(sentNodeRef);
    }

    @Override
    public int getSentEmailsCount() {
        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(generalService.getNodeRef(sentEmailPath));
        return childAssocs != null ? childAssocs.size() : 0;
    }

    @Override
    public Document getDocumentByNodeRef(NodeRef docRef) {
        Document doc = new Document(docRef);
        if (log.isDebugEnabled()) {
            log.debug("Document: " + doc);
        }
        return doc;
    }

    @Override
    public void addPropertiesModifierCallback(QName qName, PropertiesModifierCallback propertiesModifierCallback) {
        creationPropertiesModifierCallbacks.put(qName, propertiesModifierCallback);
    }

    @Override
    public Map<QName, NodeRef> getDocumentParents(NodeRef documentRef) {
        Map<QName, NodeRef> parents = new HashMap<QName, NodeRef>();
        NodeRef caseRef = generalService.getParentNodeRefWithType(documentRef, CaseModel.Types.CASE);
        parents.put(DocumentCommonModel.Props.CASE, caseRef);
        NodeRef docOrCaseRef;
        if (caseRef != null) {
            docOrCaseRef = caseRef;
        } else {
            docOrCaseRef = documentRef;
        }
        NodeRef volumeRef = generalService.getParentNodeRefWithType(docOrCaseRef, VolumeModel.Types.VOLUME);
        parents.put(DocumentCommonModel.Props.VOLUME, volumeRef);
        NodeRef seriesRef = null;
        if (volumeRef != null) {
            seriesRef = generalService.getParentNodeRefWithType(volumeRef, SeriesModel.Types.SERIES);
        }
        parents.put(DocumentCommonModel.Props.SERIES, seriesRef);
        NodeRef functionRef = null;
        if (seriesRef != null) {
            functionRef = generalService.getParentNodeRefWithType(seriesRef, FunctionsModel.Types.FUNCTION);
        }
        parents.put(DocumentCommonModel.Props.FUNCTION, functionRef);
        return parents;
    }

    @Override
    public Node getVolumeByDocument(NodeRef nodeRef) {
        return getVolumeByDocument(nodeRef, null);
    }

    @Override
    public Node getCaseByDocument(NodeRef nodeRef) {
        return generalService.getParentWithType(nodeRef, CaseModel.Types.CASE);
    }

    @Override
    public DocumentParentNodesVO getAncestorNodesByDocument(NodeRef docRef) {
        final Node caseRef = getCaseByDocument(docRef);
        Node volumeNode = getVolumeByDocument(docRef, caseRef);
        Node seriesNode = volumeNode != null ? getSeriesByVolume(volumeNode.getNodeRef()) : null;
        Node functionNode = seriesNode != null ? getFunctionBySeries(seriesNode.getNodeRef()) : null;
        return new DocumentParentNodesVO(functionNode, seriesNode, volumeNode, caseRef);
    }

    private Node getFunctionBySeries(NodeRef seriesRef) {
        return seriesRef == null ? null : generalService.getParentWithType(seriesRef, FunctionsModel.Types.FUNCTION);
    }

    private Node getSeriesByVolume(NodeRef volumeRef) {
        return volumeRef == null ? null : generalService.getParentWithType(volumeRef, SeriesModel.Types.SERIES);
    }

    private Node getVolumeByDocument(NodeRef docRef, Node caseNode) {
        final NodeRef docOrCaseRef;
        if (caseNode != null) {
            docOrCaseRef = caseNode.getNodeRef();
        } else {
            docOrCaseRef = docRef;
        }
        return generalService.getParentWithType(docOrCaseRef, VolumeModel.Types.VOLUME);
    }

    @Override
    public boolean isDocumentOwner(NodeRef document, String user) {
        return StringUtils.equals(getDocumentOwner(document), user);
    }

    private String getDocumentOwner(NodeRef document) {
        return (String) nodeService.getProperty(document, OWNER_ID);
    }

    @Override
    public void setDocumentOwner(NodeRef document, String userName) {
        if (!dictionaryService.isSubClass(nodeService.getType(document), DocumentCommonModel.Types.DOCUMENT)) {
            throw new RuntimeException("Node is not a document: " + document);
        }
        if (isDocumentOwner(document, userName)) {
            if (log.isDebugEnabled()) {
                log.debug("Document owner is already set to " + userName + ", not overwriting properties");
            }
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug("Setting document owner from " + getDocumentOwner(document) + " to " + userName + " - " + document);
        }
        Map<QName, Serializable> personProps = userService.getUserProperties(userName);
        Map<QName, Serializable> properties = new HashMap<QName, Serializable>();
        // same logic as OwnerPropertiesModifierCallback#doWithProperties
        userService.setOwnerPropsFromUser(properties, personProps);
        generalService.setPropertiesIgnoringSystem(properties, document);
    }

    @Override
    public boolean isSaved(NodeRef nodeRef) {
        final Node parentVolume = getVolumeByDocument(nodeRef);
        return parentVolume != null ? true : null != generalService.getParentWithType(nodeRef, CaseModel.Types.CASE);
    }

    @Override
    public boolean isIncomingInvoice(NodeRef nodeRef) {
        NodeRef receivedInvoicePathRef = generalService.getNodeRef(receivedInvoicePath);
        return receivedInvoicePathRef.equals(nodeService.getPrimaryParent(nodeRef).getParentRef());
    }

    @Override
    public boolean isFromDVK(NodeRef nodeRef) {
        NodeRef dvkNodeRef = generalService.getNodeRef(fromDvkXPath);
        return dvkNodeRef.equals(nodeService.getPrimaryParent(nodeRef).getParentRef());
    }

    @Override
    public boolean isFromIncoming(NodeRef nodeRef) {
        NodeRef incomingNodeRef = generalService.getNodeRef(ImapModel.Repo.INCOMING_SPACE);
        return incomingNodeRef.equals(nodeService.getPrimaryParent(nodeRef).getParentRef());
    }

    @Override
    public boolean isFromSent(NodeRef nodeRef) {
        NodeRef sentNodeRef = generalService.getNodeRef(ImapModel.Repo.SENT_SPACE);
        return sentNodeRef.equals(nodeService.getPrimaryParent(nodeRef).getParentRef());
    }

    @Override
    public boolean isRegistered(Node docNode) {
        final String existingRegNr = (String) docNode.getProperties().get(REG_NUMBER.toString());
        return StringUtils.isNotBlank(existingRegNr);
    }

    public boolean isRegistered(NodeRef docRef) {
        final String existingRegNr = (String) nodeService.getProperty(docRef, REG_NUMBER);
        return StringUtils.isNotBlank(existingRegNr);
    }

    @Override
    public void registerDocumentIfNotRegistered(NodeRef document, boolean triggeredAutomatically) {
        Node docNode = getDocument(document);
        if (triggeredAutomatically) {
            EventsLoggingHelper.disableLogging(docNode, TEMP_LOGGING_DISABLED_REGISTERED_BY_USER);
        }
        if (!isRegistered(docNode)) {
            registerDocument(docNode);
        }
        if (triggeredAutomatically) {
            EventsLoggingHelper.enableLogging(docNode, TEMP_LOGGING_DISABLED_REGISTERED_BY_USER);
        }
    }

    @Override
    public Node registerDocument(Node docNode) {
        return registerDocument(docNode, false);
    }

    private Node registerDocument(Node docNode, boolean isRelocating) {
        final Map<String, Object> props = docNode.getProperties();
        if (isRegistered(docNode) && !isRelocating) {
            throw new RuntimeException("Document already registered! docNode=" + docNode);
        }
        // only register when no existingRegNr or when relocating
        final NodeRef volumeNodeRef = (NodeRef) props.get(TransientProps.VOLUME_NODEREF);
        final NodeRef seriesNodeRef = (NodeRef) props.get(TransientProps.SERIES_NODEREF);
        final NodeRef caseNodeRef = (NodeRef) props.get(TransientProps.CASE_NODEREF);
        final NodeRef docRef = docNode.getNodeRef();
        final String volumeMark = volumeService.getVolumeByNodeRef(volumeNodeRef).getVolumeMark();
        final DocumentType documentType = documentTypeService.getDocumentType(docNode.getType());
        final List<AssociationRef> replyAssocs = nodeService.getTargetAssocs(docRef, DocumentCommonModel.Assocs.DOCUMENT_REPLY);
        final boolean isReplyOrFollowupDoc = isReplyOrFollowupDoc(docRef, replyAssocs);
        String regNumber = null;
        final Date now = new Date();
        if (!isReplyOrFollowupDoc) {
            log.debug("Starting to register initialDocument, docRef=" + docRef);
            // registration of initial document ("Algatusdokument")
            final Series series = seriesService.getSeriesByNodeRef(seriesNodeRef);
            final Map<String, Object> serProps = series.getNode().getProperties();
            Integer registerId = (Integer) serProps.get(SeriesModel.Props.REGISTER.toString());
            boolean individualizingNumbers = (Boolean) serProps.get(SeriesModel.Props.INDIVIDUALIZING_NUMBERS.toString());

            registerService.increaseCount(registerId); // increase before geting the register
            Register register = registerService.getRegister(registerId);
            // compose regNumber
            regNumber = volumeMark + VOLUME_MARK_SEPARATOR + register.getPrefix() + (register.getCounter()) + register.getSuffix();
            if (individualizingNumbers) {
                regNumber += REGISTRATION_INDIVIDUALIZING_NUM_SUFFIX;
            }
        } else { // registration of reply/followUp("Järg- või vastusdokument")
            log.debug("Starting to register " + (replyAssocs.size() > 0 ? "reply" : "followUp") + " document, docRef=" + docRef);
            final Node initialDoc = getDocument(getInitialDocument(docRef));
            final Map<String, Object> initDocProps = initialDoc.getProperties();
            final String initDocRegNr = (String) initDocProps.get(REG_NUMBER.toString());
            if (StringUtils.isNotBlank(initDocRegNr)) {
                final NodeRef initDocSeriesNodeRef = (NodeRef) initDocProps.get(TransientProps.SERIES_NODEREF.toString());

                final Series series = seriesService.getSeriesByNodeRef(initDocSeriesNodeRef);
                final Map<String, Object> serProps = series.getNode().getProperties();
                boolean individualizingNumbers = (Boolean) serProps.get(SeriesModel.Props.INDIVIDUALIZING_NUMBERS.toString());
                if (!individualizingNumbers) {
                    regNumber = initDocRegNr;
                } else { // add also individualizing number to regNr
                    final RegNrHolder initDocRegNrHolder = new RegNrHolder(initDocRegNr);
                    if (initDocRegNrHolder.getIndividualizingNr() != null) {
                        final NodeRef initDocParentRef = caseNodeRef != null ? caseNodeRef : volumeNodeRef;
                        int maxIndivNr = initDocRegNrHolder.getIndividualizingNr();
                        for (Document anotherDoc : getAllDocumentsByParentNodeRef(initDocParentRef)) {
                            if (!docRef.equals(anotherDoc.getNodeRef())) {
                                final RegNrHolder anotherDocRegNrHolder = new RegNrHolder(anotherDoc.getRegNumber());
                                if (StringUtils.equals(initDocRegNrHolder.getRegNrWithoutIndividualizingNr() //
                                        , anotherDocRegNrHolder.getRegNrWithoutIndividualizingNr())) {
                                    final Integer anotherDocIndivNr = anotherDocRegNrHolder.getIndividualizingNr();
                                    if (anotherDocIndivNr != null) {
                                        maxIndivNr = Math.max(maxIndivNr, anotherDocIndivNr);
                                    }
                                }
                            }
                        }
                        regNumber = initDocRegNrHolder.getRegNrWithoutIndividualizingNr() + (maxIndivNr + 1);
                    } else {
                        // with correct data and *current* expected user behaviors this code should not be reached,
                        // however Maiga insisted that this behavior would be applied if smth. goes wrong
                        regNumber = initDocRegNr;
                    }
                }
                if (StringUtils.isNotBlank(regNumber) && (documentType.getId().equals(DocumentSubtypeModel.Types.INSTRUMENT_OF_DELIVERY_AND_RECEIPT)
                        || documentType.getId().equals(DocumentSubtypeModel.Types.INSTRUMENT_OF_DELIVERY_AND_RECEIPT_MV))) {
                    if (replyAssocs.size() > 0) {
                        final NodeRef contractDocRef = replyAssocs.get(0).getTargetRef();
                        Date finalTermOfDeliveryAndReceiptDate = (Date) nodeService.getProperty(contractDocRef,
                                DocumentSpecificModel.Props.FINAL_TERM_OF_DELIVERY_AND_RECEIPT);
                        if (finalTermOfDeliveryAndReceiptDate == null) {
                            setPropertyAsSystemUser(DocumentSpecificModel.Props.FINAL_TERM_OF_DELIVERY_AND_RECEIPT, now, contractDocRef);
                        }
                    }
                }
                // Check if its a reply outgoing letter and update originating document info (if needed)
                if (StringUtils.isNotBlank(regNumber) && (documentType.getId().equals(DocumentSubtypeModel.Types.OUTGOING_LETTER)
                        || documentType.getId().equals(DocumentSubtypeModel.Types.OUTGOING_LETTER_MV))) {
                    if (replyAssocs.size() > 0) {
                        final NodeRef originalDocRef = replyAssocs.get(0).getTargetRef();
                        if (nodeService.hasAspect(originalDocRef, DocumentSpecificModel.Aspects.COMPLIENCE)) {
                            Date complienceDate = (Date) nodeService.getProperty(originalDocRef, DocumentSpecificModel.Props.COMPLIENCE_DATE);
                            if (complienceDate == null) {
                                setPropertyAsSystemUser(DocumentSpecificModel.Props.COMPLIENCE_DATE, now, originalDocRef);
                                setDocStatusFinished(originalDocRef);
                            }
                        }
                        DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
                        String comment = I18NUtil.getMessage("task_comment_finished_by_register_doc", regNumber, dateFormat.format(now));
                        workflowService.finishCompoundWorkflowsOnRegisterDoc(originalDocRef, comment);
                    }
                }
                // Check if its a reply outgoing mv letter and update originating document info (if needed)
                if (StringUtils.isNotBlank(regNumber) && documentType.getId().equals(DocumentSubtypeModel.Types.INCOMING_LETTER_MV)) {
                    if (replyAssocs.size() > 0) {
                        final NodeRef originalDocRef = replyAssocs.get(0).getTargetRef();
                        if (nodeService.hasAspect(originalDocRef, DocumentSpecificModel.Aspects.OUTGOING_LETTER_MV)) {
                            Date replyDate = (Date) nodeService.getProperty(originalDocRef, DocumentSpecificModel.Props.REPLY_DATE);
                            if (replyDate == null) {
                                AuthenticationUtil.runAs(new RunAsWork<NodeRef>() {
                                    @Override
                                    public NodeRef doWork() throws Exception {
                                        nodeService.setProperty(originalDocRef, DocumentSpecificModel.Props.REPLY_DATE, now);
                                        return null;
                                    }
                                }, AuthenticationUtil.getSystemUserName());
                            }
                        }
                        String docStatus = (String) nodeService.getProperty(originalDocRef, DOC_STATUS);
                        if (!DocumentStatus.FINISHED.getValueName().equals(docStatus)) {
                            AuthenticationUtil.runAs(new RunAsWork<NodeRef>() {
                                @Override
                                public NodeRef doWork() throws Exception {
                                    nodeService.setProperty(originalDocRef, DOC_STATUS, DocumentStatus.FINISHED.getValueName());
                                    return null;
                                }
                            }, AuthenticationUtil.getSystemUserName());
                            documentLogService.addDocumentLog(originalDocRef, I18NUtil.getMessage("document_log_status_proceedingFinish") //
                                    , I18NUtil.getMessage("document_log_creator_dhs"));
                        }
                    }

                }
            }
        }
        if (StringUtils.isNotBlank(regNumber)) {
            String oldRegNumber = (String) nodeService.getProperty(docNode.getNodeRef(), REG_NUMBER);
            boolean adrDeletedDocumentAdded = false;
            if (oldRegNumber != null && !StringUtils.equals(oldRegNumber, regNumber)) {
                getAdrService().addDeletedDocument(docNode.getNodeRef());
                adrDeletedDocumentAdded = true;
            }

            props.put(REG_NUMBER.toString(), regNumber);
            props.put(SHORT_REG_NUMBER.toString(), StringUtils.substringAfter(regNumber, VOLUME_MARK_SEPARATOR));
            propertyChangesMonitorHelper.addIgnoredProps(props, REG_NUMBER);
            if (!isRelocating) {
                Date oldRegDateTime = (Date) nodeService.getProperty(docNode.getNodeRef(), REG_DATE_TIME);
                if (oldRegDateTime != null && !adrDeletedDocumentAdded) {
                    getAdrService().addDeletedDocument(docNode.getNodeRef());
                }

                props.put(REG_DATE_TIME.toString(), now);
                propertyChangesMonitorHelper.addIgnoredProps(props, REG_DATE_TIME);
            }

            if (!documentType.getId().equals(DocumentSubtypeModel.Types.INCOMING_LETTER)
                    && !documentType.getId().equals(DocumentSubtypeModel.Types.INCOMING_LETTER_MV)
                    && !documentType.getId().equals(DocumentSubtypeModel.Types.INVOICE)) {
                props.put(DOC_STATUS.toString(), DocumentStatus.FINISHED.getValueName());
                propertyChangesMonitorHelper.addIgnoredProps(props, DOC_STATUS);
                documentLogService.addDocumentLog(docRef, I18NUtil.getMessage("document_log_status_registered"));
            } else {
                if (EventsLoggingHelper.isLoggingDisabled(docNode, TEMP_LOGGING_DISABLED_REGISTERED_BY_USER)) {
                    documentLogService.addDocumentLog(docRef, I18NUtil.getMessage("document_log_status_registered") //
                            , I18NUtil.getMessage("document_log_creator_dhs"));
                } else {
                    documentLogService.addDocumentLog(docRef, I18NUtil.getMessage("document_log_status_registered"));
                }
            }
            return updateDocument(docNode);
        }
        throw new UnableToPerformException(MessageSeverity.INFO, "document_errorMsg_register_initialDocNotRegistered");
    }

    @Override
    public void setDocStatusFinished(final NodeRef originalDocRef) {
        String docStatus = (String) nodeService.getProperty(originalDocRef, DOC_STATUS);
        if (!DocumentStatus.FINISHED.getValueName().equals(docStatus)) {
            setPropertyAsSystemUser(DOC_STATUS, DocumentStatus.FINISHED.getValueName(), originalDocRef);
            documentLogService.addDocumentLog(originalDocRef, I18NUtil.getMessage("document_log_status_proceedingFinish") //
                    , I18NUtil.getMessage("document_log_creator_dhs"));
        }
    }

    @Override
    public void setPropertyAsSystemUser(final QName propName, final Serializable value, final NodeRef docRef) {
        AuthenticationUtil.runAs(new RunAsWork<NodeRef>() {
            @Override
            public NodeRef doWork() throws Exception {
                nodeService.setProperty(docRef, propName, value);
                return null;
            }
        }, AuthenticationUtil.getSystemUserName());
    }

    public void addDocAssocInfo(AssociationRef assocRef, boolean isSourceAssoc, ArrayList<DocAssocInfo> assocInfos) {
        DocAssocInfo assocInf = getDocAssocInfo(assocRef, isSourceAssoc);
        if (assocInf != null) {
            assocInfos.add(assocInf);
        }
    }

    @Override
    public DocAssocInfo getDocAssocInfo(AssociationRef assocRef, boolean isSourceAssoc) {
        DocAssocInfo assocInf = new DocAssocInfo();
        if (isSourceAssoc) {
            final NodeRef sourceRef = assocRef.getSourceRef();
            assocInf.setNodeRef(sourceRef);
            if (!nodeService.hasAspect(sourceRef, DocumentCommonModel.Aspects.SEARCHABLE)) {
                if (CaseModel.Associations.CASE_DOCUMENT.equals(assocRef.getTypeQName())) {
                    assocInf.setCaseNodeRef(sourceRef);
                    assocInf.setAssocType(AssocType.DEFAULT);
                    assocInf.setType("Asi");
                    assocInf.setTitle((String) nodeService.getProperty(sourceRef, CaseModel.Props.TITLE));
                } else if (MaaisModel.Associations.MAAIS_CASE_DOCUMENT.equals(assocRef.getTypeQName())) {
                    assocInf.setCaseNodeRef(sourceRef);
                    assocInf.setMaaisCase(true);
                    assocInf.setAssocType(AssocType.DEFAULT);
                    assocInf.setType("MaaIS toimik");
                    assocInf.setRegNumber((String) nodeService.getProperty(sourceRef, MaaisModel.Props.CASE_NUMBER));
                    List<String> title = new ArrayList<String>();
                    String relatedPerson = (String) nodeService.getProperty(sourceRef, MaaisModel.Props.CASE_RELATED_PERSON);
                    addIfNotBlank(title, relatedPerson, "Isik/asutus: ");
                    String landNumber = (String) nodeService.getProperty(sourceRef, MaaisModel.Props.LAND_NUMBER);
                    addIfNotBlank(title, landNumber, "Maaüksuse nr: ");
                    String landName = (String) nodeService.getProperty(sourceRef, MaaisModel.Props.LAND_NAME);
                    addIfNotBlank(title, landName, "Maaüksuse nimi: ");
                    assocInf.setTitle(StringUtils.join(title, "; "));
                } else {
                    log.debug("not searchable: " + assocRef);
                    return null;
                }
            }
            if (DocumentCommonModel.Assocs.DOCUMENT_FOLLOW_UP.equals(assocRef.getTypeQName())) {
                assocInf.setAssocType(AssocType.FOLLOWUP);
            } else if (DocumentCommonModel.Assocs.DOCUMENT_REPLY.equals(assocRef.getTypeQName())) {
                assocInf.setAssocType(AssocType.REPLY);
            } else if (DocumentCommonModel.Assocs.DOCUMENT_2_DOCUMENT.equals(assocRef.getTypeQName())) {
                assocInf.setAssocType(AssocType.DEFAULT);
            } else if (assocInf.getAssocType() == null) {
                throw new RuntimeException("Unexpected document type: " + assocRef.getTypeQName());
            }
            if (!assocInf.isCase()) {// document association, not case
                final Node otherDocNode = new Node(sourceRef);
                assocInf.setTitle((String) nodeService.getProperty(sourceRef, DOC_NAME));
                assocInf.setType(documentTypeService.getDocumentType(otherDocNode.getType()).getName());
                assocInf.setRegNumber((String) nodeService.getProperty(sourceRef, REG_NUMBER));
                assocInf.setRegDateTime((Date) nodeService.getProperty(sourceRef, REG_DATE_TIME));
            }
        } else {
            final NodeRef targetRef = assocRef.getTargetRef();
            if (!nodeService.hasAspect(targetRef, DocumentCommonModel.Aspects.SEARCHABLE)) {
                return null;
            }
            if (DocumentCommonModel.Assocs.DOCUMENT_REPLY.equals(assocRef.getTypeQName())//
                    || DocumentCommonModel.Assocs.DOCUMENT_FOLLOW_UP.equals(assocRef.getTypeQName())) {
                assocInf.setAssocType(AssocType.INITIAL);
            } else if (DocumentCommonModel.Assocs.DOCUMENT_2_DOCUMENT.equals(assocRef.getTypeQName())) {
                assocInf.setAssocType(AssocType.DEFAULT);
            }
            final Node otherDocNode = new Node(targetRef);
            final Map<String, Object> otherDocProps = otherDocNode.getProperties();
            assocInf.setTitle((String) otherDocProps.get(DOC_NAME));
            assocInf.setRegNumber((String) otherDocProps.get(REG_NUMBER));
            assocInf.setRegDateTime((Date) otherDocProps.get(REG_DATE_TIME));
            assocInf.setType(documentTypeService.getDocumentType(otherDocNode.getType()).getName());
            assocInf.setNodeRef(assocRef.getTargetRef());
        }
        assocInf.setSource(isSourceAssoc);
        return assocInf;
    }

    private void addIfNotBlank(List<String> title, String value, String prefix) {
        if (StringUtils.isNotBlank(value)) {
            title.add(prefix + value);
        }
    }

    @Override
    public int getDocumentsCountByVolumeOrCase(NodeRef parentRef) {
        return nodeService.getChildAssocs(parentRef, RegexQNamePattern.MATCH_ALL, RegexQNamePattern.MATCH_ALL).size();
    }

    /**
     * @param docRef
     * @param replyAssocs
     * @return true if document with given docRef has any reply or followUp documents
     */
    private boolean isReplyOrFollowupDoc(final NodeRef docRef, List<AssociationRef> replyAssocs) {
        if (replyAssocs == null) {
            replyAssocs = nodeService.getTargetAssocs(docRef, DocumentCommonModel.Assocs.DOCUMENT_REPLY);
        }
        final boolean isReplyOrFollowupDoc = replyAssocs.size() > 0
                || nodeService.getTargetAssocs(docRef, DocumentCommonModel.Assocs.DOCUMENT_FOLLOW_UP).size() > 0; //
        return isReplyOrFollowupDoc;
    }

    private List<Document> getAllDocumentsByParentNodeRef(NodeRef parentRef) {
        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(parentRef, RegexQNamePattern.MATCH_ALL, RegexQNamePattern.MATCH_ALL);
        List<Document> docsOfParent = new ArrayList<Document>(childAssocs.size());
        for (ChildAssociationRef childAssocRef : childAssocs) {
            docsOfParent.add(getDocumentByNodeRef(childAssocRef.getChildRef()));
        }
        return docsOfParent;
    }

    private NodeRef getInitialDocument(NodeRef followupDocRef) {
        NodeRef sourceRef = getFirstTargetAssocRef(followupDocRef, DocumentCommonModel.Assocs.DOCUMENT_FOLLOW_UP);
        if (sourceRef == null) {
            return getFirstTargetAssocRef(followupDocRef, DocumentCommonModel.Assocs.DOCUMENT_REPLY);
        }
        return sourceRef;
    }

    private NodeRef getFirstTargetAssocRef(NodeRef sourceRef, QName assocQNamePattern) {
        List<AssociationRef> targetAssocs = nodeService.getTargetAssocs(sourceRef, assocQNamePattern);
        NodeRef targetRef = null;
        if (targetAssocs.size() > 0) {
            targetRef = targetAssocs.get(0).getTargetRef();
            if (targetAssocs.size() > 1) {
                log.warn("document with noderef '" + targetRef + "' has more than one '" + assocQNamePattern + "' relations!");
            }
        }
        return targetRef;
    }

    /**
     * Helps to identify if properties(that should not be ignored for given properties map) that have been changed
     * 
     * @author Ats Uiboupin
     */
    private class PropertyChangesMonitorHelper {
        private final QName TEMP_PROPERTY_CHANGES_IGNORED_PROPS = QName.createQName("{temp}propertyChanges_ignoredProps");

        /**
         * Add given property names to ignore list when checking changes in property values
         * 
         * @param props
         * @param newIgnoredProps
         */
        private void addIgnoredProps(final Map<String, Object> props, QName... newIgnoredProps) {
            if (newIgnoredProps == null) {
                return;
            }
            @SuppressWarnings("unchecked")
            Collection<QName> ignoredProps = (Collection<QName>) props.get(TEMP_PROPERTY_CHANGES_IGNORED_PROPS);
            if (ignoredProps == null) {
                ignoredProps = new ArrayList<QName>(newIgnoredProps.length);
            }
            for (QName qName : newIgnoredProps) {
                ignoredProps.add(qName);
            }
            props.put(TEMP_PROPERTY_CHANGES_IGNORED_PROPS.toString(), ignoredProps);
        }

        /**
         * @param nodeRef
         * @param propsToSave
         * @param ignoredProps
         * @return true, if some property was changed <br>
         *         This method ignores properties that
         *         <ul>
         *         <li>are given as an argument to this method call</li>
         *         <li>added to <code>propsToSave</code> using {@link #addIgnoredProps(Map, QName...)}</li>
         *         </ul>
         */
        private boolean setPropertiesIgnoringSystemAndReturnIfChanged(final NodeRef nodeRef, final Map<String, Object> propsToSave, QName... ignoredProps) {
            final List<QName> ignored = ignoredProps == null ? Collections.<QName> emptyList() : Arrays.asList(ignoredProps);
            final Map<QName, Serializable> oldProps = generalService.getPropertiesIgnoringSys(nodeService.getProperties(nodeRef));
            final Map<QName, Serializable> docQNameProps = RepoUtil.toQNameProperties(propsToSave);
            final Map<QName, Serializable> propsIgnoringSystem = generalService.setPropertiesIgnoringSystem(docQNameProps, nodeRef);
            @SuppressWarnings("unchecked")
            final ArrayList<QName> propertyChangesIgnoredProps = (ArrayList<QName>) propsToSave.get(TEMP_PROPERTY_CHANGES_IGNORED_PROPS);
            if (propertyChangesIgnoredProps != null) {
                oldProps.put(TEMP_PROPERTY_CHANGES_IGNORED_PROPS, propertyChangesIgnoredProps);
            }
            return checkPropertyChanges(oldProps, propsIgnoringSystem, ignored);
        }

        private boolean checkPropertyChanges(final Map<QName, Serializable> oldProps, final Map<QName, Serializable> newProps, final List<QName> ignoredProps) {
            if (oldProps.size() != newProps.size()) {
                if (isPropNamesDifferent(oldProps, newProps, ignoredProps, "removed ignored props: ")) {
                    return true;
                }
                if (isPropNamesDifferent(newProps, oldProps, ignoredProps, "added ignored props: ")) {
                    return true;
                }
            }
            return isChanges(oldProps, newProps, ignoredProps);
        }

        private boolean isPropNamesDifferent(final Map<QName, Serializable> oldProps, final Map<QName, Serializable> newProps, final List<QName> ignoredProps,
                String debugPrefix) {
            boolean differentPropNames = false;
            HashSet<QName> oldKeys = new HashSet<QName>(oldProps.keySet());
            final HashSet<QName> newKeys = new HashSet<QName>(newProps.keySet());
            oldKeys.removeAll(newKeys);
            if (oldKeys.size() > 0) {
                if (!ignoredProps.containsAll(oldKeys)) {
                    differentPropNames = isChanges(oldProps, newProps, ignoredProps);
                    if (differentPropNames && log.isDebugEnabled()) {
                        log.debug(debugPrefix + oldKeys);
                    }
                }
            }
            return differentPropNames;
        }

        private boolean isChanges(final Map<QName, Serializable> oldProps, final Map<QName, Serializable> newProps, final List<QName> ignoredProps) {
            Collection<QName> extraIgnoredProps = null;
            for (Entry<QName, Serializable> entry : newProps.entrySet()) {
                final QName key = entry.getKey();
                final Serializable newValue = entry.getValue();
                final Serializable oldValue = oldProps.get(key);
                if (!EqualsHelper.nullSafeEquals(oldValue, newValue) && !key.getNamespaceURI().equals(NamespaceService.CONTENT_MODEL_1_0_URI)
                        && !ignoredProps.contains(key) && !TEMP_PROPERTY_CHANGES_IGNORED_PROPS.equals(key)) {
                    if (extraIgnoredProps == null) {
                        @SuppressWarnings("unchecked")
                        final Collection<QName> ignoreCollection = (Collection<QName>) oldProps.get(TEMP_PROPERTY_CHANGES_IGNORED_PROPS);
                        extraIgnoredProps = ignoreCollection;
                    }
                    if (extraIgnoredProps != null) {
                        if (extraIgnoredProps.contains(key)) {
                            continue;
                        }
                    }
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Class that divides whole regNumber, that might contain individualizing number into individualizing number and rest of it
     * 
     * @author Ats Uiboupin
     */
    private static class RegNrHolder {
        /**
         * regNrWithoutIndividualizingNr - arbitary char-sequence ending with dash and followed by individualizingNr at the end<br>
         * individualizingNr - integer number at the end of regNr
         */
        private static final Pattern individualizingNrPattern = Pattern.compile("(.*-)(\\d{1,})\\z");
        private final String regNrWithoutIndividualizingNr;
        private final Integer individualizingNr;

        public RegNrHolder(String wholeRegNr) {
            if (StringUtils.isNotBlank(wholeRegNr)) {
                Matcher matcher = individualizingNrPattern.matcher(wholeRegNr.trim());
                if (matcher.find()) {
                    regNrWithoutIndividualizingNr = matcher.group(1);
                    individualizingNr = Integer.valueOf(matcher.group(2));
                } else {
                    regNrWithoutIndividualizingNr = wholeRegNr;
                    individualizingNr = null;
                }
            } else {
                regNrWithoutIndividualizingNr = wholeRegNr;
                individualizingNr = null;
            }
        }

        public String getRegNrWithoutIndividualizingNr() {
            return regNrWithoutIndividualizingNr;
        }

        public Integer getIndividualizingNr() {
            return individualizingNr;
        }
    }

    @Override
    public List<TaskAndDocument> getTasksWithDocuments(List<Task> tasks) {
        List<TaskAndDocument> results = new ArrayList<TaskAndDocument>(tasks.size());
        Map<NodeRef, Document> documents = new HashMap<NodeRef, Document>(tasks.size());
        for (Task task : tasks) {
            NodeRef workflow = nodeService.getPrimaryParent(task.getNode().getNodeRef()).getParentRef();
            NodeRef compoundWorkflow = nodeService.getPrimaryParent(workflow).getParentRef();
            NodeRef documentNodeRef = nodeService.getPrimaryParent(compoundWorkflow).getParentRef();
            Document document = documents.get(documentNodeRef);
            if (document == null) {
                document = getDocumentByNodeRef(documentNodeRef);
                documents.put(documentNodeRef, document);
            }
            results.add(new TaskAndDocument(task, document));
        }
        return results;
    }

    @Override
    public void stopDocumentPreceedingAndUpdateStatus(NodeRef nodeRef) {
        nodeService.setProperty(nodeRef, DOC_STATUS, DocumentStatus.STOPPED.getValueName());
        workflowService.stopAllCompoundWorkflows(nodeRef);
        documentLogService.addDocumentLog(nodeRef, I18NUtil.getMessage("document_log_status_proceedingStop"));
    }

    @Override
    public void continueDocumentPreceedingAndUpdateStatus(NodeRef nodeRef) {
        nodeService.setProperty(nodeRef, DOC_STATUS, DocumentStatus.WORKING.getValueName());
        workflowService.continueAllCompoundWorkflows(nodeRef);
        documentLogService.addDocumentLog(nodeRef, I18NUtil.getMessage("document_log_status_proceedingContinue"));
    }

    @Override
    public void prepareDocumentSigning(final NodeRef document) {
        AuthenticationUtil.runAs(new RunAsWork<Object>() {
            @Override
            public Object doWork() throws Exception {
                long step0 = System.currentTimeMillis();
                // Register the document, if not already registered
                registerDocumentIfNotRegistered(document, false);
                long step1 = System.currentTimeMillis();
                documentTemplateService.updateGeneratedFilesOnRegistration(document);
                long step2 = System.currentTimeMillis();
                // Generate PDF-files for all the files that support it.
                fileService.transformActiveFilesToPdf(document);
                long step3 = System.currentTimeMillis();
                if (log.isInfoEnabled()) {
                    log.info("prepareDocumentSigning service call took " + (step3 - step0) + " ms\n    register document - " + (step1 - step0)
                            + " ms\n    update word files contents - " + (step2 - step1) + " ms\n    convert files to pdf - " + (step3 - step2) + " ms");
                }
                return null;
            }
        }, AuthenticationUtil.getSystemUserName());
    }

    @Override
    public SignatureDigest prepareDocumentDigest(NodeRef document, String certHex) throws SignatureException {
        long step0 = System.currentTimeMillis();
        SignatureDigest signatureDigest = null;
        NodeRef existingDdoc = checkExistingDdoc(document);
        long step1 = System.currentTimeMillis();
        String debug = "";
        if (existingDdoc != null) {
            signatureDigest = signatureService.getSignatureDigest(existingDdoc, certHex);
            long step2 = System.currentTimeMillis();
            debug += "\n    calculate digest for existing ddoc - " + (step2 - step1) + " ms";
        } else {
            List<NodeRef> files = getSignatureTaskActiveNodeRefs(document);
            long step2 = System.currentTimeMillis();
            signatureDigest = signatureService.getSignatureDigest(files, certHex);
            long step3 = System.currentTimeMillis();
            debug += "\n    load file list - " + (step2 - step1) + " ms";
            debug += "\n    calculate digest for " + files.size() + " files - " + (step3 - step2) + " ms";
        }
        long step4 = System.currentTimeMillis();
        if (log.isInfoEnabled()) {
            log.info("prepareDocumentDigest service call took " + (step4 - step0) + " ms\n    check for existing ddoc - " + (step1 - step0) + " ms" + debug);
        }
        return signatureDigest;
    }

    private List<NodeRef> getSignatureTaskActiveNodeRefs(NodeRef document) {
        List<NodeRef> nodeRefs = new ArrayList<NodeRef>();
        List<File> files = fileService.getAllActiveFiles(document);
        for (File file : files) {
            nodeRefs.add(file.getNodeRef());
        }
        return nodeRefs;
    }

    @Override
    public void finishDocumentSigning(final SignatureTask task, final String signatureHex) {
        long step0 = System.currentTimeMillis();
        final NodeRef document = task.getParent().getParent().getParent();
        final String filename = generateDdocFilename(document);
        final long step1 = System.currentTimeMillis();
        String debug = AuthenticationUtil.runAs(new RunAsWork<String>() {
            @Override
            public String doWork() throws Exception {
                NodeRef existingDdoc = checkExistingDdoc(document);
                long step2 = System.currentTimeMillis();
                String debug1 = "\n    check for existing ddoc - " + (step2 - step1) + " ms";
                if (existingDdoc != null) {
                    signatureService.addSignature(existingDdoc, task.getSignatureDigest(), signatureHex);
                    long step3 = System.currentTimeMillis();
                    debug1 += "\n    add signature to existing ddoc - " + (step3 - step2) + " ms";
                } else {
                    List<NodeRef> files = fileService.getAllActiveFilesNodeRefs(document);
                    long step3 = System.currentTimeMillis();
                    String uniqueFilename = generalService.getUniqueFileName(document, filename);
                    NodeRef ddoc = signatureService.createContainer(document, files, uniqueFilename, task.getSignatureDigest(), signatureHex);
                    long step4 = System.currentTimeMillis();
                    documentLogService.addDocumentLog(document, I18NUtil.getMessage("document_log_status_fileAdded", uniqueFilename));
                    long step5 = System.currentTimeMillis();
                    fileService.setAllFilesInactiveExcept(document, ddoc);
                    long step6 = System.currentTimeMillis();

                    debug1 += "\n    load file list - " + (step3 - step2) + " ms";
                    debug1 += "\n    create ddoc and add signature (" + files.size() + " files) - " + (step4 - step3) + " ms";
                    debug1 += "\n    add log entry to document - " + (step5 - step4) + " ms";
                    debug1 += "\n    set files inactive - " + (step6 - step5) + " ms";
                }
                fileService.deleteGeneratedFilesByType(document, GeneratedFileType.SIGNED_PDF);
                return debug1;
            }
        }, AuthenticationUtil.getSystemUserName());
        long step7 = System.currentTimeMillis();
        workflowService.finishInProgressTask(task, 1);
        long step8 = System.currentTimeMillis();
        if (log.isInfoEnabled()) {
            log.info("finishDocumentSigning service call took " + (step8 - step0) + " ms\n    generate ddoc filename - " + (step1 - step0) + " ms" + debug
                    + "\n    finish workflow task - " + (step8 - step7) + " ms");
        }
    }

    private NodeRef checkExistingDdoc(NodeRef document) {
        List<File> files = fileService.getAllActiveFiles(document);
        if (files.size() == 1) {
            File file = files.get(0);
            if (signatureService.isDigiDocContainer(file.getNodeRef())) {
                return file.getNodeRef();
            }
        }
        return null;
    }

    private String generateDdocFilename(NodeRef document) {
        StringBuilder sb = new StringBuilder();
        Node docNode = getDocument(document);

        String existingRegNr = (String) docNode.getProperties().get(REG_NUMBER.toString());
        sb.append(existingRegNr);

        Date existingRegDate = (Date) docNode.getProperties().get(REG_DATE_TIME.toString());
        sb.append(" ");
        sb.append(Utils.getDateFormat(FacesContext.getCurrentInstance()).format(existingRegDate));

        DocumentType documentType = documentTypeService.getDocumentType(docNode.getType());
        if (documentType != null) {
            sb.append(" ");
            sb.append(documentType.getName());
        }

        return FilenameUtil.buildFileName(sb.toString(), "ddoc");
    }

    private boolean isDraft(NodeRef document) {
        return nodeService.getPrimaryParent(document).getParentRef().equals(getDrafts());
    }

    // ========================================================================
    // =============================== FAVORITES ==============================
    // ========================================================================

    @Override
    public List<Document> getFavorites() {
        NodeRef user = userService.getUser(AuthenticationUtil.getRunAsUser()).getNodeRef();
        if (!nodeService.hasAspect(user, DocumentCommonModel.Aspects.FAVORITE_CONTAINER)) {
            return Collections.emptyList();
        }
        List<AssociationRef> assocs = nodeService.getTargetAssocs(user, DocumentCommonModel.Assocs.FAVORITE);
        List<Document> favorites = new ArrayList<Document>(assocs.size());
        for (AssociationRef assoc : assocs) {
            favorites.add(getDocumentByNodeRef(assoc.getTargetRef()));
        }
        return favorites;
    }

    @Override
    public boolean isFavoriteAddable(NodeRef document) {
        return !isFavorite(document) && !isDraft(document);
    }

    @Override
    public boolean isFavorite(NodeRef document) {
        NodeRef user = userService.getUser(AuthenticationUtil.getRunAsUser()).getNodeRef();
        if (!nodeService.hasAspect(user, DocumentCommonModel.Aspects.FAVORITE_CONTAINER)) {
            return false;
        }
        for (AssociationRef assoc : nodeService.getTargetAssocs(user, DocumentCommonModel.Assocs.FAVORITE)) {
            if (assoc.getTargetRef().equals(document)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void addFavorite(NodeRef document) {
        if (isFavorite(document)) {
            return;
        }
        NodeRef user = userService.getUser(AuthenticationUtil.getRunAsUser()).getNodeRef();
        if (!nodeService.hasAspect(user, DocumentCommonModel.Aspects.FAVORITE_CONTAINER)) {
            nodeService.addAspect(user, DocumentCommonModel.Aspects.FAVORITE_CONTAINER, null);
        }
        nodeService.createAssociation(user, document, DocumentCommonModel.Assocs.FAVORITE);
    }

    @Override
    public void removeFavorite(NodeRef document) {
        NodeRef user = userService.getUser(AuthenticationUtil.getRunAsUser()).getNodeRef();
        nodeService.removeAssociation(user, document, DocumentCommonModel.Assocs.FAVORITE);
    }

    // ========================================================================
    // ========== COLLECT DOCUMENT SEARCHABLE PROPERTIES FOR INDEXING =========
    // ========================================================================

    private Map<String, Object> getSearchableOtherProps(Node document) {
        Map<String, Object> props = new HashMap<String, Object>();
        // searchableSendMode is updated in SendOutServiceImpl#sendOut
        setCollectedProps(document, props, SEARCHABLE_COST_MANAGER, DocumentSpecificModel.Props.COST_MANAGER);
        setCollectedProps(document, props, SEARCHABLE_APPLICANT_NAME, DocumentSpecificModel.Props.APPLICANT_NAME,
                DocumentSpecificModel.Props.PROCUREMENT_APPLICANT_NAME);
        setCollectedProps(document, props, SEARCHABLE_ERRAND_BEGIN_DATE, DocumentSpecificModel.Props.ERRAND_BEGIN_DATE);
        setCollectedProps(document, props, SEARCHABLE_ERRAND_END_DATE, DocumentSpecificModel.Props.ERRAND_END_DATE);
        setCollectedProps(document, props, SEARCHABLE_ERRAND_COUNTRY, DocumentSpecificModel.Props.ERRAND_COUNTRY);
        setCollectedProps(document, props, SEARCHABLE_ERRAND_COUNTY, DocumentSpecificModel.Props.ERRAND_COUNTY);
        setCollectedProps(document, props, SEARCHABLE_ERRAND_CITY, DocumentSpecificModel.Props.ERRAND_CITY);
        setCollectedProps(document, props, SEARCHABLE_PARTY_NAME, DocumentSpecificModel.Props.PARTY_NAME);
        return props;
    }

    private void setCollectedProps(Node document, Map<String, Object> props, QName targetProp, QName... sourceProps) {
        ArrayList<Serializable> results = collectProperties(document, sourceProps);
        log.debug("Collected properties " + targetProp.toPrefixString(namespaceService) + " " + results);
        props.put(targetProp.toString(), results);
    }

    private ArrayList<Serializable> collectProperties(Node node, QName... propNames) {
        ArrayList<Serializable> values = new ArrayList<Serializable>();
        for (QName propName : propNames) {
            PropertyDefinition propDef = dictionaryService.getProperty(propName);
            QName aspect = ((AspectDefinition) propDef.getContainerClass()).getName();
            collectProperties(values, node, propName, aspect);
        }
        return values;
    }

    private static void collectProperties(List<Serializable> values, Node node, QName propName, QName aspect) {
        if (node.hasAspect(aspect)) {
            Serializable value = (Serializable) node.getProperties().get(propName);
            if (value instanceof List<?>) {
                @SuppressWarnings("unchecked")
                List<Serializable> list = (List<Serializable>) value;
                values.addAll(list);
            } else {
                values.add(value);
            }
        }
        for (List<Node> list : node.getAllChildAssociationsByAssocType().values()) {
            for (Node childNode : list) {
                collectProperties(values, childNode, propName, aspect);
            }
        }
    }

    // ========================================================================
    // ========== COLLECT DOCUMENT SEARCHABLE PROPERTIES - methods with services, instead of node
    // ========================================================================

    @Override
    public ArrayList<Serializable> collectProperties(NodeRef nodeRef, List<ChildAssociationRef> childAssocs, QName... propNames) {
        ArrayList<Serializable> values = new ArrayList<Serializable>();
        for (QName propName : propNames) {
            PropertyDefinition propDef = dictionaryService.getProperty(propName);
            QName aspect = ((AspectDefinition) propDef.getContainerClass()).getName();
            collectProperties(values, nodeRef, propName, aspect, childAssocs);
        }
        return values;
    }

    private void collectProperties(List<Serializable> values, NodeRef nodeRef, QName propName, QName aspect, List<ChildAssociationRef> childAssocs) {
        if (nodeService.getAspects(nodeRef).contains(aspect)) {
            Serializable value = nodeService.getProperty(nodeRef, propName);
            if (value instanceof List<?>) {
                @SuppressWarnings("unchecked")
                List<Serializable> list = (List<Serializable>) value;
                values.addAll(list);
            } else {
                values.add(value);
            }
        }
        List<ChildAssociationRef> childAssocRefs = childAssocs;
        if (childAssocRefs == null) {
            childAssocRefs = nodeService.getChildAssocs(nodeRef);
        }
        for (ChildAssociationRef childAssocRef : nodeService.getChildAssocs(nodeRef)) {
            collectProperties(values, childAssocRef.getChildRef(), propName, aspect, null);
        }
    }

    // ========================================================================
    // ==================== PROCESS EXTENDED SEARCH RESULTS ===================
    // ========================================================================

    private static Map<QName/* searchable */, List<QName>/* filter */> searchableToFilter = new HashMap<QName, List<QName>>();
    private static Map<QName/* searchable */, List<QName>/* document */> searchableToDocument = new HashMap<QName, List<QName>>();
    static {
        searchableToFilter.put(SEARCHABLE_COST_MANAGER, Arrays.asList(DocumentSearchModel.Props.COST_MANAGER));
        searchableToFilter.put(SEARCHABLE_APPLICANT_NAME, Arrays.asList(DocumentSearchModel.Props.APPLICANT_NAME));
        searchableToFilter.put(SEARCHABLE_ERRAND_BEGIN_DATE, Arrays.asList(DocumentSearchModel.Props.ERRAND_BEGIN_DATE_BEGIN,
                DocumentSearchModel.Props.ERRAND_BEGIN_DATE_END));
        searchableToFilter.put(SEARCHABLE_ERRAND_END_DATE, Arrays.asList(DocumentSearchModel.Props.ERRAND_END_DATE_BEGIN,
                DocumentSearchModel.Props.ERRAND_END_DATE_END));
        searchableToFilter.put(SEARCHABLE_ERRAND_COUNTRY, Arrays.asList(DocumentSearchModel.Props.ERRAND_COUNTRY));
        searchableToFilter.put(SEARCHABLE_ERRAND_COUNTY, Arrays.asList(DocumentSearchModel.Props.ERRAND_COUNTY));
        searchableToFilter.put(SEARCHABLE_ERRAND_CITY, Arrays.asList(DocumentSearchModel.Props.ERRAND_CITY));

        searchableToDocument.put(SEARCHABLE_COST_MANAGER, Arrays.asList(DocumentSpecificModel.Props.COST_MANAGER));
        searchableToDocument.put(SEARCHABLE_APPLICANT_NAME, Arrays.asList(DocumentSpecificModel.Props.APPLICANT_NAME,
                DocumentSpecificModel.Props.PROCUREMENT_APPLICANT_NAME));
        searchableToDocument.put(SEARCHABLE_ERRAND_BEGIN_DATE, Arrays.asList(DocumentSpecificModel.Props.ERRAND_BEGIN_DATE));
        searchableToDocument.put(SEARCHABLE_ERRAND_END_DATE, Arrays.asList(DocumentSpecificModel.Props.ERRAND_END_DATE));
        searchableToDocument.put(SEARCHABLE_ERRAND_COUNTRY, Arrays.asList(DocumentSpecificModel.Props.ERRAND_COUNTRY));
        searchableToDocument.put(SEARCHABLE_ERRAND_COUNTY, Arrays.asList(DocumentSpecificModel.Props.ERRAND_COUNTY));
        searchableToDocument.put(SEARCHABLE_ERRAND_CITY, Arrays.asList(DocumentSpecificModel.Props.ERRAND_CITY));
    }

    @Override
    /*
     * Dokumentide _laiendatud_ otsing on 8 välja (4.1.2.23-30 - need mis child-node'idega on seotud) väärtuste võrdlemise puhul rangem kui _lihtne_ otsing!
     * Sest nende väljade puhul tehakse laiendatud otsingu tulemuste kuvamisel Java koodis lisavõrdlemist, peaaegu (aga mitte täpselt) ühtib Lucene käitumisega.
     * Sõnadeks jagamine ja case-insensitive stringi võrdlus peaks sama olema. Aga lucene teeb lisaks ka tähtedelt täppide eemaldamist. Seega lihtne otsing
     * matchib taotleja nime "Märt" puhul mõlemad "Märt" ja "Mart", laiendatud otsing ainult esimese.
     */
    public List<Document> processExtendedSearchResults(List<Document> documents, Node filter) {
        List<Document> results = new ArrayList<Document>(documents.size());
        for (Document document : documents) {
            document.getFiles(); // load files
            results.addAll(getSearchableRows(document, filter));
        }
        return results;
    }

    private List<Document> getSearchableRows(Document document, Node filter) {
        List<Document> results = new ArrayList<Document>();
        // process send modes
        for (Document row : getSendModeRows(document, filter)) {
            // for each row, process metadata props
            List<Document> rows = processExtendedSearchResults(searchableToDocument, document.getNode(), filter, row);
            if (rows == null) {
                continue;
            }
            if (rows.size() > 0) {
                results.addAll(rows);
            } else {
                results.add(row);
            }
        }
        return results;
    }

    private List<Document> getSendModeRows(Document document, Node filter) {
        @SuppressWarnings("unchecked")
        List<String> sendModes = (List<String>) filter.getProperties().get(DocumentSearchModel.Props.SEND_MODE);
        List<SendInfo> sendInfos = sendOutService.getDocumentSendInfos(document.getNodeRef());
        List<Document> results = new ArrayList<Document>(sendInfos.size());
        if (sendInfos.size() == 0) {
            results.add(document);
        }
        for (SendInfo sendInfo : sendInfos) {
            String sendMode = sendInfo.getSendMode();
            // when no filter is specified, all results match
            if (sendModes == null || sendModes.size() == 0 || !isNotMatch(sendMode, sendModes)) {
                Document row = new Document(document);
                row.setSearchableProperty(SEARCHABLE_SEND_MODE, sendMode);
                results.add(row);
            }
        }
        return results;
    }

    private List<Document> processExtendedSearchResults(Map<QName/* filter */, List<QName>/* document */> props, Node node, Node filter, Document document) {
        Set<Entry<QName, List<QName>>> entrySet = props.entrySet();
        for (Entry<QName, List<QName>> entry : entrySet) {
            List<QName> propNames = entry.getValue();
            for (QName propName : propNames) {
                PropertyDefinition propDef = dictionaryService.getProperty(propName);
                QName aspect = ((AspectDefinition) propDef.getContainerClass()).getName();
                if (node.hasAspect(aspect)) {

                    List<Serializable> filterProps = new ArrayList<Serializable>();
                    for (QName filterProp : searchableToFilter.get(entry.getKey())) {
                        filterProps.add((Serializable) filter.getProperties().get(filterProp));
                    }
                    Serializable nodeProp = (Serializable) node.getProperties().get(propName);

                    // if filterProp is not empty AND filterProp does not match node prop
                    if (isFilterPropNotBlankAndDoesNotMatch(nodeProp, filterProps)) {
                        // then return empty list
                        return null;
                    }
                    // else document.setproperty node prop
                    document.setSearchableProperty(entry.getKey(), Document.join(nodeProp));
                }
            }
        }

        List<Document> documents = new ArrayList<Document>();
        boolean oneExecuted = false;
        boolean oneReturnedNotNull = false;
        for (List<Node> list : node.getAllChildAssociationsByAssocType().values()) {
            for (Node childNode : list) {
                if (!dictionaryService.isSubClass(childNode.getType(), DocumentCommonModel.Types.METADATA_CONTAINER)) {
                    continue;
                }
                oneExecuted = true;
                List<Document> childResults = processExtendedSearchResults(props, childNode, filter, document);
                if (childResults != null) {
                    documents.addAll(childResults);
                    oneReturnedNotNull = true;
                }
            }
        }
        if (documents.size() == 0) {
            documents.add(new Document(document));
        }
        if (!oneExecuted) {
            return documents;
        }
        if (!oneReturnedNotNull) {
            return null;
        }
        return documents;
    }

    private boolean isFilterPropNotBlankAndDoesNotMatch(Serializable nodeProp, List<Serializable> filterProps) {
        if (filterProps.size() == 1) {
            if (filterProps.get(0) == null) {
                return false;
            }
            if (filterProps.get(0) instanceof List<?>) {
                @SuppressWarnings("unchecked")
                List<String> list = (List<String>) filterProps.get(0);
                if (list.size() == 0) {
                    return false;
                }
                return isNotMatch(nodeProp, list);
            }
            if (((String) filterProps.get(0)).length() == 0) {
                return false;
            }
            return isNotMatchWildcard(nodeProp, (String) filterProps.get(0));
        } else if (filterProps.size() == 2) {
            if (filterProps.get(0) == null && filterProps.get(1) == null) {
                return false;
            }
            return isNotMatch(nodeProp, (Date) filterProps.get(0), (Date) filterProps.get(1));
        }
        throw new RuntimeException("Not supported: " + filterProps);
    }

    private static boolean isNotMatch(Serializable nodeProp, List<String> list) {
        for (String value : list) {
            if (!isNotMatchExact(nodeProp, value)) {
                return false;
            }
        }
        return true;
    }

    // Exact match - if select field
    private static boolean isNotMatchExact(Serializable nodeProp, String filterProp) {
        return !filterProp.equalsIgnoreCase((String) nodeProp);
    }

    // Wildcard match - if text field
    private boolean isNotMatchWildcard(Serializable nodeProp, String filterProp) {
        List<String> words = getDocumentSearchService().parseQuickSearchWords(filterProp);
        for (String word : words) {
            if (nodeProp instanceof List<?>) {
                boolean found = false;
                @SuppressWarnings("unchecked")
                List<String> nodePropList = (List<String>) nodeProp;
                for (String nodeListItem : nodePropList) {
                    if (StringUtils.containsIgnoreCase(nodeListItem, word)) {
                        found = true;
                        break;
                    }
                }
                return !found;
            }
            if (!StringUtils.containsIgnoreCase((String) nodeProp, word)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isNotMatch(Serializable nodeProp, Date begin, Date end) {
        if (nodeProp == null) {
            return true;
        }
        Date blah = (Date) nodeProp;
        if (begin != null && blah.before(begin)) {
            return true;
        }
        if (end != null && blah.after(end)) {
            return true;
        }
        return false;
    }

    @Override
    public void updateParentNodesContainingDocsCount(NodeRef documentNodeRef, boolean documentAdded) {
        generalService.updateParentContainingDocsCount(generalService.getAncestorNodeRefWithType(documentNodeRef, CaseModel.Types.CASE),
                CaseModel.Props.CONTAINING_DOCS_COUNT, documentAdded, null);
        generalService.updateParentContainingDocsCount(generalService.getAncestorNodeRefWithType(documentNodeRef, VolumeModel.Types.VOLUME),
                VolumeModel.Props.CONTAINING_DOCS_COUNT, documentAdded, null);
        generalService.updateParentContainingDocsCount(generalService.getAncestorNodeRefWithType(documentNodeRef, SeriesModel.Types.SERIES),
                SeriesModel.Props.CONTAINING_DOCS_COUNT, documentAdded, null);
    }

    public void addPrivilegesBasedOnSeriesOnBackground(final NodeRef docRef) {
        RunAsWork<Void> work = new RunAsWork<Void>() {
            @Override
            public Void doWork() throws Exception {
                addPrivilegesBasedOnSeries(docRef);
                return null;
            }
        };
        generalService.runOnBackground(work, "addPrivilegesBasedOnSeries-" + docRef.getId());
    }

    private Set<String> addPrivilegesBasedOnSeries(NodeRef docRef) {
        Map<String, Object> docProps = new HashMap<String, Object>();
        Set<String> result = addPrivilegesBasedOnSeries(docRef, docProps, null);

        @SuppressWarnings("unchecked")
        List<String> privUsers = (List<String>) docProps.get(PrivilegeModel.Props.USER.toString());
        @SuppressWarnings("unchecked")
        List<String> privGroups = (List<String>) docProps.get(PrivilegeModel.Props.GROUP.toString());

        privilegeService.mergePrivilegeUsersGroupsLists(docRef, privUsers, privGroups);
        return result;
    }

    @Override
    public Set<String> addPrivilegesBasedOnSeries(NodeRef docRef, Map<String, Object> docProps, NodeRef parentRef) {
        NodeRef seriesRef = generalService.getAncestorNodeRefWithType(parentRef != null ? parentRef : docRef, SeriesModel.Types.SERIES);
        if (seriesRef == null) {
            log.info("Document is not under documentList, skipping adding privileges based on series: " + docRef.toString());
            return Collections.<String> emptySet();
        }
        Pair<Set<String>, Set<String>> usersAndGroups = getSeriesAuthorities(seriesRef);
        Set<String> usersOfThisSeries = usersAndGroups.getFirst();
        @SuppressWarnings("unchecked")
        List<Integer> structUnits = (List<Integer>) nodeService.getProperty(seriesRef, SeriesModel.Props.STRUCT_UNIT);
        Set<String> seriesStructunitsUsers = userService.getUsernamesByStructUnit(structUnits);
        usersOfThisSeries.addAll(seriesStructunitsUsers);
        for (String userName : usersOfThisSeries) {
            // 3.1.18.2 a,b & 3.1.18.4 a,b

            privilegeService.addPrivilege(docRef, docProps, DocumentCommonModel.Types.DOCUMENT, userName
                    , DocumentCommonModel.Privileges.VIEW_DOCUMENT_META_DATA, DocumentCommonModel.Privileges.VIEW_DOCUMENT_FILES);
        }
        Set<String> groups = usersAndGroups.getSecond();
        final QName addPrivListener = DocumentCommonModel.Types.DOCUMENT;
        for (String group : groups) {
            Set<String> authorities = authorityService.getContainedAuthorities(AuthorityType.USER, group, true);
            for (String authority : authorities) {
                privilegeService.addPrivilege(docRef, docProps, addPrivListener, authority, group, DocumentPrivilegesUpdater.SERIES_GROUPMEMBERS_PRIVILEGES);
            }
        }
        return groups;
    }

    @Override
    public void onSavePrivileges(NodeRef docRef, Map<String, UserPrivileges> privilegesByUsername) {
        for (UserPrivileges vo : privilegesByUsername.values()) {
            if (!vo.isDeleted() && vo.getPrivilegesToAdd().contains(DocumentCommonModel.Privileges.EDIT_DOCUMENT_META_DATA)) {
                List<DocAssocInfo> assocInfos = getAssocInfos(new Node(docRef));
                for (DocAssocInfo docAssocInfo : assocInfos) {
                    if (!docAssocInfo.isCase()) {
                        NodeRef relatedDocRef = docAssocInfo.getNodeRef();
                        privilegeService.addPrivilege(relatedDocRef, null, DocumentCommonModel.Types.DOCUMENT, vo.getUserName()
                                , DocumentCommonModel.Privileges.VIEW_DOCUMENT_META_DATA, DocumentCommonModel.Privileges.VIEW_DOCUMENT_FILES);
                    }
                }
            }
        }
    }

    @Override
    public void onAddPrivileges(NodeRef docRef, Set<String> permissions) {
        Set<String> clonedPermissions = new HashSet<String>(permissions); // to avoid concurrentModificationException
        for (String permission : clonedPermissions) {
            Set<String> privilegeDependencies = DocumentCommonModel.Privileges.PRIVILEGE_DEPENDENCIES.get(permission);
            if (privilegeDependencies != null) {
                permissions.addAll(privilegeDependencies);
            }
        }
    }

    @Override
    public void onCreateAssociation(AssociationRef nodeAssocRef) {
        if (nodeAssocRef.getSourceRef().equals(nodeAssocRef.getTargetRef())) {
            return;
        }
        QName assocType = nodeAssocRef.getTypeQName();
        if (DocumentCommonModel.Assocs.DOCUMENT_REPLY.equals(assocType) || DocumentCommonModel.Assocs.DOCUMENT_FOLLOW_UP.equals(assocType)
                || DocumentCommonModel.Assocs.DOCUMENT_2_DOCUMENT.equals(assocType)) {
            log.debug("onCreateAssoc " + nodeAssocRef.getSourceRef() + "->" + nodeAssocRef.getTargetRef());
            NodeRef docRef = nodeAssocRef.getSourceRef();
            NodeRef otherDocRef = nodeAssocRef.getTargetRef();

            addPrivilegesBasedOnOther(otherDocRef, docRef);
            addPrivilegesBasedOnOther(docRef, otherDocRef);
        }
    }

    private void addPrivilegesBasedOnOther(NodeRef fromRef, NodeRef toRef) {
        Map<QName, Serializable> nodeProps = nodeService.getProperties(toRef);
        @SuppressWarnings("unchecked")
        List<String> privUsers = (List<String>) nodeProps.get(PrivilegeModel.Props.USER);
        @SuppressWarnings("unchecked")
        List<String> privGroups = (List<String>) nodeProps.get(PrivilegeModel.Props.GROUP);
        RepoUtil.validateSameSize(privUsers, privGroups, "users", "groups");
        Map<String, Object> toNodeUserGroupMapping = RepoUtil.toStringProperties(nodeProps);

        { // add all permissions to the user of document that is owner of other document
            String otherDocOwner = (String) nodeService.getProperty(fromRef, DocumentCommonModel.Props.OWNER_ID);
            if (StringUtils.isNotBlank(otherDocOwner)) {
                privilegeService.addPrivilege(toRef, toNodeUserGroupMapping, null, otherDocOwner, Privileges.VIEW_DOCUMENT_META_DATA);
            }
        }
        PrivilegeMappings privMappingsFrom = privilegeService.getPrivilegeMappings(fromRef);
        Collection<UserPrivileges> fromNodeUserPrivileges = privMappingsFrom.getPrivilegesByUsername().values();
        if (!fromNodeUserPrivileges.isEmpty()) {
            for (UserPrivileges userPrivs : fromNodeUserPrivileges) {
                Set<String> staticPrivileges = userPrivs.getStaticPrivileges();
                if (staticPrivileges.contains(DocumentCommonModel.Privileges.EDIT_DOCUMENT_META_DATA)) {
                    privilegeService.addPrivilege(toRef, toNodeUserGroupMapping, DocumentCommonModel.Types.DOCUMENT, userPrivs.getUserName(),
                            DocumentCommonModel.Privileges.VIEW_DOCUMENT_META_DATA);
                }
            }
        }
        nodeService.addProperties(toRef, RepoUtil.toQNameProperties(toNodeUserGroupMapping));
    }

    // START: getters / setters

    public void setRegisterService(RegisterService registerService) {
        this.registerService = registerService;
    }

    public void setVolumeService(VolumeService volumeService) {
        this.volumeService = volumeService;
    }

    public void setSeriesService(SeriesService seriesService) {
        this.seriesService = seriesService;
    }

    public void setDocumentTypeService(DocumentTypeService documentTypeService) {
        this.documentTypeService = documentTypeService;
    }

    public void setDocumentTemplateService(DocumentTemplateService documentTemplateService) {
        this.documentTemplateService = documentTemplateService;
    }

    public void setPrivilegeService(PrivilegeService privilegeService) {
        this.privilegeService = privilegeService;
    }

    public void setAuthorityService(AuthorityService authorityService) {
        this.authorityService = authorityService;
    }

    public void setPermissionService(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    public void setDictionaryService(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    public void setNamespaceService(NamespaceService namespaceService) {
        this.namespaceService = namespaceService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setCopyService(CopyService copyService) {
        this.copyService = copyService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setFileFolderService(FileFolderService fileFolderService) {
        this.fileFolderService = fileFolderService;
    }

    public void setContentService(ContentService contentService) {
        this.contentService = contentService;
    }

    public void setFileService(FileService fileService) {
        this.fileService = fileService;
    }

    public void setSignatureService(SignatureService signatureService) {
        this.signatureService = signatureService;
    }

    public void setWorkflowService(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    public void setDocumentLogService(DocumentLogService documentLogService) {
        this.documentLogService = documentLogService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void setSendOutService(SendOutService sendOutService) {
        this.sendOutService = sendOutService;
    }

    /*
     * To break circular dependency
     */
    private AdrService getAdrService() {
        if (_adrService == null) {
            _adrService = (AdrService) beanFactory.getBean(AdrService.BEAN_NAME);
        }
        return _adrService;
    }

    protected CaseService getCaseService() {
        if (_caseService == null) {
            _caseService = (CaseService) beanFactory.getBean(CaseService.BEAN_NAME);
        }
        return _caseService;
    }

    protected DocumentSearchService getDocumentSearchService() {
        if (_documentSearchService == null) {
            _documentSearchService = (DocumentSearchService) beanFactory.getBean(DocumentSearchService.BEAN_NAME);
        }
        return _documentSearchService;
    }

    protected MaaisService getMaaisService() {
        if (_maaisService == null) {
            _maaisService = (MaaisService) beanFactory.getBean(MaaisService.BEAN_NAME);
        }
        return _maaisService;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    public void setFromDvkXPath(String fromDvkXPath) {
        this.fromDvkXPath = fromDvkXPath;
    }

    public void setIncomingEmailPath(String incomingEmailPath) {
        this.incomingEmailPath = incomingEmailPath;
    }

    public void setReceivedInvoicePath(String receivedInvoicePath) {
        this.receivedInvoicePath = receivedInvoicePath;
    }

    @Override
    public String getReceivedInvoicePath() {
        return receivedInvoicePath;
    }

    public void setSentEmailPath(String sentEmailPath) {
        this.sentEmailPath = sentEmailPath;
    }

    // END: getters / setters

}
