package ee.webmedia.mobile.alfresco;

import static ee.webmedia.alfresco.common.web.BeanHelper.getAddressbookService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getAuthorityService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getSubstituteService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getUserContactGroupSearchBean;
import static ee.webmedia.alfresco.common.web.BeanHelper.getUserService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.addressbook.model.AddressbookModel.Types;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.UserContactGroupSearchBean;
import ee.webmedia.alfresco.substitute.model.Substitute;
import ee.webmedia.alfresco.substitute.model.SubstitutionInfo;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.mobile.alfresco.common.AbstractBaseController;
import ee.webmedia.mobile.alfresco.common.model.MessageItem;
import ee.webmedia.mobile.alfresco.workflow.TaskRowMapper;
import ee.webmedia.mobile.alfresco.workflow.model.QueryString;
import ee.webmedia.mobile.alfresco.workflow.model.Task;
import ee.webmedia.mobile.alfresco.workflow.model.TaskContainer;
import ee.webmedia.mobile.alfresco.workflow.model.UserItem;

/**
 * Controller for the main screen in mDelta displaying current users tasks.
 */
@Controller
@RequestMapping("**")
public class HomeController extends AbstractBaseController {

    public static final String TASK_TYPE_ATTR = "taskType";
    public static final String REDIRECT_UNAVAILABLE = "redirectUnavailable";
    private static final String NEUTRAL_MESSAGE = "NEUTRAL";
    private static final FastDateFormat SUBSTITUTION_DATE_FORMAT = FastDateFormat.getInstance("dd.MM.yyyy");
    private static final int SUGGESTER_LIMIT = 12;
    private static final String HOME_VIEW = "home";

    private static final long serialVersionUID = 1L;

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(HomeController.class);

    // FIXME STYLESHEET GOES THROUGH FACES FILTER

    @RequestMapping(method = RequestMethod.GET)
    public String home(Model model, HttpServletRequest request) {
        setup(model, request);
        setPageTitle(model, translate("site.home.myTasks"));
        addContainers(model, updateTaskCounts(model, new TreeMap<String, TaskContainer>(new TaskBlockOrderComparator()), null));
        addRedirectUnavailableMessage(request);
        addSubstitutionInfo(model);
        return HOME_VIEW;
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
        updateTaskCounts(model, containers, getTaskViews(taskTypes));
        addContainers(model, containers);
        addSubstitutionInfo(model);
        return HOME_VIEW;
    }

