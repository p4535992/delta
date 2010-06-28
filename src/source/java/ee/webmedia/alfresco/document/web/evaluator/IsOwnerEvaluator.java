package ee.webmedia.alfresco.document.web.evaluator;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.document.model.DocumentCommonModel;

/**
 *  UI action evaluator for validating whether user is the owner of the document.
 *
 * @author Romet Aidla
 */
public class IsOwnerEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 0L;

    @Override
    public boolean evaluate(Node node) {
        return AuthenticationUtil.getRunAsUser().equals(node.getProperties().get(DocumentCommonModel.Props.OWNER_ID.toString()));
    }
}
