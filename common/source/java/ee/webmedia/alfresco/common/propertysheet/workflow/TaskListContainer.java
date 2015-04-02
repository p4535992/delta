package ee.webmedia.alfresco.common.propertysheet.workflow;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlPanelGroup;
import javax.faces.context.FacesContext;

import org.alfresco.web.data.IDataContainer;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.workflow.web.CompoundWorkflowDefinitionDialog;

public class TaskListContainer extends HtmlPanelGroup implements IDataContainer {

    private static final String LIST_PAGE_ATTRIBUTE = "taskListContainerPageNumbers";

    private int pageSize;
    private int pageCount;
    private int currentPage;
    private String currentPageAttributeKey;
    private int workflowIndex;

    public TaskListContainer() {
        pageSize = BeanHelper.getBrowseBean().getPageSizeContent();
    }

    public TaskListContainer(UIComponent parent, int workflowIndex, String currentPageAttributeKey) {
        this();
        this.currentPageAttributeKey = currentPageAttributeKey;
        currentPage = getTaskListCurrentPageNumber(getPersistentParent(parent), workflowIndex, currentPageAttributeKey, pageSize);
        this.workflowIndex = workflowIndex;
    }

    public void setRowCount(int rowCount) {
        pageCount = (rowCount / pageSize) + 1;
        if (rowCount % pageSize == 0 && pageCount != 1) {
            pageCount--;
        }
    }

    public boolean isRowRangeOnCurrentPage(int rangeStartRow, int rangeEndRow) {
        return rangeStartRow <= getFirstPageRowNumber() && rangeEndRow >= getFirstPageRowNumber()    // Range overlaps from left
                || isRowOnCurrentPage(rangeStartRow) && isRowOnCurrentPage(rangeEndRow)              // Range is entirely on page
                || rangeStartRow <= getLastPageRowNumber() && rangeEndRow >= getLastPageRowNumber(); // Range overlaps from right
    }

    public boolean isRowOnCurrentPage(int rowNumber) {
        return getFirstPageRowNumber() <= rowNumber && rowNumber <= getLastPageRowNumber();
    }

    public int getFirstPageRowNumber() {
        return currentPage * pageSize;
    }

    public int getLastPageRowNumber() {
        return currentPage * pageSize + pageSize - 1;
    }

    @Override
    public void setCurrentPage(int i) {
        currentPage = i;
        saveSelectedPageNumber(new PageInfo(i, pageSize));
    }

    private void saveSelectedPageNumber(PageInfo selectedPage) {
        UIComponent persistentParent = getPersistentParent(this);
        if (persistentParent == null) {
            return;
        }
        setTaskListCurrentPageInfo(persistentParent, workflowIndex, currentPageAttributeKey, selectedPage);
        updateView();
    }

    protected void updateView() {
        ((CompoundWorkflowDefinitionDialog) BeanHelper.getDialogManager().getBean()).updatePanelGroupWithoutWorkflowBlockUpdate();
    }

    @Override
    public Object saveState(FacesContext context) {
        Object values[] = new Object[6];
        values[0] = super.saveState(context);
        values[1] = pageSize;
        values[2] = pageCount;
        values[3] = currentPage;
        values[4] = currentPageAttributeKey;
        values[5] = workflowIndex;
        return values;
    }

    @Override
    public void restoreState(FacesContext context, Object state) {
        Object values[] = (Object[]) state;
        super.restoreState(context, values[0]);
        pageSize = (int) values[1];
        pageCount = (int) values[2];
        currentPage = (int) values[3];
        currentPageAttributeKey = (String) values[4];
        workflowIndex = (int) values[5];
    }

    @Override
    public int getPageSize() {
        return pageSize;
    }

    @Override
    public int getCurrentPage() {
        return currentPage;
    }

    @Override
    public int getPageCount() {
        return pageCount;
    }

    @Override
    public String getCurrentSortColumn() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCurrentSortDescending() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDataAvailable() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object nextRow() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sort(String s, boolean b, String s2) {
        throw new UnsupportedOperationException();
    }

    protected UIComponent getPersistentParent(UIComponent searchFromComponent) {
        return getCompoundWorkflowDialog(searchFromComponent);
    }

    // Static helper methods

