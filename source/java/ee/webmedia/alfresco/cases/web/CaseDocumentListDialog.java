package ee.webmedia.alfresco.cases.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getLogService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getUserService;

import java.util.ArrayList;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.apache.commons.collections4.CollectionUtils;

import ee.webmedia.alfresco.cases.service.UnmodifiableCase;
import ee.webmedia.alfresco.classificator.enums.VolumeType;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docdynamic.web.DocumentDynamicDialog;
import ee.webmedia.alfresco.document.associations.web.AssocsBlockBean;
import ee.webmedia.alfresco.document.search.web.DocumentListDataProvider;
import ee.webmedia.alfresco.document.web.DocumentListDialog;
import ee.webmedia.alfresco.log.model.LogEntry;
import ee.webmedia.alfresco.log.model.LogObject;
import ee.webmedia.alfresco.menu.ui.MenuBean;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.WebUtil;
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.volume.web.VolumeListDialog;

/**
 * Form backing component for cases list page
 */
public class CaseDocumentListDialog extends DocumentListDialog {
    private static final long serialVersionUID = 1L;

    public static final String BEAN_NAME = "CaseDocumentListDialog";
    public static final String DIALOG_NAME = "CaseDocListDialog";

    private Volume parent;
    private List<UnmodifiableCase> cases;
    private boolean volumeRefInvalid;

    @Override
    public void init(NodeRef volumeRef) {
        showAll(volumeRef);
        super.init(volumeRef, false);
        WebUtil.navigateTo(AlfrescoNavigationHandler.DIALOG_PREFIX + "caseDocListDialog");
    }

    @Override
    public String action() {
        String dialogPrefix = AlfrescoNavigationHandler.DIALOG_PREFIX;
        boolean tempState = volumeRefInvalid;
        volumeRefInvalid = false;
        return dialogPrefix + (tempState ? VolumeListDialog.DIALOG_NAME : "caseDetailsDialog");
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        resetFields();
        return outcome;
    }

    public String getCaseListTitle() {
        return MessageUtil.getMessage("document_case_list_title", parent != null ? parent.getVolumeMark() : "", parent != null ? parent.getTitle() : "");
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
        return parent != null ? parent.getNode() : null;
    }

    // START: jsf actions/accessors
    public void showAll(ActionEvent event) {
        NodeRef volumeRef = new NodeRef(ActionUtil.getParam(event, "volumeNodeRef"));
        if (!nodeExists(volumeRef)) {
            volumeRefInvalid = true;
            MessageUtil.addInfoMessage("volume_noderef_not_found");
            return;
        }
        super.setup(event, false);
        showAll(volumeRef);
    }

    public void showAllFromShortcut(ActionEvent event) {
        MenuBean.clearViewStack(String.valueOf(MenuBean.MY_TASKS_AND_DOCUMENTS_ID), null);
        NodeRef volumeRef = new NodeRef(ActionUtil.getParam(event, "nodeRef"));
        if (DocumentDynamicDialog.validateExists(volumeRef)) {
            ActionUtil.getParams(event).put("volumeNodeRef", volumeRef.toString());
            showAll(event);
        }
    }

    public boolean isShowAssocsBlock() {
        return parent != null && VolumeType.SUBJECT_FILE.equals(parent.getVolumeTypeEnum()) && CollectionUtils.size(BeanHelper.getAssocsBlockBean().getDocAssocInfos()) > 0;
    }

    public boolean isShowAddAssocsLink() {
        return false;
    }

    public boolean isAssocsBlockExpanded() {
        return true;
    }

    private void showAll(NodeRef volumeRef) {
        parent = getVolumeService().getVolumeByNodeRef(volumeRef, null);
        getLogService().addLogEntry(LogEntry.create(LogObject.VOLUME, getUserService(), volumeRef, "applog_space_open", parent.getVolumeMark(), parent.getTitle()));
        initAssocsBlock();
    }

    private void initAssocsBlock() {
        if (parent != null) {
            AssocsBlockBean assocsBlockBean = BeanHelper.getAssocsBlockBean();
            assocsBlockBean.init(parent.getNode());
            assocsBlockBean.disableDelete();
        }
    }

    public List<UnmodifiableCase> getCases() {
        if (parent != null) {
            cases = getCaseService().getAllCasesByVolume(parent.getNode().getNodeRef());
        } else {
            cases = new ArrayList<UnmodifiableCase>();
        }
        return cases;
    }

    @Override
    public void restored() {
        super.restored();
        doInitialSearch();
        initAssocsBlock();
    }

    @Override
    public DocumentListDataProvider getDocuments() {
        if (documentProvider == null) {
            documentProvider = new DocumentListDataProvider(new ArrayList<NodeRef>());
        }
        return documentProvider;
    }

    @Override
    public void clean() {
        super.clean();
        parent = null;
        cases = null;
    }

    public void resetCases() {
        cases = null;
    }

    public Volume getParent() {
        return parent;
    }

    // END: jsf actions/accessors

    private void resetFields() {
        parent = null;
        cases = null;
    }

}
