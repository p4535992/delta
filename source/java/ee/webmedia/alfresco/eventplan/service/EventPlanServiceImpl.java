package ee.webmedia.alfresco.eventplan.service;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentSearchService;
import static ee.webmedia.alfresco.log.PropDiffHelper.value;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.util.GUID;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;

import ee.webmedia.alfresco.casefile.model.CaseFileModel;
import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.classificator.enums.VolumeType;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.eventplan.model.EventPlan;
import ee.webmedia.alfresco.eventplan.model.EventPlanModel;
import ee.webmedia.alfresco.eventplan.model.FirstEvent;
import ee.webmedia.alfresco.eventplan.model.FirstEventStart;
import ee.webmedia.alfresco.eventplan.model.RetaintionStart;
import ee.webmedia.alfresco.eventplan.service.dto.EventPlanSeries;
import ee.webmedia.alfresco.eventplan.service.dto.EventPlanVolume;
import ee.webmedia.alfresco.functions.model.FunctionsModel;
import ee.webmedia.alfresco.log.PropDiffHelper;
import ee.webmedia.alfresco.log.model.LogEntry;
import ee.webmedia.alfresco.log.model.LogObject;
import ee.webmedia.alfresco.log.service.LogService;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.volume.model.VolumeModel;

public class EventPlanServiceImpl implements EventPlanService {

    private NodeService nodeService;
    private GeneralService generalService;
    private LogService logService;
    private UserService userService;

    @Override
    public List<EventPlan> getEventPlans() {
        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(getPlansRoot(), EventPlanModel.Assocs.EVENT_PLANS, RegexQNamePattern.MATCH_ALL);
        List<EventPlan> plans = new ArrayList<EventPlan>(childAssocs.size());
        for (ChildAssociationRef assoc : childAssocs) {
            plans.add(getEventPlan(assoc.getChildRef()));
        }
        Collections.sort(plans);
        return plans;
    }

    @Override
    public EventPlan getEventPlan(NodeRef nodeRef) {
        return new EventPlan(generalService.fetchObjectNode(nodeRef, EventPlanModel.Types.EVENT_PLAN));
    }

    @Override
    public ee.webmedia.alfresco.eventplan.model.EventPlanVolume getEventPlanVolume(NodeRef nodeRef) {
        QName type = nodeService.getType(nodeRef);
        if (!VolumeModel.Types.VOLUME.equals(type) && !CaseFileModel.Types.CASE_FILE.equals(type)) {
            throw new RuntimeException("Dialog cannot handle node type " + type);
        }
        return new ee.webmedia.alfresco.eventplan.model.EventPlanVolume(generalService.fetchObjectNode(nodeRef, type));
    }

    @Override
    public void save(EventPlan eventPlan) {
        if (eventPlan.isRetainPermanent()) {
            eventPlan.setRetaintionPeriod(null);
            eventPlan.setRetaintionStart(null);
        }

        Node node = eventPlan.getNode();
        Map<QName, Serializable> props = RepoUtil.toQNameProperties(node.getProperties());

        if (RepoUtil.isSaved(node.getNodeRef())) {
            String diff = new PropDiffHelper()
                    .label(EventPlanModel.Props.NAME, "eventplan_name")
                    .labelEnum(EventPlanModel.Props.FIRST_EVENT, "eventplan_firstEvent", FirstEvent.class)
                    .labelEnum(EventPlanModel.Props.FIRST_EVENT_START, "eventplan_firstEventStart", FirstEventStart.class)
                    .label(EventPlanModel.Props.FIRST_EVENT_PERIOD, "eventplan_firstEventPeriod")
                    .label(EventPlanModel.Props.IS_APPRAISED, "eventplan_isAppraised")
                    .label(EventPlanModel.Props.HAS_ARCHIVAL_VALUE, "eventplan_hasArchivalValue")
                    .label(EventPlanModel.Props.RETAIN_PERMANENT, "eventplan_retainPermanent")
                    .labelEnum(EventPlanModel.Props.RETAINTION_START, "eventplan_retaintionStart", RetaintionStart.class)
                    .label(EventPlanModel.Props.RETAINTION_PERIOD, "eventplan_retaintionPeriod")
                    .label(EventPlanModel.Props.RETAIN_UNTIL_DATE, "eventplan_retainUntilDate")
                    .label(EventPlanModel.Props.ARCHIVING_NOTE, "eventplan_archivingNote")
                    .diff(nodeService.getProperties(node.getNodeRef()), props);

            if (diff != null) {
                logService.addLogEntry(LogEntry.create(LogObject.EVENT_PLAN, userService, node.getNodeRef(), "eventplan_log_modified", diff));
            }

            nodeService.setProperties(node.getNodeRef(), props);
        } else {
            QName assocName = QName.createQName(EventPlanModel.URI, GUID.generate());
            NodeRef ref = nodeService.createNode(getPlansRoot(), EventPlanModel.Assocs.EVENT_PLANS, assocName, EventPlanModel.Types.EVENT_PLAN, props).getChildRef();
            eventPlan.setNode(generalService.fetchObjectNode(ref, EventPlanModel.Types.EVENT_PLAN));
            logService.addLogEntry(LogEntry.create(LogObject.EVENT_PLAN, userService, ref, "eventplan_log_created"));
        }
    }

