package ee.webmedia.alfresco.user.web;

import java.io.Serializable;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.user.model.Authority;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.WebUtil;

public class UserGroupSearchBean implements Serializable {
    private static final long serialVersionUID = 1L;
    private SelectItem[] usersGroupsFilters;
    private UserListDialog userListDialog;
    private transient DocumentSearchService documentSearchService;

    /**
     * Property accessed by the Generic Picker component.
     * 
     * @return the array of filter options to show in the users/groups picker
     */
    public SelectItem[] getUsersGroupsFilters() {
        if (usersGroupsFilters == null) {
            FacesContext context = FacesContext.getCurrentInstance();
            usersGroupsFilters = new SelectItem[] {
                    new SelectItem("0", MessageUtil.getMessage(context, "users")),
                    new SelectItem("1", MessageUtil.getMessage(context, "groups"))
            };
        }
        return usersGroupsFilters;
    }

    public SelectItem[] searchUsersGroups(int filterIndex, String contains, boolean withAdminsAndDocManagers) {
        if (filterIndex == 0) {
            return userListDialog.searchUsers(-1, contains);
        } else if (filterIndex == 1) {
            return searchGroups(-1, contains, withAdminsAndDocManagers);
        }
        throw new RuntimeException("filterIndex must be 0 or -1, but is " + filterIndex);
    }

    public SelectItem[] searchUsersGroups(int filterIndex, String contains) {
        return searchUsersGroups(filterIndex, contains, false);
    }

    public SelectItem[] searchUsersGroupsWithAdminsAndDocManagers(int filterIndex, String contains) {
        return searchUsersGroups(filterIndex, contains, true);
    }

    public SelectItem[] searchGroupsWithAdminsAndDocManagers(int filterIndex, String contains) {
        return searchGroups(1, contains, true);
    }

    public SelectItem[] searchGroups(int filterIndex, String contains, boolean withAdminsAndDocManagers) {
        List<Authority> results = getDocumentSearchService().searchAuthorityGroups(contains, true, withAdminsAndDocManagers);
        SelectItem[] selectItems = new SelectItem[results.size()];
        int i = 0;
        for (Authority authority : results) {
            selectItems[i++] = new SelectItem(authority.getAuthority(), authority.getName());
        }
        WebUtil.sort(selectItems);
        return selectItems;
    }

    protected DocumentSearchService getDocumentSearchService() {
        if (documentSearchService == null) {
            documentSearchService = BeanHelper.getDocumentSearchService();
        }
        return documentSearchService;
    }

    // START: getters / setters
    public void setUserListDialog(UserListDialog userListDialog) {
        this.userListDialog = userListDialog;
    }
    // END: getters / setters
}
