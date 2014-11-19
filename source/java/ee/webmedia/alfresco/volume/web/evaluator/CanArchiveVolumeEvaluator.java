package ee.webmedia.alfresco.volume.web.evaluator;

<<<<<<< HEAD
import org.alfresco.service.cmr.repository.StoreRef;
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
<<<<<<< HEAD
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.volume.model.VolumeModel;


=======
import ee.webmedia.alfresco.volume.model.VolumeModel;

>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
public class CanArchiveVolumeEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 0L;

    @Override
    public boolean evaluate(Node node) {
        return DocListUnitStatus.CLOSED.getValueName().equals(node.getProperties().get(VolumeModel.Props.STATUS))
<<<<<<< HEAD
                && StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.equals(node.getNodeRef().getStoreRef())
                && BeanHelper.getUserService().isArchivist();
    }

=======
                && StringUtils.equals(node.getNodeRef().getStoreRef().toString(), "workspace://SpacesStore");
    }
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
}
