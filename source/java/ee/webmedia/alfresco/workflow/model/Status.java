package ee.webmedia.alfresco.workflow.model;

<<<<<<< HEAD
/**
 * @author Alar Kvell
 */
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
public enum Status {

    /** uus */
    NEW("uus"),
    /** teostamisel */
    IN_PROGRESS("teostamisel"),
    /** peatatud */
    STOPPED("peatatud"),
    /** lõpetatud */
    FINISHED("lõpetatud"),
    /** teostamata */
<<<<<<< HEAD
    UNFINISHED("teostamata"),
    /** kustutatud - used only for linkedReviewTasks */
    DELETED("kustutatud");
=======
    UNFINISHED("teostamata");
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5

    private String name;

    private Status(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean equals(String otherName) {
        return name.equals(otherName);
    }

    public static Status of(String name) {
        for (Status status : values()) {
            if (status.equals(name)) {
                return status;
            }
        }
        throw new IllegalArgumentException("No Status with name '" + name + "'");
    }

}
