<<<<<<< HEAD
package ee.webmedia.alfresco.cases.web.evaluator;

import static ee.webmedia.alfresco.common.web.BeanHelper.getCaseDetailsDialog;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.common.web.BeanHelper;

/**
 * @author Keit Tehvan
 */
public class CaseIsClosedEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Node node) {
        throw new RuntimeException("method evaluate(Node node) is unimplemented");
    }

    @Override
    public boolean evaluate(Object obj) {
        Node caseNode = getCaseDetailsDialog().getCurrentNode();
        String status = (String) caseNode.getProperties().get(CaseModel.Props.STATUS);
        return DocListUnitStatus.CLOSED.getValueName().equals(status) && BeanHelper.getGeneralService().getStore().equals(caseNode.getNodeRef().getStoreRef());
    }
}
=======
package ee.webmedia.alfresco.cases.web.evaluator;

import static ee.webmedia.alfresco.common.web.BeanHelper.getCaseDetailsDialog;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.common.web.BeanHelper;

public class CaseIsClosedEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Node node) {
        throw new RuntimeException("method evaluate(Node node) is unimplemented");
    }

    @Override
    public boolean evaluate(Object obj) {
        Node caseNode = getCaseDetailsDialog().getCurrentNode();
        String status = (String) caseNode.getProperties().get(CaseModel.Props.STATUS);
        return DocListUnitStatus.CLOSED.getValueName().equals(status) && BeanHelper.getGeneralService().getStore().equals(caseNode.getNodeRef().getStoreRef());
    }
}
>>>>>>> develop-5.1
