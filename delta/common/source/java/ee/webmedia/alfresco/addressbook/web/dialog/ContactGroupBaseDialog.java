package ee.webmedia.alfresco.addressbook.web.dialog;

import javax.faces.context.FacesContext;

import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.addressbook.service.AddressbookService;

public class ContactGroupBaseDialog extends BaseDialogBean {

    private static final long serialVersionUID = 1L;

    private transient AddressbookService addressbookService;
    private Node currentNode;

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        // save button not used here
        return null;
    }

    protected void reset() {
        currentNode = null;
    }

    // START: setters/getters
    public Node getCurrentNode() {
        return currentNode;
    }

    public void setCurrentNode(Node currentNode) {
        this.currentNode = currentNode;
    }

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
    // END: setters/getters
}
