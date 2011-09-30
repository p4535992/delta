package ee.webmedia.alfresco.cases.web;

import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.cases.model.Case;
import ee.webmedia.alfresco.cases.service.CaseService;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.volume.service.VolumeService;

/**
 * Form backing component for cases list page
 * 
 * @author Ats Uiboupin
 */
public class CaseListDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;
    private transient VolumeService volumeService;
    private transient CaseService caseService;
    private Volume parent;

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        resetFields();
        return outcome;
    }

    @Override
    public boolean isFinishButtonVisible(boolean dialogConfOKButtonVisible) {
        return false;
    }

    @Override
    public String cancel() {
        resetFields();
        return super.cancel();
    }

    @Override
    public Object getActionsContext() {
        return parent.getNode();
    }

    // START: jsf actions/accessors
    public void showAll(ActionEvent event) {
        parent = getVolumeService().getVolumeByNodeRef((ActionUtil.getParam(event, "volumeNodeRef")));
    }

    public List<Case> getEntries() {
        final List<Case> cases = getCaseService().getAllCasesByVolume(parent.getNode().getNodeRef());
        return cases;
    }

    public Volume getParent() {
        return parent;
    }

    // END: jsf actions/accessors

    private void resetFields() {
        parent = null;

    }

    // START: getters / setters
    public void setCaseService(CaseService caseService) {
        this.caseService = caseService;
    }

    protected CaseService getCaseService() {
        if (caseService == null) {
            caseService = (CaseService) FacesContextUtils.getRequiredWebApplicationContext(//
                    FacesContext.getCurrentInstance()).getBean(CaseService.BEAN_NAME);
        }
        return caseService;
    }

    public void setVolumeService(VolumeService volumeService) {
        this.volumeService = volumeService;
    }

    protected VolumeService getVolumeService() {
        if (volumeService == null) {
            volumeService = (VolumeService) FacesContextUtils.getRequiredWebApplicationContext( //
                    FacesContext.getCurrentInstance()).getBean(VolumeService.BEAN_NAME);
        }
        return volumeService;
    }
    // END: getters / setters

}