    @Override
    public void save(ee.webmedia.alfresco.eventplan.model.EventPlanVolume eventPlanVolume) {
        save(eventPlanVolume, "eventplan_volume_log_modified");
    }

    private void save(ee.webmedia.alfresco.eventplan.model.EventPlanVolume eventPlanVolume, String logMsgCode) {
        Node node = eventPlanVolume.getNode();
        Map<QName, Serializable> newProps = RepoUtil.toQNameProperties(node.getProperties());

        if (logMsgCode != null) {
            Map<QName, Serializable> repoProps = nodeService.getProperties(node.getNodeRef());

            String diff = new PropDiffHelper()
                    .label(EventPlanModel.Props.IS_APPRAISED, "eventplan_isAppraised")
                    .label(EventPlanModel.Props.HAS_ARCHIVAL_VALUE, "eventplan_hasArchivalValue")
                    .label(EventPlanModel.Props.RETAIN_PERMANENT, "eventplan_retainPermanent")
                    .labelEnum(EventPlanModel.Props.RETAINTION_START, "eventplan_retaintionStart", RetaintionStart.class)
                    .label(EventPlanModel.Props.RETAINTION_PERIOD, "eventplan_retaintionPeriod")
                    .label(EventPlanModel.Props.RETAIN_UNTIL_DATE, "eventplan_retainUntilDate")
                    .labelEnum(EventPlanModel.Props.NEXT_EVENT, "eventplan_nextEvent", FirstEvent.class)
                    .label(EventPlanModel.Props.NEXT_EVENT_DATE, "eventplan_nextEventDate")
                    .label(EventPlanModel.Props.ARCHIVING_NOTE, "eventplan_archivingNote")
                    .label(EventPlanModel.Props.MARKED_FOR_TRANSFER, "eventplan_markedForTransfer")
                    .label(EventPlanModel.Props.EXPORTED_FOR_UAM, "eventplan_exportedForUam")
                    .label(EventPlanModel.Props.TRANSFER_CONFIRMED, "eventplan_transferConfirmed")
                    .labelDateTime(EventPlanModel.Props.TRANSFERED_DATE_TIME, "eventplan_transferedDateTime")
                    .label(EventPlanModel.Props.MARKED_FOR_DESTRUCTION, "eventplan_markedForDestruction")
                    .label(EventPlanModel.Props.DISPOSAL_ACT_CREATED, "eventplan_disposalActCreated")
                    .diff(repoProps, newProps);

            LogObject logObject = VolumeModel.Types.VOLUME.equals(node.getType()) ? LogObject.VOLUME : LogObject.CASE_FILE;
            if (diff != null) {
                logService.addLogEntry(LogEntry.create(logObject, userService, node.getNodeRef(), logMsgCode, diff));
            }

            NodeRef repoEventPlan = (NodeRef) repoProps.get(EventPlanModel.Props.EVENT_PLAN);
            NodeRef newEventPlan = (NodeRef) newProps.get(EventPlanModel.Props.EVENT_PLAN);
            if (!ObjectUtils.equals(repoEventPlan, newEventPlan)) {
                String emptyLabel = PropDiffHelper.getEmptyLabel();
                logService
                        .addLogEntry(LogEntry.create(logObject, userService, node.getNodeRef(),
                                "eventplan_volume_log_eventplan_modified",
                                value((repoEventPlan == null || !nodeService.exists(repoEventPlan)) ? null : nodeService.getProperty(repoEventPlan, EventPlanModel.Props.NAME),
                                        emptyLabel),
                                value((newEventPlan == null || !nodeService.exists(newEventPlan)) ? null : nodeService.getProperty(newEventPlan, EventPlanModel.Props.NAME),
                                        emptyLabel)
                                ));
            }
        }
        nodeService.addProperties(node.getNodeRef(), newProps);
    }

