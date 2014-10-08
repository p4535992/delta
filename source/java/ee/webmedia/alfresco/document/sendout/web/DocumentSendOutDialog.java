package ee.webmedia.alfresco.document.sendout.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getAddressbookService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getClassificatorService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentLogService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentTemplateService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getEmailService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getFileService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getParametersService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getSendOutService;
import static ee.webmedia.alfresco.utils.ComponentUtil.addFacet;
import static ee.webmedia.alfresco.utils.ComponentUtil.getAttributes;
import static ee.webmedia.alfresco.utils.ComponentUtil.getChildren;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.faces.application.Application;
import javax.faces.component.UIColumn;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.UIMessages;
import javax.faces.component.UIOutput;
import javax.faces.component.UIPanel;
import javax.faces.component.html.HtmlDataTable;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.lock.NodeLockedException;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.ui.common.converter.ByteSizeConverter;
import org.alfresco.web.ui.repo.component.UIActions;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.EmailValidator;
import org.apache.myfaces.shared_impl.renderkit.JSFAttr;
import org.apache.myfaces.shared_impl.renderkit.html.HTML;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.addressbook.util.AddressbookUtil;
import ee.webmedia.alfresco.classificator.enums.AccessRestriction;
import ee.webmedia.alfresco.classificator.enums.SendMode;
import ee.webmedia.alfresco.classificator.enums.StorageType;
import ee.webmedia.alfresco.classificator.model.Classificator;
import ee.webmedia.alfresco.classificator.model.ClassificatorValue;
import ee.webmedia.alfresco.common.propertysheet.modalLayer.ModalLayerComponent;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docdynamic.model.DocumentChildModel;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.docdynamic.web.DocumentLockHelperBean;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.web.OutboxDocumentMenuItemProcessor;
import ee.webmedia.alfresco.document.web.UnsentDocumentMenuItemProcessor;
import ee.webmedia.alfresco.menu.ui.MenuBean;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.privilege.model.Privilege;
import ee.webmedia.alfresco.template.model.DocumentTemplate;
import ee.webmedia.alfresco.template.model.ProcessedEmailTemplate;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.TextUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.utils.UserUtil;
import ee.webmedia.alfresco.utils.WebUtil;
import ee.webmedia.xtee.client.exception.XTeeServiceConsumptionException;

/**
 * Bean for sending out document dialog.
 */
