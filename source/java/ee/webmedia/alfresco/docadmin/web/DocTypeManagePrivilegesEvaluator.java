package ee.webmedia.alfresco.docadmin.web;

import org.alfresco.service.namespace.QName;
import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.base.BaseObject;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docadmin.service.CaseFileType;
import ee.webmedia.alfresco.docadmin.service.DocumentType;
import ee.webmedia.alfresco.docadmin.service.DynamicType;

/**
 * evaluates to true when permissions management should be enabled for the {@link DocumentType}
 * 
 * @author Ats Uiboupin
 */
public class DocTypeManagePrivilegesEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 0L;

    @Override
    public boolean evaluate(Object obj) {
        return evaluate((DynamicType) obj);
    }

    @Override
    public boolean evaluate(Node docTypeNode) {
        QName nodeType = docTypeNode.getType();
        Class<? extends DynamicType> dynTypeClass = DocumentAdminModel.Types.CASE_FILE_TYPE.equals(nodeType) ? CaseFileType.class : DocumentType.class;
        DynamicType dynType = BaseObject.reconstruct((WmNode) docTypeNode, dynTypeClass);
        return evaluate(dynType);
    }

    private boolean evaluate(DynamicType dynType) {
        return isDynTypeSavedAndShowingLatestVersion(dynType);
    }

    static boolean isDynTypeSavedAndShowingLatestVersion(DynamicType dynType) {
        return dynType.isSaved() && BeanHelper.getDynamicTypeDetailsDialog(dynType.getClass()).isShowingLatestVersion();
    }

}
