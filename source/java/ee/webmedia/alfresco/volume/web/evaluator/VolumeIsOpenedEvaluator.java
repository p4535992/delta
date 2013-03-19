package ee.webmedia.alfresco.volume.web.evaluator;

import static ee.webmedia.alfresco.common.web.BeanHelper.getVolumeDetailsDialog;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.volume.model.VolumeModel;

/**
 * @author Vladimir Drozdik
 */
public class VolumeIsOpenedEvaluator extends BaseActionEvaluator {

    private static final long serialVersionUID = 0L;

    @Override
    public boolean evaluate(Node node) {
        if (getVolumeDetailsDialog().isNew()) {
            return false;
        }    
        String status = (String) node.getProperties().get(VolumeModel.Props.STATUS);
        return DocListUnitStatus.OPEN.getValueName().equals(status);
    }
    
}