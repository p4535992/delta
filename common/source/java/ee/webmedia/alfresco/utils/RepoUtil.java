package ee.webmedia.alfresco.utils;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDictionaryService;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.dictionary.AspectDefinition;
import org.alfresco.service.cmr.dictionary.ClassDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.QNameMap;
import org.alfresco.util.GUID;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.service.IClonable;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WmNode;
<<<<<<< HEAD
=======
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
>>>>>>> develop-5.1
import ee.webmedia.alfresco.document.service.DocumentPropertySets;

/**
 * Utility class related to Alfresco repository
<<<<<<< HEAD
 * 
 * @author Ats Uiboupin
=======
>>>>>>> develop-5.1
 */
public class RepoUtil {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(RepoUtil.class);
    /** Namespace used for properties that shouldn't be saved to repository */
    public static final String TRANSIENT_PROPS_NAMESPACE = "temp";
    private static final StoreRef NOT_SAVED_STORE = new StoreRef("NOT_SAVED", "NOT_SAVED");

    public static boolean isSystemProperty(QName propName) {
        return isTransientProp(propName)
                || NamespaceService.SYSTEM_MODEL_1_0_URI.equals(propName.getNamespaceURI()) || ContentModel.PROP_NAME.equals(propName);
    }

    public static boolean isSystemAspect(QName aspectName) {
        return StringUtils.equals(TRANSIENT_PROPS_NAMESPACE, aspectName.getNamespaceURI())
                || NamespaceService.SYSTEM_MODEL_1_0_URI.equals(aspectName.getNamespaceURI());
    }

    /**
     * @param localName
     * @return property name that can be used to store property, that must not be saved to repository
     */
    public static QName createTransientProp(String localName) {
        return QName.createQName(TRANSIENT_PROPS_NAMESPACE, localName);
    }

    public static boolean isTransientProp(QName propName) {
        return StringUtils.equals(TRANSIENT_PROPS_NAMESPACE, propName.getNamespaceURI());
    }

    /**
     * @param node
     * @param property
     * @param testEqualityValue
     * @return true if given node has property with given qName that equals to equalityTestValue, false otherwise
     */
    public static boolean isExistingPropertyValueEqualTo(Node currentNode, final QName property, final Object equalityTestValue) {
        final Object realValue = currentNode.getProperties().get(property.toString());
        return equalityTestValue == null ? realValue == null : equalityTestValue.equals(realValue);
    }

    public static <K, V> Map<K, V> copyProperties(Map<K, V> props) {
        return copyProperties(props, null);
    }

    public static <K, V> Map<K, V> copyProperties(Map<K, V> props, Map<K, V> results) {
        return copyProperties(props, results, (K[]) null);
    }

    public static <K, V> Map<K, V> copyProperties(Map<K, V> props, Map<K, V> results, K... propsToCopy) {
        if (props == null) {
            return null;
        }
        if (results == null) {
            results = new HashMap<K, V>(props.size());
        }
        if (propsToCopy == null) {
            propsToCopy = (K[]) props.keySet().toArray();
        }
        for (K key : propsToCopy) {
            results.put(key, copyProperty(props.get(key)));
        }
        return results;
    }

    public static Map<QName, Serializable> toQNameProperties(Map<String, Object> props, boolean copy) {
        return toQNameProperties(props, copy, false);
    }

    public static Map<QName, Serializable> toQNameProperties(Map<String, Object> props, boolean copy, boolean excludeTempAndSystem) {
        Map<QName, Serializable> results = new HashMap<QName, Serializable>(props.size());
        for (String strQName : props.keySet()) {
            QName qName = QName.createQName(strQName);
            if (excludeTempAndSystem && isSystemProperty(qName)) {
                continue;
            }
            Serializable value = copy ? copyProperty((Serializable) props.get(strQName)) : (Serializable) props.get(strQName);
            results.put(qName, value);
        }
        return results;
    }

    public static Map<QName, Serializable> toQNameProperties(Map<String, Object> stringQNameProperties) {
        return toQNameProperties(stringQNameProperties, false);
    }

