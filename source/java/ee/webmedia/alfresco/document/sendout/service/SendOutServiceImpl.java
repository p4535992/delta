package ee.webmedia.alfresco.document.sendout.service;

import java.io.IOException;
import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.web.bean.repository.Node;
<<<<<<< HEAD
=======
import org.apache.commons.lang.StringEscapeUtils;
>>>>>>> develop-5.1
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.addressbook.service.AddressbookService;
import ee.webmedia.alfresco.classificator.enums.SendMode;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamicService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.sendout.model.DocumentSendInfo;
import ee.webmedia.alfresco.document.sendout.model.SendInfo;
<<<<<<< HEAD
import ee.webmedia.alfresco.dvk.model.DvkSendLetterDocuments;
import ee.webmedia.alfresco.dvk.model.DvkSendLetterDocumentsImpl;
=======
import ee.webmedia.alfresco.dvk.model.DvkSendDocuments;
>>>>>>> develop-5.1
import ee.webmedia.alfresco.dvk.service.DvkService;
import ee.webmedia.alfresco.email.model.EmailAttachment;
import ee.webmedia.alfresco.email.service.EmailException;
import ee.webmedia.alfresco.email.service.EmailService;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.service.ParametersService;
<<<<<<< HEAD
=======
import ee.webmedia.alfresco.privilege.model.Privilege;
>>>>>>> develop-5.1
import ee.webmedia.alfresco.privilege.service.PrivilegeService;
import ee.webmedia.alfresco.signature.model.SkLdapCertificate;
import ee.webmedia.alfresco.signature.service.SignatureService;
import ee.webmedia.alfresco.signature.service.SkLdapService;
import ee.webmedia.alfresco.user.model.Authority;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.UnableToPerformException;
<<<<<<< HEAD
=======
import ee.webmedia.alfresco.utils.WebUtil;
>>>>>>> develop-5.1
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.sendout.TaskSendInfo;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.Workflow;
import ee.webmedia.xtee.client.dhl.DhlXTeeService.ContentToSend;
import ee.webmedia.xtee.client.dhl.DhlXTeeService.SendStatus;

<<<<<<< HEAD
/**
 * @author Erko Hansar
 */
=======
>>>>>>> develop-5.1
public class SendOutServiceImpl implements SendOutService {

    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(SendOutServiceImpl.class);

    private static final String SAP_ORG_NAME = "SAP";

    private NodeService nodeService;
    private GeneralService generalService;
    private EmailService emailService;
    private AddressbookService addressbookService;
<<<<<<< HEAD
    private DvkService dvkService;
    private ParametersService parametersService;
    private SignatureService signatureService;
=======
    private DvkService _dvkService;
    private ParametersService parametersService;
    private SignatureService _signatureService;
>>>>>>> develop-5.1
    private SkLdapService skLdapService;

    @Override
    public List<SendInfo> getDocumentSendInfos(NodeRef document) {
        List<ChildAssociationRef> assocs = nodeService.getChildAssocs(document, RegexQNamePattern.MATCH_ALL, DocumentCommonModel.Assocs.SEND_INFO);
        List<SendInfo> result = new ArrayList<SendInfo>(assocs.size());
        for (ChildAssociationRef assoc : assocs) {
            result.add(new DocumentSendInfo(generalService.fetchNode(assoc.getChildRef())));
        }
        return result;
    }

    @Override
    public boolean hasDocumentSendInfos(NodeRef document) {
        List<ChildAssociationRef> assocs = nodeService.getChildAssocs(document, RegexQNamePattern.MATCH_ALL, DocumentCommonModel.Assocs.SEND_INFO);
        return !assocs.isEmpty();
    }

    @Override
    public List<SendInfo> getDocumentAndTaskSendInfos(NodeRef document, List<CompoundWorkflow> compoundWorkflows) {
        List<SendInfo> result = getDocumentSendInfos(document);
        for (CompoundWorkflow compoundWorkflow : compoundWorkflows) {
            for (Workflow workflow : compoundWorkflow.getWorkflows()) {
                if (workflow.isType(WorkflowSpecificModel.Types.EXTERNAL_REVIEW_WORKFLOW)) {
                    for (Task task : workflow.getTasks()) {
                        if (task.getProp(WorkflowSpecificModel.Props.SEND_STATUS) != null) {
                            result.add(new TaskSendInfo(task.getNode()));
                        }
                    }
                }
            }
        }
        return result;
    }

