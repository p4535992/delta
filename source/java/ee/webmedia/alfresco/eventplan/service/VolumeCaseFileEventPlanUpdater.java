package ee.webmedia.alfresco.eventplan.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.ObjectUtils;

import ee.webmedia.alfresco.casefile.model.CaseFileModel;
import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.eventplan.model.EventPlanModel;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.volume.model.VolumeModel;

/**
 * CL 204311
<<<<<<< HEAD
 * 
 * @author Martti Tamm
=======
>>>>>>> develop-5.1
 */
public class VolumeCaseFileEventPlanUpdater extends AbstractNodeUpdater {

    private static final QName DISPOSITION_DATE = QName.createQName(VolumeModel.URI, "dispositionDate");
    private static final QName ARCHIVING_NOTE = QName.createQName(VolumeModel.URI, "archivingNote");
    private static final QName SEND_TO_DESTRUCTION = QName.createQName(VolumeModel.URI, "sendToDestruction");

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.joinQueryPartsOr(
                SearchUtil.generateTypeQuery(VolumeModel.Types.VOLUME),
                SearchUtil.generateTypeQuery(CaseFileModel.Types.CASE_FILE)
                );

        List<ResultSet> resultSets = new ArrayList<ResultSet>();
        for (StoreRef storeRef : generalService.getAllWithArchivalsStoreRefs()) {
            resultSets.add(searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, query));
        }
        return resultSets;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) {
        QName type = nodeService.getType(nodeRef);

        Map<QName, Serializable> props = new HashMap<QName, Serializable>(3);
        if (VolumeModel.Types.VOLUME.equals(type)) {
            props.put(EventPlanModel.Props.ARCHIVING_NOTE, nodeService.getProperty(nodeRef, ARCHIVING_NOTE));
            props.put(EventPlanModel.Props.RETAIN_UNTIL_DATE, nodeService.getProperty(nodeRef, DISPOSITION_DATE));
            props.put(EventPlanModel.Props.MARKED_FOR_DESTRUCTION, nodeService.getProperty(nodeRef, SEND_TO_DESTRUCTION));

            nodeService.removeProperty(nodeRef, ARCHIVING_NOTE);
            nodeService.removeProperty(nodeRef, DISPOSITION_DATE);
            nodeService.removeProperty(nodeRef, SEND_TO_DESTRUCTION);
        }

        nodeService.addAspect(nodeRef, EventPlanModel.Aspects.VOLUME_EVENT_PLAN, props);

        return new String[] {
                type.toPrefixString(serviceRegistry.getNamespaceService()),
                (String) props.get(EventPlanModel.Props.ARCHIVING_NOTE),
                ObjectUtils.toString(props.get(EventPlanModel.Props.RETAIN_UNTIL_DATE)),
                ObjectUtils.toString(props.get(EventPlanModel.Props.MARKED_FOR_DESTRUCTION)), };
    }

}
