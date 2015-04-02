package ee.webmedia.mobile.alfresco.workflow.model;

/** The purpose of this model is to bypass encoding issues that might occur when transmitting query string as a part of uri */
public class QueryString {

    private String query;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

}
