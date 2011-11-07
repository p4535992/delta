package ee.webmedia.alfresco.base;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.EqualsHelper;
import org.alfresco.web.action.ActionEvaluator;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.model.NodeBaseVO;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.utils.Closure;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;

/**
 * NB! All subclasses must declare at least one of the two following constructors, based on required usage:<br>
 * 1) {@link #BaseObject(NodeRef, WmNode)} -- if the object is the tip of data model, parent object is null, has only parentRef<br>
 * 2) {@link #BaseObject(BaseObject, WmNode)} -- if the object is not the tip of data model, parent object is non-null<br>
 * And the following constructor:<br>
 * 3) {@link #BaseObject(BaseObject, QName)} -- if the object is used as a child of some other object, and in-memory child creation is required<br>
 * 
 * @author Alar Kvell
 */
public abstract class BaseObject extends NodeBaseVO implements Cloneable {
    private static final long serialVersionUID = 1L;
    private static final QName COPY_OF_NODE_REF = RepoUtil.createTransientProp("copyOfNodeRef");
    private static final QName CLONE_OF_NODE_REF = RepoUtil.createTransientProp("cloneOfNodeRef");
    /**
     * could be used to reconstruct object (using constructor {@link #BaseObject(NodeRef, WmNode)} <br>
     * when we only have node (for example in {@link ActionEvaluator#evaluate(org.alfresco.web.bean.repository.Node)} of the object
     */
    private static final QName PARENT_NODE_REF = RepoUtil.createTransientProp("parentNodeRef");

    // Currently only node type, properties and children (child-assocs) are supported
    // Also, when creating a new object in memory, default properties and aspects are loaded from dictionaryService
    //
    // But node aspects and assocs - adding/removing/saving - are currently unsupported; if there is need, then implement it

    private BaseObject parent;
    private NodeRef parentNodeRef;
    private Map<QName, Serializable> originalProperties;
    private Map<Class<? extends BaseObject>, List<? extends BaseObject>> children = new HashMap<Class<? extends BaseObject>, List<? extends BaseObject>>();
    private Map<Class<? extends BaseObject>, List<? extends BaseObject>> removedChildren = new HashMap<Class<? extends BaseObject>, List<? extends BaseObject>>();

    // TODO constructors; and serialization

    /** Used by {@link BaseServiceImpl#getObject(NodeRef, Class)} through reflection (when loading ancestor object) */
    protected BaseObject(BaseObject parent, WmNode node) {
        this(parent, parent.getNodeRef(), node);
    }

    /** Used by {@link BaseServiceImpl#getObject(NodeRef, Class)} through reflection (when loading this object) */
    protected BaseObject(NodeRef parentNodeRef, WmNode node) {
        this(null, parentNodeRef, node);
    }

    private BaseObject(BaseObject parent, NodeRef parentNodeRef, WmNode node) {
        Assert.notNull(node);
        Assert.isTrue(parent != null || WmNode.isSaved(parentNodeRef), "At least one of parent or parentNodeRef must be non-null");
        setParent(parent);
        this.node = node;
        setParentNodeRef(parentNodeRef);
        if (parent != null) {
            Assert.isTrue(ObjectUtils.equals(parent.getNodeRef(), parentNodeRef));
        }
        if (node.isSaved()) {
            Assert.isTrue(WmNode.isSaved(parentNodeRef));
        }
        if (node.isUnsaved()) {
            originalProperties = new HashMap<QName, Serializable>();
        } else {
            originalProperties = getProperties(true);
        }
    }

    private <P extends BaseObject> void setParent(P parent) {
        this.parent = parent;
    }

    private void setParentNodeRef(NodeRef parentNodeRef) {
        this.parentNodeRef = parentNodeRef;
        setProp(PARENT_NODE_REF, parentNodeRef);
    }

    /** used only by subclass to construct new unsaved object */
    protected BaseObject(BaseObject parent, QName type) {
        this(parent, parent.getNodeRef(), type);
    }

    /** used only by subclass to construct new unsaved object */
    protected BaseObject(NodeRef parentNodeRef, QName type) {
        this(null, parentNodeRef, type);
    }

