package ee.webmedia.alfresco.addressbook.web.dialog;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.faces.model.SelectItem;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.Application;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.ui.common.component.UIGenericPicker;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.addressbook.model.AddressbookModel.Types;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageDataWrapper;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UserUtil;

public class ContactGroupAddDialog extends ContactGroupBaseDialog {

    private static final long serialVersionUID = 1L;
    public static final String PARAM_GROUP_NODEREF = "nodeRef";

    /** selected users to be added to a group */
    protected List<UserDetails> usersForGroup;

    /** datamodel for table of users added to group */
    transient protected DataModel usersDataModel = null;

    @Override
    public void init(Map<String, String> parameters) {
        super.init(parameters);

        usersDataModel = null;
        usersForGroup = new ArrayList<UserDetails>();
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        // add each selected user to the current group in turn
        try {
            final MessageDataWrapper feedback = getAddressbookService().addToGroup(getCurrentNode().getNodeRef(), usersForGroup);
            final boolean isErrorAdded = MessageUtil.addStatusMessages(context, feedback);
            if(!isErrorAdded) {
                MessageUtil.addInfoMessage("save_success");
            }
        } catch (RuntimeException e) {
            System.out.println(e);
            throw e;
        }
        reset();
        return outcome;
    }

    @Override
    public boolean getFinishButtonDisabled() {
        return false;
    }

    @Override
    protected void reset() {
        super.reset();
        usersDataModel = null;
        usersForGroup = null;
    }

    public void setupAddGroup(ActionEvent event) {
        String groupNodeRef = ActionUtil.getParam(event, PARAM_GROUP_NODEREF);
        setCurrentNode(getAddressbookService().getNode(new NodeRef(groupNodeRef)));
    }

    public SelectItem[] pickerCallback(@SuppressWarnings("unused") int filterIndex, final String contains) {
        final String privPersonLabel = Application.getMessage(FacesContext.getCurrentInstance(), "addressbook_private_person").toLowerCase();
        final String organizationLabel = Application.getMessage(FacesContext.getCurrentInstance(), "addressbook_org").toLowerCase();
        List<Node> nodes = getAddressbookService().search(contains);
        return AddressbookMainViewDialog.transformNodesToSelectItems(nodes, privPersonLabel, organizationLabel);
    }

    public void addSelectedUsers(ActionEvent event) {
        UIGenericPicker picker = (UIGenericPicker) event.getComponent().findComponent("picker");
        String[] results = picker.getSelectedResults();
        if (results != null) {
            for (String nodeRefStr : results) {
                // check for same nodeRef so not added twice
                boolean foundExisting = false;
                for (UserDetails userDetails : usersForGroup) {
                    if (nodeRefStr.equals(userDetails.getNodeRef())) {
                        foundExisting = true;
                        break;
                    }
                }

                if (foundExisting == false) {
                    NodeRef nodeRef = new NodeRef(nodeRefStr);
                    StringBuilder label = new StringBuilder();

                    // build a display label showing the user person name
                    Map<QName, Serializable> props = getNodeService().getProperties(nodeRef);
                    QName type = getNodeService().getType(nodeRef);

                    if (Types.ORGANIZATION.equals(type)) {
                        label.append((String) props.get(AddressbookModel.Props.ORGANIZATION_NAME));
                    } else {
                        label.append(UserUtil.getPersonFullName((String) props.get(AddressbookModel.Props.PERSON_FIRST_NAME), (String) props
                                .get(AddressbookModel.Props.PERSON_LAST_NAME)));
                    }
                    // add a wrapper object with the details to the results list for display
                    UserDetails userDetails = new UserDetails(label.toString(), nodeRefStr);
                    this.usersForGroup.add(userDetails);
                }
            }
        }
    }

    public void removeUserSelection(@SuppressWarnings("unused") ActionEvent event) {
        UserDetails wrapper = (UserDetails) this.usersDataModel.getRowData();
        if (wrapper != null) {
            this.usersForGroup.remove(wrapper);
        }
    }

    // START: setters/getters
    public DataModel getUsersDataModel() {
        if (usersDataModel == null) {
            usersDataModel = new ListDataModel();
        }
        // only set the wrapped data once otherwise the rowindex is reset
        if (usersDataModel.getWrappedData() == null) {
            usersDataModel.setWrappedData(usersForGroup);
        }
        return usersDataModel;
    }

    // END: setters/getters

    /**
     * Simple wrapper bean exposing contact name and nodeRef for JSF results list.
     */
    public static class UserDetails implements Serializable {
        private static final long serialVersionUID = 1L;

        private String name;
        private String nodeRef;

        public UserDetails(String name, String nodeRef) {
            this.name = name;
            this.nodeRef = nodeRef;
        }

        public String getName() {
            return name;
        }

        public String getNodeRef() {
            return nodeRef;
        }
    }
}
