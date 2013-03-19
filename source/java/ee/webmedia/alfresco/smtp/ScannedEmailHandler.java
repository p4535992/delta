package ee.webmedia.alfresco.smtp;

import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.alfresco.email.server.handler.AbstractForumEmailMessageHandler;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.repo.transaction.TransactionListenerAdapter;
import org.alfresco.service.cmr.email.EmailMessage;
import org.alfresco.service.cmr.email.EmailMessagePart;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.document.scanned.model.ScannedModel;
import ee.webmedia.alfresco.ocr.service.OcrService;
import ee.webmedia.alfresco.user.service.UserService;

/**
 * Handler to handle scanned emails.
 * 
 * @author Ats Uiboupin
 */
public class ScannedEmailHandler extends AbstractForumEmailMessageHandler {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(ScannedEmailHandler.class);
    private static final Pattern fileNamePattern = Pattern.compile("(\\d{11})\\w*(.*)");
    private UserService userService;
    private GeneralService generalService;
    private OcrService ocrService;
    private FileService fileService;

    @Override
    public void processMessage(NodeRef contentNodeRef, EmailMessage message) {
        if (log.isDebugEnabled()) {
            log.info("Processing message '" + message.getSubject() + "' from '" + message.getFrom() + "' sent at '" + message.getSentDate() + "' to '"
                    + message.getTo() + "'");
        }
        addAttachments(contentNodeRef, message);
    }

    protected void addAttachments(final NodeRef contentNodeRef, EmailMessage message) {
        EmailMessagePart[] attachments = message.getAttachments();
        int counter = 0;
        for (final EmailMessagePart attachment : attachments) {
            final String fileName = attachment.getFileName();
            log.debug("Processing attachment '" + fileName + "'");
            try {
                final String userIdCode = getSSNFromFilename(fileName);
                if (userIdCode != null && !userService.isDocumentManager(userIdCode)) {
                    log.info("User '" + userIdCode + "' is not a documentManager, not adding attachments!");
                    continue;
                }
                final String systemUserName = AuthenticationUtil.getSystemUserName();
                AuthenticationUtil.runAs(new RunAsWork<NodeRef>() {
                    @Override
                    public NodeRef doWork() throws Exception {
                        AuthenticationUtil.runAs(new RunAsWork<NodeRef>() {
                            @Override
                            public NodeRef doWork() throws Exception {
                                final String originalFullyAuthenticatedUser = AuthenticationUtil.getFullyAuthenticatedUser();
                                // set authenticated user based on username whom this attachement is sent or system if user could not be specified
                                if (userIdCode != null) {
                                    AuthenticationUtil.setFullyAuthenticatedUser(userIdCode);
                                } else {
                                    AuthenticationUtil.setFullyAuthenticatedUser(systemUserName);
                                }
                                // ... but set runAsUser system to get rights to create childNodes
                                AuthenticationUtil.setRunAsUser(systemUserName);
                                // process message so that creator of attachement file will be set based on userIdCode, but also having rights to create
                                // childNodes, that is otherwise granted only for EMAIL_CONTRIBUTORS group
                                NodeRef personalFolderRef = null;
                                if (userIdCode != null) {
                                    personalFolderRef = getPersonalFolderRef(contentNodeRef, userIdCode);
                                } else {
                                    personalFolderRef = generalService.getNodeRef(ScannedModel.Repo.SCANNED_SPACE);
                                }
                                addAttachment(personalFolderRef, fileName, attachment);
                                // restore original fullyAuthenticatedUser
                                AuthenticationUtil.setFullyAuthenticatedUser(originalFullyAuthenticatedUser);
                                return null;
                            }
                        }, systemUserName);
                        return null;
                    }
                }, userIdCode != null ? userIdCode : systemUserName);
                counter++;
            } catch (IllegalArgumentException e) {
                // just ignore files, that don't have valid SocalSecurityNumber
                log.info(e.getMessage());
            }
        }
        log.info("saved " + counter + "/" + attachments.length + " attachments");
    }

    private void addAttachment(NodeRef spaceNodeRef, String fileName, EmailMessagePart attachment) {
        QName assocType = ContentModel.ASSOC_CONTAINS;
        final NodeService nodeService = getNodeService();
        Map<QName, Serializable> contentProps = new HashMap<QName, Serializable>();
        contentProps.put(ContentModel.PROP_NAME, generalService.getUniqueFileName(spaceNodeRef, QName.createValidLocalName(fileName.trim())));
        final QName assocQName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, fileName);
        ChildAssociationRef associationRef = nodeService.createNode(
                spaceNodeRef,
                assocType,
                assocQName,
                ContentModel.TYPE_CONTENT,
                contentProps);

        InputStream contentIs = attachment.getContent();
        String mimetype = getMimetypeService().guessMimetype(fileName);
        String encoding = attachment.getEncoding();
        final NodeRef fileNodeRef = associationRef.getChildRef();
        writeContent(fileNodeRef, contentIs, mimetype, encoding);

        AlfrescoTransactionSupport.bindListener(new TransactionListenerAdapter() {
            @Override
            public void afterCommit() {
                ocrService.queueOcr(fileNodeRef);
            }
        });
    }

    private NodeRef getPersonalFolderRef(NodeRef contentNodeRef, String userIdCode) {
        final QName SCANNED_BY_PERSONS_ASSOC = ContentModel.ASSOC_CONTAINS;
        NodeRef spaceNodeRef = fileService.findSubfolderWithName(contentNodeRef, userIdCode, ContentModel.TYPE_FOLDER);
        if (spaceNodeRef == null) {
            final HashMap<QName, Serializable> props = new HashMap<QName, Serializable>();
            props.put(ContentModel.PROP_NAME, userService.getUserFullName(userIdCode));
            ChildAssociationRef association = getNodeService().createNode(
                    contentNodeRef,
                    SCANNED_BY_PERSONS_ASSOC,
                    QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, userIdCode),
                    ContentModel.TYPE_FOLDER,
                    props
                    );
            spaceNodeRef = association.getChildRef();
        }
        return spaceNodeRef;
    }

    public String getSSNFromFilename(String wholeFileName) {
        if (StringUtils.isBlank(wholeFileName)) {
            throw new RuntimeException("empty fileName");
        }
        Matcher matcher = fileNamePattern.matcher(wholeFileName.trim());
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    // START: getters / setters
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setOcrService(OcrService ocrService) {
        this.ocrService = ocrService;
    }

    public void setFileService(FileService fileService) {
        this.fileService = fileService;
    }
    // END: getters / setters
}
