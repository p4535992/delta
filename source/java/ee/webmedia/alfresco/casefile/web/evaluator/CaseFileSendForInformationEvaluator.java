package ee.webmedia.alfresco.casefile.web.evaluator;

import java.io.Serializable;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.functions.model.FunctionsModel;

/**
 * @author Marti Laast
 */
public class CaseFileSendForInformationEvaluator extends BaseActionEvaluator {

    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Node caseFileNode) {
        boolean hasEditCaseFilePrivilege = caseFileNode.hasPermissions(DocumentCommonModel.Privileges.EDIT_CASE_FILE);
        if (!hasEditCaseFilePrivilege) {
            return false;
        }
        NodeRef functionNodeRef = (NodeRef) caseFileNode.getProperties().get(DocumentCommonModel.Props.FUNCTION);
        Serializable docActivitiesAreLimited = BeanHelper.getNodeService().getProperty(functionNodeRef, FunctionsModel.Props.DOCUMENT_ACTIVITIES_ARE_LIMITED);
        return docActivitiesAreLimited == null || !((Boolean) docActivitiesAreLimited);
    }

}
