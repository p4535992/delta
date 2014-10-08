package ee.webmedia.mobile.alfresco;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.alfresco.service.namespace.QName;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ee.webmedia.mobile.alfresco.common.AbstractBaseController;
import ee.webmedia.mobile.alfresco.workflow.TaskRowMapper;
import ee.webmedia.mobile.alfresco.workflow.model.Task;
import ee.webmedia.mobile.alfresco.workflow.model.TaskContainer;

/**
 * Controller for the main screen in mDelta displaying current users tasks.
 */
@Controller
@RequestMapping("**")
public class HomeController extends AbstractBaseController {

    public static final String TASK_TYPE_ATTR = "taskType";
    public static final String REDIRECT_UNAVAILABLE = "redirectUnavailable";

    private static final long serialVersionUID = 1L;

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(HomeController.class);

    public static final String REDIRECT_FROM_FINISH_TASK_ATTR = "redirectFromFinishTask";

    // FIXME STYLESHEET GOES THROUGH FACES FILTER

    @RequestMapping(method = RequestMethod.GET)
    public String home(Model model, HttpServletRequest request) {
        setup(model, request);
        setPageTitle(model, translate("site.home.myTasks"));
        addContainers(model, updateTaskCounts(model, new TreeMap<String, TaskContainer>(new TaskBlockOrderComparator()), null));
        addRedirectUnavailableMessage(request);
        return "home";
    }

    private void addRedirectUnavailableMessage(HttpServletRequest request) {
        HttpSession session = request.getSession();
        String message = (String) session.getAttribute(REDIRECT_UNAVAILABLE);
        if (message != null) {
            addWarnMessage(translate(message));
            session.removeAttribute(REDIRECT_UNAVAILABLE);
        }
    }

    // TODO change when proper URL scheme is determined
    @RequestMapping("/tasks/{type}")
    public String tasks(Model model, @PathVariable String type, @RequestParam(value = "ajax", required = false) String ajax, HttpServletRequest request) {
        if (ajax == null) {
            setup(model, request);
            setPageTitle(model, translate("site.home.myTasks"));
        }
        Set<QName> taskTypes = resolveQname(type);

        // Handle invalid type queries by redirecting to front page
        if (taskTypes.size() == 0) {
            return redirect("/");
        }

        TreeMap<String, TaskContainer> containers = new TreeMap<String, TaskContainer>(new TaskBlockOrderComparator());
        List<Task> activeContainerTasks = documentSearchService.searchCurrentUsersTasksInProgress(new TaskRowMapper<Task>(messageSource),
                taskTypes.toArray(new QName[taskTypes.size()]));

        for (final QName taskType : taskTypes) {
            Collection<Task> tasks = CollectionUtils.select(activeContainerTasks, new Predicate<Task>() {

                @Override
                public boolean evaluate(Task task) {
                    return taskType.equals(task.getType());
                }
            });

            if (tasks.isEmpty()) {
                continue;
            }

            TaskContainer container = new TaskContainer(translate("workflow.task.type." + taskType.getLocalName()), type, tasks.size());
            container.setExpanded(true);
            Collections.sort((List<Task>) tasks);
            container.setTasks(tasks);
            containers.put(getTaskView(taskType), container);
        }

        // Return only the requested blocks for AJAX requests
        if (ajax != null) {
            model.addAttribute("containers", containers);
            return "ajax/task-container";
        }

        // Add model objects
        Boolean redirectFromFinishTask = (Boolean) model.asMap().get(REDIRECT_FROM_FINISH_TASK_ATTR);
        if (Boolean.TRUE.equals(redirectFromFinishTask)) {
            updateTaskCounts(model, containers, getTaskViews(taskTypes));
        }
        addContainers(model, containers);
        return "home";
    }

    private Set<String> getTaskViews(Set<QName> taskTypes) {
        Set<String> taskViews = new HashSet<String>();
        for (QName taskType : taskTypes) {
            taskViews.add(getTaskView(taskType));
        }
        return taskViews;
    }

    private String getTaskView(QName taskType) {
        for (Map.Entry<String, Set<QName>> entry : TASK_TYPE_MAPPING.entrySet()) {
            if (entry.getValue().contains(taskType)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public void addContainers(Model model, Map<String, TaskContainer> containers) {
        if (!containers.isEmpty()) {
            model.addAttribute("containers", containers);
        } else {
            addInfoMessage(translate("site.home.myTasks.noTasksFound"));
        }
    }

    public Map<String, TaskContainer> updateTaskCounts(Model model, Map<String, TaskContainer> containers, Set<String> activeTaskTypes) {
        // Update task number for other blocks
        @SuppressWarnings("unchecked")
        Map<QName, Integer> taskCounts = (Map<QName, Integer>) getFromPageModel(model, TASK_COUNT_BY_TYPE);
        for (Entry<String, Set<QName>> entry : TASK_TYPE_MAPPING.entrySet()) {
            String key = entry.getKey();
            Set<QName> currentTypes = entry.getValue();
            if (currentTypes.size() > 1) {
                continue; // Ignore combined mappings
            }

            QName currentType = currentTypes.iterator().next();
            Integer count = taskCounts.get(currentType);
            if (count == null || count < 1) {
                containers.remove(key); // Remove empty containers
                continue;
            }

            boolean isActive = activeTaskTypes != null && activeTaskTypes.contains(key);
            if (isActive) {
                // Task container must be already up to date
                continue;
            }

            String title = translate("workflow.task.type." + currentType.getLocalName());
            TaskContainer tc = new TaskContainer(title, key, count);
            tc.setExpanded(isActive);
            containers.put(key, tc);
        }

        return containers;
    }

    private Set<QName> resolveQname(String type) {
        if (StringUtils.isBlank(type) || !TASK_TYPE_MAPPING.containsKey(type)) {
            return new HashSet<QName>();
        }

        return TASK_TYPE_MAPPING.get(type);
    }

    @Override
    protected boolean isMenuEnabled() {
        return false;
    }

    /**
     * Determines the order of task blocks based on their key.
     */
    private static class TaskBlockOrderComparator implements Comparator<String> {

        private static final List<String> TASK_ORDER = Arrays.asList("assignment", "group-assignment", "information", "order-assignment",
                "opinion", "review", "external-review", "confirmation", "task-due-extension", "signature");

        @Override
        public int compare(String o1, String o2) {
            return TASK_ORDER.indexOf(o1) < TASK_ORDER.indexOf(o2) ? -1 : 1;
        }

    }
}
