package ee.webmedia.alfresco.eventplan.service.dto;

import java.io.Serializable;
import java.util.Comparator;

import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.comparators.ComparatorChain;
import org.apache.commons.collections.comparators.NullComparator;
import org.apache.commons.collections.comparators.TransformingComparator;

import ee.webmedia.alfresco.app.AppConstants;

/**
 * @author Martti Tamm
 * @author Alar Kvell
 */
public class EventPlanSeries implements Serializable, Comparable<EventPlanSeries> {
    private static final long serialVersionUID = 1L;

    private final NodeRef nodeRef;
    private final NodeRef functionRef;
    private final int order;
    private final String identifier;
    private final String title;
    private final String status;
    private final String functionMarkAndTitle;
    private final int functionOrder;
    private final String functionMark;
    private final String location;

    public EventPlanSeries(NodeRef nodeRef, NodeRef functionRef, Integer order, String identifier, String title, String status, String functionMarkAndTitle, Integer functionOrder,
                           String functionMark, String location) {
        this.nodeRef = nodeRef;
        this.functionRef = functionRef;
        this.order = order == null ? 0 : order;
        this.identifier = identifier;
        this.title = title;
        this.status = status;
        this.functionMarkAndTitle = functionMarkAndTitle;
        this.functionOrder = functionOrder == null ? 0 : functionOrder;
        this.functionMark = functionMark;
        this.location = location;
    }

    public NodeRef getNodeRef() {
        return nodeRef;
    }

    public NodeRef getFunctionRef() {
        return functionRef;
    }

    public int getOrder() {
        return order;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getTitle() {
        return title;
    }

    public String getStatus() {
        return status;
    }

    public String getFunctionMarkAndTitle() {
        return functionMarkAndTitle;
    }

    public int getFunctionOrder() {
        return functionOrder;
    }

    public String getFunctionMark() {
        return functionMark;
    }

    public String getLocation() {
        return location;
    }

    @Override
    public int compareTo(EventPlanSeries other) {
        return COMPARATOR.compare(this, other);
    }

    public static final Comparator<EventPlanSeries> COMPARATOR;
    static {
        // ComparatorChain is not thread-safe at construction time, but it is thread-safe to perform multiple comparisons after all the setup operations are
        // complete.
        ComparatorChain chain = new ComparatorChain();
        chain.addComparator(new TransformingComparator(new Transformer() {
            @Override
            public Object transform(Object input) {
                return ((EventPlanSeries) input).getFunctionOrder();
            }
        }, new NullComparator()));
        chain.addComparator(new TransformingComparator(new Transformer() {
            @Override
            public Object transform(Object input) {
                return ((EventPlanSeries) input).getFunctionMark();
            }
        }, new NullComparator(AppConstants.DEFAULT_COLLATOR)));
        chain.addComparator(new TransformingComparator(new Transformer() {
            @Override
            public Object transform(Object input) {
                return ((EventPlanSeries) input).getOrder();
            }
        }, new NullComparator()));
        chain.addComparator(new TransformingComparator(new Transformer() {
            @Override
            public Object transform(Object input) {
                return ((EventPlanSeries) input).getIdentifier();
            }
        }, new NullComparator(AppConstants.DEFAULT_COLLATOR)));
        COMPARATOR = chain;
    }

}