    public static void addWorkflowTaskListPageInfo(UIComponent eventSource, int addedWorkflowIndex) {
        List<Map<String, PageInfo>> pageAttribute = getCurrentPageAttribute(getCompoundWorkflowDialog(eventSource));
        pageAttribute.add(addedWorkflowIndex, new HashMap<String, PageInfo>());
    }

    public static void removeWorkflowTaskListPageInfo(UIComponent eventSource, int removedWorkflowIndex) {
        List<Map<String, PageInfo>> pageAttribute = getCurrentPageAttribute(getCompoundWorkflowDialog(eventSource));
        pageAttribute.remove(removedWorkflowIndex);
    }

    private static void setTaskListCurrentPageInfo(UIComponent persistentParentComponent, int workflowIndex, String currentPageAttributeKey, PageInfo currentPageInfo) {
        List<Map<String, PageInfo>> listPages = getCurrentPageAttribute(persistentParentComponent);
        listPages.get(workflowIndex).put(currentPageAttributeKey, currentPageInfo);
    }

    private static Integer getTaskListCurrentPageNumber(UIComponent persistentParentComponent, int workflowIndex, String currentPageAttributeKey, Integer pageSize) {
        List<Map<String, PageInfo>> listPages = getCurrentPageAttribute(persistentParentComponent);
        return getCurrentPageInfoFromAttributes(listPages, workflowIndex, currentPageAttributeKey, pageSize).getPageNumber();
    }

    /**
     * Fetches (and creates if needed) a list of stored task list pager states.
     * Returns an ArrayList where element indices match workflow IDs. Elements are maps where key is name of the property sheet item that is generated by TaskListGenerator.
     * Map entry value contains currently selected page number for referenced task list.
     *
     * @param persistentParentComponent Component where task list pager state is stored
     * @return see above
     */
    @SuppressWarnings("unchecked")
    private static List<Map<String, PageInfo>> getCurrentPageAttribute(UIComponent persistentParentComponent) {
        List<Map<String, PageInfo>> listPages = (List<Map<String, PageInfo>>) persistentParentComponent.getAttributes().get(TaskListContainer.LIST_PAGE_ATTRIBUTE);
        if (listPages == null) {
            listPages = new ArrayList<>();
            persistentParentComponent.getAttributes().put(TaskListContainer.LIST_PAGE_ATTRIBUTE, listPages);
        }
        return listPages;
    }

    private static PageInfo getCurrentPageInfoFromAttributes(List<Map<String, PageInfo>> currentPageNumbers, int workflowIndex, String currentPageAttributeKey, int pageSize) {
        int pageNumberHolders = currentPageNumbers.size();
        if (workflowIndex >= pageNumberHolders) {
            // Registration workflows don't generate a TaskListContainer, thus we need to add extra elements if needed
            for (int i = pageNumberHolders; i <= workflowIndex; i++) {
                currentPageNumbers.add(i, new HashMap<String, PageInfo>());
            }
        }
        Map<String, PageInfo> pageInfosByPropertyIds = currentPageNumbers.get(workflowIndex);

        PageInfo page = pageInfosByPropertyIds.get(currentPageAttributeKey);
        if (page == null) {
            page = new PageInfo(0, pageSize);
            pageInfosByPropertyIds.put(currentPageAttributeKey, page);
        } else if (!page.getPageSize().equals(pageSize)) {
            int oldFirstRow = page.getPageSize() * page.getPageNumber();
            int newFirstRow = pageSize * page.getPageNumber();
            if (oldFirstRow < newFirstRow) {
                page = new PageInfo(((int) Math.floor(oldFirstRow / pageSize)), pageSize);
            }
        }
        return page;
    }

    private static UIComponent getCompoundWorkflowDialog(UIComponent searchFromComponent) {
        return ComponentUtil.findParentComponentById(searchFromComponent, "compound-workflow-dialog");
    }

    private static class PageInfo implements Serializable {
        private static final long serialVersionUID = 1L;

        private final Integer pageNumber;
        private final Integer pageSize;

        public PageInfo(int pageNumber, int pageSize) {
            this.pageNumber = pageNumber;
            this.pageSize = pageSize;
        }

        public Integer getPageNumber() {
            return pageNumber;
        }

        public Integer getPageSize() {
            return pageSize;
        }
    }
}
