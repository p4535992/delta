package ee.webmedia.alfresco.workflow.web;

import static org.alfresco.web.bean.dialog.BaseDialogBean.hasPermission;

import java.io.Serializable;
import java.util.List;

import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.search.web.AbstractSearchBlockBean;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;

/**
 * @author Riina Tens
 */
public class CompoundWorkflowAssocSearchBlock extends AbstractSearchBlockBean implements Serializable {

    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "CompoundWorkflowAssocSearchBlock";

    @Override
    public String getSearchBlockTitle() {
        return MessageUtil.getMessage("compoundWorkflow_search_title");
    }

    @Override
    protected QName getDefaultAssocType() {
        return DocumentCommonModel.Assocs.WORKFLOW_DOCUMENT;
    }

    @Override
    public void addAssocDocHandler(ActionEvent event) {
        addAssocDocHandler(new NodeRef(ActionUtil.getParam(event, PARAM_NODEREF)));
    }

    public void addAssocDocHandler(NodeRef nodeRef) {
        if (!hasPermission(nodeRef, DocumentCommonModel.Privileges.VIEW_DOCUMENT_META_DATA)
                || !hasPermission(nodeRef, DocumentCommonModel.Privileges.VIEW_DOCUMENT_FILES)) {
            MessageUtil.addErrorMessage("compoundWorkflow_addAssoc_erro_no_permissions");
            return;
        }
        saveAssocNow(nodeRef, sourceObjectRef, getDefaultAssocType());
    }

    @Override
    protected void createAssoc(final NodeRef sourceRef, final NodeRef targetRef, final QName assocType) {
        boolean workflowUpdated = false;
        if (targetRef != null && RepoUtil.isSaved(targetRef)) {
            workflowUpdated = BeanHelper.getDocumentAssociationsService().createWorkflowAssoc(sourceRef, targetRef, true, true);
        } else {
            addUnsavedAssoc(sourceRef);
            workflowUpdated = BeanHelper.getCompoundWorkflowAssocListDialog().getNewAssocs().size() == 1;
        }
        if (workflowUpdated) {
            BeanHelper.getCompoundWorkflowDialog().updateCompoundWorkflowMainDoc(sourceRef, null);
        } else {
            BeanHelper.getCompoundWorkflowDialog().updateAssocRelatedBlocks();
        }
    }

    private void addUnsavedAssoc(NodeRef docRef) {
        BeanHelper.getCompoundWorkflowAssocListDialog().getNewAssocs().add(docRef);
    }

    @Override
    public List<NodeRef> getNewAssocs() {
        return BeanHelper.getCompoundWorkflowAssocListDialog().getNewAssocs();
    }

    @Override
    protected void doPostSave() {
        MessageUtil.addInfoMessage("compoundWorkflow_assocAdd_success");
    }

    @Override
    public String getActionColumnFileName() {
        return "/WEB-INF/classes/ee/webmedia/alfresco/workflow/web/compound-workflow-assocs-block-actions-column.jsp";
    }

    public void searchDocs(@SuppressWarnings("unused") ActionEvent event) {
        setExpanded(true);
    }

    @Override
    protected String getBeanName() {
        return BEAN_NAME;
    }

}
