package ee.webmedia.alfresco.eventplan.service;

import java.util.Date;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.Pair;

import ee.webmedia.alfresco.eventplan.model.EventPlan;
import ee.webmedia.alfresco.eventplan.service.dto.EventPlanSeries;
import ee.webmedia.alfresco.eventplan.service.dto.EventPlanVolume;

/**
 * Services for working with {@link EventPlan} entities.
<<<<<<< HEAD
 * 
 * @author Martti Tamm
=======
>>>>>>> develop-5.1
 */
public interface EventPlanService {

    String BEAN_NAME = "EventPlanService";

    List<EventPlan> getEventPlans();

    EventPlan getEventPlan(NodeRef nodeRef);

    ee.webmedia.alfresco.eventplan.model.EventPlanVolume getEventPlanVolume(NodeRef nodeRef);

    void save(EventPlan eventPlan);

    void save(ee.webmedia.alfresco.eventplan.model.EventPlanVolume eventPlanVolume);

    byte deleteEventPlan(NodeRef nodeRef);

    List<EventPlanSeries> getSeries(NodeRef eventPlanRef);

    List<EventPlanVolume> getVolumes(NodeRef eventPlanRef, String title, List<String> status, List<NodeRef> store);

    void initVolumeOrCaseFileFromSeriesEventPlan(NodeRef volumeOrCaseFileRef);

    Pair<Boolean, Date> closeVolumeOrCaseFile(NodeRef volumeOrCaseFileRef);

}
