package ee.webmedia.alfresco.addressbook.web.dialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.app.Application;
import org.alfresco.web.app.context.IContextListener;
import org.alfresco.web.app.context.UIContextService;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.ui.common.component.data.UIRichList;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.addressbook.model.AddressbookModel.Types;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UserUtil;

public class ContactGroupContactsDialog extends ContactGroupBaseDialog implements IContextListener {

    private static final long serialVersionUID = 1L;

    public static final String PARAM_GROUP_NODEREF = "groupNodeRef";
    public static final String PARAM_CONTACT_NODEREF = "contactNodeRef";

    protected UIRichList usersRichList;

    private String personLabel;
    private String organizationLabel;

    public ContactGroupContactsDialog() {
        UIContextService.getInstance(FacesContext.getCurrentInstance()).registerBean(this);
    }

    @Override
    public void init(Map<String, String> parameters) {
        super.init(parameters);
        personLabel = Application.getMessage(FacesContext.getCurrentInstance(), "addressbook_private_person").toLowerCase();
        organizationLabel = Application.getMessage(FacesContext.getCurrentInstance(), "addressbook_org").toLowerCase();
    }

    public List<Map<String, String>> getGroupContacts() {
        List<Node> personNodes = getAddressbookService().getContactsByType(Types.PERSON_BASE, getCurrentNode().getNodeRef());
        List<Node> orgNodes = getAddressbookService().getContactsByType(Types.ORGANIZATION, getCurrentNode().getNodeRef());
        List<Map<String, String>> data = new ArrayList<Map<String, String>>(personNodes.size() + orgNodes.size());

        for (Node person : personNodes) {
            Map<String, String> elem = new HashMap<String, String>(3);
            elem.put("name", UserUtil.getPersonFullName((String) person.getProperties().get(AddressbookModel.Props.PERSON_FIRST_NAME.toString()),
                    (String) person.getProperties().get(AddressbookModel.Props.PERSON_LAST_NAME.toString())));
            elem.put("type", personLabel);
            elem.put("nodeRef", person.getNodeRefAsString());
            data.add(elem);
        }

        for (Node organization : orgNodes) {
            Map<String, String> elem = new HashMap<String, String>(3);
            elem.put("name", (String) organization.getProperties().get(AddressbookModel.Props.ORGANIZATION_NAME.toString()));
            elem.put("type", organizationLabel);
            elem.put("nodeRef", organization.getNodeRefAsString());
            data.add(elem);
        }
        return data;
    }

    /**
     * Action called when a Group folder is clicked.
     * Navigate into the Group and show child contacts.
     */
    public void clickGroup(ActionEvent event) {
        String groupNodeRef = ActionUtil.getParam(event, PARAM_GROUP_NODEREF);
        setCurrentNode(getAddressbookService().getNode(new NodeRef(groupNodeRef)));
        contextUpdated();
    }

    public void removeContact(ActionEvent event) {
        String contactNodeRef = ActionUtil.getParam(event, PARAM_CONTACT_NODEREF);
        getAddressbookService().deleteFromGroup(getCurrentNode().getNodeRef(), new NodeRef(contactNodeRef));
        contextUpdated();
        MessageUtil.addInfoMessage("addressbook_contactgroup_remove_contact_success");
    }

    @Override
    public Object getActionsContext() {
        return getCurrentNode();
    }

    @Override
    public String getContainerTitle() {
        return MessageUtil.getMessage("addressbook_contactgroup_contacts_title" //
                , getNodeService().getProperty(getCurrentNode().getNodeRef(), AddressbookModel.Props.GROUP_NAME));
    }

    @Override
    public void areaChanged() {
        // noop
    }

    @Override
    public void contextUpdated() {
        if (usersRichList != null) {
            usersRichList.setValue(null);
        }
    }

    @Override
    public void spaceChanged() {
        // noop
    }

    // START: setters/getters
    public UIRichList getUsersRichList() {
        return usersRichList;
    }

    public void setUsersRichList(UIRichList usersRichList) {
        this.usersRichList = usersRichList;
    }
    // END: setters/getters
}
