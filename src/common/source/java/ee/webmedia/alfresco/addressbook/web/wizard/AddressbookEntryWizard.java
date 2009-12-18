package ee.webmedia.alfresco.addressbook.web.wizard;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.wizard.BaseWizardBean;
import org.alfresco.web.ui.common.Utils;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.addressbook.service.AddressbookService;
import ee.webmedia.alfresco.utils.ActionUtil;

/**
 * @author Keit Tehvan
 */
public abstract class AddressbookEntryWizard extends BaseWizardBean {
    private static final long serialVersionUID = 1L;

    transient private AddressbookService addressbookService;
    private Node entry = null;
    private boolean add = true;

    // ------------------------------------------------------------------------------
    // Wizard implementation

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Exception {
        if (this.getEntry() == null) {
            Utils.addErrorMessage("addressbook_data_not_found");
            return null;
        }
        return saveData(context, outcome);

    }

    // XXX quick fix
    public Object getAllActionsDataModel(){
        return new Object(){
            public int getRowCount(){ return 0;}
        };
    }
    
    protected String saveData(FacesContext context, String outcome) {
        getAddressbookService().addOrUpdateNode(getEntry(), null);
        return outcome;
    }

    public void setupEdit(ActionEvent event) {
        setAdd(false);
        setEntry(getAddressbookService().getNode(new NodeRef(ActionUtil.getParam(event, "nodeRef"))));
    }

    public void setupEntry(QName type) {
        if (isAdd()) {
            entry = getAddressbookService().getEmptyNode(type);
        }
        setAdd(true);
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

    public Node getSummary() {
        return entry;
    }

    public Node getEntry() {
        return this.entry;
    }

    public void setEntry(Node entry) {
        this.entry = entry;
    }

    public boolean isAdd() {
        return add;
    }

    public void setAdd(boolean add) {
        this.add = add;
    }

}
