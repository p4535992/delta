package ee.webmedia.alfresco.volume.web.evaluator;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.volume.model.Volume;

public class VolumeEventPlanEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 0L;

    @Override
    public boolean evaluate(Object obj) {
        Assert.isInstanceOf(Volume.class, obj, "This evaluator expects Volume to be passed as argument");
        Volume vol = (Volume) obj;
        return vol.getNode().isSaved();
    }

    @Override
    public boolean evaluate(Node node) {
        Assert.isInstanceOf(WmNode.class, node, "WmNode is expected");
        return ((WmNode) node).isSaved();
    }

}
