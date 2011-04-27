package ee.webmedia.alfresco.document.web.evaluator;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;

/**
 * UI action evaluator for validating whether reopen current document.
 * 
 * @author Romet Aidla
 */
public class ReopenDocumentEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 0L;

    @Override
    public boolean evaluate(Node node) {
        boolean isFinished = DocumentStatus.FINISHED.getValueName().equals(node.getProperties().get(DocumentCommonModel.Props.DOC_STATUS.toString()));
        return isFinished && new ViewStateActionEvaluator().evaluate(node) && hasUserRights(node);
    }

    public static boolean hasUserRights(Node docNode) {
        boolean isOwner = new IsOwnerEvaluator().evaluate(docNode);
        return isOwner || BeanHelper.getUserService().isDocumentManager()
                || BeanHelper.getWorkflowService().isOwnerOfInProgressActiveResponsibleAssignmentTask(docNode.getNodeRef());
    }
}
