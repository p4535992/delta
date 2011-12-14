package ee.webmedia.alfresco.docdynamic.service;

import static ee.webmedia.alfresco.common.web.BeanHelper.getGeneralService;
import static ee.webmedia.alfresco.docadmin.web.DocAdminUtil.getDocTypeIdAndVersionNr;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.ACCESS_RESTRICTION;
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
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
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
        return createNewDocument(docVer, parent);
    }

    @Override
    public Pair<DocumentDynamic, DocumentTypeVersion> createNewDocument(DocumentTypeVersion docVer, NodeRef parent) {

        Map<QName, Serializable> props = new HashMap<QName, Serializable>();
        setTypeProps(getDocTypeIdAndVersionNr(docVer), props);

        return createNewDocument(docVer, parent, props);
    }

    private Pair<DocumentDynamic, DocumentTypeVersion> createNewDocument(DocumentTypeVersion docVer, NodeRef parent, Map<QName, Serializable> props) {

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

        documentConfigService.setDefaultPropertyValues(docNode, null, false, true, docVer);

        DocumentServiceImpl.PropertyChangesMonitorHelper propertyChangesMonitorHelper = new DocumentServiceImpl.PropertyChangesMonitorHelper();
        saveThisNodeAndChildNodes(null, docNode, childAssocTypeQNamesRoot.getChildren(), propertyChangesMonitorHelper);

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
                LOG.info("node " + parentNode.getType().toPrefixString(namespaceService) + " addAspect " + containerClass.getName().toPrefixString(namespaceService));
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
            if (overrideProps.containsKey(propName)) {
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
    public NodeRef copyDocument(DocumentDynamic sourceDocument, Map<QName, Serializable> overrideProps, QName... ignoredProps) {
        DocumentDynamic targetDocument = createNewDocumentInDrafts(sourceDocument.getDocumentTypeId()).getFirst();

        Set<QName> ignoredPropsSet = new HashSet<QName>(Arrays.asList(ignoredProps));
        Map<String, Pair<DynamicPropertyDefinition, Field>> propDefs = documentConfigService.getPropertyDefinitions(targetDocument.getNode());

        TreeNode<QName> childAssocTypeQNamesRoot = documentConfigService.getChildAssocTypeQNameTree(targetDocument.getNode());
        Assert.isNull(childAssocTypeQNamesRoot.getData());

        // TODO XXX FIXME Alar: something does not work correctly with removing child nodes
        removeChildNodes(sourceDocument, childAssocTypeQNamesRoot);
        copyPropsAndChildNodesHierarchy(sourceDocument.getNode(), targetDocument.getNode(), childAssocTypeQNamesRoot.getChildren(), propDefs, overrideProps, ignoredPropsSet, null);

        DocumentServiceImpl.PropertyChangesMonitorHelper propertyChangesMonitorHelper = new DocumentServiceImpl.PropertyChangesMonitorHelper();
        saveThisNodeAndChildNodes(null, targetDocument.getNode(), childAssocTypeQNamesRoot.getChildren(), propertyChangesMonitorHelper);

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

        properties.clear();
        properties.putAll(newProps);
        setParentFolderProps(document);

        TreeNode<QName> childAssocTypeQNamesRoot = documentConfigService.getChildAssocTypeQNameTree(docVer);
        Assert.isNull(childAssocTypeQNamesRoot.getData());

        // remove all existing subnodes in memory
        removeChildNodes(document, childAssocTypeQNamesRoot);

        // create new subnodes in memory
        createChildNodesHierarchy(document.getNode(), childAssocTypeQNamesRoot.getChildren());

        // set default values in memory - does not overwrite existing values
        documentConfigService.setDefaultPropertyValues(document.getNode(), null, false, true, docVer);
    }

    private void removeChildNodes(DocumentDynamic document, TreeNode<QName> childAssocTypeQNamesRoot) {
        for (TreeNode<QName> childAssocTypeQName : childAssocTypeQNamesRoot.getChildren()) {
            QName assocTypeQName = childAssocTypeQName.getData();
            List<Node> childNodes = document.getNode().getAllChildAssociationsByAssocType().get(assocTypeQName);
            if (childNodes != null && !childNodes.isEmpty()) {
                document.getNode().removeChildAssociations(assocTypeQName, childNodes);
            }
            // We don't remove container aspects, it is not strictly necessary
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
    public DocumentDynamic updateDocument(DocumentDynamic documentOriginal, List<String> saveListenerBeanNames, boolean addPrivilegesOnBackground) {
        DocumentDynamic document = documentOriginal.clone();
        NodeRef docRef = document.getNodeRef();

        setParentFolderProps(document);

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

        WmNode docNode = document.getNode();

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

        { // update properties and log changes made in properties
            DocumentServiceImpl.PropertyChangesMonitorHelper propertyChangesMonitorHelper = new DocumentServiceImpl.PropertyChangesMonitorHelper();
            propertyChangesMonitorHelper.addIgnoredProps(docProps //
                    , REG_NUMBER, SHORT_REG_NUMBER, INDIVIDUAL_NUMBER, REG_DATE_TIME // registration changes
                    , ACCESS_RESTRICTION // access restriction changed
                    );
            List<Pair<QName, Pair<Serializable, Serializable>>> changedPropsNewValues = saveThisNodeAndChildNodesAndReturnChanged(null, document.getNode(),
                    childAssocTypeQNamesRoot.getChildren(),
                    propertyChangesMonitorHelper);
            boolean propsChanged = !changedPropsNewValues.isEmpty();
            if (!EventsLoggingHelper.isLoggingDisabled(docNode, DocumentService.TransientProps.TEMP_LOGGING_DISABLED_DOCUMENT_METADATA_CHANGED)) {
                if (document.isDraft()) {
                    documentLogService.addDocumentLog(docRef, MessageUtil.getMessage("document_log_status_created"));
                } else if (propsChanged) {
                    Map<String, Pair<DynamicPropertyDefinition, Field>> propDefs = documentConfigService.getPropertyDefinitions(document.getNode());
                    for (Pair<QName, Pair<Serializable, Serializable>> keyOldNewValuesPair : changedPropsNewValues) {
                        if (!DocumentDynamicModel.URI.equals(keyOldNewValuesPair.getFirst().getNamespaceURI())) {
                            continue;
                        }
                        Pair<DynamicPropertyDefinition, Field> pair = propDefs.get(keyOldNewValuesPair.getFirst().getLocalName());
                        Field field = pair.getSecond();
                        if (field == null) {
                            continue;
                        }
                        String originalFieldId = field.getOriginalFieldId();
                        Serializable oldValue = keyOldNewValuesPair.getSecond().getFirst();
                        Serializable newValue = keyOldNewValuesPair.getSecond().getSecond();
                        String messageKey = "document_log_location_changed";
                        if (DocumentCommonModel.Props.FUNCTION.getLocalName().equals(originalFieldId)) {
                            oldValue = nodeService.getProperty((NodeRef) oldValue, FunctionsModel.Props.MARK) + " "
                                    + nodeService.getProperty((NodeRef) oldValue, FunctionsModel.Props.TITLE);
                            newValue = nodeService.getProperty((NodeRef) newValue, FunctionsModel.Props.MARK) + " "
                                    + nodeService.getProperty((NodeRef) newValue, FunctionsModel.Props.TITLE);
                        } else if (DocumentCommonModel.Props.SERIES.getLocalName().equals(originalFieldId)) {
                            oldValue = nodeService.getProperty((NodeRef) oldValue, SeriesModel.Props.SERIES_IDENTIFIER) + " "
                                    + nodeService.getProperty((NodeRef) oldValue, SeriesModel.Props.TITLE);
                            newValue = nodeService.getProperty((NodeRef) newValue, SeriesModel.Props.SERIES_IDENTIFIER) + " "
                                    + nodeService.getProperty((NodeRef) newValue, SeriesModel.Props.TITLE);
                        } else if (DocumentCommonModel.Props.VOLUME.getLocalName().equals(originalFieldId)) {
                            oldValue = nodeService.getProperty((NodeRef) oldValue, VolumeModel.Props.MARK) + " "
                                    + nodeService.getProperty((NodeRef) oldValue, VolumeModel.Props.TITLE);
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
                }
            }

            documentTemplateService.updateGeneratedFiles(docRef, false);
            if (document.isDraftOrImapOrDvk()) {
                if (addPrivilegesOnBackground) {
                    documentService.addPrivilegesBasedOnSeriesOnBackground(docRef);
                } else {
                    documentService.addPrivilegesBasedOnSeries(docRef);
                }
            }
        }
        return document;
    }

    public void setParentFolderProps(DocumentDynamic document) {
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
        String docTypeId = (String) nodeService.getProperty(docRef, Props.OBJECT_TYPE_ID);
        return SystematicDocumentType.OUTGOING_LETTER.getId().equals(docTypeId);
    }

    @Override
    public String getDocumentTypeName(NodeRef documentRef) {
        String docTypeIdOfDoc = (String) nodeService.getProperty(documentRef, Props.OBJECT_TYPE_ID);
        return documentAdminService.getDocumentTypeProperty(docTypeIdOfDoc, DocumentAdminModel.Props.NAME, String.class);
    }

    private List<Pair<QName, Pair<Serializable, Serializable>>> saveThisNodeAndChildNodesAndReturnChanged(NodeRef parentRef, Node node, List<TreeNode<QName>> childAssocTypeQNames,
            DocumentServiceImpl.PropertyChangesMonitorHelper propertyChangesMonitorHelper) {
        boolean propsChanged = false;
        NodeRef nodeRef;
        List<Pair<QName, Pair<Serializable, Serializable>>> changedPropsNewValues = new ArrayList<Pair<QName, Pair<Serializable, Serializable>>>();
        if (RepoUtil.isUnsaved(node)) {
            propsChanged = true;
            Map<QName, Serializable> props = RepoUtil.toQNameProperties(node.getProperties(), false, true);
            nodeRef = nodeService.createNode(parentRef, node.getType(), node.getType(), node.getType(), props).getChildRef();
            generalService.setAspectsIgnoringSystem(nodeRef, node.getAspects());
        } else {
            generalService.setAspectsIgnoringSystem(node);
            changedPropsNewValues = propertyChangesMonitorHelper.setPropertiesIgnoringSystemAndReturnNewValues(node.getNodeRef(), node.getProperties());
            propsChanged |= !changedPropsNewValues.isEmpty();
            propsChanged |= generalService.saveRemovedChildAssocs(node) > 0;
            nodeRef = node.getNodeRef();
        }

        for (TreeNode<QName> childAssocTypeQName : childAssocTypeQNames) {
            QName assocTypeQName = childAssocTypeQName.getData();
            List<Node> childNodes = node.getAllChildAssociations(assocTypeQName);
            if (childNodes != null) {
                for (Node childNode : childNodes) {
                    List<Pair<QName, Pair<Serializable, Serializable>>> changedChildNodePropsNewValues = saveThisNodeAndChildNodesAndReturnChanged(nodeRef, childNode,
                            childAssocTypeQName.getChildren(), propertyChangesMonitorHelper);
                    if (!changedChildNodePropsNewValues.isEmpty()) {
                        changedPropsNewValues.addAll(changedChildNodePropsNewValues);
                    }
                }
            }
        }
        return changedPropsNewValues;
    }

    private boolean saveThisNodeAndChildNodes(NodeRef parentRef, Node node, List<TreeNode<QName>> childAssocTypeQNames,
            DocumentServiceImpl.PropertyChangesMonitorHelper propertyChangesMonitorHelper) {
        return !saveThisNodeAndChildNodesAndReturnChanged(parentRef, node, childAssocTypeQNames, propertyChangesMonitorHelper).isEmpty();
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
