package ee.webmedia.alfresco.volume.web.evaluator;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.volume.model.VolumeModel;

/**
 * @author Vladimir Drozdik
 */
public class VolumeIsOpenedEvaluator extends BaseActionEvaluator {

    private static final long serialVersionUID = 0L;

    @Override
    public boolean evaluate(Object obj) {
        Volume volume = (Volume) obj;
        String status = (String) volume.getNode().getProperties().get(VolumeModel.Props.STATUS);
        return DocListUnitStatus.OPEN.getValueName().equals(status);
    }

    @Override
    public boolean evaluate(Node node) {
        throw new RuntimeException("method evaluate(Node node) is unimplemented");
    }
}