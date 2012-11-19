package ee.webmedia.alfresco.docconfig.generator;

import java.util.Map;

import org.alfresco.util.Pair;

import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docconfig.service.DynamicPropertyDefinition;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamic;
import ee.webmedia.alfresco.utils.UnableToPerformException;

/**
 * @author Alar Kvell
 */
public interface SaveListener {

    // XXX make document class more general, so this system could be used on other types of objects?

    String getBeanName();

    /**
     * @param document
     * @param validationHelper you can add multiple error messages using validationHelper. If at least one error message is added, then saving is not performed and no save methods
     *            will be called (validate method will be called on all SaveListeners regardless).
     * @throws UnableToPerformException throw an exception if you wish to cancel saving; then all other error messages added using validationHelper, even by any other
     *             SaveListeners, are ignored.
     */
    void validate(DocumentDynamic document, ValidationHelper validationHelper);

    /**
     * @param document
     * @throws UnableToPerformException throw an exception if you wish to cancel saving.
     */
    void save(DocumentDynamic document);

    interface ValidationHelper {

        void addErrorMessage(String msgKey, Object... messageValuesForHolders);

        Map<String, Pair<DynamicPropertyDefinition, Field>> getPropDefs();

    }
}
