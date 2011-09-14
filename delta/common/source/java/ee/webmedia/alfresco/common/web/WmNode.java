package ee.webmedia.alfresco.common.web;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.QNameNodeMap;
import org.alfresco.web.bean.repository.TransientNode;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.utils.RepoUtil;

/**
 * Node that does not fetch properties (or anything) lazily, but takes data on creation. Thus does not depend on static FacesContext and can be used in service
 * layer (but see additional TODO in {@link #getNamespacePrefixResolver()}.
 * Can be used to both represent nodes loaded from repository or new nodes not saved to repository yet (see {@link #NOT_SAVED}.
 * 
 * @author Alar Kvell
 */
public class WmNode extends TransientNode {
    private static final long serialVersionUID = 1L;

    /**
     * Special NodeRef that can be used for detection that a {@link Node} object is not backed by repository node (not saved yet).
     */
    // public static final NodeRef NOT_SAVED = new NodeRef("NOT_SAVED", "NOT_SAVED", "NOT_SAVED");

    // TODO override getaspects to return unmodifiableSet - aspects are not changed with propertysheet operations

    // Currently this class only initializes: type, aspects, properties
    // If you need, then add support for other things (assocs, ...)

    /**
     * Aspects and properties map are copied. NodeRef can be null to indicate a not-saved node.
     */
    public WmNode(NodeRef nodeRef, QName type, Set<QName> aspects, Map<QName, Serializable> props) {
        this(nodeRef, type, aspects);
        if (props != null && props.size() != 0) {
            for (Entry<QName, Serializable> entry : props.entrySet()) {
                properties.put(entry.getKey(), entry.getValue());
            }
            propsRetrieved = true;
        }
    }

    public WmNode(NodeRef nodeRef, QName type, Set<QName> aspects, Map<QName, Serializable> props, Map<String, Map<String, AssociationRef>> addedAssociations) {
        this(nodeRef, type, aspects, props);
        getAddedAssociations().putAll(addedAssociations);
    }

    /**
     * Aspects and properties map are copied. NodeRef can be null to indicate a not-saved node.
     */
    public WmNode(NodeRef nodeRef, QName type, Map<String, Object> props, Set<QName> aspects) {
        this(nodeRef, type, aspects);

        if (props != null && props.size() != 0) {
            for (Entry<String, Object> entry : props.entrySet()) {
                properties.put(entry.getKey(), entry.getValue());
            }
            propsRetrieved = true;
        }
    }

    /**
     * Create new WmNode that will lazy-init its properties and aspects when needed
     * 
     * @param nodeRef
     * @param type
     */
    public WmNode(NodeRef nodeRef, QName type) {
        this(nodeRef, type, null);
    }

    private WmNode(NodeRef nodeRef, QName type, Set<QName> aspects) {
        super(type, null, null);
        Assert.notNull(type);

        this.nodeRef = nodeRef;// super constructor initializes nodeRef if one is not given(must reset)
        id = nodeRef == null ? null : nodeRef.getId();

        associations = new QNameNodeMap<String, List<AssociationRef>>(this, this);
        childAssociations = new QNameNodeMap<String, List<ChildAssociationRef>>(this, this);

        // show that the maps have been initialised
        boolean unsaved = isUnsaved(nodeRef);
        if (unsaved) {
            propsRetrieved = true;
            assocsRetrieved = true;
            childAssocsRetrieved = true;
        }

        // setup remaining variables
        path = null;
        locked = Boolean.FALSE;
        workingCopyOwner = Boolean.FALSE;

        if (aspects != null) {
            this.aspects = new HashSet<QName>(aspects);
        } else if (unsaved) {
            this.aspects = new HashSet<QName>();
        }
    }

    @Override
    public WmNode clone() {
        return new WmNode(getNodeRef(), getType(), getAspects(), RepoUtil.toQNameProperties(getProperties(), true), getAddedAssociations());
    }

    public void updateNodeRef(NodeRef newRef) {
        if (newRef == null) {
            newRef = RepoUtil.createNewUnsavedNodeRef();
        }
        nodeRef = newRef;
        id = newRef.getId();
    }

    @Override
    protected void initNode(Map<QName, Serializable> data) {
        // Do nothing
    }

