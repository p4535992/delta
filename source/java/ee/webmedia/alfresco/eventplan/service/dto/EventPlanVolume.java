package ee.webmedia.alfresco.eventplan.service.dto;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Date;

import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.comparators.ComparatorChain;
import org.apache.commons.collections.comparators.NullComparator;
import org.apache.commons.collections.comparators.TransformingComparator;

import ee.webmedia.alfresco.app.AppConstants;

public class EventPlanVolume implements Serializable, Comparable<EventPlanVolume> {
    private static final long serialVersionUID = 1L;

    private final NodeRef nodeRef;
    private final NodeRef seriesRef;
    private final NodeRef functionRef;
    private final String volumeMark;
    private final String title;
    private final Date validFrom;
    private final Date validTo;
    private final String status;
    private final String volumeType;
    private final String ownerName;
    private final String location;
    private final String series;
    private final String function;
    private final boolean dynamic;

    public EventPlanVolume(NodeRef nodeRef, NodeRef seriesRef, NodeRef functionRef, String volumeMark, String title, Date validFrom,
                           Date validTo, String status, String volumeType, String ownerName, String location, String series, String function, boolean dynamic) {
        this.nodeRef = nodeRef;
        this.seriesRef = seriesRef;
        this.functionRef = functionRef;
        this.volumeMark = volumeMark;
        this.title = title;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.status = status;
        this.volumeType = volumeType;
        this.ownerName = ownerName;
        this.location = location;
        this.series = series;
        this.function = function;
        this.dynamic = dynamic;
    }

    public final NodeRef getNodeRef() {
        return nodeRef;
    }

    public final NodeRef getSeriesRef() {
        return seriesRef;
    }

    public final NodeRef getFunctionRef() {
        return functionRef;
    }

    public final String getVolumeMark() {
        return volumeMark;
    }

    public final String getTitle() {
        return title;
    }

    public final Date getValidFrom() {
        return validFrom;
    }

    public final Date getValidTo() {
        return validTo;
    }

    public final String getStatus() {
        return status;
    }

    public final String getVolumeType() {
        return volumeType;
    }

    public final String getOwnerName() {
        return ownerName;
    }

    public final String getLocation() {
        return location;
    }

    public final String getSeries() {
        return series;
    }

    public final String getFunction() {
        return function;
    }

    public boolean isDynamic() {
        return dynamic;
    }

    @Override
    public int compareTo(EventPlanVolume other) {
        return COMPARATOR.compare(this, other);
    }

    public static final Comparator<EventPlanVolume> COMPARATOR;
    static {
        // ComparatorChain is not thread-safe at construction time, but it is thread-safe to perform multiple comparisons after all the setup operations are
        // complete.
        ComparatorChain chain = new ComparatorChain();
        chain.addComparator(new TransformingComparator(new Transformer() {
            @Override
            public Object transform(Object input) {
                return ((EventPlanVolume) input).getValidFrom();
            }
        }, new NullComparator()));
        chain.addComparator(new TransformingComparator(new Transformer() {
            @Override
            public Object transform(Object input) {
                return ((EventPlanVolume) input).getVolumeMark();
            }
        }, new NullComparator(AppConstants.DEFAULT_COLLATOR)));
        chain.addComparator(new TransformingComparator(new Transformer() {
            @Override
            public Object transform(Object input) {
                return ((EventPlanVolume) input).getTitle();
            }
        }, new NullComparator(AppConstants.DEFAULT_COLLATOR)));
        COMPARATOR = chain;
    }

}
