package ee.webmedia.alfresco.document.forum.web.evaluator;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentDialogHelperBean;

import java.util.List;

import javax.faces.context.FacesContext;

import org.alfresco.model.ForumModel;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.Repository;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.common.service.GeneralService;

/**
 * UI Action Evaluator - Discuss a node.
 * 
 * @author Kevin Roast
 */
public class DiscussNodeEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 1L;

    /**
     * @see org.alfresco.web.action.ActionEvaluator#evaluate(org.alfresco.web.bean.repository.Node)
     */
    @Override
    public boolean evaluate(Node node) {
        boolean result = false;
        GeneralService generalService = (GeneralService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance()).getBean(GeneralService.BEAN_NAME);
        node = generalService.fetchNode(node.getNodeRef()); // refresh the node, because dialog caches it, and sub dialogs change props/aspects

        if (node.hasAspect(ForumModel.ASPECT_DISCUSSABLE)) {
            NodeService nodeService = Repository.getServiceRegistry(
                    FacesContext.getCurrentInstance()).getNodeService();
            List<ChildAssociationRef> children = nodeService.getChildAssocs(
                    node.getNodeRef(), ForumModel.ASSOC_DISCUSSION,
                    RegexQNamePattern.MATCH_ALL);

            // make sure there is one visible child association for the node
            if (children.size() == 1) {
                result = true;
            }
        }

        return result && !getDocumentDialogHelperBean().isInEditMode();
    }
}