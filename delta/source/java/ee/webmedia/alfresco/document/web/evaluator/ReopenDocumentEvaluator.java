package ee.webmedia.alfresco.document.web.evaluator;

import javax.faces.context.FacesContext;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.user.service.UserService;

/**
 * UI action evaluator for validating whether reopen current document.
 * 
 * @author Romet Aidla
 */
public class ReopenDocumentEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 0L;

    @Override
    public boolean evaluate(Node node) {
        ViewStateActionEvaluator viewStateEval = new ViewStateActionEvaluator();
        boolean isInViewState = viewStateEval.evaluate(node);

        boolean isFinished = DocumentStatus.FINISHED.getValueName().equals(node.getProperties().get(DocumentCommonModel.Props.DOC_STATUS.toString()));

        IsOwnerEvaluator isOwnerEval = new IsOwnerEvaluator();
        boolean isOwner = isOwnerEval.evaluate(node);

        UserService userService = (UserService) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), UserService.BEAN_NAME);
        boolean hasUserRights = isOwner || userService.isAdministrator() || userService.isDocumentManager();
        return isInViewState && isFinished && hasUserRights;
    }
}
