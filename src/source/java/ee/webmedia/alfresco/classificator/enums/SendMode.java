package ee.webmedia.alfresco.classificator.enums;

/**
 * Enum for "sendMode" classificator values
 * 
 * @author Erko Hansar
 */
public enum SendMode {
    
    EMAIL_DVK("e-post/DVK"),
    EMAIL("e-post"),
    MAIL("post");
    
    private String valueName;
    
    SendMode(String value) {
        this.valueName = value;
    }

    public String getValueName() {
        return valueName;
    }

    public boolean equals(String value) {
        return this.valueName.equalsIgnoreCase(value);
    }

}
