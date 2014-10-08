package ee.webmedia.alfresco.workflow.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getWorkflowService;

import java.util.Date;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.WebUtil;
import ee.webmedia.alfresco.workflow.model.RelatedUrl;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;

public class RelatedUrlDetailsDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "RelatedUrlDetailsDialog";

    private static final String PARAM_URL_NODEREF = "urlNodeRef";
    private static final String PARAM_URL_INDEX_IN_WORKFLOW = "urlIndexInWorkflow";

    private RelatedUrl relatedUrl;
    private transient UIPropertySheet propertySheet;

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        if (validate()) {
            NodeRef compoundWorkflowRef = BeanHelper.getCompoundWorkflowDialog().getWorkflow().getNodeRef();
            if (RepoUtil.isSaved(compoundWorkflowRef)) {
                getWorkflowService().saveRelatedUrl(relatedUrl, compoundWorkflowRef);
                resetFields();
                BeanHelper.getRelatedUrlListBlock().restored();
                BeanHelper.getCompoundWorkflowLogBlockBean().restore();
            } else if (relatedUrl.getIndexInWorkflow() == -1) {
                List<RelatedUrl> relatedUrls = BeanHelper.getRelatedUrlListBlock().getRelatedUrls();
                int indexInWorkflow = relatedUrls.isEmpty() ? 0 : relatedUrls.get(relatedUrls.size() - 1).getIndexInWorkflow() + 1;
                relatedUrl.setIndexInWorkflow(indexInWorkflow);
                relatedUrls.add(relatedUrl);
            }
            MessageUtil.addInfoMessage("save_success");
            return outcome;
        }
        isFinished = false;
        return null;
    }

    private boolean validate() {
        String url = relatedUrl.getUrl();
        if (WebUtil.isNotValidUrl(url)) {
            MessageUtil.addErrorMessage("compoundWorkflow_relatedUrl_not_url");
            return false;
        }
        return true;
    }

    @Override
    public String cancel() {
        resetFields();
        return super.cancel();
    }

    public void showDetails(ActionEvent event) {
        resetFields();
        NodeRef relatedUrlNodeRef = ActionUtil.getParam(event, PARAM_URL_NODEREF, NodeRef.class);
        if (RepoUtil.isSaved(relatedUrlNodeRef)) {
            relatedUrl = getWorkflowService().getRelatedUrl(relatedUrlNodeRef);
        } else {
            Integer relatedUrlIndexInWorkflow = ActionUtil.getParam(event, PARAM_URL_INDEX_IN_WORKFLOW, Integer.class);
            for (RelatedUrl url : BeanHelper.getRelatedUrlListBlock().getRelatedUrls()) {
                if (url.getIndexInWorkflow() == relatedUrlIndexInWorkflow) {
                    relatedUrl = url;
                    return;
                }
            }
        }
    }

    public void addRelatedUrl(ActionEvent event) {
        relatedUrl = new RelatedUrl(new WmNode(RepoUtil.createNewUnsavedNodeRef(), WorkflowCommonModel.Types.RELATED_URL));
        relatedUrl.setCreated(new Date());
        relatedUrl.setUrlCreatorName(BeanHelper.getUserService().getUserFullName());
    }

    public Node getRelatedUrl() {
        return relatedUrl.getNode();
    }

    @Override
    public Object getActionsContext() {
        return relatedUrl;
    }

    private void resetFields() {
        relatedUrl = null;
        propertySheet = null;
    }

    // START: getters / setters

    public void setPropertySheet(UIPropertySheet propertySheet) {
        this.propertySheet = propertySheet;
    }

    public UIPropertySheet getPropertySheet() {
        return propertySheet;
    }
    // END: getters / setters
}
