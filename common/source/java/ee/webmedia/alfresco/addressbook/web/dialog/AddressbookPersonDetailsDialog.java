package ee.webmedia.alfresco.addressbook.web.dialog;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.MapNode;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

<<<<<<< HEAD
/**
 * @author Keit Tehvan
 */
=======
>>>>>>> develop-5.1
public class AddressbookPersonDetailsDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "AddressbookPersonDetailsDialog";

    private NodeRef currentNode = null;

    @Override
    public String getCancelButtonLabel() {
        return MessageUtil.getMessage("back_button");
    }

    protected void reset() {
        currentNode = null;
        BeanHelper.getAddressbookGroupsManagerBeanBean().reset();
    }

    @Override
    public void restored() {
        BeanHelper.getAddressbookGroupsManagerBeanBean().setup(currentNode);
    }

    public void setupViewEntry(String nodeRefString) {
        reset();
        NodeRef ref = new NodeRef(nodeRefString);
        setCurrentNodeRef(ref);
        BeanHelper.getAddressbookGroupsManagerBeanBean().setup(ref);
    }

    public void setupViewEntry(ActionEvent event) {
        setupViewEntry(ActionUtil.getParam(event, "nodeRef"));
    }

    public Node getCurrentNode() {
        if (currentNode == null) {
            return null;
        }
        return new MapNode(currentNode);
    }

    public String getPanelLabel() {
        if (getCurrentNode().getType().equals(AddressbookModel.Types.PRIV_PERSON)) {
            return MessageUtil.getMessage("addressbook_private_person_data");
        }
        return MessageUtil.getMessage("addressbook_org_person_data");
    }

    public NodeRef getCurrentNodeRef() {
        return currentNode;
    }

    protected void setCurrentNodeRef(NodeRef ref) {
        currentNode = ref;
    }

    public void setCurrentNode(Node currentNode) {
        this.currentNode = currentNode != null ? currentNode.getNodeRef() : null;
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

    @Override
    public Object getActionsContext() {
        return this;
    }

}
