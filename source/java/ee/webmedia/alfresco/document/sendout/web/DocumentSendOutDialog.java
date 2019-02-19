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
import static ee.webmedia.alfresco.common.web.BeanHelper.getSignatureService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getSkLdapService;
import static ee.webmedia.alfresco.utils.ComponentUtil.addFacet;
import static ee.webmedia.alfresco.utils.ComponentUtil.getAttributes;
import static ee.webmedia.alfresco.utils.ComponentUtil.getChildren;

import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
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
//import javax.faces.component.UISelectBoolean;
import javax.faces.component.html.HtmlDataTable;
import javax.faces.component.html.HtmlOutputText;
import javax.faces.component.html.HtmlSelectBooleanCheckbox;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import ee.sk.digidoc.SignedDoc;
import ee.smit.digisign.domain.SignCertificate;
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
import org.alfresco.web.ui.common.component.UIActionLink;
import org.alfresco.web.ui.common.converter.ByteSizeConverter;
import org.alfresco.web.ui.repo.component.UIActions;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.EmailValidator;
import org.apache.myfaces.shared_impl.renderkit.JSFAttr;
import org.apache.myfaces.shared_impl.renderkit.html.HTML;
import org.apache.myfaces.shared_impl.taglib.UIComponentTagUtils;

import com.nortal.jroad.client.exception.XRoadServiceConsumptionException;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.addressbook.model.AddressbookModel.Types;
import ee.webmedia.alfresco.addressbook.service.AddressbookEntry;
import ee.webmedia.alfresco.addressbook.util.AddressbookUtil;
import ee.webmedia.alfresco.classificator.enums.AccessRestriction;
import ee.webmedia.alfresco.classificator.enums.SendMode;
import ee.webmedia.alfresco.classificator.enums.StorageType;
import ee.webmedia.alfresco.classificator.model.Classificator;
import ee.webmedia.alfresco.classificator.model.ClassificatorValue;
import ee.webmedia.alfresco.common.propertysheet.modalLayer.ModalLayerComponent;
import ee.webmedia.alfresco.common.service.CreateObjectCallback;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docconfig.bootstrap.SystematicDocumentType;
import ee.webmedia.alfresco.docdynamic.model.DocumentChildModel;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.docdynamic.web.DocumentLockHelperBean;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.menu.model.MenuItem;
import ee.webmedia.alfresco.menu.ui.MenuBean;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.privilege.model.Privilege;
import ee.webmedia.alfresco.signature.model.SkLdapCertificate;
import ee.webmedia.alfresco.template.model.ProcessedEmailTemplate;
import ee.webmedia.alfresco.template.model.UnmodifiableDocumentTemplate;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.TextUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.utils.UserUtil;
import ee.webmedia.alfresco.utils.WebUtil;

/**
 * Bean for sending out document dialog.
 */
