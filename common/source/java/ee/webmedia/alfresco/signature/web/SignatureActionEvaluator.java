package ee.webmedia.alfresco.signature.web;

import javax.faces.context.FacesContext;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.Repository;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.signature.service.DigiDoc4JSignatureService;

/**
 * Check necessary permissions for signing a file.
 * Use this evaluator for document nodes, not for folders. You must check for <code>ReadProperties</code> and <code>ReadContent</code> permissions beforehand
 * (in web-client config, action permissions).
 * If selected node is an existing DigiDoc container, then this evaluator checks for <code>Write</code> permission, because signing overwrites existing DigiDoc
 * file contents.
 * Otherwise, this evaluator checks for <code>CreateChildren</code> permission on the primary parent of selected node, because signing then creates a new
 * DigiDoc container file in the same folder.
 */
public class SignatureActionEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 1L;
    
    // TODO: digidoc4j adjust
    @Override
    public boolean evaluate(Node node) {
        DigiDoc4JSignatureService digiDoc4JSignatureService = (DigiDoc4JSignatureService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance()).getBean(
        		DigiDoc4JSignatureService.BEAN_NAME);
        if (digiDoc4JSignatureService.isBDocContainer(node.getNodeRef())) {
            return node.hasPermission(PermissionService.WRITE);
        } else {
            ServiceRegistry serviceRegistry = Repository.getServiceRegistry(FacesContext.getCurrentInstance());
            NodeRef parent = serviceRegistry.getNodeService().getPrimaryParent(node.getNodeRef()).getParentRef();
            if (parent == null) {
                return false;
            }
            return serviceRegistry.getPermissionService().hasPermission(parent, PermissionService.CREATE_CHILDREN) == AccessStatus.ALLOWED;
        }
    }

}