public class DocumentSendOutDialog extends BaseDialogBean {

    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "DocumentSendOutDialog";

    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DocumentSendOutDialog.class);

    private static final String[] PROP_KEYS = { "recipientName", "recipientId", "recipientEmail", "recipientSendMode", "recipientGroup" };

    private transient UIPanel modalContainer;

    private SendOutModel model;
    private List<SelectItem> sendModes;
    private List<SelectItem> emailTemplates;
    private List<EncryptionRecipient> encryptionRecipients;
    private String modalId;

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        if (BeanHelper.getDocumentLockHelperBean().isLockReleased(model.nodeRef)) {
            MessageUtil.addErrorMessage("lock_send_out_administrator_released");
            WebUtil.navigateTo("dialog:close");
            return null;
        }

        if (validate(context)) {
            if (model.isEncrypt()) {
                showEncryptionRecipientsModal(context);
                return null;
            }
            if (sendOutAndFinish(context)) {
                return outcome;
            }
        }
        return null;
    }

    public void encryptAndFinish(@SuppressWarnings("unused") ActionEvent event) {
        try {
            if (sendOutAndFinish(FacesContext.getCurrentInstance())) {
                WebUtil.navigateWithCancel();
            }
        } catch (UnableToPerformException e) {
            handleException(e);
        }
    }

    private boolean sendOutAndFinish(FacesContext context) {
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
        }
        return success;
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
        docNode.clearPermissionsCache();
        if (!new SendOutActionEvaluator().evaluate(docNode)) {
            MessageUtil.addErrorMessage("document_send_out_error_noPermission");
            return null;
        }
        if (!getNodeService().exists(docNode.getNodeRef())) {
            return AlfrescoNavigationHandler.CLOSE_DIALOG_OUTCOME;
        }

        try {
            // Lock the node
            DocumentLockHelperBean documentLockHelperBean = BeanHelper.getDocumentLockHelperBean();
            documentLockHelperBean.lockOrUnlockIfNeeded(documentLockHelperBean.isLockingAllowed());
            BaseDialogBean.validatePermission(docNode, Privilege.EDIT_DOCUMENT);
        } catch (NodeLockedException e) {
            BeanHelper.getDocumentLockHelperBean().handleLockedNode("document_validation_alreadyLocked_sendOut");
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
        model.setDocAccessRestriction(AccessRestriction.valueNameOf((String) props.get(DocumentCommonModel.Props.ACCESS_RESTRICTION)));
        Map<String, List<String>> properties = new HashMap<String, List<String>>();

        String defaultSendMode = "";
        String sendModeByStorageType = "";
        String storageType = (String) props.get(DocumentCommonModel.Props.STORAGE_TYPE);
        if (StringUtils.isNotBlank(storageType)) {
            if (StorageType.DIGITAL.equals(storageType)) {
                defaultSendMode = sendModeByStorageType = SendMode.EMAIL_DVK.getValueName();
            } else if (StorageType.PAPER.equals(storageType) || "Paber".equalsIgnoreCase(storageType)) {
                defaultSendMode = sendModeByStorageType = SendMode.MAIL.getValueName();
            }
        }
        NodeRef initialDocRef = BeanHelper.getDocumentAssociationsService().getInitialDocumentRef(model.getNodeRef());
        if (initialDocRef != null
                && SendMode.STATE_PORTAL_EESTI_EE.getValueName().equals(getNodeService().getProperty(initialDocRef, DocumentSpecificModel.Props.TRANSMITTAL_MODE))) {
            defaultSendMode = SendMode.STATE_PORTAL_EESTI_EE.getValueName();
        }
        model.setDefaultSendMode(defaultSendMode);
        model.setSendModeByContentType(sendModeByStorageType);

        List<String> names = newListIfNull((List<String>) props.get(DocumentCommonModel.Props.RECIPIENT_NAME), false);
        List<String> idCodes = newListIfNull((List<String>) props.get(DocumentDynamicModel.Props.RECIPIENT_ID), false);
        List<String> emails = newListIfNull((List<String>) props.get(DocumentCommonModel.Props.RECIPIENT_EMAIL), false);
        List<String> groups = newListIfNull((List<String>) props.get(DocumentCommonModel.Props.RECIPIENT_GROUP), false);
        while (groups.size() < names.size()) {
            groups.add("");
        }
        List<Node> childNodes = node.getAllChildAssociations(DocumentChildModel.Assocs.CONTRACT_PARTY);
        if (childNodes != null) {
            for (Node childNode : childNodes) {
                String name = (String) childNode.getProperties().get(DocumentSpecificModel.Props.PARTY_NAME);
                String email = (String) childNode.getProperties().get(DocumentSpecificModel.Props.PARTY_EMAIL);
                if (StringUtils.isBlank(name) && StringUtils.isBlank(email)) {
                    continue;
                }

                names.add(name);
                idCodes.add("");
                emails.add(email);
                groups.add("");
            }
        }

        if (names.size() == 1 && emails.size() == 1 && StringUtils.isBlank(names.get(0)) && StringUtils.isBlank(emails.get(0))) {
            names = new ArrayList<String>();
            idCodes = new ArrayList<String>();
            emails = new ArrayList<String>();
            groups = new ArrayList<String>();
        }

        addAdditionalRecipients(props, names, idCodes, emails, groups);
        List<String> recSendModes = new ArrayList<String>(names.size());
        for (int i = 0; i < names.size(); i++) {
            if (StringUtils.isNotBlank(names.get(i)) || StringUtils.isNotBlank(emails.get(i))) {
                recSendModes.add(defaultSendMode);
            } else {
                recSendModes.add("");
            }
        }

        properties.put(PROP_KEYS[0], names);
        properties.put(PROP_KEYS[1], idCodes);
        properties.put(PROP_KEYS[2], emails);
        properties.put(PROP_KEYS[3], recSendModes);
        properties.put(PROP_KEYS[4], groups);

        model.setProperties(properties);
    }

    private void addAdditionalRecipients(Map<String, Object> props, List<String> names, List<String> idCodes, List<String> emails, List<String> groups) {
        @SuppressWarnings("unchecked")
        List<String> namesAdd = newListIfNull((List<String>) props.get(DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_NAME), false);
        @SuppressWarnings("unchecked")
        List<String> idCodesAdd = newListIfNull((List<String>) props.get(DocumentDynamicModel.Props.ADDITIONAL_RECIPIENT_ID), false);
        @SuppressWarnings("unchecked")
        List<String> emailsAdd = newListIfNull((List<String>) props.get(DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_EMAIL), false);
        @SuppressWarnings("unchecked")
        List<String> groupsAdd = newListIfNull((List<String>) props.get(DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_GROUP), false);
        RepoUtil.validateSameSize(namesAdd, emailsAdd, "additionalNames", "additionalEmails");
        while (groupsAdd.size() < namesAdd.size()) {
            groupsAdd.add("");
        }

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
            groupsAdd.remove((int) index);
        }

        names.addAll(namesAdd);
        idCodes.addAll(idCodesAdd);
        emails.addAll(emailsAdd);
        groups.addAll(groupsAdd);

        if (names.isEmpty()) {
            names.add("");
        }
        if (idCodes.isEmpty()) {
            idCodes.add("");
        }
        if (emails.isEmpty()) {
            emails.add("");
        }
        if (groups.isEmpty()) {
            groups.add("");
        }
    }

    public String getPanelTitle() {
        return model.getDocName();
    }

    public List<SelectItem> getSendModes() {
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

    public List<SelectItem> getEmailTemplates() {
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

    public List<EncryptionRecipient> getEncryptionRecipients() {
        return encryptionRecipients;
    }

    public void updateSendModes(@SuppressWarnings("unused") ActionEvent event) {
        List<String> recSendModes = model.getProperties().get(PROP_KEYS[3]);
        for (int i = 0; i < recSendModes.size(); i++) {
            recSendModes.set(i, model.getSendMode());
        }
    }

    public void updateTemplate(@SuppressWarnings("unused") ActionEvent event) {
        if (StringUtils.isNotBlank(model.getTemplate())) {
            LinkedHashMap<String, NodeRef> nodeRefs = new LinkedHashMap<String, NodeRef>();
            nodeRefs.put(null, model.getNodeRef());
            ProcessedEmailTemplate template = getDocumentTemplateService().getProcessedEmailTemplate(nodeRefs, new NodeRef(model.getTemplate()));
            model.setContent(template.getContent());
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
        result.add(model.getSendModeByContentType());
        return result;
    }

    private List<String> getPersonData(String nodeRef) {
        Map<QName, Serializable> personProps = getNodeService().getProperties(new NodeRef(nodeRef));
        String name = UserUtil.getPersonFullName1(personProps);
        String id = (String) personProps.get(ContentModel.PROP_USERNAME);
        String email = (String) personProps.get(ContentModel.PROP_EMAIL);
        List<String> data = new ArrayList<String>();
        data.add(name);
        data.add(id);
        data.add(email);
        return data;
    }

    // /// PRIVATE METHODS /////

    private void resetState() {
        model = null;
        sendModes = null;
        emailTemplates = null;
        modalId = null;
    }

    private boolean validate(FacesContext context) {
        boolean valid = true;
        EmailValidator emailValidator = EmailValidator.getInstance();

        boolean hasValidRecipient = false;
        boolean hasInvalidRecipient = false;
        boolean hasMissingIdCodes = false;
        boolean hasMissingEmails = false;
        boolean hasEmailMode = false;

        List<String> names = model.getProperties().get(PROP_KEYS[0]);
        List<String> idCodes = trimAndGetIdCodes();
        List<String> emails = model.getProperties().get(PROP_KEYS[2]);
        List<String> modes = model.getProperties().get(PROP_KEYS[3]);
        List<Pair<String, String>> dvkRecipients = new ArrayList<Pair<String, String>>();
        Set<String> idCodesToCheck = new HashSet<String>();
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            String idCode = idCodes.get(i);
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
            if (!hasMissingIdCodes && StringUtils.isNotBlank(mode) && SendMode.STATE_PORTAL_EESTI_EE.equals(mode)) {
                if (StringUtils.isBlank(idCode)) {
                    hasMissingIdCodes = true;
                } else {
                    idCodesToCheck.add(idCode);
                    dvkRecipients.add(new Pair<String, String>(name, mode));
                }
            }
            if (StringUtils.isNotBlank(mode) && SendMode.EMAIL_DVK.equals(mode)) {
                dvkRecipients.add(new Pair<String, String>(name, mode));
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
        if (hasMissingIdCodes) {
            valid = false;
            MessageUtil.addErrorMessage(context, "document_send_out_id_code_req");
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

        if (valid && modes.contains(SendMode.EMAIL_DVK.getValueName())) {
            AccessRestriction restriction = model.getDocAccessRestriction();
            if (!(AccessRestriction.OPEN.equals(restriction) || AccessRestriction.AK.equals(restriction))) {
                valid = false;
                MessageUtil.addErrorMessage(context, "document_send_out_forbidden_wrong_access_restriction_for_dvk");
            }
        }

        if (valid && hasEmailMode && model.getSelectedFiles().size() > 0) {
            Long maxSizeMB = getParametersService().getLongParameter(Parameters.MAX_ATTACHED_FILE_SIZE);
            if (maxSizeMB != null && maxSizeMB.longValue() > 0) {
                // Check total files size
                long maxSizeBytes = maxSizeMB.longValue() * 1024 * 1024;
                long totalSizeBytes = getEmailService().getAttachmentsTotalSize(getFileRefs(), model.isZip(), model.isEncrypt());
                log.debug("Validate - total file size: " + totalSizeBytes + ", maxSize: " + maxSizeBytes);
                if (totalSizeBytes > maxSizeBytes) {
                    valid = false;
                    MessageUtil.addErrorMessage(context, "document_send_out_size", maxSizeMB);
                }
            }
        }

        if (valid && model.isEncrypt()) {
            List<String> encryptionForbidden = new ArrayList<String>();
            List<String> dvkCapableOrgs = BeanHelper.getAddressbookService().getDvkCapableOrgNames();
            for (Pair<String, String> nameAndSendMode : dvkRecipients) {
                String name = nameAndSendMode.getFirst();
                String sendMode = nameAndSendMode.getSecond();
                if (SendMode.STATE_PORTAL_EESTI_EE.equals(sendMode) || SendMode.EMAIL_DVK.equals(sendMode) && dvkCapableOrgs.contains(name)) {
                    encryptionForbidden.add(name);
                }
            }
            if (!encryptionForbidden.isEmpty()) {
                valid = false;
                MessageUtil.addErrorMessage("document_send_out_encryption_forbidden_for_dvk", StringUtils.join(encryptionForbidden, TextUtil.LIST_SEPARATOR));
            }
        }

        if (valid && !idCodesToCheck.isEmpty()) {
            Set<String> unregisteredAditUsers = null;
            try {
                unregisteredAditUsers = BeanHelper.getAditService().getUnregisteredAditUsers(idCodesToCheck);
            } catch (XTeeServiceConsumptionException e) {
                valid = false;
                String faultMessage = e.getNonTechnicalFaultString();
                MessageUtil.addErrorMessage(context, "document_send_failed_xtee_query", StringUtils.isNotBlank(faultMessage) ? faultMessage : e.getFaultString());
            }
            if (unregisteredAditUsers != null && !unregisteredAditUsers.isEmpty()) {
                valid = false;
                for (String user : unregisteredAditUsers) {
                    MessageUtil.addErrorMessage(context, "document_send_failed_no_adit_account", user);
                }
            }
        }

        return valid;
    }

    private List<String> trimAndGetIdCodes() {
        List<String> idCodes = model.getProperties().get(PROP_KEYS[1]);
        List<String> trimmedIdCodes = new ArrayList<String>(idCodes.size());
        for (int i = 0; i < idCodes.size(); i++) {
            trimmedIdCodes.add(i, StringUtils.trim(idCodes.get(i)));
        }
        model.getProperties().put(PROP_KEYS[1], trimmedIdCodes);
        return trimmedIdCodes;
    }

    private boolean sendOut(FacesContext context) {
        boolean result = true;
        boolean isEncrypt = model.isEncrypt();
        List<String> names = new ArrayList<String>();
        List<String> idCodes = new ArrayList<String>();
        List<String> emails = new ArrayList<String>();
        List<String> modes = new ArrayList<String>();
        List<String> encryptionIdCodes = null;

        names.addAll(model.getProperties().get(PROP_KEYS[0]));
        idCodes.addAll(model.getProperties().get(PROP_KEYS[1]));
        emails.addAll(model.getProperties().get(PROP_KEYS[2]));
        modes.addAll(model.getProperties().get(PROP_KEYS[3]));

        if (isEncrypt) {
            encryptionIdCodes = new ArrayList<String>();
            for (EncryptionRecipient encryptionRecipient : getEncryptionRecipients()) {
                if (StringUtils.isBlank(encryptionRecipient.getIdCode())) {
                    MessageUtil.addErrorMessage("document_send_out_encryptionRecipient_idCode_mandatory");
                    return false;
                }
                encryptionIdCodes.add(StringUtils.strip(encryptionRecipient.getIdCode()));
            }
        }

        List<NodeRef> fileRefs = getFileRefs();
        try {
            result = getSendOutService().sendOut(model.getNodeRef(), names, emails, modes, idCodes, encryptionIdCodes, model.getSenderEmail(), model.getSubject(),
                    model.getContent(), fileRefs, model.isZip());
        } catch (UnableToPerformException e) {
            MessageUtil.addStatusMessage(context, e);
            return false;
        } catch (Exception e) {
            log.error(
                    "Sending out document failed\n  nodeRef=" + model.getNodeRef() + "\n  names=" + names + "\n  emails=" + emails + "\n  modes=" + modes
                            + "\n  encryptionIdCodes=" + encryptionIdCodes + "\n  senderEmail=" + model.getSenderEmail() + "\n  subject=" + model.getSubject() + "\n  content="
                            + (model.getContent() == null ? "null" : "String[" + model.getContent().length() + "]") + "\n  fileRefs=" + fileRefs + "\n  zip=" + model.isZip()
                            + "\n  encrypt=" + isEncrypt, e);
            result = false;
        }
        if (!result) {
            MessageUtil.addErrorMessage(context, "document_send_failed");
            getDocumentLogService().addDocumentLog(model.getNodeRef(), MessageUtil.getMessage("document_log_status_sending_failed"));
        } else {
            getDocumentLogService().addDocumentLog(model.getNodeRef(),
                    MessageUtil.getMessage(isEncrypt ? "document_log_status_encrypted_sent" : "document_log_status_sent"));
            ((MenuBean) FacesHelper.getManagedBean(context, MenuBean.BEAN_NAME)).processTaskItem(
                    OutboxDocumentMenuItemProcessor.OUTBOX_DOCUMENT,
                    UnsentDocumentMenuItemProcessor.UNSENT_DOCUMENT);
        }
        return result;
    }

    private List<NodeRef> getFileRefs() {
        @SuppressWarnings("unchecked")
        List<NodeRef> fileRefs = (List<NodeRef>) CollectionUtils.collect(model.getSelectedFiles(), new Transformer() {
            @Override
            public Object transform(Object fileRefString) {
                return new NodeRef((String) fileRefString);
            }
        });
        return fileRefs;
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

    private void showEncryptionRecipientsModal(FacesContext context) {
        final Application application = context.getApplication();
        ModalLayerComponent modal = (ModalLayerComponent) application.createComponent(ModalLayerComponent.class.getCanonicalName());
        modal.setId("encryptionRecipients");
        modal.setActionListener(application.createMethodBinding("#{DocumentSendOutDialog.encryptAndFinish}", UIActions.ACTION_CLASS_ARGS));
        Map<String, Object> attributes = getAttributes(modal);
        attributes.put(ModalLayerComponent.ATTR_HEADER_KEY, "document_send_out_encryption_title");
        attributes.put(ModalLayerComponent.ATTR_SUBMIT_BUTTON_MSG_KEY, "document_send_out_encryption_perform");
        attributes.put(ModalLayerComponent.ATTR_SET_RENDERED_FALSE_ON_CLOSE, Boolean.TRUE);

        UIMessages messages = (UIMessages) application.createComponent(UIMessages.COMPONENT_TYPE);
        messages.setRendererType("org.alfresco.faces.Errors");
        messages.setValueBinding("message", application.createValueBinding("#{DialogManager.errorMessage}"));
        attributes = getAttributes(messages);
        attributes.put("styleClass", "message");
        attributes.put("errorClass", "error-message");
        attributes.put("infoClass", "info-message");
        getChildren(modal).add(messages);

        UIOutput output = (UIOutput) application.createComponent(UIOutput.COMPONENT_TYPE);
        output.setValue("<p>");
        getAttributes(output).put(JSFAttr.ESCAPE_ATTR, Boolean.FALSE);
        getChildren(modal).add(output);

        output = (UIOutput) application.createComponent(UIOutput.COMPONENT_TYPE);
        output.setValue(MessageUtil.getMessage("document_send_out_encryption_intro"));
        getChildren(modal).add(output);

        output = (UIOutput) application.createComponent(UIOutput.COMPONENT_TYPE);
        output.setValue("</p><br />");
        getAttributes(output).put(JSFAttr.ESCAPE_ATTR, Boolean.FALSE);
        getChildren(modal).add(output);

        HtmlDataTable table = (HtmlDataTable) application.createComponent(HtmlDataTable.COMPONENT_TYPE);
        table.setId("encryptRecipients");
        table.setValueBinding("value", application.createValueBinding("#{DocumentSendOutDialog.encryptionRecipients}"));
        table.setVar("row");
        table.setRowClasses("selectedItemsRow,selectedItemsRowAlt");
        table.setHeaderClass("selectedItemsHeader");
        table.setWidth("100%");
        getChildren(modal).add(table);
        createAndAddColumn(application, table, "document_send_out_encryptionRecipient_name", "#{row.name}", true);
        createAndAddColumn(application, table, "document_send_out_encryptionRecipient_idCode", "#{row.idCode}", false);

        List<UIComponent> children = getChildren(getModalContainer());
        children.clear();
        children.add(modal);
        modalId = modal.getClientId(context) + "_popup";

        Map<Pair<String /* name */, String /* email */>, String /* idCode */> idCodesByNameAndEmail = new HashMap<Pair<String, String>, String>();
        for (Node contact : getAddressbookService().listOrganizationAndPerson()) {
            String name = AddressbookUtil.getContactFullName(RepoUtil.toQNameProperties(contact.getProperties()), contact.getType());
            String email = (String) contact.getProperties().get(AddressbookModel.Props.EMAIL.toString());
            String idCode = null;
            if (contact.getType().equals(AddressbookModel.Types.ORGANIZATION)) {
                idCode = (String) contact.getProperties().get(AddressbookModel.Props.ENCRYPTION_PERSON_ID.toString());
            } else if (contact.getType().equals(AddressbookModel.Types.PRIV_PERSON)) {
                idCode = (String) contact.getProperties().get(AddressbookModel.Props.PERSON_ID.toString());
            }
            if (StringUtils.isNotBlank(name) && StringUtils.isNotBlank(email) && StringUtils.isNotBlank(idCode)) {
                idCodesByNameAndEmail.put(Pair.newInstance(name.toLowerCase(), email.toLowerCase()), idCode);
            }
        }

        encryptionRecipients = new ArrayList<EncryptionRecipient>();
        List<String> names = model.getProperties().get(PROP_KEYS[0]);
        List<String> idCodes = model.getProperties().get(PROP_KEYS[1]);
        List<String> emails = model.getProperties().get(PROP_KEYS[2]);
        List<String> sendModes = model.getProperties().get(PROP_KEYS[3]);
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            String email = StringUtils.trim(emails.get(i));
            String idCode = StringUtils.trimToEmpty(idCodes.get(i));
            if (StringUtils.isBlank(idCode)) { // If user hasn't entered any id code, then try to fetch from repo
                idCode = StringUtils.trimToEmpty(idCodesByNameAndEmail.get(Pair.newInstance(name.toLowerCase(), email.toLowerCase())));
            }

            EncryptionRecipient encryptionRecipient = new EncryptionRecipient();
            encryptionRecipient.setName(name);
            encryptionRecipient.setIdCode(idCode);
            encryptionRecipient.setSendMode(sendModes.get(i));
            encryptionRecipients.add(encryptionRecipient);
        }
    }

    private void createAndAddColumn(final Application application, HtmlDataTable table, String titleKey, String valueBinding, boolean disabled) {
        UIColumn column = (UIColumn) application.createComponent(UIColumn.COMPONENT_TYPE);
        getChildren(table).add(column);

        UIOutput headerOutput = (UIOutput) application.createComponent(UIOutput.COMPONENT_TYPE);
        headerOutput.setValue(MessageUtil.getMessage(titleKey));
        addFacet(column, "header", headerOutput);

        UIInput input = (UIInput) application.createComponent(UIInput.COMPONENT_TYPE);
        input.setValueBinding("value", application.createValueBinding(valueBinding));
        if (disabled) {
            getAttributes(input).put(HTML.READONLY_ATTR, Boolean.TRUE);
        }
        getChildren(column).add(input);
    }

    // START: getters / setters

    public SendOutModel getModel() {
        return model;
    }

    public String getModalId() {
        return modalId;
    }

    public void setModalContainer(UIPanel panel) {
        modalContainer = panel;
    }

    public UIPanel getModalContainer() {
        if (modalContainer == null) {
            modalContainer = new UIPanel();
        }
        return modalContainer;
    }

    // END: getters / setters

    public static class EncryptionRecipient implements Serializable {
        private static final long serialVersionUID = 1L;

        private String name;
        private String idCode;
        private String sendMode;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getIdCode() {
            return idCode;
        }

        public void setIdCode(String idCode) {
            this.idCode = idCode;
        }

        public String getSendMode() {
            return sendMode;
        }

        public void setSendMode(String sendMode) {
            this.sendMode = sendMode;
        }

    }

    public static class SendOutModel implements Serializable {

        private static final long serialVersionUID = 1L;

        public SendOutModel() {
            // default constructor
        }

        private NodeRef nodeRef;
        private AccessRestriction docAccessRestriction;
        private SortedMap<String, String> files;
        private List<String> selectedFiles;
        private String docName;
        private String sendDesc;
        private String sendMode;
        private String sendModeByContentType;
        private boolean zip;
        private boolean encrypt;
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

        public boolean isEncrypt() {
            return encrypt;
        }

        public void setEncrypt(boolean encrypt) {
            this.encrypt = encrypt;
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

        public String getSendModeByContentType() {
            return sendModeByContentType;
        }

        public void setSendModeByContentType(String sendModeByContentType) {
            this.sendModeByContentType = sendModeByContentType;
        }

        public AccessRestriction getDocAccessRestriction() {
            return docAccessRestriction;
        }

        public void setDocAccessRestriction(AccessRestriction docAccessRestriction) {
            this.docAccessRestriction = docAccessRestriction;
        }

    }

}
