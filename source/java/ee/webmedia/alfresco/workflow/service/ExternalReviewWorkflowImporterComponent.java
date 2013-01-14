package ee.webmedia.alfresco.workflow.service;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.repo.importer.ImportNode;
import org.alfresco.repo.importer.Importer;
import org.alfresco.repo.importer.ImporterComponent;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.view.ImportPackageHandler;
import org.alfresco.service.cmr.view.ImporterBinding;
import org.alfresco.service.cmr.view.ImporterProgress;
import org.alfresco.service.cmr.view.Location;
import org.alfresco.service.namespace.QName;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xml.security.exceptions.Base64DecodingException;
import org.apache.xml.security.utils.Base64;

import ee.webmedia.alfresco.classificator.model.Classificator;
import ee.webmedia.alfresco.classificator.model.ClassificatorValue;
import ee.webmedia.alfresco.classificator.service.ClassificatorService;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.dvk.service.DvkService;
import ee.webmedia.alfresco.dvk.service.ReviewTaskException;
import ee.webmedia.alfresco.dvk.service.ReviewTaskException.ExceptionType;
import ee.webmedia.alfresco.notification.model.NotificationModel;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.UserUtil;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.xtee.client.dhl.types.ee.sk.digiDoc.v13.DataFileType;

public class ExternalReviewWorkflowImporterComponent extends ImporterComponent implements ExternalReviewWorkflowImporterService {
    // Logger
    private static final Log log = LogFactory.getLog(ExternalReviewWorkflowImporterComponent.class);

    private static final String TASK_OWNER_CLASSIFICATOR_NAME = "externalReviewAutomaticTaskOwners";

    private DocumentService documentService;
    private WorkflowService workflowService;
    private WorkflowDbService workflowDbService;
    private ClassificatorService classificatorService;
    private UserService userService;
    private MimetypeService mimetypeService;
    private DvkService dvkService;
    private FileService fileService;

    @Override
    public NodeRef importWorkflowDocument(Reader viewReader, Location location, NodeRef existingDocumentRef
            , List<DataFileType> dataFiles, String dvkId, Map<QName, Task> notifications) {
        NodeRef nodeRef = getNodeRef(location, null);

        List<Task> originalExternalReviewTasks = getOriginalExternalReviewTasks(existingDocumentRef);
        if (existingDocumentRef != null) {
            processExistingDocument(existingDocumentRef);
        }

        DocImporter nodeImporter = new DocNodeImporter(nodeRef, location.getChildAssocType(), null, new DefaultStreamHandler(), null, existingDocumentRef,
                dataFiles);
        try {
            viewParser.parse(viewReader, nodeImporter);
        } catch (RuntimeException e) {
            nodeImporter.error(e);
            throw new ReviewTaskException(ExceptionType.PARSING_EXCEPTION, e);
        }
        NodeRef dvkDocumentNodeRef = nodeImporter.getImportedRootNodeRef();
        if (dvkDocumentNodeRef == null) {
            // import failed
            return null;
        }
        addTasksMissingInfoAndNotifications(dvkDocumentNodeRef, dvkId, notifications, originalExternalReviewTasks);
        if (existingDocumentRef != null) {
            return existingDocumentRef;
        }
        // if new document was created, add missing location properties
        addDocumentMissingProperties(dvkDocumentNodeRef);
        return dvkDocumentNodeRef;
    }

    private List<Task> getOriginalExternalReviewTasks(NodeRef existingDocumentRef) {
        List<Task> externalReviewTasks = new ArrayList<Task>();
        if (existingDocumentRef != null) {
            for (CompoundWorkflow compoundWorkflow : workflowService.getCompoundWorkflows(existingDocumentRef)) {
                for (Workflow workflow : compoundWorkflow.getWorkflows()) {
                    if (WorkflowSpecificModel.Types.EXTERNAL_REVIEW_WORKFLOW.equals(workflow.getType())) {
                        externalReviewTasks.addAll(workflow.getTasks());
                    }
                }
            }
        }
        return externalReviewTasks;
    }

