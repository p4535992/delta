package ee.webmedia.alfresco.substitute.web;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.util.GUID;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Repository;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.service.ParametersService;
import ee.webmedia.alfresco.substitute.model.Substitute;
import ee.webmedia.alfresco.substitute.service.SubstituteService;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Dialog for substitutes list.
 * 
 * @author Romet Aidla
 */
public class SubstituteListDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;
    private static final Log log = LogFactory.getLog(SubstituteListDialog.class);

    private transient SubstituteService substituteService;
    private transient PersonService personService;
    private transient ParametersService parametersService;
    private transient UserService userService;

    private NodeRef userNodeRef;
    private Map<String, Substitute> originalSubstitutes = new HashMap<String, Substitute>();
    private List<Substitute> substitutes;
    private Map<String, Substitute> addedSubstitutes;
    private Map<String, String> emailAddresses;
    private NotificationSender notificationSender = new EmptyNotificationSender();

    @Override
    public void init(Map<String, String> params) {
        super.init(params);
        userNodeRef = getUserService().getUser(AuthenticationUtil.getRunAsUser()).getNodeRef();
        refreshData();
    }

    public void setUserNodeRef(NodeRef userNodeRef) {
        this.userNodeRef = userNodeRef;
    }

    public void refreshData() {
        substitutes = getSubstituteService().getSubstitutes(userNodeRef);
        originalSubstitutes = new HashMap<String, Substitute>();
        emailAddresses = new HashMap<String, String>();
        for (Substitute substitute : substitutes) {
            originalSubstitutes.put(substitute.getNodeRef().toString(), new Substitute(substitute));
            NodeRef personRef = getPersonService().getPerson(substitute.getSubstituteId());
            addEmailAddress(substitute, personRef);
        }
        addedSubstitutes = new HashMap<String, Substitute>();
    }

    private void addEmailAddress(Substitute substitute, NodeRef personRef) {
        if (personRef != null) {
            emailAddresses.put(substitute.getSubstituteId(), (String) getNodeService().getProperty(personRef, ContentModel.PROP_EMAIL));
        }
    }

    public List<Substitute> getSubstitutes() {
        return substitutes;
    }

    @Override
    public String cancel() {
        substitutes = null;
        originalSubstitutes = null;
        addedSubstitutes = null;
        emailAddresses = null;
        return super.cancel();
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        if (log.isDebugEnabled()) {
            log.debug("Saving substitutes changes");
        }
        save(context);
        isFinished = false;
        return null;
    }

    public void save(FacesContext context) {
        if (validate(context)) {
            List<Substitute> savedSubstitutes = new ArrayList<Substitute>();
            addSubstitutes(savedSubstitutes);
            updateSubstitutes(savedSubstitutes);
            sendNotificationEmails(savedSubstitutes);
            refreshData();
            if (log.isDebugEnabled()) {
                log.debug("Substitutes changes saved");
            }
            MessageUtil.addInfoMessage("save_success");
        }
    }

    private boolean validate(FacesContext context) {
        boolean isValid = true;
        for (Substitute substitute : substitutes) {
            if (substitute.isReadOnly()) {
                continue; // Only validate substitutes that user can change
            }
            // check mandatory fields
            if (StringUtils.isEmpty(substitute.getSubstituteName())) {
                isValid = false;
                addRequiredFieldValidationError(context, "substitute_name");
            }
            final Date substitutionStartDate = substitute.getSubstitutionStartDate();
            if (substitutionStartDate == null) {
                isValid = false;
                addRequiredFieldValidationError(context, "substitute_startdate");
            }
            final Date substitutionEndDate = substitute.getSubstitutionEndDate();
            if (substitutionEndDate == null) {
                isValid = false;
                addRequiredFieldValidationError(context, "substitute_enddate");
            }

            if (isValid && substitutionStartDate.after(substitutionEndDate)) {
                isValid = false;
                MessageUtil.addErrorMessage(context, "substitute_start_after_end");
            }

            if (isValid && substitutionEndDate.before(new Date())) {
                isValid = false;
                MessageUtil.addErrorMessage(context, "substitute_end_before_now");
            }

            if (isValid
                    && !getSubstituteService().findSubstitutionDutiesInPeriod(AuthenticationUtil.getRunAsUser(), substitutionStartDate, substitutionEndDate)
                            .isEmpty()) {
                isValid = false;
                MessageUtil.addErrorMessage(context, "substitute_substitution_while_substituting");
            }

        }
        return isValid;
    }

    private static void addRequiredFieldValidationError(FacesContext context, String field) {
        MessageUtil.addErrorMessage(context, "common_propertysheet_validator_mandatory", MessageUtil.getMessage(context, field));
    }

    @Override
    public boolean getFinishButtonDisabled() {
        return false; // finish button is always enabled
    }

    private void updateSubstitutes(List<Substitute> savedSubstitutes) {
        for (Substitute substitute : substitutes) {
            Substitute originalSubstitute = originalSubstitutes.get(substitute.getNodeRef().toString());
            if (originalSubstitute == null) {
                continue;
            }

            if (!originalSubstitute.equals(substitute)) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Updating the substitute with nodeRef = %s", substitute.getNodeRef()));
                }
                getSubstituteService().updateSubstitute(substitute);
                savedSubstitutes.add(substitute);
            }
        }
    }

    private void addSubstitutes(List<Substitute> savedSubstitutes) {
        if (addedSubstitutes != null && addedSubstitutes.size() > 0) {
            for (Substitute addedSubstitute : addedSubstitutes.values()) {
                getSubstituteService().addSubstitute(userNodeRef, addedSubstitute);
                savedSubstitutes.add(addedSubstitute);
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Added new substitute: %s", addedSubstitute.getSubstituteName()));
                }
            }

            addedSubstitutes.clear();
        }
    }

    private void sendNotificationEmails(List<Substitute> savedSubstitutes) {
        for (Substitute savedSubstitute : savedSubstitutes) {
            notificationSender.sendNotification(savedSubstitute);
        }
    }

    public void deleteSubstitute(ActionEvent event) {
        String ref = ActionUtil.getParam(event, "nodeRef");
        if (log.isDebugEnabled()) {
            log.debug(String.format("Starting to delete substitute with nodeRef = %s.", ref));
        }
        NodeRef subsNodeRef = new NodeRef(ref);
        if (originalSubstitutes.containsKey(ref)) {
            originalSubstitutes.remove(ref);
            getSubstituteService().deleteSubstitute(subsNodeRef);
        } else {
            addedSubstitutes.remove(ref);
        }
        substitutes.remove(findSubstitute(subsNodeRef));
        if (log.isDebugEnabled()) {
            log.debug(String.format("Substitute with nodeRef = %s deleted.", ref));
        }
        MessageUtil.addInfoMessage("substitute_remove_success");
    }

    private Substitute findSubstitute(NodeRef subsNodeRef) {
        for (Substitute substitute : substitutes) {
            if (substitute.getNodeRef().equals(subsNodeRef)) {
                return substitute;
            }
        }
        throw new RuntimeException(String.format("Substitute with nodeRef = %s was not found.", subsNodeRef.toString()));
    }

    public void setPersonToSubstitute(String userName, Substitute substitute) {
        Assert.hasText(userName, "User name not provided");
        Assert.notNull(substitute, "Substitute not provided");

        NodeRef personNodeRef = getPersonService().getPerson(userName);
        String firstName = (String) getNodeService().getProperty(personNodeRef, ContentModel.PROP_FIRSTNAME);
        String lastName = (String) getNodeService().getProperty(personNodeRef, ContentModel.PROP_LASTNAME);

        substitute.setSubstituteId(userName);
        substitute.setSubstituteName(firstName + " " + lastName);
        addEmailAddress(substitute, personNodeRef);
    }

    public void addNewValue(ActionEvent event) {
        Substitute newSubstitute = new Substitute();
        newSubstitute.setValid(false);
        // set the temporary random unique ID to be used in the UI form
        newSubstitute.setNodeRef(new NodeRef(newSubstitute.hashCode() + "", event.hashCode() + "", GUID.generate()));
        substitutes.add(newSubstitute);
        addedSubstitutes.put(newSubstitute.getNodeRef().toString(), newSubstitute);
    }

    @Override
    public Object getActionsContext() {
        // since we are using actions, but not action context,
        // we don't need instance of NavigationBean that is used in the overloadable method
        return null;
    }

    public static interface NotificationSender extends Serializable {
        void sendNotification(Substitute substitute);
    }

    // default implementation that doesn't send any notification
    public static class EmptyNotificationSender implements NotificationSender {
        private static final long serialVersionUID = 1L;

        @Override
        public void sendNotification(Substitute substitute) {
            // to nothing
        }
    }

    public void setNotificationSender(NotificationSender notificationSender) {
        this.notificationSender = notificationSender;
    }

    // used from JSP
    public String getEmailSubject() {
        return getParametersService().getStringParameter(Parameters.EMAIL_FOR_SUBSTITUTE_SUBJECT);
    }

    // used from JSP
    public Map<String, String> getEmailAddress() {
        return emailAddresses;
    }

    protected SubstituteService getSubstituteService() {
        if (substituteService == null) {
            substituteService = (SubstituteService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
                    .getBean(SubstituteService.BEAN_NAME);
        }
        return substituteService;
    }

    protected PersonService getPersonService() {
        if (personService == null) {
            personService = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getPersonService();
        }
        return personService;
    }

    protected ParametersService getParametersService() {
        if (parametersService == null) {
            parametersService = (ParametersService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
                    .getBean(ParametersService.BEAN_NAME);
        }
        return parametersService;
    }

    protected UserService getUserService() {
        if (userService == null) {
            userService = (UserService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
                    .getBean(UserService.BEAN_NAME);
        }
        return userService;
    }
}
