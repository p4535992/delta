package ee.webmedia.alfresco.document.sendout.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;

import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.MD5;
import org.alfresco.util.URLEncoder;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.web.evaluator.DocumentSendForInformationEvaluator;
import ee.webmedia.alfresco.log.model.LogEntry;
import ee.webmedia.alfresco.log.model.LogObject;
import ee.webmedia.alfresco.template.model.ProcessedEmailTemplate;
import ee.webmedia.alfresco.template.model.UnmodifiableDocumentTemplate;
import ee.webmedia.alfresco.user.model.Authority;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

public class DocumentSendForInformationDialog extends BaseDialogBean {

    private static final long serialVersionUID = 1L;

    protected List<String> authorities;
    protected String selectedEmailTemplate;
    protected List<SelectItem> emailTemplates;
    protected Node contentNode;
    protected String subject;
    protected String content;
    private static String requestContextPath;
    
    static {
      requestContextPath = FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath();
      if (!StringUtils.endsWith(requestContextPath, "/")) {
          requestContextPath += "/";
      }
    }
    
    public String init() {
        Node contentNode = getNode();
        contentNode.clearPermissionsCache();
        if (hasNoPermission(contentNode)) {
            return null;
        }
        if (!getNodeService().exists(contentNode.getNodeRef())) {
            return AlfrescoNavigationHandler.CLOSE_DIALOG_OUTCOME;
        }
        setDefaultMessage();
        return getDialogText();
    }

    protected Node getNode() {
        return BeanHelper.getDocumentDialogHelperBean().getNode();
    }

    protected String getDialogText() {
        return "dialog:documentSendForInformationDialog";
    }

    protected boolean hasNoPermission(Node contentNode) {
        boolean hasNoPermission = !evaluatePermission(contentNode);
        if (hasNoPermission) {
            MessageUtil.addErrorMessage("document_send_for_information_error_noPermission");
            return true;
        }
        return false;
    }

    protected boolean evaluatePermission(Node contentNode) {
        return new DocumentSendForInformationEvaluator().evaluate(contentNode);
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        if (validate()) {
            long notificationId = BeanHelper.getSendOutService().sendForInformation(authorities, contentNode, selectedEmailTemplate, subject, content);
            addSuccessMessage();
            createLogEntry(notificationId);
            return outcome;
        }
        return null;
    }

    protected void addSuccessMessage() {
        MessageUtil.addInfoMessage("document_send_for_information_success");
    }

    protected void createLogEntry(long notificationLogId) {
        UserService userService = BeanHelper.getUserService();
        String logContent = content.replaceAll("(?s)<!--.*?-->", "").replaceAll("\\<.*?\\>", "");
        BeanHelper.getLogService().addLogEntry(
                LogEntry.create(getLogObject(), userService, contentNode.getNodeRef(), getLogMessage(), getUsersAndGroups(userService, notificationLogId),
                        StringEscapeUtils.unescapeHtml(logContent)));
    }

	protected String getUsersAndGroups(UserService userService, long notificationLogId) {
        List<String> usersAndGroups = new ArrayList<String>();
        for (String authorityId : authorities) {
            Authority authority = userService.getAuthorityOrNull(authorityId);
            if (authority == null) {
                continue;
            }
            if (authority.isGroup()) {
            	String authorityName = authority.getName();
            	StringBuilder htmlLogEntry = new StringBuilder();                        	
            	htmlLogEntry.append("<a href=\"");
            	htmlLogEntry.append(requestContextPath);
            	htmlLogEntry.append("printTable?tableMode=NOTIFICATION_LOG&notificationLogId=");
            	htmlLogEntry.append(notificationLogId);
            	htmlLogEntry.append("&userGroup=");
            	htmlLogEntry.append(URLEncoder.encode(authorityName));            	
            	htmlLogEntry.append("&userGroupHash=");
            	htmlLogEntry.append(MD5.Digest(authorityName.getBytes()));
            	htmlLogEntry.append("\" target='_blank'>");
            	htmlLogEntry.append(authorityName);
            	htmlLogEntry.append("</a>");
              usersAndGroups.add(htmlLogEntry.toString());
            } else {
                usersAndGroups.add(userService.getUserFullName(authorityId));
            }
        }
        return StringUtils.join(usersAndGroups, ", ");
    }