    private void processExistingDocument(NodeRef existingDocumentRef) {
        // remove workflows created not by current institution
        List<CompoundWorkflow> compoundWorkflows = workflowService.getCompoundWorkflows(existingDocumentRef);
        List<NodeRef> compoundWorkflowsToRemove = new ArrayList<NodeRef>(compoundWorkflows.size());
        String currentInstitutionCode = dvkService.getInstitutionCode();
        for (CompoundWorkflow compoundWorkflow : compoundWorkflows) {
            for (Workflow workflow : compoundWorkflow.getWorkflows()) {
                if (workflow.isType(WorkflowSpecificModel.Types.EXTERNAL_REVIEW_WORKFLOW)) {
                    for (Task task : workflow.getTasks()) {
                        // compound workflow is created by other institution, delete it
                        if (task.getInstitutionCode().equalsIgnoreCase(currentInstitutionCode)) {
                            compoundWorkflowsToRemove.add(compoundWorkflow.getNodeRef());
                        }
                    }
                }
            }
        }
        for (NodeRef compoundWorkflowRef : compoundWorkflowsToRemove) {
            if (nodeService.exists(compoundWorkflowRef)) {
                nodeService.deleteNode(compoundWorkflowRef);
            }
        }
        // remove all files
        List<File> files = fileService.getAllFilesExcludingDigidocSubitems(existingDocumentRef);
        for (File file : files) {
            if (nodeService.exists(file.getNodeRef())) {
                nodeService.deleteNode(file.getNodeRef());
            }
        }
    }

    private void addTasksMissingInfoAndNotifications(NodeRef dvkDocumentNodeRef, String dvkId, Map<QName, Task> notifications, List<Task> originalExternalReviewTasks) {
        List<CompoundWorkflow> compoundWorkflows = workflowService.getCompoundWorkflows(dvkDocumentNodeRef);
        for (CompoundWorkflow compoundWorkflow : compoundWorkflows) {
            for (Workflow workflow : compoundWorkflow.getWorkflows()) {
                for (Task task : workflow.getTasks()) {
                    if (task.isType(WorkflowSpecificModel.Types.EXTERNAL_REVIEW_TASK)) {
                        Map<QName, Serializable> taskOriginalProps = nodeService.getProperties(task.getNodeRef());
                        Map<QName, Serializable> taskNewProps = new HashMap<QName, Serializable>();
                        if (!taskOriginalProps.containsKey(WorkflowSpecificModel.Props.SENT_DVK_ID)) {
                            taskNewProps.put(WorkflowSpecificModel.Props.SENT_DVK_ID, null);
                        }
                        if (!taskOriginalProps.containsKey(WorkflowSpecificModel.Props.RECIEVED_DVK_ID)) {
                            taskNewProps.put(WorkflowSpecificModel.Props.RECIEVED_DVK_ID, null);
                        }
                        if (!taskOriginalProps.containsKey(WorkflowSpecificModel.Props.SEND_DATE_TIME)) {
                            taskNewProps.put(WorkflowSpecificModel.Props.SEND_DATE_TIME, null);
                        }
                        if (!taskOriginalProps.containsKey(WorkflowSpecificModel.Props.SEND_STATUS)) {
                            taskNewProps.put(WorkflowSpecificModel.Props.SENT_DVK_ID, null);
                        }
                        if (dvkService.getInstitutionCode().equalsIgnoreCase(task.getInstitutionCode())) {
                            taskNewProps.put(WorkflowSpecificModel.Props.RECIEVED_DVK_ID, dvkId);
                        }
                        if (task.isStatus(Status.IN_PROGRESS) && taskOriginalProps.get(WorkflowSpecificModel.Props.ORIGINAL_DVK_ID) == null) {
                            taskNewProps.put(WorkflowSpecificModel.Props.ORIGINAL_DVK_ID, dvkId);
                        }
                        if (task.isStatus(Status.IN_PROGRESS, Status.STOPPED)) {
                            boolean ownerFound = setTaskOwner(task, taskNewProps);
                            if (!ownerFound) {
                                log.error("Failde to find owner for external review task: " + task);
                                notifications.put(NotificationModel.NotificationType.EXTERNAL_REVIEW_WORKFLOW_OWNER_ERROR, task);
                            } else {
                                if (isNewTask(dvkId, task, originalExternalReviewTasks)) {
                                    notifications.put(NotificationModel.NotificationType.TASK_NEW_TASK_NOTIFICATION, task);
                                }
                            }
                        }
                        if (isCancelledTask(dvkId, task, originalExternalReviewTasks)) {
                            notifications.put(NotificationModel.NotificationType.TASK_CANCELLED_TASK_NOTIFICATION, task);
                        }
                        if (taskNewProps.size() > 0) {
                            nodeService.addProperties(task.getNode().getNodeRef(), taskNewProps);
                            task.getNode().getProperties().putAll(RepoUtil.toStringProperties(taskNewProps));
                        }
                    }
                    workflowDbService.createTaskEntry(task);
                }
            }
        }
    }

    private boolean isCancelledTask(String dvkId, Task task, List<Task> originalExternalReviewTasks) {
        if (dvkId.equalsIgnoreCase((String) task.getProp(WorkflowSpecificModel.Props.ORIGINAL_DVK_ID))) {
            return false;
        }
        Task originalTask = getOriginalTask(task, originalExternalReviewTasks);
        if (originalTask == null) {
            return false;
        }
        if (task.isStatus(Status.STOPPED) && !originalTask.getStatus().equals(task.getStatus())) {
            return true;
        }
        return false;
    }

