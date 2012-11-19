package org.alfresco.web.ui.common.component;

import java.io.Serializable;

public class PickerSearchParams implements Serializable {
    private static final long serialVersionUID = 1L;
    private int filterIndex;
    private String searchString;
    private String groupSelectLimitation;
    private Boolean filterByStructUnitParam = Boolean.FALSE;
    private int limit = -1;
    private boolean includeFilterIndex = false;

    /**
     * @param filterIndex Index of the filter drop-down selection
     * @param contains Text from the contains textbox
     * @param limit Result limit
     **/
    public PickerSearchParams(int filterIndex, String searchString, int limit) {
        this.filterIndex = filterIndex;
        this.searchString = searchString;
        this.limit = limit;
    }

    public PickerSearchParams(int filterIndex, String searchString, int limit, String groupSelectLimitation) {
        this(filterIndex, searchString, limit);
        this.groupSelectLimitation = groupSelectLimitation;
    }

    public PickerSearchParams(int filterIndex, String searchString, int limit, String groupSelectLimitation, Boolean filterByStructUnitParam) {
        this(filterIndex, searchString, limit, groupSelectLimitation);
        this.filterByStructUnitParam = filterByStructUnitParam;
    }

    public PickerSearchParams(int filterIndex, String searchString, int limit, String groupSelectLimitation, Boolean filterByStructUnitParam, boolean includeFilterIndex) {
        this(filterIndex, searchString, limit, groupSelectLimitation, filterByStructUnitParam);
        this.includeFilterIndex = includeFilterIndex;
    }

    public int getFilterIndex() {
        return filterIndex;
    }

    public boolean isFilterIndex(int compareTo) {
        return (filterIndex & compareTo) == compareTo;
    }

    public String getSearchString() {
        return searchString;
    }

    public int getLimit() {
        return limit;
    }

    public boolean isIncludeFilterIndex() {
        return includeFilterIndex;
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

    public void setGroupSelectLimitation(String groupSelectLimitation) {
        this.groupSelectLimitation = groupSelectLimitation;
    }

    public String getGroupSelectLimitation() {
        return groupSelectLimitation;
    }

    public void setFilterByStructUnitParam(Boolean filterByStructUnitParam) {
        this.filterByStructUnitParam = filterByStructUnitParam;
    }

    public Boolean getFilterByStructUnitParam() {
        return filterByStructUnitParam;
    }

    public void setIncludeFilterIndex(boolean includeFilterIndex) {
        this.includeFilterIndex = includeFilterIndex;
    }
}
