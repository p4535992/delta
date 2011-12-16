package ee.webmedia.alfresco.document.sendout.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getClassificatorService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentLogService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentTemplateService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getFileService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getGeneralService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getParametersService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getSendOutService;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.lock.NodeLockedException;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.ui.common.converter.ByteSizeConverter;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.EmailValidator;

import ee.webmedia.alfresco.addressbook.util.AddressbookUtil;
import ee.webmedia.alfresco.classificator.enums.SendMode;
import ee.webmedia.alfresco.classificator.enums.StorageType;
import ee.webmedia.alfresco.classificator.model.Classificator;
import ee.webmedia.alfresco.classificator.model.ClassificatorValue;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docdynamic.web.DocumentLockHelperBean;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.web.OutboxDocumentMenuItemProcessor;
import ee.webmedia.alfresco.document.web.UnsentDocumentMenuItemProcessor;
import ee.webmedia.alfresco.menu.ui.MenuBean;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.template.model.DocumentTemplate;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.utils.UserUtil;

/**
 * Bean for sending out document dialog.
 * 
 * @author Erko Hansar
 */
public class DocumentSendOutDialog extends BaseDialogBean {

    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "DocumentSendOutDialog";

    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DocumentSendOutDialog.class);

    private static final String[] PROP_KEYS = { "recipientName", "recipientEmail", "recipientSendMode" };

    private SendOutModel model;
    private List<SelectItem> sendModes;
    private List<SelectItem> emailTemplates;

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        if (validate(context)) {
            boolean success = false;
            try {
                success = sendOut(context);
            } catch (NodeLockedException e) {
                MessageUtil.addErrorMessage(context, "document_sendOut_error_docLocked");
            }
            if (success) {
                BeanHelper.getDocumentLockHelperBean().lockOrUnlockIfNeeded(false);
                resetState();
                MessageUtil.addInfoMessage("document_sendOut_success");
                return outcome;
            }
        }
        super.isFinished = false;
        return null;
    }

    @Override
    public String cancel() {
        // Unlock when finished
        BeanHelper.getDocumentLockHelperBean().lockOrUnlockIfNeeded(false);
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

    public String init() {
        FacesContext context = FacesContext.getCurrentInstance();
        Node docNode = BeanHelper.getDocumentDialogHelperBean().getNode();
        if (!getNodeService().exists(docNode.getNodeRef())) {
            return AlfrescoNavigationHandler.CLOSE_DIALOG_OUTCOME;
        }

        try {
            // Lock the node
            DocumentLockHelperBean documentLockHelperBean = BeanHelper.getDocumentLockHelperBean();
            documentLockHelperBean.lockOrUnlockIfNeeded(documentLockHelperBean.isLockingAllowed());
            BaseDialogBean.validatePermission(docNode, DocumentCommonModel.Privileges.EDIT_DOCUMENT_META_DATA);
        } catch (NodeLockedException e) {
            BeanHelper.getDocumentLockHelperBean().handleLockedNode("document_validation_alreadyLocked");
            return null;
        } catch (UnableToPerformException e) {
            MessageUtil.addStatusMessage(context, e);
            return null;
        }
        return "dialog:documentSendOutDialog";
    }

    @SuppressWarnings("unchecked")
    public void loadDocument(ActionEvent event) {
        resetState();
        final FacesContext context = FacesContext.getCurrentInstance();
        Node node = null;
        try {
            node = getDocumentService().getDocument(new NodeRef(ActionUtil.getParam(event, "documentNodeRef")));
        } catch (InvalidNodeRefException e) {
            MessageUtil.addErrorMessage(context, "document_sendOut_error_docDeleted");
            return;
        }
        model = new SendOutModel();
        model.setNodeRef(node.getNodeRef());
        boolean ddocExists = false;
        List<String> selectedFiles = new ArrayList<String>();
        SortedMap<String, String> files = new TreeMap<String, String>();
        final List<File> allFiles = getFileService().getAllActiveFiles(node.getNodeRef());
        ByteSizeConverter converter = (ByteSizeConverter) context.getApplication().createConverter(ByteSizeConverter.CONVERTER_ID);
        for (File file : allFiles) {
            final String fileRef = file.getNodeRef().toString();

            String fileName = file.getName();
            fileName += " (" + converter.getAsString(context, null, file.getSize()) + ")";

            files.put(fileName, fileRef);
            boolean isDdoc = file.isDigiDocContainer();
            if (isDdoc) {
                if (!ddocExists) {
                    selectedFiles = new ArrayList<String>();
                }
                selectedFiles.add(fileRef);
            } else if (!ddocExists) {
                selectedFiles.add(fileRef);
            }
            ddocExists |= isDdoc;
        }
        model.setFiles(files);
        model.setSelectedFiles(selectedFiles);
        Map<String, Object> props = node.getProperties();
        model.setDocName((String) props.get(DocumentCommonModel.Props.DOC_NAME));
        model.setSendDesc((String) props.get(DocumentCommonModel.Props.SEND_DESC_VALUE));
        model.setSenderEmail(getParametersService().getStringParameter(Parameters.DOC_SENDER_EMAIL));
        model.setSendoutInfo(getParametersService().getStringParameter(Parameters.DOC_SENDOUT_INFO));
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
        List<String> partyNames = (List<String>) props.get(DocumentSpecificModel.Props.PARTY_NAME);
        List<String> partyEmails = (List<String>) props.get(DocumentSpecificModel.Props.PARTY_EMAIL);
        if (partyNames != null && partyEmails != null) {
            RepoUtil.validateSameSize(partyNames, partyEmails, "partyNames", "partyEmails");
            String name, email;
            for (int i = 0; i < partyNames.size(); i++) {
                name = partyNames.get(i);
                email = partyEmails.get(i);
                if (StringUtils.isBlank(name) && StringUtils.isBlank(email)) {
                    continue;
                }

                names.add(name);
                emails.add(email);
            }
        }

        if (names.size() == 1 && emails.size() == 1 && StringUtils.isBlank(names.get(0)) && StringUtils.isBlank(emails.get(0))) {
            names = new ArrayList<String>();
            emails = new ArrayList<String>();
        }

        addAdditionalRecipients(props, names, emails);
        List<String> recSendModes = new ArrayList<String>(names.size());
        for (int i = 0; i < names.size(); i++) {
            if (StringUtils.isNotBlank(names.get(i)) || StringUtils.isNotBlank(emails.get(i))) {
                recSendModes.add(defaultSendMode);
            } else {
                recSendModes.add("");
            }
        }

        properties.put(PROP_KEYS[0], names);
        properties.put(PROP_KEYS[1], emails);
        properties.put(PROP_KEYS[2], recSendModes);

        model.setProperties(properties);
    }

    private void addAdditionalRecipients(Map<String, Object> props, List<String> names, List<String> emails) {
        @SuppressWarnings("unchecked")
        List<String> namesAdd = newListIfNull((List<String>) props.get(DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_NAME), false);
        @SuppressWarnings("unchecked")
        List<String> emailsAdd = newListIfNull((List<String>) props.get(DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_EMAIL), false);
        RepoUtil.validateSameSize(namesAdd, emailsAdd, "additionalNames", "additionalEmails");

        List<Integer> removeIndexes = new ArrayList<Integer>();
        int j = 0;
        for (Iterator<String> it = emailsAdd.iterator(); it.hasNext();) {
            String email = it.next();
            if (StringUtils.isBlank(email) && StringUtils.isBlank(namesAdd.get(j))) {
                it.remove();
                removeIndexes.add(j);
            }
            j++;
        }
        Collections.reverse(removeIndexes);
        for (Integer index : removeIndexes) {
            namesAdd.remove((int) index);
        }

        names.addAll(namesAdd);
        emails.addAll(emailsAdd);

        if (names.isEmpty()) {
            names.add("");
        }
        if (emails.isEmpty()) {
            emails.add("");
        }
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

    public void updateSendModes(@SuppressWarnings("unused") ActionEvent event) {
        List<String> recSendModes = model.getProperties().get(PROP_KEYS[2]);
        for (int i = 0; i < recSendModes.size(); i++) {
            recSendModes.set(i, model.getSendMode());
        }
    }

    public void updateTemplate(@SuppressWarnings("unused") ActionEvent event) {
        if (StringUtils.isNotBlank(model.getTemplate())) {
            LinkedHashMap<String, NodeRef> nodeRefs = new LinkedHashMap<String, NodeRef>();
            nodeRefs.put(null, model.getNodeRef());
            String templateTxt = getDocumentTemplateService().getProcessedEmailTemplate(nodeRefs, new NodeRef(model.getTemplate()));
            model.setContent(templateTxt);
        }
    }

    public List<String> fetchContactData(String nodeRef) {
        List<String> result;
        QName type = getNodeService().getType(new NodeRef(nodeRef));
        if (type.equals(ContentModel.TYPE_PERSON)) {
            result = getPersonData(nodeRef);
        } else {
            result = AddressbookUtil.getContactData(nodeRef);
        }
        result.add(model.getDefaultSendMode());
        return result;
    }

    private List<String> getPersonData(String nodeRef) {
        Map<QName, Serializable> personProps = getNodeService().getProperties(new NodeRef(nodeRef));
        String name = UserUtil.getPersonFullName1(personProps);
        String email = (String) personProps.get(ContentModel.PROP_EMAIL);
        List<String> data = new ArrayList<String>();
        data.add(name);
        data.add(email);
        return data;
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

        List<String> names = model.getProperties().get(PROP_KEYS[0]);
        List<String> emails = model.getProperties().get(PROP_KEYS[1]);
        List<String> modes = model.getProperties().get(PROP_KEYS[2]);
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            String email = StringUtils.trim(emails.get(i));
            String mode = modes.get(i);
            if (!hasValidRecipient && StringUtils.isNotBlank(name) && StringUtils.isNotBlank(mode) && (StringUtils.isNotBlank(email)
                    || (!SendMode.EMAIL.equals(mode) && !SendMode.EMAIL_DVK.equals(mode) && !SendMode.EMAIL_BCC.equals(mode)))) {
                hasValidRecipient = true;
            } else if (!hasMissingEmails && StringUtils.isNotBlank(mode) && (SendMode.EMAIL.equals(mode) || SendMode.EMAIL_DVK.equals(mode) || SendMode.EMAIL_BCC.equals(mode))
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
            if (!hasEmailMode && (SendMode.EMAIL.equals(mode) || SendMode.EMAIL_DVK.equals(mode) || SendMode.EMAIL_BCC.equals(mode))) {
                hasEmailMode = true;
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

        final String senderEmail = StringUtils.trim(model.getSenderEmail());
        if (StringUtils.isBlank(senderEmail) || !emailValidator.isValid(senderEmail)) {
            valid = false;
            MessageUtil.addErrorMessage(context, "document_docSenderEmail_problem");
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
                    MessageUtil.addErrorMessage(context, "document_send_out_size", maxSizeMB);
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

        names.addAll(model.getProperties().get(PROP_KEYS[0]));
        emails.addAll(model.getProperties().get(PROP_KEYS[1]));
        modes.addAll(model.getProperties().get(PROP_KEYS[2]));

        try {
            result = getSendOutService().sendOut(model.getNodeRef(), names, emails, modes, model.getSenderEmail(), model.getSubject(), model.getContent(),
                    model.getSelectedFiles(), model.isZip());
        } catch (UnableToPerformException e) {
            MessageUtil.addStatusMessage(context, e);
            return false;
        } catch (Exception e) {
            log.error("Sending out document failed\n  nodeRef=" + model.getNodeRef() + "\n  names=" + names + "\n  emails=" + emails + "\n  modes=" + modes
                    + "\n  senderEmail=" + model.getSenderEmail() + "\n  subject=" + model.getSubject() + "\n  content="
                    + (model.getContent() == null ? "null" : "String[" + model.getContent().length() + "]") + "\n  selectedFiles=" + model.getSelectedFiles()
                    + "\n  zip=" + model.isZip(), e);
            result = false;
        }
        if (!result) {
            MessageUtil.addErrorMessage(context, "document_send_failed");
            getDocumentLogService().addDocumentLog(model.getNodeRef(), MessageUtil.getMessage("document_log_status_sending_failed"));
        } else {
            getDocumentLogService().addDocumentLog(model.getNodeRef(), MessageUtil.getMessage("document_log_status_sent"));
            ((MenuBean) FacesHelper.getManagedBean(context, MenuBean.BEAN_NAME)).processTaskItem(
                    OutboxDocumentMenuItemProcessor.OUTBOX_DOCUMENT,
                    UnsentDocumentMenuItemProcessor.UNSENT_DOCUMENT);
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

    // END: getters / setters

    public static class SendOutModel implements Serializable {

        private static final long serialVersionUID = 1L;

        public SendOutModel() {
            // default constructor
        }

        private NodeRef nodeRef;
        private SortedMap<String, String> files;
        private List<String> selectedFiles;
        private String docName;
        private String sendDesc;
        private String sendMode;
        private boolean zip;
        private String senderEmail;
        private String sendoutInfo;
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

        public void setFiles(SortedMap<String /* fileName */, String /* noderef */> files) {
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

        public String getSendoutInfo() {
            return sendoutInfo;
        }

        public void setSendoutInfo(String sendoutInfo) {
            this.sendoutInfo = sendoutInfo;
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
