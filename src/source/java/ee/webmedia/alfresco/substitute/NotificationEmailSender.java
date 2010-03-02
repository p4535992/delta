package ee.webmedia.alfresco.substitute;

import ee.webmedia.alfresco.email.service.EmailException;
import ee.webmedia.alfresco.email.service.EmailService;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.service.ParametersService;
import ee.webmedia.alfresco.substitute.model.Substitute;
import ee.webmedia.alfresco.substitute.web.SubstituteListDialog;
import ee.webmedia.alfresco.template.service.DocumentTemplateService;
import ee.webmedia.alfresco.utils.MessageUtil;
import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.web.bean.repository.Repository;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;
import org.springframework.web.jsf.FacesContextUtils;

import javax.faces.context.FacesContext;
import java.util.Arrays;
import java.util.LinkedHashMap;

/**
 * Notification send that send notification as email.
 *
 * @author Romet Aidla
 */
public class NotificationEmailSender implements SubstituteListDialog.NotificationSender {
    private static final long serialVersionUID = 1L;
    private static final Log log = LogFactory.getLog(NotificationEmailSender.class);

    private static final String EMAIL_TEMPLATE_NAME = "Teavitus asendajale.html";
    private transient DocumentTemplateService documentTemplateService;
    private transient EmailService emailService;
    private transient NodeService nodeService;
    private transient PersonService personService;
    private transient ParametersService parametersService;

    @Override
    public void sendNotification(Substitute substitute) {
        if (log.isDebugEnabled()) log.debug("Sending notification email to substitute: " + substitute.getSubstituteId());
        NodeRef templateNodeRef = getDocumentTemplateService().getSystemTemplateByName(EMAIL_TEMPLATE_NAME);
        String emailBody = "";
        if (templateNodeRef != null) {
            LinkedHashMap<String, NodeRef> nodeRefs = new LinkedHashMap<String, NodeRef>();
            nodeRefs.put("default", substitute.getNodeRef());
            emailBody = getDocumentTemplateService().getProcessedEmailTemplate(nodeRefs, templateNodeRef);
        }

        NodeRef personRef = getPersonService().getPerson(substitute.getSubstituteId());
        if (personRef == null) {
            if (log.isDebugEnabled()) log.debug("Person not found, not notification email is sent");
            return;
        }

        String toEmailAddress = (String) getNodeService().getProperty(personRef, ContentModel.PROP_EMAIL);

        if (StringUtils.isEmpty(toEmailAddress)) {
            if (log.isDebugEnabled()) log.debug("Person doesn't have email address defined, no notification is sent.");
            return;
        }

        String fromEmailAddress = getParametersService().getStringParameter(Parameters.TASK_SENDER_EMAIL);

        Assert.hasLength(fromEmailAddress, "Sender email address not found");
        try {
            getEmailService().sendEmail(
                Arrays.asList(toEmailAddress),
                Arrays.asList(substitute.getSubstituteName()),
                fromEmailAddress,
                MessageUtil.getMessage("substitute_notification_subject"),
                emailBody,
                true,
                null,
                null,
                false,
                null);
        } catch (EmailException ee) {
            // just wrap it to runtime exception
            throw new RuntimeException("Cannot send notification email", ee);
        }
        if (log.isDebugEnabled()) log.debug("Notification email sent");
    }


    protected DocumentTemplateService getDocumentTemplateService() {
        if (documentTemplateService == null) {
            documentTemplateService = (DocumentTemplateService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
                    .getBean(DocumentTemplateService.BEAN_NAME);
        }
        return documentTemplateService;
    }

    protected EmailService getEmailService() {
        if (emailService == null) {
            emailService = (EmailService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
                    .getBean(EmailService.BEAN_NAME);
        }
        return emailService;
    }

    protected NodeService getNodeService() {
        if (nodeService == null) {
            nodeService = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getNodeService();
        }
        return nodeService;
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
}
