package ee.webmedia.alfresco.classificator.enums;

/**
 * Enum for "sendMode" classificator values
 */
public enum SendMode {

    DVK("DVK"),
    EMAIL_DVK("e-post/DVK"),
    EMAIL("e-post"),
    EMAIL_BCC("e-post (bcc)"),
    MAIL("post"),
    REGISTERED_MAIL("tähitud post"),
    STATE_PORTAL_EESTI_EE("Riigiportaal eesti.ee");

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
