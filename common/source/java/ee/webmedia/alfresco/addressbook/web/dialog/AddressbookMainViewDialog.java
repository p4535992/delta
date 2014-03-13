package ee.webmedia.alfresco.addressbook.web.dialog;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.Application;
import org.alfresco.web.app.context.IContextListener;
import org.alfresco.web.app.context.UIContextService;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.ui.common.component.data.UIRichList;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.addressbook.model.AddressbookModel.Types;
import ee.webmedia.alfresco.app.AppConstants;
import ee.webmedia.alfresco.utils.UserUtil;

public class AddressbookMainViewDialog extends AddressbookBaseDialog implements IContextListener {
    private static final long serialVersionUID = 1L;

    private transient UIRichList orgRichList;
    private transient UIRichList peopleRichList;
    private transient UIRichList orgPeopleRichList;

    private List<Node> organizations = Collections.emptyList();
    private List<Node> people = Collections.emptyList();
    private String searchCriteria = "";
    private boolean search = false;

    public AddressbookMainViewDialog() {
        UIContextService.getInstance(FacesContext.getCurrentInstance()).registerBean(this);
    }

    /**
     * @return the list of org Nodes to display
     */
    public List<Node> getOrganizations() {
        return organizations;
    }

    public NodeRef getAddressbookNode() {
        return getAddressbookService().getAddressbookNodeRef();
    }

    @Override
    public void init(Map<String, String> params) {
        organizations = Collections.emptyList();
        people = Collections.emptyList();
        setSearchCriteria("");
        showAll();
        super.init(params);
    }

    @Override
    public void restored() {
        if (getSearchCriteria().trim().length() == 0) {
            showAll();
        } else {
            search();
        }
        super.restored();
    }

    @Override
    public String getCancelButtonLabel() {
        return Application.getMessage(FacesContext.getCurrentInstance(), "back_button");
    }

    /**
     * Action handler to show all the entrys currently in the system
     * 
     * @return The outcome
     */
    public String showAll() {
        setSearch(false);
        clearRichLists();
        setSearchCriteria("");
        setOrgPeople(Collections.<Node> emptyList());
        setOrganizations(getAddressbookService().listOrganization());
        setPeople(getAddressbookService().listPerson());
        // return null to stay on the same page
        return null;
    }

    public List<Node> getPeople() {
        return people;
    }

    public void setPeople(List<Node> people) {
        this.people = people;
    }

    public void setOrganizations(List<Node> organizations) {
        this.organizations = organizations;
    }

    public String search() {
        setSearch(false);
        clearRichLists();
        setCurrentNode(null);
        List<Node> a = getAddressbookService().search(getSearchCriteria());
        List<Node> o = new ArrayList<Node>();
        List<Node> p = new ArrayList<Node>();
        List<Node> op = new ArrayList<Node>();
        for (Node node : a) {
            if (node.getType().equals(Types.ORGANIZATION)) {
                o.add(node);
            } else if (node.getType().equals(Types.PRIV_PERSON)) {
                p.add(node);
            } else if (node.getType().equals(Types.ORGPERSON)) {
                setSearch(true);
                // add?
                op.add(node);
            }
        }
        setOrganizations(o);
        setPeople(p);
        setOrgPeople(op);
        return null;
    }

    /**
     * Query callback method executed by the Generic Picker component.
     * This method is part of the contract to the Generic Picker, it is up to the backing bean
     * to execute whatever query is appropriate and return the results.
     * 
     * @param filterIndex Index of the filter drop-down selection
     * @param contains Text from the contains textbox
     * @return An array of SelectItem objects containing the results to display in the picker.
     */
    public SelectItem[] searchContacts(int filterIndex, String contains) {
        final String personLabel = Application.getMessage(FacesContext.getCurrentInstance(), "addressbook_private_person").toLowerCase();
        final String organizationLabel = Application.getMessage(FacesContext.getCurrentInstance(), "addressbook_org").toLowerCase();
        List<Node> nodes = getAddressbookService().search(contains);
        return transformNodesToSelectItems(nodes, personLabel, organizationLabel);
    }

    public SelectItem[] searchOrgContacts(int filterIndex, String contains) {
        final String personLabel = Application.getMessage(FacesContext.getCurrentInstance(), "addressbook_private_person").toLowerCase();
        final String organizationLabel = Application.getMessage(FacesContext.getCurrentInstance(), "addressbook_org").toLowerCase();
        List<Node> nodes = getAddressbookService().searchOrgContacts(contains);
        return transformNodesToSelectItems(nodes, personLabel, organizationLabel);
    }

