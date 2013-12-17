package ee.webmedia.alfresco.classificator.enums;

/**
 * Enum for "sendMode" classificator values
 * 
 * @author Erko Hansar
 */
public enum SendMode {

    DVK("DVK"),
    EMAIL_DVK("e-post/DVK"),
    EMAIL("e-post"),
    EMAIL_BCC("e-post (bcc)"),
    MAIL("post"),
    REGISTERED_MAIL("tähitud post");

    private String valueName;

    SendMode(String value) {
        valueName = value;
    }

    public String getValueName() {
        return valueName;
    }

    public boolean equals(String value) {
        return valueName.equalsIgnoreCase(value);
    }

}
