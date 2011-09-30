package ee.webmedia.alfresco.addressbook.web.bean;

import static ee.webmedia.alfresco.addressbook.util.AddressbookUtil.getContactFullName;
import static ee.webmedia.alfresco.addressbook.util.AddressbookUtil.transformAddressbookNodesToSelectItems;
import static ee.webmedia.alfresco.common.web.BeanHelper.getAddressbookService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getNodeService;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.addressbook.web.dialog.ContactGroupAddDialog.UserDetails;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageDataWrapper;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * @author Keit Tehvan
 */
public class AddressbookGroupsManagerBean implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "AddressbookGroupsManagerBean";
    private List<Node> groups;
    private String groupToAdd;
    private NodeRef currentNode;

    public void setup(NodeRef ref) {
        currentNode = ref;
    }

    public void setGroups(List<Node> groups) {
        this.groups = groups;
    }

    public List<Node> getGroups() {
        if (groups == null) {
            groups = getAddressbookService().getContactsGroups(currentNode);
        }
        return groups;
    }

    public void reset() {
        resetGroups();
        currentNode = null;
    }

    private void resetGroups() {
        groups = null;
        groupToAdd = null;
    }

    public void removeContactFromGroup(ActionEvent event) {
        String groupNodeRef = ActionUtil.getParam(event, "nodeRef");
        getAddressbookService().deleteFromGroup(new NodeRef(groupNodeRef), currentNode);
        MessageUtil.addInfoMessage("addressbook_contactgroup_remove_contact_from_group_success");
        resetGroups();
    }

    public SelectItem[] searchContactGroups(@SuppressWarnings("unused") int filterIndex, String contains) {
        List<Node> nodes = getAddressbookService().searchContactGroups(contains, false, !getNodeService().getType(currentNode).equals(AddressbookModel.Types.ORGANIZATION));
        return transformAddressbookNodesToSelectItems(nodes);
    }

    public void addToContactGroup(String groupNodeRef) {
        UserDetails details = new UserDetails(getContactFullName(getNodeService().getProperties(currentNode), getNodeService().getType(currentNode)), currentNode.toString());

        final MessageDataWrapper feedback = getAddressbookService().addToGroup(new NodeRef(groupNodeRef), Arrays.asList(details));
        MessageUtil.addStatusMessages(FacesContext.getCurrentInstance(), feedback);
        resetGroups();
    }

    public void setGroupToAdd(String groupToAdd) {
        this.groupToAdd = groupToAdd;
    }

    public String getGroupToAdd() {
        return groupToAdd;
    }

    public boolean isShowGroupsManager() {
        if (currentNode != null && AddressbookModel.Types.ORGPERSON.equals(getNodeService().getType(currentNode))) {
            return false;
        }
        return true;
    }

}
