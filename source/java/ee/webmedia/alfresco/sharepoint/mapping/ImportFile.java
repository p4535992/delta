package ee.webmedia.alfresco.sharepoint.mapping;

import java.io.File;
import java.util.Date;

import ee.webmedia.alfresco.sharepoint.ImportValidationException;

public class ImportFile {

    private final String title;
    private final File file;
    private final Date created;
    private final String creator;
    private Date modified;
    private String modifier;
    private final boolean active;

    public ImportFile(String title, File file) {
        this.title = title;
        this.file = file;
        created = modified = new Date();
        creator = modifier = "IMPORT";
        active = false;
    }

    public ImportFile(String title, File file, Date created, String creator, Date modified, String modifier) {
        this.title = title;
        this.file = file;
        this.created = created;
        this.creator = creator;
        this.modified = modified;
        this.modifier = modifier;
        active = true;
    }

    public String getTitle() {
        return title;
    }

    public String getFilename() {
        return file.getName();
    }

    public File getFile() {
        return file;
    }

    public Date getCreated() {
        return created;
    }

    public String getCreator() {
        return creator;
    }

    public Date getModified() {
        return modified;
    }

    public String getModifier() {
        return modifier;
    }

    public Boolean isActive() {
        return Boolean.valueOf(active);
    }

    public void validate() throws ImportValidationException {
        if (!file.exists()) {
            throw new ImportValidationException("Could not find document file " + file.getName() + " (" + title + ")");
        }
    }
}
