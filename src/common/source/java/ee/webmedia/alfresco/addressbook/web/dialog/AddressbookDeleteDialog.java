package ee.webmedia.alfresco.addressbook.web.dialog;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.utils.ActionUtil;

/**
 * @author Keit Tehvan
 */
public class AddressbookDeleteDialog extends AddressbookBaseDialog {
    private static final long serialVersionUID = 1L;

    public static String BEAN_NAME = "addressbookDeleteDialog";

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Exception {
        getAddressbookService().deleteNode(getCurrentNode().getNodeRef());
        reset();
        return outcome;
    }

    public void setupDelete(ActionEvent event) {
        setCurrentNode(getAddressbookService().getNode(new NodeRef(ActionUtil.getParam(event, "nodeRef"))));
    }

}
