<<<<<<< HEAD
package ee.webmedia.alfresco.document.einvoice.model;

import java.io.Serializable;

import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.web.WmNode;

public class TransactionTemplate implements Serializable {

    private static final long serialVersionUID = 1L;
    private final WmNode node;

    public TransactionTemplate(WmNode node) {
        Assert.notNull(node);
        this.node = node;
    }

    public void setName(String valueName) {
        node.getProperties().put(TransactionModel.Props.NAME.toString(), valueName);
    }

    public String getName() {
        return (String) node.getProperties().get(TransactionModel.Props.NAME.toString());
    }

    public void setActive(Boolean mandatory) {
        node.getProperties().put(TransactionModel.Props.ACTIVE.toString(), mandatory);
    }

    public Boolean getActive() {
        return (Boolean) node.getProperties().get(TransactionModel.Props.ACTIVE.toString());
    }

    public WmNode getNode() {
        return node;
    }

}
=======
package ee.webmedia.alfresco.document.einvoice.model;

import java.io.Serializable;

import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.web.WmNode;

public class TransactionTemplate implements Serializable {

    private static final long serialVersionUID = 1L;
    private final WmNode node;

    public TransactionTemplate(WmNode node) {
        Assert.notNull(node);
        this.node = node;
    }

    public void setName(String valueName) {
        node.getProperties().put(TransactionModel.Props.NAME.toString(), valueName);
    }

    public String getName() {
        return (String) node.getProperties().get(TransactionModel.Props.NAME.toString());
    }

    public void setActive(Boolean mandatory) {
        node.getProperties().put(TransactionModel.Props.ACTIVE.toString(), mandatory);
    }

    public Boolean getActive() {
        return (Boolean) node.getProperties().get(TransactionModel.Props.ACTIVE.toString());
    }

    public WmNode getNode() {
        return node;
    }

}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
