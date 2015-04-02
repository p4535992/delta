package ee.webmedia.alfresco.workflow.model;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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

    public static final Set<String> NEW_INPROGRESS_OR_STOPPED = new HashSet<>(Arrays.asList(Status.NEW.getName(), Status.IN_PROGRESS.getName(), Status.STOPPED.getName()));

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

    public static Set<Status> getAllExept(Status status) {
        Set<Status> result = new HashSet<>();
        for (Status s : Status.values()) {
            if (status.equals(s)) {
                continue;
            }
            result.add(s);
        }
        return result;
    }

}
