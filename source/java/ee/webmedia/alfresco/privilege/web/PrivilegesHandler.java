package ee.webmedia.alfresco.privilege.web;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import javax.faces.event.ValueChangeEvent;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.apache.commons.lang.ObjectUtils;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.privilege.model.Privilege;
import ee.webmedia.alfresco.privilege.model.UserPrivileges;
import ee.webmedia.alfresco.privilege.web.ManageInheritablePrivilegesDialog.State;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.MessageData;
import ee.webmedia.alfresco.utils.MessageDataImpl;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException.MessageSeverity;

/**
 * Base class for node-type specific logic related to {@link ManageInheritablePrivilegesDialog}
 */
public abstract class PrivilegesHandler implements Serializable {
    private static final long serialVersionUID = 1L;
    private static Collection<Privilege> DOCUMENT_CASEFILE_MANAGEABLE_PERMISSIONS;

    private final Collection<Privilege> manageablePermissions;
    private final QName nodeType;

    protected Boolean checkboxValue;
    protected boolean checkboxValueBeforeSave;
    private Boolean editable;
    protected ManageInheritablePrivilegesDialog dialogBean;
    protected State state;

    protected PrivilegesHandler(QName nodeType, Collection<Privilege> manageablePermissions) {
        this.nodeType = nodeType;
        this.manageablePermissions = manageablePermissions;
    }

    public boolean isSubmitWhenCheckboxUnchecked() {
        return true;
    }

    /** @param loosingPrivileges - privileges by authority that are removed */
    protected boolean validate(Map<String, UserPrivileges> loosingPrivileges) {
        return true; // subclasses could override this method
    }

    public void save() {
        // subclasses could override this method
    }

    public void reset() {
        checkboxValue = null;
        checkboxValueBeforeSave = false;
        editable = null;
        dialogBean = null;
        state = null;
    }

    /** Called by JSF before {@link BaseDialogBean#finish()} */
    public void checkboxChanged(ValueChangeEvent e) {
        boolean newValue = (Boolean) e.getNewValue();
        if (checkboxValue.equals(newValue)) {
            throw new RuntimeException("FIXME checkboxValue=" + checkboxValue + ", newValue=" + newValue + " oldValue=" + e.getOldValue());
        }
        if (ObjectUtils.equals(e.getOldValue(), newValue)) {
            throw new RuntimeException("FIXME value not really changed checkboxValue=" + checkboxValue + ", newValue=" + newValue + " oldValue=" + e.getOldValue());
        }
    }

    /** Called by JSF after {@link #checkboxChanged(ValueChangeEvent)} */
    public void setCheckboxValue(Boolean checkboxValue) {
        this.checkboxValue = checkboxValue;
    }

    public Boolean getCheckboxValue() {
        initCheckboxValuesIfNeeded();
        return checkboxValue;
    }

    private void initCheckboxValuesIfNeeded() {
        if (checkboxValue == null) {
            checkboxValueBeforeSave = initCheckboxValue();
            checkboxValue = checkboxValueBeforeSave;
        }
    }

    /** Get manageable document permissions and case file permissions if <code>conf.casefile.enabled = true</code> */
    protected static Collection<Privilege> getDocumentCaseFilePrivs() {
        if (DOCUMENT_CASEFILE_MANAGEABLE_PERMISSIONS == null) {
            ArrayList<Privilege> privs = new ArrayList<Privilege>();
            if (BeanHelper.getApplicationConstantsBean().isCaseVolumeEnabled()) {
                privs.add(Privilege.VIEW_CASE_FILE);
                privs.add(Privilege.EDIT_CASE_FILE);
            }
            privs.add(Privilege.VIEW_DOCUMENT_FILES);
            privs.add(Privilege.VIEW_DOCUMENT_META_DATA);
            privs.add(Privilege.EDIT_DOCUMENT);
            DOCUMENT_CASEFILE_MANAGEABLE_PERMISSIONS = Collections.unmodifiableList(privs);
        }
        return DOCUMENT_CASEFILE_MANAGEABLE_PERMISSIONS;
    }

    protected abstract boolean initCheckboxValue();

    public String getCheckboxLabel() {
        return MessageUtil.getMessage(nodeType.getLocalName() + "_manage_inheritable_permissions_checkbox_label");
    }

    public void setDialogBean(ManageInheritablePrivilegesDialog dialogBean) {
        this.dialogBean = dialogBean;
    }

    public void setDialogState(State state) {
        this.state = state;
    }

    public QName getNodeType() {
        return nodeType;
    }

    public String getObjectOwner() {
        return null;
    }

    public Privilege getImplicitPrivilege() {
        return Privilege.VIEW_DOCUMENT_META_DATA;
    }

    /**
     * All authorities should have been added by the time of calling this method.
     * This method allows adding more privileges to already added authorities
     */
    protected void addDynamicPrivileges() {
        // subclasses could add more dynamic privileges if needed
    }

    public String getConfirmInlineGroupUsersMsg() {
        return MessageUtil.getMessageAndEscapeJS("manage_permissions_confirm_inlineGroupUsers") + " "
                + MessageUtil.getMessageAndEscapeJS(getNodeType().getLocalName() + "_manage_permissions_confirm_inlineGroupUsers_suffix");
    }

    public Collection<Privilege> getManageablePermissions() {
        return manageablePermissions;
    }

    public boolean isEditable() {
        if (editable == null) {
            UserService userService = BeanHelper.getUserService();
            if (userService.isAdministrator()) {
                editable = true;
            } else if (userService.isDocumentManager()
                    && BeanHelper.getPrivilegeService().hasPermission(state.getManageableRef(), AuthenticationUtil.getRunAsUser(), Privilege.VIEW_DOCUMENT_META_DATA)) {
                editable = true;
            } else {
                editable = false;
            }
        }
        return editable;
    }

    public String getContainerTitle() {
        return MessageUtil.getMessage(getNodeType().getLocalName() + "_manage_permissions_title");
    }

    public MessageData getInfoMessage() {
        return new MessageDataImpl(MessageSeverity.INFO, getNodeType().getLocalName() + "_manage_permissions_info");
    }

    public String getConfirmMessage() {
        return "";
    }

    public boolean isPermissionColumnDisabled(@SuppressWarnings("unused") Privilege privilege) {
        return false;
    }

}
