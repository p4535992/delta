package ee.webmedia.alfresco.addressbook.web.dialog;

import static ee.webmedia.alfresco.common.web.BeanHelper.getAddressbookService;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;

import ee.webmedia.alfresco.common.service.CreateObjectCallback;
import ee.webmedia.alfresco.common.web.WmNode;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel.Types;
import ee.webmedia.alfresco.addressbook.service.AddressbookEntry;
import ee.webmedia.alfresco.addressbook.util.AddressbookUtil;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.utils.MessageUtil;

public class AddressbookListDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "AddressbookListDialog";
    private List<AddressbookEntry> organizations;
    private List<AddressbookEntry> people;
    private List<AddressbookEntry> orgPeople;
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

    public boolean isShowAction() {
        return BeanHelper.getUserService().isDocumentManager();
    }

    /**
     * Action handler to show all the entries currently in the system
     *
     * @return The outcome
     */
    public String showAll() {
        setSearchCriteria("");
        BeanHelper.getTransactionService().getRetryingTransactionHelper().doInTransaction(new RetryingTransactionHelper.RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable {

                setOrganizations(getAddressbookService().listAddressbookEntries(Types.ORGANIZATION));
                setPeople(getAddressbookService().listAddressbookEntries(Types.PRIV_PERSON));

                if (!getOrganizations().isEmpty()) {
                    List<NodeRef> orgs = new ArrayList<>();
                    for (AddressbookEntry organization : getOrganizations()) {
                        orgs.add(organization.getNodeRef());
                    }
                    Map<NodeRef, List<Node>> personsByOrg = BeanHelper.getBulkLoadNodeService().loadChildNodes(orgs, new HashSet<QName>(), Types.ORGPERSON, null,
                            new CreateObjectCallback<Node>() {
                                @Override
                                public Node create(NodeRef nodeRef, Map<QName, Serializable> properties) {
                                    return new WmNode(nodeRef, Types.ORGPERSON, null, properties);
                                }
                            });

                    List<AddressbookEntry> organizationPeople = new ArrayList<>();
                    for (Map.Entry<NodeRef, List<Node>> addressbookEntries : personsByOrg.entrySet()) {
                        NodeRef organizationRef = addressbookEntries.getKey();
                        for (Node node : addressbookEntries.getValue()) {
                            organizationPeople.add(new AddressbookEntry(node, organizationRef));
                        }
                    }
                    setOrgPeople(organizationPeople);
                }
                return null;
            }
        });

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
                node.addPropertyResolver("parentOrgRef", AddressbookUtil.resolverParentOrgRef);
                getOrgPeople().add(new AddressbookEntry(node));
            }
        }
        return null;
    }

    @Override
    public void clean() {
        super.clean();
        reset();
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
        if (orgPeople == null) {
            orgPeople = new ArrayList<>();
        }
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
        if (organizations == null) {
            organizations = new ArrayList<>();
        }
        return organizations;
    }

    public void setOrganizations(List<AddressbookEntry> organizations) {
        this.organizations = organizations;
    }

    public List<AddressbookEntry> getPeople() {
        if (people == null) {
            people = new ArrayList<>();
        }
        return people;
    }

    public void setPeople(List<AddressbookEntry> people) {
        this.people = people;
    }
}
