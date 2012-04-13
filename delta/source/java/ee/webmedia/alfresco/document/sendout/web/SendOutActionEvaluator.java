package ee.webmedia.alfresco.document.sendout.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAdminService;

import java.util.Map;

import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel.Props;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.web.evaluator.DocumentSavedActionEvaluator;
import ee.webmedia.alfresco.document.web.evaluator.ViewStateActionEvaluator;
import ee.webmedia.alfresco.workflow.service.HasNoStoppedOrInprogressWorkflowsEvaluator;

/**
 * Evaluator, that evaluates to true if user is admin or document manager or document owner.
 * 
 * @author Erko Hansar
 */
public class SendOutActionEvaluator extends BaseActionEvaluator {

    private static final long serialVersionUID = 2958297435415449179L;

    @Override
    public boolean evaluate(Node node) {
        if (!node.getNodeRef().getStoreRef().getProtocol().equals(StoreRef.PROTOCOL_WORKSPACE)) {
            return false;
        }
        boolean result = new ViewStateActionEvaluator().evaluate(node) && new DocumentSavedActionEvaluator().evaluate(node)
                && node.hasPermission(DocumentCommonModel.Privileges.EDIT_DOCUMENT);
        if (result) {
            final Map<String, Object> props = node.getProperties();
            final String regNumber = (String) props.get(DocumentCommonModel.Props.REG_NUMBER);
            if (regNumber == null) {
                result = new HasNoStoppedOrInprogressWorkflowsEvaluator().evaluate(node);
            }
            if (result) {
                BeanHelper.getDocumentService().throwIfNotDynamicDoc(node);
                String docTypeId = (String) node.getProperties().get(Props.OBJECT_TYPE_ID);
                if (!getDocumentAdminService().getDocumentTypeProperty(docTypeId, DocumentAdminModel.Props.SEND_UNREGISTRATED_DOC_ENABLED, Boolean.class)) {
                    result = regNumber != null;
                }
            }
        }
        return result;
    }
}
