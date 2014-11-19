package ee.webmedia.alfresco.document.log.service;

import java.io.Serializable;

import org.alfresco.service.namespace.QName;

/**
 * Object for storing a (node) property value change.
<<<<<<< HEAD
 * 
 * @author Martti Tamm
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
 */
public class PropertyChange {

    private final QName property;

    private final Serializable oldValue;

    private final Serializable newValue;

    public PropertyChange(QName property, Serializable oldValue, Serializable newValue) {
        this.property = property;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public QName getProperty() {
        return property;
    }

    public Serializable getOldValue() {
        return oldValue;
    }

    public Serializable getNewValue() {
        return newValue;
    }
}
