package ee.webmedia.alfresco.docdynamic.service;

import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.ACCESS_RESTRICTION;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.CASE;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.FILE_CONTENTS;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.FILE_NAMES;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.FUNCTION;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.REG_DATE_TIME;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.REG_NUMBER;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.SERIES;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.SHORT_REG_NUMBER;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.VOLUME;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;
import ee.webmedia.alfresco.docadmin.service.DocumentType;
import ee.webmedia.alfresco.docadmin.service.DocumentTypeVersion;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docconfig.generator.SaveListener;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.document.log.service.DocumentLogService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.sendout.service.SendOutService;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.document.service.DocumentServiceImpl;
import ee.webmedia.alfresco.document.service.EventsLoggingHelper;
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
    private SendOutService sendOutService;
    private DocumentLogService documentLogService;

    private BeanFactory beanFactory;

    @Override
    public NodeRef createDraft(String documentTypeId) {
        QName type = DocumentDynamicModel.Types.DOCUMENT_DYNAMIC;
        DocumentType documentType = documentAdminService.getDocumentType(documentTypeId);

        Map<QName, Serializable> props = new HashMap<QName, Serializable>();
        props.put(DocumentDynamicModel.Props.DOCUMENT_TYPE_ID, documentType.getDocumentTypeId());
        DocumentTypeVersion docVer = documentType.getLatestDocumentTypeVersion();
        props.put(DocumentDynamicModel.Props.DOCUMENT_TYPE_VERSION_NR, docVer.getVersionNr());

        LinkedHashSet<QName> aspects = generalService.getDefaultAspects(type);

        for (Field field : docVer.getFieldsDeeply()) {
            if (!field.getFieldId().getNamespaceURI().equals(DocumentDynamicModel.URI)) {
                PropertyDefinition propDef = dictionaryService.getProperty(field.getFieldId());
                aspects.add(propDef.getContainerClass().getName());
                RepoUtil.getMandatoryAspects(propDef.getContainerClass(), aspects);
            }
        }

        // TODO temporary
        for (QName docAspect : aspects) {
            documentService.callbackAspectProperiesModifier(docAspect, props);
        }

        NodeRef drafts = documentService.getDrafts();
        NodeRef docRef = nodeService.createNode(drafts, DocumentCommonModel.Assocs.DOCUMENT, DocumentCommonModel.Assocs.DOCUMENT, type,
                props).getChildRef();

        for (QName aspect : aspects) {
            if (!nodeService.hasAspect(docRef, aspect)) {
                LOG.info("Adding aspect: " + aspect.toPrefixString(namespaceService));
                nodeService.addAspect(docRef, aspect, null);
            }
        }
        return docRef;
    }

    @Override
    public NodeRef copyDocument(DocumentDynamic document, Map<QName, Serializable> overriddenProperties, QName... ignoredProperty) {
        NodeRef draftRef = createDraft(document.getDocumentTypeId());
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
        Assert.isTrue(DocumentDynamicModel.Types.DOCUMENT_DYNAMIC.equals(type));
        Set<QName> aspects = RepoUtil.getAspectsIgnoringSystem(nodeService.getAspects(docRef));
        Map<QName, Serializable> props = RepoUtil.getPropertiesIgnoringSystem(nodeService.getProperties(docRef), dictionaryService);
        WmNode docNode = new WmNode(docRef, type, aspects, props);
        DocumentDynamic doc = new DocumentDynamic(docNode);
        LOG.info("getDocument document=" + doc);
        return doc;
    }

    private static class ValidationHelperImpl implements SaveListener.ValidationHelper {

        private final List<MessageData> errorMessages = new ArrayList<MessageData>();

        @Override
        public void addErrorMessage(String msgKey, Object... messageValuesForHolders) {
            errorMessages.add(new MessageDataImpl(msgKey, messageValuesForHolders));
        }

    }

    @Override
    public void updateDocument(DocumentDynamic documentOriginal, List<String> saveListenerBeanNames) {
        DocumentDynamic document = documentOriginal.clone();
        NodeRef docRef = document.getNodeRef();

        document.setDraft(isDraft(docRef));

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
        generalService.setPropertiesIgnoringSystem(docRef, docProps);
        generalService.saveAddedAssocs(document.getNode());
        { // update properties and log changes made in properties
            DocumentServiceImpl.PropertyChangesMonitorHelper propertyChangesMonitorHelper = new DocumentServiceImpl.PropertyChangesMonitorHelper();// FIXME:
            boolean propsChanged = propertyChangesMonitorHelper.setPropertiesIgnoringSystemAndReturnIfChanged(docRef, docProps //
                    , FUNCTION, SERIES, VOLUME, CASE // location changes
                    , REG_NUMBER, SHORT_REG_NUMBER, REG_DATE_TIME // registration changes
                    , ACCESS_RESTRICTION // access restriction changed
                    );
            if (!EventsLoggingHelper.isLoggingDisabled(document.getNode(), DocumentService.TransientProps.TEMP_LOGGING_DISABLED_DOCUMENT_METADATA_CHANGED)) {
                if (document.isDraft()) {
                    documentLogService.addDocumentLog(docRef, MessageUtil.getMessage("document_log_status_created"));
                } else if (propsChanged) {
                    documentLogService.addDocumentLog(docRef, MessageUtil.getMessage("document_log_status_changed"));
                }
            }
        }

        if (document.isDraft()) {
            documentService.addPrivilegesBasedOnSeriesOnBackground(docRef);
        }
    }

    @Override
    public void deleteDocumentIfDraft(NodeRef docRef) {
        LOG.info("deleteDocumentIfDraft document=" + docRef);
        if (!nodeService.exists(docRef)) {
            LOG.debug("Document does not exist, not deleting: " + docRef);
            return;
        }
        if (!isDraft(docRef)) {
            LOG.warn("Document is not a draft, not deleting: " + docRef);
            return;
        }
        nodeService.deleteNode(docRef);
    }

    @Override
    public boolean isDraft(NodeRef docRef) {
        NodeRef parentRef = nodeService.getPrimaryParent(docRef).getParentRef();
        return DocumentCommonModel.Types.DRAFTS.equals(nodeService.getType(parentRef));
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
    // END: setters

}
