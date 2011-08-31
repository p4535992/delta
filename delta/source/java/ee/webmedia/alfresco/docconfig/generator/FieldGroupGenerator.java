package ee.webmedia.alfresco.docconfig.generator;

import ee.webmedia.alfresco.docadmin.service.FieldGroup;

/**
 * @author Alar Kvell
 */
public interface FieldGroupGenerator {

    void generateFieldGroup(FieldGroup fieldGroup, GeneratorResults generatorResults);

}
