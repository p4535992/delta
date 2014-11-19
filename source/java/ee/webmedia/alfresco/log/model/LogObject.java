package ee.webmedia.alfresco.log.model;

/**
 * Defines all objects (object names) that get logged. Each object has corresponding log level.
<<<<<<< HEAD
 * 
 * @author Martti Tamm
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
 */
public enum LogObject {
    FUNCTION("Funktsioon", LogLevel.SPACES),
    SERIES("Sari", LogLevel.SPACES),
    VOLUME("Toimik", LogLevel.SPACES),
    CASE("Teema", LogLevel.SPACES),
    DOCUMENT("Dokument", LogLevel.DOCUMENT),
    WORKFLOW("Dokument", LogLevel.WORKFLOW),
<<<<<<< HEAD
    COMPOUND_WORKFLOW("Terviktöövoog", LogLevel.WORKFLOW),
    LOG_IN_OUT("Kasutaja", LogLevel.LOG_IN_OUT),
    USER("Kasutaja", LogLevel.USER_USERGROUP),
    USER_GROUP("Kasutajagrupp", LogLevel.USER_USERGROUP),
    EVENT_PLAN("Elukäik", LogLevel.SPACES),
=======
    LOG_IN_OUT("Kasutaja", LogLevel.LOG_IN_OUT),
    USER("Kasutaja", LogLevel.USER_USERGROUP),
    USER_GROUP("Kasutajagrupp", LogLevel.USER_USERGROUP),
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    RIGHTS_SERIES("Sari", LogLevel.SPACES),
    RIGHTS_VOLUME("Toimik", LogLevel.SPACES),
    RIGHTS_DOCUMENT("Dokument", LogLevel.DOCUMENT),
    NOTICE("Teavitus", LogLevel.EMAIL_NOTICES),
    REGISTER("Register", LogLevel.SPACES),
    RESTORE("Kustutamine", LogLevel.DELETED_OBJECTS),
    TASK("Tööülesanne", LogLevel.WORKFLOW),
<<<<<<< HEAD
    SEARCH_DOCUMENTS("Dokumentide otsing", LogLevel.SEARCHES),
    SEARCH_VOLUMES("Toimikute otsing", LogLevel.SEARCHES),
    SEARCH_TASKS("Tööülesannete otsing", LogLevel.SEARCHES),
    SEARCH_COMPOUND_WORKFLOWS("Terviktöövoogude otsing", LogLevel.SEARCHES),
    CASE_FILE("Asjatoimik", LogLevel.SPACES);
=======
    SEARCH_DOC("Dokumendi otsing", LogLevel.SEARCHES),
    SEARCH_TASK("Tööülesande otsing", LogLevel.SEARCHES);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5

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