package ee.webmedia.alfresco.docconfig.generator;

import ee.webmedia.alfresco.docadmin.service.Field;

public interface FieldGenerator {

    /**
     * Service pre-generates item and sets the following attributes on it:
     * <ul>
     * <li>name - from field.getFieldId()</li>
     * <li>display-label - from field.getName()</li>
     * <li>read-only and readOnlyIf - from field.getChangeableIf()</li>
     * </ul>
     * You must at least set the following yourself:
     * <ul>
     * <li>component-generator</li>
     * <li>forcedMandatory - if you change item name, then set this yourself, otherwise not needed</li>
     * <li>
     * </ul>
     * 
     * @param field
     * @param generatorResults
     */
    void generateField(Field field, GeneratorResults generatorResults);

}
