package ee.webmedia.alfresco.document.sendout.web;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;

import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.EmailValidator;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.addressbook.web.dialog.AddressbookMainViewDialog;
import ee.webmedia.alfresco.classificator.enums.SendMode;
import ee.webmedia.alfresco.classificator.enums.StorageType;
import ee.webmedia.alfresco.classificator.model.Classificator;
import ee.webmedia.alfresco.classificator.model.ClassificatorValue;
import ee.webmedia.alfresco.classificator.service.ClassificatorService;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.sendout.service.SendOutService;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.service.ParametersService;
import ee.webmedia.alfresco.signature.service.SignatureService;
import ee.webmedia.alfresco.template.model.DocumentTemplate;
import ee.webmedia.alfresco.template.service.DocumentTemplateService;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Bean for sending out document dialog.
 * 
 * @author Erko Hansar
 */
public class DocumentSendOutDialog extends BaseDialogBean {

    private static final long serialVersionUID = 1L;

    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DocumentSendOutDialog.class);

    private static final String[][] PROP_KEYS = { { "recipientName", "recipientEmail", "recipientSendMode" },
            { "additionalRecipientName", "additionalRecipientEmail", "additionalRecipientSendMode" } };

    private transient GeneralService generalService;
    private transient DocumentService documentService;
    private transient ClassificatorService classificatorService;
    private transient ParametersService parametersService;
    private transient DocumentTemplateService documentTemplateService;
    private transient SendOutService sendOutService;    
    private transient SignatureService signatureService;    
    private AddressbookMainViewDialog addressbookDialog;

    private SendOutModel model;
    private List<SelectItem> sendModes;
    private List<SelectItem> emailTemplates;

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        if (validate(context)) {
            boolean success = sendOut(context);
            if (success) {
                resetState();
                // Does not work as intended. Will display the normal error message "error_dialog" first and only then the info message.
                //MessageUtil.addStatusMessage(context, "Saatmine onnestus!", FacesMessage.SEVERITY_INFO);
                return outcome;
            }
        }
        super.isFinished = false;
        return null;
    }

    @Override
    public String cancel() {
        resetState();
        return super.cancel();
    }

    @Override
    public String getContainerTitle() {
        return MessageUtil.getMessage(FacesContext.getCurrentInstance(), "document_send_out_title");
    }

    @Override
    public String getFinishButtonLabel() {
        return MessageUtil.getMessage(FacesContext.getCurrentInstance(), "document_send_out");
    }

    @Override
    public boolean getFinishButtonDisabled() {
        return false;
    }

    @SuppressWarnings("unchecked")
    public void loadDocument(ActionEvent event) {
        resetState();
        Node node = getDocumentService().getDocument(new NodeRef(ActionUtil.getParam(event, "documentNodeRef")));
        model = new SendOutModel();
        model.setNodeRef(node.getNodeRef());
        boolean ddocExists = false;
        List<String> selectedFiles = new ArrayList<String>();
        SortedMap<String, String> files = new TreeMap<String, String>();
        for (FileInfo fileInfo : getFileFolderService().listFiles(node.getNodeRef())) {
            files.put(fileInfo.getName(), fileInfo.getNodeRef().toString());
            boolean isDdoc = getSignatureService().isDigiDocContainer(fileInfo);
            if (isDdoc) {
                if (!ddocExists) {
                    selectedFiles = new ArrayList<String>();
                }
                selectedFiles.add(fileInfo.getNodeRef().toString());
            } else if (!ddocExists) {
                selectedFiles.add(fileInfo.getNodeRef().toString());
            }
            ddocExists |= isDdoc;
        }
        model.setFiles(files);
        model.setSelectedFiles(selectedFiles);
        Map<String, Object> props = node.getProperties();
        model.setDocName((String) props.get(DocumentCommonModel.Props.DOC_NAME));
        model.setSendDesc((String) props.get(DocumentCommonModel.Props.SEND_DESC_VALUE));
        model.setSenderEmail(getParametersService().getStringParameter(Parameters.DOC_SENDER_EMAIL));
        model.setSubject(model.getDocName());
        Map<String, List<String>> properties = new HashMap<String, List<String>>();

        String defaultSendMode = "";
        String storageType = (String) props.get(DocumentCommonModel.Props.STORAGE_TYPE);
        if (StringUtils.isNotBlank(storageType)) {
            if (StorageType.DIGITAL.equals(storageType)) {
                defaultSendMode = SendMode.EMAIL_DVK.getValueName();
            } else if (StorageType.PAPER.equals(storageType) || "Paber".equalsIgnoreCase(storageType)) {
                defaultSendMode = SendMode.MAIL.getValueName();
            }
        }
        model.setDefaultSendMode(defaultSendMode);
        
        List<String> names = newListIfNull((List<String>) props.get(DocumentCommonModel.Props.RECIPIENT_NAME), false);
        List<String> emails = newListIfNull((List<String>) props.get(DocumentCommonModel.Props.RECIPIENT_EMAIL), false);
        String contractName = (String) props.get(DocumentSpecificModel.Props.SECOND_PARTY_NAME);
        String contractEmail = (String) props.get(DocumentSpecificModel.Props.SECOND_PARTY_EMAIL);
        if (StringUtils.isNotBlank(contractName) || StringUtils.isNotBlank(contractEmail)) {
            names.add(StringUtils.isNotBlank(contractName) ? contractName : "");
            emails.add(StringUtils.isNotBlank(contractEmail) ? contractEmail : "");
        }
        contractName = (String) props.get(DocumentSpecificModel.Props.THIRD_PARTY_NAME);
        contractEmail = (String) props.get(DocumentSpecificModel.Props.THIRD_PARTY_EMAIL);
        if (StringUtils.isNotBlank(contractName) || StringUtils.isNotBlank(contractEmail)) {
            names.add(StringUtils.isNotBlank(contractName) ? contractName : "");
            emails.add(StringUtils.isNotBlank(contractEmail) ? contractEmail : "");
        }
        if (names.isEmpty()) {
            names.add("");
        }
        if (emails.isEmpty()) {
            emails.add("");
        }
        List<String> recSendModes = new ArrayList<String>(names.size());
        for (int i = 0; i < names.size(); i++) {
            if (StringUtils.isNotBlank(names.get(i)) || StringUtils.isNotBlank(emails.get(i))) {
                recSendModes.add(defaultSendMode);
            } else {
                recSendModes.add("");
            }
        }
        properties.put(PROP_KEYS[0][0], names);
        properties.put(PROP_KEYS[0][1], emails);
        properties.put(PROP_KEYS[0][2], recSendModes);

        names = newListIfNull((List<String>) props.get(DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_NAME), true);
        emails = newListIfNull((List<String>) props.get(DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_EMAIL), true);
        recSendModes = new ArrayList<String>(names.size());
        for (int i = 0; i < names.size(); i++) {
            if (StringUtils.isNotBlank(names.get(i)) || StringUtils.isNotBlank(emails.get(i))) {
                recSendModes.add(defaultSendMode);
            } else {
                recSendModes.add("");
            }
        }
        properties.put(PROP_KEYS[1][0], names);
        properties.put(PROP_KEYS[1][1], emails);
        properties.put(PROP_KEYS[1][2], recSendModes);

        model.setProperties(properties);
    }

    public String getPanelTitle() {
        return model.getDocName();
    }

    public synchronized List<SelectItem> getSendModes() {
        if (sendModes == null) {
            sendModes = new ArrayList<SelectItem>();
            sendModes.add(new SelectItem("", MessageUtil.getMessage(FacesContext.getCurrentInstance(), "document_choose")));
            Classificator classificator = getClassificatorService().getClassificatorByName("sendMode");
            List<ClassificatorValue> values = getClassificatorService().getActiveClassificatorValues(classificator);
            Collections.sort(values);
            for (ClassificatorValue val : values) {
                sendModes.add(new SelectItem(val.getValueName(), val.getValueName()));
            }
        }
        return sendModes;
    }

    public synchronized List<SelectItem> getEmailTemplates() {
        if (emailTemplates == null) {
            emailTemplates = new ArrayList<SelectItem>();
            emailTemplates.add(new SelectItem("", MessageUtil.getMessage(FacesContext.getCurrentInstance(), "document_choose")));
            for (DocumentTemplate template : getDocumentTemplateService().getEmailTemplates()) {
                String templateName = FilenameUtils.getBaseName(template.getName());
                emailTemplates.add(new SelectItem(template.getNodeRef().toString(), templateName));
            }
        }
        return emailTemplates;
    }
    
    public List<String> fetchContactData(String nodeRef) {
        List<String> result = addressbookDialog.getContactData(nodeRef);
        result.add(model.getDefaultSendMode());
        return result;
    }

    public void updateSendModes(ActionEvent event) {
        List<String> recSendModes = model.getProperties().get(PROP_KEYS[0][2]);
        for (int i = 0; i < recSendModes.size(); i++) {
            recSendModes.set(i, model.getSendMode());
        }
        List<String> additionalSendModes = model.getProperties().get(PROP_KEYS[1][2]);
        for (int i = 0; i < additionalSendModes.size(); i++) {
            additionalSendModes.set(i, model.getSendMode());
        }
    }

    public void updateTemplate(ActionEvent event) {
        if (StringUtils.isNotBlank(model.getTemplate())) {
            LinkedHashMap<String, NodeRef> nodeRefs = new LinkedHashMap<String, NodeRef>();
            nodeRefs.put("default", model.getNodeRef());
            String templateTxt = getDocumentTemplateService().getProcessedEmailTemplate(nodeRefs, new NodeRef(model.getTemplate()));
            model.setContent(templateTxt);
        }
    }

    // /// PRIVATE METHODS /////

    private void resetState() {
        model = null;
        sendModes = null;
        emailTemplates = null;
    }

    private boolean validate(FacesContext context) {
        boolean valid = true;
        EmailValidator emailValidator = EmailValidator.getInstance();

        boolean hasValidRecipient = false;
        boolean hasInvalidRecipient = false;
        boolean hasMissingEmails = false;
        boolean hasEmailMode = false;
        for (int round = 0; round < 2; round++) {
            List<String> names = model.getProperties().get(PROP_KEYS[round][0]);
            List<String> emails = model.getProperties().get(PROP_KEYS[round][1]);
            List<String> modes = model.getProperties().get(PROP_KEYS[round][2]);
            for (int i = 0; i < names.size(); i++) {
                String name = names.get(i);
                String email = emails.get(i);
                String mode = modes.get(i);
                if (round == 0 && !hasValidRecipient && StringUtils.isNotBlank(name) && StringUtils.isNotBlank(mode)
                        && (StringUtils.isNotBlank(email) || (!SendMode.EMAIL.equals(mode) && !SendMode.EMAIL_DVK.equals(mode)))) {
                    hasValidRecipient = true;
                } else if (!hasMissingEmails && StringUtils.isNotBlank(mode) && (SendMode.EMAIL.equals(mode) || SendMode.EMAIL_DVK.equals(mode))
                        && StringUtils.isBlank(email)) {
                    hasMissingEmails = true;
                } else if (!hasInvalidRecipient && (StringUtils.isNotBlank(name) || StringUtils.isNotBlank(mode) || StringUtils.isNotBlank(email))
                        && (StringUtils.isBlank(name) || StringUtils.isBlank(mode))) {
                    hasInvalidRecipient = true;
                }
                if (StringUtils.isNotBlank(email)) {
                    if (!emailValidator.isValid(email)) {
                        valid = false;
                        MessageUtil.addErrorMessage(context, "email_format_is_not_valid");
                    }
                }
                if (!hasEmailMode && (SendMode.EMAIL.equals(mode) || SendMode.EMAIL_DVK.equals(mode))) {
                    hasEmailMode = true;
                }
            }
        }

        if (!hasValidRecipient) {
            valid = false;
            MessageUtil.addErrorMessage(context, "common_propertysheet_validator_mandatory", MessageUtil.getMessage(context, "document_recipients"));
        }
        if (hasMissingEmails) {
            valid = false;
            MessageUtil.addErrorMessage(context, "document_send_out_email_req");
        }
        if (hasInvalidRecipient) {
            valid = false;
            MessageUtil.addErrorMessage(context, "document_send_out_fields_req");
        }

        if (StringUtils.isBlank(model.getSenderEmail())) {
            valid = false;
            MessageUtil.addErrorMessage(context, "common_propertysheet_validator_mandatory", MessageUtil.getMessage(context, "document_senderEmail"));
        } else if (!emailValidator.isValid(model.getSenderEmail())) {
            valid = false;
            MessageUtil.addErrorMessage(context, "email_format_is_not_valid");
        }

        if (StringUtils.isBlank(model.getContent())) {
            valid = false;
            MessageUtil.addErrorMessage(context, "common_propertysheet_validator_mandatory", MessageUtil.getMessage(context, "document_send_content"));
        }

        if (valid && hasEmailMode && model.getSelectedFiles().size() > 0) {
            Long maxSizeMB = getParametersService().getLongParameter(Parameters.MAX_ATTACHED_FILE_SIZE);
            if (maxSizeMB != null && maxSizeMB.longValue() > 0) {
                // Check total files size
                long maxSizeBytes = maxSizeMB.longValue() * 1024 * 1024;
                long totalSizeBytes = 0;

                if (model.isZip()) {
                    ByteArrayOutputStream byteStream = getGeneralService().getZipFileFromFiles(model.getNodeRef(), model.getSelectedFiles());
                    totalSizeBytes = byteStream.size();
                    byteStream.reset();

                } else {
                    for (FileInfo fileInfo : getFileFolderService().listFiles(model.getNodeRef())) {
                        if (model.getSelectedFiles().contains(fileInfo.getNodeRef().toString())) {
                            log.debug("Validate - file size: " + fileInfo.getContentData().getSize());
                            totalSizeBytes += fileInfo.getContentData().getSize();
                        }
                    }
                }
                log.debug("Validate - total file size: " + totalSizeBytes + ", maxSize: " + maxSizeBytes);

                if (totalSizeBytes > maxSizeBytes) {
                    valid = false;
                    MessageUtil.addErrorMessage(context, "document_send_out_size");
                }
            }
        }

        return valid;
    }

    private boolean sendOut(FacesContext context) {
        boolean result = true;
        
        List<String> names = new ArrayList<String>();
        List<String> emails = new ArrayList<String>();
        List<String> modes = new ArrayList<String>();
        for (int round = 0; round < 2; round++) {
            names.addAll(model.getProperties().get(PROP_KEYS[round][0]));
            emails.addAll(model.getProperties().get(PROP_KEYS[round][1]));
            modes.addAll(model.getProperties().get(PROP_KEYS[round][2]));
        }

        result = getSendOutService().sendOut(model.getNodeRef(), names, emails, modes, model.getSenderEmail(), model.getSubject(), model.getContent(), model.getSelectedFiles(), model.isZip());
        getDocumentService().getDocumentLogService().addDocumentLog(model.getNodeRef(), MessageUtil.getMessage("document_log_status_sent"));
        if (!result) {
            MessageUtil.addErrorMessage(context, "document_send_failed");
        }
        
        return result;
    }

    public static List<String> newListIfNull(List<String> list, boolean checkEmpty) {
        List<String> result = list;
        if (result == null) {
            result = new ArrayList<String>();
        }
        if (result.isEmpty() && checkEmpty) {
            result.add("");
        }
        return result;
    }

    // START: getters / setters

    public SendOutModel getModel() {
        return model;
    }

    public void setDocumentService(DocumentService documentService) {
        this.documentService = documentService;
    }

    public DocumentService getDocumentService() {
        if (documentService == null) {
            documentService = (DocumentService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())//
                    .getBean(DocumentService.BEAN_NAME);
        }
        return documentService;
    }

    public void setDocumentTemplateService(DocumentTemplateService documentTemplateService) {
        this.documentTemplateService = documentTemplateService;
    }

    public DocumentTemplateService getDocumentTemplateService() {
        if (documentTemplateService == null) {
            documentTemplateService = (DocumentTemplateService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())//
                    .getBean(DocumentTemplateService.BEAN_NAME);
        }
        return documentTemplateService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public GeneralService getGeneralService() {
        if (generalService == null) {
            generalService = (GeneralService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())//
                    .getBean(GeneralService.BEAN_NAME);
        }
        return generalService;
    }

    public void setClassificatorService(ClassificatorService classificatorService) {
        this.classificatorService = classificatorService;
    }

    public ClassificatorService getClassificatorService() {
        if (classificatorService == null) {
            classificatorService = (ClassificatorService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())//
                    .getBean(ClassificatorService.BEAN_NAME);
        }
        return classificatorService;
    }

    public void setParametersService(ParametersService parametersService) {
        this.parametersService = parametersService;
    }

    public ParametersService getParametersService() {
        if (parametersService == null) {
            parametersService = (ParametersService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())//
                    .getBean(ParametersService.BEAN_NAME);
        }
        return parametersService;
    }

    public void setAddressbookDialog(AddressbookMainViewDialog addressbookDialog) {
        this.addressbookDialog = addressbookDialog;
    }

    public SendOutService getSendOutService() {
        if (sendOutService == null) {
            sendOutService = (SendOutService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())//
                    .getBean(SendOutService.BEAN_NAME);
        }
        return sendOutService;
    }

    public SignatureService getSignatureService() {
        if (signatureService == null) {
            signatureService = (SignatureService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())//
                    .getBean(SignatureService.BEAN_NAME);
        }
        return signatureService;
    }

    // END: getters / setters

    public static class SendOutModel implements Serializable {

        private static final long serialVersionUID = 1L;

        public SendOutModel() {
        }

        private NodeRef nodeRef;
        private SortedMap<String, String> files;
        private List<String> selectedFiles;
        private String docName;
        private String sendDesc;
        private String sendMode;
        private boolean zip;
        private String senderEmail;
        private String subject;
        private String content;
        private String template;
        private Map<String, List<String>> properties;
        private String defaultSendMode;

        public NodeRef getNodeRef() {
            return nodeRef;
        }

        public void setNodeRef(NodeRef nodeRef) {
            this.nodeRef = nodeRef;
        }

        public String getDocName() {
            return docName;
        }

        public void setDocName(String docName) {
            this.docName = docName;
        }

        public String getSendDesc() {
            return sendDesc;
        }

        public void setSendDesc(String sendDesc) {
            this.sendDesc = sendDesc;
        }

        public SortedMap<String, String> getFiles() {
            return files;
        }

        public void setFiles(SortedMap<String, String> files) {
            this.files = files;
        }

        public List<String> getSelectedFiles() {
            return selectedFiles;
        }

        public void setSelectedFiles(List<String> selectedFiles) {
            this.selectedFiles = selectedFiles;
        }

        public boolean isZip() {
            return zip;
        }

        public void setZip(boolean zip) {
            this.zip = zip;
        }

        public String getSenderEmail() {
            return senderEmail;
        }

        public void setSenderEmail(String senderEmail) {
            this.senderEmail = senderEmail;
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getTemplate() {
            return template;
        }

        public void setTemplate(String template) {
            this.template = template;
        }

        public String getSendMode() {
            return sendMode;
        }

        public void setSendMode(String sendMode) {
            this.sendMode = sendMode;
        }

        public Map<String, List<String>> getProperties() {
            return properties;
        }

        public void setProperties(Map<String, List<String>> properties) {
            this.properties = properties;
        }

        public String getDefaultSendMode() {
            return defaultSendMode;
        }

        public void setDefaultSendMode(String defaultSendMode) {
            this.defaultSendMode = defaultSendMode;
        }

    }

}
