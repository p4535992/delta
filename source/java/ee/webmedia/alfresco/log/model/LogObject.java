package ee.webmedia.alfresco.log.model;

/**
 * Defines all objects (object names) that get logged. Each object has corresponding log level.
 */
public enum LogObject {
    FUNCTION("Funktsioon", LogLevel.SPACES),
    SERIES("Sari", LogLevel.SPACES),
    VOLUME("Toimik", LogLevel.SPACES),
    CASE("Teema", LogLevel.SPACES),
    DOCUMENT("Dokument", LogLevel.DOCUMENT),
    WORKFLOW("Dokument", LogLevel.WORKFLOW),
    LOG_IN_OUT("Kasutaja", LogLevel.LOG_IN_OUT),
    USER("Kasutaja", LogLevel.USER_USERGROUP),
    USER_GROUP("Kasutajagrupp", LogLevel.USER_USERGROUP),
    RIGHTS_SERIES("Sari", LogLevel.SPACES),
    RIGHTS_VOLUME("Toimik", LogLevel.SPACES),
    RIGHTS_DOCUMENT("Dokument", LogLevel.DOCUMENT),
    NOTICE("Teavitus", LogLevel.EMAIL_NOTICES),
    REGISTER("Register", LogLevel.SPACES),
    RESTORE("Kustutamine", LogLevel.DELETED_OBJECTS),
    TASK("Tööülesanne", LogLevel.WORKFLOW),
    SEARCH_DOC("Dokumendi otsing", LogLevel.SEARCHES),
    SEARCH_TASK("Tööülesande otsing", LogLevel.SEARCHES);

    private String objectName;
    private LogLevel level;

    private LogObject(String objectName, LogLevel level) {
        this.objectName = objectName;
        this.level = level;
    }

    public String getObjectName() {
        return objectName;
    }

    public String getLevel() {
        return level.name();
    }
}