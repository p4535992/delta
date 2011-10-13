package ee.webmedia.alfresco.volume.web.evaluator;

import java.util.List;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.classificator.enums.VolumeType;
import ee.webmedia.alfresco.series.model.SeriesModel;

/**
 * @author Vladimir Drozdik
 */
public class CanAddVolumeEvaluator extends BaseActionEvaluator {

    private static final long serialVersionUID = 0L;

    @Override
    public boolean evaluate(Object obj) {
        throw new RuntimeException("method evaluate(Object obj) is unimplemented");
    }

    @Override
    public boolean evaluate(Node node) {
        String status = (String) node.getProperties().get(SeriesModel.Props.STATUS);
        @SuppressWarnings("unchecked")
        List<String> volTypes = (List<String>) node.getProperties().get(SeriesModel.Props.VOL_TYPE);
        return DocListUnitStatus.OPEN.getValueName().equals(status) &&
                (volTypes.contains(VolumeType.YEAR_BASED.name()) || volTypes.contains(VolumeType.OBJECT.name()));
    }
}
