package ee.webmedia.alfresco.document.web.evaluator;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAdminService;

import java.util.Map;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.common.evaluator.SharedResourceEvaluator;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel.Props;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;

public class ChangeByNewDocumentEvaluator extends SharedResourceEvaluator {

    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Node node) {
        return node.getNodeRef().getStoreRef().getProtocol().equals(StoreRef.PROTOCOL_WORKSPACE)
                && !BeanHelper.getDocumentDialogHelperBean().isInEditMode()
                && new DocumentNotInDraftsFunctionActionEvaluator().evaluate(node)
                && (BeanHelper.getUserService().isDocumentManager() 
                        || AuthenticationUtil.getRunAsUser().equals(node.getProperties().get(DocumentCommonModel.Props.OWNER_ID.toString())))
                && isChangeByNewDocumentEnabled(node)
                && docActivitiesNotLimited(node);
    }

    private boolean isChangeByNewDocumentEnabled(Node node) {
        String docTypeId = (String) node.getProperties().get(Props.OBJECT_TYPE_ID);
        NodeRef docTypeRef = getDocumentAdminService().getDocumentTypeRef(docTypeId);
        return docTypeRef != null && getDocumentAdminService().getDocumentTypeProperty(docTypeId, DocumentAdminModel.Props.CHANGE_BY_NEW_DOCUMENT_ENABLED, Boolean.class);
    }

    private boolean docActivitiesNotLimited(Node node) {
        Map<String, Object> props = node.getProperties();
        NodeRef funRef = (NodeRef) props.get(DocumentCommonModel.Props.FUNCTION);
        return funRef != null && !BeanHelper.getFunctionsService().getUnmodifiableFunction(funRef, null).isDocumentActivitiesAreLimited();
    }

    @Override
    public boolean evaluate(Object obj) {
        throw new RuntimeException("method evaluate(obj) is unimplemented");
    }

    @Override
    public boolean evaluate() {
        DocumentDynamicActionsGroupResources resource = (DocumentDynamicActionsGroupResources) sharedResource;
        return resource.isWorkspaceNode() && !resource.isInEditMode()
                && resource.isNotInDraftsFunction()
                && (BeanHelper.getUserService().isDocumentManager() || resource.isDocOwner())
                && resource.getDocumentTypeRef() != null
                && getDocumentAdminService().getDocumentTypeProperty(resource.getObjectTypeId(), DocumentAdminModel.Props.CHANGE_BY_NEW_DOCUMENT_ENABLED, Boolean.class)
                && docActivitiesNotLimited(resource.getObject());
    }
}
