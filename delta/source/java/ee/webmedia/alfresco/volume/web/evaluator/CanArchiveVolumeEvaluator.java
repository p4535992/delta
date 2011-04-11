package ee.webmedia.alfresco.volume.web.evaluator;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.volume.model.Volume;

public class CanArchiveVolumeEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 0L;

    @Override
    public boolean evaluate(Object obj) {
        Assert.isInstanceOf(Volume.class, obj, "This evaluator expects Volume to be passed as argument");
        Volume vol = (Volume) obj;
        return DocListUnitStatus.CLOSED.getValueName().equals(vol.getStatus())
                && StringUtils.equals(vol.getNode().getNodeRef().getStoreRef().toString(), "workspace://SpacesStore");
    }
}
