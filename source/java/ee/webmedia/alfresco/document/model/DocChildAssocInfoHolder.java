<<<<<<< HEAD
package ee.webmedia.alfresco.document.model;

import java.io.Serializable;
import java.util.Map;

import org.alfresco.service.namespace.QName;

/**
 * Conveniently holds needed information for document child creation
 * 
 * @author Kaarel Jõgeva
 */
public class DocChildAssocInfoHolder {

    private QName assocType;
    private QName assocTargetType;
    private Map<QName, Serializable> properties;

    public DocChildAssocInfoHolder(QName assocType, QName assocTargetType) {
        this.assocType = assocType;
        this.assocTargetType = assocTargetType;
    }

    public DocChildAssocInfoHolder(QName assocType, QName assocTargetType, Map<QName, Serializable> properties) {
        this.assocType = assocType;
        this.assocTargetType = assocTargetType;
        this.properties = properties;
    }

    public QName getAssocType() {
        return assocType;
    }

    public void setAssocType(QName assocType) {
        this.assocType = assocType;
    }

    public QName getAssocTargetType() {
        return assocTargetType;
    }

    public void setAssocTargetType(QName assocTargetType) {
        this.assocTargetType = assocTargetType;
    }

    public Map<QName, Serializable> getProperties() {
        return properties;
    }

    public void setProperties(Map<QName, Serializable> properties) {
        this.properties = properties;
    }
}
=======
package ee.webmedia.alfresco.document.model;

import java.io.Serializable;
import java.util.Map;

import org.alfresco.service.namespace.QName;

/**
 * Conveniently holds needed information for document child creation
 */
public class DocChildAssocInfoHolder {

    private QName assocType;
    private QName assocTargetType;
    private Map<QName, Serializable> properties;

    public DocChildAssocInfoHolder(QName assocType, QName assocTargetType) {
        this.assocType = assocType;
        this.assocTargetType = assocTargetType;
    }

    public DocChildAssocInfoHolder(QName assocType, QName assocTargetType, Map<QName, Serializable> properties) {
        this.assocType = assocType;
        this.assocTargetType = assocTargetType;
        this.properties = properties;
    }

    public QName getAssocType() {
        return assocType;
    }

    public void setAssocType(QName assocType) {
        this.assocType = assocType;
    }

    public QName getAssocTargetType() {
        return assocTargetType;
    }

    public void setAssocTargetType(QName assocTargetType) {
        this.assocTargetType = assocTargetType;
    }

    public Map<QName, Serializable> getProperties() {
        return properties;
    }

    public void setProperties(Map<QName, Serializable> properties) {
        this.properties = properties;
    }
}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
