<<<<<<< HEAD
package ee.webmedia.alfresco.common.web;

import java.io.Serializable;

/**
 * Holds information about what page number of the list was last shown and how list was sorted
 * 
 * @author Ats Uiboupin
 */
public class PagedListBookmark implements Serializable {
    private static final long serialVersionUID = 1L;

    private int pageNr;
    private String sortColumn = null;
    private boolean sortDescending = true;

    @SuppressWarnings("hiding")
    public void update(int pageNr, String sortColumn, boolean sortDescending) {
        this.pageNr = pageNr;
        this.sortColumn = sortColumn;
        this.sortDescending = sortDescending;
    }

    public int getPageNr() {
        return pageNr;
    }

    public String getSortColumn() {
        return sortColumn;
    }

    public boolean isSortDescending() {
        return sortDescending;
    }

    @Override
    public String toString() {
        String sort = "";
        if (sortColumn != null) {
            sort = "; sort " + sortColumn + " " + (sortDescending ? "desc" : "");
        }
        return "page " + pageNr + sort;
    }
}
=======
package ee.webmedia.alfresco.common.web;

import java.io.Serializable;

/**
 * Holds information about what page number of the list was last shown and how list was sorted
 */
public class PagedListBookmark implements Serializable {
    private static final long serialVersionUID = 1L;

    private int pageNr;
    private String sortColumn = null;
    private boolean sortDescending = true;

    @SuppressWarnings("hiding")
    public void update(int pageNr, String sortColumn, boolean sortDescending) {
        this.pageNr = pageNr;
        this.sortColumn = sortColumn;
        this.sortDescending = sortDescending;
    }

    public int getPageNr() {
        return pageNr;
    }

    public String getSortColumn() {
        return sortColumn;
    }

    public boolean isSortDescending() {
        return sortDescending;
    }

    @Override
    public String toString() {
        String sort = "";
        if (sortColumn != null) {
            sort = "; sort " + sortColumn + " " + (sortDescending ? "desc" : "");
        }
        return "page " + pageNr + sort;
    }
}
>>>>>>> develop-5.1
