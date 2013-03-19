package ee.webmedia.alfresco.document.assignresponsibility.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getAssignResponsibilityService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getParametersService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getUserService;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.TransientNode;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.assignresponsibility.model.AssignResponsibilityModel;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.user.model.UserModel;
import ee.webmedia.alfresco.user.web.UserDetailsDialog;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UserUtil;

/**
 * @author Alar Kvell
 */
public class AssignResponsibilityBean implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String BEAN_NAME = "AssignResponsibilityBean";
    private final String OWNER_ID = "{temp}ownerId";

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

    public void unsetOwner() {
        node = null;
    }

    public boolean isOwnerUnset() {
        return StringUtils.isBlank((String) getNode().getProperties().get(OWNER_ID));
    }

    public void execute(@SuppressWarnings("unused") ActionEvent event) {
        String toOwnerId = (String) getNode().getProperties().get(OWNER_ID);
        getAssignResponsibilityService().changeOwnerOfAllDesignatedObjects(fromOwnerId, toOwnerId, true);
        leaving = true;
        MessageUtil.addInfoMessage("assign_responsibility_perform_success", UserUtil.getPersonFullName1(BeanHelper.getUserService().getUserProperties(toOwnerId)));
    }

    public void revert(@SuppressWarnings("unused") ActionEvent event) {
        String toOwnerId = (String) getUserService().getUserProperties(fromOwnerId).get(UserModel.Props.LIABILITY_GIVEN_TO_PERSON_ID);
        getAssignResponsibilityService().changeOwnerOfAllDesignatedObjects(fromOwnerId, toOwnerId, false);
        leaving = false;
        unsetOwner();
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

    public void updateLiabilityGivenToPerson(Node user) {
        leaving = user.hasAspect(UserModel.Aspects.LEAVING);
        if (leaving) {
            String liabilityGivenToPersonId = (String) user.getProperties().get(UserModel.Props.LIABILITY_GIVEN_TO_PERSON_ID);
            String fullName = getUserService().getUserFullName(liabilityGivenToPersonId);
            Map<String, Object> props = getNode().getProperties();
            props.put(AssignResponsibilityModel.Props.OWNER_NAME.toString(), fullName);
            props.put(OWNER_ID, liabilityGivenToPersonId);
        } else {
            unsetOwner();
        }
    }

}
