package ee.webmedia.alfresco.common.richlist;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.web.data.IDataContainer;
import org.alfresco.web.data.QuickSort;
import org.alfresco.web.data.Sort;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.utils.MessageUtil;

public abstract class LazyListDataProvider<Key, Value> implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log LOG = LogFactory.getLog(LazyListDataProvider.class);

    // TODO: make configurable?
    private static final int MIN_SLICE_SIZE = 100;
    protected List<Key> objectKeys;
    private int loadedRowsBeginIndex = -1;
    private int loadedRowsEndIndex = -1;
    private Map<Key, Value> loadedRows;
    private PageLoadCallback<Key, Value> pageLoadCallback;

    public LazyListDataProvider() {
    }

    public LazyListDataProvider(List<Key> objectKeys, PageLoadCallback<Key, Value> pageLoadCallback) {
        this.objectKeys = objectKeys;
        this.pageLoadCallback = pageLoadCallback;
    }

    public int getListSize() {
        return objectKeys == null ? 0 : objectKeys.size();
    }

    public boolean isAllLoaded() {
        return objectKeys != null && loadedRows != null && objectKeys.size() == loadedRows.size();
    }

    public List<Key> getObjectKeys() {
        return objectKeys;
    }

    public void revertOrder() {
        Collections.reverse(objectKeys);
        if (!isAllLoaded()) {
            loadPage(loadedRowsBeginIndex, loadedRowsEndIndex);
        }
    }

    public boolean sortAndReload(String column, boolean descending) {
        boolean sorted = sort(column, descending);
        if (sorted && loadedRowsEndIndex > 0) {
            // reload only if some data is already loaded
            loadPage(loadedRowsBeginIndex, loadedRowsEndIndex);
        }
        return sorted;
    }

    public boolean sort(String column, boolean descending) {
        if (isAllLoaded()) {
            try {
                List<Value> orderedRows = new ArrayList<>(loadedRows.values());
                QuickSort sorter = new QuickSort(orderedRows, column, !descending, IDataContainer.SORT_CASEINSENSITIVE);
                sorter.sort();
                resetObjectKeyOrder(orderedRows);
                return true;
            } catch (Exception e) {
                LOG.error("Failed to sort list by column " + column + ", error: " + e.getMessage());
                return false;
            }
        }
        try {
            return loadOrderFromDb(column, descending);
        } catch (Exception e) {
            LOG.error("Failed to sort list by column " + column + ", error: " + e.getMessage());
            return false;
        }
    }

    protected abstract boolean loadOrderFromDb(String column, boolean descending);

    protected abstract void resetObjectKeyOrder(List<Value> orderedRows);

    public int loadPage(int pageStartIndex, int pageEndIndex) {
        if (isAllLoaded()) {
            return 0;
        }
        Assert.isTrue(pageStartIndex >= 0 && (pageEndIndex >= pageStartIndex || (pageStartIndex == 0 && pageEndIndex < 0)),
                "pageEndIndex must be greater than 0 and greater than pageStartIndex, or, to load all data, pageEndIndex must be set to negative value.");
        loadedRowsBeginIndex = pageStartIndex;
        loadedRowsEndIndex = pageEndIndex + 1;
        if (pageEndIndex < 0 || pageEndIndex > getListSize()) {
            loadedRowsEndIndex = getListSize();
        }
        else if (pageEndIndex - pageStartIndex < MIN_SLICE_SIZE) {
            loadedRowsEndIndex = pageStartIndex + MIN_SLICE_SIZE;
        }
        if (loadedRowsEndIndex > getListSize()) {
            loadedRowsEndIndex = getListSize();
        }
        if (loadedRowsEndIndex == 0) {
            return 0;
        }
        List<Key> nodesToLoad = loadedRowsEndIndex >= 0 ? objectKeys.subList(loadedRowsBeginIndex, loadedRowsEndIndex) : objectKeys;
        loadedRows = loadData(nodesToLoad);
        int missingNodesCount = nodesToLoad.size() - loadedRows.size();
        if (missingNodesCount > 0) {
            @SuppressWarnings("unchecked")
            Collection<Key> missingNodes = CollectionUtils.subtract(nodesToLoad, loadedRows.keySet());
            objectKeys.removeAll(missingNodes);
            missingNodesCount += loadPage(pageStartIndex, pageEndIndex);
        } else if (pageLoadCallback != null) {
            pageLoadCallback.doWithPageItems(loadedRows);
        }
        return missingNodesCount;
    }

    protected abstract Map<Key, Value> loadData(List<Key> rowsToLoad);

    public void reloadRows(Set<Key> set) {
        if (loadedRows == null) {
            loadedRows = new HashMap<>();
            return;
        }
        List<Key> rowsToUpdate = new ArrayList<>();
        for (Key rowToLoad : set) {
            if (loadedRows.containsKey(rowToLoad)) {
                rowsToUpdate.add(rowToLoad);
            }
        }
        Map<Key, Value> reloadedRows = loadData(rowsToUpdate);
        for (Map.Entry<Key, Value> entry : reloadedRows.entrySet()) {
            if (loadedRows.containsKey(entry.getKey())) {
                loadedRows.put(entry.getKey(), entry.getValue());
            }
        }
    }

    public Object getRow(int index) {
        Assert.isTrue(index >= loadedRowsBeginIndex && index < loadedRowsEndIndex, "Row with index=" + index + " is not loaded, call loadSlice for loading required data!");
        Key rowRef = objectKeys.get(index);
        Assert.isTrue(loadedRows.containsKey(rowRef), "Row for nodeRef=" + rowRef + " is not loaded, use loadPage to load required rows!");
        return loadedRows.get(rowRef);
    }

    protected boolean checkAndSetOrderedList(List<Key> orderedList) {
        if (orderedList == null) {
            MessageUtil.addErrorMessage("document_search_sort_error_too_much_rows");
            return false;
        }
        objectKeys = orderedList;
        return true;
    }

    protected void sortAndFillOrderedList(List<Key> orderedList, String column, Map<Key, Value> values, boolean descending) {
        ArrayList<Value> valuesToSort = new ArrayList<>(values.values());
        Sort sort = new QuickSort(valuesToSort, column, !descending, IDataContainer.SORT_CASEINSENSITIVE);
        sort.sort();
        for (Value value : valuesToSort) {
            orderedList.add(getKeyFromValue(value));
        }
    }

    protected abstract Key getKeyFromValue(Value value);

}
