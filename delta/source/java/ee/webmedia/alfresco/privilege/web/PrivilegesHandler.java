package ee.webmedia.alfresco.privilege.web;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import javax.faces.event.ValueChangeEvent;

import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.apache.commons.lang.ObjectUtils;

import ee.webmedia.alfresco.privilege.model.UserPrivileges;
import ee.webmedia.alfresco.privilege.web.ManageInheritablePrivilegesDialog.State;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Base class for node-type specific logic related to {@link ManageInheritablePrivilegesDialog}
 * 
 * @author Ats Uiboupin
 */
public abstract class PrivilegesHandler implements Serializable {
    private static final long serialVersionUID = 1L;
    /** used by series and volume permissions management */
    public static final Collection<String> DEFAULT_MANAGEABLE_PERMISSIONS = Arrays.asList(
            "viewCaseFile", "editCaseFile", "viewDocumentMetaData", "viewDocumentFiles", "editDocument");

    private final Collection<String> manageablePermissions;
    private final QName nodeType;

    protected Boolean checkboxValue;
    protected ManageInheritablePrivilegesDialog dialogBean;
    protected State state;

    protected PrivilegesHandler(QName nodeType) {
        this(nodeType, DEFAULT_MANAGEABLE_PERMISSIONS);
    }

    protected PrivilegesHandler(QName nodeType, Collection<String> manageablePermissions) {
        this.nodeType = nodeType;
        this.manageablePermissions = manageablePermissions;
    }

    public boolean isSubmitWhenCheckboxUnchecked() {
        return true;
    }

    public void reset() {
        checkboxValue = null;
        dialogBean = null;
        state = null;
    }

    /** Called by JSF before {@link BaseDialogBean#finish()} */
    final public void checkboxChanged(ValueChangeEvent e) {
        boolean newValue = (Boolean) e.getNewValue();
        if (checkboxValue.equals(newValue)) {
            throw new RuntimeException("FIXME checkboxValue=" + checkboxValue + ", newValue=" + newValue + " oldValue=" + e.getOldValue());
        }
        if (ObjectUtils.equals(e.getOldValue(), newValue)) {
            throw new RuntimeException("FIXME value not really changed checkboxValue=" + checkboxValue + ", newValue=" + newValue + " oldValue=" + e.getOldValue());
        }
        checkboxChanged(newValue);
    }

    /** Called by JSF after {@link #checkboxChanged(ValueChangeEvent)} */
    public void setCheckboxValue(Boolean checkboxValue) {
        this.checkboxValue = checkboxValue;
    }

    public abstract Boolean getCheckboxValue();

    protected abstract void checkboxChanged(boolean newValue);

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

    protected void addDynamicPrivileges() {
        // subclasses could add more dynamic privileges if needed
    }

    protected boolean validate(@SuppressWarnings("unused") Map<String, UserPrivileges> loosingPrivileges) {
        return true; // subclasses could override this method
    }

    public String getConfirmInlineGroupUsersMsg() {
        return MessageUtil.getMessageAndEscapeJS("manage_permissions_confirm_inlineGroupUsers") + " "
                + MessageUtil.getMessageAndEscapeJS(getNodeType().getLocalName() + "_manage_permissions_confirm_inlineGroupUsers_suffix");
    }

    public Collection<String> getManageablePermissions() {
        return manageablePermissions;
    }

}
