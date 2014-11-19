<<<<<<< HEAD
package ee.webmedia.alfresco.document.einvoice.model;

import java.io.Serializable;

import org.alfresco.web.bean.repository.Node;
import org.springframework.util.Assert;

public class TransactionDescParameter implements Serializable {

    private static final long serialVersionUID = 1L;
    private final Node node;

    public TransactionDescParameter(Node node) {
        Assert.notNull(node);
        this.node = node;
    }

    public void setName(String valueName) {
        node.getProperties().put(TransactionDescParameterModel.Props.NAME.toString(), valueName);
    }

    public String getName() {
        return (String) node.getProperties().get(TransactionDescParameterModel.Props.NAME.toString());
    }

    public void setMandatoryForOwner(Boolean mandatory) {
        node.getProperties().put(TransactionDescParameterModel.Props.MANDATORY_FOR_OWNER.toString(), mandatory);
    }

    public Boolean getMandatoryForOwner() {
        return (Boolean) node.getProperties().get(TransactionDescParameterModel.Props.MANDATORY_FOR_OWNER.toString());
    }

    public void setMandatoryForCostManager(Boolean mandatory) {
        node.getProperties().put(TransactionDescParameterModel.Props.MANDATORY_FOR_COST_MANAGER.toString(), mandatory);
    }

    public Boolean getMandatoryForCostManager() {
        return (Boolean) node.getProperties().get(TransactionDescParameterModel.Props.MANDATORY_FOR_COST_MANAGER.toString());
    }

    public void setMandatoryForAccountant(Boolean mandatory) {
        node.getProperties().put(TransactionDescParameterModel.Props.MANDATORY_FOR_ACCOUNTANT.toString(), mandatory);
    }

    public Boolean getMandatoryForAccountant() {
        return (Boolean) node.getProperties().get(TransactionDescParameterModel.Props.MANDATORY_FOR_ACCOUNTANT.toString());
    }

    public Node getNode() {
        return node;
    }

}
=======
package ee.webmedia.alfresco.document.einvoice.model;

import java.io.Serializable;

import org.alfresco.web.bean.repository.Node;
import org.springframework.util.Assert;

public class TransactionDescParameter implements Serializable {

    private static final long serialVersionUID = 1L;
    private final Node node;

    public TransactionDescParameter(Node node) {
        Assert.notNull(node);
        this.node = node;
    }

    public void setName(String valueName) {
        node.getProperties().put(TransactionDescParameterModel.Props.NAME.toString(), valueName);
    }

    public String getName() {
        return (String) node.getProperties().get(TransactionDescParameterModel.Props.NAME.toString());
    }

    public void setMandatoryForOwner(Boolean mandatory) {
        node.getProperties().put(TransactionDescParameterModel.Props.MANDATORY_FOR_OWNER.toString(), mandatory);
    }

    public Boolean getMandatoryForOwner() {
        return (Boolean) node.getProperties().get(TransactionDescParameterModel.Props.MANDATORY_FOR_OWNER.toString());
    }

    public void setMandatoryForCostManager(Boolean mandatory) {
        node.getProperties().put(TransactionDescParameterModel.Props.MANDATORY_FOR_COST_MANAGER.toString(), mandatory);
    }

    public Boolean getMandatoryForCostManager() {
        return (Boolean) node.getProperties().get(TransactionDescParameterModel.Props.MANDATORY_FOR_COST_MANAGER.toString());
    }

    public void setMandatoryForAccountant(Boolean mandatory) {
        node.getProperties().put(TransactionDescParameterModel.Props.MANDATORY_FOR_ACCOUNTANT.toString(), mandatory);
    }

    public Boolean getMandatoryForAccountant() {
        return (Boolean) node.getProperties().get(TransactionDescParameterModel.Props.MANDATORY_FOR_ACCOUNTANT.toString());
    }

    public Node getNode() {
        return node;
    }

}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
