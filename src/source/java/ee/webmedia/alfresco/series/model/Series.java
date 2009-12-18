package ee.webmedia.alfresco.series.model;

import java.io.Serializable;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.utils.beanmapper.AlfrescoModelProperty;
import ee.webmedia.alfresco.utils.beanmapper.AlfrescoModelType;

@AlfrescoModelType(uri = SeriesModel.URI)
public class Series implements Serializable, Comparable<Series> {

    private static final long serialVersionUID = 1L;

    @AlfrescoModelProperty(isMappable = false)
    private NodeRef functionNodeRef;
    // mappable fields
    private String type;
    private String seriesIdentifier;
    private String title;
    private String description;
    private String status;
    private Integer retentionPeriod; // can be empty
    private int order;
    private List<String> docType;

    @AlfrescoModelProperty(isMappable = false)
    private Node node;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSeriesIdentifier() {
        return seriesIdentifier;
    }

    public void setSeriesIdentifier(String seriesIdentifier) {
        this.seriesIdentifier = seriesIdentifier;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setFunctionNodeRef(NodeRef function) {
        this.functionNodeRef = function;
    }

    public NodeRef getFunctionNodeRef() {
        return functionNodeRef;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public Integer getRetentionPeriod() {
        return retentionPeriod;
    }

    public void setRetentionPeriod(Integer retentionPeriod) {
        this.retentionPeriod = retentionPeriod;
    }

    public List<String> getDocType() {
        return docType;
    }

    public void setDocType(List<String> docType) {
        this.docType = docType;
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\nType = " + type + "\n");
        sb.append("seriesIdentifier = " + seriesIdentifier + "\n");
        sb.append("title = " + title + "\n");
        sb.append("description = " + description + "\n");
        sb.append("status = " + status + "\n");
        sb.append("order = " + order + "\n");
        return sb.toString();
    }

    @Override
    public int compareTo(Series other) {
        if (getOrder() == other.getOrder()) {
            int cmpMark;
            if ((cmpMark = getSeriesIdentifier().compareTo(other.getSeriesIdentifier())) == 0) {
                return 0;
            }
            return cmpMark;
        }
        return getOrder() - other.getOrder();
    }

}