    public boolean isNewTask(String dvkId, Task task, List<Task> originalExternalReviewTasks) {
        if (dvkId.equalsIgnoreCase((String) task.getProp(WorkflowSpecificModel.Props.ORIGINAL_DVK_ID))) {
            return true;
        }
        Task originalTask = getOriginalTask(task, originalExternalReviewTasks);
        if (originalTask == null
                || (task.isStatus(Status.IN_PROGRESS) && !originalTask.getStatus().equals(task.getStatus()))) {
            return true;
        }
        return false;
    }

    private Task getOriginalTask(Task task, List<Task> originalExternalReviewTasks) {
        for (Task originalTask : originalExternalReviewTasks) {
            if (task.getOriginalDvkId() != null && task.getOriginalDvkId().equals(originalTask.getOriginalDvkId())) {
                return originalTask;
            }
        }
        return null;
    }

    private boolean setTaskOwner(Task task, Map<QName, Serializable> taskNewProps) {
        String ownerInstitutionCode = task.getProp(WorkflowSpecificModel.Props.INSTITUTION_CODE);
        if (StringUtils.isNotEmpty(ownerInstitutionCode)) {
            Classificator ownerClassificator = classificatorService.getClassificatorByName(TASK_OWNER_CLASSIFICATOR_NAME);
            if (ownerClassificator != null) {
                List<ClassificatorValue> ownerValues = classificatorService.getActiveClassificatorValues(ownerClassificator);
                for (ClassificatorValue ownerValue : ownerValues) {
                    if (ownerInstitutionCode.equalsIgnoreCase(ownerValue.getClassificatorDescription())) {
                        String userCode = ownerValue.getValueName();
                        Map<QName, Serializable> userProps = userService.getUserProperties(userCode);
                        if (userProps != null) {
                            String userFullName = UserUtil.getPersonFullName1(userProps);
                            task.setOwnerId(userCode);
                            task.setOwnerName(userFullName);
                            taskNewProps.put(WorkflowCommonModel.Props.OWNER_ID, userCode);
                            taskNewProps.put(WorkflowCommonModel.Props.OWNER_NAME, userFullName);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private void addDocumentMissingProperties(NodeRef newDocumentRef) {
        Map<QName, Serializable> notEditableProps = new HashMap<QName, Serializable>();
        notEditableProps.put(DocumentCommonModel.Props.NOT_EDITABLE, Boolean.TRUE);
        nodeService.addAspect(newDocumentRef, DocumentCommonModel.Aspects.NOT_EDITABLE, notEditableProps);
        Map<QName, Serializable> propsToAdd = new HashMap<QName, Serializable>();
        propsToAdd.putAll(documentService.getDocumentParents(newDocumentRef));
        nodeService.addProperties(newDocumentRef, propsToAdd);
    }

    private class DocNodeImporter extends NodeImporter implements DocImporter {

        private NodeRef existingDocumentRef = null;
        private List<DataFileType> dataFiles = null;
        private NodeRef importedRootNodeRef = null;

        private DocNodeImporter(NodeRef rootRef, QName rootAssocType, ImporterBinding binding, ImportPackageHandler streamHandler, ImporterProgress progress,
                                NodeRef existingDocumentRef, List<DataFileType> dataFiles) {
            super(rootRef, rootAssocType, binding, streamHandler, progress);
            this.existingDocumentRef = existingDocumentRef;
            importedRootNodeRef = existingDocumentRef;
            this.dataFiles = dataFiles;
        }

        @SuppressWarnings("unchecked")
        @Override
        public List<NodeRef> getImportedNodeRefs() {
            return (List<NodeRef>) CollectionUtils.collect(nodeRefs, new Transformer() {
                @Override
                public Object transform(Object importedNodeRef) {
                    return ((ImportedNodeRef) importedNodeRef).getContext().getNodeRef();
                }
            });
        }

        @Override
        protected NodeImporterStrategy createNodeImporterStrategy(ImporterBinding.UUID_BINDING uuidBinding) {
            if (uuidBinding == null) {
                return new UpdateExternalReviewWorkflowDocImporterStrategy();
            } else {
                return super.createNodeImporterStrategy(uuidBinding);
            }
        }

        @Override
        protected void findAndImportContent(ImportNode context, NodeRef nodeRef) {
            String filename = null;
            if (org.alfresco.model.ContentModel.TYPE_CONTENT.equals(nodeService.getType(nodeRef))) {
                filename = (String) context.getProperties().get(org.alfresco.model.ContentModel.PROP_NAME);
            }
            for (Map.Entry<QName, Serializable> property : context.getProperties().entrySet()) {
                // filter out content properties (they're imported later)
                DataTypeDefinition valueDataType = context.getPropertyDataType(property.getKey());
                if (valueDataType != null && valueDataType.getName().equals(DataTypeDefinition.CONTENT)) {
                    // the property may be a single value or a collection - handle both
                    Object objVal = property.getValue();
                    if (objVal instanceof String) {
                        importContent(nodeRef, property.getKey(), (String) objVal, filename);
                    } else if (objVal instanceof Collection) {
                        @SuppressWarnings("unchecked")
                        Collection<String> importContents = (Collection<String>) objVal;
                        for (String value : importContents) {
                            importContent(nodeRef, property.getKey(), value, filename);
                        }
                    }
                }
            }
        }

        private void importContent(NodeRef nodeRef, QName propertyName, String importContentData, String fileName) {
            importContentData = bindPlaceHolder(importContentData, binding);
            if (StringUtils.isNotBlank(fileName)) {
                DataFileType dataFile = findDataFile(fileName);
                if (dataFile == null) {
                    // couldn't find corresponding content
                    return;
                }
                ContentWriter writer = contentService.getWriter(nodeRef, propertyName, true);
                String originalMimeType = StringUtils.lowerCase(dataFile.getMimeType());
                String mimeType = mimetypeService.guessMimetype(fileName);
                if (log.isInfoEnabled() && !StringUtils.equals(mimeType, originalMimeType)) {
                    log.info("Original mimetype '" + originalMimeType + "', but we are guessing mimetype based on filename '" + fileName + "' => '" + mimeType);
                }
                writer.setMimetype(mimeType);
                try {
                    final OutputStream os = writer.getContentOutputStream();
                    os.write(Base64.decode(dataFile.getStringValue()));
                    os.close();
                } catch (Base64DecodingException e) {
                    throw new RuntimeException("Failed to decode", e);
                } catch (IOException e) {
                    throw new RuntimeException("Failed write output to nodeRef=" + nodeRef + " contentUrl="
                            + writer.getContentUrl(), e);
                }
                reportContentCreated(nodeRef, fileName);
            }
        }

        private DataFileType findDataFile(String contentUrl) {
            if (dataFiles != null) {
                for (DataFileType dataFile : dataFiles) {
                    if (StringUtils.equals(dataFile.getFilename(), contentUrl)) {
                        return dataFile;
                    }
                }
            }
            return null;
        }

        @Override
        public NodeRef getImportedRootNodeRef() {
            return importedRootNodeRef;
        }

        private class UpdateExternalReviewWorkflowDocImporterStrategy extends UpdateExistingNodeImporterStrategy {

            UpdateExternalReviewWorkflowDocImporterStrategy() {
                createNewStrategy = new CreateNewExternalReviewWorkflowDocImporterStrategy(true);
            }

            @Override
            public NodeRef importNode(ImportNode node) {
                NodeRef existingNodeRef = super.importNode(node);
                if (importedRootNodeRef == null) {
                    importedRootNodeRef = existingNodeRef;
                }
                // use existingDocumentRef only for importig root node
                existingDocumentRef = null;
                return existingNodeRef;
            }

            @Override
            protected NodeRef getExistingNodeRef(ImportNode node) {
                return existingDocumentRef;
            }

        }

        private class CreateNewExternalReviewWorkflowDocImporterStrategy extends CreateNewNodeImporterStrategy {

            public CreateNewExternalReviewWorkflowDocImporterStrategy(boolean assignNewUUID) {
                super(assignNewUUID);
            }

            @Override
            public NodeRef importNode(ImportNode node) {
                NodeRef nodeRef = super.importNode(node);
                if (importedRootNodeRef == null) {
                    importedRootNodeRef = nodeRef;
                }
                return nodeRef;
            }
        }

    }

    public void setDocumentService(DocumentService documentService) {
        this.documentService = documentService;
    }

    public void setWorkflowService(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    public void setWorkflowDbService(WorkflowDbService workflowDbService) {
        this.workflowDbService = workflowDbService;
    }

    public void setClassificatorService(ClassificatorService classificatorService) {
        this.classificatorService = classificatorService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void setMimetypeService(MimetypeService mimetypeService) {
        this.mimetypeService = mimetypeService;
    }

    public void setDvkService(DvkService dvkService) {
        this.dvkService = dvkService;
    }

    public void setFileService(FileService fileService) {
        this.fileService = fileService;
    }

    public interface DocImporter extends Importer {

        List<NodeRef> getImportedNodeRefs();

        NodeRef getImportedRootNodeRef();

    }

}
