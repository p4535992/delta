package ee.webmedia.alfresco.casefile.web.evaluator;

import static ee.webmedia.alfresco.common.web.BeanHelper.getCaseFileService;

import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.casefile.service.CaseFile;
import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.common.web.BeanHelper;

<<<<<<< HEAD
/**
 * @author Keit Tehvan
 */
=======
>>>>>>> develop-5.1
public class ArchiveCaseFileEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 0L;

    @Override
    public boolean evaluate(Object obj) {
        Assert.isInstanceOf(CaseFile.class, obj, "This evaluator expects CaseFile to be passed as argument");
        CaseFile caseFile = (CaseFile) obj;
        return DocListUnitStatus.CLOSED.getValueName().equals(caseFile.getStatus())
                && caseFile.getNode().getNodeRef().getStoreRef().equals(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE) && BeanHelper.getUserService().isArchivist();
    }

    @Override
    public boolean evaluate(Node node) {
        return evaluate(getCaseFileService().getCaseFile(node.getNodeRef()));
    }
}