    public static Map<String, Object> toStringProperties(Map<QName, Serializable> props) {
        @SuppressWarnings("unchecked")
        Map<String, Object> results = new QNameMap<String, Object>(BeanHelper.getInstance()); // can get value by QName
        Set<Entry<QName, Serializable>> entrySet = props.entrySet();
        for (Entry<QName, Serializable> entry : entrySet) {
            results.put(entry.getKey().toString(), entry.getValue());
        }
        return results;
    }

    public static <T> T copyProperty(T property) {
        if (property == null) {
            return null;

        } else if (isImmutableProperty(property)) {
            // immutable object
            return property;

        } else if (property instanceof Date) {
            @SuppressWarnings("unchecked")
            T tmp = (T) new Date(((Date) property).getTime());
            return tmp;

        } else if (property instanceof ContentData) {
            ContentData contentData = (ContentData) property;
            @SuppressWarnings("unchecked")
            T tmp = (T) new ContentData(contentData.getContentUrl(), contentData.getMimetype() //
                    , contentData.getSize(), contentData.getEncoding(), contentData.getLocale());
            return tmp;

        } else if (property instanceof List<?>) {
            @SuppressWarnings("unchecked")
            List<Serializable> list = (List<Serializable>) property;
            ArrayList<Serializable> newList = new ArrayList<Serializable>(list.size());
            for (Serializable prop : list) {
                newList.add(copyProperty(prop));
            }
            @SuppressWarnings("unchecked")
            T tmp = (T) newList;
            return tmp;

        } else if (property instanceof IClonable<?>) {
            @SuppressWarnings("unchecked")
            T tmp = (T) ((IClonable<?>) property).clone();
            return tmp;
        }
        throw new RuntimeException("Copying property not supported: " + property.getClass());
    }

    private static boolean isImmutableProperty(Object property) {
        return property instanceof String || property instanceof Integer || property instanceof Long || property instanceof Float
                || property instanceof Double || property instanceof Boolean || property instanceof QName || property instanceof NodeRef;
    }

    public static boolean isValidCopyProperty(Serializable property) {
        if (property == null) {
            return true;
        }
        if (property instanceof List<?>) {
            @SuppressWarnings("unchecked")
            List<Serializable> list = (List<Serializable>) property;
            for (Serializable prop : list) {
                if (!isValidCopyProperty(prop)) {
                    return false;
                }
            }
            return true;
        }
        return isImmutableProperty(property) || property instanceof Date || property instanceof ContentData || property instanceof IClonable<?>;
    }

    /**
     * Gets a flattened list of all mandatory aspects for a given class
     * 
     * @param classDef the class
     * @param aspects a list to hold the mandatory aspects
     */
    // Copied from TransientNode#getMandatoryAspects
    public static void getMandatoryAspects(ClassDefinition classDef, LinkedHashSet<QName> aspects) {
        for (AspectDefinition aspect : classDef.getDefaultAspects()) {
            QName aspectName = aspect.getName();
            if (!aspects.contains(aspectName)) {
                getMandatoryAspects(aspect, aspects);
                aspects.add(aspect.getName());
            }
        }
    }

    public static boolean getPropertyBooleanValue(final Map<String, Object> properties, String property) {
        final Object val = properties.get(property);
        if (val == null) {
            return false;
        }
        if (val instanceof Boolean) {
            return (Boolean) val;
        } else if (val instanceof String) {
            return Boolean.valueOf((String) val);
        } else {
            throw new RuntimeException("Can't convert property '" + property + "' class '" + val.getClass() + " to boolean! property value='\n" + val + "'\n");
        }
    }

    public static Map<QName, Serializable> getNotEmptyProperties(Map<QName, Serializable> props) {
        Map<QName, Serializable> results = new HashMap<QName, Serializable>();
        for (Entry<QName, Serializable> entry : props.entrySet()) {
            Serializable value = entry.getValue();
            if (value == null) {
                continue;
            }
            if (value instanceof Collection) {
                Collection<?> collection = (Collection<?>) value;
                if (collection.size() > 0) {
                    for (Object object : collection) {
                        if (object != null && StringUtils.isNotBlank(object.toString())) {
                            results.put(entry.getKey(), value);
                            break;
                        }
                    }
                }
            } else if (StringUtils.isNotEmpty(value.toString())) {
                results.put(entry.getKey(), value);
            }
        }
        return results;
    }

