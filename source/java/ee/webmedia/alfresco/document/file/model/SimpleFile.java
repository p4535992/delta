package ee.webmedia.alfresco.document.file.model;

import java.io.Serializable;

import ee.webmedia.alfresco.utils.FilenameUtil;

public class SimpleFile implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String DIGIDOC_IMAGE_PATH = "/images/icons/ddoc_sign_small.gif";
    public static final String NON_DIGIDOC_IMAGE_PATH = "/images/icons/attachment.gif";

    protected final String displayName;
    protected final String imagePath;
    protected final String readOnlyUrl;

    public SimpleFile(String displayName, String readOnlyUrl) {
        this.displayName = displayName;
        imagePath = FilenameUtil.isDigiDocFile(displayName) ? DIGIDOC_IMAGE_PATH : NON_DIGIDOC_IMAGE_PATH;
        this.readOnlyUrl = readOnlyUrl;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getImagePath() {
        return imagePath;
    }

    public String getReadOnlyUrl() {
        return readOnlyUrl;
    }

}
