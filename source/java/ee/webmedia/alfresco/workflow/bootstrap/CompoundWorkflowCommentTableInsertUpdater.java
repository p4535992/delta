package ee.webmedia.alfresco.workflow.bootstrap;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.workflow.model.Comment;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.service.WorkflowService;

/**
 * Move compound workflow comment from property to delta_compound_workflow_comment table
 */
public class CompoundWorkflowCommentTableInsertUpdater extends AbstractNodeUpdater {

    private static final String COMMENT_CREATOR_DHS = "DHS";

    private WorkflowService workflowService;
    private Date now;

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        now = new Date();
    }

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.generateAndNotQuery(SearchUtil.generateTypeQuery(WorkflowCommonModel.Types.COMPOUND_WORKFLOW),
                SearchUtil.generateTypeQuery(WorkflowCommonModel.Types.COMPOUND_WORKFLOW_DEFINITION));
        List<ResultSet> result = new ArrayList<ResultSet>(6);
        for (StoreRef storeRef : generalService.getAllStoreRefsWithTrashCan()) {
            result.add(searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, query));
        }
        return result;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        String compoundWorkflowComment = (String) nodeService.getProperty(nodeRef, WorkflowCommonModel.Props.COMMENT);
        if (StringUtils.isBlank(compoundWorkflowComment)) {
            return new String[] { "comment is empty, no update needed" };
        }
        String systemUserName = AuthenticationUtil.SYSTEM_USER_NAME;
        Comment comment = new Comment(nodeRef.getId(), now, COMMENT_CREATOR_DHS, COMMENT_CREATOR_DHS, compoundWorkflowComment);
        workflowService.addCompoundWorkflowComment(comment);
        return new String[] { "updated" };
    }

    public void setWorkflowService(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }
}
