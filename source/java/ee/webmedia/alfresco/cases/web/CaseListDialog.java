package ee.webmedia.alfresco.cases.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getLogService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getUserService;

import java.util.ArrayList;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.cases.model.Case;
import ee.webmedia.alfresco.cases.service.CaseService;
import ee.webmedia.alfresco.log.model.LogEntry;
import ee.webmedia.alfresco.log.model.LogObject;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.WebUtil;
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.volume.service.VolumeService;
import ee.webmedia.alfresco.volume.web.VolumeListDialog;

/**
 * Form backing component for cases list page
 */
public class CaseListDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;

    public static final String BEAN_NAME = "CaseListDialog";

    private transient VolumeService volumeService;
    private transient CaseService caseService;
    private boolean volumeRefInvalid;
    private Volume parent;

    public void init(NodeRef volumeRef) {
        showAll(volumeRef);
        WebUtil.navigateTo(AlfrescoNavigationHandler.DIALOG_PREFIX + "caseListDialog");
    }

    public String action() {
        String dialogPrefix = AlfrescoNavigationHandler.DIALOG_PREFIX;
        boolean tempState = volumeRefInvalid;
        volumeRefInvalid = false;
        return dialogPrefix + (tempState ? VolumeListDialog.DIALOG_NAME : "caseListDialog");
    }

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
        NodeRef volumeRef = new NodeRef(ActionUtil.getParam(event, "volumeNodeRef"));
        if (!nodeExists(volumeRef)) {
            volumeRefInvalid = true;
            MessageUtil.addInfoMessage("volume_noderef_not_found");
            return;
        }
        showAll(volumeRef);
    }

    private void showAll(NodeRef volumeRef) {
        parent = getVolumeService().getVolumeByNodeRef(volumeRef);
        getLogService().addLogEntry(LogEntry.create(LogObject.VOLUME, getUserService(), volumeRef, "applog_space_open", parent.getVolumeMark(), parent.getTitle()));
    }

    public List<Case> getEntries() {
        List<Case> cases;
        try {
            cases = getCaseService().getAllCasesByVolume(parent.getNode().getNodeRef());
        } catch (Exception e) {
            cases = new ArrayList<Case>();
        }
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
