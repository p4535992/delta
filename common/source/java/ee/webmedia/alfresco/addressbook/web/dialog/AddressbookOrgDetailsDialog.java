package ee.webmedia.alfresco.addressbook.web.dialog;

import static ee.webmedia.alfresco.common.web.BeanHelper.getAddressbookService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import ee.webmedia.alfresco.addressbook.service.AddressbookEntry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.repository.MapNode;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.utils.TextUtil;

public class AddressbookOrgDetailsDialog extends AddressbookPersonDetailsDialog {
    private static final long serialVersionUID = 1L;
    @SuppressWarnings("hiding")
    public static final String BEAN_NAME = "AddressbookOrgDetailsDialog";
    MapNode currentNode;

    private List<AddressbookEntry> orgPeople;

    @Override
    protected void reset() {
        super.reset();
        orgPeople = null;
        currentNode = null;
    }

    public List<AddressbookEntry> getOrgPeople() {
        if (getCurrentNode() == null) {
            return (orgPeople != null) ? orgPeople : Collections.<AddressbookEntry>emptyList();
        }

        if (orgPeople == null) {
            List<AddressbookEntry> addressbookEntries = getAddressbookService().listOrganizationPeople(getCurrentNodeRef());
            orgPeople = (addressbookEntries == null) ? new ArrayList<AddressbookEntry>() : addressbookEntries;
        }
        return orgPeople;
    }

    public void setOrgPeople(List<AddressbookEntry> list) {
        orgPeople = list;
    }

    @Override
    public void restored() {
        super.restored();
        currentNode = groupNodeValues(super.getCurrentNodeRef());
        orgPeople = null;
    }

    @Override
    public Node getCurrentNode() {
        if (currentNode == null) {
            currentNode = groupNodeValues(getCurrentNodeRef());
        }
        return currentNode;
    }

    @Override
    protected void setCurrentNodeRef(NodeRef ref) {
        super.setCurrentNodeRef(ref);
        currentNode = groupNodeValues(ref);
    }

    private MapNode groupNodeValues(NodeRef ref) {
        MapNode groupedNode = new MapNode(ref);
        Map<String, Object> properties = groupedNode.getProperties();
        properties.put(
                    AddressbookModel.Props.PHONE.toString(),
                    TextUtil.joinNonBlankStringsWithComma(
                            Arrays.asList(
                                    (String) groupedNode.getProperties().get(AddressbookModel.Props.PHONE),
                                    (String) groupedNode.getProperties().get(AddressbookModel.Props.FIRST_ADDITIONAL_PHONE),
                                    (String) groupedNode.getProperties().get(AddressbookModel.Props.SECOND_ADDITIONAL_PHONE)
                                    ))
                    );
        properties.put(
                    AddressbookModel.Props.EMAIL.toString(),
                    TextUtil.joinNonBlankStringsWithComma(
                            Arrays.asList(
                                    (String) groupedNode.getProperties().get(AddressbookModel.Props.EMAIL),
                                    (String) groupedNode.getProperties().get(AddressbookModel.Props.FIRST_ADDITIONAL_EMAIL),
                                    (String) groupedNode.getProperties().get(AddressbookModel.Props.SECOND_ADDITIONAL_EMAIL)
                                    ))
                    );
        return groupedNode;
    }
}
