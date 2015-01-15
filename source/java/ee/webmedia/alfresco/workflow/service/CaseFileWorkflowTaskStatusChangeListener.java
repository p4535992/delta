package ee.webmedia.alfresco.workflow.service;

import static ee.webmedia.alfresco.privilege.service.PrivilegeUtil.getPrivsWithDependencies;
import static ee.webmedia.alfresco.privilege.service.PrivilegeUtil.getRequiredPrivsForInprogressTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.Pair;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.InitializingBean;

import ee.webmedia.alfresco.casefile.model.CaseFileModel;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.privilege.model.Privilege;
import ee.webmedia.alfresco.privilege.service.PrivilegeService;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEvent;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventListener;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventQueue;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventType;

public class CaseFileWorkflowTaskStatusChangeListener implements WorkflowEventListener, InitializingBean {

    private WorkflowService workflowService;
    private PrivilegeService privilegeService;
    private GeneralService generalService;
    private DocumentSearchService documentSearchService;

    @Override
    public void afterPropertiesSet() throws Exception {
        workflowService.registerEventListener(this);
    }

    @Override
    public void handle(WorkflowEvent event, WorkflowEventQueue queue) {
        final BaseWorkflowObject object = event.getObject();

        if (!(object instanceof Task)) {
            return;
        }

        Task task = (Task) object;
        String ownerId = task.getOwnerId();
        if (StringUtils.isBlank(ownerId)) {
            return;
        }

        CompoundWorkflow compoundWorkflow = task.getParent().getParent();
        if (!compoundWorkflow.isCaseFileWorkflow() || !event.getType().equals(WorkflowEventType.STATUS_CHANGED) || !task.isStatus(Status.IN_PROGRESS)
                || task.isType(WorkflowSpecificModel.Types.SIGNATURE_TASK)) {
            return;
        }

        NodeRef caseFileRef = generalService.getAncestorNodeRefWithType(compoundWorkflow.getNodeRef(), CaseFileModel.Types.CASE_FILE);
        if (caseFileRef == null) {
            return;
        }

        // Add task owner privileges to case file
        Map<NodeRef, Pair<Boolean, Boolean>> digiDocStatuses = new HashMap<>();
        privilegeService.setPermissions(caseFileRef, ownerId, getPrivsWithDependencies(getRequiredPrivsForInprogressTask(task, null, null, true, digiDocStatuses)));
        // and to documents under this case file
        Set<Privilege> privsWithDependencies = getPrivsWithDependencies(getRequiredPrivsForInprogressTask(task, null, null, false, digiDocStatuses));
        for (NodeRef docRef : documentSearchService.searchAllDocumentRefsByParentRef(caseFileRef)) {
            privilegeService.setPermissions(docRef, ownerId, privsWithDependencies);
        }
    }

    // START: getters/setters

    public void setWorkflowService(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    public void setPrivilegeService(PrivilegeService privilegeService) {
        this.privilegeService = privilegeService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setDocumentSearchService(DocumentSearchService documentSearchService) {
        this.documentSearchService = documentSearchService;
    }

    // END: getters/setters

}
