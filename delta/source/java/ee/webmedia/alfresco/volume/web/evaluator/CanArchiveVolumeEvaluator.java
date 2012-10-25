package ee.webmedia.alfresco.volume.web.evaluator;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.volume.model.VolumeModel;

public class CanArchiveVolumeEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 0L;

    @Override
    public boolean evaluate(Node node) {
        return DocListUnitStatus.CLOSED.getValueName().equals(node.getProperties().get(VolumeModel.Props.STATUS))
                && StringUtils.equals(node.getNodeRef().getStoreRef().toString(), "workspace://SpacesStore");
    }
}
