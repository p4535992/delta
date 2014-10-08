<<<<<<< HEAD
package ee.webmedia.alfresco.workflow.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getWorkflowService;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.workflow.model.RelatedUrl;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;
import ee.webmedia.alfresco.workflow.service.WorkflowUtil;

/**
 * @author Riina Tens
 */
public class RelatedUrlListBlock extends BaseDialogBean {

    public static final String BEAN_NAME = "RelatedUrlListBlock";

    private static final long serialVersionUID = 1L;

    private CompoundWorkflow compoundWorkflow;
    private List<RelatedUrl> relatedUrls;

    @Override
    public Object getActionsContext() {
        return null;
    }

    @Override
    public void restored() {
        if (isSavedCompoundWorkflow()) {
            relatedUrls = BeanHelper.getWorkflowService().getRelatedUrls(compoundWorkflow.getNodeRef());
            String currentDeltaUrlPrefix = BeanHelper.getDocumentTemplateService().getServerUrl();
            if (StringUtils.isNotBlank(currentDeltaUrlPrefix)) {
                for (RelatedUrl relatedUrl : relatedUrls) {
                    String url = relatedUrl.getUrl();
                    if (StringUtils.isNotBlank(url) && url.startsWith(currentDeltaUrlPrefix)) {
                        relatedUrl.setTarget(RelatedUrl.TARGET_SELF);
                    }
                }
            }
        } else {
            relatedUrls = new ArrayList<RelatedUrl>();
        }
    }

    private boolean isSavedCompoundWorkflow() {
        return compoundWorkflow != null && compoundWorkflow.isSaved();
    }

    @Override
    public void init(Map<String, String> params) {
        super.init(params);
        restored();
    }

    public void setup(CompoundWorkflow compoundWorkflow) {
        this.compoundWorkflow = compoundWorkflow;
        restored();
    }

    @Override
    public String cancel() {
        relatedUrls = null;
        return super.cancel();
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {

        return null;
    }

    /**
     * Getter for JSP.
     */
    public List<RelatedUrl> getRelatedUrls() {
        return relatedUrls;
    }

    public void delete(ActionEvent event) {
        if (!isSavedCompoundWorkflow()) {
            Integer relatedUrlIndexInWorkflow = ActionUtil.getParam(event, "urlIndexInWorkflow", Integer.class);
            for (Iterator<RelatedUrl> i = relatedUrls.iterator(); i.hasNext();) {
                RelatedUrl relatedUrl = i.next();
                if (relatedUrl.getIndexInWorkflow() == relatedUrlIndexInWorkflow) {
                    i.remove();
                    MessageUtil.addInfoMessage("compoundWorkflow_relatedUrl_delete_success");
                    return;
                }
            }
        } else {
            NodeRef relatedUrlNodeRef = ActionUtil.getParam(event, "nodeRef", NodeRef.class);
            getWorkflowService().deleteRelatedUrl(relatedUrlNodeRef);
            restored();
            BeanHelper.getCompoundWorkflowLogBlockBean().restore();
            MessageUtil.addInfoMessage("compoundWorkflow_relatedUrl_delete_success");
        }
    }

    public String getListTitle() {
        return MessageUtil.getMessage("compoundWorkflow_relatedUrl_listTitle");
    }

    public boolean isShowLinkActions() {
        if (compoundWorkflow == null) {
            return false;
        }
        String runAsUser = AuthenticationUtil.getRunAsUser();
        return runAsUser.equals(compoundWorkflow.getOwnerId())
                || BeanHelper.getUserService().isDocumentManager()
                || WorkflowUtil.isOwnerOfInProgressTask(compoundWorkflow, runAsUser);
    }

    public boolean isExpanded() {
        return relatedUrls != null && !relatedUrls.isEmpty();
    }
}
=======
package ee.webmedia.alfresco.workflow.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getWorkflowService;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.dialog.BaseDialogBean;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.WebUtil;
import ee.webmedia.alfresco.workflow.model.RelatedUrl;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;
import ee.webmedia.alfresco.workflow.service.WorkflowUtil;

public class RelatedUrlListBlock extends BaseDialogBean {

    public static final String BEAN_NAME = "RelatedUrlListBlock";

    private static final long serialVersionUID = 1L;

    private CompoundWorkflow compoundWorkflow;
    private List<RelatedUrl> relatedUrls;

    @Override
    public Object getActionsContext() {
        return null;
    }

    @Override
    public void restored() {
        if (isSavedCompoundWorkflow()) {
            relatedUrls = BeanHelper.getWorkflowService().getRelatedUrls(compoundWorkflow.getNodeRef());
            WebUtil.toggleSystemUrlTarget(BeanHelper.getDocumentTemplateService().getServerUrl(), relatedUrls);
        } else {
            relatedUrls = new ArrayList<RelatedUrl>();
        }
    }

    private boolean isSavedCompoundWorkflow() {
        return compoundWorkflow != null && compoundWorkflow.isSaved();
    }

    @Override
    public void init(Map<String, String> params) {
        super.init(params);
        restored();
    }

    public void setup(CompoundWorkflow compoundWorkflow) {
        this.compoundWorkflow = compoundWorkflow;
        restored();
    }

    @Override
    public String cancel() {
        relatedUrls = null;
        return super.cancel();
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {

        return null;
    }

    /**
     * Getter for JSP.
     */
    public List<RelatedUrl> getRelatedUrls() {
        return relatedUrls;
    }

    public void delete(ActionEvent event) {
        if (!isSavedCompoundWorkflow()) {
            Integer relatedUrlIndexInWorkflow = ActionUtil.getParam(event, "urlIndexInWorkflow", Integer.class);
            for (Iterator<RelatedUrl> i = relatedUrls.iterator(); i.hasNext();) {
                RelatedUrl relatedUrl = i.next();
                if (relatedUrl.getIndexInWorkflow() == relatedUrlIndexInWorkflow) {
                    i.remove();
                    MessageUtil.addInfoMessage("compoundWorkflow_relatedUrl_delete_success");
                    return;
                }
            }
        } else {
            NodeRef relatedUrlNodeRef = ActionUtil.getParam(event, "nodeRef", NodeRef.class);
            getWorkflowService().deleteRelatedUrl(relatedUrlNodeRef);
            restored();
            BeanHelper.getCompoundWorkflowLogBlockBean().restore();
            MessageUtil.addInfoMessage("compoundWorkflow_relatedUrl_delete_success");
        }
    }

    public String getListTitle() {
        return MessageUtil.getMessage("compoundWorkflow_relatedUrl_listTitle");
    }

    public boolean isShowLinkActions() {
        if (compoundWorkflow == null) {
            return false;
        }
        String runAsUser = AuthenticationUtil.getRunAsUser();
        return runAsUser.equals(compoundWorkflow.getOwnerId())
                || BeanHelper.getUserService().isDocumentManager()
                || WorkflowUtil.isOwnerOfInProgressTask(compoundWorkflow, runAsUser);
    }

    public boolean isExpanded() {
        return relatedUrls != null && !relatedUrls.isEmpty();
    }
}
>>>>>>> develop-5.1
