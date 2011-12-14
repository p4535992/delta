package ee.webmedia.alfresco.privilege.web;

import java.util.Collection;

import org.alfresco.service.namespace.QName;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Base class for PrivilegesHandler that allow users to change inheriting permissions from parent nodes
 * 
 * @author Ats Uiboupin
 */
public abstract class AbstractInheritingPrivilegesHandler extends PrivilegesHandler {
    private static final long serialVersionUID = 1L;

    protected AbstractInheritingPrivilegesHandler(QName nodeType, Collection<String> manageablePermissions) {
        super(nodeType, manageablePermissions);
    }

    @Override
    public Boolean getCheckboxValue() {
        if (checkboxValue == null) {
            checkboxValue = BeanHelper.getPermissionService().getInheritParentPermissions(state.getManageableRef());
        }
        return checkboxValue;
    }

    @Override
    protected void checkboxChanged(boolean newValue) {
        BeanHelper.getPermissionService().setInheritParentPermissions(state.getManageableRef(), newValue);
        MessageUtil.addErrorMessage("FIXME inherit permissions ? " + newValue);// FIXME PRIV2 Ats
        if (BeanHelper.getApplicationService().isTest()) { // FIXME PRIV2 Ats võib hiljem eemaldada
            Assert.isTrue(newValue == BeanHelper.getPermissionService().getInheritParentPermissions(state.getManageableRef()), "permission not applied");
        }
    }

}