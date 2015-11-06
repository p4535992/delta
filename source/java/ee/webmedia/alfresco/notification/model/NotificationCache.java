package ee.webmedia.alfresco.notification.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.email.model.EmailAttachment;
import ee.webmedia.alfresco.template.model.DocumentTemplateModel;

public class NotificationCache {

    private final Map<NodeRef, Map<String, String>> cachedFormulas = new HashMap<>();
    private final Map<NodeRef, NodeRef> workflowRefToCWFRef = new HashMap<>();
    private final Map<NodeRef, Pair<String, NodeRef>> cwfRefToTypeStrAndParentRef = new HashMap<>();
    private final Map<String, Template> cachedTemplates = new HashMap<>();
    private final Map<NodeRef, Node> cachedProps = new HashMap<>();
    private final Map<NodeRef, List<EmailAttachment>> attachments = new HashMap<>();
    private final Map<NodeRef, List<EmailAttachment>> zippedAttachments = new HashMap<>();

    public Map<NodeRef, Map<String, String>> getFormulas() {
        return cachedFormulas;
    }

    public Map<NodeRef, NodeRef> getWorkflowRefToCWFRef() {
        return workflowRefToCWFRef;
    }

    public Map<NodeRef, Pair<String, NodeRef>> getCwfRefToTypeStrAndParentRef() {
        return cwfRefToTypeStrAndParentRef;
    }

    public boolean foundTemplate(String templateName) {
        return cachedTemplates.containsKey(templateName);
    }

    public Map<NodeRef, Node> getCachedProps() {
        return cachedProps;
    }

    public Map<NodeRef, List<EmailAttachment>> getAttachments() {
        return attachments;
    }

    public Map<NodeRef, List<EmailAttachment>> getZippedAttachments() {
        return zippedAttachments;
    }

    public Template getTemplate(String templateName) {
        if (cachedTemplates.containsKey(templateName)) {
            return cachedTemplates.get(templateName);
        }
        NodeRef templateRef = BeanHelper.getDocumentTemplateService().getNotificationTemplateByName(templateName);
        Template template = null;
        if (templateRef == null) { // didn't find template
            cachedTemplates.put(templateName, null);
        } else {
            template = new Template(templateRef);
            cachedTemplates.put(templateName, template);
        }
        return template;
    }

    public class Template {

        private final String content;
        private final String subject;

        public Template(NodeRef templateRef) {
            ContentReader templateReader = BeanHelper.getFileFolderService().getReader(templateRef);
            content = templateReader.getContentString();
            String sub = (String) BeanHelper.getNodeService().getProperty(templateRef, DocumentTemplateModel.Prop.NOTIFICATION_SUBJECT);
            subject = sub == null ? "" : sub;
        }

        public String getContent() {
            return new String(content);
        }

        public String getSubject() {
            return new String(subject);
        }
    }
}