public class DocumentSendOutDialog extends BaseDialogBean {

    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "DocumentSendOutDialog";

    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DocumentSendOutDialog.class);

    private static final String[] PROP_KEYS = { "recipientName", "recipientId", "recipientEmail", "recipientSendMode", "recipientDvkCapable", "recipientGroup" };

    private transient UIPanel modalContainer;

    private SendOutModel model;
    private List<SelectItem> sendModes;
    private List<SelectItem> emailTemplates;
    private List<EncryptionRecipient> encryptionRecipients;
    private List<OrgCertificate> orgCertificates;
    private String selectedEncriptionRecipientClientId;
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

    public void encryptAndFinish(ActionEvent event) {
        try {
            if (sendOutAndFinish(FacesContext.getCurrentInstance())) {
                WebUtil.navigateWithCancel();
            }
        } catch (UnableToPerformException e) {
            handleException(e);
        }
    }
    
    
    
    private boolean sendOutAndFinish(FacesContext context) {
        log.info("SEND OUT AND FINISH....");
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
    public void clean() {
        resetState();
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
        boolean bdocExists = false;
        List<String> selectedFiles = new ArrayList<>();
        SortedMap<String, String> files = new TreeMap<>();
        final List<File> allFiles = getFileService().getAllActiveFiles(node.getNodeRef());
        ByteSizeConverter converter = (ByteSizeConverter) context.getApplication().createConverter(ByteSizeConverter.CONVERTER_ID);
        for (File file : allFiles) {
            final String fileRef = file.getNodeRef().toString();

            String fileName = file.getName();
            fileName += " (" + converter.getAsString(context, null, file.getSize()) + ")";

            files.put(fileName, fileRef);
            boolean isBdoc = file.isBdoc();
            if (isBdoc) {
                if (!bdocExists) {
                    selectedFiles = new ArrayList<>();
                }
                selectedFiles.add(fileRef);
            } else if (!bdocExists) {
                selectedFiles.add(fileRef);
            }
            bdocExists |= isBdoc;
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
        Map<String, List<String>> properties = new HashMap<>();

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

        List<String> names;
        List<String> idCodes;
        List<String> emails;
        List<String> groups;

        boolean isContract = SystematicDocumentType.CONTRACT.getId().equals(props.get(DocumentAdminModel.Props.OBJECT_TYPE_ID));
        if (isContract) {
            names = new ArrayList<>();
            idCodes = new ArrayList<>();
            emails = new ArrayList<>();
            groups = new ArrayList<>();

            Object partys = props.get(DocumentSpecificModel.Props.PARTY_NAME);
            if (partys instanceof String) {
                String partyName = (String) partys;
                if (StringUtils.isNotBlank(partyName)) {
                    names.add(partyName);
                    String email = (String) props.get(DocumentSpecificModel.Props.PARTY_EMAIL);
                    emails.add(email);
                    groups.add("");
                }
            } else if (partys instanceof List) {
                List<String> partyList = (List<String>) partys;
                List<String> emailList = (List<String>) props.get(DocumentSpecificModel.Props.PARTY_EMAIL);
                for (int i = 0; i < partyList.size(); i++) {
                    names.add(StringUtils.trimToEmpty(partyList.get(i)));
                    emails.add(StringUtils.trimToEmpty(emailList.get(i)));
                    groups.add("");
                }
            }
        } else {
            names = getNames(props, DocumentCommonModel.Props.RECIPIENT_NAME, DocumentDynamicModel.Props.RECIPIENT_PERSON_NAME);
            idCodes = newListIfNull((List<String>) props.get(DocumentDynamicModel.Props.RECIPIENT_ID), false);

            emails = newListIfNull(convertStringToList(props.get(DocumentCommonModel.Props.RECIPIENT_EMAIL)), false);
            groups = newListIfNull((List<String>) props.get(DocumentCommonModel.Props.RECIPIENT_GROUP), false);

            while (groups.size() < names.size()) {
                groups.add("");
            }
            while (idCodes.size() < names.size()) {
                idCodes.add("");
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
                names = new ArrayList<>();
                idCodes = new ArrayList<>();
                emails = new ArrayList<>();
                groups = new ArrayList<>();
            }

        }
        addAdditionalRecipients(props, idCodes, names, emails, groups);
        removeEmptyValuesLeavingOneEmptyLineIfNeeded(names, idCodes, emails, groups);

        Map<Pair<String /* name */, String /* email */>, Boolean /* dvkCapable */> dvkCapableByNameAndEmail = new HashMap<>();
        if (names.size() > 0) {
        	for (Node contact : getAddressbookService().listOrganizationAndPerson()) {
                String name = AddressbookUtil.getContactFullName(RepoUtil.toQNameProperties(contact.getProperties()), contact.getType());
                String email = (String) contact.getProperties().get(AddressbookModel.Props.EMAIL.toString());
                Pair<String, String> nameEmail = Pair.newInstance(name.toLowerCase(), email.toLowerCase());
                Boolean dvkCapable = (Boolean)contact.getProperties().get(AddressbookModel.Props.DVK_CAPABLE.toString());
                
                
                if (StringUtils.isNotBlank(name) && StringUtils.isNotBlank(email)) {
                	dvkCapableByNameAndEmail.put(nameEmail, (dvkCapable != null)?dvkCapable:Boolean.FALSE);
                }
            }
        }
        
        List<String> recSendModes = new ArrayList<>(names.size());
        List<String> recDvkCapable = new ArrayList<>(names.size());
        String dvkNo = MessageUtil.getMessage("document_send_out_dvk_capable_no");
        String dvkYes = MessageUtil.getMessage("document_send_out_dvk_capable_yes");
        
        for (int i = 0; i < names.size(); i++) {
        	String dvkCapableMsg = dvkNo;
        	String recSendModeValue = "";
        	if (StringUtils.isNotBlank(names.get(i)) || StringUtils.isNotBlank(emails.get(i))) {
        		recSendModeValue = defaultSendMode;
            }
        	if (StringUtils.isNotBlank(names.get(i)) && StringUtils.isNotBlank(emails.get(i))) {
        		Pair<String, String> nameEmail = Pair.newInstance(names.get(i).toLowerCase(), emails.get(i).toLowerCase());
        		Boolean dvkCapable = null;
        		if (dvkCapableByNameAndEmail.containsKey(nameEmail)) {
        			dvkCapable = dvkCapableByNameAndEmail.get(nameEmail);
        		}
        		if (dvkCapable != null && dvkCapable) {
        			dvkCapableMsg = dvkYes;
        			recSendModeValue = SendMode.EMAIL_DVK.getValueName();
        		} else if (dvkCapable != null && !dvkCapable) {
        			recSendModeValue = SendMode.EMAIL.getValueName();
        		}
            }
        	recDvkCapable.add(dvkCapableMsg);
        	recSendModes.add(recSendModeValue);
        }

        properties.put(PROP_KEYS[0], names);
        properties.put(PROP_KEYS[1], idCodes);
        properties.put(PROP_KEYS[2], emails);
        properties.put(PROP_KEYS[3], recSendModes);
        properties.put(PROP_KEYS[4], recDvkCapable);
        properties.put(PROP_KEYS[5], groups);

        model.setProperties(properties);
    }

    /**
     * 
     * @param obj
     * @return
     */
    private List<String> convertStringToList(Object obj){
        if(obj instanceof String){
            List<String> list = new ArrayList<>();
            String str = (String) obj;
            list.add(str);
            return list;
        }

        return (List<String>) obj;
    }

    @SuppressWarnings("unchecked")
    private void addAdditionalRecipients(Map<String, Object> props, List<String> idCodes, List<String> names, List<String> emails, List<String> groups) {
        List<String> namesAdd = getNames(props, DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_NAME, DocumentDynamicModel.Props.ADDITIONAL_RECIPIENT_PERSON_NAME);
        List<String> idCodesAdd = newListIfNull((List<String>) props.get(DocumentDynamicModel.Props.ADDITIONAL_RECIPIENT_ID), false);
        List<String> emailsAdd = newListIfNull((List<String>) props.get(DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_EMAIL), false);
        List<String> groupsAdd = newListIfNull((List<String>) props.get(DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_GROUP), false);
        RepoUtil.validateSameSize(namesAdd, emailsAdd, "additionalNames", "additionalEmails");
        while (groupsAdd.size() < namesAdd.size()) {
            groupsAdd.add("");
        }
        while (idCodesAdd.size() < namesAdd.size()) {
            idCodesAdd.add("");
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

    private void removeEmptyValuesLeavingOneEmptyLineIfNeeded(List<String> names, List<String> idCodes, List<String> emails, List<String> groups) {
        List<Integer> removeIndexes = new ArrayList<>();
        int j = 0;
        for (Iterator<String> it = names.iterator(); it.hasNext();) {
            String name = it.next();
            if (StringUtils.isBlank(name) && StringUtils.isBlank(emails.get(j))) {
                it.remove();
                removeIndexes.add(j);
            }
            j++;
        }
        Collections.reverse(removeIndexes);
        for (Integer index : removeIndexes) {
            emails.remove((int) index);
            groups.remove((int) index);
            if (idCodes.size() > index) {
                idCodes.remove((int) index);
            }
        }
        if (names.size() == 0) {
            names.add("");
            idCodes.add("");
            emails.add("");
            groups.add("");
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> getNames(Map<String, Object> props, QName prop, QName alternativeProp) {
        List<String> mainValues = newListIfNull((List<String>) props.get(prop), false);
        Object alternative = props.get(alternativeProp);
        List<String> alternativeValues = (alternative instanceof List<?>) ? newListIfNull((List<String>) alternative, false) : null;
        List<String> result = new ArrayList<>(mainValues);
        for (int i = 0; i < mainValues.size(); i++) {
            if (StringUtils.isBlank(mainValues.get(i)) && (alternativeValues != null && StringUtils.isNotBlank(alternativeValues.get(i)))) {
                result.set(i, alternativeValues.get(i));
            }
        }
        return result;
    }

    public String getPanelTitle() {
        return model.getDocName();
    }

    public List<SelectItem> getSendModes() {
        if (sendModes == null) {
            sendModes = new ArrayList<>();
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
            emailTemplates = new ArrayList<>();
            emailTemplates.add(new SelectItem("", MessageUtil.getMessage(FacesContext.getCurrentInstance(), "document_choose")));
            for (UnmodifiableDocumentTemplate template : getDocumentTemplateService().getEmailTemplates()) {
                String templateName = FilenameUtils.getBaseName(template.getName());
                emailTemplates.add(new SelectItem(template.getNodeRef().toString(), templateName));
            }
        }
        return emailTemplates;
    }

    public List<EncryptionRecipient> getEncryptionRecipients() {
        return encryptionRecipients;
    }
    
    public List<OrgCertificate> getOrgCertificates() {
        return orgCertificates;
    }

    public void updateSendModes(ActionEvent event) {
        List<String> recSendModes = model.getProperties().get(PROP_KEYS[3]);
        for (int i = 0; i < recSendModes.size(); i++) {
            recSendModes.set(i, model.getSendMode());
        }
    }

    public void updateTemplate(ActionEvent event) {
        if (StringUtils.isNotBlank(model.getTemplate())) {
            LinkedHashMap<String, NodeRef> nodeRefs = new LinkedHashMap<>();
            nodeRefs.put(null, model.getNodeRef());
            ProcessedEmailTemplate template = getDocumentTemplateService().getProcessedEmailTemplate(nodeRefs, new NodeRef(model.getTemplate()));
            model.setContent(template.getContent());
        }
    }

    public List<String> fetchContactData(String nodeRef) {
        List<String> result;
        QName type = getNodeService().getType(new NodeRef(nodeRef));
        boolean dvkCapable = false;
        if (type.equals(ContentModel.TYPE_PERSON)) {
            result = getPersonData(nodeRef);
        } else {
            result = AddressbookUtil.getContactData(nodeRef);
            Boolean contactDvkCapable = (Boolean)getNodeService().getProperty(new NodeRef(nodeRef), AddressbookModel.Props.DVK_CAPABLE);
            if (contactDvkCapable != null && contactDvkCapable) {
            	dvkCapable = true;
            }
        }
        String defaultSendMode = SendMode.EMAIL.getValueName();
        String dvkCapableMsg = MessageUtil.getMessage("document_send_out_dvk_capable_no");
        if (dvkCapable) {
	        String storageType = (String) getNodeService().getProperty(model.getNodeRef(), DocumentCommonModel.Props.STORAGE_TYPE);
	        if (StringUtils.isNotBlank(storageType)) {
	            if (StorageType.DIGITAL.equals(storageType)) {
	                defaultSendMode = SendMode.EMAIL_DVK.getValueName();
	                dvkCapableMsg = MessageUtil.getMessage("document_send_out_dvk_capable_yes");
	            }
	        }
        }
        result.add(defaultSendMode);
        result.add(dvkCapableMsg);
        return result;
    }

    private List<String> getPersonData(String nodeRef) {
        Map<QName, Serializable> personProps = getNodeService().getProperties(new NodeRef(nodeRef));
        String name = UserUtil.getPersonFullName1(personProps);
        String id = (String) personProps.get(ContentModel.PROP_USERNAME);
        String email = (String) personProps.get(ContentModel.PROP_EMAIL);
        List<String> data = new ArrayList<>();
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
        encryptionRecipients = null;
        orgCertificates = null;
        selectedEncriptionRecipientClientId = null;
    }

    private boolean validate(FacesContext context) {
        log.info("DOCUMENT SEND OUT - VALIDATE....");
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
        List<Pair<String, String>> dvkRecipients = new ArrayList<>();
        Set<String> idCodesToCheck = new HashSet<>();
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            log.debug("NAME: " + name);
            String idCode = idCodes.get(i);
            log.debug("ID-CODE: " + idCode);
            String email = StringUtils.trim(emails.get(i));
            log.debug("EMAIL: " + email);
            String mode = modes.get(i);
            log.debug("MODE: " + mode);

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
                log.info("SEND MODE IS STATE PORTAL EESTI EE..");
                if (StringUtils.isBlank(idCode)) {
                    hasMissingIdCodes = true;
                } else {
                    log.debug("ADD ID-CODE TO check SET..." + idCode);
                    idCodesToCheck.add(idCode);
                    dvkRecipients.add(new Pair<>(name, mode));
                }
            }
            if (StringUtils.isNotBlank(mode) && SendMode.EMAIL_DVK.equals(mode)) {
                dvkRecipients.add(new Pair<>(name, mode));
            }
            if (StringUtils.isNotBlank(email)) {
                if (!email.contains("@") || email.startsWith("@") || email.endsWith("@") || StringUtils.countMatches(email, "@") > 1) {
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
            List<String> encryptionForbidden = new ArrayList<>();
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
                log.info("ADIT: GET USER(S) ADIT STATUS....");
                unregisteredAditUsers = BeanHelper.getAditService().getUnregisteredAditUsers(idCodesToCheck);

            } catch (XRoadServiceConsumptionException e) {
                valid = false;
                String faultMessage = e.getNonTechnicalFaultString();
                MessageUtil.addErrorMessage(context, "document_send_failed_xtee_query", StringUtils.isNotBlank(faultMessage) ? faultMessage : e.getFaultString());
            }
            if (unregisteredAditUsers != null && !unregisteredAditUsers.isEmpty()) {
                log.info("ADIT: unregisteredAditUsers is not NULL or is not EMPTY!");
                valid = false;
                for (String user : unregisteredAditUsers) {
                    log.info("ADIT: USER: " + user);
                    MessageUtil.addErrorMessage(context, "document_send_failed_no_adit_account", user);
                }
            }
        }
        if(valid){
            log.info("DOCUMENT SEND OUT - VALIDATE.... ALL IS VALID! - TRUE");
        } else {
            log.info("DOCUMENT SEND OUT - VALIDATE.... NOT VALID! - FALSE");
        }
        return valid;
    }

    private List<String> trimAndGetIdCodes() {
        List<String> idCodes = model.getProperties().get(PROP_KEYS[1]);
        List<String> trimmedIdCodes = new ArrayList<>(idCodes.size());
        for (int i = 0; i < idCodes.size(); i++) {
            trimmedIdCodes.add(i, StringUtils.trim(idCodes.get(i)));
        }
        model.getProperties().put(PROP_KEYS[1], trimmedIdCodes);
        return trimmedIdCodes;
    }

    private boolean sendOut(FacesContext context) {
        log.info("SEND OUT...");
        boolean result = true;
        boolean isEncrypt = model.isEncrypt();
        List<String> names = new ArrayList<>();
        List<String> idCodes = new ArrayList<>();
        List<String> emails = new ArrayList<>();
        List<String> modes = new ArrayList<>();
        List<String> encryptionIdCodes = null;
        List<X509Certificate> allCertificates = null;

        log.info("ADD ALL NAMES...");
        names.addAll(model.getProperties().get(PROP_KEYS[0]));
        log.info("ADD ALL ID-CODES...");
        idCodes.addAll(model.getProperties().get(PROP_KEYS[1]));
        log.info("ADD ALL EMAILS...");
        emails.addAll(model.getProperties().get(PROP_KEYS[2]));
        log.info("ADD ALL MODES...");
        modes.addAll(model.getProperties().get(PROP_KEYS[3]));

        if (isEncrypt) {
            encryptionIdCodes = new ArrayList<>();
            allCertificates = new ArrayList<>();
            for (EncryptionRecipient encryptionRecipient : getEncryptionRecipients()) {
                if (StringUtils.isBlank(encryptionRecipient.getIdCode()) && !encryptionRecipient.getEncryptForOrganization()) {
                    MessageUtil.addErrorMessage("document_send_out_encryptionRecipient_idCode_mandatory");
                    return false;
                }
                if (encryptionRecipient.getEncryptForOrganization()) {
                	if (StringUtils.isBlank(encryptionRecipient.getOrganizationCode())) {
                		MessageUtil.addErrorMessage("document_send_out_encryptionRecipient_organizationCode_missing", encryptionRecipient.getName());
                        return false;
                	}
                	if (!hasSelectedOrgCerts(encryptionRecipient.getOrgCertificates())) {
                		MessageUtil.addErrorMessage("document_send_out_encryptionRecipient_organizationCerts_missing", encryptionRecipient.getName());
                        return false;
                	}
                	List<OrgCertificate> orgCertificates = encryptionRecipient.getOrgCertificates();
                	for (OrgCertificate orgCertificate: orgCertificates) {
                		if (orgCertificate.getSelected()) {
                			allCertificates.add(getSignatureService().getCertificateForEncryption(orgCertificate.getCertData(), orgCertificate.getCertName()));
                		}
                	}
                	encryptionIdCodes.add("");
                } else {
                	encryptionIdCodes.add(StringUtils.strip(encryptionRecipient.getIdCode()));
                }
            }
        }

        List<NodeRef> fileRefs = getFileRefs();
        try {
            log.debug("Try send out...");
            result = getSendOutService().sendOut(model.getNodeRef(), names, emails, modes, idCodes, encryptionIdCodes, allCertificates, model.getSenderEmail(), model.getSubject(),
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
                    MenuItem.OUTBOX_DOCUMENT,
                    MenuItem.UNSENT_DOCUMENT);
        }
        return result;
    }
    
    private boolean hasSelectedOrgCerts(List<OrgCertificate> orgCerts) {
    	boolean found = false;
    	if (orgCerts != null && !orgCerts.isEmpty()) {
    		for (OrgCertificate orgCert: orgCerts) {
    			if (orgCert.getSelected()) {
    				found = true;
    				break;
    			}
    		}
    	}
    	return found;
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
            result = new ArrayList<>();
        }
        if (result.isEmpty() && checkEmpty) {
            result.add("");
        }
        return result;
    }
    
    private List<OrgCertificate> fillOrgCertificates(NodeRef contactNodeRef, String orgName, String orgCode) {
    	List<OrgCertificate> orgCerts = new ArrayList<OrgCertificate>();
    	// get stored orgCerts
    	List<AddressbookEntry> addressbookOrgCerts = null;
    	if (contactNodeRef != null && getNodeService().exists(contactNodeRef)) {
    		addressbookOrgCerts = getAddressbookService().listOrganizationCertificates(contactNodeRef);
    	}
		if (addressbookOrgCerts == null || addressbookOrgCerts.isEmpty()) {

            if(BeanHelper.getDigiSignService().getDigiSignServiceActive()){
                log.info("Using DigiSign-service to get certificates...");
                List<SignCertificate> signCertificateList = BeanHelper.getDigiSignSearches().getCertificatesFromDigiSignService(orgCode, orgName);
                if(signCertificateList == null || signCertificateList.isEmpty()){
                    log.info("Certificates not found!");
                }

                int i = 0;
                for(SignCertificate cert : signCertificateList){
                    i++;
                    try{
                        X509Certificate certX509 = SignedDoc.readCertificate(cert.getData());
                        log.info("Certificate CN: " + cert.getCn());
                        OrgCertificate orgCert = new OrgCertificate();
                        orgCert.setCertName(cert.getCn());
                        orgCert.setCertData(cert.getData());

                        Date validTo = certX509.getNotAfter();
                        orgCert.setValidTo(validTo);
                        orgCerts.add(orgCert);

                    }catch (Exception e){
                        log.error(e.getMessage(), e);
                    }
                }



            } else {
                log.info("Using SK LDAP to get certificates...");
                // if no stored orgCerts, retrieve from SKLDAP
                List<SkLdapCertificate> skLdapCerts;
                if (StringUtils.isNotBlank(orgCode)) {
                    try {
                        skLdapCerts = getSkLdapService().getCertificates(orgCode);
                    } catch (Exception e) {
                        // if SKLDAP by serialNumber returns error try to search by cn
                        if (StringUtils.isNotBlank(orgName)) {
                            skLdapCerts = getSkLdapService().getCertificatesByName(orgName);
                        } else {
                            throw e;
                        }
                    }
                } else {
                    skLdapCerts = getSkLdapService().getCertificatesByName(orgName);
                }

                if (skLdapCerts != null && !skLdapCerts.isEmpty()) {
                    // update orgCertificates
                    for (SkLdapCertificate skLdapCert: skLdapCerts) {
                        X509Certificate cert = getSignatureService().getCertificateForEncryption(skLdapCert);
                        if (cert != null) {
                            OrgCertificate orgCert = new OrgCertificate();
                            String cnName = skLdapCert.getCn();
                            Date validTo = cert.getNotAfter();
                            orgCert.setCertName(cnName);
                            orgCert.setValidTo(validTo);
                            orgCert.setCertData(skLdapCert.getUserEncryptionCertificate());

                            orgCerts.add(orgCert);
                        }
                    }
                }
            }
		} else {
			for (AddressbookEntry addressbookOrgCert: addressbookOrgCerts) {
				OrgCertificate orgCert = new OrgCertificate();
				orgCert.setCertName(addressbookOrgCert.getCertName());
				orgCert.setCertDescription(addressbookOrgCert.getCertDescription());
				orgCert.setValidTo(addressbookOrgCert.getCertValidTo());
				orgCert.setCertData(Base64.decodeBase64(addressbookOrgCert.getCertContent()));
				
				orgCerts.add(orgCert);
			}
		}
    	return orgCerts;
    }
    private void showEncryptionRecipientsModal(FacesContext context) {
    	Map<Pair<String /* name */, String /* email */>, String /* idCode */> idCodesByNameAndEmail = new HashMap<>();
        Map<Pair<String /* name */, String /* email */>, String /* idCode */> organizationCodeByNameAndEmail = new HashMap<>();
        Map<Pair<String /* name */, String /* email */>, NodeRef /* idCode */> organizationNodeRefByNameAndEmail = new HashMap<>();
        Map<Pair<String /* name */, String /* email */>, Boolean /* isOrganizationType */> isOrganizationTypeByNameAndEmail = new HashMap<>();
        List<NodeRef> orgs = new ArrayList<>();
        for (Node contact : getAddressbookService().listOrganizationAndPerson()) {
            String name = AddressbookUtil.getContactFullName(RepoUtil.toQNameProperties(contact.getProperties()), contact.getType());
            String email = (String) contact.getProperties().get(AddressbookModel.Props.EMAIL.toString());
            Pair<String, String> nameEmail = Pair.newInstance(name.toLowerCase(), email.toLowerCase());
            String idCode = null;
            if (contact.getType().equals(AddressbookModel.Types.ORGANIZATION)) {
            	orgs.add(contact.getNodeRef());
            	if (StringUtils.isNotBlank(name) && StringUtils.isNotBlank(email)) {
            		String orgIdCode = (String) contact.getProperties().get(AddressbookModel.Props.ORGANIZATION_CODE.toString());
            		isOrganizationTypeByNameAndEmail.put(nameEmail, true);
            		organizationCodeByNameAndEmail.put(nameEmail, orgIdCode);
            		organizationNodeRefByNameAndEmail.put(nameEmail, contact.getNodeRef());
                }
                idCode = (String) contact.getProperties().get(AddressbookModel.Props.ENCRYPTION_PERSON_ID.toString());
            } else if (contact.getType().equals(AddressbookModel.Types.PRIV_PERSON)) {
            	if (StringUtils.isNotBlank(name) && StringUtils.isNotBlank(email)) {
            		isOrganizationTypeByNameAndEmail.put(nameEmail, false);
                }
            	idCode = (String) contact.getProperties().get(AddressbookModel.Props.PERSON_ID.toString());
            }
            if (StringUtils.isNotBlank(name) && StringUtils.isNotBlank(email) && StringUtils.isNotBlank(idCode)) {
                idCodesByNameAndEmail.put(nameEmail, idCode);
            }
        }
        
        if (!orgs.isEmpty()) {
	        Map<NodeRef, List<Node>> personsByOrg = BeanHelper.getBulkLoadNodeService().loadChildNodes(orgs, new HashSet<QName>(), Types.ORGPERSON, null,
	                new CreateObjectCallback<Node>() {
	                    @Override
	                    public Node create(NodeRef nodeRef, Map<QName, Serializable> properties) {
	                        return new WmNode(nodeRef, Types.ORGPERSON, null, properties);
	                    }
	                });
	
	        for (Map.Entry<NodeRef, List<Node>> addressbookEntries : personsByOrg.entrySet()) {
	            for (Node node : addressbookEntries.getValue()) {
	            	String name = AddressbookUtil.getContactFullName(RepoUtil.toQNameProperties(node.getProperties()), node.getType());
	                String email = (String) node.getProperties().get(AddressbookModel.Props.EMAIL.toString());
	                Pair<String, String> nameEmail = Pair.newInstance(name.toLowerCase(), email.toLowerCase());
	                String idCode = (String) node.getProperties().get(AddressbookModel.Props.PERSON_ID.toString());
	                if (StringUtils.isNotBlank(email) && StringUtils.isNotBlank(idCode)) {
	                	idCodesByNameAndEmail.put(nameEmail, idCode);
	                }
	            }
	        }
        }
        
    	encryptionRecipients = new ArrayList<>();
        List<String> names = model.getProperties().get(PROP_KEYS[0]);
        List<String> idCodes = model.getProperties().get(PROP_KEYS[1]);
        List<String> emails = model.getProperties().get(PROP_KEYS[2]);
        List<String> sendModes = model.getProperties().get(PROP_KEYS[3]);
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            String email = StringUtils.trim(emails.get(i));
            String idCode = StringUtils.trimToEmpty(idCodes.get(i));
            Pair<String, String> nameEmail = Pair.newInstance(name.toLowerCase(), email.toLowerCase());
            if (StringUtils.isBlank(idCode)) { // If user hasn't entered any id code, then try to fetch from repo
                idCode = StringUtils.trimToEmpty(idCodesByNameAndEmail.get(nameEmail));
            }
            Boolean isOrganizationType = isOrganizationTypeByNameAndEmail.get(nameEmail);
            String organizationCode  = organizationCodeByNameAndEmail.get(nameEmail);
            NodeRef orgNodeRef = organizationNodeRefByNameAndEmail.get(nameEmail);
            
            EncryptionRecipient encryptionRecipient = new EncryptionRecipient();
            encryptionRecipient.setName(name);
            encryptionRecipient.setIdCode(idCode);
            encryptionRecipient.setOrganizationCode(organizationCode);
            encryptionRecipient.setSendMode(sendModes.get(i));
            encryptionRecipient.setEncryptForOrganization((isOrganizationType != null)?isOrganizationType:false);
            encryptionRecipient.setEncryptForOrganizationNotAllowed((isOrganizationType != null)?!isOrganizationType:false);
            encryptionRecipient.setNodeRef(orgNodeRef);
            
            encryptionRecipients.add(encryptionRecipient);
        }
        
        buildEncryptRecipientsModal(context);

    }
    
    private void buildEncryptRecipientsModal(FacesContext context) {
        final Application application = context.getApplication();
        ModalLayerComponent modal = (ModalLayerComponent) application.createComponent(ModalLayerComponent.class.getCanonicalName());
        modal.setId("encryptionRecipients");
        modal.setActionListener(application.createMethodBinding("#{DocumentSendOutDialog.encryptAndFinish}", UIActions.ACTION_CLASS_ARGS));
        Map<String, Object> attributes = getAttributes(modal);
        attributes.put(ModalLayerComponent.ATTR_HEADER_KEY, "document_send_out_encryption_title");
        attributes.put(ModalLayerComponent.ATTR_SUBMIT_BUTTON_MSG_KEY, "document_send_out_encryption_perform");
        attributes.put(ModalLayerComponent.ATTR_SET_RENDERED_FALSE_ON_CLOSE, Boolean.TRUE);
        
        attributes.put(ModalLayerComponent.ATTR_STYLE_CLASS, "modalpopup-large");

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
        createAndAddCheckboxColumn(context, table, "document_send_out_encryptionRecipient_encryptForOrganization", "#{row.encryptForOrganization}", "#{row.encryptForOrganizationNotAllowed}", "#{DialogManager.bean.updateContactId}");
        createAndAddColumn(application, table, "document_send_out_encryptionRecipient_idCode", "#{row.currentIdCode}", false);
        createAndAddColumnOutput(application, table, "document_send_out_encryptionRecipient_orgCertificates", "#{row.orgCertificatesList}", "#{row.showChooseOrgCertificatesLink}");
        createAndAddColumnLink(application, table, null, null, "#{row.showChooseOrgCertificatesLink}", "#{DialogManager.bean.chooseOrgCerts}");

        List<UIComponent> children = getChildren(getModalContainer());
        children.clear();
        children.add(modal);
        modalId = modal.getClientId(context) + "_popup";
    }
    
    private void buildOrgCertificatesModal(FacesContext context, String orgName) {
        final Application application = context.getApplication();
        ModalLayerComponent modal = (ModalLayerComponent) application.createComponent(ModalLayerComponent.class.getCanonicalName());
        modal.setId("orgCertificates");
        modal.setActionListener(application.createMethodBinding("#{DocumentSendOutDialog.updateSelectedOrgCertificates}", UIActions.ACTION_CLASS_ARGS));
        Map<String, Object> attributes = getAttributes(modal);
        attributes.put(ModalLayerComponent.ATTR_HEADER_KEY, "document_send_out_encryption_org_cert_intro");
        attributes.put(ModalLayerComponent.ATTR_HEADER_KEY_ARG, orgName);
        attributes.put(ModalLayerComponent.ATTR_SUBMIT_BUTTON_MSG_KEY, "document_send_out_encryption_org_cert_next");
        attributes.put(ModalLayerComponent.ATTR_SET_RENDERED_FALSE_ON_CLOSE, Boolean.TRUE);
        
        attributes.put(ModalLayerComponent.ATTR_STYLE_CLASS, "modalpopup-large");

        UIMessages messages = (UIMessages) application.createComponent(UIMessages.COMPONENT_TYPE);
        messages.setRendererType("org.alfresco.faces.Errors");
        messages.setValueBinding("message", application.createValueBinding("#{DialogManager.errorMessage}"));
        attributes = getAttributes(messages);
        attributes.put("styleClass", "message");
        attributes.put("errorClass", "error-message");
        attributes.put("infoClass", "info-message");
        getChildren(modal).add(messages);
        
        HtmlDataTable table = (HtmlDataTable) application.createComponent(HtmlDataTable.COMPONENT_TYPE);
        table.setId("orgCertificatesTable");
        table.setValueBinding("value", application.createValueBinding("#{DocumentSendOutDialog.orgCertificates}"));
        table.setVar("row");
        table.setRowClasses("recordSetRow, recordSetRowAlt");
        table.setHeaderClass("selectedItemsHeader");
        table.setWidth("100%");
        getChildren(modal).add(table);
        
        createAndAddCheckboxColumn(context, table, null, "#{row.selected}", null, null);
        createAndAddColumnOutput(application, table, "document_send_out_encryption_org_cert_name", "#{row.certName}", null);
        createAndAddColumnOutput(application, table, "document_send_out_encryption_org_cert_description", "#{row.certDescription}", null);
        createAndAddColumnOutput(application, table, "document_send_out_encryption_org_cert_validTo", "#{row.validTo}", null);
        
        List<UIComponent> children = getChildren(getModalContainer());
        children.clear();
        children.add(modal);
        modalId = modal.getClientId(context) + "_popup";
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
    
    private void createAndAddColumnOutput(final Application application, HtmlDataTable table, String titleKey, String valueBinding, String renderedBinding) {
    	UIColumn column = (UIColumn) application.createComponent(UIColumn.COMPONENT_TYPE);
        getChildren(table).add(column);

        UIOutput headerOutput = (UIOutput) application.createComponent(UIOutput.COMPONENT_TYPE);
        headerOutput.setValue((StringUtils.isNotBlank(titleKey))?MessageUtil.getMessage(titleKey):"");
        addFacet(column, "header", headerOutput);
        HtmlOutputText output = (HtmlOutputText) application.createComponent(HtmlOutputText.COMPONENT_TYPE);
        output.setValueBinding("value", application.createValueBinding(valueBinding));
        
        getChildren(column).add(output);
    }
    
    private void createAndAddColumnLink(final Application application, HtmlDataTable table, String titleKey, String valueBinding, String renderedBinding, String methodBinding) {
    	FacesContext context = FacesContext.getCurrentInstance();
    	UIColumn column = (UIColumn) application.createComponent(UIColumn.COMPONENT_TYPE);
        getChildren(table).add(column);

        UIOutput headerOutput = (UIOutput) application.createComponent(UIOutput.COMPONENT_TYPE);
        headerOutput.setValue((StringUtils.isNotBlank(titleKey))?MessageUtil.getMessage(titleKey):"");
        addFacet(column, "header", headerOutput);
        
        UIActionLink link = (UIActionLink) application.createComponent(UIActions.COMPONENT_ACTIONLINK);
        link.setRendererType(UIActions.RENDERER_ACTIONLINK);
        link.setValue(MessageUtil.getMessage("document_send_out_encryption_choose_org_cert"));
        
        link.setActionListener(application.createMethodBinding(methodBinding, new Class[] { javax.faces.event.ActionEvent.class }));
        if (renderedBinding != null) {
            UIComponentTagUtils.setValueBinding(context, link, "rendered", renderedBinding);
        }
        getChildren(column).add(link);
    }
    
    private void createAndAddCheckboxColumn(FacesContext context, HtmlDataTable table, String titleKey, String valueBinding, String valueBindingDisabled, String valueChangeMethodBinding) {
    	final Application application = context.getApplication();
    	UIColumn column = (UIColumn) application.createComponent(UIColumn.COMPONENT_TYPE);
        getChildren(table).add(column);
        
        UIOutput headerOutput = (UIOutput) application.createComponent(UIOutput.COMPONENT_TYPE);
        headerOutput.setValue((StringUtils.isNotBlank(titleKey))?MessageUtil.getMessage(titleKey):"");
        addFacet(column, "header", headerOutput);
        
        
        HtmlSelectBooleanCheckbox checkbox = (HtmlSelectBooleanCheckbox) application.createComponent(HtmlSelectBooleanCheckbox.COMPONENT_TYPE);
        checkbox.setValueBinding("value", application.createValueBinding(valueBinding));
        if (StringUtils.isNotBlank(valueBindingDisabled)) {
        	checkbox.setValueBinding("disabled", application.createValueBinding(valueBindingDisabled));
        }
        if (StringUtils.isNotBlank(valueChangeMethodBinding)) {
        	checkbox.setValueChangeListener(application.createMethodBinding(valueChangeMethodBinding, new Class[] { ValueChangeEvent.class }));
        	checkbox.setOnchange(ComponentUtil.generateAjaxFormSubmit(context, getModalContainer(), null, null, 1));
        }
        
        getChildren(column).add(checkbox);
    }
    
    public void chooseOrgCerts(ActionEvent event) {
    	FacesContext context = FacesContext.getCurrentInstance();
    	selectedEncriptionRecipientClientId = event.getComponent().getClientId(context);
    	Integer rowIndex = ComponentUtil.getIndexFromClientId(selectedEncriptionRecipientClientId);
    	if (rowIndex != null) {
    		EncryptionRecipient encryptionRecipient = encryptionRecipients.get(rowIndex);
    		if (encryptionRecipient.getOrgCertificates().isEmpty()) {
	    		orgCertificates = fillOrgCertificates(encryptionRecipient.getNodeRef(), encryptionRecipient.getName(), encryptionRecipient.getOrganizationCode());
	    		if (orgCertificates == null || orgCertificates.isEmpty()) {
	    			MessageUtil.addErrorMessage(context, "document_send_out_encryptionRecipient_skLdapCerts_missing");
	    			return;
	    		}
    		} else {
    			orgCertificates = encryptionRecipient.getOrgCertificates();
    		}
    		encryptionRecipient.setOrgCertificates(orgCertificates);
    		buildOrgCertificatesModal(context, encryptionRecipient.getName());
    	}
    }
    
    public void updateContactId(ValueChangeEvent event) {
    	FacesContext context = FacesContext.getCurrentInstance();
    	Boolean newSelected = (Boolean) event.getNewValue();
    	String clientId = event.getComponent().getClientId(context);
    	
    	Integer rowIndex = ComponentUtil.getIndexFromClientId(clientId);

    	
    	if (rowIndex != null) {
    		EncryptionRecipient encryptionRecipient = encryptionRecipients.get(rowIndex);
    		encryptionRecipient.setEncryptForOrganization(newSelected);
    	}
    	
    	buildEncryptRecipientsModal(FacesContext.getCurrentInstance());
    }
    
    public void updateSelectedOrgCertificates(ActionEvent event) {
        try {
        	Integer rowIndex = ComponentUtil.getIndexFromClientId(selectedEncriptionRecipientClientId);
        	if (rowIndex != null) {
        		EncryptionRecipient encryptionRecipient = encryptionRecipients.get(rowIndex);
        		encryptionRecipient.setOrgCertificates(orgCertificates);
        	}
            buildEncryptRecipientsModal(FacesContext.getCurrentInstance());
        } catch (UnableToPerformException e) {
            handleException(e);
        }
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
        
        private NodeRef nodeRef;
		private String name;
        private String idCode;
        private String organizationCode;
        private Boolean encryptForOrganization;
        private Boolean encryptForOrganizationNotAllowed;
		private String sendMode;
		
		private List<OrgCertificate> orgCertificates = new ArrayList<OrgCertificate>(); 
		
		public NodeRef getNodeRef() {
			return nodeRef;
		}

		public void setNodeRef(NodeRef nodeRef) {
			this.nodeRef = nodeRef;
		}

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
        
        public String getOrganizationCode() {
			return organizationCode;
		}

		public void setOrganizationCode(String organizationCode) {
			this.organizationCode = organizationCode;
		}

		public Boolean getEncryptForOrganization() {
			return encryptForOrganization;
		}

		public void setEncryptForOrganization(Boolean encryptForOrganization) {
			this.encryptForOrganization = encryptForOrganization;
		}

		public Boolean getEncryptForOrganizationNotAllowed() {
			return encryptForOrganizationNotAllowed;
		}

		public void setEncryptForOrganizationNotAllowed(Boolean encryptForOrganizationNotAllowed) {
			this.encryptForOrganizationNotAllowed = encryptForOrganizationNotAllowed;
		}
		
		public String getCurrentIdCode() {
			if (encryptForOrganization != null && encryptForOrganization) {
				return organizationCode;
			} else {
				return idCode;
			}
		}
		
		public void setCurrentIdCode(String currentIdCode) {
			if (encryptForOrganization != null && encryptForOrganization) {
				organizationCode = currentIdCode;
			} else {
				idCode = currentIdCode;
			}
		}
		
		public String getOrgCertificatesList() {
			StringBuilder sb = new StringBuilder();
			if (encryptForOrganization != null && encryptForOrganization) {
				for (OrgCertificate orgCert: orgCertificates) {
					if (orgCert.getSelected()) {
						if (sb.length() > 0) {
							sb.append("; ");
						}
						sb.append(orgCert.getCertName());
					}
				}
			}
			return sb.toString();
		}
		
		public boolean getShowChooseOrgCertificatesLink() {
			if (encryptForOrganization != null && encryptForOrganization) {
				return true;
			}
			return false;
		}
		
		public List<OrgCertificate> getOrgCertificates() {
			return orgCertificates;
		}

		public void setOrgCertificates(List<OrgCertificate> orgCertificates) {
			this.orgCertificates = orgCertificates;
		}
		
    }
    
    public static class OrgCertificate implements Serializable {
    	private static final long serialVersionUID = 1L;

        private String certName;
		private Date validTo;
        private byte[] certData;
        private String certDescription;
        private boolean selected;
        
        public boolean getSelected() {
			return selected;
		}
		public void setSelected(boolean selected) {
			this.selected = selected;
		}
		public String getCertName() {
			return certName;
		}
		public void setCertName(String certName) {
			this.certName = certName;
		}
		public Date getValidTo() {
			return validTo;
		}
		public void setValidTo(Date validTo) {
			this.validTo = validTo;
		}
		public byte[] getCertData() {
			return certData;
		}
		public void setCertData(byte[] certData) {
			this.certData = certData;
		}
		public String getCertDescription() {
			return certDescription;
		}
		public void setCertDescription(String certDescription) {
			this.certDescription = certDescription;
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
