package ee.webmedia.alfresco.document.file.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getWorkflowService;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.document.model.DocumentCommonModel.Privileges;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;

/**
 * @author Ats Uiboupin
 */
public class AddFilesEvaluator extends BaseActionEvaluator {

    private static final long serialVersionUID = 778943700049418316L;

    @Override
    public boolean evaluate(Node docNode) {
        return docNode.hasPermission(Privileges.EDIT_DOCUMENT)
                && !Boolean.TRUE.equals(docNode.getProperties().get(DocumentSpecificModel.Props.NOT_EDITABLE))
                && !getWorkflowService().hasInprogressCompoundWorkflows(docNode.getNodeRef());
    }

    @Override
    public boolean evaluate(Object obj) {
        throw new RuntimeException("Unimplemented");
    }
}
