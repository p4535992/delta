package ee.webmedia.alfresco.base;

import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.QNamePattern;

import ee.webmedia.alfresco.utils.Predicate;
import ee.webmedia.alfresco.utils.RepoUtil;

/**
 * @author Alar Kvell
 */
public interface BaseService {

    String BEAN_NAME = "BaseService";

    /** {@link BaseObject}s that have this property are not saved when calling {@link #saveObject(BaseObject)} method */
    QName SKIP_SAVE = RepoUtil.createTransientProp("skipSave");
    QName CHILDREN_NOT_LOADED = RepoUtil.createTransientProp("childrenNotLoaded");
    QName CHILDREN_LOADING_IN_PROGRESS = RepoUtil.createTransientProp("childrenLoadingInProgress");

    void addTypeMapping(QName type, Class<? extends BaseObject> clazz);

    /**
     * Load object hierarchy from repository. Loading is performed deeply - all children whose type mapping exists are also loaded recursively.
     * 
     * @param <R> - actual return type
     * @param <T> - type or supertype of the actual return type
     * @param nodeRef
     * @param returnCompatibleClass - type or supertype of the actual return type
     * @return object hierarchy that was loaded
     */
    <T extends BaseObject> T getObject(NodeRef nodeRef, Class<T> returnCompatibleClass);

    /**
     * @param <T>
     * @param nodeRef
     * @param returnCompatibleClass - if null then type check is not performed
     * @param effort
     * @return
     */
    <T extends BaseObject> T getObject(NodeRef nodeRef, Class<T> returnCompatibleClass, Effort effort);

    /**
     * @return all children of <code>parentRef</code>
     * @see #getChildren(NodeRef, Class, Predicate)
     */
    <T extends BaseObject> List<T> getChildren(NodeRef parentRef, Class<T> childrenClass);

    /**
     * @param <T>
     * @param parentRef
     * @param assocNamePattern - used to identify parent-child association name to be used to find child
     * @param childrenClass - if node is found, then objec of this class is initiated based on node found
     * @return null if given parent has no child with given assocName, otherwise object of type T is returned
     */
    <T extends BaseObject> T getChild(NodeRef parentRef, QNamePattern assocNamePattern, Class<T> childrenClass);

    /**
     * This method could be used when hierarchy of <code>parent</code> object is partially loaded and you want to load some more objects
     */
    <T extends BaseObject> void loadChildren(T parent, Effort effort);

    /**
     * @param <T>
     * @param parentRef
     * @param childrenClass
     * @param mustIncludePredicate - optional predicate that could be used to filter children to be returned
     * @return children of the <code>parentRef</code> (if mustIncludePredicate != null then only those that match predicate)
     */
    <T extends BaseObject> List<T> getChildren(NodeRef parentRef, Class<T> childrenClass, Predicate<T> mustIncludePredicate);

    <T extends BaseObject> List<T> getChildren(NodeRef parentRef, Class<T> childrenClass, Predicate<T> mustIncludePredicate, Effort effort);

    /**
     * Method that returns only children of parent node that are associated to parent by given association type and name patterns.
     * It is more effective than {@link #getChildren(NodeRef, Class, Predicate, Effort)} <br>
     * TODO implemented it in a nick of time - probably it would be more elegant if typeQNamePattern and qnamePattern would be somehow integrated into {@link Effort} interface
     * that decides how much effort should be spent to fetch children
     * 
     * @param <T>
     * @param parentRef
     * @param childrenClass
     * @param typeQNamePattern the pattern that the type qualified name of the association must match
     * @param qnamePattern the pattern that the qnames of the assocs must match
     * @return
     */
    <T extends BaseObject> List<T> getChildren(NodeRef parentRef, Class<T> childrenClass, QNamePattern typeQNamePattern, QNamePattern qnamePattern, Effort effort);

    <T extends BaseObject> List<T> getObjects(List<NodeRef> resultRefs, Class<T> resultClass);

    /**
     * Save object hierarchy to repository. Saving is performed deeply - all children are also saved recursively. <br/>
     * <br/>
     * <b>NB!</b> Caller should use {@link BaseObject#clone()} before making any changes to value objects (and before calling {@link #saveObject}) - if transaction rollback occurs,
     * then it is usually desired that value objects in web layer are left untouched. <br>
     * <br>
     * object or its children that have {@link #SKIP_SAVE} property are not saved when calling this method - this property is removed before returning from object that was given as
     * an argument!
     * 
     * @param object object hierarchy to save
     * @return if {@code true} then there were changes and these were saved and caller needs to reload its object hierarchy from repository. If {@code false} then there were no
     *         changes to save.
     */
    boolean saveObject(BaseObject object);

    /**
     * Determines effort to be used to retrieve hierarchy of the object. <br>
     * For example you could eliminate fetching children if parent type or property doesn't match certain condition
     */
    interface Effort {
        boolean isReturnChildren(BaseObject parent);

        Effort INCLUDE_ALL_CHILDREN = null;

        Effort DONT_INCLUDE_CHILDREN = new Effort() {
            @Override
            public boolean isReturnChildren(BaseObject parent) {
                return false;
            }
        };
    }

}
