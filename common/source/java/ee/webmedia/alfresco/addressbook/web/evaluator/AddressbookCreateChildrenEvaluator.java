package ee.webmedia.alfresco.addressbook.web.evaluator;

import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.ui.repo.component.evaluator.PermissionEvaluator;

import ee.webmedia.alfresco.addressbook.service.AddressbookService;

/**
 * Evaluator to check if creating children (orgainzations, persons, groups) to addressbook is enabled
 */
public class AddressbookCreateChildrenEvaluator extends BaseActionEvaluator {

    private static final long serialVersionUID = 8856155730090956174L;

    @Override
    public boolean evaluate(Node node) {
        return evaluate();
    }

    @Override
    public boolean evaluate(Object obj) {
        return evaluate();
    }

    private boolean evaluate() {
        AddressbookService userService = (AddressbookService) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), AddressbookService.BEAN_NAME);
        final NodeRef addressbookNodeRef = userService.getAddressbookNodeRef();
        final PermissionEvaluator permissionEvaluator = new PermissionEvaluator();
        permissionEvaluator.setValue(addressbookNodeRef);
        permissionEvaluator.setAllow("CreateChildren");
        final boolean res = permissionEvaluator.evaluate();
        return res;
    }

}
