<<<<<<< HEAD
package ee.webmedia.alfresco.docconfig.service;

import java.io.Serializable;

import org.alfresco.service.namespace.QName;

/**
 * @author Alar Kvell
 *
 */
public class ContractPartyField implements Serializable {
    private static final long serialVersionUID = 1L;

    final private int index;
    final private QName field;
    final private Serializable value;

    public ContractPartyField(int index, QName field, Serializable value) {
        this.index = index;
        this.field = field;
        this.value = value;
    }

    public int getIndex() {
        return index;
    }

    public QName getField() {
        return field;
    }

    public Serializable getValue() {
        return value;
    }
=======
package ee.webmedia.alfresco.docconfig.service;

import java.io.Serializable;

import org.alfresco.service.namespace.QName;

/**
 *
 */
public class ContractPartyField implements Serializable {
    private static final long serialVersionUID = 1L;

    final private int index;
    final private QName field;
    final private Serializable value;

    public ContractPartyField(int index, QName field, Serializable value) {
        this.index = index;
        this.field = field;
        this.value = value;
    }

    public int getIndex() {
        return index;
    }

    public QName getField() {
        return field;
    }

    public Serializable getValue() {
        return value;
    }
>>>>>>> develop-5.1
}