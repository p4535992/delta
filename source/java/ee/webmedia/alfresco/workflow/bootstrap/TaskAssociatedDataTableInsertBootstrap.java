<<<<<<< HEAD
package ee.webmedia.alfresco.workflow.bootstrap;

import static ee.webmedia.alfresco.utils.FilenameUtil.getFilenameFromDisplayname;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.DueDateHistoryRecord;
import ee.webmedia.alfresco.workflow.service.WorkflowDbService;
import ee.webmedia.alfresco.workflow.service.WorkflowService;

/**
 * Insert tasks' due date extension data, due date extension history, files, multivalued properties etc
 * into corresponding tables.
 * 
 * @author Riina Tens
 */
public class TaskAssociatedDataTableInsertBootstrap extends AbstractNodeUpdater {

    private WorkflowDbService workflowDbService;
    private WorkflowService workflowService;
    private FileService fileService;

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.generateTypeQuery(WorkflowCommonModel.Types.TASK);
        List<ResultSet> result = new ArrayList<ResultSet>(6);
        for (StoreRef storeRef : generalService.getAllStoreRefsWithTrashCan()) {
            result.add(searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, query));
        }
        return result;
    }

    @Override
    protected String[] updateNode(final NodeRef nodeRef) throws Exception {
        List<String> results = new ArrayList<String>();
        String[] taskExistsError = checkTaskExists(nodeRef, log, nodeService, workflowService, workflowDbService);
        if (taskExistsError != null) {
            return taskExistsError;
        }
        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(nodeRef, WorkflowSpecificModel.Assocs.TASK_DUE_DATE_EXTENSION_HISTORY,
                WorkflowSpecificModel.Assocs.TASK_DUE_DATE_EXTENSION_HISTORY);
        if (childAssocs.isEmpty()) {
            results.add("no task due date history present");
        } else {
            List<DueDateHistoryRecord> historyRecords = new ArrayList<DueDateHistoryRecord>();
            for (ChildAssociationRef childRef : childAssocs) {
                NodeRef historyRef = childRef.getChildRef();
                DueDateHistoryRecord historyRecord = new DueDateHistoryRecord(
                        nodeRef.getId(),
                        (String) nodeService.getProperty(historyRef, WorkflowCommonModel.Props.CHANGE_REASON),
                        (Date) nodeService.getProperty(historyRef, WorkflowCommonModel.Props.PREVIOUS_DUE_DATE),
                        historyRef.getId(), null);
                historyRecords.add(historyRecord);
            }
            workflowDbService.createTaskDueDateHistoryEntries(nodeRef, historyRecords);
            results.add(historyRecords.toString());
        }
        if (WorkflowSpecificModel.Types.DUE_DATE_EXTENSION_TASK.equals(nodeService.getType(nodeRef))) {
            List<AssociationRef> assocs = nodeService.getSourceAssocs(nodeRef, WorkflowSpecificModel.Assocs.TASK_DUE_DATE_EXTENSION);
            if (assocs.isEmpty()) {
                results.add("orphan task, due date assoc data not present");
            } else {
                NodeRef initiatingTaskRef = assocs.get(0).getSourceRef();
                workflowDbService.createTaskDueDateExtensionAssocEntry(initiatingTaskRef, nodeRef);
                StringBuilder sb = new StringBuilder("Added assoc for nodeRef=" + initiatingTaskRef);
                if (assocs.size() > 1) {
                    int assocCounter = 0;
                    sb.append("; more than one assoc present; skipped nodeRefs:");
                    for (AssociationRef assocRef : assocs) {
                        if (assocCounter > 0) {
                            sb.append(assocRef.getSourceRef() + ", ");
                        }
                        assocCounter++;
                    }

                }
                results.add(sb.toString());
            }
        } else {
            results.add("not dueDateExtension task");
        }
        List<File> files = fileService.getAllFilesExcludingDigidocSubitems(nodeRef);
        NodeRef workflowRef = nodeService.getPrimaryParent(nodeRef).getParentRef();
        List<String> existingFilenames = fileService.getDocumentFileDisplayNames(workflowRef);
        for (File file : files) {
            String fileName = file.getName();
            String extension = FilenameUtils.getExtension(fileName);
            String displayName = fileName + (StringUtils.isNotBlank(extension) ? "." + extension : "");
            String uniqueFileName = getFilenameFromDisplayname(workflowRef, existingFilenames, displayName, generalService).getFirst();
            NodeRef fileRef = file.getNodeRef();
            if (!StringUtils.equals(fileName, uniqueFileName)) {
                nodeService.setProperty(fileRef, ContentModel.PROP_NAME, uniqueFileName);
            }
            nodeService.moveNode(fileRef, workflowRef, ContentModel.ASSOC_CONTAINS,
                    QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, QName.createValidLocalName(uniqueFileName)));
            existingFilenames.add(uniqueFileName);
        }
        if (files.isEmpty()) {
            results.add("no files present");
        } else {
            workflowDbService.createTaskFileEntries(nodeRef, files);
        }

        Map<QName, Serializable> newProps = new HashMap<QName, Serializable>();
        newProps.put(WorkflowCommonModel.Props.OWNER_ORGANIZATION_NAME, nodeService.getProperty(nodeRef, WorkflowCommonModel.Props.OWNER_ORGANIZATION_NAME));
        // also updates store_id value
        workflowDbService.updateTaskPropertiesAndStorRef(nodeRef, newProps);
        return results.toArray(new String[results.size()]);
    }

    public static String[] checkTaskExists(final NodeRef nodeRef, Log log, NodeService nodeService, WorkflowService workflowService, WorkflowDbService workflowDbService) {
        if (!workflowDbService.taskExists(nodeRef)) {
            WmNode taskNode = new WmNode(nodeRef, nodeService.getType(nodeRef));
            String taskStr = taskNode.toString();
            log.error("Found task from repo that doesn't exist in delta_task table, node:\n" + taskStr);
            return new String[] { "Task not present in delta_task table", taskStr };
        }
        return null;
    }

    public void setWorkflowDbService(WorkflowDbService workflowDbService) {
        this.workflowDbService = workflowDbService;
    }

    public void setWorkflowService(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    public void setFileService(FileService fileService) {
        this.fileService = fileService;
    }

}
=======
package ee.webmedia.alfresco.workflow.bootstrap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.apache.commons.logging.Log;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.WorkflowDbService;
import ee.webmedia.alfresco.workflow.service.WorkflowService;

