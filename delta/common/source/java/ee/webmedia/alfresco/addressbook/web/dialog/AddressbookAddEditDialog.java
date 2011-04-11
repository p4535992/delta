package ee.webmedia.alfresco.addressbook.web.dialog;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.TransientNode;
import org.alfresco.web.ui.common.Utils;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.addressbook.service.AddressbookService;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;

/**
 * @author Keit Tehvan
 */
public class AddressbookAddEditDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;

    public static final String BEAN_NAME = "AddressbookAddEditDialog";

    private transient AddressbookService addressbookService;
    protected Node entry;
    protected NodeRef parentOrg = null;
    private boolean skipReset = false;

    // ------------------------------------------------------------------------------
    // Wizard implementation

    public void setupAdd(ActionEvent event) {
        QName type = QName.createQName(ActionUtil.getParam(event, "type"), getNamespaceService());
        entry = getAddressbookService().getEmptyNode(type);
        if (ActionUtil.hasParam(event, "parentOrg")) {
            parentOrg = new NodeRef(ActionUtil.getParam(event, "parentOrg"));
        } else {
            parentOrg = null;
        }
    }

    public void setupEdit(ActionEvent event) {
        NodeRef nodeRef = new NodeRef(ActionUtil.getParam(event, "nodeRef"));
        entry = getAddressbookService().getNode(nodeRef);
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Exception {
        if (getEntry() == null) {
            Utils.addErrorMessage("addressbook_data_not_found");
            isFinished = false;
            skipReset = true;
            return null;
        }
        if (validate()) {
            return saveData(context, outcome);
        } else {
            skipReset = true;
            isFinished = false;
        }
        return null;
    }

    private boolean validate() {
        Node contact = getEntry();
        if (Boolean.TRUE.equals(contact.getProperties().get(AddressbookModel.Props.TASK_CAPABLE))) {
            if (StringUtils.isBlank((String) contact.getProperties().get(AddressbookModel.Props.EMAIL))) {
                MessageUtil.addInfoMessage("addressbook_contact_email_empty_error");
                return false;
            }
        }
        return true;
    }

    @Override
    public String cancel() {
        entry = null;
        skipReset = false;
        return super.cancel();
    }

    @Override
    protected String doPostCommitProcessing(FacesContext context, String outcome) {
        if (!skipReset) {
            entry = null;
        }
        return outcome;
    }

    // XXX quick fix
    public Object getAllActionsDataModel() {
        return new Object() {
            public int getRowCount() {
                return 0;
            }
        };
    }

    protected String saveData(FacesContext context, String outcome) {
        try {
            getAddressbookService().checkIfContactExists(getEntry());
            persistEntry();
        } catch (UnableToPerformException e) {
            ConfirmAddDuplicateDialog confirmBean = (ConfirmAddDuplicateDialog) FacesHelper.getManagedBean( //
                    context, ConfirmAddDuplicateDialog.BEAN_NAME);
            confirmBean.setConfirmMessage(MessageUtil.getMessage(e.getMessageKey(), e.getMessageValuesForHolders()));
            isFinished = false;
            skipReset = true;
            outcome = "dialog:confirmAddDuplicate";
        }
        MessageUtil.addInfoMessage("save_success");
        return outcome;
    }

    public void persistEntry() {
        getAddressbookService().addOrUpdateNode(getEntry(), parentOrg);
        skipReset = false;
    }

    public String getConfirmMessage() {
        return "addressbook_save_organization_error_nameExists";
    }

    @Override
    public boolean getFinishButtonDisabled() {
        return false;
    }

    @Override
    public String getContainerTitle() {
        String messageId = "addressbook_" + ((entry instanceof TransientNode) ? "add" : "edit") + "_" + entry.getType().getLocalName();
        return MessageUtil.getMessage(messageId);
    }

    public String getPanelTitle() {
        String messageId = "addressbook_" + ((entry instanceof TransientNode) ? "new" : "edit") + "_entry";
        return MessageUtil.getMessage(messageId);
    }

    public boolean getNotAllowedEditTaskCapable() {
        if (entry == null || entry instanceof TransientNode) {
            return false;
        }
        return getAddressbookService().isTaskCapableGroupMember(entry.getNodeRef());
    }

    // ------------------------------------------------------------------------------
    // Bean Getters and Setters

    protected AddressbookService getAddressbookService() {
        if (addressbookService == null) {
            addressbookService = (AddressbookService) FacesContextUtils.getRequiredWebApplicationContext(
                    FacesContext.getCurrentInstance()).getBean(AddressbookService.BEAN_NAME);
        }
        return addressbookService;
    }

    public void setAddressbookService(AddressbookService addressbookService) {
        this.addressbookService = addressbookService;
    }

    public Node getEntry() {
        return entry;
    }

}
