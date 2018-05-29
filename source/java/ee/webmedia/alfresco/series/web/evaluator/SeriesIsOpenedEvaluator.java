package ee.webmedia.alfresco.series.web.evaluator;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.series.model.SeriesModel;

public class SeriesIsOpenedEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 0L;

    @Override
    public boolean evaluate(Node node) {
        String status = (String) node.getProperties().get(SeriesModel.Props.STATUS);
        return DocListUnitStatus.OPEN.getValueName().equals(status);
    }

}
