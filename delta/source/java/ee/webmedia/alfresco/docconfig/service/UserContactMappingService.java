package ee.webmedia.alfresco.docconfig.service;

import java.io.Serializable;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.docadmin.service.Field;

public interface UserContactMappingService {

    String BEAN_NAME = "UserContactMappingService";

    void registerOriginalFieldIdsMapping(Map<String, UserContactMappingCode> mapping);

    // TODO Alar: could we eliminate mappingDependency, because hiddenFieldDependency covers the same thing?
    void registerMappingDependency(String fieldIdAndOriginalFieldId, String hiddenFieldId);

    /**
     * @param field field. If a registered mapping not found for field, then return NAME mapping.
     * @return mapping mapping entries. If a field was registered with {@code null} mapping code, then it's entry is not returned.
     */
    Map<QName, UserContactMappingCode> getFieldIdsMappingOrDefault(Field field);

    /**
     * @param field field. If a registered mapping not found for field, then return {@code null}.
     * @return mapping mapping entries. If a field was registered with {@code null} mapping code, then it's entry is not returned.
     */
    Map<QName, UserContactMappingCode> getFieldIdsMappingOrNull(Field field);

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
    void setMappedValues(Map<QName, Serializable> props, Map<QName, UserContactMappingCode> fieldIdsMapping, NodeRef userOrContactRef, boolean multiValued);

    /**
     * @param fieldIdsMapping
     * @param userOrContactRef nodeRef of user or contact
     * @return map with same keys as fieldIdsMapping and corresponding mapped values; or {@code null} if result does not exist
     */
    Map<QName, Serializable> getMappedValues(Map<QName, UserContactMappingCode> fieldIdsMapping, NodeRef userOrContactRef);

}