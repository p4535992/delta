package ee.webmedia.alfresco.user.web;

import java.util.ArrayList;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.users.UsersBeanProperties;
import org.alfresco.web.bean.users.UsersDialog;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.ActionUtil;


public class UserDetailsDialog extends BaseDialogBean {

    private transient UserService userService;

    private static final long serialVersionUID = 1L;
    protected UsersBeanProperties properties;
    private Node user;

    /*
     * This is a read-only dialog, so we have nothing to do here (Save/OK button isn't displayed)
     */
    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        return null;
    }

    @Override
    public String cancel() {
        this.user = null;
        return super.cancel();
    }
    
    public void testLink(ActionEvent event) {
        System.out.println("LINKLINKLINK: " + ActionUtil.getParam(event, "id"));
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
       this.user = (getUserService().setUsersUnit(users)).get(0);
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
        this.user = (getUserService().setUsersUnit(users)).get(0);
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
}
