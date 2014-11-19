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
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
public interface FieldGroupGenerator {

    void generateFieldGroup(FieldGroup group, FieldGroupGeneratorResults generatorResults);

    Pair<Field, List<Field>> collectAndRemoveFieldsInOriginalOrderToFakeGroup(List<Field> modifiableFieldsList, Field field, Map<String, Field> fieldsByOriginalId);

}
