package ee.webmedia.alfresco.document.assignresponsibility.web;

import java.io.Serializable;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.TransientNode;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.document.assignresponsibility.model.AssignResponsibilityModel;
import ee.webmedia.alfresco.document.assignresponsibility.service.AssignResponsibilityService;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.service.ParametersService;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.user.web.UserDetailsDialog;

/**
 * @author Alar Kvell
 */
public class AssignResponsibilityBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private String OWNER_ID = "{temp}ownerId";

    private transient AssignResponsibilityService assignResponsibilityService;
    private transient ParametersService parametersService;
    private transient UserService userService;

    private Node node;
    private String fromOwnerId;

    public Node getNode() {
        if (node == null) {
            node = new TransientNode(AssignResponsibilityModel.Types.ASSIGN_RESPONSIBILITY, null, null);
        }
        return node;
    }

    public void setOwner(String userName) {
        String fullName = getUSerService().getUserFullName(userName);
        Map<String, Object> props = getNode().getProperties();
        props.put(AssignResponsibilityModel.Props.OWNER_NAME.toString(), fullName);
        props.put(OWNER_ID, userName);
    }

    public boolean isOwnerUnset() {
        return StringUtils.isBlank((String) getNode().getProperties().get(OWNER_ID));
    }

    public void execute(ActionEvent event) {
        String toOwnerId = (String) getNode().getProperties().get(OWNER_ID);
        getAssignResponsibilityService().changeOwnerOfAllDocumentsAndTasks(fromOwnerId, toOwnerId);
    }

    public String getInstruction() {
        return getParametersService().getStringParameter(Parameters.ASSIGN_RESPONSIBILITY_INSTRUCTION);
    }

    public boolean isInstructionSet() {
        return StringUtils.isNotBlank(getInstruction());
    }

    public String getSetFromOwnerUserConsole() {
        fromOwnerId = AuthenticationUtil.getRunAsUser();
        return "";
    }

    public String getSetFromOwnerUserDetails() {
        UserDetailsDialog userDetailsDialog = (UserDetailsDialog) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), "UserDetailsDialog");
        fromOwnerId = (String) userDetailsDialog.getUser().getProperties().get(ContentModel.PROP_USERNAME);
        return "";
    }

    protected AssignResponsibilityService getAssignResponsibilityService() {
        if (assignResponsibilityService == null) {
            assignResponsibilityService = (AssignResponsibilityService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance()).getBean(AssignResponsibilityService.BEAN_NAME);
        }
        return assignResponsibilityService;
    }

    protected ParametersService getParametersService() {
        if (parametersService == null) {
            parametersService = (ParametersService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance()).getBean(
                    ParametersService.BEAN_NAME);
        }
        return parametersService;
    }

    protected UserService getUSerService() {
        if (userService == null) {
            userService = (UserService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance()).getBean(UserService.BEAN_NAME);
        }
        return userService;
    }

}
