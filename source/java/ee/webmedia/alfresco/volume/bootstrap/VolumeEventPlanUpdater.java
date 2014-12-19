package ee.webmedia.alfresco.volume.bootstrap;

import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.casefile.model.CaseFileModel;
import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.eventplan.model.EventPlan;
import ee.webmedia.alfresco.eventplan.model.EventPlanModel;
import ee.webmedia.alfresco.eventplan.model.EventPlanVolume;
import ee.webmedia.alfresco.eventplan.service.EventPlanService;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.volume.model.VolumeModel;

public class VolumeEventPlanUpdater extends AbstractNodeUpdater {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(VolumeEventPlanUpdater.class);

    private String storeString;
    private EventPlanService eventPlanService;
    private Map<NodeRef, EventPlan> seriesRefToEventPlan;

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        Assert.notNull(storeString, "Store must be provided");
        storeString = StringUtils.trim(storeString);
        StoreRef storeRef = new StoreRef(storeString);
        List<StoreRef> allRefs = nodeService.getStores();
        if (!allRefs.contains(storeRef)) {
            throw new UnableToPerformException("User entered unknown storeRef: " + storeString);
        }
        seriesRefToEventPlan = new HashMap<>();
        return Arrays.asList(searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, generateTypeQuery(VolumeModel.Types.VOLUME, CaseFileModel.Types.CASE_FILE)));
    }

    @Override
    protected String[] updateNode(NodeRef volumeRef) throws Exception {
        NodeRef eventPlanRef = (NodeRef) nodeService.getProperty(volumeRef, EventPlanModel.Props.EVENT_PLAN);
        if (eventPlanRef != null) {
            return null;
        }
        NodeRef seriesRef = nodeService.getPrimaryParent(volumeRef).getParentRef();
        EventPlan seriesEventPlan = seriesRefToEventPlan.get(seriesRef);
        if (seriesEventPlan == null) {
            eventPlanRef = (NodeRef) nodeService.getProperty(seriesRef, SeriesModel.Props.EVENT_PLAN);
            if (eventPlanRef == null) {
                return null;
            }
            seriesEventPlan = eventPlanService.getEventPlan(eventPlanRef);
            seriesRefToEventPlan.put(seriesRef, seriesEventPlan);
        }

        EventPlanVolume eventPlanVolume = eventPlanService.getEventPlanVolume(volumeRef);
        eventPlanVolume.initFromEventPlan(seriesEventPlan);
        String msg = eventPlanVolume.validateAndGetValidationMessage();
        if (msg == null) {
            eventPlanService.save(eventPlanVolume);
            String addedEventPlanRef = eventPlanVolume.getNodeRef() != null ? eventPlanVolume.getNodeRef().toString() : "";
            return new String[] { addedEventPlanRef };
        }
        LOG.warn(MessageUtil.getMessage(msg) + " (volumeRef=" + volumeRef + ")");
        return new String[] { msg };
    }

    @Override
    protected void executeUpdater() throws Exception {
        super.executeUpdater();
        resetFields();
    }

    @Override
    protected boolean usePreviousInputState() {
        return false;
    }

    @Override
    protected Set<NodeRef> loadNodesFromFile(File file, boolean readHeaders) throws Exception {
        return null;
    }

    private void resetFields() {
        seriesRefToEventPlan = null;
        storeString = null;
    }

    public void setEventPlanService(EventPlanService eventPlanService) {
        this.eventPlanService = eventPlanService;
    }

    public String getStoreString() {
        return storeString;
    }

    public void setStoreString(String storeString) {
        this.storeString = storeString;
    }

}
