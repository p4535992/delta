package ee.webmedia.alfresco.substitute.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getParametersService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getSubstituteService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getUserService;

import java.io.Serializable;
import java.util.*;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.collections.comparators.NullComparator;
import org.apache.commons.collections.comparators.ReverseComparator;
import org.apache.commons.collections.comparators.TransformingComparator;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.substitute.model.Substitute;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.Transformer;
import ee.webmedia.alfresco.utils.UserUtil;

/**
 * Dialog for substitutes list.
 */
public class SubstituteListDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "SubstituteListDialog";
    private static final Log log = LogFactory.getLog(SubstituteListDialog.class);
    private static final TransformingComparator SUBSTITUTE_START_DATE_COMPARATOR = new TransformingComparator(new Transformer<Substitute, Date>() {
        @Override
        public Date tr(Substitute component) {
            return component.getSubstitutionStartDate();
        }
    }, new ReverseComparator(new NullComparator()));

    private NodeRef userNodeRef;
    private String username;
    private Map<String, Substitute> originalSubstitutes = new HashMap<String, Substitute>();
    private List<Substitute> substitutes;
    private Map<String, Substitute> addedSubstitutes;
    private Map<String, String> emailAddresses;

    @Override
    public void init(Map<String, String> params) {
        super.init(params);
        Node user = getUserService().getUser(AuthenticationUtil.getRunAsUser());
        userNodeRef = user.getNodeRef();
        username = (String) user.getProperties().get(ContentModel.PROP_USERNAME);
        refreshData();
    }

    public void setUserNodeRef(NodeRef userNodeRef) {
        this.userNodeRef = userNodeRef;
        username = (String) BeanHelper.getNodeService().getProperty(userNodeRef, ContentModel.PROP_USERNAME);
    }

    @SuppressWarnings("unchecked")
    public void refreshData() {
        substitutes = getSubstituteService().getSubstitutes(userNodeRef);
        Collections.sort(substitutes, SUBSTITUTE_START_DATE_COMPARATOR);
        originalSubstitutes = new HashMap<String, Substitute>();
        emailAddresses = new HashMap<String, String>();
        for (Substitute substitute : substitutes) {
            originalSubstitutes.put(substitute.getNodeRef().toString(), new Substitute(substitute));
            NodeRef personRef = BeanHelper.getUserService().getPerson(substitute.getSubstituteId());
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
        return super.cancel();
    }

    @Override
    public void clean() {
        substitutes = null;
        originalSubstitutes = null;
        addedSubstitutes = null;
        emailAddresses = null;
        username = null;
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        if (log.isDebugEnabled()) {
            log.debug("Saving substitutes changes");
        }
        save();
        isFinished = false;
        return null;
    }

    public void save() {
        if (validate()) {
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

    private boolean validate() {
        boolean isValid = true;
        for (Substitute substitute : substitutes) {
            if (substitute.equals(originalSubstitutes.get(substitute.getNodeRef().toString())) && substitute.isReadOnly()) {
                continue; // Only validate substitutes that user can change or is changed
            }
            // check mandatory fields
            if (StringUtils.isEmpty(substitute.getSubstituteName())) {
                isValid = false;
                addRequiredFieldValidationError("substitute_name");
            }
            final Date substitutionStartDate = substitute.getSubstitutionStartDate();
            if (substitutionStartDate == null) {
                isValid = false;
                addRequiredFieldValidationError("substitute_startdate");
            }
            final Date substitutionEndDate = substitute.getSubstitutionEndDate();
            if (substitutionEndDate == null) {
                isValid = false;
                addRequiredFieldValidationError("substitute_enddate");
            }

            if (isValid && substitutionStartDate.after(substitutionEndDate)) {
                isValid = false;
                MessageUtil.addErrorMessage("substitute_start_after_end");
            }

            if (isValid && substitutionEndDate.before(DateUtils.truncate(new Date(), Calendar.DATE))) {
                isValid = false;
                MessageUtil.addErrorMessage("substitute_end_before_now");
            }

            if (isValid &&
                    !getSubstituteService().findSubstitutionDutiesInPeriod(userNodeRef, substitutionStartDate, substitutionEndDate).isEmpty()) {
                isValid = false;
                if (AuthenticationUtil.getFullyAuthenticatedUser().equals(username)) {
                    MessageUtil.addErrorMessage("substitute_substitution_while_substituting");
                } else {
                    MessageUtil.addErrorMessage("substitute_substitution_while_substituting_admin");
                }
            }
            substitute.setValid(isValid);
        }
        return isValid;
    }

    private static void addRequiredFieldValidationError(String field) {
        MessageUtil.addErrorMessage("common_propertysheet_validator_mandatory", MessageUtil.getMessage(field));
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
        BeanHelper.getNotificationService().notifySubstitutionEvent(savedSubstitutes);
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

        NodeRef personNodeRef = BeanHelper.getUserService().getPerson(userName);
        if (personNodeRef == null) {
            return;
        }

        Map<QName, Serializable> personProps = getNodeService().getProperties(personNodeRef);
        substitute.setSubstituteId(userName);
        substitute.setSubstituteName(UserUtil.getPersonFullName1(personProps));
        addEmailAddress(substitute, personNodeRef);
    }

    public void addNewValue(ActionEvent event) {
        Substitute newSubstitute = Substitute.newInstance();
        newSubstitute.setReplacedPersonUserName(username);
        substitutes.add(0, newSubstitute);
        addedSubstitutes.put(newSubstitute.getNodeRef().toString(), newSubstitute);
    }

    @Override
    public Object getActionsContext() {
        // since we are using actions, but not action context,
        // we don't need instance of NavigationBean that is used in the overloadable method
        return null;
    }

    // default implementation that doesn't send any notification

    // used from JSP
    public String getEmailSubject() {
        return getParametersService().getStringParameter(Parameters.EMAIL_FOR_SUBSTITUTE_SUBJECT);
    }

    // used from JSP
    public Map<String, String> getEmailAddress() {
        return emailAddresses;
    }
}
