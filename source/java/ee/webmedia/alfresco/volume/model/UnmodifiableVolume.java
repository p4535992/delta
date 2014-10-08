package ee.webmedia.alfresco.volume.model;

import static ee.webmedia.alfresco.utils.RepoUtil.getProp;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Date;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.collections.comparators.NullComparator;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.app.AppConstants;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.eventplan.model.EventPlanModel;

public class UnmodifiableVolume implements Serializable, Comparable<UnmodifiableVolume> {

    private static final long serialVersionUID = 1L;

    private final String volumeMark;
    private final String title;
    private final Date validFrom;
    private final Date validTo;
    private final String status;
    private final NodeRef nodeRef;
    private final String volumeLabel;
    private final boolean isDynamic;
    private final boolean containsCases;
    private final String volumeType;
    private final Date retainUntilDate;
    private final boolean casesCreatebleByUser;
    private final Long shortRegNumber;
    private final int containingDocsCount;
    private final String ownerName;

    public UnmodifiableVolume(Node node, boolean isDynamic) {
        volumeMark = getProp(VolumeModel.Props.VOLUME_MARK, node);
        title = getProp(VolumeModel.Props.TITLE, node);
        volumeLabel = volumeMark + " " + title;
        validFrom = getProp(VolumeModel.Props.VALID_FROM, node);
        validTo = getProp(VolumeModel.Props.VALID_TO, node);
        status = getProp(VolumeModel.Props.STATUS, node);
        if (isDynamic) {
            volumeType = getProp(DocumentAdminModel.Props.OBJECT_TYPE_ID, node);
        } else {
            volumeType = getProp(VolumeModel.Props.VOLUME_TYPE, node);
        }
        containsCases = Boolean.TRUE.equals(getProp(VolumeModel.Props.CONTAINS_CASES, node));
        retainUntilDate = getProp(EventPlanModel.Props.RETAIN_UNTIL_DATE, node);
        casesCreatebleByUser = Boolean.TRUE.equals(getProp(VolumeModel.Props.CASES_CREATABLE_BY_USER, node));
        shortRegNumber = getProp(VolumeModel.Props.VOL_SHORT_REG_NUMBER, node);
        Integer containingDocsCountPropValue = getProp(VolumeModel.Props.CONTAINING_DOCS_COUNT, node);
        containingDocsCount = containingDocsCountPropValue != null ? containingDocsCountPropValue : 0;
        ownerName = getProp(DocumentDynamicModel.Props.OWNER_NAME, node);
        nodeRef = node.getNodeRef();
        this.isDynamic = isDynamic;
    }

    public String getVolumeMark() {
        return volumeMark;
    }

    public String getTitle() {
        return title;
    }

    public Date getValidFrom() {
        return validFrom;
    }

    public Date getValidTo() {
        return validTo;
    }

    public String getStatus() {
        return status;
    }

    public NodeRef getNodeRef() {
        return nodeRef;
    }

    public boolean isDynamic() {
        return isDynamic;
    }

    public String getVolumeLabel() {
        return volumeLabel;
    }

    public String getVolumeType() {
        if (StringUtils.isBlank(volumeType) || !isDynamic) {
            return volumeType;
        }
        return BeanHelper.getDocumentAdminService().getCaseFileTypeName(volumeType);
    }

    public boolean isContainsCases() {
        return containsCases;
    }

    public Date getRetainUntilDate() {
        return retainUntilDate;
    }

    public boolean isCasesCreatableByUser() {
        return casesCreatebleByUser;
    }

    public Long getShortRegNumber() {
        return shortRegNumber;
    }

    public int getContainingDocsCount() {
        return containingDocsCount;
    }

    public String getOwnerName() {
        return ownerName;
    }

    // TODO: method logic is same as in Volume class, could unify this method with Volume.compareTo method
    @Override
    public int compareTo(UnmodifiableVolume other) {
        final Comparator<String> stringComparator = new NullComparator(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return AppConstants.getNewCollatorInstance().compare(o1, o2);
            }
        });
        if (StringUtils.equalsIgnoreCase(volumeMark, other.getVolumeMark())) {
            Comparator<UnmodifiableVolume> comparator = new NullComparator(new Comparator<UnmodifiableVolume>() {
                @Override
                public int compare(UnmodifiableVolume o1, UnmodifiableVolume o2) {
                    return stringComparator.compare(o1.getTitle(), o2.getTitle());
                }
            });
            return comparator.compare(this, other);
        }
        Comparator<UnmodifiableVolume> comparator = new NullComparator(new Comparator<UnmodifiableVolume>() {
            @Override
            public int compare(UnmodifiableVolume o1, UnmodifiableVolume o2) {
                return stringComparator.compare(o1.getVolumeMark(), o2.getVolumeMark());
            }
        });
        return comparator.compare(this, other);
    }

}
