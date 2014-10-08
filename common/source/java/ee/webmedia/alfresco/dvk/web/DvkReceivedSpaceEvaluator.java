package ee.webmedia.alfresco.dvk.web;

import javax.faces.context.FacesContext;

import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.Repository;

<<<<<<< HEAD
/**
 * @author Alar Kvell
 */
=======
>>>>>>> develop-5.1
public class DvkReceivedSpaceEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 1L;

    private static final String dvkReceived = QName.createQName(NamespaceService.APP_MODEL_1_0_URI, "dvkReceived").toString();

    @Override
    public boolean evaluate(Node node) {
        if (!Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getAuthorityService().hasAdminAuthority()) {
            return false;
        }
        return dvkReceived.equals(node.getNodePath().last().getElementString());
    }

}