    @Override
<<<<<<< HEAD
    public boolean sendOut(NodeRef document, List<String> names, List<String> emails, List<String> modes, List<String> encryptionIdCodes, String fromEmail, String subject,
            String content, List<NodeRef> fileRefs, boolean zipIt) {
=======
    public boolean sendOut(NodeRef document, List<String> names, List<String> emails, List<String> modes, List<String> idCodes, List<String> encryptionIdCodes, String fromEmail,
            String subject, String content, List<NodeRef> fileRefs, boolean zipIt) {
>>>>>>> develop-5.1

        List<X509Certificate> allCertificates = new ArrayList<X509Certificate>();
        if (encryptionIdCodes != null) {
            Set<String> encryptionIdCodesSet = new HashSet<String>();
            for (int i = 0; i < names.size(); i++) {
                String encryptionIdCode = encryptionIdCodes.get(i);
                if (encryptionIdCodesSet.contains(encryptionIdCode)) {
                    continue;
                }
                List<SkLdapCertificate> skLdapCertificates = skLdapService.getCertificates(encryptionIdCode);
<<<<<<< HEAD
                List<X509Certificate> certificates = signatureService.getCertificatesForEncryption(skLdapCertificates);
=======
                List<X509Certificate> certificates = getSignatureService().getCertificatesForEncryption(skLdapCertificates);
>>>>>>> develop-5.1
                if (certificates.isEmpty()) {
                    throw new UnableToPerformException("document_send_out_encryptionRecipient_notFound", names.get(i), encryptionIdCode);
                }
                allCertificates.addAll(certificates);
                encryptionIdCodesSet.add(encryptionIdCode);
            }
        }

        Map<QName, Serializable> docProperties = nodeService.getProperties(document);
        List<Map<QName, Serializable>> sendInfoProps = new ArrayList<Map<QName, Serializable>>();
        Date now = new Date();

        // Collect DVK data
        List<String> toRegNums = new ArrayList<String>();

        // Collect email data
        List<String> toEmails = new ArrayList<String>();
        List<String> toNames = new ArrayList<String>();
        List<String> toBccEmails = new ArrayList<String>();
        List<String> toBccNames = new ArrayList<String>();
<<<<<<< HEAD
=======
        List<String> toDvkOrgNames = new ArrayList<String>();
        List<String> toDvkPersonNames = new ArrayList<String>();
        List<String> toDvkIdCodes = new ArrayList<String>();
>>>>>>> develop-5.1

        // Loop through all recipients, keep a list for DVK sending, a list for email sending and prepare sendInfo properties
        for (int i = 0; i < names.size(); i++) {
            if (StringUtils.isNotBlank(names.get(i)) && StringUtils.isNotBlank(modes.get(i))) {
                String recipientName = names.get(i);
                String recipient = recipientName;
                final String email = emails.get(i);
                if (StringUtils.isNotBlank(email)) {
                    recipient += " (" + email + ")";
                }
                String recipientRegNr = "";
                String sendMode = modes.get(i);
                SendStatus sendStatus = SendStatus.RECEIVED;

                if (SendMode.EMAIL_DVK.equals(modes.get(i))) {
                    // Check if matches a DVK capable organization entry in addressbook
                    boolean hasDvkContact = false;
                    for (Node organization : addressbookService.getDvkCapableOrgs()) {
                        String orgName = (String) organization.getProperties().get(AddressbookModel.Props.ORGANIZATION_NAME.toString());
                        String orgEmail = (String) organization.getProperties().get(AddressbookModel.Props.EMAIL.toString());
                        if (recipientName.equalsIgnoreCase(orgName) && email.equalsIgnoreCase(orgEmail)) {
                            hasDvkContact = true;
                            recipientRegNr = (String) organization.getProperties().get(AddressbookModel.Props.ORGANIZATION_CODE.toString());
<<<<<<< HEAD
=======
                            toDvkOrgNames.add(recipientName);
>>>>>>> develop-5.1
                            break;
                        }
                    }

                    if (hasDvkContact) {
                        toRegNums.add(recipientRegNr);
                        sendMode = SendMode.DVK.getValueName();
                        sendStatus = SendStatus.SENT;
                    } else {
                        toEmails.add(email);
                        toNames.add(recipientName);
                        sendMode = SendMode.EMAIL.getValueName();
                    }
                } else if (SendMode.EMAIL.equals(modes.get(i))) {
                    toEmails.add(email);
                    toNames.add(recipientName);
                } else if (SendMode.EMAIL_BCC.equals(modes.get(i))) {
                    toBccEmails.add(email);
                    toBccNames.add(recipientName);
<<<<<<< HEAD
=======
                } else if (SendMode.STATE_PORTAL_EESTI_EE.equals(modes.get(i))) {
                    toDvkPersonNames.add(recipientName);
                    recipientRegNr = idCodes.get(i);
                    toDvkIdCodes.add(recipientRegNr);
                    sendStatus = SendStatus.SENT;
>>>>>>> develop-5.1
                }

                Map<QName, Serializable> props = new HashMap<QName, Serializable>();
                props.put(DocumentCommonModel.Props.SEND_INFO_RECIPIENT, recipient);
                props.put(DocumentCommonModel.Props.SEND_INFO_RECIPIENT_REG_NR, recipientRegNr);
                props.put(DocumentCommonModel.Props.SEND_INFO_SEND_DATE_TIME, now);
                props.put(DocumentCommonModel.Props.SEND_INFO_SEND_MODE, sendMode);
                props.put(DocumentCommonModel.Props.SEND_INFO_SEND_STATUS, sendStatus.toString());
                props.put(DocumentCommonModel.Props.SEND_INFO_DVK_ID, "");
                props.put(DocumentCommonModel.Props.SEND_INFO_RESOLUTION, content);
                sendInfoProps.add(props);
            }
        }

        // Prepare zip file name if needed
        String zipOrEncryptFileTitle = null;
        if (fileRefs != null && fileRefs.size() > 0 && (zipIt || !allCertificates.isEmpty()) && (toRegNums.size() > 0 || toEmails.size() > 0)) {
            zipOrEncryptFileTitle = buildZipAndEncryptFileTitle(docProperties);
        }

        List<EmailAttachment> attachments = emailService.getAttachments(fileRefs, zipIt, allCertificates, zipOrEncryptFileTitle);

        // Send through DVK
        String dvkId = "";
<<<<<<< HEAD
        if (toRegNums.size() > 0) {
            // Construct DvkSendDocument
            DvkSendLetterDocuments sd = new DvkSendLetterDocumentsImpl();
            sd.setSenderOrgName(parametersService.getStringParameter(Parameters.DVK_ORGANIZATION_NAME));
            sd.setSenderEmail(fromEmail);
            sd.setLetterSenderDocNr((String) docProperties.get(DocumentCommonModel.Props.REG_NUMBER));
            sd.setLetterSenderDocSignDate((Date) docProperties.get(DocumentCommonModel.Props.REG_DATE_TIME));
            sd.setLetterSenderTitle((String) docProperties.get(DocumentCommonModel.Props.DOC_NAME));
            sd.setLetterAccessRestriction((String) docProperties.get(DocumentCommonModel.Props.ACCESS_RESTRICTION));
            sd.setLetterAccessRestrictionBeginDate((Date) docProperties.get(DocumentCommonModel.Props.ACCESS_RESTRICTION_BEGIN_DATE));
            sd.setLetterAccessRestrictionEndDate((Date) docProperties.get(DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DATE));
            sd.setLetterAccessRestrictionReason((String) docProperties.get(DocumentCommonModel.Props.ACCESS_RESTRICTION_REASON));
            String ownerName = (String) docProperties.get(DocumentCommonModel.Props.OWNER_NAME);
            String ownerFirstname = "";
            String ownerSurname = "";
            if (StringUtils.isNotBlank(ownerName)) {
                int lastIndx = ownerName.lastIndexOf(" ");
                if (lastIndx != -1) {
                    ownerFirstname = ownerName.substring(0, lastIndx);
                    ownerSurname = ownerName.substring(lastIndx + 1, ownerName.length());
                } else {
                    ownerFirstname = ownerName;
                }
            }
            sd.setLetterCompilatorFirstname(ownerFirstname);
            sd.setLetterCompilatorSurname(ownerSurname);
            sd.setLetterCompilatorJobTitle((String) docProperties.get(DocumentCommonModel.Props.OWNER_JOB_TITLE));
            DocumentDynamicService documentDynamicService = getDocumentDynamicService();
            if (documentDynamicService.isOutgoingLetter(document)) {
                sd.setDocType("Kiri");
            } else {
                sd.setDocType(documentDynamicService.getDocumentTypeName(document));
            }
            sd.setRecipientsRegNrs(toRegNums);
=======
        if (toRegNums.size() > 0 || toDvkIdCodes.size() > 0) {
            // Construct DvkSendDocument
            DvkSendDocuments sd = new DvkSendDocuments();
            sd.setSenderOrgName(parametersService.getStringParameter(Parameters.DVK_ORGANIZATION_NAME));
            sd.setSenderEmail(fromEmail);
            sd.setRecipientsRegNrs(toRegNums);
            sd.setOrgNames(toDvkOrgNames);
            sd.setPersonIdCodes(toDvkIdCodes);
            sd.setPersonNames(toDvkPersonNames);
            sd.setDocumentNodeRef(document);
            sd.setTextContent(StringEscapeUtils.unescapeHtml(WebUtil.removeHtmlTags(content)));
>>>>>>> develop-5.1

            // Construct content items
            List<ContentToSend> contentsToSend = prepareContents(attachments);

            // Send it out
<<<<<<< HEAD
            dvkId = dvkService.sendLetterDocuments(document, contentsToSend, sd);
=======
            dvkId = getDvkService().sendDocuments(contentsToSend, sd);
>>>>>>> develop-5.1
        }

        // Send through email
        if (!toEmails.isEmpty() || !toBccEmails.isEmpty()) {
            try {
                emailService.sendEmail(toEmails, toNames, toBccEmails, toBccNames, fromEmail, subject, content, true, document, attachments);
            } catch (EmailException e) {
                throw new RuntimeException("Document e-mail sending failed", e);
            }
        }

        // Create the sendInfo nodes under the document
        for (Map<QName, Serializable> props : sendInfoProps) {
<<<<<<< HEAD
            if (SendMode.DVK.getValueName().equalsIgnoreCase((String) props.get(DocumentCommonModel.Props.SEND_INFO_SEND_MODE))) {
=======
            String sendMode = (String) props.get(DocumentCommonModel.Props.SEND_INFO_SEND_MODE);
            if (SendMode.DVK.getValueName().equalsIgnoreCase(sendMode) || SendMode.STATE_PORTAL_EESTI_EE.getValueName().equalsIgnoreCase(sendMode)) {
>>>>>>> develop-5.1
                props.put(DocumentCommonModel.Props.SEND_INFO_DVK_ID, dvkId);
            }

            addSendinfo(document, props);
        }

        return true;
    }

    private String buildZipAndEncryptFileTitle(Map<QName, Serializable> docProperties) {
        SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
        StringBuilder docName = new StringBuilder();
        String regNum = (String) docProperties.get(DocumentCommonModel.Props.REG_NUMBER);
        if (StringUtils.isNotBlank(regNum)) {
            docName.append(regNum);
        }
        Date regDateTime = (Date) docProperties.get(DocumentCommonModel.Props.REG_DATE_TIME);
        if (regDateTime != null) {
            if (docName.length() > 0) {
                docName.append(" ");
            }
            docName.append(format.format(regDateTime));
        }
        if (docName.length() == 0) {
            docName.append("dokument");
        }
        return docName.toString();
    }

    @Override
<<<<<<< HEAD
    public void sendDocumentForInformation(List<String> authorityIds, Node docNode, String emailTemplate) {
=======
    public void sendForInformation(List<String> authorityIds, Node docNode, String emailTemplate, String subject, String content) {
>>>>>>> develop-5.1
        List<Authority> authorities = new ArrayList<Authority>();
        PrivilegeService privilegeService = BeanHelper.getPrivilegeService();
        UserService userService = BeanHelper.getUserService();
        NodeRef docRef = docNode.getNodeRef();
<<<<<<< HEAD
        Set<String> privilegesToAdd = new HashSet<String>(Arrays.asList(DocumentCommonModel.Privileges.VIEW_DOCUMENT_META_DATA,
                DocumentCommonModel.Privileges.VIEW_DOCUMENT_FILES));
=======
        Set<Privilege> privilegesToAdd = new HashSet<Privilege>(Arrays.asList(Privilege.VIEW_DOCUMENT_META_DATA,
                Privilege.VIEW_DOCUMENT_FILES));
>>>>>>> develop-5.1
        for (String authorityId : authorityIds) {
            Authority authority = userService.getAuthorityOrNull(authorityId);
            if (authority == null) {
                continue;
            }
            authorities.add(authority);
            String authorityStr = authority.getAuthority();
<<<<<<< HEAD
            if (!privilegeService.hasPermissionOnAuthority(docRef, authorityStr, DocumentCommonModel.Privileges.VIEW_DOCUMENT_META_DATA,
                    DocumentCommonModel.Privileges.VIEW_DOCUMENT_FILES)) {
                privilegeService.setPermissions(docRef, authorityStr, privilegesToAdd);
            }
        }
        BeanHelper.getNotificationService().sendDocumentForInformationNotification(authorities, docNode, emailTemplate);
=======
            if (!privilegeService.hasPermissionOnAuthority(docRef, authorityStr, Privilege.VIEW_DOCUMENT_META_DATA, Privilege.VIEW_DOCUMENT_FILES)) {
                privilegeService.setPermissions(docRef, authorityStr, privilegesToAdd);
            }
        }
        BeanHelper.getNotificationService().sendForInformationNotification(authorities, docNode, emailTemplate, subject, content);
>>>>>>> develop-5.1
    }

    @Override
    public NodeRef addSendinfo(NodeRef document, Map<QName, Serializable> props) {
<<<<<<< HEAD
        final NodeRef sendInfoRef = nodeService.createNode(document, //
                DocumentCommonModel.Assocs.SEND_INFO, DocumentCommonModel.Assocs.SEND_INFO, DocumentCommonModel.Types.SEND_INFO, props).getChildRef();
        log.debug("created new sendInfo '" + sendInfoRef + "' for sent document '" + document + "'");
        updateSearchableSendMode(document);
=======
        return addSendinfo(document, props, true);
    }

    @Override
    public NodeRef addSendinfo(NodeRef document, Map<QName, Serializable> props, boolean updateSearchableSendInfo) {
        final NodeRef sendInfoRef = nodeService.createNode(document,
                DocumentCommonModel.Assocs.SEND_INFO, DocumentCommonModel.Assocs.SEND_INFO, DocumentCommonModel.Types.SEND_INFO, props).getChildRef();
        log.debug("created new sendInfo '" + sendInfoRef + "' for sent document '" + document + "'");
        if (updateSearchableSendInfo) {
            updateSearchableSendInfo(document);
        }
>>>>>>> develop-5.1
        return sendInfoRef;
    }

    @Override
<<<<<<< HEAD
    public void updateSearchableSendMode(NodeRef document) {
        ArrayList<String> sendModes = buildSearchableSendMode(document);
        nodeService.setProperty(document, DocumentCommonModel.Props.SEARCHABLE_SEND_MODE, sendModes);
    }

    @Override
    public ArrayList<String> buildSearchableSendMode(NodeRef document) {
        List<SendInfo> sendInfos = getDocumentSendInfos(document);
        ArrayList<String> sendModes = new ArrayList<String>(sendInfos.size());
        for (SendInfo sendInfo : sendInfos) {
            sendModes.add(sendInfo.getSendMode());
        }
        return sendModes;
=======
    public void updateSearchableSendInfo(NodeRef document) {
        nodeService.addProperties(document, buildSearchableSendInfo(document));
    }

    @Override
    public Map<QName, Serializable> buildSearchableSendInfo(NodeRef document) {
        List<SendInfo> sendInfos = getDocumentSendInfos(document);
        int size = sendInfos.size();
        ArrayList<String> sendModes = new ArrayList<String>(size);
        ArrayList<String> sendRecipients = new ArrayList<String>(size);
        ArrayList<Date> sendTimes = new ArrayList<Date>(size);
        ArrayList<String> sendResolutions = new ArrayList<String>(size);
        for (SendInfo sendInfo : sendInfos) {
            sendModes.add(sendInfo.getSendMode());
            sendRecipients.add(sendInfo.getRecipient());
            sendTimes.add(sendInfo.getSendDateTime());
            sendResolutions.add(sendInfo.getResolution());
        }

        Map<QName, Serializable> props = new HashMap<QName, Serializable>();
        props.put(DocumentCommonModel.Props.SEARCHABLE_SEND_MODE, sendModes);
        props.put(DocumentCommonModel.Props.SEARCHABLE_SEND_INFO_RECIPIENT, sendRecipients);
        props.put(DocumentCommonModel.Props.SEARCHABLE_SEND_INFO_SEND_DATE_TIME, sendTimes);
        props.put(DocumentCommonModel.Props.SEARCHABLE_SEND_INFO_RESOLUTION, sendResolutions);

        return props;
>>>>>>> develop-5.1
    }

    @Override
    public List<ContentToSend> prepareContents(NodeRef document, List<NodeRef> fileRefs, boolean zipIt) {
        String zipAndEncryptFileName = null;
        if (fileRefs != null && fileRefs.size() > 0 && zipIt) {
            Map<QName, Serializable> docProperties = nodeService.getProperties(document);
            zipAndEncryptFileName = buildZipAndEncryptFileTitle(docProperties);
        }
        List<EmailAttachment> attachments = emailService.getAttachments(fileRefs, zipIt, null, zipAndEncryptFileName);
        return prepareContents(attachments);
    }

    private List<ContentToSend> prepareContents(List<EmailAttachment> attachments) {
        List<ContentToSend> result = new ArrayList<ContentToSend>();
        for (EmailAttachment attachment : attachments) {
            ContentToSend content = new ContentToSend();
            content.setFileName(attachment.getFileName());
            content.setMimeType(attachment.getMimeType());
<<<<<<< HEAD
=======
            content.setId(attachment.getFileNodeRef().getId());
>>>>>>> develop-5.1
            try {
                content.setInputStream(attachment.getInputStreamSource().getInputStream());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            result.add(content);
        }
        return result;
    }

    // START: getters / setters
    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setEmailService(EmailService emailService) {
        this.emailService = emailService;
    }

    public void setAddressbookService(AddressbookService addressbookService) {
        this.addressbookService = addressbookService;
    }

<<<<<<< HEAD
    public void setDvkService(DvkService dvkService) {
        this.dvkService = dvkService;
    }

=======
>>>>>>> develop-5.1
    public void setParametersService(ParametersService parametersService) {
        this.parametersService = parametersService;
    }

    /**
     * Dependency cicle: dvkService -> documentService -> sendOutService -> documentDynamicService -> documentService
     */
    public DocumentDynamicService getDocumentDynamicService() {
        return BeanHelper.getDocumentDynamicService();
    }

<<<<<<< HEAD
    public void setSignatureService(SignatureService signatureService) {
        this.signatureService = signatureService;
    }

=======
>>>>>>> develop-5.1
    public void setSkLdapService(SkLdapService skLdapService) {
        this.skLdapService = skLdapService;
    }

    @Override
    public void addSapSendInfo(Node document, String dvkId) {
        Map<QName, Serializable> props = new HashMap<QName, Serializable>();
        props.put(DocumentCommonModel.Props.SEND_INFO_RECIPIENT, SAP_ORG_NAME);
        props.put(DocumentCommonModel.Props.SEND_INFO_RECIPIENT_REG_NR, parametersService.getStringParameter(Parameters.SAP_DVK_CODE));
        props.put(DocumentCommonModel.Props.SEND_INFO_SEND_DATE_TIME, new Date());
        props.put(DocumentCommonModel.Props.SEND_INFO_SEND_MODE, SendMode.DVK.getValueName());
        props.put(DocumentCommonModel.Props.SEND_INFO_SEND_STATUS, SendStatus.SENT.toString());
        props.put(DocumentCommonModel.Props.SEND_INFO_DVK_ID, dvkId);
        addSendinfo(document.getNodeRef(), props);
    }

<<<<<<< HEAD
=======
    private DvkService getDvkService() {
        if (_dvkService == null) {
            _dvkService = BeanHelper.getDvkService();
        }
        return _dvkService;
    }

    private SignatureService getSignatureService() {
        if (_signatureService == null) {
            _signatureService = BeanHelper.getSignatureService();
        }
        return _signatureService;
    }

>>>>>>> develop-5.1
    // END: getters / setters

}
