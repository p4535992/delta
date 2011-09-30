package ee.webmedia.alfresco.document.forum.web;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.cxf.common.util.StringUtils;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.email.service.EmailException;
import ee.webmedia.alfresco.email.service.EmailService;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.service.ParametersService;
import ee.webmedia.alfresco.template.service.DocumentTemplateService;
import ee.webmedia.alfresco.user.model.Authority;
import ee.webmedia.alfresco.user.web.PermissionsAddDialog;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UserUtil;

public class InviteUsersDialog extends PermissionsAddDialog {
    private static final long serialVersionUID = 1L;
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(InviteUsersDialog.class);

    private transient EmailService emailService;
    private transient DocumentTemplateService templateService;
    private transient ParametersService parametersService;

    private String templateName;

    @Override
    public void setup(ActionEvent event) {
        templateName = ActionUtil.getParam(event, "templateName");
        super.setup(event);
    }

    @Override
    public void init(Map<String, String> params) {
        super.init(params);
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        if (templateName != null) {
            notifySelectedUsers();
        } else {
            MessageUtil.addErrorMessage(context, "forum_template_not_specified");
        }
        return super.finishImpl(context, outcome);
    }

    private void notifySelectedUsers() {
        @SuppressWarnings("unchecked")
        List<Authority> auth = (List<Authority>) getAuthorities().getWrappedData();
        List<String> toEmails = new ArrayList<String>(auth.size());
        List<String> toNames = new ArrayList<String>(auth.size());
        String fromEmail = getParametersService().getStringParameter(Parameters.TASK_SENDER_EMAIL);
        if (StringUtils.isEmpty(fromEmail)) {
            log.debug("Sending invitation to discussion failed, task sender email is not configured!");
            return;
        }

        final List<ChildAssociationRef> parentAssocs = getNodeService().getParentAssocs(getNodeRef()); // Get the parent document
        NodeRef documentNodeRef = parentAssocs.get(0).getParentRef();
        String subject = MessageUtil.getMessage(FacesContext.getCurrentInstance(), "forum_invited_to_subject",
                getNodeService().getProperty(documentNodeRef, DocumentCommonModel.Props.DOC_NAME));
        if (templateName == null) {
            log.debug("Sending invitation to discussion failed, template to be used is not specified!");
            return;
        }
        NodeRef templateNodeRef = getDocumentTemplateService().getSystemTemplateByName(templateName);
        if (templateNodeRef == null) {
            log.debug("Discussion invitation email template '" + templateName + "' not found, no email notification is sent");
            // Ignore, because sending a message is a bonus across the system
            return;
        }
        LinkedHashMap<String, NodeRef> nodeRefs = new LinkedHashMap<String, NodeRef>();
        nodeRefs.put(null, documentNodeRef);
        String content = getDocumentTemplateService().getProcessedEmailTemplate(nodeRefs, templateNodeRef);

        for (Authority a : auth) {
            String authority = a.getAuthority();
            if (a.isGroup()) {
                Set<String> containedAuthorities = BeanHelper.getUserService().getUserNamesInGroup(authority);
                for (String user : containedAuthorities) {
                    addToEmails(user, toEmails, toNames);
                }
            } else {
                addToEmails(authority, toEmails, toNames);
            }
        }

        try {
            emailService.sendEmail(toEmails, toNames, fromEmail, subject, content, true, null, null, false, null);
        } catch (EmailException e) {
            log.error("Discussion invitation notification e-mail sending failed, ignoring and continuing", e);
            return;
        }
    }

    private void addToEmails(String user, List<String> toEmails, List<String> toNames) {
        final Map<String, Object> props = getUserService().getUser(user).getProperties();
        if (props.get(ContentModel.PROP_EMAIL) != null) {
            String email = props.get(ContentModel.PROP_EMAIL).toString();
            if (!toEmails.contains(email)) {
                toEmails.add(email);
                toNames.add(UserUtil.getPersonFullName2(props, false));
            }
        }
    }

    protected EmailService getEmailService() {
        if (emailService == null) {
            emailService = (EmailService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
                    .getBean(EmailService.BEAN_NAME);
        }
        return emailService;
    }

    public void setEmailService(EmailService emailService) {
        this.emailService = emailService;
    }

    public void setTemplateService(DocumentTemplateService templateService) {
        this.templateService = templateService;
    }

    protected DocumentTemplateService getDocumentTemplateService() {
        if (templateService == null) {
            templateService = (DocumentTemplateService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
                    .getBean(DocumentTemplateService.BEAN_NAME);
        }
        return templateService;
    }

    public void setParametersService(ParametersService parametersService) {
        this.parametersService = parametersService;
    }

    protected ParametersService getParametersService() {
        if (parametersService == null) {
            parametersService = (ParametersService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
                    .getBean(ParametersService.BEAN_NAME);
        }
        return parametersService;
    }
}