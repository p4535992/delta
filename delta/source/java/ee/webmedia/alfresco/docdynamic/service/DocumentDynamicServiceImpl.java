package ee.webmedia.alfresco.docdynamic.service;

import static ee.webmedia.alfresco.docadmin.web.DocAdminUtil.getDocTypeIdAndVersionNr;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.FILE_CONTENTS;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.FILE_NAMES;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.OWNER_ID;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.OWNER_NAME;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.PREVIOUS_OWNER_ID;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.REG_DATE_TIME;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.REG_NUMBER;
import static ee.webmedia.alfresco.utils.CalendarUtil.duration;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.alfresco.service.cmr.dictionary.AspectDefinition;
import org.alfresco.service.cmr.dictionary.AssociationDefinition;
import org.alfresco.service.cmr.dictionary.ChildAssociationDefinition;
import org.alfresco.service.cmr.dictionary.ClassDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.TypeDefinition;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.comparators.NullComparator;
import org.apache.commons.collections.comparators.TransformingComparator;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel.Props;
import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;
import ee.webmedia.alfresco.docadmin.service.DocumentType;
import ee.webmedia.alfresco.docadmin.service.DocumentTypeVersion;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docadmin.service.FieldGroup;
import ee.webmedia.alfresco.docconfig.bootstrap.SystematicDocumentType;
import ee.webmedia.alfresco.docconfig.generator.SaveListener;
import ee.webmedia.alfresco.docconfig.generator.systematic.DocumentLocationGenerator;
import ee.webmedia.alfresco.docconfig.service.DocumentConfig;
import ee.webmedia.alfresco.docconfig.service.DocumentConfigService;
import ee.webmedia.alfresco.docconfig.service.DynamicPropertyDefinition;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.docdynamic.web.DocumentDynamicDialog;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.document.log.service.DocumentLogService;
import ee.webmedia.alfresco.document.log.service.DocumentPropertiesChangeHolder;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.sendout.service.SendOutService;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.document.service.DocumentServiceImpl;
import ee.webmedia.alfresco.document.service.EventsLoggingHelper;
import ee.webmedia.alfresco.imap.model.ImapModel;
import ee.webmedia.alfresco.template.service.DocumentTemplateService;
import ee.webmedia.alfresco.utils.FilenameUtil;
import ee.webmedia.alfresco.utils.MessageData;
import ee.webmedia.alfresco.utils.MessageDataImpl;
import ee.webmedia.alfresco.utils.MessageDataWrapper;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.TreeNode;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.utils.UnableToPerformMultiReasonException;

/**
 * @author Alar Kvell
 */
public class DocumentDynamicServiceImpl implements DocumentDynamicService, BeanFactoryAware {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(DocumentDynamicServiceImpl.class);

    // Dokument ei saa eksisteerida ainult mälus (nagu Worfklow objektid), alati on salvestatud kuhugi, esimesel loomisel draftsi alla

    private DictionaryService dictionaryService;
    private NamespaceService namespaceService;
    private NodeService nodeService;
    private GeneralService generalService;
    private DocumentService documentService;
    private DocumentAdminService documentAdminService;
    private DocumentConfigService documentConfigService;
    private SendOutService sendOutService;
    private DocumentLogService documentLogService;
    private DocumentTemplateService documentTemplateService;
    private FileService fileService;
    private boolean showMessageIfUnregistered;

    private BeanFactory beanFactory;

    @Override
    public void setOwner(NodeRef docRef, String ownerId, boolean retainPreviousOwnerId) {
        Map<QName, Serializable> props = nodeService.getProperties(docRef);
        setOwner(props, ownerId, retainPreviousOwnerId);
        generalService.setPropertiesIgnoringSystem(props, docRef);
    }

    @Override
    public void setOwner(Map<QName, Serializable> props, String ownerId, boolean retainPreviousOwnerId, Map<String, Pair<DynamicPropertyDefinition, Field>> propDefs) {
        Pair<DynamicPropertyDefinition, Field> pair = propDefs.get(OWNER_NAME.getLocalName());
        String previousOwnerId = (String) props.get(OWNER_ID);
        documentConfigService.setUserContactProps(props, ownerId, pair.getFirst(), pair.getSecond());

        if (!StringUtils.equals(previousOwnerId, ownerId)) {
            if (!retainPreviousOwnerId) {
                previousOwnerId = null;
            }
            props.put(PREVIOUS_OWNER_ID, previousOwnerId);
        }
    }

    @Override
    public void setOwner(Map<QName, Serializable> props, String ownerId, boolean retainPreviousOwnerId) {
        String previousOwnerId = (String) props.get(OWNER_ID);
        documentConfigService.setUserContactProps(props, ownerId, OWNER_NAME.getLocalName());

        if (!StringUtils.equals(previousOwnerId, ownerId)) {
            if (!retainPreviousOwnerId) {
                previousOwnerId = null;
            }
            props.put(PREVIOUS_OWNER_ID, previousOwnerId);
        }
    }

    @Override
    public boolean isOwner(NodeRef docRef, String ownerId) {
        return StringUtils.equals(getOwner(docRef), ownerId);
    }

    private String getOwner(NodeRef docRef) {
        return (String) nodeService.getProperty(docRef, OWNER_ID);
    }

    @Override
    public Pair<DocumentDynamic, DocumentTypeVersion> createNewDocument(String documentTypeId, NodeRef parent) {
        DocumentTypeVersion docVer = getLatestDocTypeVer(documentTypeId);
        return createNewDocument(docVer, parent, true);
    }

    @Override
    public Pair<DocumentDynamic, DocumentTypeVersion> createNewDocument(DocumentTypeVersion docVer, NodeRef parent, boolean reallySetDefaultValues) {

        Map<QName, Serializable> props = new HashMap<QName, Serializable>();
        setTypeProps(getDocTypeIdAndVersionNr(docVer), props);

        return createNewDocument(docVer, parent, props, reallySetDefaultValues);
    }

