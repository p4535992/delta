package ee.webmedia.alfresco.privilege.web;

import java.util.Collection;

import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.privilege.model.Privilege;
import ee.webmedia.alfresco.privilege.model.UserPrivileges;
import ee.webmedia.alfresco.utils.CalendarUtil;

/**
 * Base class for PrivilegesHandler that allow users to change inheriting permissions from parent nodes
 */
public abstract class AbstractInheritingPrivilegesHandler extends PrivilegesHandler {
    private static final long serialVersionUID = 1L;

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(AbstractInheritingPrivilegesHandler.class);

    protected AbstractInheritingPrivilegesHandler(QName nodeType, Collection<Privilege> manageablePermissions) {
        super(nodeType, manageablePermissions);
    }

    @Override
    protected boolean initCheckboxValue() {
        return BeanHelper.getPrivilegeService().getInheritParentPermissions(state.getManageableRef());
    }

    @Override
    public void save() {
        if (checkboxValueBeforeSave != checkboxValue) {
            long startTime = 0L;
            if (LOG.isDebugEnabled()) {
                LOG.debug("Setting inherit to " + checkboxValue + " on " + state.getManageableRef());
                startTime = System.nanoTime();
            }
            BeanHelper.getPrivilegeService().setInheritParentPermissions(state.getManageableRef(), checkboxValue);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Set inherit to " + checkboxValue + " on " + state.getManageableRef() + " - took " + CalendarUtil.duration(startTime) + " ms");
            }
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