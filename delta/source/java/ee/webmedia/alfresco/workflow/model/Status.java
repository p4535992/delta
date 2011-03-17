package ee.webmedia.alfresco.workflow.model;

/**
 * @author Alar Kvell
 */
public enum Status {

    NEW("uus"),
    IN_PROGRESS("teostamisel"),
    STOPPED("peatatud"),
    FINISHED("lõpetatud"),
    UNFINISHED("teostamata");

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
