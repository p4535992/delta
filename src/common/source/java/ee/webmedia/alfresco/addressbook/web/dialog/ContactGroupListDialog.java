package ee.webmedia.alfresco.addressbook.web.dialog;

import java.util.List;

import org.alfresco.web.bean.repository.Node;

public class ContactGroupListDialog extends ContactGroupBaseDialog {

    private static final long serialVersionUID = 1L;
    
    public List<Node> getContactGroups() {
        return getAddressbookService().listContactGroups();
    }

    @Override
    public Object getActionsContext() {
        return null;
    }

}
