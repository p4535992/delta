package ee.webmedia.alfresco.docconfig.generator;

import ee.webmedia.alfresco.docadmin.service.Field;

/**
 * @author Alar Kvell
 */
public interface FieldGenerator {

    void generateField(Field field, GeneratorResults generatorResults);

}