    @Override
    public void initVolumeOrCaseFileFromSeriesEventPlan(NodeRef volumeOrCaseFileRef) {
        NodeRef seriesRef = generalService.getAncestorNodeRefWithType(volumeOrCaseFileRef, SeriesModel.Types.SERIES);
        NodeRef eventPlanRef = (NodeRef) nodeService.getProperty(seriesRef, SeriesModel.Props.EVENT_PLAN);
        if (eventPlanRef != null && nodeService.exists(eventPlanRef)) {
            ee.webmedia.alfresco.eventplan.model.EventPlanVolume eventPlanVolume = getEventPlanVolume(volumeOrCaseFileRef);
            eventPlanVolume.initFromEventPlan(getEventPlan(eventPlanRef));
            save(eventPlanVolume, null);
        }
    }

    @Override
    public Pair<Boolean, Date> closeVolumeOrCaseFile(NodeRef volumeOrCaseFileRef) {
        ee.webmedia.alfresco.eventplan.model.EventPlanVolume eventPlanVolume = getEventPlanVolume(volumeOrCaseFileRef);

        String status = eventPlanVolume.getProp(VolumeModel.Props.STATUS);
        if (DocListUnitStatus.CLOSED.getValueName().equals(status)) {
            return Pair.newInstance(false, null);
        }

        if (eventPlanVolume.getValidTo() == null) {
            Date validTo = new Date();

            if (eventPlanVolume.getRetainUntilDate() == null) {
                Integer period = eventPlanVolume.getRetaintionPeriod();
                String start = eventPlanVolume.getRetaintionStart();
                if (period != null && RetaintionStart.FROM_CLOSING.is(start)) {
                    eventPlanVolume.setRetainUntilDate(DateUtils.addYears(validTo, period));
                } else if (period != null && RetaintionStart.FROM_CLOSING_YEAR_END.is(start)) {
                    Date retainUntilDate = DateUtils.addYears(validTo, period + 1);
                    retainUntilDate = DateUtils.truncate(retainUntilDate, Calendar.YEAR);
                    eventPlanVolume.setRetainUntilDate(retainUntilDate);
                }
            }

            if (eventPlanVolume.getNextEventDate() == null && StringUtils.isNotBlank(eventPlanVolume.getNextEvent()) && eventPlanVolume.getEventPlan() != null) {
                NodeRef eventPlanRef = eventPlanVolume.getEventPlan();

                if (nodeService.exists(eventPlanRef) && eventPlanVolume.getNextEvent().equals(nodeService.getProperty(eventPlanRef, EventPlanModel.Props.FIRST_EVENT))) {
                    Integer period = (Integer) nodeService.getProperty(eventPlanRef, EventPlanModel.Props.FIRST_EVENT_PERIOD);
                    String start = (String) nodeService.getProperty(eventPlanRef, EventPlanModel.Props.FIRST_EVENT_START);

                    if (period != null && FirstEventStart.FROM_CLOSING.is(start)) {
                        eventPlanVolume.setNextEventDate(DateUtils.addYears(validTo, period));
                    } else if (period != null && FirstEventStart.FROM_CLOSING_YEAR_END.is(start)) {
                        Date retainUntilDate = DateUtils.addYears(validTo, period + 1);
                        retainUntilDate = DateUtils.truncate(retainUntilDate, Calendar.YEAR);
                        eventPlanVolume.setNextEventDate(retainUntilDate);
                    }
                }
            }
            save(eventPlanVolume, "volume_log_eventplan_props_modified");
            return Pair.newInstance(true, validTo);
        }
        return Pair.newInstance(true, null);
    }

    @Override
    public byte deleteEventPlan(NodeRef nodeRef) {
        if (RepoUtil.isSaved(nodeRef) && nodeService.exists(nodeRef) && EventPlanModel.Types.EVENT_PLAN.equals(nodeService.getType(nodeRef))) {

            String seriesQuery = SearchUtil.joinQueryPartsAnd(SearchUtil.generateTypeQuery(SeriesModel.Types.SERIES),
                    SearchUtil.generateNodeRefQuery(nodeRef, SeriesModel.Props.EVENT_PLAN));
            String volumeQuery = SearchUtil.joinQueryPartsAnd(SearchUtil.generateTypeQuery(VolumeModel.Types.VOLUME, CaseFileModel.Types.CASE_FILE),
                    SearchUtil.generateNodeRefQuery(nodeRef, EventPlanModel.Props.EVENT_PLAN));

            if (getDocumentSearchService().isMatch(seriesQuery, true, "seriesByEventPlanExist")) {
                return 1;
            } else if (getDocumentSearchService().isMatch(volumeQuery, true, "volumeByEventPlanExist")) {
                return 2;
            }

            String name = (String) nodeService.getProperty(nodeRef, EventPlanModel.Props.NAME);
            nodeService.deleteNode(nodeRef);
            logService.addLogEntry(LogEntry.create(LogObject.EVENT_PLAN, userService, nodeRef, "eventplan_log_deleted", name));
        }
        return 0;
    }