    protected boolean validate() {
        if (authorities == null || authorities.isEmpty()) {
            MessageUtil.addErrorMessage("common_propertysheet_validator_mandatory", MessageUtil.getMessage("document_send_for_information_users"));
            return false;
        }
        if (StringUtils.isBlank(subject)) {
            MessageUtil.addErrorMessage("common_propertysheet_validator_mandatory", MessageUtil.getMessage("document_send_for_information_subject"));
            return false;
        }
        if (StringUtils.isBlank(content)) {
            MessageUtil.addErrorMessage("common_propertysheet_validator_mandatory", MessageUtil.getMessage("document_send_content"));
            return false;
        }
        return true;
    }

    public void loadDocument(ActionEvent event) {
        reset();
        try {
            initNodeValue(event);
            String header = (String) contentNode.getProperties().get(DocumentCommonModel.Props.DOC_NAME);
            if (StringUtils.isNotBlank(header)) {
            	subject = header;
            }
        } catch (InvalidNodeRefException e) {
            MessageUtil.addErrorMessage("document_sendOut_error_docDeleted");
            return;
        }
        List<UnmodifiableDocumentTemplate> templates = BeanHelper.getDocumentTemplateService().getEmailTemplates();
        emailTemplates = new ArrayList<>(templates.size() + 1);
        emailTemplates.add(new SelectItem("", MessageUtil.getMessage("select_default_label")));
        for (UnmodifiableDocumentTemplate template : templates) {
            emailTemplates.add(new SelectItem(template.getName(), FilenameUtils.getBaseName(template.getName()), template.getNodeRef().toString()));
        }
    }

    protected void initNodeValue(ActionEvent event) {
        contentNode = getDocumentService().getDocument(new NodeRef(ActionUtil.getParam(event, "documentNodeRef")));
    }

    protected void reset() {
        authorities = null;
        selectedEmailTemplate = null;
        emailTemplates = null;
        subject = null;
        content = null;
    }

    @Override
    public void clean() {
        reset();
    }

    public void updateTemplate(ActionEvent event) {
        NodeRef selectedTemplate = getSelectedTemplateNodeRef();
        if (StringUtils.isNotBlank(selectedEmailTemplate) && selectedTemplate != null) {
            Map<String, NodeRef> nodeRefs = new LinkedHashMap<String, NodeRef>();
            nodeRefs.put(getKeyPrefix(), contentNode.getNodeRef());
            ProcessedEmailTemplate template = BeanHelper.getDocumentTemplateService().getProcessedEmailTemplate(nodeRefs, selectedTemplate);
            content = template.getContent();
            String templateSubject = template.getSubject();
            if (StringUtils.isBlank(subject) && StringUtils.isNotBlank(templateSubject)) {
                subject = templateSubject;
            }
        }
    }

    protected String getKeyPrefix() {
        return null;
    }

    private NodeRef getSelectedTemplateNodeRef() {
        for (SelectItem template : emailTemplates) {
            if (StringUtils.isNotBlank(selectedEmailTemplate) && template.getValue().equals(selectedEmailTemplate)) {
                return new NodeRef(template.getDescription());
            }
        }
        return null;
    }

    public String getBlockTitle() {
        return MessageUtil.getMessage("document_send_for_information");
    }

    @Override
    public String getFinishButtonLabel() {
        return MessageUtil.getMessage("document_send_for_information_action");
    }

    protected String getUrl() {
        return BeanHelper.getDocumentTemplateService().getDocumentUrl(contentNode.getNodeRef());
    }

    protected void setDefaultMessage() {
        String type = contentNode.getType().getLocalName() + "_send_for_information_type";
        content = MessageUtil.getMessage("document_send_for_information_default_input", MessageUtil.getMessage(type), getUrl());
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public List<String> getAuthorities() {
        return authorities;
    }

    public void setAuthorities(List<String> authorities) {
        this.authorities = authorities;
    }

    public String getSelectedEmailTemplate() {
        return selectedEmailTemplate;
    }

    public void setSelectedEmailTemplate(String selectedEmailTemplate) {
        this.selectedEmailTemplate = selectedEmailTemplate;
    }

    public List<SelectItem> getEmailTemplates() {
        return emailTemplates;
    }

    protected String getLogMessage() {
        return "document_send_for_information_log_entry";
    }

    protected LogObject getLogObject() {
        return LogObject.DOCUMENT;
    }

}
