package ee.webmedia.alfresco.docconfig.service;

import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.docadmin.service.Field;

public interface UserContactMappingService {

    String BEAN_NAME = "UserContactMappingService";

    void registerOriginalFieldIdsMapping(Map<String, UserContactMappingCode> mapping);

    void registerMappingDependency(String fieldIdAndOriginalFieldId, String hiddenFieldId);

    /**
     * @param propName
     * @return mapping; if custom mapping not found for property, then return NAME mapping.
     */
    Map<QName, UserContactMappingCode> getFieldIdsMapping(Field field);

    /**
     * @param userOrContactRef nodeRef of user or contact
     * @return single value String if result exists or null if it doesn't exist.
     */
    String getMappedNameValue(NodeRef userOrContactRef);

    /**
     * Sets each prop value if result exists or null if it doesn't exist.
     * 
     * @param props
     * @param fieldIdsMapping
     * @param userOrContactRef nodeRefAsString of user or contact
     */
    void setMappedValues(Map<String, Object> props, Map<QName, UserContactMappingCode> fieldIdsMapping, NodeRef userOrContactRef, boolean multiValued);

    /**
     * @param mappingCodes
     * @param userOrContactRef nodeRefAsString of user or contact
     * @return list with same number of elements as mappingCodes; or {@code null} if result does not exist
     */
    List<String> getMappedValues(List<UserContactMappingCode> mappingCodes, NodeRef userOrContactRef);

}