package ee.webmedia.alfresco.addressbook.web.dialog;

import static ee.webmedia.alfresco.common.web.BeanHelper.getAddressbookService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.addressbook.model.AddressbookModel.Types;
import ee.webmedia.alfresco.addressbook.service.AddressbookEntry;
import ee.webmedia.alfresco.addressbook.util.AddressbookUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

<<<<<<< HEAD
/**
 * @author Keit Tehvan
 */
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
public class AddressbookListDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "AddressbookListDialog";
    private List<AddressbookEntry> organizations = Collections.emptyList();
    private List<AddressbookEntry> orgPeople = Collections.emptyList();
    private List<AddressbookEntry> people = Collections.emptyList();
    private String searchCriteria = "";

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Exception {
        return null;
    }

    @Override
    public String cancel() {
        reset();
        return super.cancel();
    }

    @Override
    public void init(Map<String, String> params) {
        reset();
        showAll();
        super.init(params);
    }

    @Override
    public void restored() {
        if (StringUtils.isBlank(getSearchCriteria())) {
            showAll();
        } else {
            search();
        }
        super.restored();
    }

    @Override
    public String getCancelButtonLabel() {
        return MessageUtil.getMessage(FacesContext.getCurrentInstance(), "back_button");
    }

    /**
     * Action handler to show all the entries currently in the system
     * 
     * @return The outcome
     */
    public String showAll() {
        setSearchCriteria("");
<<<<<<< HEAD
        setOrgPeople(new ArrayList<AddressbookEntry>());
        setOrganizations(getAddressbookService().listAddressbookEntries(AddressbookModel.Assocs.ORGANIZATIONS));
        setPeople(getAddressbookService().listAddressbookEntries(AddressbookModel.Assocs.ABPEOPLE));
=======
        setOrganizations(getAddressbookService().listAddressbookEntries(AddressbookModel.Assocs.ORGANIZATIONS));
        setPeople(getAddressbookService().listAddressbookEntries(AddressbookModel.Assocs.ABPEOPLE));

        List<AddressbookEntry> organizationPeople = new ArrayList<AddressbookEntry>();
        for (AddressbookEntry orgEntry : getOrganizations()) {
            for (Node node : getAddressbookService().listPerson(orgEntry.getNode().getNodeRef())) {
                node.addPropertyResolver("parentOrgName", AddressbookUtil.resolverParentOrgName);
                node.addPropertyResolver("parentOrgRef", AddressbookUtil.resolverParentOrgRef);
                organizationPeople.add(new AddressbookEntry(node));
            }
        }
        setOrgPeople(organizationPeople);

>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
        return null;
    }

    public String search() {
        List<Node> a = getAddressbookService().search(getSearchCriteria(), -1, false);
        setOrganizations(new ArrayList<AddressbookEntry>());
        setPeople(new ArrayList<AddressbookEntry>());
        setOrgPeople(new ArrayList<AddressbookEntry>());
        for (Node node : a) {
            if (node.getType().equals(Types.ORGANIZATION)) {
                getOrganizations().add(new AddressbookEntry(node));
            } else if (node.getType().equals(Types.PRIV_PERSON)) {
                getPeople().add(new AddressbookEntry(node));
            } else if (node.getType().equals(Types.ORGPERSON)) {
<<<<<<< HEAD
=======
                node.addPropertyResolver("parentOrgName", AddressbookUtil.resolverParentOrgName);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
                node.addPropertyResolver("parentOrgRef", AddressbookUtil.resolverParentOrgRef);
                getOrgPeople().add(new AddressbookEntry(node));
            }
        }
        return null;
    }

    protected void reset() {
        orgPeople = Collections.emptyList();
        organizations = Collections.emptyList();
        people = Collections.emptyList();
        searchCriteria = "";
    }

    @Override
    public Object getActionsContext() {
        return this;
    }

    public List<AddressbookEntry> getOrgPeople() {
        return orgPeople;
    }

    public void setOrgPeople(List<AddressbookEntry> list) {
        orgPeople = list;
    }

    public String getSearchCriteria() {
        return searchCriteria;
    }

    public void setSearchCriteria(String searchCriteria) {
        this.searchCriteria = searchCriteria;
    }

    public List<AddressbookEntry> getOrganizations() {
        return organizations;
    }

    public void setOrganizations(List<AddressbookEntry> organizations) {
        this.organizations = organizations;
    }

    public List<AddressbookEntry> getPeople() {
        return people;
    }

    public void setPeople(List<AddressbookEntry> people) {
        this.people = people;
    }
}
