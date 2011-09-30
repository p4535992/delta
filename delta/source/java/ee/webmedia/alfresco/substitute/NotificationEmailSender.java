package ee.webmedia.alfresco.substitute;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentTemplateService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getEmailService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getNodeService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getParametersService;

import java.util.Arrays;
import java.util.LinkedHashMap;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.email.service.EmailException;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.substitute.model.Substitute;
import ee.webmedia.alfresco.substitute.web.SubstituteListDialog;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Notification send that send notification as email.
 * 
 * @author Romet Aidla
 */
public class NotificationEmailSender implements SubstituteListDialog.NotificationSender {
    private static final long serialVersionUID = 1L;
    private static final Log log = LogFactory.getLog(NotificationEmailSender.class);

    private static final String EMAIL_TEMPLATE_NAME = "Teavitus asendajale.html";

    @Override
    public void sendNotification(Substitute substitute) {
        String substituteId = substitute.getSubstituteId();
        if (log.isDebugEnabled()) {
            log.debug("Sending notification email to substitute: " + substituteId);
        }
        NodeRef templateNodeRef = getDocumentTemplateService().getSystemTemplateByName(EMAIL_TEMPLATE_NAME);
        if (templateNodeRef == null) {
            if (log.isDebugEnabled()) {
                log.debug("Substitute notification email template '" + EMAIL_TEMPLATE_NAME + "' not found, no notification email is sent");
            }
            return;
        }

        LinkedHashMap<String, NodeRef> nodeRefs = new LinkedHashMap<String, NodeRef>();
        nodeRefs.put(null, substitute.getNodeRef());
        String emailBody = getDocumentTemplateService().getProcessedEmailTemplate(nodeRefs, templateNodeRef);

        NodeRef personRef = BeanHelper.getUserService().getPerson(substituteId);
        if (personRef == null) {
            if (log.isDebugEnabled()) {
                log.debug("Person '" + substituteId + "' not found, no notification email is sent");
            }
            return;
        }

        String toEmailAddress = (String) getNodeService().getProperty(personRef, ContentModel.PROP_EMAIL);

        if (StringUtils.isEmpty(toEmailAddress)) {
            if (log.isDebugEnabled()) {
                log.debug("Person '" + substituteId + "' doesn't have email address defined, no notification email is sent");
            }
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
            if (log.isDebugEnabled()) {
                log.debug("Cannot send notification email", ee);
            }
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug("Notification email sent to person '" + substituteId + "' with email address '" + toEmailAddress + "'");
        }
    }

}
