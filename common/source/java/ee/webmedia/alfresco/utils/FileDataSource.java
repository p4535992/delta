<<<<<<< HEAD
package ee.webmedia.alfresco.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataSource;

public class FileDataSource implements DataSource {

    private final File file;
    private final String contentType;

    public FileDataSource(File file, String contentType) {
        this.file = file;
        this.contentType = contentType;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new BufferedInputStream(new FileInputStream(file));
    }

    @Override
    public String getName() {
        throw new RuntimeException("No name");
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        throw new IOException("Read-only data");
    }

}
=======
package ee.webmedia.alfresco.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataSource;

public class FileDataSource implements DataSource {

    private final File file;
    private final String contentType;

    public FileDataSource(File file, String contentType) {
        this.file = file;
        this.contentType = contentType;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new BufferedInputStream(new FileInputStream(file));
    }

    @Override
    public String getName() {
        throw new RuntimeException("No name");
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        throw new IOException("Read-only data");
    }

}
>>>>>>> develop-5.1
