<<<<<<< HEAD
package ee.webmedia.alfresco.document.einvoice.service;

import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;

//TODO: remove if more sophisticated error handling is not needed
public class EInvoiceValidationEventHandler implements ValidationEventHandler {

    @Override
    public boolean handleEvent(ValidationEvent event) {
        if (event.getSeverity() == ValidationEvent.FATAL_ERROR ||
                event.getSeverity() == ValidationEvent.ERROR) {
            return false;
        }

        return true;
    }

}
=======
package ee.webmedia.alfresco.document.einvoice.service;

import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;

//TODO: remove if more sophisticated error handling is not needed
public class EInvoiceValidationEventHandler implements ValidationEventHandler {

    @Override
    public boolean handleEvent(ValidationEvent event) {
        if (event.getSeverity() == ValidationEvent.FATAL_ERROR ||
                event.getSeverity() == ValidationEvent.ERROR) {
            return false;
        }

        return true;
    }

}
>>>>>>> develop-5.1
