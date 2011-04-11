package ee.webmedia.alfresco.document.web.evaluator;

import javax.faces.context.FacesContext;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.user.service.UserService;

/**
 * UI action evaluator for validating whether user can end current document.
 * 
 * @author Romet Aidla
 */
public class EndDocumentEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 0L;

    @Override
    public boolean evaluate(Node node) {
        ViewStateActionEvaluator viewStateEval = new ViewStateActionEvaluator();
        boolean isInViewState = viewStateEval.evaluate(node);

        boolean isWorking = DocumentStatus.WORKING.getValueName().equals(node.getProperties().get(DocumentCommonModel.Props.DOC_STATUS.toString()));

        IsOwnerEvaluator isOwnerEval = new IsOwnerEvaluator();
        boolean isOwner = isOwnerEval.evaluate(node);

        String regNumber = (String) node.getProperties().get(DocumentCommonModel.Props.REG_NUMBER.toString());
        if (StringUtils.isBlank(regNumber)) {
            return false;
        }

        UserService userService = (UserService) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), UserService.BEAN_NAME);
        boolean hasUserRights = isOwner || userService.isAdministrator() || userService.isDocumentManager();
        return isInViewState && isWorking && hasUserRights;
    }
}
