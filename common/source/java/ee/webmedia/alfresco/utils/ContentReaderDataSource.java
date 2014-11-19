package ee.webmedia.alfresco.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataSource;

import org.alfresco.service.cmr.repository.ContentReader;

public class ContentReaderDataSource implements DataSource {

    private final ContentReader contentReader;
    private final String fileName;
    private final String contentType;

    public ContentReaderDataSource(ContentReader contentReader, String fileName) {
        this.contentReader = contentReader;
        this.fileName = fileName;
        contentType = MimeUtil.getContentType(contentReader);
    }

    public ContentReaderDataSource(ContentReader contentReader, String fileName, String mimeType, String encoding) {
        this.contentReader = contentReader;
        this.fileName = fileName;
        contentType = MimeUtil.getContentType(mimeType, encoding);
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return contentReader.getContentInputStream();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        throw new IOException("Read-only data");
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public String getName() {
        return fileName;
    }

<<<<<<< HEAD
    public String getMimeType() {
        return contentReader.getMimetype();
    }

    public long getContentSize() {
        return contentReader.getSize();
    }

    public String getEncoding() {
        return contentReader.getEncoding();
    }

=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
}
