package ee.webmedia.alfresco.docdynamic.service;

import static ee.webmedia.alfresco.common.web.BeanHelper.getGeneralService;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.ACCESS_RESTRICTION;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.CASE;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.FILE_CONTENTS;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.FILE_NAMES;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.FUNCTION;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.OWNER_ID;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.OWNER_NAME;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.PREVIOUS_OWNER_ID;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.REG_DATE_TIME;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.REG_NUMBER;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.SERIES;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.SHORT_REG_NUMBER;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.VOLUME;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
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
import ee.webmedia.alfresco.docadmin.service.MetadataItem;
import ee.webmedia.alfresco.docconfig.bootstrap.SystematicDocumentType;
import ee.webmedia.alfresco.docconfig.bootstrap.SystematicFieldGroupNames;
import ee.webmedia.alfresco.docconfig.generator.SaveListener;
import ee.webmedia.alfresco.docconfig.generator.systematic.UserContactRelatedGroupGenerator;
import ee.webmedia.alfresco.docconfig.service.DocumentConfigService;
import ee.webmedia.alfresco.document.log.service.DocumentLogService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.sendout.service.SendOutService;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.document.service.DocumentServiceImpl;
import ee.webmedia.alfresco.document.service.EventsLoggingHelper;
import ee.webmedia.alfresco.imap.model.ImapModel;
import ee.webmedia.alfresco.template.service.DocumentTemplateService;
import ee.webmedia.alfresco.utils.MessageData;
import ee.webmedia.alfresco.utils.MessageDataImpl;
import ee.webmedia.alfresco.utils.MessageDataWrapper;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;
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
    public DocumentDynamic createNewDocument(String documentTypeId, NodeRef parent) {
        QName type = DocumentCommonModel.Types.DOCUMENT;
        Map<QName, Serializable> props = new HashMap<QName, Serializable>();
        DocumentTypeVersion docVer = setTypeProps(documentTypeId, props);

        // TODO temporary
        props.put(DocumentCommonModel.Props.DOC_STATUS, DocumentStatus.WORKING.getValueName()); // / FIXME should be handled by setDefaultPropertyValues

        // LinkedHashSet<QName> aspects = generalService.getDefaultAspects(type);

        // for (Field field : docVer.getFieldsDeeply()) {
        // if (!field.getFieldId().getNamespaceURI().equals(DocumentDynamicModel.URI)) {
        // PropertyDefinition propDef = dictionaryService.getProperty(field.getFieldId());
        // aspects.add(propDef.getContainerClass().getName());
        // RepoUtil.getMandatoryAspects(propDef.getContainerClass(), aspects);
        // }
        // }

        // TODO temporary
        // for (QName docAspect : aspects) {
        // documentService.callbackAspectProperiesModifier(docAspect, props);
        // }

        NodeRef docRef = nodeService.createNode(parent, DocumentCommonModel.Assocs.DOCUMENT, DocumentCommonModel.Assocs.DOCUMENT, type,
                props).getChildRef();

        // for (QName aspect : aspects) {
        // if (!nodeService.hasAspect(docRef, aspect)) {
        // LOG.info("Adding aspect: " + aspect.toPrefixString(namespaceService));
        // nodeService.addAspect(docRef, aspect, null);
        // }
        // }

        DocumentDynamic document = getDocumentWithInMemoryChangesForEditing(docRef);
        WmNode docNode = document.getNode();
        documentConfigService.setDefaultPropertyValues(docNode, docVer);

        createSubnodes(docVer, document.getNode(), false);

        generalService.setPropertiesIgnoringSystem(docRef, docNode.getProperties());
        return document;
    }

    private List<Node> createSubnodes(DocumentTypeVersion docVer, Node document, boolean createInMemory) {
        List<Node> subnodeRefs = new ArrayList<Node>();
        Map<String, Object> docProps = document.getProperties();
        for (MetadataItem metadataItem : docVer.getMetadata()) {
            if (metadataItem instanceof FieldGroup) {
                FieldGroup group = (FieldGroup) metadataItem;
                if (group.getName().equals(SystematicFieldGroupNames.CONTRACT_PARTIES)) {
                    Map<QName, Serializable> subNodeProps = new HashMap<QName, Serializable>();
                    for (Field field : group.getFields()) {
                        if (docProps.containsKey(field.getQName().toString())) {
                            Serializable value = (Serializable) docProps.remove(field.getQName().toString());
                            subNodeProps.put(field.getQName(), value);
                        }
                    }
                    Pair<Field, Integer> primaryFieldAndIndex = UserContactRelatedGroupGenerator.getPrimaryFieldAndIndex(group);
                    if (!createInMemory) {
                        NodeRef subNodeRef = nodeService.createNode(
                                document.getNodeRef(),
                                DocumentCommonModel.Types.METADATA_CONTAINER,
                                primaryFieldAndIndex.getFirst().getQName(),
                                DocumentCommonModel.Types.METADATA_CONTAINER,
                                subNodeProps).getChildRef();
                        subnodeRefs.add(new Node(subNodeRef));
                    } else {
                        WmNode subNode = getGeneralService().createNewUnSaved(DocumentCommonModel.Types.METADATA_CONTAINER, null);
                        subnodeRefs.add(subNode);
                    }

                }
            }
        }
        return subnodeRefs;
    }

    private DocumentTypeVersion setTypeProps(String documentTypeId, Map<QName, Serializable> props) {
        DocumentType documentType = documentAdminService.getDocumentType(documentTypeId, DocumentAdminService.DOC_TYPE_WITH_OUT_GRAND_CHILDREN_EXEPT_LATEST_DOCTYPE_VER);

        props.put(Props.OBJECT_TYPE_ID, documentType.getId());
        DocumentTypeVersion docVer = documentType.getLatestDocumentTypeVersion();
        props.put(Props.OBJECT_TYPE_VERSION_NR, docVer.getVersionNr());
        return docVer;
    }

    @Override
    public DocumentDynamic createNewDocumentInDrafts(String documentTypeId) {
        NodeRef drafts = documentService.getDrafts();
        return createNewDocument(documentTypeId, drafts);
    }

    @Override
    public NodeRef copyDocument(DocumentDynamic document, Map<QName, Serializable> overriddenProperties, QName... ignoredProperty) {
        NodeRef draftRef = createNewDocumentInDrafts(document.getDocumentTypeId()).getNodeRef();
        Map<QName, Serializable> properties = RepoUtil.toQNameProperties(document.getNode().getProperties(), true);
        // Override properties if needed
        if (overriddenProperties != null) {
            properties.putAll(overriddenProperties);
        }
        // Remove unnecessary properties
        for (QName prop : ignoredProperty) {
            properties.remove(prop);
        }
        nodeService.addProperties(draftRef, properties);

        return draftRef;
    }

    @Override
    public DocumentDynamic getDocument(NodeRef docRef) {
        QName type = nodeService.getType(docRef);
        Assert.isTrue(DocumentCommonModel.Types.DOCUMENT.equals(type));
        Set<QName> aspects = RepoUtil.getAspectsIgnoringSystem(nodeService.getAspects(docRef));
        Map<QName, Serializable> props = RepoUtil.getPropertiesIgnoringSystem(nodeService.getProperties(docRef), dictionaryService);
        WmNode docNode = new WmNode(docRef, type, aspects, props);
        DocumentDynamic doc = new DocumentDynamic(docNode);
        LOG.info("getDocument document=" + doc);
        return doc;
    }

    @Override
    public DocumentDynamic getDocumentWithInMemoryChangesForEditing(NodeRef docRef) {
        DocumentDynamic document = getDocument(docRef);
        setParentFolderProps(document);
        if (document.isImapOrDvk()) {
            Pair<DocumentType, DocumentTypeVersion> documentTypeAndVersion = documentConfigService.getDocumentTypeAndVersion(document.getNode());
            Collection<Field> ownerNameFields = documentTypeAndVersion.getSecond().getFieldsById(Collections.singleton(DocumentCommonModel.Props.OWNER_NAME.getLocalName()));
            if (ownerNameFields.size() == 1) {
                Field ownerNameField = ownerNameFields.iterator().next();
                if (ownerNameField.isSystematic() && ownerNameField.getFieldId().equals(ownerNameField.getOriginalFieldId()) && ownerNameField.getParent() instanceof FieldGroup) {
                    documentConfigService.setDefaultPropertyValues(document.getNode(), ((FieldGroup) ownerNameField.getParent()).getFields(), true);
                }
            }
        }
        return document;
    }

    @Override
    public void changeTypeInMemory(DocumentDynamic document, String newTypeId) {
        Map<QName, Serializable> typeProps = new HashMap<QName, Serializable>();
        DocumentTypeVersion docVer = setTypeProps(newTypeId, typeProps);

        document.getNode().getAspects().clear();
        document.getNode().getAllChildAssociationsByAssocType().clear();
        document.getNode().getRemovedChildAssociations().clear();

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

        documentConfigService.setDefaultPropertyValues(document.getNode(), docVer);

        // remove all existing subnodes in memory and create new subnodes if needed
        QName subnodeAssoc = DocumentCommonModel.Types.METADATA_CONTAINER;
        List<Node> subnodes = document.getNode().getAllChildAssociationsByAssocType().get(subnodeAssoc);
        if (subnodes != null && !subnodes.isEmpty()) {
            document.getNode().removeChildAssociations(subnodeAssoc, subnodes);
        }

        List<Node> newSubnodes = createSubnodes(docVer, document.getNode(), true);
        for (Node subnode : newSubnodes) {
            document.getNode().addChildAssociations(subnodeAssoc, subnode);
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

        // If document is updated for the first time, add SEARCHABLE aspect to document and it's children files.
        Map<String, Object> docProps = document.getNode().getProperties();
        if (!nodeService.hasAspect(docRef, DocumentCommonModel.Aspects.SEARCHABLE)) {
            nodeService.addAspect(docRef, DocumentCommonModel.Aspects.SEARCHABLE, null);
            docProps.put(FILE_NAMES.toString(), documentService.getSearchableFileNames(docRef));
            docProps.put(FILE_CONTENTS.toString(), documentService.getSearchableFileContents(docRef));
            docProps.put(DocumentCommonModel.Props.SEARCHABLE_SEND_MODE.toString(), sendOutService.buildSearchableSendMode(docRef));
        }

        LOG.info("updateDocument after validation and save listeners, before real saving: " + document);

        // generalService.saveAddedAssocs(document.getNode());

        { // update properties and log changes made in properties
            DocumentServiceImpl.PropertyChangesMonitorHelper propertyChangesMonitorHelper = new DocumentServiceImpl.PropertyChangesMonitorHelper();// FIXME:
            boolean propsChanged = propertyChangesMonitorHelper.setPropertiesIgnoringSystemAndReturnIfChanged(docRef, docProps //
                    , FUNCTION, SERIES, VOLUME, CASE // location changes
                    , REG_NUMBER, SHORT_REG_NUMBER, REG_DATE_TIME // registration changes
                    , ACCESS_RESTRICTION // access restriction changed
                    );
            propsChanged |= saveChildNodes(documentOriginal.getNode() /* TODO ??? */, propertyChangesMonitorHelper);
            if (!EventsLoggingHelper.isLoggingDisabled(document.getNode(), DocumentService.TransientProps.TEMP_LOGGING_DISABLED_DOCUMENT_METADATA_CHANGED)) {
                if (document.isDraft()) {
                    documentLogService.addDocumentLog(docRef, MessageUtil.getMessage("document_log_status_created"));
                } else if (propsChanged) {
                    documentLogService.addDocumentLog(docRef, MessageUtil.getMessage("document_log_status_changed"));
                }
            }
        }

        documentTemplateService.updateGeneratedFiles(docRef, false);
        if (document.isDraftOrImapOrDvk()) {
            documentService.addPrivilegesBasedOnSeriesOnBackground(docRef);
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

    private boolean saveChildNodes(Node docNode, DocumentServiceImpl.PropertyChangesMonitorHelper propertyChangesMonitorHelper) {
        boolean propsChanged = false;
        propsChanged |= saveRemovedChildAssocsAndReturnCount(docNode) > 0;

        QName partyAssoc = DocumentCommonModel.Types.METADATA_CONTAINER;
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
        return propsChanged;
    }

    private int saveRemovedChildAssocsAndReturnCount(Node applicantNode) {
        return generalService.saveRemovedChildAssocs(applicantNode);
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

    // END: setters

}
