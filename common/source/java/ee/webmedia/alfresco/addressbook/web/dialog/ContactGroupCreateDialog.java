package ee.webmedia.alfresco.addressbook.web.dialog;

import static ee.webmedia.alfresco.common.web.BeanHelper.getAddressbookService;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.web.app.Application;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.utils.MessageUtil;

public class ContactGroupCreateDialog extends ContactGroupBaseDialog {

    private static final long serialVersionUID = 1L;

    private static final String MSG_BUTTON_NEW_GROUP = "addressbook_contactgroup_create_title";

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        getAddressbookService().addOrUpdateNode(getCurrentNode(), null);
        MessageUtil.addInfoMessage("save_success");
        return outcome;
    }

    @Override
    public String cancel() {
        reset();
        return super.cancel();
    }

    @Override
    public String getFinishButtonLabel() {
        return Application.getMessage(FacesContext.getCurrentInstance(), MSG_BUTTON_NEW_GROUP);
    }

    public void createContactGroup(@SuppressWarnings("unused") ActionEvent event) {
        setCurrentNode(getAddressbookService().getEmptyNode(AddressbookModel.Types.CONTACT_GROUP));
    }
}
