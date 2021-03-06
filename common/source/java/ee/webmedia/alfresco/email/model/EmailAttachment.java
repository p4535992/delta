package ee.webmedia.alfresco.email.model;

import org.alfresco.service.cmr.repository.NodeRef;
import org.springframework.core.io.InputStreamSource;

public class EmailAttachment {

    private final String fileName;
    private final String mimeType;
    private final String encoding;
    private final InputStreamSource inputStreamSource;
    private final NodeRef fileNodeRef;

    public EmailAttachment(String fileName, String mimeType, String encoding, InputStreamSource inputStreamSource, NodeRef fileNodeRef) {
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.encoding = encoding;
        this.inputStreamSource = inputStreamSource;
        this.fileNodeRef = fileNodeRef;
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

    public NodeRef getFileNodeRef() {
        return fileNodeRef;
    }

}
