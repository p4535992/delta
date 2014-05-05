package ee.webmedia.alfresco.casefile.web.evaluator;

import static ee.webmedia.alfresco.common.web.BeanHelper.getUserService;
import static ee.webmedia.alfresco.privilege.service.PrivilegeUtil.isAdminOrDocmanagerWithPermission;

import java.util.Date;

import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.casefile.service.CaseFile;
import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.document.web.evaluator.IsOwnerEvaluator;
import ee.webmedia.alfresco.document.web.evaluator.ViewStateActionEvaluator;
import ee.webmedia.alfresco.privilege.model.Privilege;
import ee.webmedia.alfresco.utils.CalendarUtil;
import ee.webmedia.alfresco.volume.model.VolumeModel;

public class CaseFileIsClosedEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 0L;

    @Override
    public boolean evaluate(Object obj) {
        return evaluate(((CaseFile) obj).getNode());
    }

    @Override
    public boolean evaluate(Node caseFileNode) {
        Date validTo = (Date) caseFileNode.getProperties().get(VolumeModel.Props.VALID_TO);
        return (isAdminOrDocmanagerWithPermission(caseFileNode, Privilege.VIEW_CASE_FILE)
                || new IsOwnerEvaluator().evaluate(caseFileNode)
                || (getUserService().isArchivist() && validTo != null && CalendarUtil.getDaysBetweenSigned(validTo, new Date()) > 0)
                ) &&
                StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.equals(caseFileNode.getNodeRef().getStoreRef()) &&
                new ViewStateActionEvaluator().evaluate(caseFileNode)
                && DocListUnitStatus.CLOSED.getValueName().equals(caseFileNode.getProperties().get(DocumentDynamicModel.Props.STATUS.toString()));
    }
}
