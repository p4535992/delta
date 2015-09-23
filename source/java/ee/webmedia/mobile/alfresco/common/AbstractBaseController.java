package ee.webmedia.mobile.alfresco.common;

import static ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel.Types.ASSIGNMENT_TASK;
import static ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel.Types.CONFIRMATION_TASK;
import static ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel.Types.DUE_DATE_EXTENSION_TASK;
import static ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel.Types.INFORMATION_TASK;
import static ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel.Types.OPINION_TASK;
import static ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel.Types.REVIEW_TASK;
import static ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel.Types.SIGNATURE_TASK;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.RequestContextUtils;

import ee.webmedia.alfresco.common.service.ApplicationService;
import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.utils.UnableToPerformException.MessageSeverity;
import ee.webmedia.mobile.alfresco.common.holder.UserRequestInfo;
import ee.webmedia.mobile.alfresco.menu.model.MenuEntry;
import ee.webmedia.mobile.alfresco.util.Util;

/**
 * Base controller that handles common functionality. All mDelta controllers should extend this class.
 */
public abstract class AbstractBaseController implements Serializable {

    private static final long serialVersionUID = 1L;
    protected static final String MESSAGES_ATTR = "messages";

    public static final String MESSAGES_FLASH_ATTR = "messagesFlashAttribute";
    protected static final String TASK_COUNT_BY_TYPE = "taskCounts";
    private static final String PAGE = "page";
    private static final String UNSEEN_TASK_COUNT = "unseenTaskCount";

    @Autowired
    protected DocumentSearchService documentSearchService;
    @Autowired
    private ApplicationService applicationService;
    @Resource
    protected MessageSource messageSource;
    @Resource
    private UserRequestInfo userRequestInfo;

    protected void setup(Model model) {
        setup(model, null);
    }

    /**
     * Common setup point for functionality that is used on every page.
     * 
     * @param model Model object to populate
     * @param request
     */
    protected void setup(Model model, HttpServletRequest request) {
        setupCommon(model, true);
        addMessagesToModel(model, request);
    }

    protected void setupWithoutSidebarMenu(Model model, HttpServletRequest request) {
        setupCommon(model, false);
        addMessagesToModel(model, request);
    }

    private void setupCommon(Model model, boolean addSidebarMenu) {
        Map<String, Object> page = new HashMap<String, Object>();
        page.put(UNSEEN_TASK_COUNT, documentSearchService.getCurrentUsersUnseenTasksCount(TASK_TYPES));
        Map<QName, Integer> taskCountByType = getTaskCountByType();
        page.put(TASK_COUNT_BY_TYPE, taskCountByType);
        page.put("footerText", applicationService.getMDeltaFooterText());
        page.put("projectVersion", applicationService.getProjectVersion());
        if (addSidebarMenu) {
            page.put("menu", setupSidebarMenu(taskCountByType));
        }
        model.addAttribute(PAGE, page);
    }

    private void addMessagesToModel(Model model, HttpServletRequest request) {
        if (request != null) {
            Map<String, ?> inputFlashMap = RequestContextUtils.getInputFlashMap(request);
            if (inputFlashMap != null) {
                @SuppressWarnings("unchecked")
                List<Pair<MessageSeverity, String>> redirectMessages = (List<Pair<MessageSeverity, String>>) inputFlashMap.get(MESSAGES_FLASH_ATTR);
                if (redirectMessages != null) {
                    for (Pair<MessageSeverity, String> message : redirectMessages) {
                        userRequestInfo.addMessage(message.getSecond(), message.getFirst());
                    }
                }
            }
        }
        model.addAttribute(MESSAGES_ATTR, userRequestInfo.getMessages());
    }

    private Map<QName, Integer> getTaskCountByType() {
        Map<QName, Integer> counts = documentSearchService.getCurrentUserTaskCountByType(TASK_TYPES);
        Map<QName, Integer> result = new HashMap<QName, Integer>(counts.size());

        for (Entry<QName, Integer> entry : counts.entrySet()) {
            Integer count = entry.getValue();
            result.put(entry.getKey(), (count == null ? 0 : count));
        }

        return result;
    }

    private List<MenuEntry> setupSidebarMenu(Map<QName, Integer> taskCountByType) {
        List<MenuEntry> menu = new ArrayList<MenuEntry>();
        if (!isMenuEnabled()) {
            return menu;
        }

        // Add task related entries
        MenuEntry tasks = MenuEntry.parent(translate("site.home.myTasks"));
        for (Entry<String, Set<QName>> mapping : TASK_TYPE_MAPPING.entrySet()) {
            Set<QName> taskTypes = mapping.getValue();
            if (taskTypes.contains(CONFIRMATION_TASK) || taskTypes.contains(DUE_DATE_EXTENSION_TASK)) {
                continue; // These two tasks are mapped to on menu entry
            }
            QName taskType = taskTypes.iterator().next();
            String title = translate("workflow.task.type." + taskType.getLocalName());
            int count = 0;
            for (QName type : mapping.getValue()) {
                count += taskCountByType.get(type) == null ? 0 : taskCountByType.get(type);
            }
            if (count < 1) {
                continue;
            }
            String details = Integer.toString(count);
            String target = "tasks/" + mapping.getKey(); // TODO - Better URL scheme mapping.
            tasks.addSubItem(title, details, target);
        }
        menu.add(tasks);

        return menu;
    }

