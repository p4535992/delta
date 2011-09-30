package ee.webmedia.alfresco.addressbook.web.dialog;

import static ee.webmedia.alfresco.common.web.BeanHelper.getAddressbookService;

import java.util.Collections;
import java.util.List;

import org.alfresco.web.bean.repository.Node;

/**
 * @author Keit Tehvan
 */
public class AddressbookOrgDetailsDialog extends AddressbookPersonDetailsDialog {
    private static final long serialVersionUID = 1L;
    @SuppressWarnings("hiding")
    public static final String BEAN_NAME = "AddressbookOrgDetailsDialog";

    private List<Node> orgPeople;

    @Override
    protected void reset() {
        super.reset();
        orgPeople = null;
    }

    public List<Node> getOrgPeople() {
        if (getCurrentNode() == null) {
            if (orgPeople != null && !orgPeople.isEmpty()) {
                return orgPeople;
            }
            return Collections.emptyList();
        }
        return getAddressbookService().listPerson(getCurrentNodeRef());
    }

    public void setOrgPeople(List<Node> list) {
        orgPeople = list;
    }

}
