package ee.webmedia.alfresco.smtp;

import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.alfresco.email.server.handler.AbstractForumEmailMessageHandler;
import org.alfresco.i18n.I18NUtil;
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
import ee.webmedia.alfresco.ocr.service.OcrService;
import ee.webmedia.alfresco.user.service.UserService;

/**
 * Handler to handle scanned emails.
 */
public class ScannedEmailHandler extends AbstractForumEmailMessageHandler {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(ScannedEmailHandler.class);
    private UserService userService;
    private GeneralService generalService;
    private OcrService ocrService;

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
                final FileNameSplitter fileNameSplitter = new FileNameSplitter(fileName);
                final String userIdCode = fileNameSplitter.getSSN().toString();
                if (!userService.isDocumentManager(userIdCode)) {
                    log.info("User '" + userIdCode + "' is not a documentManager, not adding attachments!");
                    continue;
                }
                AuthenticationUtil.runAs(new RunAsWork<NodeRef>() {
                    @Override
                    public NodeRef doWork() throws Exception {
                        AuthenticationUtil.runAs(new RunAsWork<NodeRef>() {
                            @Override
                            public NodeRef doWork() throws Exception {
                                final String originalFullyAuthenticatedUser = AuthenticationUtil.getFullyAuthenticatedUser();
                                // set authenticated user based on username whom this attachement is sent
                                AuthenticationUtil.setFullyAuthenticatedUser(userIdCode);
                                // ... but set runAsUser system to get rights to create childNodes
                                AuthenticationUtil.setRunAsUser(AuthenticationUtil.getSystemUserName());
                                // process message so that creator of attachement file will be set based on userIdCode, but also having rights to create
                                // childNodes, that is otherwise granted only for EMAIL_CONTRIBUTORS group
                                NodeRef personalFolderRef = getPersonalFolderRef(contentNodeRef, userIdCode);
                                addAttachment(personalFolderRef, fileNameSplitter, attachment, userIdCode);
                                // restore original fullyAuthenticatedUser
                                AuthenticationUtil.setFullyAuthenticatedUser(originalFullyAuthenticatedUser);
                                return null;
                            }
                        }, AuthenticationUtil.getSystemUserName());
                        return null;
                    }
                }, userIdCode);
                counter++;
            } catch (IllegalArgumentException e) {
                // just ignore files, that don't have valid SocalSecurityNumber
                log.info(e.getMessage());
            }
        }
        log.info("saved " + counter + "/" + attachments.length + " attachments");
    }

    private void addAttachment(NodeRef spaceNodeRef, FileNameSplitter fileNameSplitter, EmailMessagePart attachment, String userIdCode) {
        String fileName = fileNameSplitter.getFileNameWithoutSSN().trim();
        QName assocType = ContentModel.ASSOC_CONTAINS;
        final NodeService nodeService = getNodeService();
        fileName = generalService.getUniqueFileName(spaceNodeRef, fileName);
        Map<QName, Serializable> contentProps = new HashMap<QName, Serializable>();
        contentProps.put(ContentModel.PROP_NAME, fileName);
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
        NodeRef spaceNodeRef = getNodeService().getChildByName(contentNodeRef, SCANNED_BY_PERSONS_ASSOC, userIdCode);
        if (spaceNodeRef == null) {
            final HashMap<QName, Serializable> props = new HashMap<QName, Serializable>();
            props.put(ContentModel.PROP_NAME, userIdCode);
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

    /**
     * Helper that splits SocalSecurityNumber from rest of the file name
     */
    private static class FileNameSplitter {
        private static final Pattern fileNamePattern = Pattern.compile("(\\d{11})\\w*(.*)");
        private final Long ssn;
        private final String fileNameWithoutSSN;

        /**
         * @param wholeFileName
         * @throws IllegalArgumentException - if <code>wholeFileName</code> doesn't contain SocalSecurityNumber
         */
        public FileNameSplitter(String wholeFileName) {
            if (StringUtils.isBlank(wholeFileName)) {
                throw new RuntimeException("empty fileName");
            }
            Matcher matcher = fileNamePattern.matcher(wholeFileName.trim());
            if (matcher.find()) {
                final String strSSN = matcher.group(1);
                ssn = Long.valueOf(strSSN);
                String baseName = matcher.group(2);
                if (StringUtils.isBlank(baseName)) {
                    baseName = I18NUtil.getMessage("imap.letter_subject_missing");
                }
                fileNameWithoutSSN = baseName;
            } else {
                throw new IllegalArgumentException("FileName '" + wholeFileName + "' doesn't match pattern '" + fileNamePattern + "'");
            }
        }

        public Long getSSN() {
            return ssn;
        }

        public String getFileNameWithoutSSN() {
            return fileNameWithoutSSN;
        }
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
    // END: getters / setters
}