    public static Map<QName, Serializable> copyTypeProperties(Map<QName, PropertyDefinition> typeProps, Node baseDoc) {
        if (typeProps == null) {
            return Collections.<QName, Serializable> emptyMap();
        }

        Map<QName, Serializable> baseProps = RepoUtil.toQNameProperties(baseDoc.getProperties());
        Map<QName, Serializable> targetProps = new HashMap<QName, Serializable>(baseProps.size());
        Serializable propValue = null;
        for (QName prop : typeProps.keySet()) {
            propValue = baseProps.get(prop);
            if (!isSystemProperty(prop) && !DocumentPropertySets.ignoredPropertiesWhenMakingCopy.contains(prop) && propValue != null) {
                targetProps.put(prop, propValue);
            }
        }
        return targetProps;
    }

    public static void validateSameSize(Collection<String> first, Collection<String> second, String firstName, String secondName) {
        if (first == null) {
            first = Collections.emptyList();
        }
        if (second == null) {
            second = Collections.emptyList();
        }
        if (first.size() != second.size()) {
            throw new RuntimeException("There should be same amount of " + firstName + " and " + secondName + "!\n\t" + first.size()
                    + " " + firstName + ":" + first + "\n\t" + second.size() + " " + secondName + ":" + second);
        }
    }

    public static Map<QName, Serializable> getPropertiesIgnoringSystem(Map<QName, Serializable> props, DictionaryService dictionaryService) {
        Map<QName, Serializable> filteredProps = new HashMap<QName, Serializable>(props.size());
        for (QName qName : props.keySet()) {
            // ignore system and contentModel properties
            if (RepoUtil.isSystemProperty(qName)) {
                continue;
            }
            Serializable value = props.get(qName);
            if (value == null) {
                // problem: when null is set as a value to multivalued property and stored to repository, then after loading back instead of null value is list containing null
                // workaround: replace null values with empty list
                PropertyDefinition propDef = dictionaryService.getProperty(qName);
                if (propDef != null && propDef.isMultiValued()) {
                    value = new ArrayList<Object>(0);
                }
            } else if (value instanceof String && (value.toString().length() == 0)) {
                // check for empty strings when using number types, set to null in this case
                PropertyDefinition propDef = dictionaryService.getProperty(qName);
                if (propDef != null) {
                    if (propDef.getDataType().getName().equals(DataTypeDefinition.DOUBLE) ||
                            propDef.getDataType().getName().equals(DataTypeDefinition.FLOAT) ||
                            propDef.getDataType().getName().equals(DataTypeDefinition.INT) ||
                            propDef.getDataType().getName().equals(DataTypeDefinition.LONG)) {
                        value = null;
                    }
                }
            }
            filteredProps.put(qName, value);
        }
        return filteredProps;
    }

