package ee.webmedia.alfresco.document.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.faces.context.FacesContext;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.i18n.I18NUtil;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.content.transform.ContentTransformer;
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
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.util.EqualsHelper;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.ui.common.Utils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrBuilder;
import org.hibernate.StaleObjectStateException;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.document.associations.model.DocAssocInfo;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.document.log.service.DocumentLogService;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentParentNodesVO;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;
import ee.webmedia.alfresco.document.search.model.DocumentSearchModel;
import ee.webmedia.alfresco.document.sendout.model.SendInfo;
import ee.webmedia.alfresco.document.sendout.service.SendOutService;
import ee.webmedia.alfresco.document.sendout.web.DocumentSendOutDialog;
import ee.webmedia.alfresco.document.type.model.DocumentType;
import ee.webmedia.alfresco.document.type.service.DocumentTypeService;
import ee.webmedia.alfresco.functions.model.FunctionsModel;
import ee.webmedia.alfresco.imap.model.ImapModel;
import ee.webmedia.alfresco.orgstructure.service.OrganizationStructureService;
import ee.webmedia.alfresco.register.model.Register;
import ee.webmedia.alfresco.register.service.RegisterService;
import ee.webmedia.alfresco.series.model.Series;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.series.service.SeriesService;
import ee.webmedia.alfresco.signature.exception.SignatureException;
import ee.webmedia.alfresco.signature.model.SignatureDigest;
import ee.webmedia.alfresco.signature.service.SignatureService;
import ee.webmedia.alfresco.template.service.DocumentTemplateService;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.FilenameUtil;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.utils.UserUtil;
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
public class DocumentServiceImpl implements DocumentService {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DocumentServiceImpl.class);

    private DictionaryService dictionaryService;
    private NamespaceService namespaceService;
    private NodeService nodeService;
    private GeneralService generalService;
    private DocumentTypeService documentTypeService;
    private DocumentTemplateService documentTemplateService;
    private RegisterService registerService;
    private VolumeService volumeService;
    private SeriesService seriesService;
    private FileFolderService fileFolderService;
    private ContentService contentService;
    private FileService fileService;
    private SignatureService signatureService;
    private WorkflowService workflowService;
    private DocumentLogService documentLogService;
    private UserService userService;
    private OrganizationStructureService organizationStructureService;
    private SendOutService sendOutService;

    private String fromDvkXPath;
    private String incomingEmailPath;

    // doesn't need to be synchronized, because it is not modified during runtime
    private final Map<QName/* nodeType/nodeAspect */, PropertiesModifierCallback> creationPropertiesModifierCallbacks = new LinkedHashMap<QName, PropertiesModifierCallback>();

    private static final String REGISTRATION_INDIVIDUALIZING_NUM_SUFFIX = "-1";
    private static final DateFormat userDateFormat = new SimpleDateFormat("dd.MM.yyyy");

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
        return createDocument(documentTypeId, parentRef, properties, false);
    }
    
    private Node createDocument(QName documentTypeId, NodeRef parentRef, Map<QName, Serializable> properties, boolean withoutPropModifyingCallbacks) {

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

        NodeRef document = nodeService.createNode(parentRef, DocumentCommonModel.Assocs.DOCUMENT, DocumentCommonModel.Assocs.DOCUMENT //
                , documentTypeId, properties).getChildRef();

        final Node documentNode = getDocument(document);
        // first iterate over callbacks to be able to predict in which order callbacks will be called (that is registration order).
        if (!withoutPropModifyingCallbacks) {
            for (QName callbackAspect : creationPropertiesModifierCallbacks.keySet()) {
                for (QName docAspect : aspects) {
                    if (dictionaryService.isSubClass(docAspect, callbackAspect)) {
                        PropertiesModifierCallback callback = creationPropertiesModifierCallbacks.get(docAspect);
                        callback.doWithNode(documentNode);
                    }
                }
            }
        }
        return documentNode;
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
    public void changeTypeInMemory(Node node, QName newType) {
        node.setType(newType);
        Set<QName> aspects = node.getAspects();
        aspects.clear();
        aspects.addAll(generalService.getDefaultAspects(newType));
        
        fillDefaultProperties(node);
    }

    @Override
    public void endDocument(NodeRef documentRef) {
        if (log.isDebugEnabled()) log.debug("Ending document:" + documentRef);
        Assert.notNull(documentRef, "Reference to document must be provided");
        nodeService.setProperty(documentRef, DocumentCommonModel.Props.DOC_STATUS, DocumentStatus.FINISHED.getValueName());
        documentLogService.addDocumentLog(documentRef, I18NUtil.getMessage("document_log_status_proceedingFinish"));
        if (log.isDebugEnabled()) log.debug("Document ended");
    }

    @Override
    public void reopenDocument(NodeRef documentRef) {
        if (log.isDebugEnabled()) log.debug("Reopening document:" + documentRef);
        Assert.notNull(documentRef, "Reference to document must be provided");
        nodeService.setProperty(documentRef, DocumentCommonModel.Props.DOC_STATUS, DocumentStatus.WORKING.getValueName());
        if (log.isDebugEnabled()) log.debug("Document reopened");
    }

    private void fillDefaultProperties(Node node) {
        Map<String, Object> props = node.getProperties();
        if (node.hasAspect(DocumentCommonModel.Aspects.RECIPIENT)) {
            @SuppressWarnings("unchecked")
            List<String> list1 = (List<String>)props.get(DocumentCommonModel.Props.RECIPIENT_NAME);
            list1 = DocumentSendOutDialog.newListIfNull(list1, true);
            
            @SuppressWarnings("unchecked")
            List<String> list2 = (List<String>)props.get(DocumentCommonModel.Props.RECIPIENT_EMAIL);
            list2 = DocumentSendOutDialog.newListIfNull(list2, true);
            
            props.put(DocumentCommonModel.Props.RECIPIENT_NAME.toString(), list1);
            props.put(DocumentCommonModel.Props.RECIPIENT_EMAIL.toString(), list2);
        }
        if (node.hasAspect(DocumentCommonModel.Aspects.ADDITIONAL_RECIPIENT)) {
            @SuppressWarnings("unchecked")
            List<String> list1 = (List<String>)props.get(DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_NAME);
            list1 = DocumentSendOutDialog.newListIfNull(list1, true);
            
            @SuppressWarnings("unchecked")
            List<String> list2 = (List<String>)props.get(DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_EMAIL);
            list2 = DocumentSendOutDialog.newListIfNull(list2, true);
            
            props.put(DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_NAME.toString(), list1);
            props.put(DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_EMAIL.toString(), list2);
        }
    }

    @Override
    public Node updateDocument(final Node docNode) {
        final NodeRef docNodeRef = docNode.getNodeRef();
        final Map<String, Object> docProps = docNode.getProperties();
        // Prepare existingParentNode and targetParentRef properties
        final String volumeNodeRef = (String) docProps.get(TransientProps.VOLUME_NODEREF);
        final String caseNodeRef = (String) docProps.get(TransientProps.CASE_NODEREF);
        final NodeRef targetParentRef;
        Node existingParentNode = null;
        if (StringUtils.isNotBlank(caseNodeRef)) {
            targetParentRef = new NodeRef(caseNodeRef);
            existingParentNode = getCaseByDocument(docNodeRef);
            if (existingParentNode == null) { // moving from volume to case?
                existingParentNode = getVolumeByDocument(docNodeRef);
            }
        } else {
            targetParentRef = new NodeRef(volumeNodeRef);
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
        NodeRef series = nodeService.getPrimaryParent(new NodeRef(volumeNodeRef)).getParentRef();
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
        docProps.put(DocumentCommonModel.Props.FUNCTION.toString(), function);
        docProps.put(DocumentCommonModel.Props.SERIES.toString(), series);

        // If document is updated for the first time, add SEARCHABLE aspect to document and it's children files.
        if (!nodeService.hasAspect(docNodeRef, DocumentCommonModel.Aspects.SEARCHABLE)) {
            nodeService.addAspect(docNodeRef, DocumentCommonModel.Aspects.SEARCHABLE, null);
            docProps.put(DocumentCommonModel.Props.FILE_NAMES.toString(), getSearchableFileNames(docNodeRef));
            docProps.put(DocumentCommonModel.Props.FILE_CONTENTS.toString(), getSearchableFileContents(docNodeRef));
        }
        if (docNode.hasAspect(DocumentSpecificModel.Aspects.COMPLIENCE)) {
            Date complienceDate = (Date) docProps.get(DocumentSpecificModel.Props.COMPLIENCE_DATE);
            if (complienceDate != null) {
                docProps.put(DocumentCommonModel.Props.DOC_STATUS.toString(), DocumentStatus.FINISHED.getValueName());
            }
        }
        docProps.putAll(getSearchableOtherProps(docNode));

        saveChildNodes(docNode);

        // add any associations added in the UI
        generalService.saveAddedAssocs(docNode);

        // Write document properties to repository
        // XXX If owner is changed to another user, then after this call we don't have permissions any more to write document properties
        generalService.setPropertiesIgnoringSystem(docNodeRef, docProps);
        // ==================================================================================================================================
        // ==================================================================================================================================
        // XXX If owner is changed to another user, then after previous call we don't have permissions any more to write document properties

        
        if (existingParentNode == null || !targetParentRef.equals(existingParentNode.getNodeRef())) {
            // was not saved (under volume nor case) or saved, but parent (volume or case) must be changed
            
            try {
                // Moving is executed with System user rights, because this is not appropriate to implement in permissions model
                AuthenticationUtil.runAs(new RunAsWork<NodeRef>() {
                    @Override
                    public NodeRef doWork() throws Exception {
                        NodeRef newDocNodeRef = nodeService.moveNode(docNodeRef, targetParentRef //
                                , DocumentCommonModel.Assocs.DOCUMENT, DocumentCommonModel.Assocs.DOCUMENT).getChildRef();
                        if (!newDocNodeRef.equals(docNodeRef)) {
                            throw new RuntimeException("NodeRef changed while moving");
                        }
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
                    final Map<String, Object> props = docProps;
                    final String existingRegNr = (String) props.get(DocumentCommonModel.Props.REG_NUMBER.toString());
                    if (StringUtils.isNotBlank(existingRegNr)) {
                        registerDocument(docNode, true);
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
                        
                        if (!baseVol.getNodeRefAsString().equals(volumeNodeRef)) {
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
        }
        makeChildNodesSearchable(docNodeRef);
        return getDocument(docNodeRef);
    }

    private void saveChildNodes(Node docNode) {
        final boolean isErrandDocAbroad = DocumentSubtypeModel.Types.ERRAND_ORDER_ABROAD.equals(docNode.getType());
        final boolean isErrandDocDomestic = DocumentSubtypeModel.Types.ERRAND_APPLICATION_DOMESTIC.equals(docNode.getType());
        final boolean isTraining = DocumentSubtypeModel.Types.TRAINING_APPLICATION.equals(docNode.getType());
        generalService.saveRemovedChildAssocs(docNode);
        if (isErrandDocAbroad || isErrandDocDomestic || isTraining) {
            final QName applicantAssoc;
            final QName errandAssocType;
            if (isErrandDocAbroad) {
                applicantAssoc = DocumentSpecificModel.Assocs.ERRAND_ORDER_APPLICANTS_ABROAD;
                errandAssocType = DocumentSpecificModel.Assocs.ERRAND_ABROAD;
            } else if (isErrandDocDomestic) {
                applicantAssoc = DocumentSpecificModel.Assocs.ERRAND_APPLICATION_DOMESTIC_APPLICANTS;
                errandAssocType = DocumentSpecificModel.Assocs.ERRAND_DOMESTIC;
            } else if (isTraining) {
                applicantAssoc = DocumentSpecificModel.Assocs.TRAINING_APPLICATION_APPLICANTS;
                errandAssocType = null;
            } else {
                throw new RuntimeException("Unimplemented");
            }
            
            final List<Node> applicants = docNode.getAllChildAssociations(applicantAssoc);
            if(applicants == null || applicants.size() == 0) {
                return;
            }
            for (int i = 0; i < applicants.size(); i++) {
                Node applicantNode = applicants.get(i);
                generalService.saveRemovedChildAssocs(applicantNode);
                Node newApplicantNode = saveChildNode(docNode, applicantNode, applicantAssoc, applicants, i);
                final List<Node> errandNodes = errandAssocType == null ? null : applicantNode.getAllChildAssociations(errandAssocType);
                if(newApplicantNode == null) {
                    generalService.setPropertiesIgnoringSystem(applicantNode.getNodeRef(), applicantNode.getProperties());
                } else {
                    applicantNode = newApplicantNode;
                }
                if(errandAssocType == null) {
                    continue;
                }
                for (int j = 0; j < errandNodes.size(); j++) {
                    Node errandNode = errandNodes.get(j);
                    generalService.saveRemovedChildAssocs(errandNode);
                    try {
                        Node newErrandNode = saveChildNode(applicantNode, errandNode, errandAssocType, errandNodes, j);
                        if(newErrandNode == null) {
                            generalService.setPropertiesIgnoringSystem(errandNode.getNodeRef(), errandNode.getProperties());
                        }
                    } catch (AlfrescoRuntimeException e) {
                        final String msg = "failed to set properties for nodeRef="+errandNode.getNodeRef()+"; properties: "+errandNode.getProperties();
                        log.error(msg, e);
                        throw e;
                    }
                }
            }
        }
    }

    private void makeChildNodesSearchable(final NodeRef docRef) {
        String childProps = getChildNodesPropsForIndexing(docRef, new StringBuilder()).toString();
        nodeService.setProperty(docRef, DocumentCommonModel.Props.SEARCHABLE_SUB_NODE_PROPERTIES, childProps);
    }

    private Node saveChildNode(Node docNode, Node applicantNode, final QName assocTypeAndNameQName, final List<Node> applicants, int i) {
        if (applicantNode instanceof WmNode) {
            WmNode wmNode = (WmNode) applicantNode;
            if(wmNode.isUnsaved()) {
                final Map<QName, Serializable> props = RepoUtil.toQNameProperties(applicantNode.getProperties());
                final ChildAssociationRef applicantNode2 = nodeService.createNode(docNode.getNodeRef(), assocTypeAndNameQName, assocTypeAndNameQName, applicantNode.getType(), props);
                final Node newApplicantNode = generalService.fetchNode(applicantNode2.getChildRef());
                applicants.remove(i);
                applicants.add(i, newApplicantNode);
                return newApplicantNode;
            }
        }
        return null;
    }

    private StringBuilder getChildNodesPropsForIndexing(NodeRef parentRef, StringBuilder sb) {
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
        if (nodeService.hasAspect(document, DocumentCommonModel.Aspects.SEARCHABLE)) {
            Map<QName, Serializable> props = new HashMap<QName, Serializable>();
            props.put(DocumentCommonModel.Props.FILE_NAMES, (Serializable) getSearchableFileNames(document));
            props.put(DocumentCommonModel.Props.FILE_CONTENTS, getSearchableFileContents(document));
            nodeService.addProperties(document, props);
        }
    }

    private List<String> getSearchableFileNames(NodeRef document) {
        List<FileInfo> files = fileFolderService.listFiles(document);
        List<String> fileNames = new ArrayList<String>(files.size());
        for (FileInfo file : files) {
            fileNames.add(file.getName());
        }
        return fileNames;
    }

    private ContentData getSearchableFileContents(NodeRef document) {
        List<FileInfo> files = fileFolderService.listFiles(document);
        if (files.size() == 0) {
            return null;
        }
        ContentWriter allWriter = contentService.getWriter(document, DocumentCommonModel.Props.FILE_CONTENTS, false);
        allWriter.setMimetype(MimetypeMap.MIMETYPE_TEXT_PLAIN);
        allWriter.setEncoding("UTF-8");
        OutputStream allOutput = allWriter.getContentOutputStream();

        for (FileInfo file : files) {
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
    public Node createFollowUp(QName docType, NodeRef nodeRef) {
        return createFollowUpDocumentFromExisting(docType, nodeRef);
    }
    
    private Node createFollowUpDocumentFromExisting(QName docType, NodeRef nodeRef) {
        Node followUpDoc = createDocument(docType);
        Node doc = getDocument(nodeRef);
        
        Map<String, Object> props = followUpDoc.getProperties();
        Map<String, Object> docProps = doc.getProperties();
        /** All types share common properties */
        Set<String> copiedProps = DocumentPropertySets.commonProperties;
        
        /** Substitute and choose properties */
        if (DocumentSubtypeModel.Types.INSTRUMENT_OF_DELIVERY_AND_RECEIPT.equals(docType) && DocumentSubtypeModel.Types.CONTRACT_SIM.equals(doc.getType())) {
            props.put(DocumentSpecificModel.Props.SECOND_PARTY_REG_NUMBER.toString(), docProps.get(
                    DocumentSpecificModel.Props.SECOND_PARTY_CONTRACT_NUMBER));
            props.put(DocumentSpecificModel.Props.SECOND_PARTY_REG_DATE.toString(), docProps.get(
                    DocumentSpecificModel.Props.SECOND_PARTY_CONTRACT_DATE));
        }
        
        /** Copy Properties */
        for (Map.Entry<String, Object> prop : docProps.entrySet()) {
            if (copiedProps.contains(prop.getKey())) {
                props.put(prop.getKey(), prop.getValue());
            }
        }
        
        /** Copy Ancestors (function, series, volume, case) */
        setTransientProperties(followUpDoc, getAncestorNodesByDocument(doc.getNodeRef()));

        /** Add association from new to original doc */
        nodeService.createAssociation(followUpDoc.getNodeRef(), doc.getNodeRef(), DocumentCommonModel.Assocs.DOCUMENT_FOLLOW_UP);

        if (log.isDebugEnabled()) {
            log.debug("Created followUp: " + docType.getLocalName() + " from " + doc.getType().getLocalName());
        }
        
        return followUpDoc;
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
        if (DocumentSubtypeModel.Types.OUTGOING_LETTER.equals(docType)) {
            @SuppressWarnings("unchecked")
            List<String> recipientNames = (List<String>) props.get(DocumentCommonModel.Props.RECIPIENT_NAME);
            if (recipientNames.size() > 0) {
                recipientNames.remove(0);
            }
            recipientNames.add((String) docProps.get(DocumentSpecificModel.Props.SENDER_DETAILS_NAME.toString()));
            @SuppressWarnings("unchecked")
            List<String> recipientEmails = (List<String>) props.get(DocumentCommonModel.Props.RECIPIENT_EMAIL);
            if (recipientEmails.size() > 0) {
                recipientEmails.remove(0);
            }
            recipientEmails.add((String) docProps.get(DocumentSpecificModel.Props.SENDER_DETAILS_EMAIL.toString()));
            copiedProps = DocumentPropertySets.incomingAndOutgoingLetterProperties;

        } else if (DocumentSubtypeModel.Types.INSTRUMENT_OF_DELIVERY_AND_RECEIPT.equals(docType)) {

            props.put(DocumentSpecificModel.Props.SECOND_PARTY_REG_NUMBER.toString(), docProps.get(
                    DocumentSpecificModel.Props.SECOND_PARTY_CONTRACT_NUMBER));
            props.put(DocumentSpecificModel.Props.SECOND_PARTY_REG_DATE.toString(), docProps.get(
                    DocumentSpecificModel.Props.SECOND_PARTY_CONTRACT_DATE));
            copiedProps = DocumentPropertySets.commonProperties;
        } else {
            throw new RuntimeException("Unexpected docType: "+docType);
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
        nodeService.createAssociation(replyDoc.getNodeRef(), doc.getNodeRef(), DocumentCommonModel.Assocs.DOCUMENT_REPLY);

        if (log.isDebugEnabled()) {
            log.debug("Created reply: " + docType.getLocalName() + " from " + doc.getType().getLocalName());
        }
        return replyDoc;
    }


    @Override
    public Node copyDocument(NodeRef nodeRef) {
        Node doc = getDocument(nodeRef);
        // create document without calling propertiesModifierCallbacks
        Node copiedDoc = createDocument(doc.getType(), null, null, true);

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
        copiedDoc.getProperties().put(DocumentCommonModel.Props.DOC_STATUS.toString(), DocumentStatus.WORKING.getValueName());

        if (log.isDebugEnabled())
            log.debug("Copied document: " + copiedDoc.toString());
        return copiedDoc;
    }

    @Override
    public void setTransientProperties(Node document, DocumentParentNodesVO documentParentNodesVO) {
        Node functionNode = documentParentNodesVO.getFunctionNode();
        Node seriesNode = documentParentNodesVO.getSeriesNode();
        Node volumeNode = documentParentNodesVO.getVolumeNode();
        Node caseNode = documentParentNodesVO.getCaseNode();

        // put props with empty values if missing, otherwise use existing values
        final Map<String, Object> props = document.getProperties();
        props.put(TransientProps.FUNCTION_NODEREF, functionNode != null ? functionNode.getNodeRef().toString() : null);
        props.put(TransientProps.SERIES_NODEREF, seriesNode != null ? seriesNode.getNodeRef().toString() : null);
        props.put(TransientProps.VOLUME_NODEREF, volumeNode != null ? volumeNode.getNodeRef().toString() : null);
        props.put(TransientProps.CASE_NODEREF, caseNode != null ? caseNode.getNodeRef().toString() : null);

        // add labels
        String volumeLbl = volumeNode != null ? volumeNode.getProperties().get(VolumeModel.Props.MARK).toString() //
                + " " + volumeNode.getProperties().get(VolumeModel.Props.TITLE).toString() : " ";
        String seriesLbl = seriesNode != null ? seriesNode.getProperties().get(SeriesModel.Props.SERIES_IDENTIFIER).toString() //
                + " " + seriesNode.getProperties().get(SeriesModel.Props.TITLE).toString() : " ";
        String functionLbl = functionNode != null ? functionNode.getProperties().get(FunctionsModel.Props.MARK).toString() //
                + " " + functionNode.getProperties().get(FunctionsModel.Props.TITLE).toString() : " ";
        props.put(TransientProps.FUNCTION_LABEL, functionLbl);
        props.put(TransientProps.SERIES_LABEL, seriesLbl);
        props.put(TransientProps.VOLUME_LABEL, volumeLbl);
    }

    @Override
    public void deleteDocument(NodeRef nodeRef) {
        log.debug("Deleting document: " + nodeRef);
        List<AssociationRef> assocs = nodeService.getTargetAssocs(nodeRef, RegexQNamePattern.MATCH_ALL);
        assocs.addAll(nodeService.getSourceAssocs(nodeRef, RegexQNamePattern.MATCH_ALL));
        for (AssociationRef assoc : assocs) {
            nodeService.removeAssociation(assoc.getSourceRef(), assoc.getTargetRef(), assoc.getTypeQName());
        }
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
    public List<Document> getReplyOrFollowUpDocuments(NodeRef base) {
        List<Document> docs = new ArrayList<Document>();
        // reply and follow up are source associations regarding the base document
        final List<AssociationRef> sourceAssocs = nodeService.getSourceAssocs(base, RegexQNamePattern.MATCH_ALL);
        for (AssociationRef srcAssocRef : sourceAssocs) {
            if (DocumentCommonModel.Assocs.DOCUMENT_FOLLOW_UP.equals(srcAssocRef.getTypeQName()) ||
                DocumentCommonModel.Assocs.DOCUMENT_REPLY.equals(srcAssocRef.getTypeQName())) {
         
                Document doc = getDocumentByNodeRef(srcAssocRef.getSourceRef());
                docs.add(doc);
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
    public void deleteAssoc(NodeRef sourceNodeRef, NodeRef targetNodeRef, QName assocQName) {
        if(assocQName == null) {
            assocQName = DocumentCommonModel.Assocs.DOCUMENT_2_DOCUMENT;
        }
        log.debug("Deleting " + assocQName + " association from document " + sourceNodeRef + " that points to " + targetNodeRef);
        if(assocQName.equals(CaseModel.Associations.CASE_DOCUMENT)) {
            nodeService.removeAssociation(targetNodeRef, sourceNodeRef, assocQName);
        } else {
            nodeService.removeAssociation(sourceNodeRef, targetNodeRef, assocQName);
        }
    }

    @Override
    public List<Document> getIncomingEmails() {
        NodeRef incomingNodeRef  = generalService.getNodeRef(incomingEmailPath);
        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(incomingNodeRef);
        List<Document> docs = new ArrayList<Document>();
        for (ChildAssociationRef assocRef : childAssocs) {
            final Node doc = getDocument(assocRef.getChildRef());
            docs.add(0, getDocumentByNodeRef(doc.getNodeRef())); // flips the list, so newest are first
            
        }
        return docs;
    }
    
    @Override
    public Document getDocumentByNodeRef(NodeRef docRef) {
        final Node documentNode = getDocument(docRef);
        DocumentType documentType = documentTypeService.getDocumentType(documentNode.getType());
        Document doc = new Document(documentNode, documentType);
        if (log.isDebugEnabled()) {
            log.debug("Document: " + doc);
        }
        return doc;
    }

    @Override
    public void addPropertiesModifierCallback(QName qName, PropertiesModifierCallback propertiesModifierCallback) {
        this.creationPropertiesModifierCallbacks.put(qName, propertiesModifierCallback);
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
        return (String) nodeService.getProperty(document, DocumentCommonModel.Props.OWNER_ID);
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
        properties.put(DocumentCommonModel.Props.OWNER_ID, personProps.get(ContentModel.PROP_USERNAME));
        properties.put(DocumentCommonModel.Props.OWNER_NAME, UserUtil.getPersonFullName1(personProps));
        properties.put(DocumentCommonModel.Props.OWNER_JOB_TITLE, personProps.get(ContentModel.PROP_JOBTITLE));
        String orgstructName = organizationStructureService.getOrganizationStructure((String) personProps.get(ContentModel.PROP_ORGID));
        properties.put(DocumentCommonModel.Props.OWNER_ORG_STRUCT_UNIT, orgstructName);
        properties.put(DocumentCommonModel.Props.OWNER_EMAIL, personProps.get(ContentModel.PROP_EMAIL));
        properties.put(DocumentCommonModel.Props.OWNER_PHONE, personProps.get(ContentModel.PROP_TELEPHONE));
        generalService.setPropertiesIgnoringSystem(properties, document);
    }

    @Override
    public boolean isSaved(NodeRef nodeRef) {
        final Node parentVolume = getVolumeByDocument(nodeRef);
        return parentVolume != null ? true : null != generalService.getParentWithType(nodeRef, CaseModel.Types.CASE);
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
    public boolean isRegistered(Node docNode) {
        final String existingRegNr = (String) docNode.getProperties().get(DocumentCommonModel.Props.REG_NUMBER.toString());
        return StringUtils.isNotBlank(existingRegNr);
    }

    public boolean isRegistered(NodeRef docRef) {
        final String existingRegNr = (String) nodeService.getProperty(docRef, DocumentCommonModel.Props.REG_NUMBER);
        return StringUtils.isNotBlank(existingRegNr);
    }
    
    @Override
    public void registerDocumentIfNotRegistered(NodeRef document, boolean logging) {
        Node docNode = getDocument(document);
        if (logging) {
            docNode.getProperties().put("{temp}logging", "true");
        }
        if (!isRegistered(docNode)) {
            registerDocument(docNode);
        }
        if (logging) {
            docNode.getProperties().remove("{temp}logging");
        }
    }

    @Override
    public Node registerDocument(Node docNode) {
        return registerDocument(docNode, false);
    }
    
    public Node registerDocument(Node docNode, boolean isRelocating) {
        final Map<String, Object> props = docNode.getProperties();
        if (isRegistered(docNode) && !isRelocating) {
            throw new RuntimeException("Document already registered! docNode=" + docNode);
        }
        // only register when no existingRegNr or when relocating
        final String volumeNodeRef = (String) props.get(TransientProps.VOLUME_NODEREF);
        final String seriesNodeRef = (String) props.get(TransientProps.SERIES_NODEREF);
        final String caseNodeRef = (String) props.get(TransientProps.CASE_NODEREF);
        final NodeRef docRef = docNode.getNodeRef();
        final String volumeMark = volumeService.getVolumeByNodeRef(volumeNodeRef).getVolumeMark();
        final DocumentType documentType = documentTypeService.getDocumentType(docNode.getType());
        final List<AssociationRef> replyAssocs = nodeService.getTargetAssocs(docRef, DocumentCommonModel.Assocs.DOCUMENT_REPLY);
        final boolean isReplyOrFollowupDoc = isReplyOrFollowupDoc(docRef, replyAssocs);
        String regNumber = null;
        if (!isReplyOrFollowupDoc) {
            log.debug("Starting to register initialDocument, docRef="+docRef);
            // registration of initial document ("Algatusdokument")
            final Series series = seriesService.getSeriesByNodeRef(seriesNodeRef);
            final Map<String, Object> serProps = series.getNode().getProperties();
            Integer registerId = (Integer) serProps.get(SeriesModel.Props.REGISTER.toString());
            boolean individualizingNumbers = (Boolean) serProps.get(SeriesModel.Props.INDIVIDUALIZING_NUMBERS.toString());

            registerService.increaseCount(registerId); // increase before geting the register
            Register register = registerService.getRegister(registerId);
            // compose regNumber
            regNumber = volumeMark + "/" + register.getPrefix() + (register.getCounter()) + register.getSuffix();
            if (individualizingNumbers) {
                regNumber += REGISTRATION_INDIVIDUALIZING_NUM_SUFFIX;
            }
        } else { // registration of reply/followUp("Järg- või vastusdokument")
            log.debug("Starting to register "+(replyAssocs.size() > 0 ? "reply" : "followUp")+" document, docRef="+docRef);
            final Node initialDoc = getDocument(getInitialDocument(docRef));
            final Map<String, Object> initDocProps = initialDoc.getProperties();
            final String initDocRegNr = (String) initDocProps.get(DocumentCommonModel.Props.REG_NUMBER.toString());
            if (StringUtils.isNotBlank(initDocRegNr)) {
                final String initDocSeriesNodeRef = (String) initDocProps.get(TransientProps.SERIES_NODEREF.toString());

                final Series series = seriesService.getSeriesByNodeRef(initDocSeriesNodeRef);
                final Map<String, Object> serProps = series.getNode().getProperties();
                boolean individualizingNumbers = (Boolean) serProps.get(SeriesModel.Props.INDIVIDUALIZING_NUMBERS.toString());
                if (!individualizingNumbers) {
                    regNumber = initDocRegNr;
                } else { // add also individualizing number to regNr
                    final RegNrHolder initDocRegNrHolder = new RegNrHolder(initDocRegNr);
                    if (initDocRegNrHolder.getIndividualizingNr() != null) {
                        final NodeRef initDocParentRef = new NodeRef(caseNodeRef != null ? caseNodeRef : volumeNodeRef);
                        int maxIndivNr = initDocRegNrHolder.getIndividualizingNr();
                        for (Document anotherDoc : getAllDocumentsByParentNodeRef(initDocParentRef)) {
                            if (!docRef.equals(anotherDoc.getNode().getNodeRef())) {
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
                if (StringUtils.isNotBlank(regNumber) && documentType.getId().equals(DocumentSubtypeModel.Types.INSTRUMENT_OF_DELIVERY_AND_RECEIPT)) {
                    if (replyAssocs.size() > 0) {
                        final NodeRef contractDocRef = replyAssocs.get(0).getTargetRef();
                        Date finalTermOfDeliveryAndReceiptDate = (Date) nodeService.getProperty(contractDocRef,
                                DocumentSpecificModel.Props.FINAL_TERM_OF_DELIVERY_AND_RECEIPT);
                        if (finalTermOfDeliveryAndReceiptDate == null) {
                            nodeService.setProperty(contractDocRef, DocumentSpecificModel.Props.FINAL_TERM_OF_DELIVERY_AND_RECEIPT, new Date());
                        }
                    }
                }
            }
        }
        if (StringUtils.isNotBlank(regNumber)) {
            props.put(DocumentCommonModel.Props.REG_NUMBER.toString(), regNumber);
            if (!isRelocating) {
                props.put(DocumentCommonModel.Props.REG_DATE_TIME.toString(), new Date());
            }
            if (!documentType.getId().equals(DocumentSubtypeModel.Types.INCOMING_LETTER)) {
                props.put(DocumentCommonModel.Props.DOC_STATUS.toString(), DocumentStatus.FINISHED.getValueName());
                documentLogService.addDocumentLog(docRef, I18NUtil.getMessage("document_log_status_proceedingFinish") //
                        , I18NUtil.getMessage("document_log_creator_dhs"));
            } else {
                if (StringUtils.isNotBlank((String) props.get("{temp}logging"))) {
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

    public void addDocAssocInfo(AssociationRef assocRef, boolean isSourceAssoc, ArrayList<DocAssocInfo> assocInfos) {
        DocAssocInfo assocInf = getDocAssocInfo(assocRef, isSourceAssoc);
        if(assocInf != null) {
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
            if(!assocInf.isCase()) {// document association, not case
                final Node otherDocNode = new Node(sourceRef);
                assocInf.setTitle((String) nodeService.getProperty(sourceRef, DocumentCommonModel.Props.DOC_NAME));
                assocInf.setType(documentTypeService.getDocumentType(otherDocNode.getType()).getName());
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
            assocInf.setTitle((String) otherDocProps.get(DocumentCommonModel.Props.DOC_NAME));
            assocInf.setType(documentTypeService.getDocumentType(otherDocNode.getType()).getName());
            assocInf.setNodeRef(assocRef.getTargetRef());
        }
        assocInf.setSource(isSourceAssoc);
        return assocInf;
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

    private NodeRef getInitialDocument(NodeRef nodeRef) {
        NodeRef sourceRef = getFirstTargetAssocRef(nodeRef, DocumentCommonModel.Assocs.DOCUMENT_FOLLOW_UP);
        if (sourceRef == null) {
            return getFirstTargetAssocRef(nodeRef, DocumentCommonModel.Assocs.DOCUMENT_REPLY);
        }
        return sourceRef;
    }

    private NodeRef getFirstTargetAssocRef(NodeRef sourceRef, QName assocQNamePattern) {
        List<AssociationRef> targetAssocs = nodeService.getTargetAssocs(sourceRef, assocQNamePattern);
        NodeRef targetRef = null;
        if(targetAssocs.size() > 0) {
            targetRef = targetAssocs.get(0).getTargetRef();
            if(targetAssocs.size() > 1) {
                log.warn("document with noderef '"+targetRef+"' has more than one '"+assocQNamePattern+"' relations!");
            }
        }
        return targetRef;
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
        nodeService.setProperty(nodeRef, DocumentCommonModel.Props.DOC_STATUS, DocumentStatus.STOPPED.getValueName());
        workflowService.stopAllCompoundWorkflows(nodeRef);
        documentLogService.addDocumentLog(nodeRef, I18NUtil.getMessage("document_log_status_proceedingStop"));
    }

	@Override
    public void continueDocumentPreceedingAndUpdateStatus(NodeRef nodeRef) {
        nodeService.setProperty(nodeRef, DocumentCommonModel.Props.DOC_STATUS, DocumentStatus.WORKING.getValueName());
        workflowService.continueAllCompoundWorkflows(nodeRef);
        documentLogService.addDocumentLog(nodeRef, I18NUtil.getMessage("document_log_status_proceedingContinue"));
    }

    @Override
    public void prepareDocumentSigning(final NodeRef document) {
        AuthenticationUtil.runAs(new RunAsWork<Object>() {
            @Override
            public Object doWork() throws Exception {
                // Register the document, if not already registered
                registerDocumentIfNotRegistered(document, false);
                documentTemplateService.updateGeneratedFilesOnRegistration(document);
                // Generate PDF-files for all the files that support it.
                fileService.transformActiveFilesToPdf(document);
                return null;
            }
        }, AuthenticationUtil.getSystemUserName());
    }
    
    @Override
    public SignatureDigest prepareDocumentDigest(NodeRef document, String certHex) throws SignatureException {
        SignatureDigest signatureDigest = null;
        NodeRef existingDdoc = checkExistingDdoc(document);
        if (existingDdoc != null) {
            signatureDigest = signatureService.getSignatureDigest(existingDdoc, certHex);
        } else {
            signatureDigest = signatureService.getSignatureDigest(getSignatureTaskActiveNodeRefs(document), certHex);
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
        final NodeRef document = task.getParent().getParent().getParent();
        final String filename = generateDdocFilename(document);
        AuthenticationUtil.runAs(new RunAsWork<Object>() {
            @Override
            public Object doWork() throws Exception {
                NodeRef existingDdoc = checkExistingDdoc(document);
                if (existingDdoc != null) {
                    signatureService.addSignature(existingDdoc, task.getSignatureDigest(), signatureHex);
                } else {
                    NodeRef ddoc = signatureService.createContainer(document, fileService.getAllActiveFilesNodeRefs(document), filename, task
                            .getSignatureDigest(), signatureHex);
                    documentLogService.addDocumentLog(document, I18NUtil.getMessage("document_log_status_fileAdded", filename));
                    fileService.setAllFilesInactiveExcept(document, ddoc);
                }
                return null;
            }
        }, AuthenticationUtil.getSystemUserName());
        workflowService.finishInProgressTask(task, 1);
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

        String existingRegNr = (String) docNode.getProperties().get(DocumentCommonModel.Props.REG_NUMBER.toString());
        sb.append(existingRegNr);

        Date existingRegDate = (Date) docNode.getProperties().get(DocumentCommonModel.Props.REG_DATE_TIME.toString());
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
        setCollectedProps(document, props, DocumentCommonModel.Props.SEARCHABLE_COST_MANAGER, DocumentSpecificModel.Props.COST_MANAGER);
        setCollectedProps(document, props, DocumentCommonModel.Props.SEARCHABLE_APPLICANT_NAME, DocumentSpecificModel.Props.APPLICANT_NAME, DocumentSpecificModel.Props.PROCUREMENT_APPLICANT_NAME);
        setCollectedProps(document, props, DocumentCommonModel.Props.SEARCHABLE_ERRAND_BEGIN_DATE, DocumentSpecificModel.Props.ERRAND_BEGIN_DATE);
        setCollectedProps(document, props, DocumentCommonModel.Props.SEARCHABLE_ERRAND_END_DATE, DocumentSpecificModel.Props.ERRAND_END_DATE);
        setCollectedProps(document, props, DocumentCommonModel.Props.SEARCHABLE_ERRAND_COUNTRY, DocumentSpecificModel.Props.ERRAND_COUNTRY);
        setCollectedProps(document, props, DocumentCommonModel.Props.SEARCHABLE_ERRAND_COUNTY, DocumentSpecificModel.Props.ERRAND_COUNTY);
        setCollectedProps(document, props, DocumentCommonModel.Props.SEARCHABLE_ERRAND_CITY, DocumentSpecificModel.Props.ERRAND_CITY);
        return props;
    }

    private void setCollectedProps(Node document, Map<String, Object> props, QName targetProp, QName ... sourceProps) {
        ArrayList<Serializable> results = collectProperties(document, sourceProps);
        log.debug("Collected properties " + targetProp.toPrefixString(namespaceService) + " " + results);
        props.put(targetProp.toString(), results);
    }

    private ArrayList<Serializable> collectProperties(Node node, QName ... propNames) {
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
    // ==================== PROCESS EXTENDED SEARCH RESULTS ===================
    // ========================================================================

    private static Map<QName/*searchable*/,List<QName>/*filter*/> searchableToFilter = new HashMap<QName, List<QName>>();
    private static Map<QName/*searchable*/, List<QName>/*document*/> searchableToDocument = new HashMap<QName, List<QName>>();
    static {
        searchableToFilter.put(DocumentCommonModel.Props.SEARCHABLE_COST_MANAGER, Arrays.asList(DocumentSearchModel.Props.COST_MANAGER));
        searchableToFilter.put(DocumentCommonModel.Props.SEARCHABLE_APPLICANT_NAME, Arrays.asList(DocumentSearchModel.Props.APPLICANT_NAME));
        searchableToFilter.put(DocumentCommonModel.Props.SEARCHABLE_ERRAND_BEGIN_DATE, Arrays.asList(DocumentSearchModel.Props.ERRAND_BEGIN_DATE_BEGIN, DocumentSearchModel.Props.ERRAND_BEGIN_DATE_END));
        searchableToFilter.put(DocumentCommonModel.Props.SEARCHABLE_ERRAND_END_DATE, Arrays.asList(DocumentSearchModel.Props.ERRAND_END_DATE_BEGIN, DocumentSearchModel.Props.ERRAND_END_DATE_END));
        searchableToFilter.put(DocumentCommonModel.Props.SEARCHABLE_ERRAND_COUNTRY, Arrays.asList(DocumentSearchModel.Props.ERRAND_COUNTRY));
        searchableToFilter.put(DocumentCommonModel.Props.SEARCHABLE_ERRAND_COUNTY, Arrays.asList(DocumentSearchModel.Props.ERRAND_COUNTY));
        searchableToFilter.put(DocumentCommonModel.Props.SEARCHABLE_ERRAND_CITY, Arrays.asList(DocumentSearchModel.Props.ERRAND_CITY));

        searchableToDocument.put(DocumentCommonModel.Props.SEARCHABLE_COST_MANAGER, Arrays.asList(DocumentSpecificModel.Props.COST_MANAGER));
        searchableToDocument.put(DocumentCommonModel.Props.SEARCHABLE_APPLICANT_NAME, Arrays.asList(DocumentSpecificModel.Props.APPLICANT_NAME, DocumentSpecificModel.Props.PROCUREMENT_APPLICANT_NAME));
        searchableToDocument.put(DocumentCommonModel.Props.SEARCHABLE_ERRAND_BEGIN_DATE, Arrays.asList(DocumentSpecificModel.Props.ERRAND_BEGIN_DATE));
        searchableToDocument.put(DocumentCommonModel.Props.SEARCHABLE_ERRAND_END_DATE, Arrays.asList(DocumentSpecificModel.Props.ERRAND_END_DATE));
        searchableToDocument.put(DocumentCommonModel.Props.SEARCHABLE_ERRAND_COUNTRY, Arrays.asList(DocumentSpecificModel.Props.ERRAND_COUNTRY));
        searchableToDocument.put(DocumentCommonModel.Props.SEARCHABLE_ERRAND_COUNTY, Arrays.asList(DocumentSpecificModel.Props.ERRAND_COUNTY));
        searchableToDocument.put(DocumentCommonModel.Props.SEARCHABLE_ERRAND_CITY, Arrays.asList(DocumentSpecificModel.Props.ERRAND_CITY));
    }

    @Override
    /*
     * Dokumentide _laiendatud_ otsing on 8 välja (4.1.2.23-30 - need mis child-node'idega on seotud) väärtuste võrdlemise puhul rangem kui _lihtne_ otsing!
     * Sest nende väljade puhul tehakse laiendatud otsingu tulemuste kuvamisel Java koodis lisavõrdlemist, peaaegu (aga mitte täpselt) ühtib Lucene käitumisega.
     * 
     * 
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
//            System.out.println("RRRRRRRRRRRR " + document.getNode().getType().toPrefixString(namespaceService) + " returned " + (rows == null ? null : rows.size()));
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
        List<SendInfo> sendInfos = sendOutService.getSendInfos(document.getNode().getNodeRef());
        List<Document> results = new ArrayList<Document>(sendInfos.size());
        if (sendInfos.size() == 0) {
            results.add(document);
        }
        for (SendInfo sendInfo : sendInfos) {
            String sendMode = (String) sendInfo.getSendMode();
            // when no filter is specified, all results match
            if (sendModes == null || sendModes.size() == 0 || !isNotMatch(sendMode, sendModes)) {
                Document row = new Document(document);
                row.setSearchableProperty(DocumentCommonModel.Props.SEARCHABLE_SEND_MODE, sendMode);
                results.add(row);
            }
        }
        return results;
    }

    private List<Document> processExtendedSearchResults(Map<QName/*filter*/, List<QName>/*document*/> props, Node node, Node filter, Document document) {
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
//                System.out.println("RRRRRRRRRRRR " + childNode.getType().toPrefixString(namespaceService) + " returned " + (childResults == null ? null : childResults.size()));
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

    private static boolean isFilterPropNotBlankAndDoesNotMatch(Serializable nodeProp, List<Serializable> filterProps) {
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
    private static boolean isNotMatchWildcard(Serializable nodeProp, String filterProp) {
        List<String> words = SearchUtil.parseQuickSearchWords(filterProp);
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

    public void setDictionaryService(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    public void setNamespaceService(NamespaceService namespaceService) {
        this.namespaceService = namespaceService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
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

    public void setOrganizationStructureService(OrganizationStructureService organizationStructureService) {
        this.organizationStructureService = organizationStructureService;
    }

    public void setSendOutService(SendOutService sendOutService) {
        this.sendOutService = sendOutService;
    }

    public void setFromDvkXPath(String fromDvkXPath) {
        this.fromDvkXPath = fromDvkXPath;
    }

    public void setIncomingEmailPath(String incomingEmailPath) {
        this.incomingEmailPath = incomingEmailPath;
    }
    // END: getters / setters

}
