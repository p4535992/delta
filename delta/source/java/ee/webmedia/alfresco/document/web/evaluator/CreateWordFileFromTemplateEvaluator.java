package ee.webmedia.alfresco.document.web.evaluator;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentTemplateService;

import java.util.HashSet;
import java.util.Set;

import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.util.EqualsHelper;
import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.utils.Predicate;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.WorkflowUtil;

public class CreateWordFileFromTemplateEvaluator extends BaseActionEvaluator {

    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Node node) {
        if (!node.getNodeRef().getStoreRef().getProtocol().equals(StoreRef.PROTOCOL_WORKSPACE)) {
            return false;
        }
        boolean viewMode = new ViewStateActionEvaluator().evaluate(node);
        Object docStatus = node.getProperties().get(DocumentCommonModel.Props.DOC_STATUS.toString());
        if (!(viewMode
                && EqualsHelper.nullSafeEquals(DocumentStatus.WORKING.getValueName(), docStatus)
                && getDocumentTemplateService().hasDocumentsTemplate(node.getNodeRef())
                && node.hasPermission(DocumentCommonModel.Privileges.EDIT_DOCUMENT))) {
            return false;
        }
        Set<Task> allActiveTasks = WorkflowUtil.getTasks(new HashSet<Task>(), BeanHelper.getWorkflowBlockBean().getCompoundWorkflows(), allActiveTasksPredicate);
        return allActiveTasks.isEmpty() || containsResponsibleAssignmentTask(allActiveTasks);
    }

    private static final Predicate<Task> allActiveTasksPredicate = new Predicate<Task>() {
        @Override
        public boolean eval(Task task) {
            return task.isStatus(Status.IN_PROGRESS);
        }
    };

    private boolean containsResponsibleAssignmentTask(Set<Task> tasks) {
        for (Task task : tasks) {
            if (task.isType(WorkflowSpecificModel.Types.ASSIGNMENT_TASK) && task.getNode().hasAspect(WorkflowSpecificModel.Aspects.RESPONSIBLE)) {
                return true;
            }
        }
        return false;
    }
}