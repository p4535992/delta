package ee.webmedia.alfresco.cases.web;

import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.user.web.DocumentManagerEvaluator;
import ee.webmedia.alfresco.volume.model.VolumeModel;

public class AddCaseEvaluator extends DocumentManagerEvaluator {

    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Node node) {
        if (!super.evaluate(node)) {
            return false;
        }
        String status = (String) node.getProperties().get(VolumeModel.Props.STATUS);
        Boolean containsCases = (Boolean) node.getProperties().get(VolumeModel.Props.CONTAINS_CASES);
        return DocListUnitStatus.OPEN.equals(status) && Boolean.TRUE.equals(containsCases);
    }
}
