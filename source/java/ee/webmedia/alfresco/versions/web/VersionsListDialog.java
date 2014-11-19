package ee.webmedia.alfresco.versions.web;

import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

<<<<<<< HEAD
import org.alfresco.service.cmr.lock.NodeLockedException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.app.AlfrescoNavigationHandler;
=======
import org.alfresco.service.cmr.repository.NodeRef;
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.common.service.GeneralService;
<<<<<<< HEAD
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.utils.WebUtil;
=======
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
import ee.webmedia.alfresco.versions.model.Version;
import ee.webmedia.alfresco.versions.service.VersionsService;

public class VersionsListDialog extends BaseDialogBean {

    private static final long serialVersionUID = 1L;
    private static final String PARAM_SELECT_NODEREF = "nodeRef";
    private static final String PARAM_SELECT_FILENAME = "fileName";
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
        return MessageUtil.getMessage(FacesContext.getCurrentInstance(), "versions_title", new Object[] { fileName });
    }

    /**
     * JSP event handler.
     * 
     * @param event
     */
    public void select(ActionEvent event) {
        NodeRef nodeRef = new NodeRef(ActionUtil.getParam(event, PARAM_SELECT_NODEREF));
        fileName = ActionUtil.getParam(event, PARAM_SELECT_FILENAME);
        versions = loadVersions(nodeRef);
    }

<<<<<<< HEAD
    public void activateVersion(ActionEvent event) {
        try {
            versionsService.activateVersion(new NodeRef(ActionUtil.getParam(event, PARAM_SELECT_NODEREF)));
            MessageUtil.addInfoMessage("version_activated");
            WebUtil.navigateTo(AlfrescoNavigationHandler.CLOSE_DIALOG_OUTCOME);
        } catch (NodeLockedException e) {
            BeanHelper.getDocumentLockHelperBean().handleLockedNode("version_source_file_locked", e.getNodeRef());
        } catch (UnableToPerformException u) {
            MessageUtil.addStatusMessage(u);
        }
    }

    // START: private methods
    private List<Version> loadVersions(NodeRef nodeRef) {
        return getVersionsService().getAllVersions(nodeRef);
=======
    // START: private methods
    private List<Version> loadVersions(NodeRef nodeRef) {
        return getVersionsService().getAllVersions(nodeRef, fileName);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
