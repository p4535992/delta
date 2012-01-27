package ee.webmedia.alfresco.log.service;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import ee.webmedia.alfresco.log.model.Level;

/**
 * Wrapper object for maintaining logging setup.
 * 
 * @author Martti Tamm
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

    public Set<Level> toLogLevels() {
        Set<Level> levels = new HashSet<Level>();
        if (documents) {
            levels.add(Level.DOCUMENT);
        }
        if (workflows) {
            levels.add(Level.WORKFLOW);
        }
        if (logInOuts) {
            levels.add(Level.LOG_IN_OUT);
        }
        if (userUsergroups) {
            levels.add(Level.USER_USERGROUP);
        }
        if (spaces) {
            levels.add(Level.SPACES);
        }
        if (notices) {
            levels.add(Level.EMAIL_NOTICES);
        }
        if (searches) {
            levels.add(Level.SEARCHES);
        }
        if (deletedObjects) {
            levels.add(Level.DELETED_OBJECTS);
        }
        return levels;
    }

    public static LogSetup fromLogLevels(Collection<?> levels) {
        LogSetup setup = new LogSetup();
        if (levels == null) {
            setup.documents = true;
            setup.workflows = true;
        } else {
            setup.documents = levels.contains(Level.DOCUMENT);
            setup.workflows = levels.contains(Level.WORKFLOW);
            setup.logInOuts = levels.contains(Level.LOG_IN_OUT);
            setup.userUsergroups = levels.contains(Level.USER_USERGROUP);
            setup.spaces = levels.contains(Level.SPACES);
            setup.notices = levels.contains(Level.EMAIL_NOTICES);
            setup.searches = levels.contains(Level.SEARCHES);
            setup.deletedObjects = levels.contains(Level.DELETED_OBJECTS);
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
