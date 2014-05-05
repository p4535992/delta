package ee.webmedia.alfresco.common.web;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.alfresco.repo.security.sync.NodeDescription;
import org.alfresco.service.cmr.dictionary.TypeDefinition;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceException;
import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.QNameNodeMap;
import org.alfresco.web.bean.repository.TransientNode;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.archivals.model.ArchivalsStoreVO;
import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.classificator.enums.VolumeType;
import ee.webmedia.alfresco.common.propertysheet.component.WMUIProperty;
import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;
import ee.webmedia.alfresco.docadmin.service.FieldDefinition;
import ee.webmedia.alfresco.docconfig.generator.fieldtype.DateGenerator;
import ee.webmedia.alfresco.docconfig.service.DocumentConfigServiceImpl;
import ee.webmedia.alfresco.document.search.web.DocumentDynamicSearchDialog;
import ee.webmedia.alfresco.functions.model.FunctionsModel;
import ee.webmedia.alfresco.log.PropDiffHelper;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.volume.model.VolumeModel;
import ee.webmedia.alfresco.volume.search.model.VolumeSearchModel;
import ee.webmedia.alfresco.workflow.model.CompoundWorkflowType;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.search.model.CompoundWorkflowSearchModel;

/**
 * Node that does not fetch properties (or anything) lazily, but takes data on creation. Thus does not depend on static FacesContext and can be used in service
 * layer (but see additional TODO in {@link #getNamespacePrefixResolver()}.
 * Can be used to both represent nodes loaded from repository or new nodes not saved to repository yet (see {@link #NOT_SAVED}.
 */
