package ee.webmedia.alfresco.volume.model;

<<<<<<< HEAD
import static ee.webmedia.alfresco.document.model.Document.dateFormat;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.collections.comparators.NullComparator;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.app.AppConstants;
import ee.webmedia.alfresco.casefile.model.CaseFileModel;
import ee.webmedia.alfresco.classificator.enums.VolumeType;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docconfig.generator.systematic.DocumentLocationGenerator;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.PropsConvertedMap;
import ee.webmedia.alfresco.eventplan.model.EventPlan;
import ee.webmedia.alfresco.eventplan.model.EventPlanVolume;
import ee.webmedia.alfresco.functions.model.Function;
import ee.webmedia.alfresco.series.model.Series;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.TextUtil;
=======
import static ee.webmedia.alfresco.app.AppConstants.DEFAULT_COLLATOR;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;

import ee.webmedia.alfresco.app.AppConstants;
import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.classificator.enums.VolumeType;
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
import ee.webmedia.alfresco.utils.beanmapper.AlfrescoModelProperty;
import ee.webmedia.alfresco.utils.beanmapper.AlfrescoModelType;

@AlfrescoModelType(uri = VolumeModel.URI)
<<<<<<< HEAD
public class Volume extends EventPlanVolume implements VolumeOrCaseFile {
=======
public class Volume implements Serializable, Comparable<Volume> {
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5

    private static final long serialVersionUID = 1L;

    private String volumeType;
<<<<<<< HEAD
    @AlfrescoModelProperty(isMappable = false)
    private String volumeTypeName;
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    private String volumeMark;
    private String title;
    private Date validFrom;
    private Date validTo;
    private String status;
<<<<<<< HEAD
    // FIXME: Alar, milleks see väli? On see üldse kuskil kasutusel?
=======
    private Date dispositionDate;
    // FIXME: milleks see väli? On see üldse kuskil kasutusel?
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    private Date seriesIdentifier;
    private boolean containsCases;
    private boolean casesCreatableByUser;
    private int containingDocsCount;
<<<<<<< HEAD
    private String ownerName;
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    // non-mappable fields
    @AlfrescoModelProperty(isMappable = false)
    private NodeRef seriesNodeRef;

<<<<<<< HEAD
    public Volume() {
        super(null);
    }
=======
    @AlfrescoModelProperty(isMappable = false)
    private Node node;
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5

    public void setVolumeMark(String volumeMark) {
        this.volumeMark = volumeMark;
    }

<<<<<<< HEAD
    @Override
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    public String getVolumeMark() {
        return volumeMark;
    }

<<<<<<< HEAD
    @Override
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    public String getTitle() {
        return title;
    }

<<<<<<< HEAD
    @Override
    public String getType() {
        return "";// used for CaseFile only
    }

=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    public void setTitle(String title) {
        this.title = title;
    }

<<<<<<< HEAD
    public String getVolumeMarkAndTitle() {
        return TextUtil.joinNonBlankStrings(Arrays.asList(volumeMark, title), " ");
    }

