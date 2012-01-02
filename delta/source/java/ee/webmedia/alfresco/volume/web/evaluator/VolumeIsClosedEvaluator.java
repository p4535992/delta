package ee.webmedia.alfresco.volume.web.evaluator;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.volume.model.VolumeModel;

/**
 * @author Vladimir Drozdik
 */
public class VolumeIsClosedEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 0L;

    @Override
    public boolean evaluate(Object obj) {
        Node volumeNode = ((Volume) obj).getNode();
        String status = (String) volumeNode.getProperties().get(VolumeModel.Props.STATUS);
        return DocListUnitStatus.CLOSED.getValueName().equals(status) && BeanHelper.getGeneralService().getStore().equals(volumeNode.getNodeRef().getStoreRef());
    }

    @Override
    public boolean evaluate(Node node) {
        throw new RuntimeException("method evaluate(Object obj) is unimplemented");
    }
}