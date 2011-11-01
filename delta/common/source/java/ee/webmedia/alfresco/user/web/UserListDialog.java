package ee.webmedia.alfresco.user.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.web.bean.users.UsersBeanProperties;
import org.alfresco.web.ui.common.component.data.UIRichList;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.orgstructure.service.OrganizationStructureService;
import ee.webmedia.alfresco.user.model.UserListRowVO;
import ee.webmedia.alfresco.user.model.UserModel;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.UserUtil;
import ee.webmedia.alfresco.utils.WebUtil;

public class UserListDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "UserListDialog";

    private transient UserService userService;
    private transient OrganizationStructureService organizationStructureService;
    private UsersBeanProperties properties;

    private transient UIRichList usersList;
    private List<UserListRowVO> users = Collections.<UserListRowVO> emptyList();

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        // finish button not shown or used
        return null;
    }

    @Override
    public void init(Map<String, String> params) {
        super.init(params);
        reset();
    }

    @Override
    public String cancel() {
        reset();
        return super.cancel();
    }

    private void reset() {
        users = null;
        usersList = null;
        if (properties != null) {
            properties.setSearchCriteria(null);
        }
    }

    /**
     * @return the list of user Nodes to display
     * @throws Exception
     */
    public List<UserListRowVO> getUsers() throws Exception {
        if (users == null) {
            search();
        }
        return users;
    }

    /**
     * Event handler called when the user wishes to search for a user
     * 
     * @return The outcome
     */
    public String search() {
        if (usersList != null) {
            usersList.setValue(null);
        }
        users = getUserListVOs(getOrganizationStructureService().setUsersUnit(getUserService().searchUsers(properties.getSearchCriteria(), false)));

        // return null to stay on the same page
        return null;
    }

    /**
     * Action handler to show all the users currently in the system
     * 
     * @return The outcome
     */
    public String showAll() {
        if (usersList != null) {
            usersList.setValue(null);
        }
        if (properties != null) {
            properties.setSearchCriteria(null);
        }
        users = getUserListVOs(getOrganizationStructureService().setUsersUnit(
                Repository.getUsers(FacesContext.getCurrentInstance(), properties.getNodeService(), properties.getSearchService())));

        // return null to stay on the same page
        return null;
    }

    private List<UserListRowVO> getUserListVOs(List<Node> userNodes) {
        List<UserListRowVO> userListVOs = new ArrayList<UserListRowVO>();
        if (userNodes != null) {
            for (Node userNode : userNodes) {
                userListVOs.add(new UserListRowVO(userNode));
            }
        }
        return userListVOs;
    }

    /**
     * Query callback method executed by the Generic Picker component.
     * This method is part of the contract to the Generic Picker, it is up to the backing bean
     * to execute whatever query is appropriate and return the results.
     * 
     * @param filterIndex Index of the filter drop-down selection
     * @param contains Text from the contains textbox
     * @return An array of SelectItem objects containing the results to display in the picker.
     */
    public SelectItem[] searchUsers(int filterIndex, String contains) {
        return searchUsers(contains, false, false, true);
    }

    public SelectItem[] searchUsersWithoutSubstitutionInfoShown(int filterIndex, String contains) {
        return searchUsers(contains, false, false, false);
    }

    /**
     * @see #searchUsers(int, String)
     * @return SelectItems representing users. Current user is excluded.
     */
    public SelectItem[] searchOtherUsers(int filterIndex, String contains) {
        return searchUsers(contains, true, false, true);
    }

    public SelectItem[] searchUsersWithNameValue(int filterIndex, String contains) {
        return searchUsers(contains, false, true, true);
    }

    private SelectItem[] searchUsers(String contains, boolean excludeCurrentUser, boolean useNameAsValue, boolean showSubstitutionInfo) {
        List<Node> nodes = getOrganizationStructureService().setUsersUnit(getUserService().searchUsers(contains, true));
        int nodesSize = nodes.size();
        List<SelectItem> results = new ArrayList<SelectItem>(nodesSize);

        String currentUser = null;
        if (excludeCurrentUser) {
            Node user = BeanHelper.getUserDetailsDialog().getUser();
            if (user != null) {
                currentUser = (String) user.getProperties().get(ContentModel.PROP_USERNAME);
            } else {
                currentUser = AuthenticationUtil.getRunAsUser();
            }
        }

        for (Node node : nodes) {
            String userName = (String) node.getProperties().get(ContentModel.PROP_USERNAME);
            if (excludeCurrentUser && StringUtils.equals(userName, currentUser) || node.hasAspect(UserModel.Aspects.LEAVING)) {
                continue;
            }
            String label = UserUtil.getPersonFullNameWithUnitName(node.getProperties(), showSubstitutionInfo);
            String value = userName;
            results.add(new SelectItem(value, label));
        }

        WebUtil.sort(results);
        return results.toArray(new SelectItem[results.size()]);
    }

    public void setProperties(UsersBeanProperties properties) {
        this.properties = properties;
    }

    public void setUsers(List<UserListRowVO> users) {
        this.users = users;
    }

    public UIRichList getUsersList() {
        return usersList;
    }

    public void setUsersList(UIRichList usersList) {
        this.usersList = usersList;
    }

    protected UserService getUserService() {
        if (userService == null) {
            userService = (UserService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
                    .getBean(UserService.BEAN_NAME);
        }
        return userService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    protected OrganizationStructureService getOrganizationStructureService() {
        if (organizationStructureService == null) {
            organizationStructureService = (OrganizationStructureService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
                    .getBean(OrganizationStructureService.BEAN_NAME);
        }
        return organizationStructureService;
    }

    public void setOrganizationStructureService(OrganizationStructureService organizationStructureService) {
        this.organizationStructureService = organizationStructureService;
    }

}

