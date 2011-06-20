package ee.webmedia.alfresco.document.assignresponsibility.web;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.web.bean.repository.TransientNode;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.document.assignresponsibility.model.AssignResponsibilityModel;
import ee.webmedia.alfresco.document.assignresponsibility.service.AssignResponsibilityService;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.service.ParametersService;
import ee.webmedia.alfresco.user.model.UserModel;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.user.web.UserDetailsDialog;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UserUtil;

/**
 * @author Alar Kvell
 */
public class AssignResponsibilityBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String OWNER_ID = "{temp}ownerId";

    private transient AssignResponsibilityService assignResponsibilityService;
    private transient ParametersService parametersService;
    private transient UserService userService;
    private transient PersonService personService;
    private transient NodeService nodeService;

    private Node node;
    private String fromOwnerId;
    private boolean leaving;

    public Node getNode() {
        if (node == null) {
            node = new TransientNode(AssignResponsibilityModel.Types.ASSIGN_RESPONSIBILITY, null, null);
        }
        return node;
    }

    public void setOwner(String userName) {
        String fullName = getUserService().getUserFullName(userName);
        Map<String, Object> props = getNode().getProperties();
        props.put(AssignResponsibilityModel.Props.OWNER_NAME.toString(), fullName);
        props.put(OWNER_ID, userName);
    }

    public boolean isOwnerUnset() {
        return StringUtils.isBlank((String) getNode().getProperties().get(OWNER_ID));
    }

    public void execute(@SuppressWarnings("unused") ActionEvent event) {
        String toOwnerId = (String) getNode().getProperties().get(OWNER_ID);
        getAssignResponsibilityService().changeOwnerOfAllDocumentsAndTasks(fromOwnerId, toOwnerId, true);
        leaving = true;
        MessageUtil.addInfoMessage("assign_responsibility_perform_success", UserUtil.getPersonFullName1(getPersonProps(toOwnerId)));
    }

    public void revert(@SuppressWarnings("unused") ActionEvent event) {
        String toOwnerId = (String) getUserService().getUserProperties(fromOwnerId).get(UserModel.Props.LIABILITY_GIVEN_TO_PERSON_ID);
        getAssignResponsibilityService().changeOwnerOfAllDocumentsAndTasks(fromOwnerId, toOwnerId, false);
        leaving = false;
        MessageUtil.addInfoMessage("assign_responsibility_revert_success");
    }

    public String getAssingResponsibilityMessage() {
        Node currentUser = getUserService().getUser(AuthenticationUtil.getRunAsUser());

        if (currentUser.hasAspect(UserModel.Aspects.LEAVING)) {
            Map<String, Object> properties = currentUser.getProperties();
            DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
            StringBuilder builder = new StringBuilder();
            builder.append("<div class=\"message message-red\">");
            builder.append(MessageUtil.getMessage(FacesContext.getCurrentInstance(), "assign_responsibility_message",
                    dateFormat.format(properties.get(UserModel.Props.LEAVING_DATE_TIME)),
                    getUserService().getUserFullName((String) properties.get(UserModel.Props.LIABILITY_GIVEN_TO_PERSON_ID)
                            )));
            builder.append("</div>");

            return builder.toString();
        }

        return "";
    }

    public String getInstruction() {
        return getParametersService().getStringParameter(Parameters.ASSIGN_RESPONSIBILITY_INSTRUCTION);
    }

    public boolean isInstructionSet() {
        return StringUtils.isNotBlank(getInstruction());
    }

    public boolean isNotLeaving() {
        return !leaving;
    }

    public boolean isLeavingAndAdmin() {
        return leaving && getUserService().isAdministrator();
    }

    public boolean isPanelVisible() {
        return isNotLeaving() || isLeavingAndAdmin();
    }

    public String getSetFromOwnerUserConsole() {
        fromOwnerId = AuthenticationUtil.getRunAsUser();
        leaving = getUserService().getUser(fromOwnerId).hasAspect(UserModel.Aspects.LEAVING);
        return "";
    }

    public String getSetFromOwnerUserDetails() {
        UserDetailsDialog userDetailsDialog = (UserDetailsDialog) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), "UserDetailsDialog");
        userDetailsDialog.refreshCurrentUser();
        Node user = userDetailsDialog.getUser();
        leaving = user.hasAspect(UserModel.Aspects.LEAVING);
        fromOwnerId = (String) user.getProperties().get(ContentModel.PROP_USERNAME);
        return "";
    }

    private Map<QName, Serializable> getPersonProps(String userName) {
        NodeRef person = getPersonService().getPerson(userName);
        Map<QName, Serializable> personProps = getNodeService().getProperties(person);
        return personProps;
    }

    private PersonService getPersonService() {
        if (personService == null) {
            personService = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getPersonService();
        }
        return personService;
    }

    private NodeService getNodeService() {
        if (nodeService == null) {
            nodeService = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getNodeService();
        }
        return nodeService;
    }

    protected AssignResponsibilityService getAssignResponsibilityService() {
        if (assignResponsibilityService == null) {
            assignResponsibilityService = (AssignResponsibilityService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
            .getBean(AssignResponsibilityService.BEAN_NAME);
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

    protected UserService getUserService() {
        if (userService == null) {
            userService = (UserService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance()).getBean(UserService.BEAN_NAME);
        }
        return userService;
    }

}
