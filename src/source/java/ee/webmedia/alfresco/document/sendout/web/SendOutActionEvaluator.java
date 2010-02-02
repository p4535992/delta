package ee.webmedia.alfresco.document.sendout.web;

import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.Repository;

import ee.webmedia.alfresco.document.permissions.Permission;
import ee.webmedia.alfresco.document.web.evaluator.DocumentSavedActionEvaluator;
import ee.webmedia.alfresco.document.web.evaluator.ViewStateActionEvaluator;

/**
 * Evaluator, that evaluates to true if user is admin or document manager or document owner.
 * 
 * @author Erko Hansar
 */
public class SendOutActionEvaluator extends BaseActionEvaluator {

    private static final long serialVersionUID = 2958297435415449179L;

    public boolean evaluate(Node node) {
        ViewStateActionEvaluator viewStateEval = new ViewStateActionEvaluator();
        DocumentSavedActionEvaluator documentSavedEval = new DocumentSavedActionEvaluator();
        PermissionService permissionService = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getPermissionService();
        return viewStateEval.evaluate(node) && documentSavedEval.evaluate(node)
                && permissionService.hasPermission(node.getNodeRef(), Permission.DOCUMENT_WRITE.getValueName()).equals(AccessStatus.ALLOWED);
    }

}
