package ee.webmedia.alfresco.casefile.service;

import static ee.webmedia.alfresco.common.web.BeanHelper.getEventPlanService;
import static ee.webmedia.alfresco.docadmin.web.DocAdminUtil.getDocTypeIdAndVersionNr;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.EqualsHelper;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.casefile.log.service.CaseFileLogService;
import ee.webmedia.alfresco.casefile.model.CaseFileModel;
import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.service.CaseFileType;
import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;
import ee.webmedia.alfresco.docadmin.service.DocumentTypeVersion;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docconfig.generator.SaveListener;
import ee.webmedia.alfresco.docconfig.service.DocumentConfigService;
import ee.webmedia.alfresco.docconfig.service.DynamicPropertyDefinition;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamicService;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamicServiceImpl.ValidationHelperImpl;
import ee.webmedia.alfresco.document.lock.service.DocLockService;
import ee.webmedia.alfresco.document.log.service.DocumentPropertiesChangeHolder;
import ee.webmedia.alfresco.document.log.service.PropertyChange;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.register.model.RegNrHolder2;
import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.document.service.DocumentServiceImpl;
import ee.webmedia.alfresco.log.model.LogEntry;
import ee.webmedia.alfresco.log.model.LogObject;
import ee.webmedia.alfresco.log.service.LogService;
import ee.webmedia.alfresco.privilege.model.Privilege;
import ee.webmedia.alfresco.privilege.service.PrivilegeService;
import ee.webmedia.alfresco.register.model.Register;
import ee.webmedia.alfresco.register.service.RegisterService;
import ee.webmedia.alfresco.series.model.Series;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.series.service.SeriesService;
import ee.webmedia.alfresco.utils.DynamicTypeUtil;
import ee.webmedia.alfresco.utils.MessageDataWrapper;
import ee.webmedia.alfresco.utils.TreeNode;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.utils.UnableToPerformMultiReasonException;
import ee.webmedia.alfresco.volume.model.VolumeModel;
import ee.webmedia.alfresco.volume.service.VolumeService;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;
import ee.webmedia.alfresco.workflow.service.WorkflowService;

public class CaseFileServiceImpl implements CaseFileService, BeanFactoryAware {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(CaseFileServiceImpl.class);

    private NodeService nodeService;
    private DocumentAdminService documentAdminService;
    private DocumentConfigService documentConfigService;
    private DocumentService documentService;
    private DocumentSearchService documentSearchService;
    private DocumentDynamicService documentDynamicService;
    private WorkflowService workflowService;
    private GeneralService generalService;
    private BeanFactory beanFactory;
    private SeriesService seriesService;
    private RegisterService registerService;
    private PrivilegeService privilegeService;
    private CaseFileLogService caseFileLogService;
    private DocLockService docLockService;
    private LogService logService;
    private VolumeService volumeService;

    @Override
    public Pair<CaseFile, DocumentTypeVersion> createNewCaseFile(String typeId, NodeRef parent) {
        DocumentTypeVersion docVer = getLatestDocTypeVer(typeId);
        if (docVer == null) {
            throw new RuntimeException("Could not retrieve latest version of case file type [" + typeId + "]. Please review the case file type name/settings before continuing.");
        }
        return createNewCaseFile(docVer, parent, true);
    }

    @Override
    public Pair<CaseFile, DocumentTypeVersion> createNewCaseFile(DocumentTypeVersion docVer, NodeRef parent, boolean reallySetDefaultValues) {
        Map<QName, Serializable> props = new HashMap<QName, Serializable>();
        DynamicTypeUtil.setTypeProps(getDocTypeIdAndVersionNr(docVer), props);

        return createNewCaseFile(docVer, parent, props, reallySetDefaultValues);
    }

    @Override
    public Pair<CaseFile, DocumentTypeVersion> createNewCaseFileInDrafts(String typeId) {
        NodeRef drafts = BeanHelper.getConstantNodeRefsBean().getDraftsRoot();
        return createNewCaseFile(typeId, drafts);
    }

    @Override
    public CaseFile getCaseFile(NodeRef caseFileRef) {
        CaseFile caseFile = new CaseFile(generalService.fetchObjectNode(caseFileRef, CaseFileModel.Types.CASE_FILE));
        if (LOG.isDebugEnabled()) {
            LOG.debug("getCaseFile caseFile=" + caseFile);
        }
        return caseFile;
    }

