package ee.webmedia.alfresco.menu.web;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.faces.context.FacesContext;

import net.sf.acegisecurity.context.Context;
import net.sf.acegisecurity.context.ContextHolder;

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

// TODO: Parallel lucene queries in this class should be revised,
// as majority of them are very short and as seen from Yourkit profiler,
// these queries are blocking each other, and as the result there is no real parallelism.
// See https://jira.nortal.com/browse/DELTA-784 for details.
public class MenuItemCountBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private static Logger log = Logger.getLogger(MenuItemCountBean.class);

    public static final String BEAN_NAME = "MenuItemCountBean";
    public static final String MENU_ITEM_ID_PARAM = "menuItemId";

    private int luceneLimit;
    private int maxResults;

    private ConstantNodeRefsBean constantNodeRefsBean;

    private final Map<String, Integer> menuItemCounts = new HashMap<>();

    private final static ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool();

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

    private enum MenuItemGroup {
        MY_TASKS,
        MY_RESPONSIBILITIES,
        SENT_RECEIVED_DOCUMENTS
    }

    public void updateMyTaskMenuCounts() throws IOException {
        updateMenuItemsCount(MenuItemGroup.MY_TASKS);
    }

    public void updateMyResponsibilitiesMenuCount() throws IOException {
        updateMenuItemsCount(MenuItemGroup.MY_RESPONSIBILITIES);
    }

    public void updateReceivedSentMenuItemsCount() throws IOException {
        updateMenuItemsCount(MenuItemGroup.SENT_RECEIVED_DOCUMENTS);
    }

    public void updateMenuItemsCount() throws IOException {
        updateMenuItemsCount(null);
    }

    public void updateMenuItemsCount(final MenuItemGroup menuItemGroup) throws IOException {
        long startTime = System.currentTimeMillis();
        FacesContext context = FacesContext.getCurrentInstance();
        maxResults = getMaxSearchResultRows();
        luceneLimit = maxResults + 1;

        boolean isDocumentManager = BeanHelper.getUserService().isDocumentManager();
        boolean isInvoiceEnabled = BeanHelper.getApplicationConstantsBean().isEinvoiceEnabled();

        final List<Pair<String, Integer>> counts = Collections.synchronizedList(new ArrayList<Pair<String, Integer>>());

        int latchCount = 1;

        if (isDocumentManager) {
            latchCount++;
        }

        if (isInvoiceEnabled) {
            latchCount++;
        }

        final CountDownLatch latch = new CountDownLatch(latchCount);
        final Context authContext = ContextHolder.getContext();

        EXECUTOR_SERVICE.submit(new Runnable() {

            @Override
            public void run() {
                counts.addAll(getUserCounts(authContext, menuItemGroup));
                latch.countDown();
            }
        });

        if (isDocumentManager) {
            EXECUTOR_SERVICE.submit(new Runnable() {

                @Override
                public void run() {
                    counts.addAll(getDocManagerCounts(authContext, menuItemGroup));
                    latch.countDown();
                }
            });

        }

        if (BeanHelper.getApplicationConstantsBean().isEinvoiceEnabled()) {
            counts.add(getIncomingEincoiceCount());
        }

        List<Map<String, String>> jsonList = new ArrayList<>();
        if (menuItemGroup == null || MenuItemGroup.MY_TASKS.equals(menuItemGroup)) {
            Map<String, Integer> taskAndCount = BeanHelper.getWorkflowDbService().countAllCurrentUserTasks();
            for (Entry<String, String> entry : GROUPED_TASKS.entrySet()) {
                if (taskAndCount.containsKey(entry.getValue()) && !taskAndCount.containsKey(entry.getKey())) {
                    taskAndCount.put(entry.getKey(), 0);
                }
            }

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
        }

        try {
            latch.await();
        } catch (Exception e) {
            // Nothing to do here
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

    private List<Pair<String, Integer>> getUserCounts(final Context context, MenuItemGroup menuItemGroup) {
        if (MenuItemGroup.SENT_RECEIVED_DOCUMENTS.equals(menuItemGroup)) {
            return new ArrayList<>();
        }
        final DocumentSearchService documentSearchService = BeanHelper.getDocumentSearchService();

        final List<Pair<String, Integer>> result = Collections.synchronizedList(new ArrayList<Pair<String, Integer>>());

        if (MenuItemGroup.MY_TASKS.equals(menuItemGroup)) {
            addDiscussionCount(result, documentSearchService);
            return result;
        }

        ContextHolder.setContext(context);

        int latchCount = menuItemGroup == null ? 3 : 2;
        final CountDownLatch latch = new CountDownLatch(latchCount);

        if (menuItemGroup == null) {
            EXECUTOR_SERVICE.submit(new Runnable() {

                @Override
                public void run() {
                    ContextHolder.setContext(context);
                    addDiscussionCount(result, documentSearchService);
                    latch.countDown();

                }
            });
        }

        EXECUTOR_SERVICE.submit(new Runnable() {

            @Override
            public void run() {
                ContextHolder.setContext(context);
                result.add(new Pair<>(MenuItem.USER_CASE_FILES, documentSearchService.getCurrentUserCaseFilesCount(luceneLimit)));
                latch.countDown();
            }
        });

        EXECUTOR_SERVICE.submit(new Runnable() {

            @Override
            public void run() {
                ContextHolder.setContext(context);
                result.add(new Pair<>(MenuItem.USER_COMPOUND_WORKFLOWS, documentSearchService.getCurrentUserCompoundWorkflowsCount(luceneLimit)));
                latch.countDown();
            }
        });

        result.add(new Pair<>(MenuItem.USER_WORKING_DOCUMENTS, documentSearchService.searchInProcessUserDocumentsCount(luceneLimit)));

        try {
            latch.await();
        } catch (Exception e) {

        }

        return result;
    }

    protected void addDiscussionCount(final List<Pair<String, Integer>> result, final DocumentSearchService documentSearchService) {
        result.add(new Pair<>(MenuItem.DISCUSSIONS, documentSearchService.getDiscussionDocumentsCount(luceneLimit)));
    }

    private List<Pair<String, Integer>> getDocManagerCounts(final Context context, MenuItemGroup menuItemGroup) {
        if (MenuItemGroup.MY_RESPONSIBILITIES.equals(menuItemGroup)) {
            return new ArrayList<>();
        }

        final DocumentSearchService documentSearchService = BeanHelper.getDocumentSearchService();
        final List<Pair<String, Integer>> result = Collections.synchronizedList(new ArrayList<Pair<String, Integer>>());

        if (MenuItemGroup.MY_TASKS.equals(menuItemGroup)) {
            addForRegisteringDocCount(documentSearchService, result);
            return result;
        }

        final DocumentService documentService = BeanHelper.getDocumentService();
        final ImapServiceExt imapServiceExt = BeanHelper.getImapServiceExt();
        ContextHolder.setContext(context);

        int latchCount = menuItemGroup == null ? 10 : 9;
        final CountDownLatch latch = new CountDownLatch(latchCount);

        if (menuItemGroup == null) {
            EXECUTOR_SERVICE.submit(new Runnable() {

                @Override
                public void run() {
                    ContextHolder.setContext(context);
                    addForRegisteringDocCount(documentSearchService, result);
                    latch.countDown();
                }
            });
        }

        EXECUTOR_SERVICE.submit(new Runnable() {

            @Override
            public void run() {
                ContextHolder.setContext(context);
                result.add(new Pair<>(MenuItem.EMAIL_ATTACHMENTS, imapServiceExt.getAllFilesCount(constantNodeRefsBean.getAttachmentRoot(), true, luceneLimit)));
                latch.countDown();
            }
        });

        EXECUTOR_SERVICE.submit(new Runnable() {

            @Override
            public void run() {
                ContextHolder.setContext(context);
                result.add(new Pair<>(MenuItem.DVK_CORRUPT, BeanHelper.getBulkLoadNodeService()
                        .countChildNodes(constantNodeRefsBean.getDvkCorruptRoot(), ContentModel.TYPE_CONTENT)));
                latch.countDown();
            }
        });

        EXECUTOR_SERVICE.submit(new Runnable() {

            @Override
            public void run() {
                ContextHolder.setContext(context);
                result.add(new Pair<>(MenuItem.DVK_DOCUMENTS, documentService.getAllDocumentFromDvkCount()));
                latch.countDown();
            }
        });

        EXECUTOR_SERVICE.submit(new Runnable() {

            @Override
            public void run() {
                ContextHolder.setContext(context);
                result.add(new Pair<>(MenuItem.INCOMING_EMAILS, documentService.getIncomingEmailsCount(luceneLimit)));
                latch.countDown();
            }
        });

        EXECUTOR_SERVICE.submit(new Runnable() {

            @Override
            public void run() {
                ContextHolder.setContext(context);
                result.add(new Pair<>(MenuItem.OUTBOX_DOCUMENT, documentSearchService.searchDocumentsInOutboxCount(luceneLimit)));
                latch.countDown();
            }
        });

        EXECUTOR_SERVICE.submit(new Runnable() {

            @Override
            public void run() {
                ContextHolder.setContext(context);
                result.add(new Pair<>(MenuItem.SCANNED_DOCUMENTS, documentService.countFilesInFolder(constantNodeRefsBean.getScannedFilesRoot(), true, luceneLimit)));
                latch.countDown();
            }
        });

        EXECUTOR_SERVICE.submit(new Runnable() {

            @Override
            public void run() {
                ContextHolder.setContext(context);
                result.add(new Pair<>(MenuItem.SEND_FAILURE_NOTIFICATION, imapServiceExt.getAllFilesCount(constantNodeRefsBean.getSendFailureNoticeSpaceRoot(), true, luceneLimit)));
                latch.countDown();
            }
        });

        EXECUTOR_SERVICE.submit(new Runnable() {

            @Override
            public void run() {
                ContextHolder.setContext(context);
                result.add(new Pair<>(MenuItem.SENT_EMAILS, documentService.getSentEmailsCount(luceneLimit)));
                latch.countDown();
            }
        });

        EXECUTOR_SERVICE.submit(new Runnable() {

            @Override
            public void run() {
                ContextHolder.setContext(context);
                result.add(new Pair<>(MenuItem.UNSENT_DOCUMENT, documentSearchService.searchRecipientFinishedDocumentsCount(luceneLimit)));
                latch.countDown();
            }
        });

        result.add(new Pair<>(MenuItem.WEB_SERVICE_DOCUMENTS, documentService.getAllDocumentsFromFolderCount(constantNodeRefsBean.getWebServiceDocumentsRoot())));

        try {
            latch.await();
        } catch (Exception e) {

        }

        return result;
    }

    private void addForRegisteringDocCount(final DocumentSearchService documentSearchService, final List<Pair<String, Integer>> result) {
        result.add(new Pair<>(MenuItem.FOR_REGISTERING_LIST, documentSearchService.getCountOfDocumentsForRegistering(luceneLimit)));
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