    @Override
    public boolean hasPermission(String permission) {
        if (isUnsaved()) {
            throw new IllegalStateException("Not supported");
        }
        Boolean valid = null;
        if (permissions != null) {
            valid = permissions.get(permission);
        } else {
            permissions = new HashMap<String, Boolean>(8, 1.0f);
        }
        if (valid == null) {
            valid = BeanHelper.getPermissionService().hasPermission(nodeRef, permission) == AccessStatus.ALLOWED;
            permissions.put(permission, valid);
        }
        return valid;
    }

    @Override
    public void reset() {
        throw new RuntimeException("Not supported");
    }

    public boolean isSaved() {
        return !isUnsaved();
    }

    public boolean isUnsaved() {
        return isUnsaved(nodeRef);
    }

    public static boolean isSaved(final NodeRef nodeRef) {
        return !isUnsaved(nodeRef);
    }

    public static boolean isUnsaved(final NodeRef nodeRef) {
        return RepoUtil.isUnsaved(nodeRef);
    }

    @Override
    public String toString() {
        return toString(this) + "[\n  nodeRef=" + getNodeRef() + "\n  type=" + getType().toPrefixString(getNamespacePrefixResolver()) + "\n  aspects="
                + toString(getAspects(), getNamespacePrefixResolver()) + "\n  props=" + toString(RepoUtil.toQNameProperties(getProperties()), getNamespacePrefixResolver()) + "\n]";
    }

    public static String toString(Collection<?> collection) {
        if (collection == null) {
            return null;
        }
        StringBuilder s = new StringBuilder();
        s.append("[").append(collection.size()).append("]");
        if (collection.size() > 0) {
            for (Object o : collection) {
                s.append("\n    ");
                if (o != null) {
                    s.append(StringUtils.replace(o.toString(), "\n", "\n    "));
                }
            }
        }
        return s.toString();
    }

    public static String toString(Collection<QName> collection, NamespacePrefixResolver namespacePrefixResolver) {
        if (collection == null) {
            return null;
        }
        List<QName> list = new ArrayList<QName>(collection);
        Collections.sort(list);
        StringBuilder s = new StringBuilder();
        s.append("[").append(list.size()).append("]");
        if (list.size() > 0) {
            for (QName o : list) {
                s.append("\n    ");
                s.append(o.toPrefixString(namespacePrefixResolver));
            }
        }
        return s.toString();
    }

    public static String toString(Map<QName, Serializable> collection, NamespacePrefixResolver namespacePrefixResolver) {
        return toString(collection, namespacePrefixResolver, true);
    }

    public static String toString(Map<QName, Serializable> collection, NamespacePrefixResolver namespacePrefixResolver, boolean printValueClass) {
        if (collection == null) {
            return null;
        }
        Map<QName, Serializable> map = new TreeMap<QName, Serializable>(collection);
        StringBuilder s = new StringBuilder();
        s.append("[").append(map.size()).append("]");
        if (map.size() > 0) {
            for (Entry<QName, Serializable> entry : map.entrySet()) {
                s.append("\n    ");
                s.append(entry.getKey().toPrefixString(namespacePrefixResolver));
                s.append("=[");
                Serializable value = entry.getValue();
                if (value == null) {
                    s.append("null]");
                } else {
                    Class<? extends Serializable> valueClass = value.getClass();
                    String className = valueClass.getName();
                    if (valueClass.isPrimitive() || className.startsWith("java.lang.") || NodeRef.class.equals(valueClass) || QName.class.equals(valueClass)) {
                        s.append(valueClass.getSimpleName());
                    } else {
                        s.append(className);
                    }
                    s.append("]");
                    if (value instanceof QName) {
                        value = ((QName) value).toPrefixString(namespacePrefixResolver);
                    }
                    s.append(value);
                }
            }
        }
        return s.toString();
    }

    public static String toString(Map<? extends Object, ? extends Collection<?>> map) {
        if (map == null) {
            return null;
        }
        StringBuilder s = new StringBuilder();
        s.append("[").append(map.size()).append("]");
        if (map.size() > 0) {
            for (Entry<? extends Object, ? extends Collection<?>> entry : map.entrySet()) {
                s.append("\n    ");
                s.append(entry.getKey());
                s.append("=").append(StringUtils.replace(toString(entry.getValue()), "\n", "\n  "));
            }
        }
        return s.toString();
    }

    public static String toString(Object object) {
        if (object == null) {
            return "null";
        }
        return object.getClass().getSimpleName() + "@" + Integer.toHexString(object.hashCode());
    }

    public static String toString(String string) {
        if (string == null) {
            return "null";
        }
        return "String[" + string.length() + "]";
    }

}