    @Override
    public CaseFile update(CaseFile caseFile, List<String> saveListenerBeanNames) {
        CaseFile cf = caseFile.clone();
        Map<String, Pair<DynamicPropertyDefinition, Field>> propDefs = documentConfigService.getPropertyDefinitions(cf.getNode());
        if (saveListenerBeanNames != null) {
            saveListenerBeanNames = setListenerDependencies(saveListenerBeanNames);
            ValidationHelperImpl validationHelper = new ValidationHelperImpl(propDefs);
            for (String saveListenerBeanName : saveListenerBeanNames) {
                SaveListener saveListener = beanFactory.getBean(saveListenerBeanName, SaveListener.class);
                saveListener.validate(cf, validationHelper);
            }
            if (!validationHelper.getErrorMessages().isEmpty()) {
                throw new UnableToPerformMultiReasonException(new MessageDataWrapper(validationHelper.getErrorMessages()));
            }
            for (String saveListenerBeanName : saveListenerBeanNames) {
                SaveListener saveListener = beanFactory.getBean(saveListenerBeanName, SaveListener.class);
                saveListener.save(cf);
            }
        }

        WmNode caseNode = cf.getNode();
        // If case file is updated for the first time, add SEARCHABLE aspect to case file and register it
        if (!caseNode.hasAspect(DocumentCommonModel.Aspects.SEARCHABLE)) {
            caseNode.getAspects().add(DocumentCommonModel.Aspects.SEARCHABLE);
        }

        Map<String, Object> props = caseNode.getProperties();
        // If case file isn't saved yet and user left the field empty (NB! can be done only in the initial save!), then register the case file now
        if (StringUtils.isBlank((String) props.get(VolumeModel.Props.VOLUME_MARK))) {
            registerCaseFile(caseNode, null, false);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("update after validation and save listeners, before real saving: " + cf);
        }

        { // update properties and log changes made in properties
            DocumentServiceImpl.PropertyChangesMonitorHelper propertyChangesMonitorHelper = new DocumentServiceImpl.PropertyChangesMonitorHelper();
            DocumentPropertiesChangeHolder propChangeHolder = documentDynamicService.saveThisNodeAndChildNodes(null, caseNode, Collections.<TreeNode<QName>> emptyList(), null,
                    propertyChangesMonitorHelper, propDefs);
            if (cf.isDraft()) {
                caseFileLogService.addCaseFileLog(cf.getNodeRef(), "casefile_log_status_created");
                cf.setDraft(false);
                getEventPlanService().initVolumeOrCaseFileFromSeriesEventPlan(cf.getNodeRef());
            } else {
                for (Serializable msg : propChangeHolder.generateLogMessages(propDefs, cf.getNodeRef())) {
                    caseFileLogService.addCaseFileLogMessage(cf.getNodeRef(), (String) msg);
                }
            }
            for (PropertyChange propChange : propChangeHolder.getChanges(caseNode.getNodeRef())) {
                if (DocumentCommonModel.Props.OWNER_ID.equals(propChange.getProperty())) {
                    addOwnerDocumentPermissions(caseNode.getNodeRef(), (String) propChange.getNewValue());
                    break;
                }
            }
        }
        generalService.refreshMaterializedViews(CaseFileModel.Types.CASE_FILE);
        volumeService.removeFromCache(cf.getNodeRef());
        return cf;
    }

    private List<String> setListenerDependencies(List<String> saveListenerBeanNames) {
        ArrayList<String> listeners = new ArrayList<String>(saveListenerBeanNames.size());
        for (String listener : saveListenerBeanNames) {
            if ("regNumberGenerator".equals(listener)) {
                listeners.add(0, listener);
            } else {
                listeners.add(listener);
            }
        }
        return listeners;
    }

    private void addOwnerDocumentPermissions(NodeRef caseFileRef, String ownerId) {
        for (NodeRef docRef : documentSearchService.searchAllDocumentRefsByParentRef(caseFileRef)) {
            privilegeService.setPermissions(docRef, ownerId, Privilege.EDIT_DOCUMENT); // With dependencies
        }
    }

    @Override
    public void registerCaseFile(Node caseFileNode, Node previousSeries, boolean relocating) {
        // Only regenerate the regNr if we are relocating and user hasn't modified the volume mark field
        boolean relocatingWithUnmodifiedVolMark = relocating && caseFileNode.getProperties().get(VolumeModel.Props.VOL_SHORT_REG_NUMBER) != null;
        boolean initialSave = previousSeries == null && relocating == false;
        if (relocatingWithUnmodifiedVolMark || initialSave) {
            NodeRef newSeriesRef = (NodeRef) caseFileNode.getProperties().get(DocumentCommonModel.Props.SERIES);
            boolean sameRegister = false;
            if (newSeriesRef != null && previousSeries != null) {
                Integer newSeriesVolumeRegister = seriesService.getSeriesByNodeRef(newSeriesRef).getVolRegister();
                Integer existingSeriesVolumeRegister = seriesService.getSeriesByNodeRef(previousSeries.getNodeRef()).getVolRegister();
                sameRegister = EqualsHelper.nullSafeEquals(newSeriesVolumeRegister, existingSeriesVolumeRegister);
            }
            Map<String, Object> props = caseFileNode.getProperties();
            Series series = seriesService.getSeriesByNodeRef((NodeRef) props.get(DocumentCommonModel.Props.SERIES));
            if (series.getVolRegister() == null) {
                throw new UnableToPerformException("caseFile_volume_register_missing");
            }
            Register volRegister = registerService.getRegister(series.getVolRegister());

            RegNrHolder2 holder = new RegNrHolder2(null, null, null);
            documentService.setRegNrBasedOnPattern(series, caseFileNode.getNodeRef(), null, volRegister, holder, new Date(), series.getVolNumberPattern(), sameRegister);
            props.put(VolumeModel.Props.VOLUME_MARK.toString(), holder.getRegNumber());
            props.put(VolumeModel.Props.VOL_SHORT_REG_NUMBER.toString(), volRegister.getCounter());
        }

        // Relocate all the documents under the case file
        relocateCaseFileDocuments(caseFileNode);
    }

