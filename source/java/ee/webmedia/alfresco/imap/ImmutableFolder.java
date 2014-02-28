package ee.webmedia.alfresco.imap;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.mail.Flags;
import javax.mail.internet.MimeMessage;
import javax.mail.search.SearchTerm;

import org.alfresco.repo.imap.AlfrescoImapFolder;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;

import com.icegreen.greenmail.foedus.util.MsgRangeFilter;
import com.icegreen.greenmail.mail.MovingMessage;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.store.FolderListener;
import com.icegreen.greenmail.store.MailFolder;
import com.icegreen.greenmail.store.SimpleStoredMessage;

/**
 * Mail folder implementation, that disables most activities on folder: makes it immutable, unlistable etc.
 * Append behaviour can be defined using {@link ee.webmedia.alfresco.imap.AppendBehaviour}.
 * 
 * @author Romet Aidla
 */
public class ImmutableFolder implements MailFolder {
    public static final String PERMISSION_DENIED = "Permission denied.";
    protected MailFolder innerMailFolder;
    private static final Log log = LogFactory.getLog(ImmutableFolder.class);

    private final AppendBehaviour appendBehaviour;

    public ImmutableFolder(MailFolder mailFolder, AppendBehaviour appendBehaviour) {
        Assert.notNull(mailFolder);
        Assert.notNull(appendBehaviour);

        innerMailFolder = mailFolder;
        this.appendBehaviour = appendBehaviour;
    }

    public NodeRef getFolderNodeRef() {
        return ((AlfrescoImapFolder) innerMailFolder).getFolderInfo().getNodeRef();
    }

    @Override
    public String getName() {
        return innerMailFolder.getName();
    }

    @Override
    public String getFullName() {
        String fullName = innerMailFolder.getFullName();
        if (StringUtils.isNotBlank(fullName)) {
            fullName = StringUtils.remove(fullName, '"'); // Trim double quotes, since they are not used in Delta and cause crashing in Outlook 2010/2013
        }
        return fullName;
    }

    @Override
    public Flags getPermanentFlags() {
        return innerMailFolder.getPermanentFlags();
    }

    @Override
    public int getMessageCount() {
        return 0;
    }

    @Override
    public int getRecentCount(boolean b) {
        return 0;
    }

    @Override
    public long getUidValidity() {
        return innerMailFolder.getUidValidity();
    }

    @Override
    public int getFirstUnseen() {
        return 0;
    }

    @Override
    public int getUnseenCount() {
        return 0;
    }

    @Override
    public boolean isSelectable() {
        return innerMailFolder.isSelectable();
    }

    @Override
    public long getUidNext() {
        return innerMailFolder.getUidNext();
    }

    @Override
    public long appendMessage(MimeMessage mimeMessage, Flags flags, Date date) throws FolderException {
        return appendBehaviour.appendMessage(this, mimeMessage, flags, date);
    }

    @Override
    public void deleteAllMessages() throws FolderException {
        throw new FolderException(PERMISSION_DENIED);
    }

    @Override
    public void expunge() throws FolderException {
        throw new FolderException(PERMISSION_DENIED);
    }

    @Override
    public void addListener(FolderListener folderListener) {
        // do nothing
    }

    @Override
    public void removeListener(FolderListener folderListener) {
        // do nothing
    }

    @Override
    public void store(MovingMessage movingMessage) throws Exception {
        throw new FolderException(PERMISSION_DENIED);
    }

    @Override
    public void store(MimeMessage mimeMessage) throws Exception {
        throw new FolderException(PERMISSION_DENIED);
    }

    @Override
    public SimpleStoredMessage getMessage(long l) {
        return innerMailFolder.getMessage(l);
    }

    @Override
    public long[] getMessageUids() {
        return innerMailFolder.getMessageUids();
    }

    @Override
    public long[] search(SearchTerm searchTerm) {
        return innerMailFolder.search(searchTerm);
    }

    @Override
    public void copyMessage(long l, MailFolder mailFolder) throws FolderException {
        throw new FolderException(PERMISSION_DENIED);
    }

    @Override
    public void setFlags(Flags flags, boolean b, long l, FolderListener folderListener, boolean b1) throws FolderException {
        // do nothing
    }

    @Override
    public void replaceFlags(Flags flags, long l, FolderListener folderListener, boolean b) throws FolderException {
        // do nothing
    }

    @Override
    public int getMsn(long l) throws FolderException {
        throw new FolderException(PERMISSION_DENIED);
    }

    @Override
    public void signalDeletion() {
        // do nothing
    }

    @Override
    public List getMessages(MsgRangeFilter msgRangeFilter) {
        return Collections.EMPTY_LIST;
    }

    @Override
    public List getMessages() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public List getNonDeletedMessages() {
        return Collections.EMPTY_LIST;
    }
    
    @Override
    public boolean isMarked() {
        return false;
    }
}
