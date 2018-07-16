package ee.webmedia.alfresco.document.web.evaluator;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.common.web.BeanHelper;

/**
 * Check if current user can add users to groups in this installation.
 */
public class AddUserToGroupEvaluator extends BaseActionEvaluator {

    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Node node) {
        return evaluate((Object) node);
    }

    @Override
    public boolean evaluate(Object obj) {
        return BeanHelper.getUserService().isDocumentManager() && BeanHelper.getApplicationConstantsBean().isGroupsEditingAllowed(); // admins are doc managers
    }

}
