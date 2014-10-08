package ee.webmedia.alfresco.casefile.web.evaluator;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.functions.model.FunctionsModel;
import ee.webmedia.alfresco.privilege.model.Privilege;

public class CaseFileSendForInformationEvaluator extends BaseActionEvaluator {

    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Node caseFileNode) {
        boolean hasEditCaseFilePrivilege = caseFileNode.hasPermission(Privilege.EDIT_CASE_FILE);
        if (!hasEditCaseFilePrivilege) {
            return false;
        }
        boolean isDraft = Boolean.TRUE.equals(caseFileNode.getProperties().get(DocumentService.TransientProps.TEMP_DOCUMENT_IS_DRAFT_QNAME));
        if (isDraft) {
            return false;
        }
        NodeRef functionNodeRef = (NodeRef) caseFileNode.getProperties().get(DocumentCommonModel.Props.FUNCTION);
        Boolean docActivitiesAreLimited = functionNodeRef == null ? Boolean.TRUE :
            (Boolean) BeanHelper.getNodeService().getProperty(functionNodeRef, FunctionsModel.Props.DOCUMENT_ACTIVITIES_ARE_LIMITED);
        return (docActivitiesAreLimited == null || !(docActivitiesAreLimited)) && !isDraft;
    }

}
