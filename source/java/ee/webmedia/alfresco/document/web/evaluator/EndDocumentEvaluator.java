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
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
 */
public class EndDocumentEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 0L;

    @Override
    public boolean evaluate(Node node) {
<<<<<<< HEAD
        if (!node.getNodeRef().getStoreRef().getProtocol().equals(StoreRef.PROTOCOL_WORKSPACE) || !new DocumentNotInDraftsFunctionActionEvaluator().evaluate(node)) {
            return false;
        }
        String regNumber = (String) node.getProperties().get(DocumentCommonModel.Props.REG_NUMBER.toString());
        if (StringUtils.isBlank(regNumber) && !BeanHelper.getDocumentService().isFinishUnregisteredDocumentEnabled()) {
            return false;
        }
        boolean isWorking = DocumentStatus.WORKING.getValueName().equals(node.getProperties().get(DocumentCommonModel.Props.DOC_STATUS.toString()));
        return isWorking && ReopenDocumentEvaluator.hasUserRights(node);
=======
        if (!node.getNodeRef().getStoreRef().getProtocol().equals(StoreRef.PROTOCOL_WORKSPACE)) {
            return false;
        }
        String regNumber = (String) node.getProperties().get(DocumentCommonModel.Props.REG_NUMBER.toString());
        if (StringUtils.isBlank(regNumber)) {
            return false;
        }
        boolean isWorking = DocumentStatus.WORKING.getValueName().equals(node.getProperties().get(DocumentCommonModel.Props.DOC_STATUS.toString()));
        return isWorking && new ViewStateActionEvaluator().evaluate(node)
                && (ReopenDocumentEvaluator.hasUserRights(node) || BeanHelper.getWorkflowService().isOwnerOfInProgressActiveResponsibleAssignmentTask(node.getNodeRef()));
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    }
}
