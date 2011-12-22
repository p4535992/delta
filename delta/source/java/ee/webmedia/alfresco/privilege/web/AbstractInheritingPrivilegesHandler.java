package ee.webmedia.alfresco.privilege.web;

import java.util.Collection;

import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.common.web.BeanHelper;

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
    }

}