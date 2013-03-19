package ee.webmedia.alfresco.privilege.web;

import java.util.Collection;

import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.privilege.model.UserPrivileges;

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
    protected boolean initCheckboxValue() {
        return BeanHelper.getPermissionService().getInheritParentPermissions(state.getManageableRef());
    }

    @Override
    public void save() {
        if (checkboxValueBeforeSave != checkboxValue) {
            BeanHelper.getPermissionService().setInheritParentPermissions(state.getManageableRef(), checkboxValue);
            if (!checkboxValue) {
                for (UserPrivileges authPrivs : state.getPrivMappings().getPrivilegesByUsername().values()) {
                    authPrivs.makeInheritedPrivilegesAsStatic();
                }
                for (UserPrivileges authPrivs : state.getPrivMappings().getPrivilegesByGroup().values()) {
                    authPrivs.makeInheritedPrivilegesAsStatic();
                }
            }
        }
    }
}