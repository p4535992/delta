package ee.webmedia.alfresco.document.web.evaluator;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;

/**
 * UI action evaluator for validating whether user can end current document.
 * 
 * @author Romet Aidla
 */
public class EndDocumentEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 0L;

    @Override
    public boolean evaluate(Node node) {
        String regNumber = (String) node.getProperties().get(DocumentCommonModel.Props.REG_NUMBER.toString());
        if (StringUtils.isBlank(regNumber)) {
            return false;
        }
        boolean isWorking = DocumentStatus.WORKING.getValueName().equals(node.getProperties().get(DocumentCommonModel.Props.DOC_STATUS.toString()));
        return isWorking && new ViewStateActionEvaluator().evaluate(node) && ReopenDocumentEvaluator.hasUserRights(node);
    }
}
