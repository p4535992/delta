package ee.webmedia.alfresco.cases.web;

import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.TransientNode;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.cases.model.Case;
import ee.webmedia.alfresco.cases.service.CaseService;
import ee.webmedia.alfresco.utils.ActionUtil;

/**
 * Form backing component for cases details page
 * 
 * @author Ats Uiboupin
 */
public class CaseDetailsDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;

    private static final String PARAM_VOLUME_NODEREF = "volumeNodeRef";
    private static final String PARAM_CASE_NODEREF = "caseNodeRef";
    private transient CaseService caseService;
    private Case currentEntry;

    @Override
    public void init(Map<String, String> params) {
        super.init(params);
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        getCaseService().saveOrUpdate(currentEntry);
        resetFields();
        return outcome;
    }

    @Override
    public String cancel() {
        resetFields();
        return super.cancel();
    }

    // START: jsf actions/accessors
    public void showDetails(ActionEvent event) {
        String caseRef = ActionUtil.getParam(event, PARAM_CASE_NODEREF);
        currentEntry = getCaseService().getCaseByNoderef(caseRef);
        if(null==currentEntry) {
            throw new RuntimeException("Didn't find currentEntry");
        }
    }

    public void addNewCase(ActionEvent event) {
        NodeRef caseRef = new NodeRef(ActionUtil.getParam(event, PARAM_VOLUME_NODEREF));
        // create new node for currentEntry
        currentEntry = getCaseService().createCase(caseRef);
    }

    public Node getCurrentNode() {
        return currentEntry.getNode();
    }
    
    public String close() {
        if(currentEntry.getNode() instanceof TransientNode) {
            return null;
        }
        if (!isClosed()) {
            getCaseService().closeCase(currentEntry);
            return getDefaultFinishOutcome();
        }
        return null;
    }

    public boolean isClosed() {
        return caseService.isClosed(getCurrentNode());
    }

    // END: jsf actions/accessors

    private void resetFields() {
        currentEntry = null;
    }

    // START: getters / setters
    protected CaseService getCaseService() {
        if (caseService == null) {
            caseService = (CaseService) FacesContextUtils.getRequiredWebApplicationContext(//
                    FacesContext.getCurrentInstance()).getBean(CaseService.BEAN_NAME);
        }
        return caseService;
    }

    public void setCaseService(CaseService caseService) {
        this.caseService = caseService;
    }

    // END: getters / setters
}