/**
 * Insert tasks' due date extension data, due date extension history, files, multivalued properties etc
 * into corresponding tables.
 */
public class TaskAssociatedDataTableInsertBootstrap extends AbstractNodeUpdater {

    private WorkflowDbService workflowDbService;
    private WorkflowService workflowService;

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.generateTypeQuery(WorkflowCommonModel.Types.TASK);
        List<ResultSet> result = new ArrayList<ResultSet>(6);
        for (StoreRef storeRef : generalService.getAllStoreRefsWithTrashCan()) {
            result.add(searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, query));
        }
        return result;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        List<String> results = new ArrayList<String>();
        String[] taskExistsError = checkTaskExists(nodeRef, log, nodeService, workflowService, workflowDbService);
        if (taskExistsError != null) {
            return taskExistsError;
        }
        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(nodeRef, WorkflowSpecificModel.Assocs.TASK_DUE_DATE_EXTENSION_HISTORY,
                WorkflowSpecificModel.Assocs.TASK_DUE_DATE_EXTENSION_HISTORY);
        if (childAssocs.isEmpty()) {
            results.add("no task due date history present");
        } else {
            List<Pair<String, Date>> historyRecords = new ArrayList<Pair<String, Date>>();
            for (ChildAssociationRef childRef : childAssocs) {
                NodeRef historyRef = childRef.getChildRef();
                historyRecords.add(
                        new Pair<String, Date>(
                                (String) nodeService.getProperty(historyRef, WorkflowCommonModel.Props.CHANGE_REASON),
                                (Date) nodeService.getProperty(historyRef, WorkflowCommonModel.Props.PREVIOUS_DUE_DATE)));
            }
            List<Pair<NodeRef, Pair<String, Date>>> historyRecordsList = new ArrayList<Pair<NodeRef, Pair<String, Date>>>();
            for (Pair<String, Date> record : historyRecords) {
                historyRecordsList.add(new Pair(nodeRef, record));
            }
            workflowDbService.createTaskDueDateHistoryEntries(historyRecordsList);
            results.add(historyRecords.toString());
        }
        if (WorkflowSpecificModel.Types.DUE_DATE_EXTENSION_TASK.equals(nodeService.getType(nodeRef))) {
            List<AssociationRef> assocs = nodeService.getSourceAssocs(nodeRef, WorkflowSpecificModel.Assocs.TASK_DUE_DATE_EXTENSION);
            if (assocs.isEmpty()) {
                results.add("orphan task, due date assoc data not present");
            } else {
                NodeRef initiatingTaskRef = assocs.get(0).getSourceRef();
                List<Pair<NodeRef, NodeRef>> taskPair = new ArrayList<Pair<NodeRef, NodeRef>>();
                taskPair.add(new Pair(initiatingTaskRef, nodeRef));
                workflowDbService.createTaskDueDateExtensionAssocEntries(taskPair);
                StringBuilder sb = new StringBuilder("Added assoc for nodeRef=" + initiatingTaskRef);
                if (assocs.size() > 1) {
                    int assocCounter = 0;
                    sb.append("; more than one assoc present; skipped nodeRefs:");
                    for (AssociationRef assocRef : assocs) {
                        if (assocCounter > 0) {
                            sb.append(assocRef.getSourceRef() + ", ");
                        }
                        assocCounter++;
                    }

                }
                results.add(sb.toString());
            }
        } else {
            results.add("not dueDateExtension task");
        }
        List<File> files = BeanHelper.getFileService().getAllFilesExcludingDigidocSubitems(nodeRef);
        NodeRef workflowRef = nodeService.getPrimaryParent(nodeRef).getParentRef();
        for (File file : files) {
            nodeService.moveNode(file.getNodeRef(), workflowRef, ContentModel.ASSOC_CONTAINS,
                    QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, QName.createValidLocalName(file.getName())));
        }
        if (files.isEmpty()) {
            results.add("no files present");
        } else {
            workflowDbService.createTaskFileEntries(nodeRef, files);
        }
        Map<QName, Serializable> newProps = new HashMap<QName, Serializable>();
        newProps.put(WorkflowCommonModel.Props.OWNER_ORGANIZATION_NAME, nodeService.getProperty(nodeRef, WorkflowCommonModel.Props.OWNER_ORGANIZATION_NAME));
        // also updates store_id value
        workflowDbService.updateTaskPropertiesAndStorRef(nodeRef, newProps);
        return results.toArray(new String[results.size()]);
    }

    public static String[] checkTaskExists(final NodeRef nodeRef, Log log, NodeService nodeService, WorkflowService workflowService, WorkflowDbService workflowDbService) {
        if (!workflowDbService.taskExists(nodeRef)) {
            WmNode taskNode = new WmNode(nodeRef, nodeService.getType(nodeRef));
            String taskStr = taskNode.toString();
            log.error("Found task from repo that doesn't exist in delta_task table, node:\n" + taskStr);
            return new String[] { "Task not present in delta_task table", taskStr };
        }
        return null;
    }

    public void setWorkflowDbService(WorkflowDbService workflowDbService) {
        this.workflowDbService = workflowDbService;
    }

    public void setWorkflowService(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
