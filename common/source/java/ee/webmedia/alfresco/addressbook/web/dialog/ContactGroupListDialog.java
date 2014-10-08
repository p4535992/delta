package ee.webmedia.alfresco.addressbook.web.dialog;

import static ee.webmedia.alfresco.common.web.BeanHelper.getAddressbookService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getUserService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.addressbook.model.AddressbookModel.Types;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UserUtil;

public class ContactGroupListDialog extends ContactGroupBaseDialog {

    public static final String BEAN_NAME = "ContactGroupListDialog";
    private static final long serialVersionUID = 1L;

    private List<Node> contactGroups;
    private final Map<NodeRef, Boolean> originalTaskCapableValues = new HashMap<NodeRef, Boolean>();

    @Override
    public void init(Map<String, String> params) {
        super.init(params);
        initContactGroups();
    }

    @Override
    public void restored() {
        initContactGroups();
    }

    private void initContactGroups() {
        contactGroups = getAddressbookService().listContactGroups();
        for (Node contactGroup : getContactGroups()) {
            originalTaskCapableValues.put(contactGroup.getNodeRef(), Boolean.TRUE.equals(contactGroup.getProperties().get(AddressbookModel.Props.TASK_CAPABLE)));
        }
    }

    @Override
    public void clean() {
        super.clean();
        contactGroups = null;
        originalTaskCapableValues.clear();
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {

        // collect contacts for groups where taskCapable property has changed
        Map<NodeRef, Pair<Pair<String, Boolean>, List<Node>>> contactGroupContacts = new HashMap<NodeRef, Pair<Pair<String, Boolean>, List<Node>>>();
        for (Node contactGroup : getContactGroups()) {
            Map<String, Object> contactGroupProperties = contactGroup.getProperties();
            Boolean taskCapableValue = Boolean.TRUE.equals(contactGroupProperties.get(AddressbookModel.Props.TASK_CAPABLE));
            Boolean originalTaskCapableValue = originalTaskCapableValues.get(contactGroup.getNodeRef());
            // if taskCapable property is changed, update all contacts in the contact group
            if (taskCapableValue && taskCapableValue != originalTaskCapableValue) { // Kontaktgrupid.docx 4.1.5.4 - update contacts only if new value is true
                List<Node> contactNodes = getAddressbookService().getContactsByType(Types.PERSON_BASE, contactGroup.getNodeRef());
                if (!contactNodes.isEmpty()) {
                    MessageUtil.addInfoMessage("addressbook_contactgroup_edit_contains_people_error");
                    return null;
                }
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
            MessageUtil.addInfoMessage("save_success");
        }
        return null;
    }

    public boolean getNotAllowedEditTaskCapable() {
        return !getUserService().isAdministrator() && !getUserService().isDocumentManager();
    }

    public List<Node> getContactGroups() {
        if (contactGroups == null) {
            contactGroups = new ArrayList<>();
        }
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

    public boolean getHideFinishButton() {
        return !BeanHelper.getUserService().isDocumentManager();
    }
}