    /**
     * Allows extending controllers to disable the menu if needed.
     * 
     * @return
     */
    protected boolean isMenuEnabled() {
        return true;
    }

    @SuppressWarnings("unchecked")
    protected String setPageTitle(Model model, String title) {
        Map<String, Object> map;
        if (model.containsAttribute("page")) {
            map = (HashMap<String, Object>) model.asMap().get("page");
        } else {
            map = new HashMap<String, Object>();
        }

        map.put("title", title);
        model.addAttribute("page", map);

        return title;
    }

    @SuppressWarnings("unchecked")
    protected Object getFromPageModel(Model model, String key) {
        Map<String, Object> asMap = model.asMap();
        if (!asMap.containsKey(PAGE)) {
            throw new RuntimeException("Model is not set up yet! See ee.webmedia.mobile.alfresco.common.AbstractBaseController.setup(Model)");
        }

        return ((Map<String, Object>) asMap.get(PAGE)).get(key);

    }

    protected String redirect(String destination) {
        if (StringUtils.isBlank(destination)) {
            destination = "/";
        }
        return "redirect:" + destination;
    }

    public Pair<MessageSeverity, String> addRedirectInfoMsg(RedirectAttributes redirectAttributes, String msgKey, Object... args) {
        return addRedirectMessage(redirectAttributes, msgKey, MessageSeverity.INFO, args);
    }

    public Pair<MessageSeverity, String> addRedirectWarnMsg(RedirectAttributes redirectAttributes, String msgKey, Object... args) {
        return addRedirectMessage(redirectAttributes, msgKey, MessageSeverity.WARN, args);
    }

    public Pair<MessageSeverity, String> addRedirectErrorMsg(RedirectAttributes redirectAttributes, String msgKey, Object... args) {
        return addRedirectMessage(redirectAttributes, msgKey, MessageSeverity.ERROR, args);
    }

    private Pair<MessageSeverity, String> addRedirectMessage(RedirectAttributes redirectAttributes, String msgKey, MessageSeverity severity, Object... args) {
        @SuppressWarnings("unchecked")
        List<Pair<MessageSeverity, String>> messages = (List<Pair<MessageSeverity, String>>) redirectAttributes.getFlashAttributes().get(MESSAGES_FLASH_ATTR);
        if (messages == null) {
            messages = new ArrayList<Pair<MessageSeverity, String>>();
            redirectAttributes.addFlashAttribute(MESSAGES_FLASH_ATTR, messages);
        }
        Pair<MessageSeverity, String> message = Pair.newInstance(severity, translate(msgKey, args));
        messages.add(message);
        return message;
    }

    public String translate(String code, Object... args) {
        return Util.translate(messageSource, code, args);
    }

    protected void addErrorMessage(String messageId, Object... messageValueHolders) {
        userRequestInfo.addMessage(translate(messageId, messageValueHolders), MessageSeverity.ERROR);
    }

    protected void addWarnMessage(String message) {
        userRequestInfo.addMessage(message, MessageSeverity.WARN);
    }

    protected void addInfoMessage(String messageId, Object... messageValueHolders) {
        addInfoMessage(translate(messageId, messageValueHolders));
    }

    protected void addInfoMessage(String message) {
        userRequestInfo.addMessage(message, MessageSeverity.INFO);
    }

    // STATIC

    /**
     * Read-only fields during runtime, synchronization not needed.
     */
    protected static final Map<String, Set<QName>> TASK_TYPE_MAPPING = new TreeMap<>();
    protected static final Map<QName, String> TASK_TYPE_TO_KEY_MAPPING;
    public static final QName[] TASK_TYPES = { ASSIGNMENT_TASK, /* GROUP_ASSIGNMENT_TASK, */INFORMATION_TASK, /* ORDER_ASSIGNMENT_TASK, */OPINION_TASK,
        REVIEW_TASK, /* EXTERNAL_REVIEW_TASK, CONFIRMATION_TASK, DUE_DATE_EXTENSION_TASK, */SIGNATURE_TASK };

    // Also defines sidebar menu order
    static {
        addTypeMapping("assignment", ASSIGNMENT_TASK);
        // addTypeMapping("group-assignment", GROUP_ASSIGNMENT_TASK);
        addTypeMapping("information", INFORMATION_TASK);
        // addTypeMapping("order-assignment", ORDER_ASSIGNMENT_TASK);
        addTypeMapping("opinion", OPINION_TASK);
        addTypeMapping("review", REVIEW_TASK);
        // addTypeMapping("external-review", EXTERNAL_REVIEW_TASK);
        // addTypeMapping("confirmation", CONFIRMATION_TASK);
        // addTypeMapping("due-date-extension", DUE_DATE_EXTENSION_TASK);
        // addTypeMapping("confirmation-and-due-date-extension", CONFIRMATION_TASK, DUE_DATE_EXTENSION_TASK);
        addTypeMapping("signature", SIGNATURE_TASK);

        Map<QName, String> temp = new HashMap<>();
        for (String key : TASK_TYPE_MAPPING.keySet()) {
            for (QName type : TASK_TYPE_MAPPING.get(key)) {
                temp.put(type, key);
            }
        }
        TASK_TYPE_TO_KEY_MAPPING = Collections.unmodifiableMap(temp);
    }

    private static void addTypeMapping(String key, QName... types) {
        if (types == null || types.length < 1) {
            return;
        }
        TASK_TYPE_MAPPING.put(key, new HashSet<>(Arrays.asList(types)));
    }
}