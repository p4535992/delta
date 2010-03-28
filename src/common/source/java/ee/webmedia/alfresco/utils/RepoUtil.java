package ee.webmedia.alfresco.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.dictionary.AspectDefinition;
import org.alfresco.service.cmr.dictionary.ClassDefinition;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.service.IClonable;

/**
 * Utility class related to Alfresco repository
 * 
 * @author Ats Uiboupin
 */
public class RepoUtil {
    public static final String TRANSIENT_PROPS_NAMESPACE = "temp";

    public static boolean isSystemProperty(QName propName) {
        return StringUtils.equals(TRANSIENT_PROPS_NAMESPACE, propName.getNamespaceURI()) || NamespaceService.SYSTEM_MODEL_1_0_URI.equals(propName.getNamespaceURI()) || ContentModel.PROP_NAME.equals(propName);
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

    public static boolean isSystemAspect(QName aspectName) {
        return ContentModel.ASPECT_REFERENCEABLE.equals(aspectName);
    }

    public static Map<QName, Serializable> copyProperties(Map<QName, Serializable> props) {
        Map<QName, Serializable> results = new HashMap<QName, Serializable>(props.size());
        for (Entry<QName, Serializable> entry : props.entrySet()) {
            results.put(entry.getKey(), copyProperty(entry.getValue()));
        }
        return results;
    }

    public static Map<QName, Serializable> toQNameProperties(Map<String, Object> props, boolean copy) {
        Map<QName, Serializable> results = new HashMap<QName, Serializable>(props.size());
        for (String strQName : props.keySet()) {
            QName qName = QName.createQName(strQName);
            Serializable value = copy ? copyProperty((Serializable) props.get(strQName)) : (Serializable) props.get(strQName);
            results.put(qName, value);
        }
        return results;
    }

    public static Map<QName, Serializable> toQNameProperties(Map<String, Object> stringQNameProperties) {
        return toQNameProperties(stringQNameProperties, false);
    }

    public static Map<String, Object> toStringProperties(Map<QName, Serializable> props) {
        Map<String, Object> results = new HashMap<String, Object>(props.size());
        Set<Entry<QName, Serializable>> entrySet = props.entrySet();
        for (Entry<QName, Serializable> entry : entrySet) {
            results.put(entry.getKey().toString(), entry.getValue());
        }
        return results;
    }

    public static Serializable copyProperty(Serializable property) {
        if (property == null) {
            return null;

        } else if (property instanceof String || property instanceof Integer || property instanceof Long || property instanceof Float || property instanceof Double || property instanceof Boolean || property instanceof QName || property instanceof NodeRef) {
            // immutable object
            return property;

        } else if (property instanceof Date) {
            return new Date(((Date) property).getTime());

        } else if (property instanceof ContentData) {
            ContentData contentData = (ContentData) property;
            return new ContentData(contentData.getContentUrl(), contentData.getMimetype(), contentData.getSize(), contentData.getEncoding(), contentData.getLocale());

        } else if (property instanceof List<?>) {
            @SuppressWarnings("unchecked")
            List<Serializable> list = (List<Serializable>) property;
            ArrayList<Serializable> newList = new ArrayList<Serializable>(list.size());
            for (Serializable prop : list) {
                newList.add(copyProperty(prop));
            }
            return newList;

        } else if (property instanceof IClonable) {
            return (Serializable) ((IClonable)property).clone();
        }
        throw new RuntimeException("Copying property not supported: " + property.getClass());
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

}
