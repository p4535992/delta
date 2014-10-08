package ee.webmedia.alfresco.log.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;


/**
 * Wrapper object for maintaining logging setup.
<<<<<<< HEAD
 * 
 * @author Martti Tamm
=======
>>>>>>> develop-5.1
 */
public class LogSetup implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean documents;

    private boolean workflows;

    private boolean logInOuts;

    private boolean userUsergroups;

    private boolean spaces;

    private boolean searches;

    private boolean notices;

    private boolean deletedObjects;

    public Set<LogLevel> toLogLevels() {
        Set<LogLevel> levels = new HashSet<LogLevel>();
        if (documents) {
            levels.add(LogLevel.DOCUMENT);
        }
        if (workflows) {
            levels.add(LogLevel.WORKFLOW);
        }
        if (logInOuts) {
            levels.add(LogLevel.LOG_IN_OUT);
        }
        if (userUsergroups) {
            levels.add(LogLevel.USER_USERGROUP);
        }
        if (spaces) {
            levels.add(LogLevel.SPACES);
        }
        if (notices) {
            levels.add(LogLevel.EMAIL_NOTICES);
        }
        if (searches) {
            levels.add(LogLevel.SEARCHES);
        }
        if (deletedObjects) {
            levels.add(LogLevel.DELETED_OBJECTS);
        }
        return levels;
    }

    public static LogSetup fromLogLevels(Collection<?> levels) {
        LogSetup setup = new LogSetup();
        if (levels == null) {
            setup.documents = true;
            setup.workflows = true;
        } else {
            setup.documents = levels.contains(LogLevel.DOCUMENT);
            setup.workflows = levels.contains(LogLevel.WORKFLOW);
            setup.logInOuts = levels.contains(LogLevel.LOG_IN_OUT);
            setup.userUsergroups = levels.contains(LogLevel.USER_USERGROUP);
            setup.spaces = levels.contains(LogLevel.SPACES);
            setup.notices = levels.contains(LogLevel.EMAIL_NOTICES);
            setup.searches = levels.contains(LogLevel.SEARCHES);
            setup.deletedObjects = levels.contains(LogLevel.DELETED_OBJECTS);
        }
        return setup;
    }

    public boolean isDocuments() {
        return documents;
    }

    public void setDocuments(boolean documents) {
        this.documents = documents;
    }

    public boolean isWorkflows() {
        return workflows;
    }

    public void setWorkflows(boolean workflows) {
        this.workflows = workflows;
    }

    public boolean isLogInOuts() {
        return logInOuts;
    }

    public void setLogInOuts(boolean logInOuts) {
        this.logInOuts = logInOuts;
    }

    public boolean isUserUsergroups() {
        return userUsergroups;
    }

    public void setUserUsergroups(boolean userUsergroups) {
        this.userUsergroups = userUsergroups;
    }

    public boolean isSpaces() {
        return spaces;
    }

    public void setSpaces(boolean spaces) {
        this.spaces = spaces;
    }

    public boolean isSearches() {
        return searches;
    }

    public void setSearches(boolean searches) {
        this.searches = searches;
    }

    public boolean isNotices() {
        return notices;
    }

    public void setNotices(boolean notices) {
        this.notices = notices;
    }

    public boolean isDeletedObjects() {
        return deletedObjects;
    }

    public void setDeletedObjects(boolean deletedObjects) {
        this.deletedObjects = deletedObjects;
    }
}