    private Pair<DocumentDynamic, DocumentTypeVersion> createNewDocument(DocumentTypeVersion docVer, NodeRef parent, Map<QName, Serializable> props, boolean reallySetDefaultValues) {

        // TODO FIXME this is handled by setDefaultPropertyValues, but currently admin can configure inappropriate values to docStatus
        props.put(DocumentCommonModel.Props.DOC_STATUS, DocumentStatus.WORKING.getValueName());

        QName type = DocumentCommonModel.Types.DOCUMENT;
        NodeRef docRef = nodeService.createNode(parent, DocumentCommonModel.Assocs.DOCUMENT, DocumentCommonModel.Assocs.DOCUMENT, type,
                props).getChildRef();

        DocumentDynamic document = getDocument(docRef);
        WmNode docNode = document.getNode();

        TreeNode<QName> childAssocTypeQNamesRoot = documentConfigService.getChildAssocTypeQNameTree(docVer);
        Assert.isNull(childAssocTypeQNamesRoot.getData());

        createChildNodesHierarchy(docNode, childAssocTypeQNamesRoot.getChildren(), null);

        documentConfigService.setDefaultPropertyValues(docNode, null, false, reallySetDefaultValues, docVer);

        DocumentServiceImpl.PropertyChangesMonitorHelper propertyChangesMonitorHelper = new DocumentServiceImpl.PropertyChangesMonitorHelper();
        saveThisNodeAndChildNodes(null, docNode, childAssocTypeQNamesRoot.getChildren(), null, propertyChangesMonitorHelper, null);

        return Pair.newInstance(getDocument(docRef), docVer);
    }

    @Override
    public void createChildNodesHierarchyAndSetDefaultPropertyValues(Node parentNode, QName[] hierarchy, DocumentTypeVersion docVer) {
        TreeNode<QName> root = documentConfigService.getChildAssocTypeQNameTree(parentNode);
        Assert.isNull(root.getData());

        int i = 0;
        TreeNode<QName> current = root;
        while (i < hierarchy.length) {
            Assert.isTrue(hierarchy[i] != null);
            TreeNode<QName> foundChild = null;
            for (TreeNode<QName> currentChild : current.getChildren()) {
                if (currentChild.getData().equals(hierarchy[i])) {
                    Assert.isNull(foundChild);
                    foundChild = currentChild;
                }
            }
            Assert.notNull(foundChild);
            current = foundChild;
            i++;
        }
        Assert.notNull(current.getData());

        List<Pair<QName, WmNode>> childNodes = createChildNodesHierarchy(parentNode, Collections.singletonList(current), null);
        Assert.isTrue(childNodes.size() == 1);

        documentConfigService.setDefaultPropertyValues(childNodes.get(0).getSecond(), hierarchy, false, false, docVer);
    }

    private List<Pair<QName, WmNode>> createChildNodesHierarchy(Node parentNode, List<TreeNode<QName>> childAssocTypeQNames, Node firstChild) {
        List<Pair<QName, WmNode>> childNodes = new ArrayList<Pair<QName, WmNode>>();
        for (TreeNode<QName> childAssocTypeQName : childAssocTypeQNames) {
            QName assocTypeQName = childAssocTypeQName.getData();
            addContainerAspectIfNecessary(parentNode, assocTypeQName);
            Map<QName, Serializable> props = new HashMap<QName, Serializable>();

            Node newFirstChild = null;
            List<Node> list = (firstChild == null ? parentNode : firstChild).getAllChildAssociations(assocTypeQName);
            if (list != null && !list.isEmpty()) {
                newFirstChild = list.get(0);
                if (firstChild != null) {
                    props.putAll(RepoUtil.toQNameProperties(newFirstChild.getProperties(), true, true));
                }
            }
            firstChild = newFirstChild;

            // objectTypeId and objectTypeVersion are set on every child node, because if
            // documentConfigService.getPropertyDefinition is called, then we don't have to find parent document
            setTypeProps(getDocTypeIdAndVersionNr(parentNode), props);
            WmNode childNode = generalService.createNewUnSaved(assocTypeQName, props);
            parentNode.addChildAssociations(assocTypeQName, childNode);
            childNodes.add(Pair.newInstance(assocTypeQName, childNode));
            createChildNodesHierarchy(childNode, childAssocTypeQName.getChildren(), firstChild);
        }
        return childNodes;
    }

    private void addContainerAspectIfNecessary(Node parentNode, QName assocTypeQName) {
        AssociationDefinition assocDef = dictionaryService.getAssociation(assocTypeQName);
        Assert.isTrue(assocDef instanceof ChildAssociationDefinition);
        ClassDefinition containerClass = assocDef.getSourceClass();
        if (containerClass instanceof TypeDefinition) {
            Assert.isTrue(dictionaryService.isSubClass(parentNode.getType(), containerClass.getName()));
        } else if (containerClass instanceof AspectDefinition) {
            if (!parentNode.hasAspect(containerClass.getName())) {
                parentNode.getAspects().add(containerClass.getName());
                if (LOG.isDebugEnabled()) {
                    LOG.debug("node " + parentNode.getType().toPrefixString(namespaceService) + " addAspect " + containerClass.getName().toPrefixString(namespaceService));
                }
            }
        } else {
            throw new RuntimeException("Unknown subclass of ClassDefinition: " + WmNode.toString(containerClass));
        }
    }

