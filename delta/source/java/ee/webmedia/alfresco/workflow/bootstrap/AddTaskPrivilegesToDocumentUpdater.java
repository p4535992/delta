package ee.webmedia.alfresco.workflow.bootstrap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamic;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamicService;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.notification.service.event.WorkflowStatusEventListener;
import ee.webmedia.alfresco.privilege.service.PrivilegeService;
import ee.webmedia.alfresco.privilege.service.PrivilegeUtil;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.WorkflowService;

/**
 * Add same privileges to all tasks (except with {@link Status#NEW}) that would be given normally
 * (in {@link WorkflowStatusEventListener}), when task changes status to {@link Status#IN_PROGRESS}.
 * 
 * @author Alar Kvell
 */
public class AddTaskPrivilegesToDocumentUpdater extends AbstractNodeUpdater {

    private PrivilegeService privilegeService;
    private FileService fileService;
    private WorkflowService workflowService;
    private DocumentDynamicService documentDynamicService;

    private String validUsers = "";
    private Set<String> ownerIds;

    @Override
    protected boolean usePreviousInputState() {
        return false;
    }

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        ownerIds = new HashSet<String>();
        String[] validUsersArray = validUsers.split("\\s");
        for (String string : validUsersArray) {
            string = StringUtils.strip(string);
            if (StringUtils.isNotEmpty(string)) {
                ownerIds.add(string);
            }
        }
        log.info("Valid users input " + WmNode.toString(ownerIds));
        if (ownerIds.isEmpty()) {
            throw new RuntimeException("Valid users input is empty");
        }
        String query = SearchUtil.joinQueryPartsAnd(
                SearchUtil.generateTypeQuery(WorkflowCommonModel.Types.TASK),
                SearchUtil.generateMultiStringExactQuery(
                        Arrays.asList(Status.FINISHED.getName(), Status.IN_PROGRESS.getName(), Status.STOPPED.getName(), Status.UNFINISHED.getName()),
                        WorkflowCommonModel.Props.STATUS),
                SearchUtil.generateMultiStringExactQuery(new ArrayList<String>(ownerIds), WorkflowCommonModel.Props.OWNER_ID));
        log.info("Query: " + query);
        List<ResultSet> result = new ArrayList<ResultSet>(6);
        for (StoreRef storeRef : generalService.getAllWithArchivalsStoreRefs()) {
            result.add(searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, query));
        }
        return result;
    }

    @Override
    protected String[] updateNode(NodeRef taskRef) throws Exception {
        QName taskType = nodeService.getType(taskRef);
        if (!serviceRegistry.getDictionaryService().isSubClass(taskType, WorkflowCommonModel.Types.TASK)) {
            return new String[] {
                    "isNotTaskType",
                    taskType.toPrefixString(serviceRegistry.getNamespaceService()) };
        }
        Task task = workflowService.getTask(taskRef, false);
        if (!ownerIds.contains(task.getOwnerId())) {
            return new String[] {
                    "taskOwnerIdIsNotValid",
                    taskType.toPrefixString(serviceRegistry.getNamespaceService()),
                    task.getOwnerId() };
        }

        if (Status.NEW.getName().equals(task.getStatus())) {
            return new String[] {
                    "taskStatusIsNew",
                    taskType.toPrefixString(serviceRegistry.getNamespaceService()),
                    task.getOwnerId(),
                    task.getStatus() };
        }

        NodeRef workflowRef = nodeService.getPrimaryParent(taskRef).getParentRef();
        NodeRef compoundWorkflowRef = nodeService.getPrimaryParent(workflowRef).getParentRef();
        NodeRef docRef = nodeService.getPrimaryParent(compoundWorkflowRef).getParentRef();
        QName docType = nodeService.getType(docRef);
        if (!DocumentCommonModel.Types.DOCUMENT.equals(docType)) {
            return new String[] {
                    "taskIsNotUnderDocument",
                    taskType.toPrefixString(serviceRegistry.getNamespaceService()),
                    task.getOwnerId(),
                    task.getStatus(),
                    docRef.toString(),
                    docType.toPrefixString(serviceRegistry.getNamespaceService()) };
        }
        Set<String> privilegesToTaskOwnerId = PrivilegeUtil.getRequiredPrivsForInprogressTask(task, docRef, fileService);
        privilegeService.setPermissions(docRef, task.getOwnerId(), privilegesToTaskOwnerId);
        DocumentDynamic doc = documentDynamicService.getDocument(docRef);
        return new String[] {
                "addedDocumentPrivilegesFromTaskOwnerId",
                taskType.toPrefixString(serviceRegistry.getNamespaceService()),
                task.getOwnerId(),
                task.getStatus(),
                docRef.toString(),
                docType.toPrefixString(serviceRegistry.getNamespaceService()),
                privilegesToTaskOwnerId.toString(),
                doc.getRegDateTime() == null ? "" : dateFormat.format(doc.getRegDateTime()),
                doc.getRegNumber(),
                doc.getDocName() };
    }

    @Override
    protected String[] getCsvFileHeaders() {
        return new String[] {
                "taskRef",
                "action",
                "taskType",
                "taskOwnerId",
                "taskStatus",
                "docRef",
                "docNodeType",
                "privilegesToTaskOwnerId",
                "docRegDateTime",
                "docRegNumber",
                "docName" };
    }

    public void setValidUsers(String validUsers) {
        this.validUsers = validUsers;
    }

    public String getValidUsers() {
        return validUsers;
    }

    public void setPrivilegeService(PrivilegeService privilegeService) {
        this.privilegeService = privilegeService;
    }

    public void setFileService(FileService fileService) {
        this.fileService = fileService;
    }

    public void setWorkflowService(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    public void setDocumentDynamicService(DocumentDynamicService documentDynamicService) {
        this.documentDynamicService = documentDynamicService;
    }

}
