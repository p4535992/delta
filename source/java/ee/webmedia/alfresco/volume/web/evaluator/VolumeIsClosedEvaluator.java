<<<<<<< HEAD
package ee.webmedia.alfresco.volume.web.evaluator;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.volume.model.VolumeModel;

/**
 * @author Vladimir Drozdik
 */
public class VolumeIsClosedEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 0L;

    @Override
    public boolean evaluate(Node node) {
        String status = (String) node.getProperties().get(VolumeModel.Props.STATUS);
        return DocListUnitStatus.CLOSED.getValueName().equals(status) && BeanHelper.getGeneralService().getStore().equals(node.getNodeRef().getStoreRef());
    }
}
=======
package ee.webmedia.alfresco.volume.web.evaluator;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.volume.model.VolumeModel;

public class VolumeIsClosedEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 0L;

    @Override
    public boolean evaluate(Node node) {
        String status = (String) node.getProperties().get(VolumeModel.Props.STATUS);
        return DocListUnitStatus.CLOSED.getValueName().equals(status) && BeanHelper.getGeneralService().getStore().equals(node.getNodeRef().getStoreRef());
    }
}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
