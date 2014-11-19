package ee.webmedia.alfresco.email.model;

import org.springframework.core.io.InputStreamSource;

<<<<<<< HEAD
/**
 * @author Alar Kvell
 */
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
public class EmailAttachment {

    private final String fileName;
    private final String mimeType;
    private final String encoding;
    private final InputStreamSource inputStreamSource;

    public EmailAttachment(String fileName, String mimeType, String encoding, InputStreamSource inputStreamSource) {
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.encoding = encoding;
        this.inputStreamSource = inputStreamSource;
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

}
