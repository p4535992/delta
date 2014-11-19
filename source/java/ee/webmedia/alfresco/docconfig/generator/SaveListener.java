package ee.webmedia.alfresco.docconfig.generator;

import java.util.Map;

import org.alfresco.util.Pair;

<<<<<<< HEAD
import ee.webmedia.alfresco.common.model.DynamicBase;
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docconfig.service.DynamicPropertyDefinition;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamic;
import ee.webmedia.alfresco.utils.UnableToPerformException;

<<<<<<< HEAD
/**
 * @author Alar Kvell
 */
public interface SaveListener {

=======
public interface SaveListener {

    // XXX make document class more general, so this system could be used on other types of objects?

>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    String getBeanName();

    /**
     * @param document
     * @param validationHelper you can add multiple error messages using validationHelper. If at least one error message is added, then saving is not performed and no save methods
     *            will be called (validate method will be called on all SaveListeners regardless).
     * @throws UnableToPerformException throw an exception if you wish to cancel saving; then all other error messages added using validationHelper, even by any other
     *             SaveListeners, are ignored.
     */
<<<<<<< HEAD
    void validate(DynamicBase document, ValidationHelper validationHelper);
=======
    void validate(DocumentDynamic document, ValidationHelper validationHelper);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5

    /**
     * @param document
     * @throws UnableToPerformException throw an exception if you wish to cancel saving.
     */
<<<<<<< HEAD
    void save(DynamicBase document);
=======
    void save(DocumentDynamic document);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5

    interface ValidationHelper {

        void addErrorMessage(String msgKey, Object... messageValuesForHolders);

        Map<String, Pair<DynamicPropertyDefinition, Field>> getPropDefs();

    }
}
