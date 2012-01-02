package ee.webmedia.alfresco.docdynamic.service;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentConfigService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getGeneralService;
import static ee.webmedia.alfresco.docadmin.web.DocAdminUtil.getDocTypeIdAndVersionNr;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.FILE_CONTENTS;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.FILE_NAMES;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.INDIVIDUAL_NUMBER;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.OWNER_ID;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.OWNER_NAME;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.PREVIOUS_OWNER_ID;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.REG_DATE_TIME;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.REG_NUMBER;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.SHORT_REG_NUMBER;

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

import ee.webmedia.alfresco.cases.model.CaseModel;
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
import ee.webmedia.alfresco.docconfig.generator.systematic.AccessRestrictionGenerator;
import ee.webmedia.alfresco.docconfig.generator.systematic.DocumentLocationGenerator;
import ee.webmedia.alfresco.docconfig.service.DocumentConfig;
import ee.webmedia.alfresco.docconfig.service.DocumentConfigService;
import ee.webmedia.alfresco.docconfig.service.DynamicPropertyDefinition;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.document.log.service.DocumentLogService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.sendout.service.SendOutService;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.document.service.DocumentServiceImpl;
import ee.webmedia.alfresco.document.service.EventsLoggingHelper;
import ee.webmedia.alfresco.functions.model.FunctionsModel;
import ee.webmedia.alfresco.imap.model.ImapModel;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.template.service.DocumentTemplateService;
import ee.webmedia.alfresco.utils.MessageData;
import ee.webmedia.alfresco.utils.MessageDataImpl;
import ee.webmedia.alfresco.utils.MessageDataWrapper;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.TreeNode;
import ee.webmedia.alfresco.utils.UnableToPerformMultiReasonException;
import ee.webmedia.alfresco.volume.model.VolumeModel;

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

        createChildNodesHierarchy(docNode, childAssocTypeQNamesRoot.getChildren());

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

        List<Pair<QName, WmNode>> childNodes = createChildNodesHierarchy(parentNode, Collections.singletonList(current));
        Assert.isTrue(childNodes.size() == 1);

        documentConfigService.setDefaultPropertyValues(childNodes.get(0).getSecond(), hierarchy, false, false, docVer);
    }

    private List<Pair<QName, WmNode>> createChildNodesHierarchy(Node parentNode, List<TreeNode<QName>> childAssocTypeQNames) {
        List<Pair<QName, WmNode>> childNodes = new ArrayList<Pair<QName, WmNode>>();
        for (TreeNode<QName> childAssocTypeQName : childAssocTypeQNames) {
            QName assocTypeQName = childAssocTypeQName.getData();
            addContainerAspectIfNecessary(parentNode, assocTypeQName);
            Map<QName, Serializable> props = new HashMap<QName, Serializable>();
            // objectTypeId and objectTypeVersion are set on every child node, because if
            // documentConfigService.getPropertyDefinition is called, then we don't have to find parent document
            setTypeProps(getDocTypeIdAndVersionNr(parentNode), props);
            WmNode childNode = getGeneralService().createNewUnSaved(assocTypeQName, props);
            parentNode.addChildAssociations(assocTypeQName, childNode);
            childNodes.add(Pair.newInstance(assocTypeQName, childNode));
            createChildNodesHierarchy(childNode, childAssocTypeQName.getChildren());
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
                    WmNode targetChildNode = getGeneralService().createNewUnSaved(assocTypeQName, props);
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

        Map<String, Object> properties = document.getNode().getProperties();
        Map<String, Object> newProps = new HashMap<String, Object>();
        newProps.putAll(RepoUtil.toStringProperties(typeProps));
        newProps.put(DocumentCommonModel.Props.DOC_NAME.toString(), properties.get(DocumentCommonModel.Props.DOC_NAME));
        newProps.put(DocumentCommonModel.Props.ACCESS_RESTRICTION.toString(), properties.get(DocumentCommonModel.Props.ACCESS_RESTRICTION));
        newProps.put(DocumentCommonModel.Props.ACCESS_RESTRICTION_BEGIN_DATE.toString(), properties.get(DocumentCommonModel.Props.ACCESS_RESTRICTION_BEGIN_DATE));
        newProps.put(DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DATE.toString(), properties.get(DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DATE));
        newProps.put(DocumentCommonModel.Props.ACCESS_RESTRICTION_REASON.toString(), properties.get(DocumentCommonModel.Props.ACCESS_RESTRICTION_REASON));
        newProps.put(DocumentCommonModel.Props.DOC_STATUS.toString(), properties.get(DocumentCommonModel.Props.DOC_STATUS));
        newProps.put(DocumentCommonModel.Props.STORAGE_TYPE.toString(), properties.get(DocumentCommonModel.Props.STORAGE_TYPE));

        // remove all existing subnodes in memory
        TreeNode<QName> oldChildAssocTypeQNamesRoot = documentConfigService.getChildAssocTypeQNameTree(document.getNode());
        Assert.isNull(oldChildAssocTypeQNamesRoot.getData());
        removeChildNodes(document, oldChildAssocTypeQNamesRoot);

        properties.clear();
        properties.putAll(newProps);
        setParentFolderProps(document);

        TreeNode<QName> childAssocTypeQNamesRoot = documentConfigService.getChildAssocTypeQNameTree(docVer);
        Assert.isNull(childAssocTypeQNamesRoot.getData());

        // create new subnodes in memory
        createChildNodesHierarchy(document.getNode(), childAssocTypeQNamesRoot.getChildren());

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
    public DocumentDynamic updateDocument(DocumentDynamic documentOriginal, List<String> saveListenerBeanNames) {
        return updateDocumentGetDocAndNodeRefs(documentOriginal, saveListenerBeanNames).getFirst();
    }

    @Override
    public List<NodeRef> updateDocumentGetOriginalNodeRefs(DocumentDynamic documentOriginal, List<String> saveListenerBeanNames) {
        return updateDocumentGetDocAndNodeRefs(documentOriginal, saveListenerBeanNames).getSecond();
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

    private Pair<DocumentDynamic, List<NodeRef>> updateDocumentGetDocAndNodeRefs(DocumentDynamic documentOriginal, List<String> saveListenerBeanNames) {
        // originalDocumentNodeRef may be null, but in that case it must be the only null nodeRef among all documents saved during this operation
        NodeRef originalDocumentNodeRef = documentOriginal.getNodeRef();
        List<DocumentDynamic> associatedDocs = new ArrayList<DocumentDynamic>();
        Set<NodeRef> checkedDocs = new HashSet<NodeRef>();
        associatedDocs.add(documentOriginal);
        NodeRef functionRef = documentOriginal.getFunction();
        NodeRef seriesRef = documentOriginal.getSeries();
        NodeRef volumeRef = documentOriginal.getVolume();
        NodeRef caseRef = documentOriginal.getCase();
        String caseLabel = documentOriginal.getProp(DocumentLocationGenerator.CASE_LABEL_EDITABLE);
        getAssociatedDocRefs(documentOriginal, associatedDocs, checkedDocs);
        DocumentDynamic originalDocumentUpdated = null;
        List<NodeRef> originalNodeRefs = new ArrayList<NodeRef>();
        Collections.sort(associatedDocs, DOCUMENT_BY_REG_DATE_TIME_COMPARATOR);
        for (DocumentDynamic associatedDocument : associatedDocs) {
            if (!associatedDocument.getNodeRef().getId().equals(originalDocumentNodeRef.getId())) {
                DocumentConfig cfg = getDocumentConfigService().getConfig(associatedDocument.getNode());
                associatedDocument.setFunction(functionRef);
                associatedDocument.setSeries(seriesRef);
                associatedDocument.setVolume(volumeRef);
                associatedDocument.setCase(caseRef);
                associatedDocument.setProp(DocumentLocationGenerator.CASE_LABEL_EDITABLE, caseLabel);
                originalNodeRefs.add(associatedDocument.getNodeRef());
                update(associatedDocument, cfg.getSaveListenerBeanNames());
            } else {
                originalDocumentUpdated = update(associatedDocument, saveListenerBeanNames);
            }
        }
        return new Pair<DocumentDynamic, List<NodeRef>>(originalDocumentUpdated, originalNodeRefs);
    }

    // NB! This method may change document nodeRef when moving from archive to active store (see DocumentLocationGenerator save method)
    private DocumentDynamic update(DocumentDynamic documentOriginal, List<String> saveListenerBeanNames) {
        DocumentDynamic document = documentOriginal.clone();

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

        if (LOG.isDebugEnabled()) {
            LOG.debug("updateDocument after validation and save listeners, before real saving: " + document);
        }

        TreeNode<QName> childAssocTypeQNamesRoot = documentConfigService.getChildAssocTypeQNameTree(document.getNode());
        Assert.isNull(childAssocTypeQNamesRoot.getData());

        Map<String, Pair<DynamicPropertyDefinition, Field>> propDefs = documentConfigService.getPropertyDefinitions(document.getNode());

        updateSearchableChildNodeProps(docNode, null, childAssocTypeQNamesRoot.getChildren(), propDefs);

        { // update properties and log changes made in properties
            DocumentServiceImpl.PropertyChangesMonitorHelper propertyChangesMonitorHelper = new DocumentServiceImpl.PropertyChangesMonitorHelper();
            propertyChangesMonitorHelper.addIgnoredProps(docProps //
                    , REG_NUMBER, SHORT_REG_NUMBER, INDIVIDUAL_NUMBER, REG_DATE_TIME // registration changes
                    );
            // TODO refactor, so that accessRestriction changes would be logged here, not in AccessRestrictionGenerator
            propertyChangesMonitorHelper.addIgnoredProps(docProps //
                    , AccessRestrictionGenerator.ACCESS_RESTRICTION_PROPS // access restriction changed
                    );
            List<Pair<QName, Pair<Serializable, Serializable>>> changedPropsNewValues = saveThisNodeAndChildNodes(null, docNode,
                    childAssocTypeQNamesRoot.getChildren(), null, propertyChangesMonitorHelper, propDefs);
            boolean propsChanged = !changedPropsNewValues.isEmpty();
            if (!EventsLoggingHelper.isLoggingDisabled(docNode, DocumentService.TransientProps.TEMP_LOGGING_DISABLED_DOCUMENT_METADATA_CHANGED)) {
                if (document.isDraft()) {
                    documentLogService.addDocumentLog(docRef, MessageUtil.getMessage("document_log_status_created"));
                } else if (propsChanged) {
                    for (Pair<QName, Pair<Serializable, Serializable>> keyOldNewValuesPair : changedPropsNewValues) {
                        logChangedProp(docRef, propDefs, keyOldNewValuesPair);
                    }
                }
            }

            documentTemplateService.updateGeneratedFiles(docRef, false);
        }
        generalService.saveAddedAssocs(docNode);
        return document;
    }

    // NB! It is assumed that only one unsaved document may occur in the updated hierarchy (i.e. one document with nodeRef = null)
    // Otherwise all unsaved nodes are considered as same nodeRef and only the first one is processed
    private void getAssociatedDocRefs(DocumentDynamic document, List<DocumentDynamic> associatedDocs, Set<NodeRef> checkedDocs) {
        NodeRef docRef = document.getNodeRef();
        Set<DocumentDynamic> currentAssociatedDocs = new HashSet<DocumentDynamic>();
        if (checkedDocs.contains(docRef)) {
            return;
        }
        checkedDocs.add(docRef);

        Collection<AssociationRef> newAssocs = new ArrayList<AssociationRef>();
        Map<String, AssociationRef> addedAssocs = document.getNode().getAddedAssociations().get(DocumentCommonModel.Assocs.DOCUMENT_REPLY.toString());
        if (addedAssocs != null) {
            newAssocs.addAll(addedAssocs.values());
        }
        addedAssocs = document.getNode().getAddedAssociations().get(DocumentCommonModel.Assocs.DOCUMENT_FOLLOW_UP.toString());
        if (addedAssocs != null) {
            newAssocs.addAll(addedAssocs.values());
        }
        if (newAssocs != null) {
            for (AssociationRef assocRef : newAssocs) {
                NodeRef sourceRef = assocRef.getSourceRef();
                if (!sourceRef.equals(docRef) && !containsDocument(sourceRef, associatedDocs) && isSearchable(sourceRef)) {
                    currentAssociatedDocs.add(getDocument(sourceRef));
                }
                NodeRef targetRef = assocRef.getTargetRef();
                if (!targetRef.equals(docRef) && !containsDocument(targetRef, associatedDocs) && isSearchable(targetRef)) {
                    currentAssociatedDocs.add(getDocument(targetRef));
                }
            }
        }
        if (docRef != null) {
            List<AssociationRef> targetAssocs = nodeService.getTargetAssocs(docRef, DocumentCommonModel.Assocs.DOCUMENT_REPLY);
            targetAssocs.addAll(nodeService.getTargetAssocs(docRef, DocumentCommonModel.Assocs.DOCUMENT_FOLLOW_UP));
            for (AssociationRef assoc : targetAssocs) {
                NodeRef targetRef = assoc.getTargetRef();
                if (!containsDocument(targetRef, associatedDocs) && isSearchable(targetRef)) {
                    currentAssociatedDocs.add(getDocument(targetRef));
                }
            }
            List<AssociationRef> sourceAssocs = nodeService.getSourceAssocs(docRef, DocumentCommonModel.Assocs.DOCUMENT_REPLY);
            sourceAssocs.addAll(nodeService.getSourceAssocs(docRef, DocumentCommonModel.Assocs.DOCUMENT_FOLLOW_UP));
            for (AssociationRef assoc : sourceAssocs) {
                NodeRef sourceRef = assoc.getSourceRef();
                if (!containsDocument(sourceRef, associatedDocs) && isSearchable(sourceRef)) {
                    currentAssociatedDocs.add(getDocument(sourceRef));
                }
            }
        }
        associatedDocs.addAll(currentAssociatedDocs);
        for (DocumentDynamic associatedDoc : currentAssociatedDocs) {
            getAssociatedDocRefs(associatedDoc, associatedDocs, checkedDocs);
        }
    }

    private boolean isSearchable(NodeRef sourceRef) {
        return nodeService.hasAspect(sourceRef, DocumentCommonModel.Aspects.SEARCHABLE);
    }

    private boolean containsDocument(NodeRef sourceRef, List<DocumentDynamic> associatedDocs) {
        if (associatedDocs == null) {
            return false;
        }
        for (DocumentDynamic document : associatedDocs) {
            if (document.getNodeRef().equals(sourceRef)) {
                return true;
            }
        }
        return false;
    }

    private void logChangedProp(NodeRef docRef, Map<String, Pair<DynamicPropertyDefinition, Field>> propDefs, Pair<QName, Pair<Serializable, Serializable>> keyOldNewValuesPair) {
        if (!DocumentDynamicModel.URI.equals(keyOldNewValuesPair.getFirst().getNamespaceURI())) {
            return;
        }
        Pair<DynamicPropertyDefinition, Field> pair = propDefs.get(keyOldNewValuesPair.getFirst().getLocalName());
        Field field = pair.getSecond();
        if (field == null) {
            return;
        }
        String originalFieldId = field.getOriginalFieldId();
        Serializable oldValue = keyOldNewValuesPair.getSecond().getFirst();
        Serializable newValue = keyOldNewValuesPair.getSecond().getSecond();
        String messageKey = "document_log_location_changed";
        if (DocumentCommonModel.Props.FUNCTION.getLocalName().equals(originalFieldId)) {
            NodeRef functionRef = (NodeRef) oldValue;
            if (functionRef != null) {
                oldValue = nodeService.getProperty(functionRef, FunctionsModel.Props.MARK) + " "
                        + nodeService.getProperty(functionRef, FunctionsModel.Props.TITLE);
            }
            newValue = nodeService.getProperty((NodeRef) newValue, FunctionsModel.Props.MARK) + " "
                    + nodeService.getProperty((NodeRef) newValue, FunctionsModel.Props.TITLE);
        } else if (DocumentCommonModel.Props.SERIES.getLocalName().equals(originalFieldId)) {
            NodeRef seriesRef = (NodeRef) oldValue;
            if (seriesRef != null) {
                oldValue = nodeService.getProperty(seriesRef, SeriesModel.Props.SERIES_IDENTIFIER) + " "
                        + nodeService.getProperty(seriesRef, SeriesModel.Props.TITLE);
            }
            newValue = nodeService.getProperty((NodeRef) newValue, SeriesModel.Props.SERIES_IDENTIFIER) + " "
                    + nodeService.getProperty((NodeRef) newValue, SeriesModel.Props.TITLE);
        } else if (DocumentCommonModel.Props.VOLUME.getLocalName().equals(originalFieldId)) {
            NodeRef volumeRef = (NodeRef) oldValue;
            if (volumeRef != null) {
                oldValue = nodeService.getProperty(volumeRef, VolumeModel.Props.MARK) + " "
                        + nodeService.getProperty(volumeRef, VolumeModel.Props.TITLE);
            }
            newValue = nodeService.getProperty((NodeRef) newValue, VolumeModel.Props.MARK) + " "
                    + nodeService.getProperty((NodeRef) newValue, VolumeModel.Props.TITLE);
        } else if (DocumentCommonModel.Props.CASE.getLocalName().equals(originalFieldId)) {
            if (oldValue != null) {
                oldValue = nodeService.getProperty((NodeRef) oldValue, CaseModel.Props.TITLE);
            }
            if (newValue != null) {
                newValue = nodeService.getProperty((NodeRef) newValue, CaseModel.Props.TITLE);
            }
        } else {
            messageKey = "document_log_status_changed";
        }
        String message = MessageUtil.getMessage(messageKey, field.getName(), oldValue, newValue);
        documentLogService.addDocumentLog(docRef, message);
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

    private List<Pair<QName, Pair<Serializable, Serializable>>> saveThisNodeAndChildNodes(NodeRef parentRef, Node node, List<TreeNode<QName>> childAssocTypeQNames,
            QName[] currentHierarchy, DocumentServiceImpl.PropertyChangesMonitorHelper propertyChangesMonitorHelper, Map<String, Pair<DynamicPropertyDefinition, Field>> propDefs) {
        boolean propsChanged = false;
        NodeRef nodeRef;
        if (currentHierarchy == null) {
            currentHierarchy = new QName[] {};
        }
        List<Pair<QName, Pair<Serializable, Serializable>>> changedPropsNewValues = new ArrayList<Pair<QName, Pair<Serializable, Serializable>>>();
        if (RepoUtil.isUnsaved(node)) {
            propsChanged = true;
            Map<QName, Serializable> props = RepoUtil.toQNameProperties(node.getProperties(), false, true);
            nodeRef = nodeService.createNode(parentRef, node.getType(), node.getType(), node.getType(), props).getChildRef();
            generalService.setAspectsIgnoringSystem(nodeRef, node.getAspects());
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
            changedPropsNewValues = propertyChangesMonitorHelper.setPropertiesIgnoringSystemAndReturnNewValues(node.getNodeRef(), node.getProperties(), ignoredPropsArray);
            propsChanged |= !changedPropsNewValues.isEmpty();
            propsChanged |= generalService.saveRemovedChildAssocs(node) > 0;
            nodeRef = node.getNodeRef();
        }

        for (TreeNode<QName> childAssocTypeQName : childAssocTypeQNames) {
            QName assocTypeQName = childAssocTypeQName.getData();
            List<Node> childNodes = node.getAllChildAssociations(assocTypeQName);
            if (childNodes != null) {
                for (Node childNode : childNodes) {
                    QName[] childHierarchy = (QName[]) ArrayUtils.add(currentHierarchy, assocTypeQName);
                    List<Pair<QName, Pair<Serializable, Serializable>>> changedChildNodePropsNewValues = saveThisNodeAndChildNodes(nodeRef, childNode,
                            childAssocTypeQName.getChildren(), childHierarchy, propertyChangesMonitorHelper, propDefs);
                    changedPropsNewValues.addAll(changedChildNodePropsNewValues);
                }
            }
        }
        return changedPropsNewValues;
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

    public void setShowMessageIfUnregistered(boolean showMessageIfUnregistered) {
        this.showMessageIfUnregistered = showMessageIfUnregistered;
    }

    @Override
    public boolean isShowMessageIfUnregistered() {
        return showMessageIfUnregistered;
    }

    // END: setters

}