package ee.webmedia.alfresco.document.permissions;

/**
 * Enum for permissions
 * 
 * @author Erko Hansar
 */
public enum Permission {
    
    DOCUMENT_WRITE("DocumentWrite");
    
    private String valueName;
    
    Permission(String value) {
        this.valueName = value;
    }

    public String getValueName() {
        return valueName;
    }

    public boolean equals(String value) {
        return this.valueName.equals(value);
    }

}
