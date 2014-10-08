<<<<<<< HEAD
package ee.webmedia.alfresco.document.einvoice.model;

import java.io.Serializable;

import org.alfresco.web.bean.repository.Node;
import org.springframework.util.Assert;

public class Dimension implements Serializable {

    private static final long serialVersionUID = 1L;
    private final Node node;

    public Dimension(Node node) {
        Assert.notNull(node);
        this.node = node;
    }

    public void setName(String valueName) {
        node.getProperties().put(DimensionModel.Props.NAME.toString(), valueName);
    }

    public String getName() {
        return (String) node.getProperties().get(DimensionModel.Props.NAME.toString());
    }

    public void setComment(String valueComment) {
        node.getProperties().put(DimensionModel.Props.COMMENT.toString(), valueComment);
    }

    public String getComment() {
        return (String) node.getProperties().get(DimensionModel.Props.COMMENT.toString());
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

public class Dimension implements Serializable {

    private static final long serialVersionUID = 1L;
    private final Node node;

    public Dimension(Node node) {
        Assert.notNull(node);
        this.node = node;
    }

    public void setName(String valueName) {
        node.getProperties().put(DimensionModel.Props.NAME.toString(), valueName);
    }

    public String getName() {
        return (String) node.getProperties().get(DimensionModel.Props.NAME.toString());
    }

    public void setComment(String valueComment) {
        node.getProperties().put(DimensionModel.Props.COMMENT.toString(), valueComment);
    }

    public String getComment() {
        return (String) node.getProperties().get(DimensionModel.Props.COMMENT.toString());
    }

    public Node getNode() {
        return node;
    }

}
>>>>>>> develop-5.1
