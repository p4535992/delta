package ee.webmedia.alfresco.document.web.evaluator;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.casefile.model.CaseFileModel;
import ee.webmedia.alfresco.casefile.service.CaseFile;
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
        if (parentNode == null || !BeanHelper.getUserService().isDocumentManager()
                && !BeanHelper.getUserService().isArchivist()) {
            return false;
        }
        NodeRef parentRef = parentNode.getNodeRef();
        NodeService nodeService = BeanHelper.getNodeService();
        if (nodeService.getType(parentRef).equals(CaseModel.Types.CASE)) {
            Case aCase = BeanHelper.getCaseService().getCaseByNoderef(parentRef);
            return DocListUnitStatus.OPEN.getValueName().equals(aCase.getStatus());
        } else if (nodeService.getType(parentRef).equals(VolumeModel.Types.VOLUME)) {
            UnmodifiableVolume volume = BeanHelper.getVolumeService().getUnmodifiableVolume(parentRef, null);
            return (DocListUnitStatus.OPEN.getValueName().equals(volume.getStatus())
                    || DocListUnitStatus.CLOSED.getValueName().equals(volume.getStatus()))
                    && !volume.isTransferConfirmed() && !volume.isMarkedForDestruction();
        } else if (nodeService.getType(parentRef).equals(CaseFileModel.Types.CASE_FILE)) {
            CaseFile caseFile = BeanHelper.getCaseFileService().getCaseFile(parentRef);
            return (DocListUnitStatus.OPEN.getValueName().equals(caseFile.getStatus())
                    || DocListUnitStatus.CLOSED.getValueName().equals(caseFile.getStatus()))
                    && !caseFile.isTransferConfirmed() && !caseFile.isMarkedForDestruction();
        }
        return false;
    }
}
