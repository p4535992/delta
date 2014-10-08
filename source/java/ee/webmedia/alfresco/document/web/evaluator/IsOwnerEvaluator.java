package ee.webmedia.alfresco.document.web.evaluator;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.document.model.DocumentCommonModel;

/**
 * UI action evaluator for validating whether user is the owner of the document.
<<<<<<< HEAD
 * 
 * @author Romet Aidla
=======
>>>>>>> develop-5.1
 */
public class IsOwnerEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 0L;

    @Override
    public boolean evaluate(Object obj) {
        if (obj instanceof NodeRef) {
            return evaluate(new Node((NodeRef) obj));
        }
        throw new RuntimeException("Expected nodeRef (or Node), got " + obj);
    }

    @Override
    public boolean evaluate(Node node) {
        return AuthenticationUtil.getRunAsUser().equals(node.getProperties().get(DocumentCommonModel.Props.OWNER_ID.toString()));
    }
}