    private BaseObject(BaseObject parent, NodeRef parentNodeRef, QName type) {
        this(parent, parentNodeRef, new WmNode(RepoUtil.createNewUnsavedNodeRef(), type, getDefaultAspects(type), getDefaultProperties(type)));
    }

    private static Set<QName> getDefaultAspects(QName type) {
        return RepoUtil.getAspectsIgnoringSystem(BeanHelper.getGeneralService().getDefaultAspects(type));
    }

    private static Map<QName, Serializable> getDefaultProperties(QName type) {
        return RepoUtil.getPropertiesIgnoringSystem(BeanHelper.getGeneralService().getDefaultProperties(type), BeanHelper.getDictionaryService());
    }

    protected <T extends BaseObject> void nextSaveToParent(BaseObject newParent, Class<T> childrenListClass) {
        Assert.notNull(newParent, "new parent shouldn't be null");
        setParent(newParent);
        nextSaveToParent(newParent.getNodeRef());
        // add self to parent
        ChildrenList<T> childrenList = newParent.getChildren(childrenListClass);
        @SuppressWarnings("unchecked")
        T self = (T) this;
        childrenList.addExisting(self);
    }

    protected void nextSaveToParent(NodeRef newParentRef) {
        Assert.isTrue(newParentRef != null && !newParentRef.equals(parentNodeRef));
        setParentNodeRef(newParentRef);
        visit(new Closure<BaseObject>() { // reset nodeRef so it would appear as unsaved
            @Override
            public void exec(BaseObject object) {
                int expectedChangedPropsCount = object != BaseObject.this ? 0 : /* parentNodeRef is changed */1;
                Assert.isTrue(object.getChangedProperties().size() == expectedChangedPropsCount,
                        "if properties are added before calling Baseobject.nextSaveToParent() method, then code here must be updated");
                object.getNode().updateNodeRef(null);
                Map<QName, Serializable> props = new HashMap<QName, Serializable>(originalProperties);
                object.clearOriginalProperties();
                getNode().getProperties().putAll(RepoUtil.toStringProperties(props));
            }
        });
    }

    public BaseObject cloneAndResetBaseState() {
        BaseObject clone = clone();
        CloneUtil.resetBaseState(clone);
        CloneUtil.resetChildrenBaseState(clone);
        return clone;
    }

    @Override
    public BaseObject clone() {
        try {
            BaseObject copy = (BaseObject) super.clone();
            copy.setCloneOfNodeRef(getNodeRef());
            return CloneUtil.deepCopy(this, copy);
        } catch (CloneNotSupportedException e) {
            // shouldn't happen, because BaseObject is clonable and children are also subtypes of BaseObject
            throw new RuntimeException(e);
        }
    }

    public static class CloneUtil {
        private static BaseObject deepCopy(BaseObject source, BaseObject target) {
            target.originalProperties = RepoUtil.copyProperties(source.originalProperties);
            target.children = cloneMap(target.children, target);
            target.removedChildren = cloneMap(target.removedChildren, target);
            return target;
        }

        private static BaseObject clone(BaseObject originalChild, BaseObject copyParent) {
            BaseObject copyChild = originalChild.clone();
            copyChild.setParent(copyParent);
            return copyChild;
        }

        private static <T extends BaseObject> List<T> cloneList(List<T> originalList, BaseObject copyParent) {
            List<T> copyList = null;
            if (originalList != null) {
                copyList = new ArrayList<T>(originalList.size());
                for (BaseObject child : originalList) {
                    @SuppressWarnings("unchecked")
                    T copyChild = (T) clone(child, copyParent);
                    copyList.add(copyChild);
                }
            }
            return copyList;
        }

        private static Map<Class<? extends BaseObject>, List<? extends BaseObject>> cloneMap(Map<Class<? extends BaseObject>, List<? extends BaseObject>> originalMap,
                BaseObject copyParent) {
            Map<Class<? extends BaseObject>, List<? extends BaseObject>> copyMap = new HashMap<Class<? extends BaseObject>, List<? extends BaseObject>>(originalMap.size());
            for (Entry<Class<? extends BaseObject>, List<? extends BaseObject>> entry : originalMap.entrySet()) {
                List<? extends BaseObject> list = entry.getValue();
                List<? extends BaseObject> copyList = cloneList(list, copyParent);
                copyMap.put(entry.getKey(), copyList);
            }
            return copyMap;
        }