public class WmNode extends TransientNode {
    private static final long serialVersionUID = 1L;
    private static PropDiffHelper propDiffHelper;

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
        if (props != null && !props.isEmpty()) {
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

        if (props != null && !props.isEmpty()) {
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
        WmNode clone = new WmNode(getNodeRef(), getType(), getAspects(), RepoUtil.toQNameProperties(getProperties(), true), getAddedAssociations());

        // TODO deep cloning of values would be more correct
        clone.allChildAssociationsByAssocType = allChildAssociationsByAssocType;
        clone.allChildAssociationsByAssocTypeRetrieved = allChildAssociationsByAssocTypeRetrieved;
        clone.childAssocsRetrieved = childAssocsRetrieved;
        clone.childAssociations = childAssociations;
        clone.childAssociationsAdded = childAssociationsAdded;
        clone.childAssociationsRemoved = childAssociationsRemoved;

        return clone;
    }

    @Override
    public void updateNodeRef(NodeRef newRef) {
        if (newRef == null) {
            newRef = RepoUtil.createNewUnsavedNodeRef();
        }
        super.updateNodeRef(newRef);
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
        return super.hasPermission(permission);
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
        return toString(collection, false);
    }

    public static String toString(Collection<?> collection, boolean printClass) {
        StringBuilder s = new StringBuilder();
        toString(s, collection, printClass);
        return s.toString();
    }

    private static void toString(StringBuilder s, Collection<?> collection, boolean printClass) {
        toString(s, collection, null, printClass, false, "\n    ");
    }

    private static void toString(StringBuilder s, Collection<?> collection, QName prop, boolean printClass, boolean translateSpecialClasses, String argumentSeparator) {
        if (collection == null) {
            s.append("null");
            return;
        }
        boolean showCollectionsize = printClass;
        if (showCollectionsize) {
            s.append("[").append(collection.size()).append("]");
            showCollectionsize = false;
        }
        if (!collection.isEmpty()) {
            NamespaceService namespaceService = BeanHelper.getNamespaceService();
            for (Object o : collection) {
                if (!showCollectionsize) {
                    showCollectionsize = true;
                } else {
                    s.append(argumentSeparator);
                }
                if (printClass) {
                    toStringWithClass(s, o, namespaceService);
                } else {
                    if (o != null) {
                        valueToString(s, o, prop, namespaceService, printClass, translateSpecialClasses, argumentSeparator);
                    }
                }
            }
        }
        return;
    }

    public static String toString(Collection<QName> collection, NamespacePrefixResolver namespacePrefixResolver) {
        if (collection == null) {
            return null;
        }
        List<QName> list = new ArrayList<QName>(collection);
        Collections.sort(list);
        StringBuilder s = new StringBuilder();
        s.append("[").append(list.size()).append("]");
        if (!list.isEmpty()) {
            for (QName o : list) {
                s.append("\n    ");
                s.append(o.toPrefixString(namespacePrefixResolver));
            }
        }
        return s.toString();
    }

    /**
     * @param collection - properties
     * @param namespacePrefixResolver - always required
     * @param documentAdminService - only required, if we have dynamic properties in collection, otherwise can be null
     * @return
     */
    public static String toHumanReadableStringIfPossible(Map<QName, Serializable> collection, NamespacePrefixResolver namespacePrefixResolver,
            DocumentAdminService documentAdminService) {
        if (collection == null || collection.isEmpty()) {
            return "";
        }
        List<String> propsWithValues = new ArrayList<String>();
        for (Entry<QName, Serializable> entry : collection.entrySet()) {
            QName key = entry.getKey();
            String localName = key.getLocalName();
            if (localName.endsWith(WMUIProperty.AFTER_LABEL_BOOLEAN) || localName.endsWith(DateGenerator.PICKER_PREFIX) || key.equals(DocumentDynamicSearchDialog.SELECTED_STORES)) {
                continue;
            }
            StringBuilder s = new StringBuilder();
            if (localName.endsWith(DateGenerator.END_PREFIX)) {
                s.append(getPropTitle(namespacePrefixResolver, documentAdminService, DateGenerator.getOriginalQName(key)));
                s.append(" kuni");
            } else {
                s.append(getPropTitle(namespacePrefixResolver, documentAdminService, key));
            }
            s.append(" = ");
            Serializable value = entry.getValue();
            valueToString(s, value, key, namespacePrefixResolver, false, true, ", ");
            propsWithValues.add(s.toString());
        }
        return StringUtils.join(propsWithValues, ",\n ");
    }

    private static String getPropTitle(NamespacePrefixResolver namespacePrefixResolver, DocumentAdminService documentAdminService, QName key) {
        if (DocumentConfigServiceImpl.searchLabelIds.containsKey(key)) {
            return MessageUtil.getMessage(DocumentConfigServiceImpl.searchLabelIds.get(key));
        }
        if (documentAdminService != null) {
            FieldDefinition fieldDef = documentAdminService.getFieldDefinition(key.getLocalName());
            if (fieldDef != null) {
                return fieldDef.getName();
            }
        }
        return key.toPrefixString(namespacePrefixResolver);
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
        if (!map.isEmpty()) {
            for (Entry<QName, Serializable> entry : map.entrySet()) {
                s.append("\n    ");
                s.append(qnameToPrefixString(namespacePrefixResolver, entry.getKey()));
                s.append("=");
                toStringWithClass(s, entry.getValue(), namespacePrefixResolver);
            }
        }
        return s.toString();
    }

    private static String qnameToPrefixString(NamespacePrefixResolver namespacePrefixResolver, QName qname) {
        String qnameStr;
        try {
            qnameStr = qname.toPrefixString(namespacePrefixResolver);
        } catch (NamespaceException e) {
            qnameStr = qname.toString();
        }
        return qnameStr;
    }

    public static String toStringWithClass(Object value) {
        StringBuilder s = new StringBuilder();
        toStringWithClass(s, value, BeanHelper.getNamespaceService());
        return s.toString();
    }

    private static void toStringWithClass(StringBuilder s, Object value, NamespacePrefixResolver namespacePrefixResolver) {
        s.append("[");
        if (value == null) {
            s.append("null]");
        } else {
            Class<? extends Object> valueClass = value.getClass();
            String className = valueClass.getName();
            if (valueClass.isPrimitive() || className.startsWith("java.lang.") || NodeRef.class.equals(valueClass) || QName.class.equals(valueClass)) {
                s.append(valueClass.getSimpleName());
            } else {
                s.append(className);
            }
            s.append("]");
            valueToString(s, value, null, namespacePrefixResolver, true, false, "\n    ");
        }
    }

    private static void valueToString(StringBuilder s, Object value, QName prop, NamespacePrefixResolver namespacePrefixResolver, boolean printClass,
            boolean translateSpecialClasses,
            String argumentSeparator) {
        if (value instanceof QName) {
            TypeDefinition typeDef = BeanHelper.getDictionaryService().getType((QName) value);
            if (typeDef == null) {
                value = qnameToPrefixString(namespacePrefixResolver, (QName) value);
            } else {
                value = typeDef.getTitle();
            }
        }
        if (translateSpecialClasses && value instanceof NodeRef) {
            NodeService nodeService = BeanHelper.getNodeService();
            // properties may refer to not existing (deleted or not saved) nodeRefs
            QName theType = nodeService.exists((NodeRef) value) ? nodeService.getType((NodeRef) value) : null;
            if (theType != null) {
                if (theType.equals(FunctionsModel.Types.FUNCTIONS_ROOT)) {
                    if (BeanHelper.getFunctionsService().getFunctionsRoot().equals(value)) {
                        value = MessageUtil.getMessage("functions_title");
                    } else {
                        for (ArchivalsStoreVO archivalsStore : BeanHelper.getGeneralService().getArchivalsStoreVOs()) {
                            if (archivalsStore.getNodeRef().equals(value)) {
                                value = archivalsStore.getTitle();
                                break;
                            }
                        }
                    }
                } else if (theType.equals(FunctionsModel.Types.FUNCTION)) {
                    value = BeanHelper.getFunctionsService().getFunctionByNodeRef((NodeRef) value).getTitle();
                } else if (theType.equals(VolumeModel.Types.VOLUME)) {
                    value = BeanHelper.getVolumeService().getVolumeByNodeRef((NodeRef) value).getTitle();
                } else if (theType.equals(CaseModel.Types.CASE)) {
                    value = BeanHelper.getCaseService().getCaseByNoderef((NodeRef) value).getTitle();
                } else if (theType.equals(SeriesModel.Types.SERIES)) {
                    value = BeanHelper.getSeriesService().getSeriesByNodeRef((NodeRef) value).getTitle();
                }
            }
        }
        if (value instanceof Collection) {
            toString(s, (Collection<?>) value, prop, printClass, translateSpecialClasses, argumentSeparator);
        } else if (value instanceof NodeDescription) {
            NodeDescription nodeDesc = (NodeDescription) value;
            String childAssocs = nodeDesc.getChildAssociations().isEmpty() ? "" : "\n  childAssociations=" + toString(nodeDesc.getChildAssociations(), true);
            s.append(StringUtils.replace(toString(nodeDesc.getProperties(), namespacePrefixResolver) + childAssocs, "\n", "\n    "));
        } else {
            s.append(StringUtils.replace(getPropDiffHelper().value(prop, value, ""), "\n", "\n    "));
        }
    }

    private static PropDiffHelper getPropDiffHelper() {
        if (propDiffHelper == null) {
            propDiffHelper = new PropDiffHelper();
            propDiffHelper.labelEnum(CompoundWorkflowSearchModel.Props.TYPE, "", CompoundWorkflowType.class);
            propDiffHelper.labelEnum(CompoundWorkflowSearchModel.Props.STATUS, "", Status.class);
            propDiffHelper.labelEnum(VolumeSearchModel.Props.VOLUME_TYPE, "", VolumeType.class);
        }
        return propDiffHelper;
    }

    public static String toString(Map<? extends Object, ? extends Collection<?>> map) {
        if (map == null) {
            return null;
        }
        StringBuilder s = new StringBuilder();
        s.append("[").append(map.size()).append("]");
        if (!map.isEmpty()) {
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