    @Override
    public Node getSeriesByCaseFile(NodeRef caseFileNodeRef) {
        return caseFileNodeRef == null ? null : generalService.getParentWithType(caseFileNodeRef, SeriesModel.Types.SERIES);
    }

    private void relocateCaseFileDocuments(final Node caseFileNode) {
        generalService.runOnBackground(new RunAsWork<Void>() {

            @Override
            public Void doWork() throws Exception {
                List<NodeRef> caseFileDocs = documentService.getAllDocumentRefsByParentRefWithoutRestrictedAccess(caseFileNode.getNodeRef());
                for (NodeRef docRef : caseFileDocs) {
                    Document doc = documentService.getDocumentByNodeRef(docRef);
                    if (doc.getRegDateTime() == null) {
                        continue;
                    }

                    documentService.registerDocumentRelocating(doc, caseFileNode);
                }
                return null;
            }

        }, "relocateCaseFileDocuments", true);

    }

    private Pair<CaseFile, DocumentTypeVersion> createNewCaseFile(DocumentTypeVersion docVer, NodeRef parent, Map<QName, Serializable> props, boolean reallySetDefaultValues) {
        QName type = CaseFileModel.Types.CASE_FILE;
        NodeRef docRef = nodeService.createNode(parent, CaseFileModel.Assocs.CASE_FILE, CaseFileModel.Assocs.CASE_FILE, type, props).getChildRef();

        CaseFile caseFile = getCaseFile(docRef);
        WmNode docNode = caseFile.getNode();

        TreeNode<QName> childAssocTypeQNamesRoot = documentConfigService.getChildAssocTypeQNameTree(docVer);
        Assert.isNull(childAssocTypeQNamesRoot.getData());

        documentDynamicService.createChildNodesHierarchy(docNode, childAssocTypeQNamesRoot.getChildren(), null);
        documentConfigService.setDefaultPropertyValues(docNode, null, false, reallySetDefaultValues, docVer);

        DocumentServiceImpl.PropertyChangesMonitorHelper propertyChangesMonitorHelper = new DocumentServiceImpl.PropertyChangesMonitorHelper();
        documentDynamicService.saveThisNodeAndChildNodes(null, docNode, childAssocTypeQNamesRoot.getChildren(), null, propertyChangesMonitorHelper, null);

        return Pair.newInstance(getCaseFile(docRef), docVer);
    }

    private DocumentTypeVersion getLatestDocTypeVer(String caseFileTypeId) {
        CaseFileType documentType = documentAdminService.getCaseFileType(caseFileTypeId, DocumentAdminService.DOC_TYPE_WITH_OUT_GRAND_CHILDREN_EXEPT_LATEST_DOCTYPE_VER);
        return documentType != null ? documentType.getLatestDocumentTypeVersion() : null;
    }

    @Override
    public List<DocumentToCompoundWorkflow> getCaseFileDocumentWorkflows(NodeRef caseFileRef) {
        List<NodeRef> docRefs = documentSearchService.searchAllDocumentRefsByParentRef(caseFileRef);
        final ArrayList<DocumentToCompoundWorkflow> documentToCompoundWorkflows = new ArrayList<DocumentToCompoundWorkflow>();
        for (NodeRef docRef : docRefs) {
            Document document = new Document(docRef);
            final List<AssociationRef> targetAssocs = nodeService.getTargetAssocs(docRef, DocumentCommonModel.Assocs.WORKFLOW_DOCUMENT);
            // independent workflows
            for (AssociationRef targetAssocRef : targetAssocs) {
                CompoundWorkflow compoundWorkflow = workflowService.getCompoundWorkflow(targetAssocRef.getTargetRef());
                documentToCompoundWorkflows.add(new DocumentToCompoundWorkflow(document, compoundWorkflow));
            }
            // document workflows
            List<CompoundWorkflow> compoundWorkflows = workflowService.getCompoundWorkflows(docRef);
            for (CompoundWorkflow compoundWorkflow : compoundWorkflows) {
                documentToCompoundWorkflows.add(new DocumentToCompoundWorkflow(document, compoundWorkflow));
            }
        }
        return documentToCompoundWorkflows;
    }

