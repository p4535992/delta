package ee.webmedia.alfresco.email.model;

<<<<<<< HEAD
import org.springframework.core.io.InputStreamSource;

/**
 * @author Alar Kvell
 */
=======
import org.alfresco.service.cmr.repository.NodeRef;
import org.springframework.core.io.InputStreamSource;

>>>>>>> develop-5.1
public class EmailAttachment {

    private final String fileName;
    private final String mimeType;
    private final String encoding;
    private final InputStreamSource inputStreamSource;
<<<<<<< HEAD

    public EmailAttachment(String fileName, String mimeType, String encoding, InputStreamSource inputStreamSource) {
=======
    private final NodeRef fileNodeRef;

    public EmailAttachment(String fileName, String mimeType, String encoding, InputStreamSource inputStreamSource, NodeRef fileNodeRef) {
>>>>>>> develop-5.1
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.encoding = encoding;
        this.inputStreamSource = inputStreamSource;
<<<<<<< HEAD
=======
        this.fileNodeRef = fileNodeRef;
>>>>>>> develop-5.1
    }

    public String getFileName() {
        return fileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getEncoding() {
        return encoding;
    }

    public InputStreamSource getInputStreamSource() {
        return inputStreamSource;
    }

<<<<<<< HEAD
=======
    public NodeRef getFileNodeRef() {
        return fileNodeRef;
    }

>>>>>>> develop-5.1
}
