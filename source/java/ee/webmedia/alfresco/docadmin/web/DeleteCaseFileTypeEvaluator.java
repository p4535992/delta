package ee.webmedia.alfresco.docadmin.web;

import static ee.webmedia.alfresco.docadmin.web.DocTypeManagePrivilegesEvaluator.isDynTypeSavedAndShowingLatestVersion;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.base.BaseObject;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.service.CaseFileType;
import ee.webmedia.alfresco.docadmin.service.DocumentType;

/**
 * UI action evaluator that decides whether {@link CaseFileType} can be deleted
<<<<<<< HEAD
 * 
 * @author Ats Uiboupin
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
 */
public class DeleteCaseFileTypeEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 0L;

    @Override
    public boolean evaluate(Object obj) {
        return evaluate((CaseFileType) obj);
    }

    @Override
    public boolean evaluate(Node docTypeNode) {
        DocumentType documentType = BaseObject.reconstruct((WmNode) docTypeNode, DocumentType.class);
        return evaluate(documentType);
    }

    private boolean evaluate(CaseFileType caseFileType) {
        return isDynTypeSavedAndShowingLatestVersion(caseFileType) && !BeanHelper.getDocumentAdminService().isCaseFileTypeUsed(caseFileType.getId());
    }

}
