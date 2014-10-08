package ee.webmedia.alfresco.docconfig.generator;

import java.util.List;
import java.util.Map;

import org.alfresco.util.Pair;

import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docadmin.service.FieldGroup;

<<<<<<< HEAD
/**
 * @author Alar Kvell
 */
=======
>>>>>>> develop-5.1
public interface FieldGroupGenerator {

    void generateFieldGroup(FieldGroup group, FieldGroupGeneratorResults generatorResults);

    Pair<Field, List<Field>> collectAndRemoveFieldsInOriginalOrderToFakeGroup(List<Field> modifiableFieldsList, Field field, Map<String, Field> fieldsByOriginalId);

}
