package ee.webmedia.alfresco.workflow.service;

import static ee.webmedia.alfresco.workflow.web.DelegationHistoryGenerator.TMP_CO_OWNER;
import static ee.webmedia.alfresco.workflow.web.DelegationHistoryGenerator.TMP_MAIN_OWNER;
import static ee.webmedia.alfresco.workflow.web.DelegationHistoryGenerator.TMP_STYLE_CLASS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.collections.comparators.ComparatorChain;
import org.apache.commons.collections.comparators.NullComparator;
import org.apache.commons.collections.comparators.TransformingComparator;

import ee.webmedia.alfresco.app.AppConstants;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.utils.ComparableTransformer;

public class DelegationHistoryUtil {

    public static final Comparator<Task> COMPARATOR;
    static {
        COMPARATOR = getTaskComparator();
    }

    private static Comparator<Task> getTaskComparator() {
        ComparatorChain chain = new ComparatorChain();
        chain.addComparator(new TransformingComparator(new ComparableTransformer<Task>() {
            @Override
            public Comparable<?> tr(Task input) {
                return input.getStartedDateTime();
            }
        }, new NullComparator()));
        chain.addComparator(new TransformingComparator(new ComparableTransformer<Task>() {
            @Override
            public Comparable<?> tr(Task input) {
                return input.getDueDate();
            }
        }, new NullComparator()));
        chain.addComparator(new TransformingComparator(new ComparableTransformer<Task>() {
            @Override
            public Comparable<?> tr(Task input) {
                return input.getOwnerName();
            }
        }, new NullComparator(AppConstants.getNewCollatorInstance())));
        @SuppressWarnings("unchecked")
        Comparator<Task> tmp = chain;
        return tmp;
    }

    public static List<Node> getDelegationNodes(NodeRef delegatableTask, List<Task> tasks4History) {
        return getDelegationNodes(Arrays.asList(delegatableTask), tasks4History);
    }

    public static List<Node> getDelegationNodes(List<NodeRef> delegatableTasks, List<Task> tasks4History) {
        Collections.sort(tasks4History, COMPARATOR);
        List<Node> delegationHistories = new ArrayList<>(tasks4History.size());
        for (Task task : tasks4History) {
            WmNode taskNode = task.getNode();
            final QName mainOrCoOwner = task.isResponsible() ? TMP_MAIN_OWNER : TMP_CO_OWNER;
            Map<String, Object> props = taskNode.getProperties();
            if (delegatableTasks.contains(task.getNodeRef())) {
                props.put(TMP_STYLE_CLASS.toString(), "bold");
            }
            props.put(mainOrCoOwner.toString(), task.getOwnerName());
            delegationHistories.add(taskNode);
        }
        return delegationHistories;
    }

}
