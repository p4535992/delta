package ee.webmedia.alfresco.casefile.service;

import static ee.webmedia.alfresco.app.AppConstants.DEFAULT_COLLATOR;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAdminService;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.app.AppConstants;
import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.common.model.DynamicBase;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docconfig.generator.systematic.DocumentLocationGenerator;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.PropsConvertedMap;
import ee.webmedia.alfresco.eventplan.model.EventPlanCommon;
import ee.webmedia.alfresco.eventplan.model.EventPlanModel;
import ee.webmedia.alfresco.eventplan.model.EventPlanModel.Props;
import ee.webmedia.alfresco.eventplan.model.RetaintionStart;
import ee.webmedia.alfresco.functions.model.Function;
import ee.webmedia.alfresco.series.model.Series;
import ee.webmedia.alfresco.utils.TextUtil;
import ee.webmedia.alfresco.volume.model.VolumeOrCaseFile;
import ee.webmedia.alfresco.workflow.service.WorkflowUtil;

/**
 * @author Kaarel Jõgeva
 */
public class CaseFile extends DynamicBase implements Cloneable, VolumeOrCaseFile {

    private static final long serialVersionUID = 1L;
    private static final FastDateFormat dateFormat = FastDateFormat.getInstance("dd.MM.yyyy");
    private String compoundWorkflowState;

    protected CaseFile(WmNode node) {
        Assert.notNull(node);
        this.node = node;
    }

    @Override
    public CaseFile clone() {
        try {
            return (CaseFile) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Failed to clone object: " + toString());
        }
    }

    public boolean isStatus(DocListUnitStatus status) {
        return status != null && status.getValueName().equals(getProp(DocumentDynamicModel.Props.STATUS));
    }

    public String getStatus() {
        return (String) getProp(DocumentDynamicModel.Props.STATUS);
    }

    @Override
    public String getTitle() {
        return StringUtils.defaultString((String) getProp(DocumentDynamicModel.Props.DOC_TITLE));
    }

    @Override
    public String getType() {
        return getDocumentAdminService().getCaseFileTypeName(getNode());
    }

    public String getDueDateStr() {
        return getDateProperty(DocumentDynamicModel.Props.WORKFLOW_DUE_DATE);
    }

    public Date getDueDate() {
        return getProp(DocumentDynamicModel.Props.WORKFLOW_DUE_DATE);
    }

    @Override
    public Date getValidFrom() {
        return getProp(DocumentDynamicModel.Props.VALID_FROM);
    }

    public String getValidFromStr() {
        return getDateProperty(DocumentDynamicModel.Props.VALID_FROM);
    }

    public Date getValidTo() {
        return getProp(DocumentDynamicModel.Props.VALID_TO);
    }

    public String getValidToStr() {
        return getDateProperty(DocumentDynamicModel.Props.VALID_TO);
    }

    public String getOwnerName() {
        return getProp(DocumentDynamicModel.Props.OWNER_NAME);
    }

    @Override
    public String getVolumeMark() {
        String volumeMark = getProp(DocumentDynamicModel.Props.VOLUME_MARK);
        return volumeMark == null ? "" : volumeMark;
    }

    private String getDateProperty(QName qname) {
        Date dueDate = (Date) getProp(qname);
        if (dueDate == null) {
            return null;
        }
        return dateFormat.format(dueDate);
    }

    @Override
    public String getFunctionLabel() {
        if (getFunctionNodeRef() != null) {
            Function function = BeanHelper.getFunctionsService().getFunctionByNodeRef(getFunctionNodeRef());
            if (function != null) {
                return DocumentLocationGenerator.getFunctionLabel(function);
            }
        }
        return null;
    }

    public NodeRef getFunctionNodeRef() {
        return (NodeRef) getNode().getProperties().get(DocumentCommonModel.Props.FUNCTION);
    }

    public NodeRef getSeriesNodeRef() {
        return (NodeRef) getNode().getProperties().get(DocumentCommonModel.Props.SERIES);
    }

