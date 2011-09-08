package ee.webmedia.alfresco.docconfig.service;

import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.classificator.constant.FieldType;
import ee.webmedia.alfresco.docconfig.generator.FieldGenerator;

/**
 * @author Alar Kvell
 */
public interface DocumentConfigService {

    String BEAN_NAME = "DocumentConfigService";

    void registerFieldGeneratorByType(FieldGenerator fieldGenerator, FieldType... fieldTypes);

    void registerFieldGeneratorById(FieldGenerator fieldGenerator, QName... fieldIds);

    DocumentConfig getConfig(Node documentDynamicNode);

    PropertyDefinition getPropertyDefinition(Node node, QName property);

}
