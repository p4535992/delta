package ee.webmedia.alfresco.imap.web;

import java.io.Serializable;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.repository.Node;
import org.springframework.util.Assert;

public class ImapFolder implements Serializable {

    private static final long serialVersionUID = 1L;
    private final Node node;
    private final boolean isEmpty;

    public ImapFolder(Node node, boolean isEmpty) {
        Assert.notNull(node);
        this.node = node;
        this.isEmpty = isEmpty;
    }

    public Node getNode() {
        return node;
    }

    public NodeRef getNodeRef() {
        return node.getNodeRef();
    }

    public boolean getIsEmpty() {
        return isEmpty;
    }

    public String getName() {
        return (String) node.getProperties().get(ContentModel.PROP_NAME);
    }
}
