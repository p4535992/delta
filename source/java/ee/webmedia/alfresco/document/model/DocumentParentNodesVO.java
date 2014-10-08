<<<<<<< HEAD
package ee.webmedia.alfresco.document.model;

import org.alfresco.web.bean.repository.Node;

public class DocumentParentNodesVO {
    private final Node functionNode;
    private final Node seriesNode;
    private final Node volumeNode;
    private final Node caseNode;

    public DocumentParentNodesVO(Node functionNode, Node seriesNode, Node volumeNode, Node caseNode) {
        super();
        this.functionNode = functionNode;
        this.seriesNode = seriesNode;
        this.volumeNode = volumeNode;
        this.caseNode = caseNode;
    }

    // START: getters / setters
    public Node getFunctionNode() {
        return functionNode;
    }

    public Node getSeriesNode() {
        return seriesNode;
    }

    public Node getVolumeNode() {
        return volumeNode;
    }

    public Node getCaseNode() {
        return caseNode;
    }
    // END: getters / setters
}
=======
package ee.webmedia.alfresco.document.model;

import org.alfresco.web.bean.repository.Node;

public class DocumentParentNodesVO {
    private final Node functionNode;
    private final Node seriesNode;
    private final Node volumeNode;
    private final Node caseNode;

    public DocumentParentNodesVO(Node functionNode, Node seriesNode, Node volumeNode, Node caseNode) {
        super();
        this.functionNode = functionNode;
        this.seriesNode = seriesNode;
        this.volumeNode = volumeNode;
        this.caseNode = caseNode;
    }

    // START: getters / setters
    public Node getFunctionNode() {
        return functionNode;
    }

    public Node getSeriesNode() {
        return seriesNode;
    }

    public Node getVolumeNode() {
        return volumeNode;
    }

    public Node getCaseNode() {
        return caseNode;
    }
    // END: getters / setters
}
>>>>>>> develop-5.1
