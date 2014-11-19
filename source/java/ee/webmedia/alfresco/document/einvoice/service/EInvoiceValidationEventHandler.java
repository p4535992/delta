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
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