    private List<Pair<QName, WmNode>> copyPropsAndChildNodesHierarchy(Node sourceParentNode, Node targetParentNode, List<TreeNode<QName>> childAssocTypeQNames,
            Map<String, Pair<DynamicPropertyDefinition, Field>> propDefs, Map<QName, Serializable> overrideProps, Set<QName> ignoredPropsSet, QName[] requiredHierarchy) {

        if (requiredHierarchy == null) {
            requiredHierarchy = new QName[] {};
        }
        Map<String, Object> sourceProps = sourceParentNode.getProperties();
        Map<String, Object> targetProps = targetParentNode.getProperties();
        for (Entry<String, Pair<DynamicPropertyDefinition, Field>> entry : propDefs.entrySet()) {
            DynamicPropertyDefinition propDef = entry.getValue().getFirst();
            QName[] hierarchy = propDef.getChildAssocTypeQNameHierarchy();
            if (hierarchy == null) {
                hierarchy = new QName[] {};
            }
            if (!Arrays.equals(hierarchy, requiredHierarchy)) {
                continue;
            }
            QName propName = propDef.getName();
            if (overrideProps != null && overrideProps.containsKey(propName)) {
                targetProps.put(propName.toString(), overrideProps.get(propName));
                continue;
            }
            if (ignoredPropsSet.contains(propName)) {
                continue;
            }
            if (sourceProps.containsKey(propName.toString()) || targetProps.containsKey(propName.toString())) {
                targetProps.put(propName.toString(), sourceProps.get(propName.toString()));
            }
        }

        List<Pair<QName, WmNode>> childNodes = new ArrayList<Pair<QName, WmNode>>();
        for (TreeNode<QName> childAssocTypeQName : childAssocTypeQNames) {
            QName assocTypeQName = childAssocTypeQName.getData();
            addContainerAspectIfNecessary(targetParentNode, assocTypeQName);

            List<Node> sourceChildNodes = sourceParentNode.getAllChildAssociations(assocTypeQName);
            if (sourceChildNodes != null) {
                for (Node sourceChildNode : sourceChildNodes) {
                    Map<QName, Serializable> props = new HashMap<QName, Serializable>();
                    // objectTypeId and objectTypeVersion are set on every child node, because if
                    // documentConfigService.getPropertyDefinition is called, then we don't have to find parent document
                    setTypeProps(getDocTypeIdAndVersionNr(targetParentNode), props);
                    WmNode targetChildNode = generalService.createNewUnSaved(assocTypeQName, props);
                    targetParentNode.addChildAssociations(assocTypeQName, targetChildNode);
                    childNodes.add(Pair.newInstance(assocTypeQName, targetChildNode));
                    QName[] childRequiredHierarchy = (QName[]) ArrayUtils.add(requiredHierarchy, assocTypeQName);
                    copyPropsAndChildNodesHierarchy(sourceChildNode, targetChildNode, childAssocTypeQName.getChildren(), propDefs, overrideProps, ignoredPropsSet,
                            childRequiredHierarchy);
                }
            }
        }
        return childNodes;
    }

    private DocumentTypeVersion getLatestDocTypeVer(String documentTypeId) {
        DocumentType documentType = documentAdminService.getDocumentType(documentTypeId, DocumentAdminService.DOC_TYPE_WITH_OUT_GRAND_CHILDREN_EXEPT_LATEST_DOCTYPE_VER);
        DocumentTypeVersion docVer = documentType.getLatestDocumentTypeVersion();
        return docVer;
    }

    private void setTypeProps(Pair<String, Integer> docTypeIdAndVersionNr, Map<QName, Serializable> props) {
        props.put(Props.OBJECT_TYPE_ID, docTypeIdAndVersionNr.getFirst());
        props.put(Props.OBJECT_TYPE_VERSION_NR, docTypeIdAndVersionNr.getSecond());
    }

    @Override
    public Pair<DocumentDynamic, DocumentTypeVersion> createNewDocumentInDrafts(String documentTypeId) {
        NodeRef drafts = documentService.getDrafts();
        return createNewDocument(documentTypeId, drafts);
    }

    @Override
    public NodeRef copyDocumentToDrafts(DocumentDynamic sourceDocument, Map<QName, Serializable> overrideProps, QName... ignoredProps) {
        DocumentDynamic targetDocument = createNewDocumentInDrafts(sourceDocument.getDocumentTypeId()).getFirst();

        Set<QName> ignoredPropsSet = new HashSet<QName>(Arrays.asList(ignoredProps));
        Map<String, Pair<DynamicPropertyDefinition, Field>> propDefs = documentConfigService.getPropertyDefinitions(targetDocument.getNode());

        TreeNode<QName> childAssocTypeQNamesRoot = documentConfigService.getChildAssocTypeQNameTree(targetDocument.getNode());
        Assert.isNull(childAssocTypeQNamesRoot.getData());

        removeChildNodes(targetDocument, childAssocTypeQNamesRoot);
        copyPropsAndChildNodesHierarchy(sourceDocument.getNode(), targetDocument.getNode(), childAssocTypeQNamesRoot.getChildren(), propDefs, overrideProps, ignoredPropsSet, null);

        DocumentServiceImpl.PropertyChangesMonitorHelper propertyChangesMonitorHelper = new DocumentServiceImpl.PropertyChangesMonitorHelper();
        saveThisNodeAndChildNodes(null, targetDocument.getNode(), childAssocTypeQNamesRoot.getChildren(), null, propertyChangesMonitorHelper, null);

        return targetDocument.getNodeRef();
    }

    @Override
    public DocumentDynamic getDocument(NodeRef docRef) {
        DocumentDynamic doc = new DocumentDynamic(generalService.fetchObjectNode(docRef, DocumentCommonModel.Types.DOCUMENT));
        setParentFolderProps(doc);
        if (LOG.isDebugEnabled()) {
            LOG.debug("getDocument document=" + doc);
        }
        return doc;
    }

    @Override
    public DocumentDynamic getDocumentWithInMemoryChangesForEditing(NodeRef docRef) {
        DocumentDynamic document = getDocument(docRef);
        if (document.isImapOrDvk()) {
            Pair<DocumentType, DocumentTypeVersion> documentTypeAndVersion = documentConfigService.getDocumentTypeAndVersion(document.getNode());
            Collection<Field> ownerNameFields = documentTypeAndVersion.getSecond().getFieldsById(Collections.singleton(DocumentCommonModel.Props.OWNER_NAME.getLocalName()));
            if (ownerNameFields.size() == 1) {
                Field ownerNameField = ownerNameFields.iterator().next();
                if (ownerNameField.isSystematic() && ownerNameField.getFieldId().equals(ownerNameField.getOriginalFieldId()) && ownerNameField.getParent() instanceof FieldGroup) {
                    documentConfigService.setDefaultPropertyValues(document.getNode(), null, true, true, ((FieldGroup) ownerNameField.getParent()).getFields());
                }
            }
        }
        return document;
    }

