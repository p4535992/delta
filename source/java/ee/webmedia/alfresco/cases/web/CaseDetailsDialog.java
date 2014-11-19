package ee.webmedia.alfresco.cases.web;

<<<<<<< HEAD
import static ee.webmedia.alfresco.common.web.BeanHelper.getCaseService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getVolumeService;

=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
<<<<<<< HEAD
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.MapNode;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.TransientNode;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.cases.model.Case;
import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;

/**
 * Form backing component for cases details page
 * 
 * @author Ats Uiboupin
=======
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.TransientNode;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.cases.model.Case;
import ee.webmedia.alfresco.cases.service.CaseService;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.volume.web.VolumeListDialog;

/**
 * Form backing component for cases details page
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
 */
public class CaseDetailsDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "CaseDetailsDialog";

    private static final String PARAM_VOLUME_NODEREF = "volumeNodeRef";
<<<<<<< HEAD
    public static final String PARAM_CASE_NODEREF = "caseNodeRef";

    private Case currentEntry;
    private boolean newCase;
    private transient UIPropertySheet propertySheet;
=======
    private static final String PARAM_CASE_NODEREF = "caseNodeRef";

    private transient CaseService caseService;
    private Case currentEntry;
    private boolean newCase;
    private boolean caseRefInvalid;
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5

    @Override
    public void init(Map<String, String> params) {
        super.init(params);
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        try {
            getCaseService().saveOrUpdate(currentEntry);
            resetFields();
            MessageUtil.addInfoMessage("save_success");
        } catch (UnableToPerformException e) {
            MessageUtil.addStatusMessage(context, e);
            outcome = null;
            isFinished = false;
        }
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
<<<<<<< HEAD
=======
        if (!nodeExists(new NodeRef(caseRef))) {
            MessageUtil.addInfoMessage("volume_noderef_not_found");
            caseRefInvalid = true;
            return;
        }
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
        currentEntry = getCaseService().getCaseByNoderef(caseRef);
        if (null == currentEntry) {
            throw new RuntimeException("Didn't find currentEntry");
        }
<<<<<<< HEAD
        propertySheet = null;
=======
    }

    public String action() {
        String dialogPrefix = AlfrescoNavigationHandler.DIALOG_PREFIX;
        boolean tempState = caseRefInvalid;
        caseRefInvalid = false;
        return dialogPrefix + (tempState ? VolumeListDialog.DIALOG_NAME : "caseDetailsDialog");
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    }

    public void addNewCase(ActionEvent event) {
        newCase = true;
        NodeRef caseRef = new NodeRef(ActionUtil.getParam(event, PARAM_VOLUME_NODEREF));
        // create new node for currentEntry
        currentEntry = getCaseService().createCase(caseRef);
<<<<<<< HEAD
        propertySheet = null;
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    }

    public Node getCurrentNode() {
        return currentEntry.getNode();
    }

    public String close() {
        if (currentEntry.getNode() instanceof TransientNode) {
            return null;
        }
        if (!isClosed()) {
<<<<<<< HEAD
            propertySheet.getChildren().clear();
            if (StringUtils.isBlank((String) getCurrentNode().getProperties().get(CaseModel.Props.TITLE))) {
                MessageUtil.addInfoMessage("case_error_mandatory_title");
                return null;
            }
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
            getCaseService().closeCase(currentEntry);
            MessageUtil.addInfoMessage("case_close_success");
            return getDefaultFinishOutcome();
        }
        return null;
    }

<<<<<<< HEAD
    public String open() {
        if (currentEntry.getNode() instanceof TransientNode) {
            return null;
        }
        if (isClosed()) {
            propertySheet.getChildren().clear();
            if (getVolumeService().isClosed(new MapNode(currentEntry.getVolumeNodeRef()))) {
                MessageUtil.addInfoMessage("case_open_volume_closed");
                return null;
            }
            getCaseService().openCase(currentEntry);
            MessageUtil.addInfoMessage("case_open_success");
            return getDefaultFinishOutcome();
        }
        return null;
    }

    public String delete() {
        if (currentEntry.getNode() instanceof TransientNode) {
            return null;
        }
        if (isClosed()) {
            try {
                getCaseService().delete(currentEntry);
                MessageUtil.addInfoMessage("case_deleted");
                return getDefaultFinishOutcome();
            } catch (UnableToPerformException e) {
                MessageUtil.addStatusMessage(e);
            }
        }
        return null;
    }

    public boolean isClosed() {
        return getCaseService().isClosed(getCurrentNode());
=======
    public boolean isClosed() {
        return caseService.isClosed(getCurrentNode());
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    }

    public boolean isNew() {
        return newCase;
    }

    // END: jsf actions/accessors

    private void resetFields() {
        currentEntry = null;
        newCase = false;
    }

<<<<<<< HEAD
    public void setPropertySheet(UIPropertySheet propertySheet) {
        this.propertySheet = propertySheet;
    }

    public UIPropertySheet getPropertySheet() {
        return propertySheet;
    }

=======
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
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
}
