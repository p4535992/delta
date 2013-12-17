package ee.webmedia.alfresco.smtp;

import java.util.Map;

import org.alfresco.email.server.EmailServiceImpl;
import org.alfresco.email.server.handler.EmailMessageHandler;
import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.node.integrity.IntegrityException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.email.EmailMessage;
import org.alfresco.service.cmr.email.EmailMessageException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.service.GeneralService;

/**
 * Email service implementation, that selects target folder based on emailPrefixToFolderMap
 * 
 * @author Ats Uiboupin
 */
public class DeltaEmailServiceImpl extends EmailServiceImpl {

    private GeneralService generalService;
    /**
     * Mappings for email address local part to repository location(XPath)
     */
    private Map<String, String> emailPrefixToFolderMap;

    @Override
    public void importMessage(final EmailMessage message) {
        importMessage(null, message);
    }

    @Override
    public void importMessage(final NodeRef nodeRef, final EmailMessage message) {
        if (!emailInboundEnabled) {
            throw new EmailMessageException(ERR_INBOUND_EMAIL_DISABLED);
        }
        try {
            // Process the message using the username's account
            final RetryingTransactionCallback<Object> processMessageCallback = new RetryingTransactionCallback<Object>() {
                @Override
                public Object execute() throws Throwable {
                    final NodeRef targetNodeRef;
                    if (nodeRef == null) {
                        String recipient = message.getTo();
                        targetNodeRef = getTargetNodeByRecipient(recipient);
                        if (targetNodeRef == null) {
                            DeltaEmailServiceImpl.super.importMessage(nodeRef, message);
                            return null;
                        }
                    } else {
                        targetNodeRef = nodeRef;
                    }
                    EmailMessageHandler messageHandler = getMessageHandler(targetNodeRef);
                    messageHandler.processMessage(targetNodeRef, message);
                    return null;
                }
            };
            RunAsWork<Object> processMessageRunAsWork = new RunAsWork<Object>() {
                @Override
                public Object doWork() throws Exception {
                    return retryingTransactionHelper.doInTransaction(processMessageCallback, false);
                }
            };
            AuthenticationUtil.runAs(processMessageRunAsWork, AuthenticationUtil.getSystemUserName());
        } catch (EmailMessageException e) {
            // These are email-specific errors
            throw e;
        } catch (AccessDeniedException e) {
            throw new EmailMessageException(ERR_ACCESS_DENIED, message.getFrom(), message.getTo());
        } catch (IntegrityException e) {
            throw new EmailMessageException(ERR_INVALID_SUBJECT);
        } catch (Throwable e) {
            throw new AlfrescoRuntimeException("Email message processing failed", e);
        }
    }

    /**
     * @return Target node based on recipient emailAddress local part or null
     */
    private NodeRef getTargetNodeByRecipient(String recipient) {
        final int atIndex = recipient.indexOf("@");
        final String target = recipient.substring(0, atIndex);
        NodeRef targetRef = null;
        final String targetXPath = emailPrefixToFolderMap.get(target);
        if (targetXPath == null) {
            return null;
        }
        targetRef = generalService.getNodeRef(targetXPath);
        Assert.notNull(targetRef, "Target node reference not found for recipient '" + recipient + "'");
        return targetRef;
    }

    // START: getters / setters
    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setEmailPrefixToFolderMap(Map<String, String> emailPrefixToFolderMap) {
        this.emailPrefixToFolderMap = emailPrefixToFolderMap;
    }

    // END: getters / setters
}