    @Override
    public void changeTypeInMemory(DocumentDynamic document, String newTypeId) {
        DocumentTypeVersion docVer = getLatestDocTypeVer(newTypeId);
        Map<QName, Serializable> typeProps = new HashMap<QName, Serializable>();
        setTypeProps(getDocTypeIdAndVersionNr(docVer), typeProps);

        Map<String, Object> oldProps = document.getNode().getProperties();
        Map<String, Object> newProps = new HashMap<String, Object>();
        newProps.putAll(RepoUtil.toStringProperties(typeProps));
        newProps.put(DocumentCommonModel.Props.DOC_NAME.toString(), oldProps.get(DocumentCommonModel.Props.DOC_NAME));
        newProps.put(DocumentCommonModel.Props.ACCESS_RESTRICTION.toString(), oldProps.get(DocumentCommonModel.Props.ACCESS_RESTRICTION));
        newProps.put(DocumentCommonModel.Props.ACCESS_RESTRICTION_BEGIN_DATE.toString(), oldProps.get(DocumentCommonModel.Props.ACCESS_RESTRICTION_BEGIN_DATE));
        newProps.put(DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DATE.toString(), oldProps.get(DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DATE));
        newProps.put(DocumentCommonModel.Props.ACCESS_RESTRICTION_REASON.toString(), oldProps.get(DocumentCommonModel.Props.ACCESS_RESTRICTION_REASON));
        newProps.put(DocumentCommonModel.Props.DOC_STATUS.toString(), oldProps.get(DocumentCommonModel.Props.DOC_STATUS));
        newProps.put(DocumentCommonModel.Props.STORAGE_TYPE.toString(), oldProps.get(DocumentCommonModel.Props.STORAGE_TYPE));

        // remove all existing subnodes in memory
        TreeNode<QName> oldChildAssocTypeQNamesRoot = documentConfigService.getChildAssocTypeQNameTree(document.getNode());
        Assert.isNull(oldChildAssocTypeQNamesRoot.getData());
        removeChildNodes(document, oldChildAssocTypeQNamesRoot);

        // this is needed to overwrite all existing properties (possibly belonging to different document type)
        for (String oldPropName : oldProps.keySet()) {
            QName oldPropQName = QName.createQName(oldPropName);
            if (DocumentDynamicModel.URI.equals(oldPropQName.getNamespaceURI()) && !newProps.containsKey(oldPropName)) {
                newProps.put(oldPropName, null);
            }
        }
        oldProps.clear();
        oldProps.putAll(newProps);
        setParentFolderProps(document);

        TreeNode<QName> childAssocTypeQNamesRoot = documentConfigService.getChildAssocTypeQNameTree(docVer);
        Assert.isNull(childAssocTypeQNamesRoot.getData());

        // create new subnodes in memory
        createChildNodesHierarchy(document.getNode(), childAssocTypeQNamesRoot.getChildren(), null);

        // set default values in memory - does not overwrite existing values
        documentConfigService.setDefaultPropertyValues(document.getNode(), null, false, true, docVer);
    }

