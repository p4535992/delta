package ee.webmedia.alfresco.imap;

import com.icegreen.greenmail.foedus.util.MsgRangeFilter;
import com.icegreen.greenmail.mail.MovingMessage;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.store.FolderListener;
import com.icegreen.greenmail.store.MailFolder;
import com.icegreen.greenmail.store.SimpleStoredMessage;
import org.alfresco.repo.imap.AlfrescoImapFolder;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;

import javax.mail.Flags;
import javax.mail.internet.MimeMessage;
import javax.mail.search.SearchTerm;
import java.util.Date;
import java.util.List;

/**
 * TODO: add comment
 *
 * @author Romet Aidla
 */
public class ImmutableFolder implements MailFolder {
    public static final String PERMISSION_DENIED = "Permission denied.";
    protected MailFolder innerMailFolder;
    private static final Log log = LogFactory.getLog(ImmutableFolder.class);

    private AppendBehaviour appendBehaviour;


    public ImmutableFolder(MailFolder mailFolder, AppendBehaviour appendBehaviour) {
        Assert.notNull(mailFolder);
        Assert.notNull(appendBehaviour);
        
        this.innerMailFolder = mailFolder;
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
        return innerMailFolder.getFullName();
    }

    @Override
    public Flags getPermanentFlags() {
        return innerMailFolder.getPermanentFlags();
    }

    @Override
    public int getMessageCount() {
        return innerMailFolder.getMessageCount();
    }

    @Override
    public int getRecentCount(boolean b) {
        return innerMailFolder.getRecentCount(b);
    }

    @Override
    public long getUidValidity() {
        return innerMailFolder.getUidValidity();
    }

    @Override
    public int getFirstUnseen() {
        return innerMailFolder.getFirstUnseen();
    }

    @Override
    public int getUnseenCount() {
        return innerMailFolder.getUnseenCount();
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
        innerMailFolder.addListener(folderListener);
    }

    @Override
    public void removeListener(FolderListener folderListener) {
        innerMailFolder.addListener(folderListener);
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
        innerMailFolder.setFlags(flags, b, l, folderListener, b1);
    }

    @Override
    public void replaceFlags(Flags flags, long l, FolderListener folderListener, boolean b) throws FolderException {
        innerMailFolder.replaceFlags(flags, l, folderListener, b);
    }

    @Override
    public int getMsn(long l) throws FolderException {
        return innerMailFolder.getMsn(l);
    }

    @Override
    public void signalDeletion() {
        innerMailFolder.signalDeletion();
    }

    @Override
    public List getMessages(MsgRangeFilter msgRangeFilter) {
        return innerMailFolder.getMessages(msgRangeFilter);
    }

    @Override
    public List getMessages() {
        return innerMailFolder.getMessages();
    }

    @Override
    public List getNonDeletedMessages() {
        return innerMailFolder.getNonDeletedMessages();
    }
}
