package ee.webmedia.alfresco.volume.web.evaluator;

import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.volume.model.Volume;
import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.springframework.util.Assert;

public class VolumeClosedActionEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 0L;

    public boolean evaluate(Object obj) {
        Assert.isInstanceOf(Volume.class, obj, "This evaluator expects Volume to be passed as argument");
        Volume vol = (Volume) obj;
        return DocListUnitStatus.CLOSED.getValueName().equals(vol.getStatus());
    }
}
