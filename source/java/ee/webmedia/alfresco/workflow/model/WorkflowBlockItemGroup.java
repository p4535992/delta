package ee.webmedia.alfresco.workflow.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.comparators.ComparatorChain;
import org.apache.commons.collections.comparators.NullComparator;
import org.apache.commons.collections.comparators.TransformingComparator;

public class WorkflowBlockItemGroup implements Serializable {
    private static final long serialVersionUID = 1L;
    private List<WorkflowBlockItem> items;
    private int workflowCount;

    public WorkflowBlockItemGroup(List<WorkflowBlockItem> items, int workflowCount) {
        this.items = items;
        this.workflowCount = workflowCount;
    }

    public List<WorkflowBlockItem> getItems() {
        if (items == null) {
            items = new ArrayList<WorkflowBlockItem>(0);
        }
        return items;
    }

    public void setItems(List<WorkflowBlockItem> items) {
        this.items = items;
    }

    public int getWorkflowCount() {
        return workflowCount;
    }

    public void setWorkflowCount(int workflowCount) {
        this.workflowCount = workflowCount;
    }

    public Date getFirstStartDate() {
        WorkflowBlockItem item = (!getItems().isEmpty()) ? getItems().get(0) : null;
        return (item != null) ? item.getStartedDateTime() : null;
    }

    public static final Comparator<WorkflowBlockItemGroup> COMPARATOR;
    static {
        // ComparatorChain is not thread-safe at construction time, but it is thread-safe to perform multiple comparisons after all the setup operations are
        // complete.
        ComparatorChain chain = new ComparatorChain();
        // CL task 168225 - Maiga asked not to delete code
        // for comparing compound workflows by number of workflows
        // as this functionality may be needed again
        // chain.addComparator(new TransformingComparator(new Transformer() {
        // @Override
        // public Object transform(Object input) {
        // return -((WorkflowBlockItemGroup) input).getWorkflowCount();
        // }
        // }, new NullComparator()));
        chain.addComparator(new TransformingComparator(new Transformer() {
            @Override
            public Object transform(Object input) {
                return ((WorkflowBlockItemGroup) input).getFirstStartDate();
            }
        }, new NullComparator()));
        COMPARATOR = chain;
    }

}
