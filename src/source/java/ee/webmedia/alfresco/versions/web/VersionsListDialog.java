package ee.webmedia.alfresco.versions.web;

import java.text.MessageFormat;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.app.Application;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.versions.model.Version;
import ee.webmedia.alfresco.versions.service.VersionsService;

public class VersionsListDialog extends BaseDialogBean {

    private static final long serialVersionUID = 1L;
    private transient VersionsService versionsService;
    private transient GeneralService generalService;
    private List<Version> versions;
    private String fileName;

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        // save button not used or shown
        return null;
    }

    @Override
    public String cancel() {
        versions = null;
        return super.cancel();
    }

    @Override
    public String getContainerTitle() {
        return MessageFormat.format(Application.getMessage(FacesContext.getCurrentInstance(), "versions_title"), new Object[] { fileName });
    }

    @Override
    public String getCancelButtonLabel() {
        return Application.getMessage(FacesContext.getCurrentInstance(), "close");
    }

    /**
     * JSP event handler.
     * 
     * @param event
     */
    public void select(ActionEvent event) {
        NodeRef nodeRef = new NodeRef(ActionUtil.getParam(event, "ref"));
        fileName = ActionUtil.getParam(event, "name");
        versions = loadVersions(nodeRef);
    }

    // START: private methods
    private List<Version> loadVersions(NodeRef nodeRef) {
        return getVersionsService().getAllVersions(nodeRef, fileName);
    }

    // END: private methods

    // START: getters / setters
    /**
     * Used in JSP pages.
     */
    public List<Version> getVersions() {
        return versions;
    }

    protected GeneralService getGeneralService() {
        if (generalService == null) {
            generalService = (GeneralService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
                    .getBean(GeneralService.BEAN_NAME);
        }
        return generalService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public VersionsService getVersionsService() {
        if (versionsService == null) {
            versionsService = (VersionsService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
                    .getBean(VersionsService.BEAN_NAME);
        }
        return versionsService;
    }

    public void setVersionsService(VersionsService versionsService) {
        this.versionsService = versionsService;
    }

    // END: getters / setters
}