    @Override
    public List<EventPlanSeries> getSeries(NodeRef eventPlanRef) {
        List<NodeRef> seriesRefs = getDocumentSearchService().searchSeriesByEventPlan(eventPlanRef);
        List<EventPlanSeries> series = new ArrayList<EventPlanSeries>(seriesRefs.size());
        for (NodeRef seriesRef : seriesRefs) {
            NodeRef functionRef = nodeService.getPrimaryParent(seriesRef).getParentRef();
            Integer order = (Integer) nodeService.getProperty(seriesRef, SeriesModel.Props.ORDER);
            String identifier = (String) nodeService.getProperty(seriesRef, SeriesModel.Props.SERIES_IDENTIFIER);
            String title = (String) nodeService.getProperty(seriesRef, SeriesModel.Props.TITLE);
            String status = (String) nodeService.getProperty(seriesRef, SeriesModel.Props.STATUS);
            Integer fnOrder = (Integer) nodeService.getProperty(functionRef, FunctionsModel.Props.ORDER);
            String fnMark = (String) nodeService.getProperty(functionRef, FunctionsModel.Props.MARK);
            String fnTitle = (String) nodeService.getProperty(functionRef, FunctionsModel.Props.TITLE);
            title += " (" + nodeService.getProperty(seriesRef, SeriesModel.Props.CONTAINING_DOCS_COUNT) + ")";
            series.add(new EventPlanSeries(seriesRef, functionRef, order, identifier, title, status, fnMark + " " + fnTitle, fnOrder, fnMark,
                    generalService.getStoreTitle(seriesRef.getStoreRef())));
        }
        Collections.sort(series);
        return series;
    }

    @Override
    public List<EventPlanVolume> getVolumes(NodeRef eventPlanRef, String inputTitle, List<String> inputStatus, List<NodeRef> location) {
        List<NodeRef> volumeRefs = getDocumentSearchService().searchVolumesByEventPlan(eventPlanRef, inputTitle, inputStatus, location);
        List<EventPlanVolume> volumes = new ArrayList<EventPlanVolume>(volumeRefs.size());
        for (NodeRef volumeRef : volumeRefs) {
            boolean dynamic = CaseFileModel.Types.CASE_FILE.equals(nodeService.getType(volumeRef));
            NodeRef seriesRef = nodeService.getPrimaryParent(volumeRef).getParentRef();
            NodeRef functionRef = nodeService.getPrimaryParent(seriesRef).getParentRef();

            String mark = (String) nodeService.getProperty(volumeRef, VolumeModel.Props.VOLUME_MARK);
            String title = (String) nodeService.getProperty(volumeRef, VolumeModel.Props.TITLE);
            Date validFrom = (Date) nodeService.getProperty(volumeRef, VolumeModel.Props.VALID_FROM);
            Date validTo = (Date) nodeService.getProperty(volumeRef, VolumeModel.Props.VALID_TO);
            String status = (String) nodeService.getProperty(volumeRef, VolumeModel.Props.STATUS);
            String volumeType = dynamic ? VolumeType.CASE_FILE.name() : (String) nodeService.getProperty(volumeRef, VolumeModel.Props.VOLUME_TYPE);
            String ownerName = (String) nodeService.getProperty(volumeRef, ContentModel.PROP_OWNER);
            Integer containingDocsCount = (Integer) nodeService.getProperty(volumeRef, VolumeModel.Props.CONTAINING_DOCS_COUNT);
            if (containingDocsCount == null) {
                containingDocsCount = 0;
            }
            title += " (" + containingDocsCount + ")";

            String identifier = (String) nodeService.getProperty(seriesRef, SeriesModel.Props.SERIES_IDENTIFIER);
            String seriesTitle = (String) nodeService.getProperty(seriesRef, SeriesModel.Props.TITLE);
            String fnMark = (String) nodeService.getProperty(functionRef, FunctionsModel.Props.MARK);
            String fnTitle = (String) nodeService.getProperty(functionRef, FunctionsModel.Props.TITLE);

            volumes.add(new EventPlanVolume(volumeRef, seriesRef, functionRef, mark, title, validFrom, validTo, status, volumeType, ownerName,
                    generalService.getStoreTitle(volumeRef.getStoreRef()), identifier + " " + seriesTitle, fnMark + " " + fnTitle, dynamic));
        }
        Collections.sort(volumes);
        return volumes;
    }

    private NodeRef getPlansRoot() {
        return generalService.getNodeRef(EventPlanModel.PATH);
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void setLogService(LogService logService) {
        this.logService = logService;
    }

}
