package ee.webmedia.alfresco.user.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import ee.webmedia.alfresco.substitute.model.Substitute;
import ee.webmedia.alfresco.substitute.web.SubstituteListDialog;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.users.UsersBeanProperties;
import org.alfresco.web.bean.users.UsersDialog;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.orgstructure.service.OrganizationStructureService;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.ActionUtil;

public class UserDetailsDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;

    private transient UserService userService;
    private transient OrganizationStructureService organizationStructureService;
    private UsersBeanProperties properties;

    private SubstituteListDialog substituteListDialog;

    private Node user;

    @Override
    public void init(Map<String, String> parameters) {
        super.init(parameters);
        substituteListDialog = new SubstituteListDialog();
        substituteListDialog.setUserNodeRef(user.getNodeRef());
        substituteListDialog.refreshData();
    }

    /*
    * This is a read-only dialog, so we have nothing to do here (Save/OK button isn't displayed)
    */
    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        substituteListDialog.save(context);
        isFinished = false;
        return null;
    }

    @Override
    public String cancel() {
        this.user = null;
        substituteListDialog.cancel();
        return super.cancel();
    }

     @Override
    public Object getActionsContext() {
        // since we are using actions, but not action context,
        // we don't need instance of NavigationBean that is used in the overloadable method
        return null;
    }

    @Override
    public boolean getFinishButtonDisabled() {
        return false;
    }

    /**
     * Action event called by all actions that need to setup a Person context on
     * the current user before an action page is called. The context will be a
     * Person Node in setPerson() which can be retrieved on the action page from
     * UsersDialog.getCurrentUserNode().
     */
    public void setupCurrentUser(ActionEvent event)
    {
       Node node = new Node(properties.getPersonService().getPerson(AuthenticationUtil.getRunAsUser()));
       // Eagerly load properties
       node.getProperties();

       // take care of UsersDialog
       UsersDialog dialog = (UsersDialog) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), UsersDialog.BEAN_NAME);
       dialog.setupUserAction(node.getId());
       
       List<Node> users = new ArrayList<Node>(1);
       users.add(node);
       this.user = getOrganizationStructureService().setUsersUnit(users).get(0);
    }

    /**
     * Action event called by all actions that need to setup a Person context on
     * the Users bean before an action page is called. The context will be a
     * Person Node in setPerson() which can be retrieved on the action page from
     * UsersDialog.getPerson().
     */
    public void setupUser(ActionEvent event) {
        setupUser(ActionUtil.getParam(event, "id"));
    }

    /**
     * Used in JSP to set up person context
     * 
     * @param userName
     */
    public void setupUser(String userName) {
        List<Node> users = new ArrayList<Node>(1);
        users.add(new Node(properties.getPersonService().getPerson(userName)));
        this.user = getOrganizationStructureService().setUsersUnit(users).get(0);
    }

    public UsersBeanProperties getProperties() {
        return properties;
    }

    public void setProperties(UsersBeanProperties properties) {
        this.properties = properties;
    }

    public Node getUser() {
        return user;
    }

    public void setUser(Node user) {
        this.user = user;
    }

    ////
    public List<Substitute> getSubstitutes() {
        return substituteListDialog.getSubstitutes();
    }

    public void deleteSubstitute(ActionEvent event) {
        substituteListDialog.deleteSubstitute(event);
    }

    public void setPersonToSubstitute(String userName, Substitute substitute) {
        substituteListDialog.setPersonToSubstitute(userName, substitute);        
    }

    public void addNewValue(ActionEvent event) {
        substituteListDialog.addNewValue(event);
    }

    public String getEmailSubject() {
        return substituteListDialog.getEmailSubject();
    }

    public Map getEmailAddress() {
        return substituteListDialog.getEmailAddress();
    }
    /////

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
