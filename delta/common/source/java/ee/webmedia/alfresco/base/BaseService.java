package ee.webmedia.alfresco.base;

import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.utils.Predicate;

/**
 * @author Alar Kvell
 */
public interface BaseService {

    String BEAN_NAME = "BaseService";

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
     * @return all children of <code>parentRef</code>
     * @see #getChildren(NodeRef, Class, Predicate)
     */
    <T extends BaseObject> List<T> getChildren(NodeRef parentRef, Class<T> childrenClass);

    /**
     * @param <T>
     * @param parentRef
     * @param assocName - used to identify parent-child association name to be used to find child
     * @param childrenClass - if node is found, then objec of this class is initiated based on node found
     * @return null if given parent has no child with given assocName, otherwise object of type T is returned
     */
    <T extends BaseObject> T getChild(NodeRef parentRef, QName assocName, Class<T> childrenClass);

    /**
     * @param <T>
     * @param parentRef
     * @param childrenClass
     * @param mustIncludePredicate - optional predicate that could be used to filter children to be returned
     * @return children of the <code>parentRef</code> (if mustIncludePredicate != null then only those that match predicate)
     */
    <T extends BaseObject> List<T> getChildren(NodeRef parentRef, Class<T> childrenClass, Predicate<T> mustIncludePredicate);

    <T extends BaseObject> List<T> getObjects(List<NodeRef> resultRefs, Class<T> resultClass);

    /**
     * Save object hierarchy to repository. Saving is performed deeply - all children are also saved recursively. <br/>
     * <br/>
     * <b>NB!</b> Caller should use {@link BaseObject#clone()} before making any changes to value objects (and before calling {@link #saveObject}) - if transaction rollback occurs,
     * then it is usually desired that value objects in web layer are left untouched.
     * 
     * @param object object hierarchy to save
     * @return if {@code true} then there were changes and these were saved and caller needs to reload its object hierarchy from repository. If {@code false} then there were no
     *         changes to save.
     */
    boolean saveObject(BaseObject object);

}
