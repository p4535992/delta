package ee.webmedia.alfresco.document.sendout.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.addressbook.service.AddressbookService;
import ee.webmedia.alfresco.classificator.enums.SendMode;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.sendout.model.DocumentSendInfo;
import ee.webmedia.alfresco.document.sendout.model.SendInfo;
import ee.webmedia.alfresco.document.type.service.DocumentTypeHelper;
import ee.webmedia.alfresco.document.type.service.DocumentTypeService;
import ee.webmedia.alfresco.dvk.model.DvkSendLetterDocuments;
import ee.webmedia.alfresco.dvk.model.DvkSendLetterDocumentsImpl;
import ee.webmedia.alfresco.dvk.service.DvkService;
import ee.webmedia.alfresco.email.service.EmailException;
import ee.webmedia.alfresco.email.service.EmailService;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.service.ParametersService;
import ee.webmedia.alfresco.utils.FilenameUtil;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.sendout.TaskSendInfo;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.Workflow;
import ee.webmedia.alfresco.workflow.service.WorkflowService;
import ee.webmedia.xtee.client.dhl.DhlXTeeService.ContentToSend;
import ee.webmedia.xtee.client.dhl.DhlXTeeService.SendStatus;

/**
 * @author Erko Hansar
 */
public class SendOutServiceImpl implements SendOutService {

    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(SendOutServiceImpl.class);

    private static final String SAP_ORG_NAME = "SAP";

    private NodeService nodeService;
    private GeneralService generalService;
    private EmailService emailService;
    private AddressbookService addressbookService;
    private DvkService dvkService;
    private ParametersService parametersService;
    private DocumentTypeService documentTypeService;
    private FileFolderService fileFolderService;
    private WorkflowService workflowService;

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
    public boolean sendOut(NodeRef document, List<String> names, List<String> emails, List<String> modes, String fromEmail, String subject, String content,
            List<String> fileNodeRefs, boolean zipIt) {

        QName docType = nodeService.getType(document);
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
        String zipFileName = null;
        if (fileNodeRefs != null && fileNodeRefs.size() > 0 && zipIt && (toRegNums.size() > 0 || toEmails.size() > 0)) {
            zipFileName = buildZipFileName(docProperties);
        }

        // Send through DVK
        String dvkId = "";
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
            if (DocumentTypeHelper.isOutgoingLetter(docType)) {
                sd.setDocType("Kiri");
            } else {
                sd.setDocType(documentTypeService.getDocumentType(docType).getName());
            }
            sd.setRecipientsRegNrs(toRegNums);

            // Construct content items
            List<ContentToSend> contentsToSend = prepareContents(document, fileNodeRefs, zipIt, zipFileName);

            // Send it out
            dvkId = dvkService.sendLetterDocuments(document, contentsToSend, sd);
        }

        // Send through email
        if (!toEmails.isEmpty() || !toBccEmails.isEmpty()) {
            try {
                emailService.sendEmail(toEmails, toNames, toBccEmails, toBccNames, fromEmail, subject, content, true, document, fileNodeRefs, zipIt,
                        zipFileName);
            } catch (EmailException e) {
                throw new RuntimeException("Document e-mail sending failed", e);
            }
        }

        // Create the sendInfo nodes under the document
        for (Map<QName, Serializable> props : sendInfoProps) {
            if (SendMode.DVK.getValueName().equalsIgnoreCase((String) props.get(DocumentCommonModel.Props.SEND_INFO_SEND_MODE))) {
                props.put(DocumentCommonModel.Props.SEND_INFO_DVK_ID, dvkId);
            }

            addSendinfo(document, props);
        }

        return true;
    }

    @Override
    public String buildZipFileName(Map<QName, Serializable> docProperties) {
        String zipFileName;
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
        zipFileName = FilenameUtil.buildFileName(docName.toString(), "zip");
        return zipFileName;
    }

    @Override
    public NodeRef addSendinfo(NodeRef document, Map<QName, Serializable> props) {
        final NodeRef sendInfoRef = nodeService.createNode(document, //
                DocumentCommonModel.Assocs.SEND_INFO, DocumentCommonModel.Assocs.SEND_INFO, DocumentCommonModel.Types.SEND_INFO, props).getChildRef();
        log.debug("created new sendInfo '" + sendInfoRef + "' for sent document '" + document + "'");
        updateSearchableSendMode(document);
        return sendInfoRef;
    }

    @Override
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
    }

    @Override
    public List<ContentToSend> prepareContents(NodeRef document, List<String> fileNodeRefs, boolean zipIt, String zipFileName) {
        List<ContentToSend> result = new ArrayList<ContentToSend>();
        if (fileNodeRefs == null || fileNodeRefs.size() == 0) {
            return result;
        }
        if (zipIt) {
            ByteArrayOutputStream byteStream = generalService.getZipFileFromFiles(document, fileNodeRefs);
            ContentToSend content = new ContentToSend();
            content.setFileName(zipFileName);
            content.setMimeType(MimetypeMap.MIMETYPE_ZIP);
            byte[] byteArray = byteStream.toByteArray();
            content.setInputStream(new ByteArrayInputStream(byteArray));
            result.add(content);
        } else {
            for (FileInfo fileInfo : fileFolderService.listFiles(document)) {
                if (fileNodeRefs.contains(fileInfo.getNodeRef().toString())) {
                    ContentReader reader = fileFolderService.getReader(fileInfo.getNodeRef());
                    ContentToSend content = new ContentToSend();
                    content.setFileName(fileInfo.getName());
                    content.setMimeType(reader.getMimetype());
                    // Instead of directly setting reader.getContentInputStream into ContentToSend, we read the bytes
                    // and set a new ByteArrayInputStream to avoid tight coupling between ContentToSend and ContentReader
                    // input stream life-cycle.
                    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                    reader.getContent(byteStream);
                    byte[] byteArray = byteStream.toByteArray();
                    content.setInputStream(new ByteArrayInputStream(byteArray));
                    result.add(content);
                }
            }
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

    public void setDvkService(DvkService dvkService) {
        this.dvkService = dvkService;
    }

    public void setParametersService(ParametersService parametersService) {
        this.parametersService = parametersService;
    }

    public void setDocumentTypeService(DocumentTypeService documentTypeService) {
        this.documentTypeService = documentTypeService;
    }

    public void setFileFolderService(FileFolderService fileFolderService) {
        this.fileFolderService = fileFolderService;
    }

    public void setWorkflowService(WorkflowService workflowService) {
        this.workflowService = workflowService;
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

    // END: getters / setters

}
