package ee.webmedia.alfresco.casefile.web.evaluator;

import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.casefile.model.CaseFileModel;
import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.common.evaluator.CaseFileActionsGroupResource;
import ee.webmedia.alfresco.common.evaluator.SharedResourceEvaluator;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;

public class ArchiveCaseFileEvaluator extends SharedResourceEvaluator {
    private static final long serialVersionUID = 0L;

    @Override
    public boolean evaluate(Node node) {
        return node.getNodeRef().getStoreRef().equals(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE)
                && CaseFileModel.Types.CASE_FILE.equals(node.getType())
                && DocListUnitStatus.CLOSED.getValueName().equals(node.getProperties().get(DocumentDynamicModel.Props.STATUS))
                && BeanHelper.getUserService().isArchivist();
    }

    @Override
    public boolean evaluate() {
        CaseFileActionsGroupResource resource = (CaseFileActionsGroupResource) sharedResource;
        Node node = resource.getObject();
        return node.getNodeRef().getStoreRef().equals(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE)
                && CaseFileModel.Types.CASE_FILE.equals(node.getType())
                && DocListUnitStatus.CLOSED.getValueName().equals(resource.getStatus())
                && resource.isArchivist();
    }
}