        private static void resetChildrenBaseState(BaseObject baseObject) {
            for (Entry<Class<? extends BaseObject>, List<? extends BaseObject>> entry : baseObject.getChildren().entrySet()) {
                for (BaseObject child : (List<? extends BaseObject>) entry.getValue()) {
                    child.setParentNodeRef(baseObject.getNodeRef());
                    resetBaseState(child);
                    resetChildrenBaseState(child);
                }
            }
        }

        private static void resetBaseState(BaseObject object) {
            object.setCopyOfNodeRef(object.getNodeRef());
            object.getNode().updateNodeRef(null);
            object.getRemovedChildren().clear();
            object.originalProperties = new HashMap<QName, Serializable>();
        }
    }

    public static <T extends BaseObject> T reconstruct(WmNode nodeOfBaseObject, Class<T> clazz) {
        NodeRef parentRef = (NodeRef) nodeOfBaseObject.getProperties().get(BaseObject.PARENT_NODE_REF);
        if (parentRef == null) {
            throw new IllegalArgumentException("Can't reconstruct object because given node is not taken from baseObject");
        }
        return BaseObject.createNewWithParentNodeRef(clazz, parentRef, nodeOfBaseObject);
    }

    protected static <T extends BaseObject> T createNewWithParentNodeRef(Class<T> clazz, NodeRef parentNodeRef, WmNode node) {
        try {
            return clazz.getDeclaredConstructor(NodeRef.class, WmNode.class).newInstance(parentNodeRef, node);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected static <T extends BaseObject> T createNewWithParentObject(Class<T> clazz, BaseObject parent, WmNode node) {
        try {
            return clazz.getDeclaredConstructor(BaseObject.class, WmNode.class).newInstance(parent, node);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected static <T extends BaseObject> T createNewUnsaved(Class<T> clazz, BaseObject parent) {
        try {
            return clazz.getDeclaredConstructor(BaseObject.class).newInstance(parent);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected BaseObject getParent() {
        return parent;
    }

    protected void resetParent() {
        parent = null;
    }

    protected NodeRef getParentNodeRef() {
        return parentNodeRef;
    }

    protected void updateParentNodeRef() {
        Assert.notNull(parent);
        setParentNodeRef(parent.getNodeRef());
    }

    protected void destroy() {
        setParentNodeRef(null);
        node = null;
        setParent(null);
        originalProperties = null;
        children.clear();
        removedChildren.clear();
    }

    // -----------------

    public class ChildrenList<T extends BaseObject> extends AbstractList<T> {

        private final Class<T> clazz;

        private ChildrenList(Class<T> clazz) {
            this.clazz = clazz;
        }

        private List<T> getModifiableList() {
            @SuppressWarnings("unchecked")
            List<T> list = (List<T>) children.get(clazz);
            if (list == null) {
                list = new ArrayList<T>();
                children.put(clazz, list);
            }
            return list;
        }

        private List<T> getRemovedList() {
            @SuppressWarnings("unchecked")
            List<T> list = (List<T>) removedChildren.get(clazz);
            if (list == null) {
                list = new ArrayList<T>();
                removedChildren.put(clazz, list);
            }
            return list;
        }

        public List<? extends T> getList() {
            return Collections.unmodifiableList(getModifiableList());
        }

        public T add(int index) {
            T child = createNewChild(clazz);
            getModifiableList().add(index, child);
            return child;
        }

        public <S extends T> S add(int index, Class<S> subClazz) {
            S child = createNewChild(subClazz);
            getModifiableList().add(index, child);
            return child;
        }

        public T add() {
            return add(getModifiableList().size());
        }

        public <S extends T> S add(Class<S> subClazz) {
            return add(getModifiableList().size(), subClazz);
        }

        public T addExisting(int index, T child) {
            Assert.isTrue(child.getParent() == BaseObject.this, "Cannot add child that was previously under another parent");
            Assert.isTrue(!contains(child), "ChildrenList already contains this child");
            getModifiableList().add(index, child);
            return child;
        }

        public T addExisting(T child) {
            return addExisting(getModifiableList().size(), child);
        }

        public T remove(NodeRef removableChildRef) {
            for (Iterator<T> it = getModifiableList().iterator(); it.hasNext();) {
                T t = it.next();
                NodeRef nodeRef = t.getNodeRef();
                if (nodeRef == null) {
                    throw new IllegalStateException("when removing nodes by nodeRef, childs should have not-null nodeRef (for unsaved nodes childRef should be created using Repo");
                } else if (nodeRef.equals(removableChildRef)) {
                    it.remove();
                    getRemovedList().add(t);
                    return t;
                }
            }
            return null;
        }

        @Override
        public T remove(int index) {
            T removed = getModifiableList().remove(index);
            getRemovedList().add(removed);
            return removed;
        }

        @Override
        public T get(int index) {
            return getModifiableList().get(index);
        }

        @Override
        public int size() {
            return getModifiableList().size();
        }

        /**
         * Finds existing child by using newChild.nodeRef method and replaces it with newChild.
         * Replacement is inserted to the same index in list where replaceable element was
         * 
         * @param newChild
         * @throws IllegalArgumentException when element equal to given <code>newChild</code> was not found
         */
        public void replaceChild(T newChild) throws IllegalArgumentException {
            T replaceableChild = getChildByNodeRef(newChild.getNodeRef());
            if (replaceableChild == null) {
                throw new IllegalArgumentException("Can't replace child, as it is not found by nodeRef='" + newChild.getNodeRef() + "'");
            }
            int indexInList = getModifiableList().indexOf(replaceableChild);
            getModifiableList().remove(replaceableChild);
            getModifiableList().add(indexInList, newChild);
        }

        public T getChildByNodeRef(NodeRef childRef) {
            for (T item : getModifiableList()) {
                if (ObjectUtils.equals(childRef, item.getNodeRef())) {
                    return item;
                }
            }
            return null;
        }
    }

    protected <T extends BaseObject> ChildrenList<T> getChildren(Class<T> clazz) {
        if (getPropBoolean(BaseService.CHILDREN_NOT_LOADED) && !getPropBoolean(BaseService.CHILDREN_LOADING_IN_PROGRESS)) {
            throw new UnableToPerformException("Developer forgot to load child objects of type " + clazz.getSimpleName() + " for this operation. Object "
                    + getNode().getType().getLocalName() + "(" + getNodeRef() + ")");//
        }
        return new ChildrenList<T>(clazz);
    }

    // For service method that needs to save all children to repository
    protected Map<Class<? extends BaseObject>, List<? extends BaseObject>> getChildren() {
        return children;
    }

    // For service method that needs to save all children to repository
    protected Map<Class<? extends BaseObject>, List<? extends BaseObject>> getRemovedChildren() {
        return removedChildren;
    }

    // Override this method, if you need to create real objects
    protected <T extends BaseObject> T createNewChild(Class<T> clazz) {
        return createNewUnsaved(clazz, this);
    }

    protected void addLoadedChild(BaseObject child) {
        @SuppressWarnings("unchecked")
        Class<BaseObject> clazz = (Class<BaseObject>) getChildGroupingClass(child);
        getChildren(clazz).addExisting(child);
    }

    // If you need to group children in any other way, override this
    protected Class<? extends BaseObject> getChildGroupingClass(BaseObject child) {
        return child.getClass();
    }

    // For service method that needs to save this object; called only on first save
    // If need another assocType, then override
    /**
     * @return parent association type for this object, that is used when saving it
     */
    protected QName getAssocType() {
        return getNode().getType();
    }

    // For service method that needs to save this object; called only on first save
    // If need another assocName, then override
    /**
     * @return parent association name for this object, that is used when saving it
     */
    protected QName getAssocName() {
        return getNode().getType();
    }

    /**
     * Called by {@link BaseService} method that needs to save this object; called on every save.
     * 
     * @return if >= 0, then this node's childAssociationIndex is set accordingly; otherwise, childAssociationIndex is left as-is
     */
    protected int getAssocIndex() {
        return -1;
    }

    // -----------------

    private Map<QName, Serializable> getProperties(boolean copy) {
        return RepoUtil.toQNameProperties(getNode().getProperties(), copy);
    }

    protected Map<QName, Serializable> getNewProperties() {
        return getProperties(false);
    }

    protected Map<QName, Serializable> getChangedProperties() {
        Map<QName, Serializable> newProperties = getNewProperties();
        Map<QName, Serializable> changedProperties = new HashMap<QName, Serializable>(newProperties.size());
        for (Entry<QName, Serializable> entry : newProperties.entrySet()) {
            QName key = entry.getKey();
            Serializable newValue = entry.getValue();
            if (!originalProperties.containsKey(key) || !EqualsHelper.nullSafeEquals(originalProperties.get(key), newValue)) {
                changedProperties.put(key, newValue);
            }
        }
        // Properties that were in original map, but do not exist in new map
        Set<QName> removedProperties = new HashSet<QName>(originalProperties.keySet());
        removedProperties.removeAll(newProperties.keySet());
        for (QName propName : removedProperties) {
            changedProperties.put(propName, null);
        }
        return changedProperties;
    }

    public boolean isPropertyChanged(QName... props) {
        Set<QName> changedProps = getChangedProperties().keySet();
        for (QName qName : props) {
            if (changedProps.contains(qName)) {
                return true;
            }
        }
        return false;
    }

    public void restoreProp(QName... propNamesToReset) {
        if (propNamesToReset != null) {
            for (QName qName : propNamesToReset) {
                if (!originalProperties.containsKey(qName)) {
                    getNode().getProperties().remove(qName);
                } else {
                    setProp(qName, originalProperties.get(qName));
                }
            }
        }
    }

    protected boolean isChangedProperty(QName propName) {
        Map<QName, Serializable> newProperties = getNewProperties();
        return originalProperties.containsKey(propName) != newProperties.containsKey(propName)
                || !EqualsHelper.nullSafeEquals(originalProperties.get(propName), newProperties.get(propName));
    }

    protected void setChangedProperties(Map<QName, Serializable> changedProperties) {
        originalProperties.putAll(changedProperties);
    }

    private void clearOriginalProperties() {
        originalProperties.clear();
    }

    // START: temp property needed only by Field (but not FieldDefinition)
    private void setCopyOfNodeRef(NodeRef nodeRef) {
        setProp(COPY_OF_NODE_REF, nodeRef);
    }

    protected NodeRef getCopyOfNodeRef() {
        return getProp(COPY_OF_NODE_REF);
    }

    private void setCloneOfNodeRef(NodeRef nodeRef) {
        setProp(CLONE_OF_NODE_REF, nodeRef);
    }

    public NodeRef getCloneOfNodeRef() {
        return getProp(CLONE_OF_NODE_REF);
    }

    // END: temp property needed only by Field (but not FieldDefinition)

    @Override
    public String toString() {
        return WmNode.toString(this) + " [\n  node=" + StringUtils.replace(getNode().toString(), "\n", "\n  ") + "\n  parent=" + WmNode.toString(parent) + "\n  parentNodeRef="
                + parentNodeRef + "\n  children=" + WmNode.toString(children) + "\n  removedChildren=" + WmNode.toString(removedChildren) + additionalToString() + "\n]";
    }

    @Override
    public final int hashCode() { // equals & hashCode overridden and made final to prevent surprises. Use comparators instead of overriding
        return getNodeRef().hashCode();
    }

    @Override
    public final boolean equals(Object obj) { // equals & hashCode overridden and made final to prevent surprises. Use comparators instead of overriding
        BaseObject other = (BaseObject) obj; // fail-fast: casting added only to throw ClassCastException if smth else is passed in
        return getNodeRef().equals(other.getNodeRef());
    }

    protected String additionalToString() {
        return "";
    }

    /**
     * Default implementation will not handle exception - it just rethrows
     * 
     * @param e
     */
    protected void handleException(RuntimeException e) {
        throw e;
    }

    private void visit(Closure<BaseObject> closure) {
        closure.execute(this);
        for (Entry<Class<? extends BaseObject>, List<? extends BaseObject>> entry : getChildren().entrySet()) {
            for (BaseObject child : entry.getValue()) {
                child.visit(closure);
            }
        }
    }

}
