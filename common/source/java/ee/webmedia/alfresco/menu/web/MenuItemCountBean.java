package ee.webmedia.alfresco.menu.web;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.faces.context.FacesContext;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.util.Pair;
import org.apache.log4j.Logger;

import ee.webmedia.alfresco.common.service.ConstantNodeRefsBean;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.imap.service.ImapServiceExt;
import ee.webmedia.alfresco.menu.model.MenuItem;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import flexjson.JSONSerializer;

public class MenuItemCountBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private static Logger log = Logger.getLogger(MenuItemCountBean.class);

    public static final String BEAN_NAME = "MenuItemCountBean";
    public static final String MENU_ITEM_ID_PARAM = "menuItemId";

    private int luceneLimit;
    private int maxResults;

    private ConstantNodeRefsBean constantNodeRefsBean;

    private final Map<String, Integer> menuItemCounts = new HashMap<>();

    /** Tasks that are grouped together (tasks in the values don't have a separate menu point and are displayed together with the tasks in keys) */
    private static final Map<String, String> GROUPED_TASKS = new HashMap<>(3);
    /** Maps task ids to menu item ids */
    private static final Map<String, String> TASK_ID_TO_MENU_ITEM_ID = new HashMap<>(8);

    static {
        GROUPED_TASKS.put(WorkflowSpecificModel.Types.CONFIRMATION_TASK.getLocalName(), WorkflowSpecificModel.Types.DUE_DATE_EXTENSION_TASK.getLocalName());
        GROUPED_TASKS.put(WorkflowSpecificModel.Types.ASSIGNMENT_TASK.getLocalName(), WorkflowSpecificModel.Types.GROUP_ASSIGNMENT_TASK.getLocalName());
        GROUPED_TASKS.put(WorkflowSpecificModel.Types.EXTERNAL_REVIEW_TASK.getLocalName(), WorkflowSpecificModel.Types.LINKED_REVIEW_TASK.getLocalName());

        TASK_ID_TO_MENU_ITEM_ID.put(WorkflowSpecificModel.Types.ASSIGNMENT_TASK.getLocalName(), MenuItem.ASSIGNMENT_TASKS);
        TASK_ID_TO_MENU_ITEM_ID.put(WorkflowSpecificModel.Types.CONFIRMATION_TASK.getLocalName(), MenuItem.CONFIRMATION_TASKS);
        TASK_ID_TO_MENU_ITEM_ID.put(WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_TASK.getLocalName(), MenuItem.ORDER_ASSIGNMENT_TASKS);
        TASK_ID_TO_MENU_ITEM_ID.put(WorkflowSpecificModel.Types.INFORMATION_TASK.getLocalName(), MenuItem.INFORMATION_TASKS);
        TASK_ID_TO_MENU_ITEM_ID.put(WorkflowSpecificModel.Types.OPINION_TASK.getLocalName(), MenuItem.OPINION_TASKS);
        TASK_ID_TO_MENU_ITEM_ID.put(WorkflowSpecificModel.Types.REVIEW_TASK.getLocalName(), MenuItem.REVIEW_TASKS);
        TASK_ID_TO_MENU_ITEM_ID.put(WorkflowSpecificModel.Types.EXTERNAL_REVIEW_TASK.getLocalName(), MenuItem.EXTERNAL_REVIEW_TASKS);
        TASK_ID_TO_MENU_ITEM_ID.put(WorkflowSpecificModel.Types.SIGNATURE_TASK.getLocalName(), MenuItem.SIGNATURE_TASKS);
    }

    public void updateMenuItemsCount() throws IOException {
        long startTime = System.currentTimeMillis();
        FacesContext context = FacesContext.getCurrentInstance();
        maxResults = getMaxSearchResultRows();
        luceneLimit = maxResults + 1;
        List<Pair<String, Integer>> counts = new ArrayList<>();
        counts.addAll(getUserCounts());
        if (BeanHelper.getUserService().isDocumentManager()) {
            counts.addAll(getDocManagerCounts());
        }
        if (BeanHelper.getApplicationConstantsBean().isEinvoiceEnabled()) {
            counts.add(getIncomingEincoiceCount());
        }

        Map<String, Integer> taskAndCount = BeanHelper.getWorkflowDbService().countAllCurrentUserTasks();
        for (Entry<String, String> entry : GROUPED_TASKS.entrySet()) {
            if (taskAndCount.containsKey(entry.getValue()) && !taskAndCount.containsKey(entry.getKey())) {
                taskAndCount.put(entry.getKey(), 0);
            }
        }
        List<Map<String, String>> jsonList = new ArrayList<>();
        for (Entry<String, Integer> entry : taskAndCount.entrySet()) {
            String taskType = entry.getKey();
            if (GROUPED_TASKS.containsValue(taskType)) {
                continue;
            }
            int count = entry.getValue();
            if (GROUPED_TASKS.containsKey(taskType)) {
                count += getNumber(taskAndCount, GROUPED_TASKS.get(taskType));
            }
            String menuItemId = TASK_ID_TO_MENU_ITEM_ID.get(taskType);
            createJsonObject(jsonList, menuItemId, count);
            menuItemCounts.put(menuItemId, count);
        }
        for (Pair<String, Integer> pair : counts) {
            String menuItemId = pair.getFirst();
            int count = pair.getSecond();
            createJsonObject(jsonList, menuItemId, count);
            menuItemCounts.put(menuItemId, count);
        }
        context.getResponseWriter().write(new JSONSerializer().serialize(jsonList));
        if (log.isDebugEnabled()) {
            log.debug("PERFORMANCE: MenuItemCountHandler call took " + (System.currentTimeMillis() - startTime) + " ms");
        }
    }

    private void createJsonObject(List<Map<String, String>> jsonList, String menuId, Integer count) {
        Map<String, String> object = new HashMap<>();
        object.put("count", formatCount(count));
        object.put("menuItem", menuId);
        jsonList.add(object);
    }

    private int getNumber(Map<String, Integer> taskAndCount, String task) {
        Integer count = taskAndCount.get(task);
        return count == null ? 0 : count;
    }

    private String formatCount(int count) {
        return count < maxResults ? String.valueOf(count) : (maxResults + "+");
    }

    public Integer getCount(String menuItemId) {
        return menuItemCounts.get(menuItemId);
    }

    private List<Pair<String, Integer>> getUserCounts() {
        DocumentSearchService documentSearchService = BeanHelper.getDocumentSearchService();
        return Arrays.asList(
                new Pair<>(MenuItem.DISCUSSIONS, documentSearchService.getDiscussionDocumentsCount(luceneLimit)),
                new Pair<>(MenuItem.USER_CASE_FILES, documentSearchService.getCurrentUserCaseFilesCount(luceneLimit)),
                new Pair<>(MenuItem.USER_COMPOUND_WORKFLOWS, documentSearchService.getCurrentUserCompoundWorkflowsCount(luceneLimit)),
                new Pair<>(MenuItem.USER_WORKING_DOCUMENTS, documentSearchService.searchInProcessUserDocumentsCount(luceneLimit)));
    }

    private List<Pair<String, Integer>> getDocManagerCounts() {
        DocumentService documentService = BeanHelper.getDocumentService();
        DocumentSearchService documentSearchService = BeanHelper.getDocumentSearchService();
        ImapServiceExt imapServiceExt = BeanHelper.getImapServiceExt();
        return Arrays.asList(
                new Pair<>(MenuItem.FOR_REGISTERING_LIST, documentSearchService.getCountOfDocumentsForRegistering(luceneLimit)),
                new Pair<>(MenuItem.EMAIL_ATTACHMENTS, imapServiceExt.getAllFilesCount(constantNodeRefsBean.getAttachmentRoot(), true, luceneLimit)),
                new Pair<>(MenuItem.DVK_CORRUPT, BeanHelper.getBulkLoadNodeService().countChildNodes(constantNodeRefsBean.getDvkCorruptRoot(), ContentModel.TYPE_CONTENT)),
                new Pair<>(MenuItem.DVK_DOCUMENTS, documentService.getAllDocumentFromDvkCount()),
                new Pair<>(MenuItem.INCOMING_EMAILS, documentService.getIncomingEmailsCount(luceneLimit)),
                new Pair<>(MenuItem.OUTBOX_DOCUMENT, documentSearchService.searchDocumentsInOutboxCount(luceneLimit)),
                new Pair<>(MenuItem.SCANNED_DOCUMENTS, documentService.countFilesInFolder(constantNodeRefsBean.getScannedFilesRoot(), true, luceneLimit)),
                new Pair<>(MenuItem.SEND_FAILURE_NOTIFICATION, imapServiceExt.getAllFilesCount(constantNodeRefsBean.getSendFailureNoticeSpaceRoot(), true, luceneLimit)),
                new Pair<>(MenuItem.SENT_EMAILS, documentService.getSentEmailsCount(luceneLimit)),
                new Pair<>(MenuItem.UNSENT_DOCUMENT, documentSearchService.searchRecipientFinishedDocumentsCount(luceneLimit)),
                new Pair<>(MenuItem.WEB_SERVICE_DOCUMENTS, documentService.getAllDocumentsFromFolderCount(constantNodeRefsBean.getWebServiceDocumentsRoot())));
    }

    private Pair<String, Integer> getIncomingEincoiceCount() {
        UserService userService = BeanHelper.getUserService();
        if (userService.isDocumentManager() || userService.isInAccountantGroup()) {
            return new Pair<>(MenuItem.INCOMING_EINVOICE, BeanHelper.getDocumentService().getAllDocumentFromIncomingInvoiceCount());
        }
        return new Pair<>(MenuItem.INCOMING_EINVOICE, BeanHelper.getDocumentService().getUserDocumentFromIncomingInvoiceCount(AuthenticationUtil.getRunAsUser()));
    }

    public Integer getMaxSearchResultRows() {
        return BeanHelper.getParametersService().getLongParameter(Parameters.MAX_SEARCH_RESULT_ROWS).intValue();
    }

    public Map<String, Integer> getMap() {
        return menuItemCounts;
    }

    public void setConstantNodeRefsBean(ConstantNodeRefsBean constantNodeRefsBean) {
        this.constantNodeRefsBean = constantNodeRefsBean;
    }

}