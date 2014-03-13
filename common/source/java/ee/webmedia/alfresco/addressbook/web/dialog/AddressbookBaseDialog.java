package ee.webmedia.alfresco.addressbook.web.dialog;

import java.util.Collections;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.app.Application;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.MapNode;
import org.alfresco.web.bean.repository.Node;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.addressbook.service.AddressbookService;
import ee.webmedia.alfresco.utils.ActionUtil;

// this class is to be extended by other classes!!
public abstract class AddressbookBaseDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;

    private transient AddressbookService addressbookService;
    private NodeRef currentNode = null;

    private List<Node> orgPeople;

    public void setAddressbookService(AddressbookService addressbookService) {
        this.addressbookService = addressbookService;
    }

    protected AddressbookService getAddressbookService() {
        if (addressbookService == null) {
            addressbookService = (AddressbookService) FacesContextUtils.getRequiredWebApplicationContext(
                    FacesContext.getCurrentInstance()).getBean(AddressbookService.BEAN_NAME);
        }
        return addressbookService;
    }

    public NodeRef getOrgOfPerson(NodeRef ref) {
        return getAddressbookService().getOrgOfPerson(ref);
    }

    @Override
    public String getCancelButtonLabel() {
        return Application.getMessage(FacesContext.getCurrentInstance(), "close");
    }

    @Override
    public Object getActionsContext() {
        return this;
    }

    public void setupViewEntry(String nodeRefString) {
        setCurrentNode(getAddressbookService().getNode(new NodeRef(nodeRefString)));
    }
    
    public void setupViewEntry(ActionEvent event) {
        setupViewEntry(ActionUtil.getParam(event, "nodeRef"));
    }

    public Node getCurrentNode() {
        if (currentNode == null) {
            return null;
        }
        Node node = new MapNode(currentNode);
        node.getProperties();
        return node;
    }

    public NodeRef getCurrentNodeRef() {
        return currentNode;
    }

    public void setCurrentNode(Node currentNode) {
        if (currentNode != null) {
            this.currentNode = currentNode.getNodeRef();
        } else {
            this.currentNode = null;
        }
    }

    public List<Node> getOrgPeople() {
        if (getCurrentNode() == null) {
            if (orgPeople != null && orgPeople.size() > 0) {
                return orgPeople;
            }
            return Collections.emptyList();
        }
        return getAddressbookService().listPerson(getCurrentNodeRef());
    }

    public void setOrgPeople(List<Node> list) {
        orgPeople = list;
    }

    public Node getSummary() {
        return getCurrentNode();
    }

    // this method is never used, extending classes may use it
    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Exception {
        return null;
    }

    @Override
    public String cancel() {
        reset();
        return super.cancel();
    }

    protected void reset() {
        currentNode = null;
        orgPeople = null;
    }

    public boolean isShowChildren() {
        if (getOrgPeople().size() > 0) {
            return true;
        }
        return false;
    }

    public boolean isUserHasPermission() {
        return getAddressbookService().hasManagePermission();
    }

}
