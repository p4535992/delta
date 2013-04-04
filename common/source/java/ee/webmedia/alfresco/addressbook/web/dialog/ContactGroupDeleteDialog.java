package ee.webmedia.alfresco.addressbook.web.dialog;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.app.Application;

import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

public class ContactGroupDeleteDialog extends ContactGroupBaseDialog {

    private static final long serialVersionUID = 1L;
    public static final String PARAM_GROUP_NODEREF = "nodeRef";
    public static final String PARAM_GROUP_NAME = "groupName";
    private static final String MSG_DELETE = "delete";
    public static final String MSG_DELETE_GROUP = "addressbook_contactgroup_delete_title";

    private String groupName;

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        getAddressbookService().deleteNode(getCurrentNode().getNodeRef());
        reset();
        MessageUtil.addInfoMessage("addressbook_contactgroup_delete_success");
        return outcome;
    }

    @Override
    public boolean getFinishButtonDisabled() {
        return false;
    }

    @Override
    public String getContainerTitle() {
        return MessageUtil.getMessage(FacesContext.getCurrentInstance(), MSG_DELETE_GROUP, groupName);
    }

    @Override
    public String getFinishButtonLabel() {
        return Application.getMessage(FacesContext.getCurrentInstance(), MSG_DELETE);
    }

    public void setupDeleteGroup(ActionEvent event) {
        String groupNodeRef = ActionUtil.getParam(event, PARAM_GROUP_NODEREF);
        groupName = ActionUtil.getParam(event, PARAM_GROUP_NAME);
        setCurrentNode(getAddressbookService().getNode(new NodeRef(groupNodeRef)));
    }

    public int getNumItemsInGroup() {
        return getAddressbookService().getContacts(getCurrentNode().getNodeRef()).size();
    }

    // START: setters/getters
    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
    // END: setters/getters
}
