package ee.webmedia.alfresco.workflow.bootstrap;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.service.WorkflowDbService;
import ee.webmedia.alfresco.workflow.service.WorkflowService;

/**
 * Insert tasks' data into delta_tasks table.
 * Tasks are retireved by compound workflow in order to retrieve task index in workflow.
 * This class is deprecated because updateNode method contains functionality that doesn't exist any more, but it is still preserved to
 * avoid rewriting bean dependencies.
 * 
 * @author Riina Tens
 */
public class TaskTableInsertBootstrap extends AbstractNodeUpdater {

    private WorkflowService workflowService;
    private WorkflowDbService workflowDbService;
    private FileService fileService;

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.generateTypeQuery(WorkflowCommonModel.Types.COMPOUND_WORKFLOW);
        List<ResultSet> result = new ArrayList<ResultSet>(6);
        for (StoreRef storeRef : generalService.getAllWithArchivalsStoreRefs()) {
            result.add(searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, query));
        }
        return result;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        // This updater is needed when migrating from 2.5 to 3.* branch,
        // but it must be rewritten before it can be used.
        throw new RuntimeException("This functionality is not supported any more, use nodeService for retrieving task data from repo");
        // List<String> taskNodeRefs = new ArrayList<String>();
        // List<Workflow> workflows;
        // if (WorkflowCommonModel.Types.COMPOUND_WORKFLOW.equals(nodeService.getType(nodeRef))) {
        // // workflows = workflowService.getCompoundWorkflowFromRepo(nodeRef).getWorkflows();
        // } else {
        // // workflows = workflowService.getCompoundWorkflowDefinitionFromRepo(nodeRef).getWorkflows();
        // }
        // for (Workflow workflow : workflows) {
        // for (Task task : workflow.getTasks()) {
        // List<File> files = fileService.getAllFilesExcludingDigidocSubitems(task.getNodeRef());
        // task.setHasFiles(files != null && !files.isEmpty());
        // workflowDbService.createTaskEntry(task);
        // taskNodeRefs.add(task.getNodeRef().toString());
        // }
        // }
        // return new String[] { "Inserted tasks: ", TextUtil.joinNonBlankStringsWithComma(taskNodeRefs) };
    }

    public void setWorkflowService(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    public void setWorkflowDbService(WorkflowDbService workflowDbService) {
        this.workflowDbService = workflowDbService;
    }

    public void setFileService(FileService fileService) {
        this.fileService = fileService;
    }

}
