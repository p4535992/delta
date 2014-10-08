package ee.webmedia.alfresco.filter.model;

import org.alfresco.service.cmr.repository.NodeRef;

public class FilterVO {
    private final NodeRef filterRef;
    private final String filterName;
    private final boolean isPrivate;

    public FilterVO(NodeRef filterRef, String filterName, boolean isPrivate) {
        this.filterRef = filterRef;
        this.filterName = filterName;
        this.isPrivate = isPrivate;
    }

    // START: getters / setters
    public NodeRef getFilterRef() {
        return filterRef;
    }

    public String getFilterName() {
        return filterName;
    }

    public boolean isPrivate() {
        return isPrivate;
    }
    // END: getters / setters
}
