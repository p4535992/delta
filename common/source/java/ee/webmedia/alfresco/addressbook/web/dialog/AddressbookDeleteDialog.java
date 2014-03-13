package ee.webmedia.alfresco.addressbook.web.dialog;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.app.Application;

import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

public class AddressbookDeleteDialog extends AddressbookBaseDialog {
    private static final long serialVersionUID = 1L;

    public static String BEAN_NAME = "addressbookDeleteDialog";

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Exception {
        getAddressbookService().deleteNode(getCurrentNode().getNodeRef());
        reset();
        MessageUtil.addInfoMessage("addressbook_contactgroup_remove_contact_success");
        return outcome;
    }

    public void setupDelete(ActionEvent event) {
        setCurrentNode(getAddressbookService().getNode(new NodeRef(ActionUtil.getParam(event, "nodeRef"))));
    }

    @Override
    public String getFinishButtonLabel() {
        return Application.getMessage(FacesContext.getCurrentInstance(), "delete");
    }

    @Override
    public String getCancelButtonLabel() {
        return Application.getMessage(FacesContext.getCurrentInstance(), "back_button");
    }
}
