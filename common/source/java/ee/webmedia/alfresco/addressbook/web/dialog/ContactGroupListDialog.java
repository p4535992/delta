package ee.webmedia.alfresco.addressbook.web.dialog;

import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.repository.Node;

public class ContactGroupListDialog extends ContactGroupBaseDialog {

    private static final long serialVersionUID = 1L;

    public List<Node> getContactGroups() {
        return getAddressbookService().listContactGroups();
    }

    public NodeRef getAddressbookNode() {
        final NodeRef addressbookNodeRef = getAddressbookService().getAddressbookNodeRef();
        return addressbookNodeRef;
    }

    @Override
    public Object getActionsContext() {
        return null;
    }

}
