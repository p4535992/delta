<<<<<<< HEAD
package ee.webmedia.alfresco.document.file.web;

import java.io.Serializable;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.repository.Node;
import org.springframework.util.Assert;

/**
 * @author Riina Tens
 */
public class Subfolder implements Serializable {

    private static final long serialVersionUID = 1L;
    private final Node node;
    private int nrOfChildren = 0;

    public Subfolder(Node node, int nrOfChildren) {
        Assert.notNull(node);
        this.node = node;
        this.nrOfChildren = nrOfChildren;
    }

    public Node getNode() {
        return node;
    }

    public NodeRef getNodeRef() {
        return node.getNodeRef();
    }

    public boolean getIsEmpty() {
        return nrOfChildren == 0;
    }

    public String getName() {
        return (String) node.getProperties().get(ContentModel.PROP_NAME);
    }

    public int getNrOfChildren() {
        return nrOfChildren;
    }
}
=======
package ee.webmedia.alfresco.document.file.web;

import java.io.Serializable;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.repository.Node;
import org.springframework.util.Assert;

public class Subfolder implements Serializable {

    private static final long serialVersionUID = 1L;
    private final Node node;
    private int nrOfChildren = 0;

    public Subfolder(Node node, int nrOfChildren) {
        Assert.notNull(node);
        this.node = node;
        this.nrOfChildren = nrOfChildren;
    }

    public Node getNode() {
        return node;
    }

    public NodeRef getNodeRef() {
        return node.getNodeRef();
    }

    public boolean getIsEmpty() {
        return nrOfChildren == 0;
    }

    public String getName() {
        return (String) node.getProperties().get(ContentModel.PROP_NAME);
    }

    public int getNrOfChildren() {
        return nrOfChildren;
    }
}
>>>>>>> develop-5.1