    @RequestMapping(value = "/ajax/search/users", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody List<UserItem> searchUsers(@RequestParam String q, HttpServletRequest request) {
        boolean withoutCurrenUser = Boolean.valueOf(StringUtils.trimToEmpty(request.getParameter("withoutCurrentUser")));
        List<Pair<String, String>> users = withoutCurrenUser ? getUserService().searchUserNamesAndIdsWithoutCurrentUser(q, SUGGESTER_LIMIT) : getUserService()
                .searchUserNamesAndIds(q, SUGGESTER_LIMIT);
        List<UserItem> userItems = new ArrayList<>();
        for (Pair<String, String> user : users) {
            userItems.add(new UserItem(user.getFirst(), user.getSecond()));
        }
        return userItems;
    }

    @RequestMapping(value = "/ajax/search/all", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody List<UserItem> searchAllPossibleAssignees(@RequestParam String q) {
        List<UserItem> userItems = searchUsersGroupsAndContacts(q);
        return userItems;
    }

    private List<UserItem> searchUsersGroupsAndContacts(String param) {
        int totalLimit = SUGGESTER_LIMIT;
        List<UserItem> result = new ArrayList<>(totalLimit);
        List<Pair<String, String>> users = getUserService().searchUserNamesAndIds(param, totalLimit);
        addSelectItems(result, users, UserContactGroupSearchBean.USERS_FILTER);
        totalLimit -= users.size();
        if (totalLimit > 0) {
            List<Pair<String, String>> groups = getUserContactGroupSearchBean().searchGroups(param, totalLimit);
            addSelectItems(result, groups, UserContactGroupSearchBean.USER_GROUPS_FILTER);
            totalLimit -= groups.size();
        }
        if (totalLimit > 0) {
            List<Pair<String, String>> contacts = getAddressbookService().searchTaskCapableContacts(param, totalLimit);
            addSelectItems(result, contacts, UserContactGroupSearchBean.CONTACTS_FILTER);
            totalLimit -= contacts.size();
        }
        if (totalLimit > 0) {
            List<Pair<String, String>> contactGroups = getAddressbookService().searchTaskCapableContactGroups(param, totalLimit);
            addSelectItems(result, contactGroups, UserContactGroupSearchBean.CONTACT_GROUPS_FILTER);
        }
        return result;
    }

    @RequestMapping(value = "/ajax/search/groupmembers", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody List<UserItem> searchGroupMembers(@RequestBody QueryString query) {
        List<UserItem> members = new ArrayList<>();
        String q = query.getQuery();
        if (StringUtils.startsWithIgnoreCase(q, "group")) {
            UserService userService = getUserService();
            AuthorityService authorityService = getAuthorityService();
            Set<String> authorities = authorityService.getContainedAuthorities(AuthorityType.USER, q, true);
            for (String authority : authorities) {
                String id = authorityService.getShortName(authority);
                String name = userService.getUserFullName(id);
                members.add(new UserItem(name, id));
            }
        } else if (NodeRef.isNodeRef(q)) {
            NodeRef groupRef = new NodeRef(q);
            List<Node> orgNodes = getAddressbookService().getContactsByType(Types.ORGANIZATION, groupRef);
            for (Node orgNode : orgNodes) {
                String name = (String) orgNode.getProperties().get(AddressbookModel.Props.ORGANIZATION_NAME);
                UserItem userItem = new UserItem(name, orgNode.getNodeRefAsString());
                userItem.setUserItemFilterType(UserContactGroupSearchBean.CONTACTS_FILTER);
                members.add(userItem);
            }
        }
        return members;
    }

    private void addSubstitutionInfo(Model model) {
        List<Substitute> substitutes = getSubstituteService().searchActiveSubstitutionDuties(AuthenticationUtil.getFullyAuthenticatedUser());
        if (CollectionUtils.isEmpty(substitutes)) {
            return;
        }
        Map<String, Object> attributes = model.asMap();
        @SuppressWarnings("unchecked")
        Map<String, List<MessageItem>> messages = (Map<String, List<MessageItem>>) attributes.get(MESSAGES_ATTR);
        if (messages == null) {
            messages = new HashMap<>();
        }
        List<MessageItem> substitutionMessages = new ArrayList<>();
        SubstitutionInfo info = BeanHelper.getSubstitutionBean().getSubstitutionInfo();
        if (info.isSubstituting()) {
            Substitute s = info.getSubstitution();
            String replaced = getUserService().getUserFullName(s.getReplacedPersonUserName());
            String msg = translate("site.home.substituting", replaced);
            String actionLabel = translate("site.home.to.my.view");
            substitutionMessages.add(new MessageItem(msg, actionLabel, "", "substitutionLink"));
        } else {
            String actionLabel = translate("site.home.to.substitution.view");
            for (Substitute s : substitutes) {
                String replaced = getUserService().getUserFullName(s.getReplacedPersonUserName());
                String startDate = SUBSTITUTION_DATE_FORMAT.format(s.getSubstitutionStartDate());
                String endDate = SUBSTITUTION_DATE_FORMAT.format(s.getSubstitutionEndDate());
                String msg = translate("site.home.active.substitution", replaced, startDate, endDate);
                String href = s.getReplacedPersonUserName();
                substitutionMessages.add(new MessageItem(msg, actionLabel, href, "substitutionLink"));
            }
        }
        messages.put(NEUTRAL_MESSAGE, substitutionMessages);
    }

    @ResponseStatus(value = HttpStatus.OK)
    @RequestMapping(value = "/ajax/substitute", method = RequestMethod.POST)
    public void handleSubstitutionEvent(HttpServletRequest request) {
        String userToSubstitute = request.getParameter("userName");
        List<Substitute> substitutes = getSubstituteService().searchActiveSubstitutionDuties(AuthenticationUtil.getFullyAuthenticatedUser());
        Substitute substitute = null;
        for (Substitute s : substitutes) {
            String replacedPerson = s.getReplacedPersonUserName();
            if (StringUtils.equals(userToSubstitute, replacedPerson)) {
                substitute = s;
                break;
            }
        }
        BeanHelper.getSubstitutionBean().selectSubstitution(substitute);
    }

    private void addSelectItems(List<UserItem> result, List<Pair<String, String>> items, int type) {
        for (Pair<String, String> res : items) {
            UserItem item = new UserItem(res.getFirst(), res.getSecond());
            item.setUserItemFilterType(type);
            result.add(item);
        }
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
