package ee.webmedia.alfresco.casefile.web.evaluator;

import java.util.Map;

import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.common.evaluator.AbstarctCaseFileStatusEvaluator;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;

public class CaseFileIsClosedEvaluator extends AbstarctCaseFileStatusEvaluator {
    private static final long serialVersionUID = 0L;

    @Override
    public boolean evaluate(Node caseFileNode) {
        return evaluateInternal(caseFileNode);
    }

    @Override
    protected boolean isValidStatus(Map<String, Object> properties) {
        return DocListUnitStatus.CLOSED.getValueName().equals(properties.get(DocumentDynamicModel.Props.STATUS.toString()));
    }

    @Override
    protected boolean isValidStatus(String status) {
        return DocListUnitStatus.CLOSED.getValueName().equals(status);
    }
}