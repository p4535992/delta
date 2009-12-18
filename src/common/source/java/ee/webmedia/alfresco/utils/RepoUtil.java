package ee.webmedia.alfresco.utils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;

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
    
    public static Map<QName, Serializable> toQNameProperties(Map<String, Object> stringQNameProperties) {
        Map<QName, Serializable> results = new HashMap<QName, Serializable>(stringQNameProperties.size());
        for (String strQName : stringQNameProperties.keySet()) {
            QName qName = QName.createQName(strQName);
            results.put(qName, (Serializable) stringQNameProperties.get(strQName));
        }
        return results;
    }
    
    /**
     * Create node from nodRef and populate it with properties and aspects
     * @param nodeRef
     * @return
     */
    public static Node fetchNode(NodeRef nodeRef) {
        final Node node = new Node(nodeRef);
        node.getAspects();
        node.getProperties();
        return node;
    }

}