    @Override
    public void closeCaseFile(CaseFile caseFile) {
        NodeRef caseFileRef = caseFile.getNodeRef();
        Pair<Boolean, Date> closeResult = getEventPlanService().closeVolumeOrCaseFile(caseFileRef);
        if (!closeResult.getFirst()) {
            return;
        }
        if (closeResult.getSecond() != null) {
            nodeService.setProperty(caseFileRef, DocumentDynamicModel.Props.VALID_TO, closeResult.getSecond());
        }
        nodeService.setProperty(caseFileRef, DocumentDynamicModel.Props.STATUS, DocListUnitStatus.CLOSED.getValueName());
        caseFileLogService.addCaseFileLog(caseFileRef, "casefile_log_close");
        volumeService.removeFromCache(caseFileRef);
    }

    @Override
    public void openCaseFile(CaseFile caseFile) {
        NodeRef caseFileRef = caseFile.getNodeRef();
        if (DocListUnitStatus.CLOSED.getValueName().equals(
                nodeService.getProperty(generalService.getParentWithType(caseFileRef, SeriesModel.Types.SERIES).getNodeRef(), SeriesModel.Props.STATUS))) {
            throw new UnableToPerformException("casefile_cannot_open_series_closed");
        }
        nodeService.setProperty(caseFileRef, DocumentDynamicModel.Props.STATUS, DocListUnitStatus.OPEN.getValueName());
        caseFileLogService.addCaseFileLog(caseFileRef, "casefile_log_open");
    }

    @Override
    public void deleteCaseFile(NodeRef nodeRef, String reason) {
        // Check if documents or files are locked before deleting
        List<ChildAssociationRef> documents = nodeService.getChildAssocs(nodeRef, DocumentCommonModel.Assocs.DOCUMENT, DocumentCommonModel.Assocs.DOCUMENT);
        for (ChildAssociationRef assocRef : documents) {
            docLockService.checkForLock(assocRef.getChildRef());
        }

        LOG.debug("Deleting case file: " + nodeRef);

        // Delete workflows
        List<ChildAssociationRef> workflows = nodeService.getChildAssocs(nodeRef, WorkflowCommonModel.Assocs.COMPOUND_WORKFLOW, WorkflowCommonModel.Assocs.COMPOUND_WORKFLOW);
        for (ChildAssociationRef assocRef : workflows) {
            workflowService.deleteCompoundWorkflow(assocRef.getChildRef(), false);
        }

        // Delete documents
        for (ChildAssociationRef assocRef : documents) {
            documentService.deleteDocument(assocRef.getChildRef());
        }

        final Map<QName, Serializable> props = nodeService.getProperties(nodeRef);
        nodeService.deleteNode(nodeRef);
        volumeService.removeFromCache(nodeRef);
        logService.addLogEntry(LogEntry.create(LogObject.CASE_FILE, BeanHelper.getUserService(), nodeRef, "caseFile_log_deleted", props.get(VolumeModel.Props.VOLUME_MARK),
                props.get(VolumeModel.Props.TITLE), props.get(VolumeModel.Props.STATUS), reason));
    }

    // START: setters/getters
    public void setDocumentAdminService(DocumentAdminService documentAdminService) {
        this.documentAdminService = documentAdminService;
    }

    public void setDocumentConfigService(DocumentConfigService documentConfigService) {
        this.documentConfigService = documentConfigService;
    }

    public void setDocumentDynamicService(DocumentDynamicService documentDynamicService) {
        this.documentDynamicService = documentDynamicService;
    }

    public void setDocumentService(DocumentService documentService) {
        this.documentService = documentService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setWorkflowService(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    public void setSeriesService(SeriesService seriesService) {
        this.seriesService = seriesService;
    }

    public void setRegisterService(RegisterService registerService) {
        this.registerService = registerService;
    }

    public void setPrivilegeService(PrivilegeService privilegeService) {
        this.privilegeService = privilegeService;
    }

    public void setCaseFileLogService(CaseFileLogService caseFileLogService) {
        this.caseFileLogService = caseFileLogService;
    }

    public void setDocLockService(DocLockService docLockService) {
        this.docLockService = docLockService;
    }

    public void setLogService(LogService logService) {
        this.logService = logService;
    }

    public void setVolumeService(VolumeService volumeService) {
        this.volumeService = volumeService;
    }

    public void setDocumentSearchService(DocumentSearchService documentSearchService) {
        this.documentSearchService = documentSearchService;
    }

    // END: setters/getters
}