    private void removeChildNodes(DocumentDynamic document, TreeNode<QName> childAssocTypeQNamesRoot) {
        for (TreeNode<QName> childAssocTypeQName : childAssocTypeQNamesRoot.getChildren()) {
            QName assocTypeQName = childAssocTypeQName.getData();
            WmNode docNode = document.getNode();
            List<Node> childNodes = docNode.getAllChildAssociations(assocTypeQName);
            while (childNodes != null && !childNodes.isEmpty()) {
                docNode.removeChildAssociations(assocTypeQName, 0);
                childNodes = docNode.getAllChildAssociations(assocTypeQName);
            }

            // Remove container aspects, otherwise integrity checker throws an error, because required child association multiplicity is 1
            AssociationDefinition assocDef = dictionaryService.getAssociation(assocTypeQName);
            Assert.isTrue(assocDef instanceof ChildAssociationDefinition);
            ClassDefinition containerClass = assocDef.getSourceClass();
            if (containerClass instanceof TypeDefinition) {
                Assert.isTrue(dictionaryService.isSubClass(docNode.getType(), containerClass.getName()));
            } else if (containerClass instanceof AspectDefinition) {
                if (docNode.hasAspect(containerClass.getName())) {
                    docNode.getAspects().remove(containerClass.getName());
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("node " + docNode.getType().toPrefixString(namespaceService) + " removeAspect " + containerClass.getName().toPrefixString(namespaceService));
                    }
                }
            } else {
                throw new RuntimeException("Unknown subclass of ClassDefinition: " + WmNode.toString(containerClass));
            }
        }
    }

    private static class ValidationHelperImpl implements SaveListener.ValidationHelper {

        private final List<MessageData> errorMessages = new ArrayList<MessageData>();

        @Override
        public void addErrorMessage(String msgKey, Object... messageValuesForHolders) {
            errorMessages.add(new MessageDataImpl(msgKey, messageValuesForHolders));
        }

    }

    @Override
    public DocumentDynamic updateDocument(DocumentDynamic documentOriginal, List<String> saveListenerBeanNames, boolean relocateAssocDocs) {
        return updateDocumentGetDocAndNodeRefs(documentOriginal, saveListenerBeanNames, relocateAssocDocs).getFirst();
    }

    private static final Comparator<DocumentDynamic> DOCUMENT_BY_REG_DATE_TIME_COMPARATOR;
    static {
        @SuppressWarnings("unchecked")
        Comparator<DocumentDynamic> tmp = new TransformingComparator(new Transformer() {
            @Override
            public Object transform(Object input) {
                return ((DocumentDynamic) input).getRegDateTime();
            }
        }, new NullComparator());
        DOCUMENT_BY_REG_DATE_TIME_COMPARATOR = tmp;
    }

    @Override
    public Pair<DocumentDynamic, List<Pair<NodeRef, NodeRef>>> updateDocumentGetDocAndNodeRefs(DocumentDynamic documentOriginal, List<String> saveListenerBeanNames,
            boolean relocateAssocDocs) {
        NodeRef originalDocumentNodeRef = documentOriginal.getNodeRef();
        Set<NodeRef> associatedDocRefs = Collections.singleton(originalDocumentNodeRef);
        List<DocumentDynamic> associatedDocs = new ArrayList<DocumentDynamic>();
        associatedDocs.add(documentOriginal);

        long findStartTime = System.nanoTime();
        NodeRef functionRef = documentOriginal.getFunction();
        NodeRef seriesRef = documentOriginal.getSeries();
        NodeRef volumeRef = documentOriginal.getVolume();
        // It is assumed that when reaching this code, original document's case label is always set,
        // if volume contains cases, no matter from where this method is called
        String caseLabel = StringUtils.trimToNull((String) documentOriginal.getProp(DocumentLocationGenerator.CASE_LABEL_EDITABLE));
        // collect associated documents that have different location than original document's _new_ location
        if (relocateAssocDocs) {
            associatedDocRefs = getAssociatedDocRefs(documentOriginal);
            if (associatedDocRefs.size() > 1) {
                NodeRef parentRef = DocumentDynamicDialog.getParent(volumeRef, caseLabel);
                for (NodeRef associatedDocRef : associatedDocRefs) {
                    NodeRef associatedDocParentRef = nodeService.getPrimaryParent(associatedDocRef).getParentRef();
                    // parentRef is null if new case is going to be created during saving document
                    if ((parentRef == null || !parentRef.equals(associatedDocParentRef)) && !associatedDocRef.equals(originalDocumentNodeRef)) {
                        associatedDocs.add(getDocument(associatedDocRef));
                    }
                }
            }
        }
        long findStopAndSaveStartTime = System.nanoTime();

        DocumentDynamic originalDocumentUpdated = null;
        List<Pair<NodeRef, NodeRef>> originalNodeRefs = new ArrayList<Pair<NodeRef, NodeRef>>();
        Collections.sort(associatedDocs, DOCUMENT_BY_REG_DATE_TIME_COMPARATOR);
        for (DocumentDynamic associatedDocument : associatedDocs) {
            if (!associatedDocument.getNodeRef().getId().equals(originalDocumentNodeRef.getId())) {
                DocumentConfig cfg = documentConfigService.getConfig(associatedDocument.getNode());
                associatedDocument.setFunction(functionRef);
                associatedDocument.setSeries(seriesRef);
                associatedDocument.setVolume(volumeRef);
                associatedDocument.setProp(DocumentLocationGenerator.CASE_LABEL_EDITABLE, caseLabel);
                NodeRef oldNodeRef = associatedDocument.getNodeRef();
                NodeRef newNodeRef = update(associatedDocument, cfg.getSaveListenerBeanNames()).getNodeRef();
                originalNodeRefs.add(Pair.newInstance(oldNodeRef, newNodeRef));
            } else {
                originalDocumentUpdated = update(associatedDocument, saveListenerBeanNames);
            }
        }
        long saveStopTime = System.nanoTime();
        LOG.info("Saved 1 original document and " + (associatedDocs.size() - 1) + " associated documents, skipped " + (associatedDocRefs.size() - associatedDocs.size())
                + " associated documents; finding and loading took " + duration(findStartTime, findStopAndSaveStartTime) + " ms, saving took "
                + duration(findStopAndSaveStartTime, saveStopTime) + " ms");
        return Pair.newInstance(originalDocumentUpdated, originalNodeRefs);
    }

    // NB! This method may change document nodeRef when moving from archive to active store (see DocumentLocationGenerator save method)
    private DocumentDynamic update(DocumentDynamic documentOriginal, List<String> saveListenerBeanNames) {
        DocumentDynamic document = documentOriginal.clone();
        boolean isDraft = document.isDraft();
        boolean isImapOrDvk = document.isImapOrDvk();
        if (saveListenerBeanNames != null) {
            ValidationHelperImpl validationHelper = new ValidationHelperImpl();
            for (String saveListenerBeanName : saveListenerBeanNames) {
                SaveListener saveListener = (SaveListener) beanFactory.getBean(saveListenerBeanName, SaveListener.class);
                saveListener.validate(document, validationHelper);
            }
            if (!validationHelper.errorMessages.isEmpty()) {
                throw new UnableToPerformMultiReasonException(new MessageDataWrapper(validationHelper.errorMessages));
            }
            for (String saveListenerBeanName : saveListenerBeanNames) {
                SaveListener saveListener = (SaveListener) beanFactory.getBean(saveListenerBeanName, SaveListener.class);
                saveListener.save(document);
            }
        }

        setParentFolderProps(document);
        WmNode docNode = document.getNode();
        NodeRef docRef = document.getNodeRef();

        // If document is updated for the first time, add SEARCHABLE aspect to document and it's children files.
        Map<String, Object> docProps = document.getNode().getProperties();
        if (!docNode.hasAspect(DocumentCommonModel.Aspects.SEARCHABLE)) {
            docNode.getAspects().add(DocumentCommonModel.Aspects.SEARCHABLE);
            docProps.put(FILE_NAMES.toString(), documentService.getSearchableFileNames(docRef));
            docProps.put(FILE_CONTENTS.toString(), documentService.getSearchableFileContents(docRef));
            docProps.put(DocumentCommonModel.Props.SEARCHABLE_SEND_MODE.toString(), sendOutService.buildSearchableSendMode(docRef));
        }

        if (isImapOrDvk) {
            List<File> files = fileService.getAllFilesExcludingDigidocSubitems(docRef);
            for (File file : files) {
                if (FilenameUtil.isEncryptedFile(file.getName())) {
                    throw new UnableToPerformException("docdyn_save_encryptedFilesForbidden");
                }
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("updateDocument after validation and save listeners, before real saving: " + document);
        }

        TreeNode<QName> childAssocTypeQNamesRoot = documentConfigService.getChildAssocTypeQNameTree(document.getNode());
        Assert.isNull(childAssocTypeQNamesRoot.getData());

        Map<String, Pair<DynamicPropertyDefinition, Field>> propDefs = documentConfigService.getPropertyDefinitions(document.getNode());

        updateSearchableChildNodeProps(docNode, null, childAssocTypeQNamesRoot.getChildren(), propDefs);

        { // update properties and log changes made in properties
            String oldRegNumber = (String) nodeService.getProperty(docRef, REG_NUMBER);
            DocumentServiceImpl.PropertyChangesMonitorHelper propertyChangesMonitorHelper = new DocumentServiceImpl.PropertyChangesMonitorHelper();
            propertyChangesMonitorHelper.addIgnoredProps(docProps //
                    , REG_NUMBER, REG_DATE_TIME // registration changes
                    );
            String newRegNumber = (String) nodeService.getProperty(docRef, REG_NUMBER);
            documentService.updateParentDocumentRegNumbers(docRef, oldRegNumber, newRegNumber);

            DocumentPropertiesChangeHolder changedPropsNewValues = saveThisNodeAndChildNodes(null, docNode,
                    childAssocTypeQNamesRoot.getChildren(), null, propertyChangesMonitorHelper, propDefs);
            if (!EventsLoggingHelper.isLoggingDisabled(docNode, DocumentService.TransientProps.TEMP_LOGGING_DISABLED_DOCUMENT_METADATA_CHANGED)) {
                if (isDraft) {
                    documentLogService.addDocumentLog(docRef, MessageUtil.getMessage("document_log_status_created"));
                } else {
                    for (Serializable msg : changedPropsNewValues.generateLogMessages(propDefs, docRef)) {
                        documentLogService.addDocumentLog(docRef, (String) msg);
                    }
                }
            }
            documentTemplateService.updateGeneratedFiles(docRef, false);
        }
        generalService.saveAddedAssocs(docNode);
        return document;
    }

    private Set<NodeRef> getAssociatedDocRefs(DocumentDynamic document) {
        NodeRef docRef = document.getNodeRef();

        Set<NodeRef> currentAssociatedDocs = new HashSet<NodeRef>();
        List<AssociationRef> newAssocs = new ArrayList<AssociationRef>();
        Map<String, AssociationRef> addedAssocs = document.getNode().getAddedAssociations().get(DocumentCommonModel.Assocs.DOCUMENT_REPLY.toString());
        if (addedAssocs != null) {
            newAssocs.addAll(addedAssocs.values());
        }
        addedAssocs = document.getNode().getAddedAssociations().get(DocumentCommonModel.Assocs.DOCUMENT_FOLLOW_UP.toString());
        if (addedAssocs != null) {
            newAssocs.addAll(addedAssocs.values());
        }
        for (AssociationRef assocRef : newAssocs) {
            NodeRef sourceRef = assocRef.getSourceRef();
            if (!sourceRef.equals(docRef) && !currentAssociatedDocs.contains(sourceRef) && isSearchable(sourceRef)) {
                currentAssociatedDocs.add(sourceRef);
            }
            NodeRef targetRef = assocRef.getTargetRef();
            if (!targetRef.equals(docRef) && !currentAssociatedDocs.contains(targetRef) && isSearchable(targetRef)) {
                currentAssociatedDocs.add(targetRef);
            }
        }

        Set<NodeRef> associatedDocs = new HashSet<NodeRef>();
        associatedDocs.add(docRef);
        getAssociatedDocRefs(docRef, associatedDocs, new HashSet<NodeRef>(), currentAssociatedDocs);
        return associatedDocs;
    }

    private void getAssociatedDocRefs(NodeRef docRef, Set<NodeRef> associatedDocs, Set<NodeRef> checkedDocs, Set<NodeRef> currentAssociatedDocs) {
        if (checkedDocs.contains(docRef)) {
            return;
        }
        checkedDocs.add(docRef);

        if (currentAssociatedDocs == null) {
            currentAssociatedDocs = new HashSet<NodeRef>();
        }
        if (docRef != null) {
            List<AssociationRef> targetAssocs = nodeService.getTargetAssocs(docRef, DocumentCommonModel.Assocs.DOCUMENT_REPLY);
            targetAssocs.addAll(nodeService.getTargetAssocs(docRef, DocumentCommonModel.Assocs.DOCUMENT_FOLLOW_UP));
            for (AssociationRef assoc : targetAssocs) {
                NodeRef targetRef = assoc.getTargetRef();
                if (!associatedDocs.contains(targetRef) && isSearchable(targetRef)) {
                    currentAssociatedDocs.add(targetRef);
                }
            }
            List<AssociationRef> sourceAssocs = nodeService.getSourceAssocs(docRef, DocumentCommonModel.Assocs.DOCUMENT_REPLY);
            sourceAssocs.addAll(nodeService.getSourceAssocs(docRef, DocumentCommonModel.Assocs.DOCUMENT_FOLLOW_UP));
            for (AssociationRef assoc : sourceAssocs) {
                NodeRef sourceRef = assoc.getSourceRef();
                if (!associatedDocs.contains(sourceRef) && isSearchable(sourceRef)) {
                    currentAssociatedDocs.add(sourceRef);
                }
            }
        }
        associatedDocs.addAll(currentAssociatedDocs);
        for (NodeRef associatedDoc : currentAssociatedDocs) {
            getAssociatedDocRefs(associatedDoc, associatedDocs, checkedDocs, null);
        }
    }

    private boolean isSearchable(NodeRef sourceRef) {
        return nodeService.hasAspect(sourceRef, DocumentCommonModel.Aspects.SEARCHABLE);
    }

    private void setParentFolderProps(DocumentDynamic document) {
        NodeRef docRef = document.getNodeRef();
        document.setDraft(isDraft(docRef));
        document.setDraftOrImapOrDvk(isDraftOrImapOrDvk(docRef));
        document.setIncomingInvoice(documentService.isIncomingInvoice(docRef));
    }

    @Override
    public void deleteDocumentIfDraft(NodeRef docRef) {
        LOG.info("deleteDocumentIfDraft document=" + docRef);
        if (!nodeService.exists(docRef)) {
            LOG.debug("Document does not exist, not deleting: " + docRef);
            return;
        }
        if (!isDraft(docRef)) {
            LOG.debug("Document is not a draft, not deleting: " + docRef);
            return;
        }
        nodeService.deleteNode(docRef);
    }

    @Override
    public boolean isDraft(NodeRef docRef) {
        ChildAssociationRef parentAssoc = nodeService.getPrimaryParent(docRef);
        QName parentType = nodeService.getType(parentAssoc.getParentRef());
        ChildAssociationRef grandParentAssoc = nodeService.getPrimaryParent(parentAssoc.getParentRef());
        return isDraft(grandParentAssoc, parentType);
    }

    private boolean isDraft(ChildAssociationRef grandParentAssoc, QName parentType) {
        return DocumentCommonModel.Types.DRAFTS.equals(parentType) && DocumentCommonModel.Types.DRAFTS.equals(grandParentAssoc.getQName());
    }

    @Override
    public boolean isDraftOrImapOrDvk(NodeRef docRef) {
        NodeRef parentRef = nodeService.getPrimaryParent(docRef).getParentRef();
        QName parentType = nodeService.getType(parentRef);
        return isDraftOrImapOrDvk(parentType);
    }

    private boolean isDraftOrImapOrDvk(QName parentType) {
        return DocumentCommonModel.Types.DRAFTS.equals(parentType) || ImapModel.Types.IMAP_FOLDER.equals(parentType);
    }

    @Override
    public boolean isImapOrDvk(NodeRef docRef) {
        ChildAssociationRef parentAssoc = nodeService.getPrimaryParent(docRef);
        QName parentType = nodeService.getType(parentAssoc.getParentRef());
        ChildAssociationRef grandParentAssoc = nodeService.getPrimaryParent(parentAssoc.getParentRef());
        return isDraftOrImapOrDvk(parentType) && !isDraft(grandParentAssoc, parentType);
    }

    @Override
    public boolean isOutgoingLetter(NodeRef docRef) {
        String docTypeId = getDocumentType(docRef);
        return SystematicDocumentType.OUTGOING_LETTER.getId().equals(docTypeId);
    }

    @Override
    public String getDocumentTypeName(NodeRef documentRef) {
        String docTypeIdOfDoc = getDocumentType(documentRef);
        return documentAdminService.getDocumentTypeProperty(docTypeIdOfDoc, DocumentAdminModel.Props.NAME, String.class);
    }

    @Override
    public String getDocumentType(NodeRef documentRef) {
        return (String) nodeService.getProperty(documentRef, Props.OBJECT_TYPE_ID);
    }

    /*
     * For reports to work correctly, all values (even nulls and blank Strings) are added to searchable props
     * *
     * NB! We don't support the following scenario for _reports_ (document search works):
     * Doc -> ChildType1 -> ChildType2
     * Doc -> ChildType1 -> ChildType3
     * *
     * NB! We don't support multi-valued properties on child/grand-child for _reports_ (document search works).
     */
    private void updateSearchableChildNodeProps(Node node, QName[] currentHierarchy, List<TreeNode<QName>> childAssocTypeQNames,
            Map<String, Pair<DynamicPropertyDefinition, Field>> propDefs) {
        if (currentHierarchy == null) {
            currentHierarchy = new QName[] {};
        }
        for (Pair<DynamicPropertyDefinition, Field> pair : propDefs.values()) {
            DynamicPropertyDefinition propDef = pair.getFirst();
            QName[] propHierarchy = propDef.getChildAssocTypeQNameHierarchy();
            if (propHierarchy == null) {
                propHierarchy = new QName[] {};
            }
            if (propHierarchy.length > currentHierarchy.length) { // prop is on child or grand-child node, not on current node
                if (Arrays.equals(ArrayUtils.subarray(propHierarchy, 0, currentHierarchy.length), currentHierarchy)) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Setting on " + node.getType().toPrefixString(namespaceService) + " prop " + propDef.getName().toPrefixString(namespaceService)
                                + " to empty list");
                    }
                    node.getProperties().put(propDef.getName().toString(), new ArrayList<Object>());
                }
            }
        }
        for (TreeNode<QName> childAssocTypeQName : childAssocTypeQNames) {
            QName assocTypeQName = childAssocTypeQName.getData();
            List<Node> childNodes = node.getAllChildAssociations(assocTypeQName);
            if (childNodes == null) {
                continue;
            }
            for (Node childNode : childNodes) {
                QName[] childHierarchy = (QName[]) ArrayUtils.add(currentHierarchy, assocTypeQName);
                updateSearchableChildNodeProps(childNode, childHierarchy, childAssocTypeQName.getChildren(), propDefs);

                int nrOfTimesToDuplicate = -1;
                Map<QName, Object> propsToDuplicate = new HashMap<QName, Object>();

                for (Pair<DynamicPropertyDefinition, Field> pair : propDefs.values()) {
                    DynamicPropertyDefinition propDef = pair.getFirst();
                    QName[] propHierarchy = propDef.getChildAssocTypeQNameHierarchy();
                    if (propHierarchy == null) {
                        propHierarchy = new QName[] {};
                    }
                    if (propHierarchy.length < childHierarchy.length) {
                        continue;// prop is not on child or grand-child node
                    }
                    if (!Arrays.equals(ArrayUtils.subarray(propHierarchy, 0, childHierarchy.length), childHierarchy)) {
                        continue;
                    }
                    QName propName = propDef.getName();
                    @SuppressWarnings("unchecked")
                    List<Object> list = (List<Object>) node.getProperties().get(propName.toString());
                    Object value = childNode.getProperties().get(propName.toString());

                    if (propHierarchy.length == childHierarchy.length) {
                        // prop is directly on child node
                        if (!propDef.isMultiValued()) {
                            // duplicate later
                            Assert.isTrue(!(value instanceof Collection));
                            propsToDuplicate.put(propName, value);
                            continue;
                        } else if (value != null) {
                            Assert.isTrue(value instanceof Collection);
                        }
                    } else {
                        // prop is on grand-child node, it means that on child node it is already a collected List
                        @SuppressWarnings("unchecked")
                        List<Object> valueList = (List<Object>) value;
                        if (!propDef.isMultiValued()) {
                            // All valueList-s must be the same size
                            if (nrOfTimesToDuplicate < 0) {
                                nrOfTimesToDuplicate = valueList.size();
                            } else {
                                Assert.isTrue(nrOfTimesToDuplicate == valueList.size());
                            }
                        }
                    }

                    if (value instanceof Collection) {
                        for (Object object : (Collection<?>) ((Collection<?>) value)) {
                            list.add(object);
                        }
                    } else {
                        list.add(value);
                    }

                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Setting on " + node.getType().toPrefixString(namespaceService) + " prop " + propName.toPrefixString(namespaceService)
                                + " to " + list);
                    }
                    node.getProperties().put(propName.toString(), list);
                }
                Assert.isTrue(nrOfTimesToDuplicate != 0);
                if (nrOfTimesToDuplicate < 1) {
                    nrOfTimesToDuplicate = 1;
                }
                for (Entry<QName, Object> entry : propsToDuplicate.entrySet()) {
                    QName propName = entry.getKey();
                    Object value = entry.getValue();

                    @SuppressWarnings("unchecked")
                    List<Object> list = (List<Object>) node.getProperties().get(propName.toString());

                    for (int i = 0; i < nrOfTimesToDuplicate; i++) {
                        list.add(value);
                    }
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Duplicated (" + nrOfTimesToDuplicate + "), setting on " + node.getType().toPrefixString(namespaceService) + " prop "
                                + propName.toPrefixString(namespaceService) + " to " + list);
                    }
                    node.getProperties().put(propName.toString(), list);
                }
            }
        }
    }

    private DocumentPropertiesChangeHolder saveThisNodeAndChildNodes(NodeRef parentRef, Node node, List<TreeNode<QName>> childAssocTypeQNames,
            QName[] currentHierarchy, DocumentServiceImpl.PropertyChangesMonitorHelper propertyChangesMonitorHelper, Map<String, Pair<DynamicPropertyDefinition, Field>> propDefs) {
        NodeRef nodeRef;
        if (currentHierarchy == null) {
            currentHierarchy = new QName[] {};
        }
        DocumentPropertiesChangeHolder docPropsChangeHolder = new DocumentPropertiesChangeHolder();
        if (RepoUtil.isUnsaved(node)) {
            Map<QName, Serializable> props = RepoUtil.toQNameProperties(node.getProperties(), false, true);
            QName typeQName = node.getType();
            nodeRef = nodeService.createNode(parentRef, typeQName, typeQName, typeQName, props).getChildRef();
            generalService.setAspectsIgnoringSystem(nodeRef, node.getAspects());
            docPropsChangeHolder.addChange(nodeRef, typeQName, null, node);
        } else {
            generalService.setAspectsIgnoringSystem(node);

            List<QName> ignoredProps = new ArrayList<QName>();
            if (propDefs != null) {
                for (Pair<DynamicPropertyDefinition, Field> pair : propDefs.values()) {
                    DynamicPropertyDefinition propDef = pair.getFirst();
                    QName[] propHierarchy = propDef.getChildAssocTypeQNameHierarchy();
                    if (propHierarchy == null) {
                        propHierarchy = new QName[] {};
                    }
                    if (!Arrays.equals(propHierarchy, currentHierarchy)) {
                        ignoredProps.add(pair.getFirst().getName());
                    }
                }
            }
            QName[] ignoredPropsArray = ignoredProps.toArray(new QName[ignoredProps.size()]);
            docPropsChangeHolder = propertyChangesMonitorHelper.setPropertiesIgnoringSystemAndReturnNewValues(node.getNodeRef(), node.getProperties(), ignoredPropsArray);
            generalService.saveRemovedChildAssocs(node, docPropsChangeHolder);
            nodeRef = node.getNodeRef();
        }

        for (TreeNode<QName> childAssocTypeQName : childAssocTypeQNames) {
            QName assocTypeQName = childAssocTypeQName.getData();
            List<Node> childNodes = node.getAllChildAssociations(assocTypeQName);
            if (childNodes != null) {
                for (Node childNode : childNodes) {
                    QName[] childHierarchy = (QName[]) ArrayUtils.add(currentHierarchy, assocTypeQName);
                    DocumentPropertiesChangeHolder changedChildNodePropsNewValues = saveThisNodeAndChildNodes(nodeRef, childNode,
                            childAssocTypeQName.getChildren(), childHierarchy, propertyChangesMonitorHelper, propDefs);
                    docPropsChangeHolder.addChanges(changedChildNodePropsNewValues);
                }
            }
        }
        return docPropsChangeHolder;
    }

    // START: setters
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

    public void setDocumentService(DocumentService documentService) {
        this.documentService = documentService;
    }

    public void setDocumentAdminService(DocumentAdminService documentAdminService) {
        this.documentAdminService = documentAdminService;
    }

    public void setDocumentConfigService(DocumentConfigService documentConfigService) {
        this.documentConfigService = documentConfigService;
    }

    public void setSendOutService(SendOutService sendOutService) {
        this.sendOutService = sendOutService;
    }

    public void setDocumentLogService(DocumentLogService documentLogService) {
        this.documentLogService = documentLogService;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    public void setDocumentTemplateService(DocumentTemplateService documentTemplateService) {
        this.documentTemplateService = documentTemplateService;
    }

    public void setFileService(FileService fileService) {
        this.fileService = fileService;
    }

    public void setShowMessageIfUnregistered(boolean showMessageIfUnregistered) {
        this.showMessageIfUnregistered = showMessageIfUnregistered;
    }

    @Override
    public boolean isShowMessageIfUnregistered() {
        return showMessageIfUnregistered;
    }

    // END: setters

}
