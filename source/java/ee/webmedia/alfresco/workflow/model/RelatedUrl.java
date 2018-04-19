package ee.webmedia.alfresco.workflow.model;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.model.NodeBaseVO;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.workflow.service.Task;

public class RelatedUrl extends NodeBaseVO {

    public static final String TARGET_BLANK = "_blank";
    public static final String TARGET_SELF = "_self";
    private static final long serialVersionUID = 1L;
    private String target = TARGET_BLANK;
    private final Map<String, Object> originalProps;
    // used to index unsaved relatedUrls in unsaved compound workflow
    private int indexInWorkflow = -1;

    public RelatedUrl(WmNode wmNode) {
        Assert.notNull(wmNode);
        node = wmNode;
        originalProps = new HashMap<String, Object>();
        for (Map.Entry<String, Object> entry : wmNode.getProperties().entrySet()) {
            originalProps.put(entry.getKey(), entry.getValue());
        }
    }

    public String getUrl() {
        return getProp(WorkflowCommonModel.Props.URL);
    }

    public void setUrl(String url) {
        setProp(WorkflowCommonModel.Props.URL, url);
    }

    public String getUrlCondenced() {
        return StringUtils.substring(getUrl(), 0, 150);
    }

    public String getUrlComment() {
        return getProp(WorkflowCommonModel.Props.URL_COMMENT);
    }

    public void setUrlComment(String urlComment) {
        setProp(WorkflowCommonModel.Props.URL_COMMENT, urlComment);
    }

    public String getUrlCreatorName() {
        return getProp(WorkflowCommonModel.Props.URL_CREATOR_NAME);
    }

    public void setUrlCreatorName(String urlCreatorName) {
        setProp(WorkflowCommonModel.Props.URL_CREATOR_NAME, urlCreatorName);
    }

    public Date getCreated() {
        return (Date) getNode().getProperties().get(WorkflowCommonModel.Props.CREATED);
    }

    public void setCreated(Date created) {
        setProp(WorkflowCommonModel.Props.CREATED, created);
    }

    public String getCreatedStr() {
        Date created = getCreated();
        return created != null ? Task.dateTimeFormat.format(created) : "";
    }

    public String getUrlModiferName() {
        return getProp(WorkflowCommonModel.Props.URL_MODIFIER_NAME);
    }

    public void setUrlModifierName(String urlModiferName) {
        setProp(WorkflowCommonModel.Props.URL_MODIFIER_NAME, urlModiferName);
    }

    public Date getModifed() {
        return (Date) getNode().getProperties().get(WorkflowCommonModel.Props.MODIFIED);
    }

    public void setModified(Date modified) {
        setProp(WorkflowCommonModel.Props.MODIFIED, modified);
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        Assert.notNull(target);
        this.target = target;
    }

    public Map<String, Object> getOriginalProps() {
        return originalProps;
    }

    public int getIndexInWorkflow() {
        return indexInWorkflow;
    }

    public void setIndexInWorkflow(int indexInWorkflow) {
        this.indexInWorkflow = indexInWorkflow;
    }

    @Override
    public boolean isUnsaved() {
        return RepoUtil.isUnsaved(node);
    }

}
