package ee.webmedia.alfresco.imap;

import java.util.Collection;
import java.util.List;

import org.alfresco.repo.imap.AlfrescoImapConst;
import org.alfresco.repo.imap.AlfrescoImapUser;

import com.icegreen.greenmail.imap.AuthorizationException;
import com.icegreen.greenmail.imap.ImapHostManager;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.store.MailFolder;
import com.icegreen.greenmail.user.GreenMailUser;

import ee.webmedia.alfresco.imap.service.ImapServiceExt;

/**
 * TODO: add comment
 */
public class ImmutableImapHostManager implements ImapHostManager {
    private ImapServiceExt imapServiceExt;

    @Override
    public List getAllMessages() {
        throw new UnsupportedOperationException();
    }

    @Override
    public char getHierarchyDelimiter() {
        return AlfrescoImapConst.HIERARCHY_DELIMITER;
    }

    @Override
    public MailFolder getFolder(GreenMailUser user, String folderName) {
        return imapServiceExt.getFolder(getAlfrescoImapUser(user), folderName);
    }

    @Override
    public MailFolder getFolder(GreenMailUser user, String folderName, boolean mustExist) throws FolderException {
        return imapServiceExt.getFolder(getAlfrescoImapUser(user), folderName);
    }

    @Override
    public MailFolder getInbox(GreenMailUser user) throws FolderException {
        // todo: this should be disabled i think
        return imapServiceExt.getFolder(getAlfrescoImapUser(user), AlfrescoImapConst.INBOX_NAME);
    }

    @Override
    public void createPrivateMailAccount(GreenMailUser greenMailUser) throws FolderException {
        throw new UnsupportedOperationException();
    }

    @Override
    public MailFolder createMailbox(GreenMailUser greenMailUser, String s) throws AuthorizationException, FolderException {
        // Return null and OK result to support Office 2013
        return null;
    }

    @Override
    public void deleteMailbox(GreenMailUser greenMailUser, String s) throws FolderException, AuthorizationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void renameMailbox(GreenMailUser greenMailUser, String s, String s1) throws FolderException, AuthorizationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection listMailboxes(GreenMailUser user, String pattern) throws FolderException {
        return imapServiceExt.createAndListFolders(getAlfrescoImapUser(user), pattern);
    }

    @Override
    public Collection listSubscribedMailboxes(GreenMailUser user, String pattern) throws FolderException {
        return imapServiceExt.createAndListFolders(getAlfrescoImapUser(user), pattern);
    }

    @Override
    public void subscribe(GreenMailUser greenMailUser, String s) throws FolderException {
        // Real subscription management occurs in Delta, Oultook just fetches the results following this request so let's pretend to be co-operative.
    }

    @Override
    public void unsubscribe(GreenMailUser greenMailUser, String s) throws FolderException {
        // Real subscription management occurs in Delta, Oultook just fetches the results following this request so let's pretend to be co-operative.
    }

    private static AlfrescoImapUser getAlfrescoImapUser(GreenMailUser user) {
        return new AlfrescoImapUser(user.getEmail(), user.getLogin(), user.getPassword());
    }

    public void setImapServiceExt(ImapServiceExt imapServiceExt) {
        this.imapServiceExt = imapServiceExt;
    }
}