    @Override
    public String getSeriesLabel() {
        if (getSeriesNodeRef() != null) {
            Series series = BeanHelper.getSeriesService().getSeriesByNodeRef(getSeriesNodeRef());
            if (series != null) {
                return DocumentLocationGenerator.getSeriesLabel(series);
            }
        }
        return null;
    }

    public String getWorkflowsStatus() {
        if (compoundWorkflowState == null) {
            compoundWorkflowState = WorkflowUtil.getCompoundWorkflowsState(BeanHelper.getWorkflowService().getCompoundWorkflows(super.getNodeRef()), true);
        }
        return compoundWorkflowState;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String getHierarchicalKeywords() {
        return TextUtil.joinStringLists((List<String>) node.getProperties().get(DocumentDynamicModel.Props.FIRST_KEYWORD_LEVEL),
                (List<String>) node.getProperties().get(DocumentDynamicModel.Props.SECOND_KEYWORD_LEVEL));
    }

    public PropsConvertedMap propsConvertedMap;

    @Override
    public Map<String, Object> getUnitStrucPropsConvertedMap() {
        if (propsConvertedMap == null) {
            if (getNode() == null) {
                return null;
            }
            propsConvertedMap = new PropsConvertedMap(getNode().getProperties(), true);
        }
        return propsConvertedMap;
    }

    public PropsConvertedMap convertedPropsMap;

    @Override
    public Map<String, Object> getConvertedPropsMap() {
        if (convertedPropsMap == null) {
            if (getNode() == null) {
                return null;
            }
            convertedPropsMap = new PropsConvertedMap(getNode().getProperties(), false);
        }
        return convertedPropsMap;
    }

    @Override
    public Map<String, Object> getProperties() {
        return node != null ? node.getProperties() : null;
    }

    @Override
    public int compareTo(VolumeOrCaseFile other) {
        if (StringUtils.equalsIgnoreCase(getVolumeMark(), other.getVolumeMark())) {
            String title = getTitle();
            String title2 = other.getTitle();
            if (title == null && title2 == null) {
                return 0;
            } else if (title == null && title2 != null) {
                return 1;
            } else if (title != null && title2 == null) {
                return 1;
            }
            return AppConstants.DEFAULT_COLLATOR.compare(title, title2);
        }
        return DEFAULT_COLLATOR.compare(getVolumeMark(), other.getVolumeMark());
    }

    @Override
    public boolean isDynamic() {
        return true;
    }

    // ----- Same as EventPlanVolume

    public NodeRef getEventPlan() {
        return (NodeRef) getProp(EventPlanModel.Props.EVENT_PLAN);
    }

    public void setEventPlan(NodeRef eventPlan) {
        setProp(EventPlanModel.Props.EVENT_PLAN, eventPlan);
    }

    public String getNextEvent() {
        return getProp(EventPlanModel.Props.NEXT_EVENT);
    }

    public void setNextEvent(String nextEvent) {
        setProp(EventPlanModel.Props.NEXT_EVENT, nextEvent);
    }

    public Date getNextEventDate() {
        return getProp(EventPlanModel.Props.NEXT_EVENT_DATE);
    }

    public void setNextEventDate(Date nextEventDate) {
        setProp(EventPlanModel.Props.NEXT_EVENT_DATE, nextEventDate);
    }

    public boolean isMarkedForTransfer() {
        return getPropBoolean(EventPlanModel.Props.MARKED_FOR_TRANSFER);
    }

    public void setMarkedForTransfer(boolean markedForTransfer) {
        setProp(EventPlanModel.Props.MARKED_FOR_TRANSFER, markedForTransfer);
    }

    public boolean isExportedForUam() {
        return getPropBoolean(EventPlanModel.Props.EXPORTED_FOR_UAM);
    }

    public void setExportedForUam(boolean exportedForUam) {
        setProp(EventPlanModel.Props.EXPORTED_FOR_UAM, exportedForUam);
    }

    public Date getExportedForUamDateTime() {
        return getProp(EventPlanModel.Props.EXPORTED_FOR_UAM_DATE_TIME);
    }

    public void setExportedForUamDateTime(Date exportedForUamDateTime) {
        setProp(EventPlanModel.Props.EXPORTED_FOR_UAM_DATE_TIME, exportedForUamDateTime);
    }

    public boolean isTransferConfirmed() {
        return getPropBoolean(EventPlanModel.Props.TRANSFER_CONFIRMED);
    }

    public void setTransferConfirmed(boolean transferConfirmed) {
        setProp(EventPlanModel.Props.TRANSFER_CONFIRMED, transferConfirmed);
    }

    public Date getTransferedDateTime() {
        return getProp(EventPlanModel.Props.TRANSFERED_DATE_TIME);
    }

    public void setTransferedDateTime(Date transferedDateTime) {
        setProp(EventPlanModel.Props.TRANSFERED_DATE_TIME, transferedDateTime);
    }

    public boolean isMarkedForDestruction() {
        return getPropBoolean(EventPlanModel.Props.MARKED_FOR_DESTRUCTION);
    }

    public void setMarkedForDestruction(boolean markedForDestruction) {
        setProp(EventPlanModel.Props.MARKED_FOR_DESTRUCTION, markedForDestruction);
    }

    public boolean isDisposalActCreated() {
        return getPropBoolean(EventPlanModel.Props.DISPOSAL_ACT_CREATED);
    }

    public void setDisposalActCreated(boolean disposalActCreated) {
        setProp(EventPlanModel.Props.DISPOSAL_ACT_CREATED, disposalActCreated);
    }

    public Date getDisposalDateTime() {
        return getProp(EventPlanModel.Props.DISPOSAL_DATE_TIME);
    }

    public void setDisposalDateTime(Date disposalDateTime) {
        setProp(EventPlanModel.Props.DISPOSAL_DATE_TIME, disposalDateTime);
    }

    // ----- Same as EventPlanCommon

    public final boolean isAppraised() {
        return getPropBoolean(Props.IS_APPRAISED);
    }

    public final void setAppraised(boolean appraised) {
        setProp(Props.IS_APPRAISED, appraised);
    }

    public final boolean isHasArchivalValue() {
        return getPropBoolean(Props.HAS_ARCHIVAL_VALUE);
    }

    public final void setHasArchivalValue(boolean hasArchivalValue) {
        setProp(Props.HAS_ARCHIVAL_VALUE, hasArchivalValue);
    }

    public final boolean isRetainPermanent() {
        return getPropBoolean(Props.RETAIN_PERMANENT);
    }

    public final void setRetainPermanent(boolean retainPermanent) {
        setProp(Props.RETAIN_PERMANENT, retainPermanent);
    }

    public final String getRetaintionStart() {
        return getProp(Props.RETAINTION_START);
    }

    public final void setRetaintionStart(String retaintionStart) {
        setProp(Props.RETAINTION_START, retaintionStart);
    }

    public final Integer getRetaintionPeriod() {
        return getProp(Props.RETAINTION_PERIOD);
    }

    public final void setRetaintionPeriod(Integer retaintionPeriod) {
        setProp(Props.RETAINTION_PERIOD, retaintionPeriod);
    }

    public final String getRetaintionPeriodLabel() {
        if (isRetainPermanent()) {
            return "Alaline";
        } else if (RetaintionStart.FIXED_DATE.name().equals(getRetaintionStart()) && getRetainUntilDate() != null) {
            return "Kuni " + EventPlanCommon.DATE_FORMAT.format(getRetainUntilDate());
        } else if (!RetaintionStart.FIXED_DATE.name().equals(getRetaintionStart()) && getRetaintionPeriod() != null) {
            return getRetaintionPeriod() + " aastat";
        }
        return "Teadmata";
    }

    public final Date getRetainUntilDate() {
        return getProp(Props.RETAIN_UNTIL_DATE);
    }

    public final void setRetainUntilDate(Date retainUntilDate) {
        setProp(Props.RETAIN_UNTIL_DATE, retainUntilDate);
    }

    public final String getArchivingNote() {
        return getProp(Props.ARCHIVING_NOTE);
    }

    public final void setArchivingNote(String archivingNote) {
        setProp(Props.ARCHIVING_NOTE, archivingNote);
    }

}