    @Override
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    public Date getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(Date validFrom) {
        this.validFrom = validFrom;
    }

<<<<<<< HEAD
    @Override
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    public Date getValidTo() {
        return validTo;
    }

<<<<<<< HEAD
    @Override
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    public void setValidTo(Date validTo) {
        this.validTo = validTo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

<<<<<<< HEAD
=======
    public Date getDispositionDate() {
        return dispositionDate;
    }

    public void setDispositionDate(Date dispositionDate) {
        this.dispositionDate = dispositionDate;
    }

>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    public Date getSeriesIdentifier() {
        return seriesIdentifier;
    }

    public void setSeriesIdentifier(Date seriesIdentifier) {
        this.seriesIdentifier = seriesIdentifier;
    }

    public boolean isContainsCases() {
        return containsCases;
    }

    public void setContainsCases(boolean containsCases) {
        this.containsCases = containsCases;
    }

    public boolean isCasesCreatableByUser() {
        return casesCreatableByUser;
    }

    public void setCasesCreatableByUser(boolean casesCreatableByUser) {
        this.casesCreatableByUser = casesCreatableByUser;
    }

    public int getContainingDocsCount() {
        return Integer.valueOf(containingDocsCount);
    }

    public void setContainingDocsCount(int containingDocsCount) {
        this.containingDocsCount = containingDocsCount;
    }

    public NodeRef getSeriesNodeRef() {
        return seriesNodeRef;
    }

<<<<<<< HEAD
    public void setSeriesNodeRef(NodeRef seriesNodeRef) {
        this.seriesNodeRef = seriesNodeRef;
    }

    public NodeRef getFunctionNodeRef() {
        return (NodeRef) getNode().getProperties().get(DocumentCommonModel.Props.FUNCTION);
    }

    public String getVolumeType() {
        if (volumeType == null && isDynamic()) {
            volumeType = BeanHelper.getDocumentAdminService().getCaseFileTypeName(node);
        }
        return volumeType;
    }

    public String getVolumeTypeName() {
        if (volumeTypeName == null) {
            if (isDynamic()) {
                volumeTypeName = getVolumeType();
            } else if (StringUtils.isNotBlank(volumeType)) {
                volumeTypeName = MessageUtil.getMessage(VolumeType.valueOf(volumeType));
            } else {
                volumeTypeName = "";
            }
        }
        return volumeTypeName;
    }

    public VolumeType getVolumeTypeEnum() {
        return volumeType == null ? null : VolumeType.valueOf(volumeType);
=======
    public void setProperty(String key, Object value) {
        node.getProperties().put(key, value);
    }

    public Object getProperty(String key) {
        return node.getProperties().get(key);
    }

    public void setSeriesNodeRef(NodeRef seriesNodeRef) {
        this.seriesNodeRef = seriesNodeRef;
    }

    public String getVolumeType() {
        return volumeType;
    }

    public VolumeType getVolumeTypeEnum() {
        return VolumeType.valueOf(volumeType);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    }

    public void setVolumeType(String volumeType) {
        this.volumeType = volumeType;
    }

    public void setVolumeTypeEnum(VolumeType volumeType) {
        this.volumeType = volumeType.name();
    }

<<<<<<< HEAD
    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public Date getWorkflowDueDate() {
        return isDynamic() ? (Date) node.getProperties().get(DocumentDynamicModel.Props.WORKFLOW_DUE_DATE) : null;
    }

    public String getWorkflowDueDateStr() {
        return getDateOrNull(getWorkflowDueDate());
    }

    private String getDateOrNull(final Date dueDate) {
        return dueDate != null ? dateFormat.format(dueDate) : "";
    }

    public Date getWorkflowEndDate() {
        return isDynamic() ? (Date) node.getProperties().get(DocumentDynamicModel.Props.WORKFLOW_END_DATE) : null;
    }

    public String getWorkflowEndDateStr() {
        return getDateOrNull(getWorkflowEndDate());
    }

    @Override
    public boolean isDynamic() {
        return node != null && CaseFileModel.Types.CASE_FILE.equals(node.getType());
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

    public String getNextEventLabel() {
        return EventPlan.getNextEventLabel(getNextEvent());
    }

    @Override
    @SuppressWarnings("unchecked")
    public String getHierarchicalKeywords() {
        return TextUtil.joinStringLists((List<String>) node.getProperties().get(DocumentDynamicModel.Props.FIRST_KEYWORD_LEVEL),
                (List<String>) node.getProperties().get(DocumentDynamicModel.Props.SECOND_KEYWORD_LEVEL));
    }

    @AlfrescoModelProperty(isMappable = false)
    private PropsConvertedMap convertedPropsMap;

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

    @AlfrescoModelProperty(isMappable = false)
    private PropsConvertedMap propsConvertedMap;

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

    // Keit: JRebel went mad if i did not add these two setters
    public void setConvertedPropsMap(PropsConvertedMap convertedPropsMap) {
        this.convertedPropsMap = convertedPropsMap;
    }

    public void setUnitStrucPropsConvertedMap(PropsConvertedMap propsConvertedMap) {
        this.propsConvertedMap = propsConvertedMap;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> getProperties() {
        return node != null ? node.getProperties() : null;
    }

    @Override
    public int compareTo(VolumeOrCaseFile other) {
        final Comparator<String> stringComparator = new NullComparator(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return AppConstants.DEFAULT_COLLATOR.compare(o1, o2);
            }
        });
        if (StringUtils.equalsIgnoreCase(getVolumeMark(), other.getVolumeMark())) {
            Comparator<VolumeOrCaseFile> comparator = new NullComparator(new Comparator<VolumeOrCaseFile>() {
                @Override
                public int compare(VolumeOrCaseFile o1, VolumeOrCaseFile o2) {
                    return stringComparator.compare(o1.getTitle(), o2.getTitle());
                }
            });
            return comparator.compare(this, other);
        }
        Comparator<VolumeOrCaseFile> comparator = new NullComparator(new Comparator<VolumeOrCaseFile>() {
            @Override
            public int compare(VolumeOrCaseFile o1, VolumeOrCaseFile o2) {
                return stringComparator.compare(o1.getVolumeMark(), o2.getVolumeMark());
            }
        });
        return comparator.compare(this, other);
    }

    public final String getRetaintionDescription() {
        if (isRetainPermanent()) {
            return "Alaline";
        } else if (!isRetainPermanent() && isHasArchivalValue()) {
            return "Arhiiviväärtuslik";
        } else if (!isRetainPermanent() && !isHasArchivalValue()) {
            return "Tähtajaline";
        }
        return "";
=======
    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public boolean isDisposed() {
        return DocListUnitStatus.DESTROYED.getValueName().equals(status) ||
                (dispositionDate != null && dispositionDate.after(DateUtils.truncate(new Date(), Calendar.DATE)));
    }

    @Override
    public int compareTo(Volume other) {
        if (StringUtils.equalsIgnoreCase(getVolumeMark(), other.getVolumeMark())) {
            return AppConstants.DEFAULT_COLLATOR.compare(getTitle(), other.getTitle());
        }
        return DEFAULT_COLLATOR.compare(getVolumeMark(), other.getVolumeMark());
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    }

    @Override
    public String toString() {
        return new StringBuilder("Volume:")//
                .append("\n\tvolumeMark = " + volumeMark)
                .append("\n\ttitle = " + title)
                .append("\n\tcontainsCases = " + containsCases)
                .append("\n\tvalidFrom = " + validFrom)
                .append("\n\tvalidTo = " + validTo)
<<<<<<< HEAD
                .append("\n\tstatus = " + status).toString();
=======
                .append("\n\tstatus = " + status)
                .append("\n\tdispositionDate = " + dispositionDate).toString();
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    }

}
