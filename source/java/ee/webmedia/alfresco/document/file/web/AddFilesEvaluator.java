package ee.webmedia.alfresco.document.file.web;

import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.privilege.model.Privilege;

public class AddFilesEvaluator extends BaseActionEvaluator {

    private static final long serialVersionUID = 778943700049418316L;

    @Override
    public boolean evaluate(Node docNode) {
        return docNode.getNodeRef().getStoreRef().getProtocol().equals(StoreRef.PROTOCOL_WORKSPACE)
                && (docNode.hasPermission(Privilege.EDIT_DOCUMENT) || (docNode.hasPermissionEveryone(Privilege.EDIT_DOCUMENT) && !BeanHelper.getUserService().isGuest()))
                && !Boolean.TRUE.equals(docNode.getProperties().get(DocumentCommonModel.Props.NOT_EDITABLE));
    }

    @Override
    public boolean evaluate(Object obj) {
        throw new RuntimeException("Unimplemented");
    }
}
