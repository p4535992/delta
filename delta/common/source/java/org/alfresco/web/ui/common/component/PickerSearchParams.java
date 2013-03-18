package org.alfresco.web.ui.common.component;

import java.io.Serializable;

public class PickerSearchParams implements Serializable {
    private static final long serialVersionUID = 1L;
    private int filterIndex;
    private String searchString;
    private int limit = -1;

    /**
     * @param filterIndex Index of the filter drop-down selection
     * @param contains Text from the contains textbox
     * @param limit Result limit
     **/
    public PickerSearchParams(int filterIndex, String searchString, int limit) {
        setFilterIndex(filterIndex);
        setSearchString(searchString);
        setLimit(limit);
    }

    public int getFilterIndex() {
        return filterIndex;
    }

    public boolean isFilterIndex(int compareTo) {
        return filterIndex == compareTo;
    }

    public String getSearchString() {
        return searchString;
    }

    public int getLimit() {
        return limit;
    }

    public void setFilterIndex(int filterIndex) {
        this.filterIndex = filterIndex;
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }
}