    /**
     * Transforms the list of contact nodes (usually returned by the search) to SelectItems in the following form:
     * OrganizationName (organizationLabel, email) -- if it's an organization
     * FirstName LastName (privPersonLabel, email) -- if it's a person (can be both orgPerson or privPerson in the model)
     * 
     * @param nodes
     * @param privPersonLabel
     * @param organizationLabel
     * @return
     */
    public static SelectItem[] transformNodesToSelectItems(List<Node> nodes, String privPersonLabel, String organizationLabel) {
        SelectItem[] results = new SelectItem[nodes.size()];
        int i = 0;
        for (Node node : nodes) {
            String value = node.getNodeRefAsString();
            StringBuilder label = new StringBuilder();
            Map<String, Object> props = node.getProperties();
            if (node.getType().equals(Types.ORGANIZATION)) {
                label.append((String) props.get(AddressbookModel.Props.ORGANIZATION_NAME.toString()));
                label.append(" (");
                label.append(organizationLabel);
            } else if (node.getType().equals(Types.CONTACT_GROUP)) {
                label.append((String) props.get(AddressbookModel.Props.GROUP_NAME));
            } else {
                label.append(UserUtil.getPersonFullName((String) props.get(AddressbookModel.Props.PERSON_FIRST_NAME.toString()), (String) props
                        .get(AddressbookModel.Props.PERSON_LAST_NAME.toString())));
                label.append(" (");
                label.append(privPersonLabel);
            }
            String email = (String) props.get(AddressbookModel.Props.EMAIL.toString());
            if (StringUtils.isNotEmpty(email)) {
                label.append(", ");
                label.append(email);
            }
            if (!node.getType().equals(Types.CONTACT_GROUP)) {
                label.append(")");
            }

            results[i++] = new SelectItem(value, label.toString());
        }

        Arrays.sort(results, new Comparator<SelectItem>() {
            @Override
            public int compare(SelectItem a, SelectItem b) {
                return AppConstants.DEFAULT_COLLATOR.compare(a.getLabel(), b.getLabel());
            }
        });
        return results;
    }

    public List<String> getContactData(String nodeRef) {
        List<String> list = new ArrayList<String>();
        Map<QName, Serializable> props = getNodeService().getProperties(new NodeRef(nodeRef));

        QName type = getNodeService().getType(new NodeRef(nodeRef));
        String name = getContactFullName(props, type);

        list.add(name);
        list.add((String) props.get(AddressbookModel.Props.EMAIL));
        return list;
    }

    public static String getContactFullName(Map<QName, Serializable> props, QName contactType) {
        String name;
        if (Types.CONTACT_GROUP.equals(contactType)) {
            name = (String) props.get(AddressbookModel.Props.GROUP_NAME);
        } else if (Types.ORGANIZATION.equals(contactType)) {
            name = (String) props.get(AddressbookModel.Props.ORGANIZATION_NAME);
        } else {
            name = UserUtil.getPersonFullName((String) props.get(AddressbookModel.Props.PERSON_FIRST_NAME), (String) props
                    .get(AddressbookModel.Props.PERSON_LAST_NAME));
        }
        return name;
    }

    private void clearRichLists() {
        if (getOrgRichList() != null) {
            getOrgRichList().setValue(null);
        }
        if (getPeopleRichList() != null) {
            getPeopleRichList().setValue(null);
        }
        if (getOrgPeopleRichList() != null) {
            getOrgPeopleRichList().setValue(null);
        }
    }

    public UIRichList getOrgRichList() {
        return orgRichList;
    }

    public void setOrgRichList(UIRichList orgRichList) {
        this.orgRichList = orgRichList;
    }

    public UIRichList getPeopleRichList() {
        return peopleRichList;
    }

    public void setPeopleRichList(UIRichList peopleRichList) {
        this.peopleRichList = peopleRichList;
    }

    @Override
    public void contextUpdated() {
        clearRichLists();
    }

    @Override
    public void areaChanged() {
        clearRichLists();
    }

    @Override
    public void spaceChanged() {
        clearRichLists();
    }

    public String getSearchCriteria() {
        return searchCriteria;
    }

    public void setSearchCriteria(String searchCriteria) {
        this.searchCriteria = searchCriteria;
    }

    public boolean isSearch() {
        return search;
    }

    public void setSearch(boolean search) {
        this.search = search;
    }

    public UIRichList getOrgPeopleRichList() {
        return orgPeopleRichList;
    }

    public void setOrgPeopleRichList(UIRichList orgPeopleRichList) {
        this.orgPeopleRichList = orgPeopleRichList;
    }

    @Override
    protected void reset() {
        super.reset();
        orgRichList = null;
        peopleRichList = null;
        orgPeopleRichList = null;
        organizations = Collections.emptyList();
        people = Collections.emptyList();
        searchCriteria = "";
    }

}
