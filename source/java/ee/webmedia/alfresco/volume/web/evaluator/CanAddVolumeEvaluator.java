package ee.webmedia.alfresco.volume.web.evaluator;

import java.util.List;

import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.classificator.enums.VolumeType;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.user.web.DocumentManagerEvaluator;

/**
 * Evaluates to true if given series has status "avatud" and volume types contains "ANNUAL_FILE" or "SUBJECT_FILE".
 */
public class CanAddVolumeEvaluator extends DocumentManagerEvaluator {

    private static final long serialVersionUID = 0L;

    @Override
    public boolean evaluate(Node node) {
        if (!super.evaluate(node)) {
            return false;
        }
        String status = (String) node.getProperties().get(SeriesModel.Props.STATUS);
        @SuppressWarnings("unchecked")
        List<String> volTypes = (List<String>) node.getProperties().get(SeriesModel.Props.VOL_TYPE);
        return DocListUnitStatus.OPEN.getValueName().equals(status) &&
                (volTypes.contains(VolumeType.ANNUAL_FILE.name()) || volTypes.contains(VolumeType.SUBJECT_FILE.name()))
                && BeanHelper.getUserService().isDocumentManager();
    }
}
