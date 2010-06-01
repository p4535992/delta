package ee.webmedia.alfresco.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataSource;

import org.alfresco.service.cmr.repository.ContentReader;

public class ContentReaderDataSource implements DataSource {

    private ContentReader contentReader;
    private String fileName;

    public ContentReaderDataSource(ContentReader contentReader, String fileName) {
        this.contentReader = contentReader;
        this.fileName = fileName;
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
        return contentReader.getMimetype();
    }

    @Override
    public String getName() {
        return fileName;
    }

}
