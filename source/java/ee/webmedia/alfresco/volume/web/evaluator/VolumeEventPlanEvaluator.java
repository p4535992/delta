package ee.webmedia.alfresco.volume.web.evaluator;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.volume.model.Volume;

public class VolumeEventPlanEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 0L;

    @Override
    public boolean evaluate(Object obj) {
        Assert.isInstanceOf(Volume.class, obj, "This evaluator expects Volume to be passed as argument");
        Volume vol = (Volume) obj;
        return vol.getNode().isSaved();
    }

}
