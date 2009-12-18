package ee.webmedia.alfresco.addressbook.web.wizard;

import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel.Types;
import ee.webmedia.alfresco.utils.ActionUtil;

/**
 * @author Keit Tehvan
 */
public class AddOrgPersonWizard extends AddressbookEntryWizard {
    private static final long serialVersionUID = 1L;

    private NodeRef currentOrg = null;

    @Override
    protected String saveData(FacesContext context, String outcome) {
        getAddressbookService().addOrUpdateNode(getEntry(), getCurrentOrg());
        return outcome;
    }

    public NodeRef getCurrentOrg() {
        return currentOrg;
    }

    public void setCurrentOrg(NodeRef currentOrg) {
        this.currentOrg = currentOrg;
    }

    public void setupEntryOrg(ActionEvent event) {
        setCurrentOrg(new NodeRef(ActionUtil.getParam(event, "org")));
    }

    @Override
    public void init(Map<String, String> params) {
        setupEntry(Types.ORGPERSON);
        super.init(params);
    }

}
