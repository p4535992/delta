package ee.webmedia.alfresco.docadmin.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAdminService;
import static ee.webmedia.alfresco.docadmin.web.DocTypeManagePrivilegesEvaluator.isDynTypeSavedAndShowingLatestVersion;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.base.BaseObject;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.service.DocumentType;

/**
 * UI action evaluator that decides whether {@link DocumentType} can be deleted
 * 
 * @author Ats Uiboupin
 */
public class DeleteDocumentTypeEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 0L;

    @Override
    public boolean evaluate(Object obj) {
        return evaluate((DocumentType) obj);
    }

    @Override
    public boolean evaluate(Node docTypeNode) {
        DocumentType documentType = BaseObject.reconstruct((WmNode) docTypeNode, DocumentType.class);
        return evaluate(documentType);
    }

    private boolean evaluate(DocumentType documentType) {
        return isDynTypeSavedAndShowingLatestVersion(documentType) && !documentType.isSystematic()
                && !getDocumentAdminService().isDocumentTypeUsed(documentType.getId());
    }

}
