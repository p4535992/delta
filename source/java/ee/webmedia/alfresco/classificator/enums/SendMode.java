package ee.webmedia.alfresco.classificator.enums;

/**
 * Enum for "sendMode" classificator values
<<<<<<< HEAD
 * 
 * @author Erko Hansar
=======
>>>>>>> develop-5.1
 */
public enum SendMode {

    DVK("DVK"),
    EMAIL_DVK("e-post/DVK"),
    EMAIL("e-post"),
    EMAIL_BCC("e-post (bcc)"),
    MAIL("post"),
<<<<<<< HEAD
    REGISTERED_MAIL("tähitud post");
=======
    REGISTERED_MAIL("tähitud post"),
    STATE_PORTAL_EESTI_EE("Riigiportaal eesti.ee");
>>>>>>> develop-5.1

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
