package ee.webmedia.alfresco.document.sendout.web;

import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.CHANCELLORS_ORDER;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.CONTRACT_SIM;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.CONTRACT_SMIT;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.DECREE;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.ERRAND_APPLICATION_DOMESTIC;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.ERRAND_ORDER_ABROAD;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.INCOMING_LETTER;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.INTERNAL_APPLICATION;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.LEAVING_LETTER;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.MANAGEMENTS_ORDER;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.PERSONELLE_ORDER_SIM;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.PERSONELLE_ORDER_SMIT;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.REGULATION;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.TENDERING_APPLICATION;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.TRAINING_APPLICATION;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.VACATION_ORDER;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.VACATION_ORDER_SMIT;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.Repository;

import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.permissions.Permission;
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
    private static final List<QName> registeredTypes = Arrays.asList(
            INCOMING_LETTER, CHANCELLORS_ORDER, PERSONELLE_ORDER_SIM, PERSONELLE_ORDER_SMIT, REGULATION, DECREE, CONTRACT_SIM
            , CONTRACT_SMIT, MANAGEMENTS_ORDER, TRAINING_APPLICATION, LEAVING_LETTER, INTERNAL_APPLICATION, VACATION_ORDER
            , VACATION_ORDER_SMIT, TENDERING_APPLICATION, ERRAND_APPLICATION_DOMESTIC, ERRAND_ORDER_ABROAD);

    @Override
    public boolean evaluate(Node node) {
        ViewStateActionEvaluator viewStateEval = new ViewStateActionEvaluator();
        PermissionService permissionService = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getPermissionService();
        boolean result = viewStateEval.evaluate(node) && new DocumentSavedActionEvaluator().evaluate(node)
                && permissionService.hasPermission(node.getNodeRef(), Permission.DOCUMENT_WRITE.getValueName()).equals(AccessStatus.ALLOWED)
                && new HasNoStoppedOrInprogressWorkflowsEvaluator().evaluate(node);
        if (result && registeredTypes.contains(node.getType())) {
            final Map<String, Object> props = node.getProperties();
            final String regNumber = (String) props.get(DocumentCommonModel.Props.REG_NUMBER);
            result = regNumber != null;
        }

        return result;
    }
}
