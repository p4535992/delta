package ee.webmedia.alfresco.document.web.evaluator;

import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;

/**
 * UI action evaluator for validating whether user can end current document.
<<<<<<< HEAD
 * 
 * @author Romet Aidla
=======
>>>>>>> develop-5.1
 */
public class EndDocumentEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 0L;

    @Override
    public boolean evaluate(Node node) {
        if (!node.getNodeRef().getStoreRef().getProtocol().equals(StoreRef.PROTOCOL_WORKSPACE) || !new DocumentNotInDraftsFunctionActionEvaluator().evaluate(node)) {
            return false;
        }
        String regNumber = (String) node.getProperties().get(DocumentCommonModel.Props.REG_NUMBER.toString());
        if (StringUtils.isBlank(regNumber) && !BeanHelper.getDocumentService().isFinishUnregisteredDocumentEnabled()) {
            return false;
        }
        boolean isWorking = DocumentStatus.WORKING.getValueName().equals(node.getProperties().get(DocumentCommonModel.Props.DOC_STATUS.toString()));
<<<<<<< HEAD
        return isWorking && ReopenDocumentEvaluator.hasUserRights(node);
=======
        return isWorking && new ViewStateActionEvaluator().evaluate(node)
                && (ReopenDocumentEvaluator.hasUserRights(node) || BeanHelper.getWorkflowService().isOwnerOfInProgressActiveResponsibleAssignmentTask(node.getNodeRef()));
>>>>>>> develop-5.1
    }
}
