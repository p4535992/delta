package ee.webmedia.alfresco.addressbook.web.dialog;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.addressbook.model.AddressbookModel.Types;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UserUtil;

public class ContactGroupListDialog extends ContactGroupBaseDialog {

    private static final long serialVersionUID = 1L;

    private transient UserService userService;
    private List<Node> contactGroups;
    private final Map<NodeRef, Boolean> originalTaskCapableValues = new HashMap<NodeRef, Boolean>();

    @Override
    public void init(Map<String, String> params) {
        super.init(params);
        contactGroups = getAddressbookService().listContactGroups();
        for (Node contactGroup : contactGroups) {
            originalTaskCapableValues.put(contactGroup.getNodeRef(), Boolean.TRUE.equals(contactGroup.getProperties().get(AddressbookModel.Props.TASK_CAPABLE)));
        }
    }

    public NodeRef getAddressbookNode() {
        final NodeRef addressbookNodeRef = getAddressbookService().getAddressbookNodeRef();
        return addressbookNodeRef;
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {

        // collect contacts for groups where taskCapable property has changed
        Map<NodeRef, Pair<Pair<String, Boolean>, List<Node>>> contactGroupContacts = new HashMap<NodeRef, Pair<Pair<String, Boolean>, List<Node>>>();
        for (Node contactGroup : contactGroups) {
            Map<String, Object> contactGroupProperties = contactGroup.getProperties();
            Boolean taskCapableValue = (Boolean) contactGroupProperties.get(AddressbookModel.Props.TASK_CAPABLE);
            Boolean originalTaskCapableValue = originalTaskCapableValues.get(contactGroup.getNodeRef());
            // if taskCapable property is changed, update all contacts in the contact group
            if (taskCapableValue != originalTaskCapableValue) {
                List<Node> contactNodes = getAddressbookService().getContactsByType(Types.PERSON_BASE, contactGroup.getNodeRef());
                contactNodes.addAll(getAddressbookService().getContactsByType(Types.ORGANIZATION, contactGroup.getNodeRef()));
                contactGroupContacts.put(contactGroup.getNodeRef()
                        , new Pair<Pair<String, Boolean>, List<Node>>(new Pair<String, Boolean>((String) contactGroupProperties.get(AddressbookModel.Props.GROUP_NAME),
                                taskCapableValue), contactNodes));
            }
        }

        // check if all contacts where taskCapable is going to be true, have email addresses
        boolean contactsValid = true;
        for (Map.Entry<NodeRef, Pair<Pair<String, Boolean>, List<Node>>> contactGroupEntry : contactGroupContacts.entrySet()) {
            if (contactGroupEntry.getValue().getFirst().getSecond()) {
                for (Node contact : contactGroupEntry.getValue().getSecond()) {
                    Map<String, Object> contactProps = contact.getProperties();
                    if (StringUtils.isBlank((String) contactProps.get(AddressbookModel.Props.EMAIL))) {
                        contactsValid = false;
                        String name = null;
                        if (Types.ORGANIZATION.equals(contact.getType())) {
                            name = (String) contactProps.get(AddressbookModel.Props.ORGANIZATION_NAME);
                        } else {
                            name = UserUtil.getPersonFullName((String) contactProps.get(AddressbookModel.Props.PERSON_FIRST_NAME), (String) contactProps
                                    .get(AddressbookModel.Props.PERSON_LAST_NAME));
                        }
                        MessageUtil.addInfoMessage("addressbook_contactgroup_edit_empty_email_error", contactGroupEntry.getValue().getFirst().getFirst(), name);
                    }
                }
            }
        }
        // update contacts' and contact groups' data
        if (contactsValid) {
            for (Node contactGroup : contactGroups) {
                Pair<Pair<String, Boolean>, List<Node>> contactGroupData = contactGroupContacts.get(contactGroup.getNodeRef());
                if (contactGroupData != null) {
                    for (Node contact : contactGroupData.getSecond()) {
                        getNodeService().setProperty(contact.getNodeRef(), AddressbookModel.Props.TASK_CAPABLE, contactGroupData.getFirst().getSecond());
                    }
                }
                getAddressbookService().addOrUpdateNode(contactGroup, null);
            }
        }
        isFinished = false;
        return null;
    }

    public boolean getNotAllowedEditTaskCapable() {
        return !getUserService().isAdministrator() && !getUserService().isDocumentManager();
    }

    public List<Node> getContactGroups() {
        return contactGroups;
    }

    @Override
    public boolean getFinishButtonDisabled() {
        return false;
    }

    @Override
    public Object getActionsContext() {
        return null;
    }

    public UserService getUserService() {
        if (userService == null) {
            userService = (UserService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())//
                    .getBean(UserService.BEAN_NAME);
        }
        return userService;
    }

}
