package ee.webmedia.mobile.alfresco.workflow.model;

public class UserItem {

    private String name;
    private String userId;
    /** @see {@link ee.webmedia.alfresco.common.web.UserContactGroupSearchBean} */
    private int userItemFilterType;

    public UserItem(String name, String userId) {
        this.name = name;
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getUserItemFilterType() {
        return userItemFilterType;
    }

    public void setUserItemFilterType(int userItemFilterType) {
        this.userItemFilterType = userItemFilterType;
    }

}
