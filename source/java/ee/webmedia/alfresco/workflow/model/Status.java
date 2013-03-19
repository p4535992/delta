package ee.webmedia.alfresco.workflow.model;

/**
 * @author Alar Kvell
 */
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
    UNFINISHED("teostamata"),
    /** kustutatud - used only for linkedReviewTasks */
    DELETED("kustutatud");

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
