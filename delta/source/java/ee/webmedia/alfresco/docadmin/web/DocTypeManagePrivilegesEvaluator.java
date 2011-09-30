package ee.webmedia.alfresco.docadmin.web;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.base.BaseObject;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.service.DocumentType;

/**
 * evaluates to true when permissions management should be enabled for the {@link DocumentType}
 * 
 * @author Ats Uiboupin
 */
public class DocTypeManagePrivilegesEvaluator extends BaseActionEvaluator {
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
        return isDocTypeSavedAndShowingLatestVersion(documentType);
    }

    static boolean isDocTypeSavedAndShowingLatestVersion(DocumentType documentType) {
        return documentType.isSaved() && BeanHelper.getDocTypeDetailsDialog().isShowingLatestVersion();
    }

}
