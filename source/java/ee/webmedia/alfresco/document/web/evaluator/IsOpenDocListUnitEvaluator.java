package ee.webmedia.alfresco.document.web.evaluator;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.cases.model.Case;
import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.volume.model.UnmodifiableVolume;
import ee.webmedia.alfresco.volume.model.VolumeModel;

public class IsOpenDocListUnitEvaluator extends BaseActionEvaluator {

    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Node parentNode) {
        if (parentNode == null || !BeanHelper.getUserService().isDocumentManager()) {
            return false;
        }
        NodeRef parentRef = parentNode.getNodeRef();
        if (BeanHelper.getGeneralService().isArchivalsStoreRef(parentRef.getStoreRef())) {
            return true;
        }
        NodeService nodeService = BeanHelper.getNodeService();
        if (nodeService.getType(parentRef).equals(CaseModel.Types.CASE)) {
            Case aCase = BeanHelper.getCaseService().getCaseByNoderef(parentRef);
            return DocListUnitStatus.OPEN.getValueName().equals(aCase.getStatus());
        } else if (nodeService.getType(parentRef).equals(VolumeModel.Types.VOLUME)) {
            UnmodifiableVolume volume = BeanHelper.getVolumeService().getUnmodifiableVolume(parentRef, null);
            return DocListUnitStatus.OPEN.getValueName().equals(volume.getStatus());
        }
        return false;
    }
}
