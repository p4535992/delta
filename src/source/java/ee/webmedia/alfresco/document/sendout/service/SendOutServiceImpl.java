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
import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;
import ee.webmedia.alfresco.document.sendout.model.SendInfo;
import ee.webmedia.alfresco.document.type.service.DocumentTypeService;
import ee.webmedia.alfresco.dvk.model.DvkSendDocuments;
import ee.webmedia.alfresco.dvk.model.DvkSendDocumentsImpl;
import ee.webmedia.alfresco.dvk.service.DvkService;
import ee.webmedia.alfresco.email.service.EmailException;
import ee.webmedia.alfresco.email.service.EmailService;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.service.ParametersService;
import ee.webmedia.alfresco.utils.FilenameUtil;
import ee.webmedia.xtee.client.service.DhlXTeeService.ContentToSend;
import ee.webmedia.xtee.client.service.DhlXTeeService.SendStatus;

/**
 * @author Erko Hansar
 */
public class SendOutServiceImpl implements SendOutService {

    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(SendOutServiceImpl.class);

    private NodeService nodeService;
    private GeneralService generalService;
    private EmailService emailService;
    private AddressbookService addressbookService;
    private DvkService dvkService;
    private ParametersService parametersService;
    private DocumentTypeService documentTypeService;
    private FileFolderService fileFolderService;

    @Override
    public List<SendInfo> getSendInfos(NodeRef document) {
        List<ChildAssociationRef> assocs = nodeService.getChildAssocs(document, RegexQNamePattern.MATCH_ALL, DocumentCommonModel.Assocs.SEND_INFO);
        List<SendInfo> result = new ArrayList<SendInfo>(assocs.size());
        for (ChildAssociationRef assoc : assocs) {
            result.add(new SendInfo(generalService.fetchNode(assoc.getChildRef())));
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
        SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");

        // Collect DVK data
        List<String> toRegNums = new ArrayList<String>();
        List<Node> dvkCapableOrgs = new ArrayList<Node>();
        for (Node organization : addressbookService.listOrganization()) {
            Object dvkObj = organization.getProperties().get(AddressbookModel.Props.DVK_CAPABLE);
            if (dvkObj != null && (Boolean)dvkObj) {
                dvkCapableOrgs.add(organization);
            }
        }

        // Collect email data
        List<String> toEmails = new ArrayList<String>();
        List<String> toNames = new ArrayList<String>();

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
                    for (Node organization : dvkCapableOrgs) {
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
                        sendMode = SEND_MODE_DVK;
                        sendStatus = SendStatus.SENT;
                    } else {
                        toEmails.add(email);
                        toNames.add(recipientName);
                        sendMode = SendMode.EMAIL.getValueName();
                    }
                } else if (SendMode.EMAIL.equals(modes.get(i))) {
                    toEmails.add(email);
                    toNames.add(recipientName);
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
        }
        
        // Send through DVK
        String dvkId = "";
        if (toRegNums.size() > 0) {
            // Construct DvkSendDocument
            DvkSendDocuments sd = new DvkSendDocumentsImpl();
            sd.setSenderOrgName(parametersService.getStringParameter(Parameters.DVK_ORGANIZATION_NAME));
            sd.setSenderRegNr(parametersService.getStringParameter(Parameters.DVK_ORGANIZATION_REG_NUM));
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
            if (docType.equals(DocumentSubtypeModel.Types.OUTGOING_LETTER)) {
                sd.setDocType("Kiri");
            } else {
                sd.setDocType(documentTypeService.getDocumentType(docType).getName());
            }
            sd.setRecipientsRegNrs(toRegNums);

            // Construct content items
            List<ContentToSend> contentsToSend = prepareContents(document, fileNodeRefs, zipIt, zipFileName);

            // Send it out
            dvkId = dvkService.sendDocuments(document, contentsToSend, sd);
        }

        // Send through email
        if (toEmails.size() > 0) {
            try {
                emailService.sendEmail(toEmails, toNames, fromEmail, subject, content, true, document, fileNodeRefs, zipIt, zipFileName);
            } catch (EmailException e) {
                throw new RuntimeException("Document e-mail sending failed", e);
            }
        }

        // Create the sendInfo nodes under the document
        for (Map<QName, Serializable> props : sendInfoProps) {
            if (((String) props.get(DocumentCommonModel.Props.SEND_INFO_SEND_MODE)).equals(SEND_MODE_DVK)) {
                props.put(DocumentCommonModel.Props.SEND_INFO_DVK_ID, dvkId);
            }

            final NodeRef sendInfoRef = nodeService.createNode(document, //
                    DocumentCommonModel.Assocs.SEND_INFO, DocumentCommonModel.Assocs.SEND_INFO, DocumentCommonModel.Types.SEND_INFO, props).getChildRef();
            log.debug("created new sendInfo '" + sendInfoRef + "' for sent document '" + document + "'");
            updateSearchableSendMode(document);
        }
        
        return true;
    }

    ///// PRIVATE METHODS

    private void updateSearchableSendMode(NodeRef document) {
        List<SendInfo> sendInfos = getSendInfos(document);
        ArrayList<String> sendModes = new ArrayList<String>(sendInfos.size());
        for (SendInfo sendInfo : sendInfos) {
            sendModes.add((String) sendInfo.getSendMode());
        }
        nodeService.setProperty(document, DocumentCommonModel.Props.SEARCHABLE_SEND_MODE, sendModes);
    }

    private List<ContentToSend> prepareContents(NodeRef document, List<String> fileNodeRefs, boolean zipIt, String zipFileName) {
        List<ContentToSend> result = new ArrayList<ContentToSend>(); 
        if (fileNodeRefs == null || fileNodeRefs.size() == 0) {
            return result;
        }
        if (zipIt) {
            ByteArrayOutputStream byteStream = generalService.getZipFileFromFiles(document, fileNodeRefs);
            ContentToSend content = new ContentToSend();
            content.setFileName(zipFileName);
            content.setMimeType(MimetypeMap.MIMETYPE_ZIP);
            content.setInputStream(new ByteArrayInputStream(byteStream.toByteArray()));
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
                    content.setInputStream(new ByteArrayInputStream(byteStream.toByteArray()));
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

    // END: getters / setters

}
