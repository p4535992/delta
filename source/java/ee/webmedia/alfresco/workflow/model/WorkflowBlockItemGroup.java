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
import org.apache.commons.lang.StringUtils;

/**
 * @author Kaarel Jõgeva
 */
public class WorkflowBlockItemGroup implements Serializable {
    private static final long serialVersionUID = 1L;
    private List<WorkflowBlockItem> items;
    private List<WorkflowBlockItem> groupedItems;
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

    public List<WorkflowBlockItem> getGroupedItems() {
        if (items == null) {
            items = new ArrayList<WorkflowBlockItem>(0);
        }
        if (groupedItems == null) {
            groupedItems = new ArrayList<WorkflowBlockItem>(0);
            int itemsSize = items.size();
            WorkflowBlockItem lastGroupItem = null;
            for (int i = 0; i < itemsSize; i++) {
                WorkflowBlockItem item = items.get(i);
                String ownerGroupName = item.getOwnerGroup();
                if (StringUtils.isBlank(ownerGroupName)) {
                    // rows with no group are always displayed as separate row and finish previous group, if one exists
                    if (lastGroupItem != null) {
                        groupedItems.add(lastGroupItem);
                        lastGroupItem = null;
                    }
                    groupedItems.add(item);
                } else {
                    boolean isLastItem = i == itemsSize - 1;
                    // start new group if two items following each other have same group and same workflow
                    boolean needStartNewGroup = false;
                    if (!isLastItem) {
                        WorkflowBlockItem nextItem = items.get(i + 1);
                        needStartNewGroup = item.getOwnerGroup().equals(nextItem.getOwnerGroup()) && item.getWorkflowIndex() == nextItem.getWorkflowIndex();
                    }
                    if (lastGroupItem == null) {
                        lastGroupItem = addItemOrGroupItem(lastGroupItem, item, ownerGroupName, needStartNewGroup);
                    } else {
                        if (lastGroupItem.getGroupName().equals(ownerGroupName) && lastGroupItem.getGroupWorkflowIndex() == item.getWorkflowIndex()) {
                            lastGroupItem.getGroupItems().add(item);
                            if (isLastItem) {
                                groupedItems.add(lastGroupItem);
                                lastGroupItem = null;
                            }
                        } else {
                            groupedItems.add(lastGroupItem);
                            lastGroupItem = null;
                            lastGroupItem = addItemOrGroupItem(lastGroupItem, item, ownerGroupName, needStartNewGroup);
                        }
                    }
                }
            }

        }
        return groupedItems;
    }

    private WorkflowBlockItem addItemOrGroupItem(WorkflowBlockItem lastGroupItem, WorkflowBlockItem item, String ownerGroupName, boolean needStartNewGroup) {
        if (needStartNewGroup) {
            lastGroupItem = new WorkflowBlockItem(ownerGroupName, item.getWorkflowIndex(), item.isRaisedRights());
            lastGroupItem.getGroupItems().add(item);
        } else {
            groupedItems.add(item);
        }
        return lastGroupItem;
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
