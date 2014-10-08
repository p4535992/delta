package ee.webmedia.alfresco.common.richlist;

import org.alfresco.util.Pair;
import org.alfresco.web.ui.common.component.data.IGridDataModel;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

public class GridLazyDataModel implements IGridDataModel {

    private static final long serialVersionUID = 1L;
    private final LazyListDataProvider dataProvider;
    private String sortColumn;
    private boolean descending;
    private int currentStartIndex = -1;
    private int currentEndIndex = -1;

    public GridLazyDataModel(LazyListDataProvider dataProvider) {
        Assert.notNull(dataProvider, "Data provider must not be null!");
        this.dataProvider = dataProvider;
    }

    @Override
    public Object getRow(int index) {
        return dataProvider.getRow(index);
    }

    @Override
    public int size() {
        return dataProvider.getListSize();
    }

    public boolean isAllLoaded() {
        return dataProvider.isAllLoaded();
    }

    @Override
    public void sort(String column, boolean descending, String mode) {
        Assert.isTrue(StringUtils.isNotBlank(column));
        if (column.equals(sortColumn)) {
            if (this.descending != descending) {
                dataProvider.revertOrder();
            }
        } else {
            boolean sorted = dataProvider.sortAndReload(column, descending);
            if (sorted) {
                sortColumn = column;
            }
        }
        this.descending = descending;
    }

    @Override
    public void loadSlice(int rowIndex, int maxRowIndex) {
        if (rowIndex != currentStartIndex || maxRowIndex != currentEndIndex) {
            dataProvider.loadPage(rowIndex, maxRowIndex);
            currentStartIndex = rowIndex;
            currentEndIndex = maxRowIndex;
        }
    }

    @Override
    public Pair<Integer, Integer> getCurrentSlice() {
        return new Pair<>(currentStartIndex, currentEndIndex);
    }
}
