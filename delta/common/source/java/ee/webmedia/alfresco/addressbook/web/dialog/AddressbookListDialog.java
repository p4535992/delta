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

import ee.webmedia.alfresco.addressbook.model.AddressbookModel.Types;
import ee.webmedia.alfresco.addressbook.util.AddressbookUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * @author Keit Tehvan
 */
public class AddressbookListDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "AddressbookListDialog";
    private List<Node> organizations = Collections.<Node> emptyList();
    private List<Node> orgPeople = Collections.<Node> emptyList();
    private List<Node> people = Collections.<Node> emptyList();
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
        setOrgPeople(Collections.<Node> emptyList());
        setOrganizations(getAddressbookService().listOrganization());
        setPeople(getAddressbookService().listPerson());
        return null;
    }

    public String search() {
        List<Node> a = getAddressbookService().search(getSearchCriteria(), -1);
        setOrganizations(new ArrayList<Node>());
        setPeople(new ArrayList<Node>());
        setOrgPeople(new ArrayList<Node>());
        for (Node node : a) {
            if (node.getType().equals(Types.ORGANIZATION)) {
                getOrganizations().add(node);
            } else if (node.getType().equals(Types.PRIV_PERSON)) {
                getPeople().add(node);
            } else if (node.getType().equals(Types.ORGPERSON)) {
                node.addPropertyResolver("parentOrgName", AddressbookUtil.resolverParentOrgName);
                node.addPropertyResolver("parentOrgRef", AddressbookUtil.resolverParentOrgRef);
                getOrgPeople().add(node);
            }
        }
        return null;
    }

    protected void reset() {
        orgPeople = Collections.<Node> emptyList();
        organizations = Collections.<Node> emptyList();
        people = Collections.<Node> emptyList();
        searchCriteria = "";
    }

    @Override
    public Object getActionsContext() {
        return this;
    }

    public List<Node> getOrgPeople() {
        return orgPeople;
    }

    public void setOrgPeople(List<Node> list) {
        orgPeople = list;
    }

    public String getSearchCriteria() {
        return searchCriteria;
    }

    public void setSearchCriteria(String searchCriteria) {
        this.searchCriteria = searchCriteria;
    }

    public List<Node> getOrganizations() {
        return organizations;
    }

    public void setOrganizations(List<Node> organizations) {
        this.organizations = organizations;
    }

    public List<Node> getPeople() {
        return people;
    }

    public void setPeople(List<Node> people) {
        this.people = people;
    }

}
