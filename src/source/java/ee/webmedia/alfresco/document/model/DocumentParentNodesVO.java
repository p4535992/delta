package ee.webmedia.alfresco.document.model;

import org.alfresco.web.bean.repository.Node;

public class DocumentParentNodesVO {
    private Node functionNode;
    private Node seriesNode;
    private Node volumeNode;
    private Node caseNode;

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
