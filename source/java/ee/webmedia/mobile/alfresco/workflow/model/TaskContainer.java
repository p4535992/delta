package ee.webmedia.mobile.alfresco.workflow.model;

import java.util.Collection;


/**
 * Model for logical task groups
 */
public class TaskContainer {

    public TaskContainer() {
        // Default constructor
    }

    public TaskContainer(String title, String target, int count) {
        this.title = title;
        this.target = target;
        this.count = count;
    }

    private String title;
    private Collection<Task> tasks;
    private boolean expanded;
    private int count;
    private String target;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Collection<Task> getTasks() {
        return tasks;
    }

    public void setTasks(Collection<Task> tasks) {
        this.tasks = tasks;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    @Override
    public String toString() {
        return "TaskContainer [title=" + title + ", tasks=" + tasks + ", expanded=" + expanded + ", count=" + count + ", target=" + target + "]";
    }

}
