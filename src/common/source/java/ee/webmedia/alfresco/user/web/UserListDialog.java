package ee.webmedia.alfresco.user.web;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.alfresco.model.ContentModel;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.web.bean.users.UsersBeanProperties;
import org.alfresco.web.ui.common.component.data.UIRichList;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.orgstructure.service.OrganizationStructureService;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.UserUtil;
import ee.webmedia.alfresco.utils.WebUtil;

public class UserListDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;

    private transient UserService userService;
    private transient OrganizationStructureService organizationStructureService;
    private UsersBeanProperties properties;

    private transient UIRichList usersList;
    private List<Node> users = Collections.<Node> emptyList();

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        // finish button not shown or used
        return null;
    }

    @Override
    public void init(Map<String, String> params) {
        super.init(params);
        reset();
        showAll();
    }

    @Override
    public String cancel() {
        reset();
        return super.cancel();
    }

    private void reset() {
        users = null;
        usersList = null;
        if(properties!=null) {
            properties.setSearchCriteria(null);
        }
    }

    /**
     * @return the list of user Nodes to display
     * @throws Exception
     */
    public List<Node> getUsers() throws Exception {
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
        users = getOrganizationStructureService().setUsersUnit(getUserService().searchUsers(properties.getSearchCriteria(), false));

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
        users = getOrganizationStructureService().setUsersUnit(Repository.getUsers(FacesContext.getCurrentInstance(), properties.getNodeService(), properties.getSearchService()));

        // return null to stay on the same page
        return null;
    }

    /**
     * Query callback method executed by the Generic Picker component.
     * This method is part of the contract to the Generic Picker, it is up to the backing bean
     * to execute whatever query is appropriate and return the results.
     * 
     * @param filterIndex        Index of the filter drop-down selection
     * @param contains           Text from the contains textbox
     * 
     * @return An array of SelectItem objects containing the results to display in the picker.
     */
    public SelectItem[] searchUsers(int filterIndex, String contains) {
        List<Node> nodes = getOrganizationStructureService().setUsersUnit(getUserService().searchUsers(contains, true));
        SelectItem[] results = new SelectItem[nodes.size()];
        int i = 0;
        for (Node node : nodes) {
            String label = UserUtil.getPersonFullNameWithUnitName(node.getProperties());
            results[i++] = new SelectItem(node.getProperties().get(ContentModel.PROP_USERNAME), label);
        }

        WebUtil.sort(results);
        return results;
    }
    
    public void setProperties(UsersBeanProperties properties) {
        this.properties = properties;
    }

    public void setUsers(List<Node> users) {
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
