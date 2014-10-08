<<<<<<< HEAD
package ee.webmedia.alfresco.document.web.evaluator;

import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.document.model.DocumentCommonModel;

/**
 * @author Riina Tens
 */
public class DocumentSendForInformationEvaluator extends BaseActionEvaluator {

    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Node docNode) {
        return docNode.getNodeRef().getStoreRef().getProtocol().equals(StoreRef.PROTOCOL_WORKSPACE)
                && new DocumentNotInDraftsFunctionActionEvaluator().evaluate(docNode)
                && new ViewStateActionEvaluator().evaluate(docNode)
                && docNode.hasPermissions(DocumentCommonModel.Privileges.VIEW_DOCUMENT_META_DATA, DocumentCommonModel.Privileges.VIEW_DOCUMENT_FILES);
    }

}
=======
package ee.webmedia.alfresco.document.web.evaluator;

import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.privilege.model.Privilege;

public class DocumentSendForInformationEvaluator extends BaseActionEvaluator {

    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Node docNode) {
        return docNode.getNodeRef().getStoreRef().getProtocol().equals(StoreRef.PROTOCOL_WORKSPACE)
                && new DocumentNotInDraftsFunctionActionEvaluator().evaluate(docNode)
                && new ViewStateActionEvaluator().evaluate(docNode)
                && docNode.hasPermission(Privilege.VIEW_DOCUMENT_META_DATA, Privilege.VIEW_DOCUMENT_FILES);
    }

}
>>>>>>> develop-5.1
