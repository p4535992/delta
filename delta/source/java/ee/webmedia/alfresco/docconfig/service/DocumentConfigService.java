package ee.webmedia.alfresco.docconfig.service;

import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.classificator.constant.FieldType;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docconfig.generator.FieldGenerator;

/**
 * @author Alar Kvell
 */
public interface DocumentConfigService {

    String BEAN_NAME = "DocumentConfigService";

    void registerFieldGeneratorByType(FieldGenerator fieldGenerator, FieldType... fieldTypes);

    void registerFieldGeneratorById(FieldGenerator fieldGenerator, String... originalFieldIds);

    DocumentConfig getConfig(Node documentDynamicNode);

    DocumentConfig getSearchConfig();

    PropertyDefinition getPropertyDefinition(Node node, QName property);

    /**
     * Get property definition and field pairs that are declared for a document.
     * A pair always has non-null propertyDefinition; but field may be null, if it is a special hidden field.
     * 
     * @param documentDynamicNode
     * @return
     */
    Map<String, Pair<PropertyDefinition, Field>> getPropertyDefinitions(Node documentDynamicNode);

    void setDefaultPropertyValues(Node documentDynamicNode);

    void registerMultiValuedOverrideInSystematicGroup(Set<String> originalFieldIds);

    void registerHiddenFieldDependency(String hiddenFieldId, String fieldIdAndOriginalFieldId);

}