    public static boolean propsEqual(Map<String, Object> savedProps, Map<String, Object> unSavedPprops) {
        Map<QName, Serializable> sP = RepoUtil.getPropertiesIgnoringSystem(RepoUtil.toQNameProperties(savedProps), getDictionaryService());
        Map<QName, Serializable> uP = RepoUtil.getPropertiesIgnoringSystem(RepoUtil.toQNameProperties(unSavedPprops), getDictionaryService());
        if (sP.size() != uP.size()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("propsEqual=false\nuP=" + WmNode.toString(uP, BeanHelper.getNamespaceService()) + "\nsP=" + WmNode.toString(sP, BeanHelper.getNamespaceService()));
            }
            return false; // at least one field/fieldGroup/separatorLine is added or removed
        }
        Set<QName> unSavedQNames = uP.keySet();
        Set<QName> savedQNames = sP.keySet();
        if (!savedQNames.containsAll(unSavedQNames)) {
            return false; // added props
        }
        if (!unSavedQNames.containsAll(savedQNames)) {
            return false; // removed props
        }
        for (Entry<QName, Serializable> entry : sP.entrySet()) {
            QName propName = entry.getKey();
            Serializable sPValue = entry.getValue();
            Serializable uPValue = uP.get(propName);
            if (!ObjectUtils.equals(sPValue, uPValue)) {
                return false;
            }
        }
        return true;
    }

    public static Set<QName> getAspectsIgnoringSystem(Set<QName> aspects) {
        Set<QName> filteredAspects = new HashSet<QName>();
        for (QName aspect : aspects) {
            if (!isSystemAspect(aspect)) {
                filteredAspects.add(aspect);
            }
        }
        return filteredAspects;
    }

    public static boolean isSaved(Node node) {
        return !isUnsaved(node);
    }

    public static boolean isUnsaved(Node node) {
        return node == null ? true : isUnsaved(node.getNodeRef());
    }

    public static boolean isSaved(NodeRef nodeRef) {
        return !isUnsaved(nodeRef);
    }

    public static boolean isUnsaved(NodeRef nodeRef) {
        return nodeRef == null || NOT_SAVED_STORE.equals(nodeRef.getStoreRef());
    }

    public static NodeRef createNewUnsavedNodeRef() {
        return new NodeRef(RepoUtil.NOT_SAVED_STORE, GUID.generate());
    }

    public static void copyProps(Map<String, Object> sourceProps, Map<String, Object> targetProps) {
        targetProps.putAll(sourceProps);
        targetProps.keySet().retainAll(sourceProps.keySet());
    }

    public static AssociationRef addAssoc(Node node, NodeRef otherNodeRef, QName assocType, boolean nodeIsSource) {
        Map<String, Map<String, AssociationRef>> addedAssociations = node.getAddedAssociations();
        Map<String, AssociationRef> newAssoc = addedAssociations.get(assocType.toString());
        if (newAssoc == null) {
            newAssoc = new HashMap<String, AssociationRef>(1);
        }
        NodeRef sourceRef = nodeIsSource ? node.getNodeRef() : otherNodeRef;
        NodeRef targetRef = nodeIsSource ? otherNodeRef : node.getNodeRef();
        final AssociationRef assocRef = new AssociationRef(sourceRef, assocType, targetRef);
        newAssoc.put(targetRef.toString(), assocRef);
        addedAssociations.put(assocType.toString(), newAssoc);
        return assocRef;
    }

    public static <T extends Serializable> T getListElement(Node document, QName propName, int index) {
        @SuppressWarnings("unchecked")
        List<T> list = (List<T>) document.getProperties().get(propName);
        T element = list != null && list.size() > index ? list.get(index) : null;
        return element;
    }

    public static List<String> getLocalNames(QName... qNames) {
        List<String> localNames = new ArrayList<String>();
        for (QName qName : qNames) {
            localNames.add(qName.getLocalName());
        }

        return localNames;
    }

    public static List<String> getNodeRefIds(List<NodeRef> nodeRefs) {
        List<String> nodeRefIds = new ArrayList<String>();
        for (NodeRef docRef : nodeRefs) {
            if (docRef != null) {
                nodeRefIds.add(docRef.getId());
            }
        }
        return nodeRefIds;
    }

    public static boolean isInWorkspace(Node node) {
        return (node == null ? false : node.getNodeRef().getStoreRef().getProtocol().equals(StoreRef.PROTOCOL_WORKSPACE));
    }

    public static Serializable flatten(Serializable value) {
        if (!(value instanceof List) || ((List) value).isEmpty()) {
            return value;
        }
        List<Serializable> list = (List) value;
        if (list.get(0) instanceof List) {
            List flattenedList = new ArrayList();
            for (Serializable nestedList : list) {
                Serializable flattenedValue = flatten(nestedList);
                if (flattenedValue instanceof List) {
                    flattenedList.addAll((Collection) flattenedValue);
                } else {
                    flattenedList.add(flattenedValue);
                }
            }
            return (Serializable) flattenedList;
        } else {
            return value;
        }
    }
<<<<<<< HEAD
=======

    public static String getArchivedObjectName(QName type, Map<QName, Serializable> properties) {
        if (type == null) {
            return null;
        }
        if (DocumentCommonModel.Types.DOCUMENT.equals(type)) {
            return (String) properties.get(DocumentCommonModel.Props.DOC_NAME);
        }
        return (String) properties.get(ContentModel.PROP_NAME);
    }

>>>>>>> develop-5.1
}
