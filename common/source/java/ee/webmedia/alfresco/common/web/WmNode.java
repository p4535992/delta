package ee.webmedia.alfresco.common.web;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.QNameNodeMap;
import org.alfresco.web.bean.repository.TransientNode;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

/**
 * Node that does not fetch properties (or anything) lazily, but takes data on creation. Thus does not depend on static FacesContext and can be used in service
 * layer (but see additional TODO in {@link #getNamespacePrefixResolver()}.
 * Can be used to both represent nodes loaded from repository or new nodes not saved to repository yet (see {@link #NOT_SAVED}.
 * 
 * @author Alar Kvell
 */
public class WmNode extends TransientNode {
    private static final long serialVersionUID = 1L;

    public static final StoreRef NOT_SAVED_STORE = new StoreRef("NOT_SAVED", "NOT_SAVED");

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
            this.propsRetrieved = true;
        }
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
            this.propsRetrieved = true;
        }
    }

    private WmNode(NodeRef nodeRef, QName type, Set<QName> aspects) {
        super(type, null, null);
        Assert.notNull(type);

        this.nodeRef = nodeRef;// super constructor initializes nodeRef if one is not given(must reset)
        this.id = nodeRef == null ? null : nodeRef.getId();

        this.associations = new QNameNodeMap<String, List<AssociationRef>>(this, this);
        this.childAssociations = new QNameNodeMap<String, List<ChildAssociationRef>>(this, this);

        // show that the maps have been initialised
        if (isUnsaved(nodeRef)) {
            this.propsRetrieved = true;
            this.assocsRetrieved = true;
            this.childAssocsRetrieved = true;
        }

        // setup remaining variables
        this.path = null;
        this.locked = Boolean.FALSE;
        this.workingCopyOwner = Boolean.FALSE;

        if (aspects == null) {
            this.aspects = new HashSet<QName>();
        } else {
            this.aspects = new HashSet<QName>(aspects);
        }
    }

    public WmNode copy() {
        return new WmNode(getNodeRef(), getType(), getProperties(), getAspects());
    }

    public void updateNodeRef(NodeRef nodeRef) {
        this.nodeRef = nodeRef;
        this.id = nodeRef == null ? null : nodeRef.getId();
    }

    @Override
    protected void initNode(Map<QName, Serializable> data) {
        // Do nothing
    }

    @Override
    protected ServiceRegistry getServiceRegistry() {
        // map'i lisamisel vaja NamespacePrefixResolver'it, mis superklassis küsitakse läbi serviceRegistry
        return super.getServiceRegistry();
    }

    @Override
    public NamespacePrefixResolver getNamespacePrefixResolver() {
        return super.getNamespacePrefixResolver();
    }

    @Override
    public boolean hasPermission(String permission) {
        throw new RuntimeException("Not supported");
    }

    @Override
    public void reset() {
        throw new RuntimeException("Not supported");
    }

    public boolean isUnsaved() {
        return isUnsaved(nodeRef);
    }

    private boolean isUnsaved(final NodeRef nodeRef) {
        return nodeRef == null || NOT_SAVED_STORE.equals(nodeRef.getStoreRef());
    }

    @Override
    public String toString() {
        return toString(this) + "[\n saved=" + !isUnsaved() + "\n nodeRef=" + getNodeRef() + "\n  type=" + getType() + "\n  aspects=" + toString(getAspects())
                + "\n  props=" + toString(getProperties().entrySet()) + "\n]";
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
                    s.append(StringUtils.replace(o.toString(), "\n", "\n        "));
                }
            }
        }
        return s.toString();
    }

    public static String toString(Collection<QName> collection, NamespaceService namespaceService) {
        if (collection == null) {
            return null;
        }
        StringBuilder s = new StringBuilder();
        s.append("[").append(collection.size()).append("]");
        if (collection.size() > 0) {
            for (QName o : collection) {
                s.append("\n    ");
                s.append(o.toPrefixString(namespaceService));
            }
        }
        return s.toString();
    }

    public static String toString(Map<QName, Serializable> collection, NamespaceService namespaceService) {
        if (collection == null) {
            return null;
        }
        StringBuilder s = new StringBuilder();
        s.append("[").append(collection.size()).append("]");
        if (collection.size() > 0) {
            for (Entry<QName, Serializable> entry : collection.entrySet()) {
                s.append("\n    ");
                s.append(entry.getKey().toPrefixString(namespaceService));
                s.append("=").append(entry.getValue());
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
